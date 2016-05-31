package org.aksw.ore.component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.aksw.ore.OREConfiguration;
import org.aksw.ore.ORESession;
import org.aksw.ore.model.SPARQLEndpointKnowledgebase;
import org.apache.commons.validator.routines.UrlValidator;
import org.dllearner.core.ComponentInitException;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;

import com.vaadin.data.Validator;
import com.vaadin.event.Action;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class SPARQLEndpointDialog extends Window implements Action.Handler{
	
	private static final String TITLE = "Set SPARQL endpoint";
	
	private TextField endpointURLField;
	private TextField defaultGraphURIField;
	
	public Button okButton;
	private Button cancelButton;

	private ListSelect namedGraphList;
	
	Action remove_named_graph = new ShortcutAction("Remove named graph", ShortcutAction.KeyCode.DELETE, null);
	
	public SPARQLEndpointDialog() {
		setCaption(TITLE);
		setModal(true);
		setWidth("400px");
		setHeight(null);
		addCloseShortcut(KeyCode.ESCAPE);
		
		initUI();
		
		addActionHandler(this);
	}
	
	public SPARQLEndpointDialog(String endpointURL, String defaultGraphURI) {
		this();
		
		endpointURLField.setValue(endpointURL);
		defaultGraphURIField.setValue(defaultGraphURI);
		
//		okButton.click();
	}
	
	private void initUI(){
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setHeight(null);
		main.setSpacing(true);
		main.setMargin(true);
		setContent(main);
		final FormLayout form = new FormLayout();
		main.addComponent(form);
		form.setWidth("100%");
//		form.setWidth("80%");
//		form.setMargin(true);
//		setContent(form);
		
		//endpoint URL
		endpointURLField = new TextField("Endpoint URL");
		endpointURLField.setRequired(true);
		endpointURLField.setWidth("100%");
		endpointURLField.setInputPrompt("Enter endpoint URL");
		form.addComponent(endpointURLField);
		
		//default graph URI
		defaultGraphURIField = new TextField("Default graph");
		defaultGraphURIField.setInputPrompt("Enter default graph IRI");
		defaultGraphURIField.setWidth("100%");
		form.addComponent(defaultGraphURIField);
		
		//named graphs in a list
		final TextField namedGraphURIField = new TextField();
		namedGraphURIField.setDescription("Enter a named graph IRI and click 'Add' or press 'Return' key to add it to the list below.");
		namedGraphURIField.setWidth("100%");
		namedGraphURIField.setInputPrompt("Enter named graph IRI");
		namedGraphURIField.addValidator(new Validator() {
			UrlValidator urlValidator = new UrlValidator();
			
			@Override
			public void validate(Object value) throws InvalidValueException {
				if(value != null && !((String) value).isEmpty()){
					if(!urlValidator.isValid((String) value)){
						throw new InvalidValueException("Invalid IRI");
					}
				}
			}
		});
		
		namedGraphList = new ListSelect();
		namedGraphList.setWidth("100%");
		namedGraphList.setNullSelectionAllowed(false);
		namedGraphList.setMultiSelect(true);
		
		final Button addButton = new Button("Add");
		addButton.setDescription("Add the named graph to the list.");
		addButton.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				namedGraphURIField.validate();
				if(namedGraphURIField.getValue() != null){
					//TODO check for valid URI
					UrlValidator urlValidator = new UrlValidator();
					if(urlValidator.isValid(namedGraphURIField.getValue())){
						namedGraphList.addItem(namedGraphURIField.getValue());
						namedGraphURIField.setValue("");
					} 
				}
			}
		});
		namedGraphURIField.addFocusListener(new FocusListener() {
	        @Override
	        public void focus(FocusEvent event) {
	        	addButton.setClickShortcut(KeyCode.ENTER);
	        }
	    });
		namedGraphURIField.addBlurListener(new BlurListener() {
	        @Override
	        public void blur(BlurEvent event) {
	        	addButton.removeClickShortcut();
	        }
	    });
		
		HorizontalLayout hl = new HorizontalLayout();
		hl.setWidth("100%");
		hl.setSpacing(true);
		hl.addComponent(namedGraphURIField);
		hl.addComponent(addButton);
		hl.setComponentAlignment(addButton, Alignment.BOTTOM_CENTER);
		hl.setExpandRatio(namedGraphURIField, 1f);
		VerticalLayout namedGraphInput = new VerticalLayout();
		namedGraphInput.setSpacing(true);
		namedGraphInput.addComponent(hl);
		namedGraphInput.addComponent(namedGraphList);
		namedGraphInput.setCaption("Named graph(s)");
		form.addComponent(namedGraphInput);
		
		//buttons
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth("100%");
		buttons.setSpacing(true);
		
		okButton = new Button("Ok");
		okButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
		okButton.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					List<String> defaultGraphURIs = new ArrayList<>();
					if(defaultGraphURIField.getValue() != null){
						defaultGraphURIs.add((String)defaultGraphURIField.getValue());
					}
					SparqlEndpoint endpoint = new SparqlEndpoint(new URL((String)endpointURLField.getValue()), defaultGraphURIs, Collections.emptyList());
					
					boolean isOnline = ORESession.getKnowledgebaseManager().isOnline(endpoint);
					if(isOnline){
//						Notification.show("Endpoint is online.");
						SparqlEndpointKS ks = new SparqlEndpointKS(endpoint, OREConfiguration.getCacheDirectory());
						ks.init();
						
						ORESession.getKnowledgebaseManager().setKnowledgebase(new SPARQLEndpointKnowledgebase(ks));
						close();
					} else {
						Notification.show("Endpoint not available.", Notification.Type.ERROR_MESSAGE);
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
					Notification.show("Invalid endpoint URL.", Notification.Type.ERROR_MESSAGE);
				} catch (ComponentInitException e) {
					e.printStackTrace();
				}
			}
		});
//		okButton.setEnabled(false);
		buttons.addComponent(okButton);
		
		cancelButton = new Button("Cancel");
		cancelButton.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				close();
			}
		});
		buttons.addComponent(cancelButton);
		buttons.setComponentAlignment(okButton, Alignment.MIDDLE_RIGHT);
		buttons.setComponentAlignment(cancelButton, Alignment.MIDDLE_LEFT);
		
		main.addComponent(buttons);
		main.setExpandRatio(form, 1f);
		
		endpointURLField.focus();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.event.Action.Handler#getActions(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Action[] getActions(Object target, Object sender) {
		return new Action[]{remove_named_graph};
	}

	/* (non-Javadoc)
	 * @see com.vaadin.event.Action.Handler#handleAction(com.vaadin.event.Action, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void handleAction(Action action, Object sender, Object target) {
		if(namedGraphList.getValue() != null){
			Set<Object> selectedItems = (Set<Object>) namedGraphList.getValue();
			for (Object item : selectedItems) {
				namedGraphList.removeItem(item);
			}
		}
	}
}
