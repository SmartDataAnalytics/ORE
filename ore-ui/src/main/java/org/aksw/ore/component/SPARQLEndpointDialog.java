package org.aksw.ore.component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.aksw.ore.ORESession;
import org.aksw.ore.model.SPARQLEndpointKnowledgebase;
import org.dllearner.kb.sparql.SparqlEndpoint;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class SPARQLEndpointDialog extends Window{
	
	private static final String TITLE = "Set SPARQL endpoint";
	
	private TextField endpointURLField;
	private TextField defaultGraphURIField;
	
	private Button okButton;
	private Button cancelButton;
	
	public SPARQLEndpointDialog() {
		setCaption(TITLE);
		setModal(true);
		setWidth("400px");
		setHeight(null);
		
		initUI();
	}
	
	public SPARQLEndpointDialog(String endpointURL, String defaultGraphURI) {
		endpointURLField.setValue(endpointURL);
		defaultGraphURIField.setValue(defaultGraphURI);
		okButton.click();
	}
	
	private void initUI(){
		FormLayout form = new FormLayout();
		form.setWidth("100%");
		form.setWidth("80%");
		setContent(form);
		
		endpointURLField = new TextField("Endpoint URL");
		endpointURLField.setRequired(true);
		endpointURLField.setWidth("80%");
		form.addComponent(endpointURLField);
		
		defaultGraphURIField = new TextField("Default graph URI");
		defaultGraphURIField.setWidth("80%");
		form.addComponent(defaultGraphURIField);
		
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth("100%");
		buttons.setSpacing(true);
		
		okButton = new Button("Ok");
		okButton.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					List<String> defaultGraphURIs = new ArrayList<String>();
					if(defaultGraphURIField.getValue() != null){
						defaultGraphURIs.add((String)defaultGraphURIField.getValue());
					}
					SparqlEndpoint endpoint = new SparqlEndpoint(new URL((String)endpointURLField.getValue()), defaultGraphURIs, Collections.<String>emptyList());
					
					boolean isOnline = ORESession.getKnowledgebaseManager().isOnline(endpoint);
					if(isOnline){
						Notification.show("Endpoint is online.");
						ORESession.getKnowledgebaseManager().setKnowledgebase(new SPARQLEndpointKnowledgebase(endpoint));
						close();
					} else {
						Notification.show("Endpoint not available.", Notification.Type.ERROR_MESSAGE);
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
					Notification.show("Invalid endpoint URL.", Notification.Type.ERROR_MESSAGE);
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
		okButton.setWidth("70px");
		cancelButton.setWidth("70px");
		buttons.setComponentAlignment(okButton, Alignment.MIDDLE_RIGHT);
		buttons.setComponentAlignment(cancelButton, Alignment.MIDDLE_LEFT);
		
		form.addComponent(buttons);
		
		endpointURLField.focus();
		
//		endpointURLField.setValue("http://live.dbpedia.org/sparql");
//		defaultGraphURIField.setValue("http://dbpedia.org");
	}
}
