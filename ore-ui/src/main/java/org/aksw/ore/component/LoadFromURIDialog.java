package org.aksw.ore.component;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.aksw.ore.ORESession;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class LoadFromURIDialog extends Window{
	
	private TextField uriField;
	private Button okButton;
	
	public LoadFromURIDialog(String ontologyURI) {
		initUI();
		
		uriField.setValue(ontologyURI);
		setResizeLazy(false);
		
		okButton.setEnabled(true);
//		okButton.click();
	}
	
	public LoadFromURIDialog() {
		this("");
		
		okButton.setEnabled(false);
	}
	
	private void initUI(){
		setCaption("Load ontology from URI");
		setModal(true);
		setWidth("400px");
		addStyleName("no-vertical-drag-hints");
		addStyleName("no-horizontal-drag-hints");
		setCloseShortcut(KeyCode.ESCAPE);
		
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setHeightUndefined();
		main.setSpacing(true);
		main.setMargin(true);
		setContent(main);
		
		FormLayout l = new FormLayout();
		l.setSpacing(true);
		l.setSizeFull();
		l.setHeight(null);
		l.setMargin(true);
		main.addComponent(l);
		main.setExpandRatio(l, 1f);
		
		uriField = new TextField("Enter URI:");
		uriField.setWidth("100%");
		uriField.setHeight(null);
		uriField.setImmediate(true);
		uriField.setRequired(true);
//		uriField.setRequiredError("The URI must not be empty.");
		uriField.setTextChangeEventMode(TextChangeEventMode.EAGER);
		uriField.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				okButton.focus();
//				onLoadOntology();
			}
		});
		uriField.addTextChangeListener(new TextChangeListener() {
			@Override
			public void textChange(TextChangeEvent event) {
				okButton.setEnabled(!event.getText().isEmpty());
			}
		});
		l.addComponent(uriField);
		
		okButton = new Button("Ok", new Button.ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				onLoadOntology();
			}
		});
		okButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
		okButton.setImmediate(true);
		Button cancelButton = new Button("Cancel", new Button.ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				close();
			}
		});
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setWidth("100%");
		buttonLayout.setHeight(null);
		buttonLayout.setSpacing(true);
		buttonLayout.addComponent(okButton);
		buttonLayout.addComponent(cancelButton);
	
		buttonLayout.setComponentAlignment(okButton, Alignment.MIDDLE_RIGHT);
		buttonLayout.setComponentAlignment(cancelButton, Alignment.MIDDLE_LEFT);
		main.addComponent(buttonLayout);
		
		uriField.focus();
		okButton.setEnabled(false);
	}
	
	public void onLoadOntology(){
		try {
			String uri = (String)uriField.getValue();
			new URL(uri);
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			man.addOntologyLoaderListener(ORESession.getKnowledgebaseManager());
			System.out.println("Loading ontology ...");
			OWLOntology ontology = man.loadOntology(IRI.create(uri));
			System.out.println("... done.");
			ORESession.getKnowledgebaseManager().setKnowledgebase(new OWLOntologyKnowledgebase(ontology));
			close();
		} catch (MalformedURLException e) {
			Notification.show("An error occured while loading the ontology", "Invalid URI.\nReason:" + e.getLocalizedMessage(), Notification.Type.ERROR_MESSAGE);
			uriField.focus();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			Notification.show("An error occured while loading the ontology", "Could not load the ontology from the given URI.\nReason: " + e.getMessage(), Notification.Type.ERROR_MESSAGE);
			uriField.focus();
		} 
	}

}
