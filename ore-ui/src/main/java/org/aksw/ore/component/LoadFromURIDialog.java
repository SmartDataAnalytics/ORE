package org.aksw.ore.component;

import java.net.URI;

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
		onLoadOntology(ontologyURI);
		setResizeLazy(false);
	}
	
	public LoadFromURIDialog() {
		initUI();
	}
	
	private void initUI(){
		setCaption("Load ontology from URI");
		setModal(true);
		setWidth("300px");
		addStyleName("no-vertical-drag-hints");
		addStyleName("no-horizontal-drag-hints");
		setClosable(true);
		setCloseShortcut(KeyCode.ESCAPE, null);
		
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setHeight(null);
		main.setSpacing(true);
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
		uriField.setTextChangeEventMode(TextChangeEventMode.EAGER);
		uriField.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				okButton.focus();
				onLoadOntology((String)uriField.getValue());
			}
		});
		uriField.addTextChangeListener(new TextChangeListener() {
			@Override
			public void textChange(TextChangeEvent event) {
				okButton.setEnabled(!event.getText().isEmpty());
			}
		});
		l.addComponent(uriField);
		l.setExpandRatio(uriField, 1.0f);
		
		okButton = new Button("Ok", new Button.ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				onLoadOntology((String)uriField.getValue());
			}
		});
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
		okButton.setWidth("70px");
		cancelButton.setWidth("70px");
		buttonLayout.setComponentAlignment(okButton, Alignment.MIDDLE_RIGHT);
		buttonLayout.setComponentAlignment(cancelButton, Alignment.MIDDLE_LEFT);
		main.addComponent(buttonLayout);
		
		uriField.focus();
		okButton.setEnabled(false);
	}
	
	private void onLoadOntology(String uri){
		try {
			URI.create(uri);
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			man.addOntologyLoaderListener(ORESession.getKnowledgebaseManager());
			OWLOntology ontology = man.loadOntology(IRI.create(uri));
			ORESession.getKnowledgebaseManager().setKnowledgebase(new OWLOntologyKnowledgebase(ontology));
			close();
		} catch (IllegalArgumentException e){
			Notification.show("Error loading ontology", "Invalid URI.", Notification.Type.ERROR_MESSAGE);
			uriField.focus();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			Notification.show("Error loading ontology", "Could not load the ontology from the given URI.", Notification.Type.ERROR_MESSAGE);
			uriField.focus();
		}
	}

}
