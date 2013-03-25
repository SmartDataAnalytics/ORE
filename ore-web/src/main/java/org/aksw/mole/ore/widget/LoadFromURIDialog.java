package org.aksw.mole.ore.widget;

import java.net.URI;

import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.model.OWLOntologyKnowledgebase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.vaadin.henrik.superimmediatetextfield.SuperImmediateTextField;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
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
	}
	
	public LoadFromURIDialog() {
		initUI();
	}
	
	private void initUI(){
		setCaption("Load ontology from URI");
		setModal(true);
		setWidth("300px");
		
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setSpacing(true);
		setContent(main);
		
		HorizontalLayout l = new HorizontalLayout();
		l.setSpacing(true);
		l.setWidth("100%");
		l.setHeight(null);
		l.setMargin(true);
		main.addComponent(l);
		main.setExpandRatio(l, 1f);
		
		Label label = new Label("Enter URI:");
		label.setWidth(null);
		l.addComponent(label);
		l.setComponentAlignment(label, Alignment.MIDDLE_LEFT);
		
		uriField = new TextField();
		uriField.setWidth("100%");
		uriField.setImmediate(true);
		uriField.addListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				okButton.focus();
				onLoadOntology((String)uriField.getValue());
			}
		});
		uriField.addListener(new TextChangeListener() {
			@Override
			public void textChange(TextChangeEvent event) {
				okButton.setEnabled(!event.getText().isEmpty());
			}
		});
		l.addComponent(uriField);
		l.setExpandRatio(uriField, 1.0f);
		
		okButton = new Button("Ok");
		okButton.addListener(new Button.ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				onLoadOntology((String)uriField.getValue());
			}
		});
		Button cancelButton = new Button("Cancel");
		cancelButton.addListener(new Button.ClickListener() {

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
			man.addOntologyLoaderListener(UserSession.getKnowledgebaseManager());
			OWLOntology ontology = man.loadOntology(IRI.create(uri));
			UserSession.getKnowledgebaseManager().setKnowledgebase(new OWLOntologyKnowledgebase(ontology));
			close();
		} catch (IllegalArgumentException e){
			getApplication().getMainWindow().showNotification("Error loading ontology", "Invalid URI.", Notification.TYPE_ERROR_MESSAGE);
			uriField.focus();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			getApplication().getMainWindow().showNotification("Error loading ontology", "Could not load the ontology from the given URI.", Notification.TYPE_ERROR_MESSAGE);
			uriField.focus();
		}
	}

}
