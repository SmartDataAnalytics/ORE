package org.aksw.mole.ore.widget;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.model.SPARQLEndpointKnowledgebase;
import org.aksw.mole.ore.view.ApplicationView;
import org.dllearner.kb.sparql.SparqlEndpoint;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Form;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class Connect2SPARQLEndpointDialog extends Window{
	
	private static final String TITLE = "Choose SPARQL endpoint";
	
	private TextField endpointURLField;
	private TextField defaultGraphURIField;
	
	private Button connectButton;
	private Button okButton;
	private Button cancelButton;
	
	private ApplicationView view;
	
	public Connect2SPARQLEndpointDialog(ApplicationView view) {
		setCaption(TITLE);
		setModal(true);
		setWidth("400px");
		setHeight(null);
		
		this.view = view;
		
		initUI();
	}
	
	public Connect2SPARQLEndpointDialog(ApplicationView view, String endpointURL, String defaultGraphURI) {
		this(view);
		
		endpointURLField.setValue(endpointURL);
		defaultGraphURIField.setValue(defaultGraphURI);
		
		onConnect();
	}
	
	private void initUI(){
		createForm();
	}
	
	private void createForm(){
		Form form = new Form();
		form.setWidth("100%");
		form.getLayout().setWidth("80%");
		addComponent(form);
		
		endpointURLField = new TextField("Endpoint URL");
		endpointURLField.setRequired(true);
		endpointURLField.setWidth("80%");
		form.addField("url", endpointURLField);
		
		defaultGraphURIField = new TextField("Default graph URI");
		defaultGraphURIField.setWidth("80%");
		form.addField("graph", defaultGraphURIField);
		
		connectButton = new Button("Connect");
		connectButton.setDisableOnClick(true);
		connectButton.addListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onConnect();
			}
		});
		form.addField("connect", connectButton);
		((FormLayout)form.getLayout()).setComponentAlignment(connectButton, Alignment.MIDDLE_CENTER);
		
		HorizontalLayout footerLayout = new HorizontalLayout();
		footerLayout.setWidth("100%");
		footerLayout.setSpacing(true);
		
		okButton = new Button("Ok");
		okButton.addListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					List<String> defaultGraphURIs = new ArrayList<String>();
					if(defaultGraphURIField.getValue() != null){
						defaultGraphURIs.add((String)defaultGraphURIField.getValue());
					}
					SparqlEndpoint endpoint = new SparqlEndpoint(new URL((String)endpointURLField.getValue()), defaultGraphURIs, Collections.<String>emptyList());
					UserSession.getKnowledgebaseManager().setKnowledgebase(new SPARQLEndpointKnowledgebase(endpoint));
					
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				Connect2SPARQLEndpointDialog.this.close();
			}
		});
		okButton.setEnabled(false);
		footerLayout.addComponent(okButton);
		
		cancelButton = new Button("Cancel");
		cancelButton.addListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				close();
			}
		});
		footerLayout.addComponent(cancelButton);
		okButton.setWidth("70px");
		cancelButton.setWidth("70px");
		footerLayout.setComponentAlignment(okButton, Alignment.MIDDLE_RIGHT);
		footerLayout.setComponentAlignment(cancelButton, Alignment.MIDDLE_LEFT);
		
		form.setFooter(footerLayout);
		
		endpointURLField.focus();
		
//		endpointURLField.setValue("http://live.dbpedia.org/sparql");
//		defaultGraphURIField.setValue("http://dbpedia.org");
	}
	
	
	private void onConnect(){
		try {
			List<String> defaultGraphURIs = new ArrayList<String>();
			if(defaultGraphURIField.getValue() != null){
				defaultGraphURIs.add((String)defaultGraphURIField.getValue());
			}
			SparqlEndpoint endpoint = new SparqlEndpoint(new URL((String)endpointURLField.getValue()), defaultGraphURIs, Collections.<String>emptyList());
			boolean isOnline = UserSession.getKnowledgebaseManager().isOnline(endpoint);
			if(isOnline){
				getWindow().showNotification("Endpoint is online.");
				okButton.setEnabled(true);
			} else {
				getWindow().showNotification("Endpoint not available.", Notification.TYPE_ERROR_MESSAGE);
			}
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
			getWindow().showNotification("Invalid endpoint URL.", Notification.TYPE_ERROR_MESSAGE);
		}
		connectButton.setEnabled(true);
	}

}
