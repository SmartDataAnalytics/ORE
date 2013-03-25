/*
 * Copyright 2009 IT Mill Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.aksw.mole.ore;

import static org.aksw.mole.ore.util.URLParameters.ACTION;
import static org.aksw.mole.ore.util.URLParameters.DEBUGGING;
import static org.aksw.mole.ore.util.URLParameters.DEFAULT_GRAPH_URI;
import static org.aksw.mole.ore.util.URLParameters.ENDPOINT_URL;
import static org.aksw.mole.ore.util.URLParameters.ENRICHMENT;
import static org.aksw.mole.ore.util.URLParameters.ONTOLOGY_URI;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.aksw.mole.ore.model.SPARQLEndpointKnowledgebase;
import org.aksw.mole.ore.util.URLParameters;
import org.aksw.mole.ore.view.ApplicationView;
import org.aksw.mole.ore.view.EnrichmentView;
import org.aksw.mole.ore.view.SPARQLDebuggingView;
import org.aksw.mole.ore.view.WelcomeView;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.vaadin.appfoundation.authentication.SessionHandler;
import org.vaadin.appfoundation.authorization.Permissions;
import org.vaadin.appfoundation.authorization.jpa.JPAPermissionManager;
import org.vaadin.appfoundation.view.ViewHandler;
import org.vaadin.jonatan.contexthelp.ContextHelp;

import com.clarkparsia.owlapi.explanation.PelletExplanation;
import com.vaadin.Application;
import com.vaadin.terminal.ParameterHandler;
import com.vaadin.terminal.Terminal;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.Notification;

/**
 * The Application's "main" class
 */
@SuppressWarnings("serial")
public class OREApplication extends Application implements ParameterHandler, Window.CloseListener{
	private Window mainWindow;
	private ApplicationView appView;
	
	private final ContextHelp contextHelp = new ContextHelp();

	@Override
	public void init() {
		PelletExplanation.setup();
		setTheme("custom");
		
//		JPAContainer<Individual> persons = JPAContainerFactory.make(Individual.class, "test");
//		persons.addEntity(new Individual("label", "iri"));
		
		ViewHandler.initialize(this);
		SessionHandler.initialize(this);
		Permissions.initialize(this, new JPAPermissionManager());
		
//		try {
//			FacadeFactory.registerFacade("default", true);
//			User user = new User();
//			user.setUsername("test");
//			user.setPassword("test");
//			FacadeFactory.getFacade().store(user);
//			
//			System.out.println(FacadeFactory.getFacade().find(User.class, user.getId()));
//			
//		} catch (InstantiationException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		}
		
		// Create the application data instance
        UserSession sessionData = new UserSession(this);
        
        // Register it as a listener in the application context
        getContext().addTransactionListener(sessionData);
        
        UserSession.setContextHelp(contextHelp);
//        UserSession.getOWLOntologyManager().addOntologyLoaderListener(this);
        
        appView = new ApplicationView();
        
		mainWindow = new Window("ORE - Ontology Repair and Enrichment");
		mainWindow.setContent(appView);
		mainWindow.setSizeFull();
		mainWindow.addParameterHandler(this);
		mainWindow.addListener(this);
		mainWindow.addComponent(contextHelp);
		
		setMainWindow(mainWindow);
	}
	
	public ApplicationView getAppView() {
		return appView;
	}
	
	@Override
	public void handleParameters(Map<String, String[]> parameters) {
		if(parameters.containsKey(ENDPOINT_URL)){
			String endpointURL = URLParameters.decode(parameters.get(ENDPOINT_URL)[0]);
			String graph = URLParameters.decode(parameters.get(DEFAULT_GRAPH_URI)[0]);
			List<String> defaultGraphURIs = new ArrayList<String>();
			if(graph != null){
				defaultGraphURIs.add(graph);
			}
			try {
				SparqlEndpoint endpoint = new SparqlEndpoint(new URL(endpointURL), defaultGraphURIs, Collections.<String>emptyList());
				boolean isOnline = UserSession.getKnowledgebaseManager().isOnline(endpoint);
				if(isOnline){
					UserSession.getKnowledgebaseManager().setKnowledgebase(new SPARQLEndpointKnowledgebase(endpoint));
					if(parameters.containsKey(ACTION)){
						String action = URLParameters.decode(parameters.get(ACTION)[0]).toLowerCase();
						if(action.equals(ENRICHMENT)){
							ViewHandler.activateView(EnrichmentView.class);
						} else if(action.equals(DEBUGGING)){
							ViewHandler.activateView(SPARQLDebuggingView.class);
						} 
					} else {
						ViewHandler.activateView(WelcomeView.class);
					}
				} else {
					appView.onSPARQLEndpoint(endpointURL, graph);
				}
			} catch (MalformedURLException e) {
				appView.onSPARQLEndpoint(endpointURL, graph);
			}
			
		} else if(parameters.containsKey(ONTOLOGY_URI)){
			String ontologyURI = URLParameters.decode(parameters.get(ONTOLOGY_URI)[0]);
			appView.onLoadFromURI(ontologyURI);
		}
		
	}
	
	public void busy(boolean busy){
		if(busy){
			getMainWindow().addStyleName("waiting-cursor");
			getMainWindow().removeStyleName("default-cursor");
		} else{
			getMainWindow().removeStyleName("waiting-cursor");
			getMainWindow().addStyleName("default-cursor");
		}
	}

	@Override
	public void windowClose(CloseEvent e) {
		super.close();
//		ConfirmDialog.show(getMainWindow(), "You are closing the application", "All unsaved changes will be lost. Are you really sure?",
//		        "I am", "Not quite", new ConfirmDialog.Listener() {
//
//		            public void onClose(ConfirmDialog dialog) {
//		                if (dialog.isConfirmed()) {
//		                	getMainWindow().getApplication().close();
//		                } else {
//		                   
//		                }
//		            }
//		        });
		
	}
	
	@Override
	public void terminalError(Terminal.ErrorEvent event) {
	    // Call the default implementation.
	    super.terminalError(event);

	    // Some custom behaviour.
	    if (getMainWindow() != null) {
	        getMainWindow().showNotification(
	                "An unchecked exception occured!",
	                event.getThrowable().toString(),
	                Notification.TYPE_ERROR_MESSAGE);
	    }
	}

//	@Override
//	public void finishedLoadingOntology(LoadingFinishedEvent event) {
//		if(!event.isImported()){
//			getMainWindow().addWindow(new OntologyInitializationDialog());
//		}
//	}
//
//	@Override
//	public void startedLoadingOntology(LoadingStartedEvent event) {
//		
//	}
	
	public static void main(String[] args) throws Exception{
		
		String ontologyURL = "http://domonet.isti.cnr.it/owl/domOnt.owl";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = man.getOWLDataFactory();
		OWLOntology ontology = man.loadOntology(IRI.create(ontologyURL));
		OWLClass cls = dataFactory.getOWLClass(IRI.create("http://localhost/owl/domOnt.owl#SwitchingLight"));
		System.out.println(cls.getEquivalentClasses(ontology));
	}

}
