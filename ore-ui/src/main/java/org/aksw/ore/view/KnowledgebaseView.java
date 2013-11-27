/**
 * 
 */
package org.aksw.ore.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.aksw.mole.ore.repository.tones.TONESRepository;
import org.aksw.ore.ORESession;
import org.aksw.ore.component.ConfigurablePanel;
import org.aksw.ore.component.FileUploadDialog;
import org.aksw.ore.component.KnowledgebaseAnalyzationDialog;
import org.aksw.ore.component.LoadFromURIDialog;
import org.aksw.ore.component.OntologyRepositoryDialog;
import org.aksw.ore.component.SPARQLEndpointDialog;
import org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener;
import org.aksw.ore.model.Knowledgebase;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.aksw.ore.model.SPARQLEndpointKnowledgebase;
import org.aksw.ore.model.SPARQLKnowledgebaseStats;
import org.aksw.ore.util.URLParameters;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.profiles.OWL2Profile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.vaadin.hene.popupbutton.PopupButton;

import com.google.common.base.Joiner;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * @author Lorenz Buehmann
 *
 */
public class KnowledgebaseView extends VerticalLayout implements View, KnowledgebaseLoadingListener {
	
	private Label kbInfo;
	private Button saveOntologyButton;
	private OntologyRepositoryDialog repositoryDialog;
	
	public KnowledgebaseView() {
		addStyleName("dashboard-view");
		setSizeFull();
		setSpacing(true);
		setMargin(true);
		
		Component buttons = createButtons();
//		buttons = createMenu();
		addComponent(buttons);
		
		ConfigurablePanel knowledgeBaseInfo = new ConfigurablePanel(createKnowledgeBaseInfo());
		addComponent(knowledgeBaseInfo);
		
		setExpandRatio(knowledgeBaseInfo, 1f);
		
		ORESession.getKnowledgebaseManager().addListener(this);
		
		refresh();
	}
	
	private Component createMenu(){
		MenuBar menu = new MenuBar();
		MenuItem item = menu.addItem("OWL Ontology", null);
		item.addItem("From file", new Command() {
			
			@Override
			public void menuSelected(MenuItem selectedItem) {
				
			}
		});
		item.addItem("From URI", new Command() {
			
			@Override
			public void menuSelected(MenuItem selectedItem) {
				
			}
		});
		
		menu.addItem("SPARQL Endpoint", new Command() {
			
			@Override
			public void menuSelected(MenuItem selectedItem) {
				SPARQLEndpointDialog dialog = new SPARQLEndpointDialog();
				getUI().addWindow(dialog);
			}
		});
		return menu;
	}
	
	private Component createButtons(){
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth("100%");
		buttons.setHeight(null);
		
//		Button ontologyButton = new Button("OWL Ontology");
//		ontologyButton.addStyleName("ontology-button");
//		buttons.addComponent(ontologyButton);
//		buttons.setComponentAlignment(ontologyButton, Alignment.MIDDLE_RIGHT);
		
		//OWL Ontology
		PopupButton ontologyButton = new PopupButton("OWL Ontology");
		ontologyButton.addStyleName("ontology-button");
		buttons.addComponent(ontologyButton);
		buttons.setComponentAlignment(ontologyButton, Alignment.MIDDLE_RIGHT);
		
		VerticalLayout popupLayout = new VerticalLayout();
		popupLayout.setSpacing(true);
		ontologyButton.setContent(popupLayout);
		popupLayout.addComponent(new Button("From file", new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onLoadOntologyFromFile();
			}
		}));
		popupLayout.addComponent(new Button("From URI", new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onLoadOntologyFromURI();
			}
		}));
		popupLayout.addComponent(new Button("Ontology repository", new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onLoadOntologyFromRepository();
			}
		}));
		
		//SPARQL endpoint
		Button endpointButton = new Button("SPARQL Endpoint", new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onSetSPARQLEndpoint();
			}
		});
		addStyleName("endpoint-button");
		buttons.addComponent(endpointButton);
		buttons.setComponentAlignment(endpointButton, Alignment.MIDDLE_LEFT);
		
		buttons.addComponent(createSaveOntologyButton());
		
		return buttons;
	}
	
	private void onLoadOntologyFromFile(){
		FileUploadDialog dialog = new FileUploadDialog();
		getUI().addWindow(dialog);
	}
	
	private void onLoadOntologyFromURI(){
		LoadFromURIDialog dialog = new LoadFromURIDialog();
		getUI().addWindow(dialog);
	}
	
	private void onLoadOntologyFromURI(String ontologyURI){
		LoadFromURIDialog dialog = new LoadFromURIDialog(ontologyURI);
		getUI().addWindow(dialog);
	}
	
	private void onLoadOntologyFromRepository(){
		if(repositoryDialog == null){
			TONESRepository repository = new TONESRepository();
			repository.initialize();
			repositoryDialog = new OntologyRepositoryDialog(repository);
		}
		getUI().addWindow(repositoryDialog);
	}
	
	private Button createSaveOntologyButton(){
		Button button = new Button("Save ontology");
		StreamResource res = new StreamResource(new StreamSource() {
			
			@Override
			public InputStream getStream() {
				Knowledgebase knowledgebase = ORESession.getKnowledgebaseManager().getKnowledgebase();
				if(knowledgebase != null){
					if(knowledgebase instanceof OWLOntologyKnowledgebase){
						final OWLOntology ontology = ((OWLOntologyKnowledgebase) knowledgebase).getOntology();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ByteArrayInputStream bais;
						try {System.out.println(ontology);
							ontology.getOWLOntologyManager().saveOntology(ontology, new RDFXMLOntologyFormat(), baos);
							bais = new ByteArrayInputStream(baos.toByteArray());
							baos.close();
							return bais;
						} catch (OWLOntologyStorageException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				return null;
			}
		},"ontology.owl");
		FileDownloader downloader = new FileDownloader(res);
		downloader.extend(button);
		
		return button;
	}
	
	private void onSetSPARQLEndpoint(){
		SPARQLEndpointDialog dialog = new SPARQLEndpointDialog();
		getUI().addWindow(dialog);
	}
	
	private void onSetSPARQLEndpoint(String endpointURL, String defaultGraph){
		SPARQLEndpointDialog dialog = new SPARQLEndpointDialog(endpointURL, defaultGraph);
		getUI().addWindow(dialog);
		dialog.okButton.click();
	}
	
	public void refresh(){
		Knowledgebase knowledgebase = ORESession.getKnowledgebaseManager().getKnowledgebase();
		if(knowledgebase != null){
			if(knowledgebase instanceof OWLOntologyKnowledgebase){
				visualizeOntology((OWLOntologyKnowledgebase) knowledgebase);
			} else {
				visualizeSPARQLEndpoint((SPARQLEndpointKnowledgebase) knowledgebase);
			}
		}
	}
	
	private Component createKnowledgeBaseInfo(){
		kbInfo = new Label("</br>");
		kbInfo.setContentMode(ContentMode.HTML);
		
		return kbInfo;
	}
	
	private void visualizeSPARQLEndpoint(SPARQLEndpointKnowledgebase kb){
		SparqlEndpoint endpoint = kb.getEndpoint();
		SPARQLKnowledgebaseStats stats = kb.getStats();
		String url = endpoint.getURL().toString();
		String htmlTable = 
				"<table>" +
				"<tr class=\"even\"><td>URL</td><td><a href=\"" + url + "\">" + url + "</td></tr>";
		if(!endpoint.getDefaultGraphURIs().isEmpty()){
			htmlTable += "<tr class=\"odd\"><td>Default Graph</td><td>" + endpoint.getDefaultGraphURIs().iterator().next() + "</td></tr>";
		}
		if(!endpoint.getNamedGraphURIs().isEmpty()){
			htmlTable += "<tr class=\"odd\"><td>Named Graph(s)</td><td>" + Joiner.on(", ").join(endpoint.getNamedGraphURIs()) + "</td></tr>";
		}
		if(stats != null){
			if(stats.getOwlClassCnt() != -1){
				htmlTable += "<tr class=\"even\"><td>#Classes</td><td>" + stats.getOwlClassCnt() + "</td></tr>";
			}
			if(stats.getOwlObjectPropertyCnt() != -1){
				htmlTable += "<tr class=\"odd\"><td>#ObjectProperties</td><td>" + stats.getOwlObjectPropertyCnt() + "</td></tr>";
			}
			if(stats.getOwlDataPropertyCnt() != -1){
				htmlTable += "<tr class=\"even\"><td>#DataProperties</td><td>" + stats.getOwlDataPropertyCnt() + "</td></tr>";
			}
		
		}
		htmlTable += "</table>";	
		kbInfo.setCaption("SPARQL Endpoint");
		kbInfo.setValue(htmlTable);
	}
	
	private void visualizeOntology(OWLOntologyKnowledgebase kb){
		OWLOntology ontology = kb.getOntology();
		int nrOfClasses = ontology.getClassesInSignature(true).size();
		int nrOfObjectProperties = ontology.getObjectPropertiesInSignature(true).size();
		int nrOfDataProperties = ontology.getDataPropertiesInSignature(true).size();
		int nrOfIndividuals = ontology.getIndividualsInSignature(true).size();
		OWLProfileReport report = new OWL2Profile().checkOntology(ontology);
		OWLProfile profile = report.getProfile();
		
		String htmlTable = 
				"<table>" +
				"<tr class=\"even\"><td>#Classes</td><td>" + nrOfClasses + "</td></tr>" +
				"<tr class=\"odd\"><td>#ObjectProperties</td><td>" + nrOfObjectProperties + "</td></tr>" +
				"<tr class=\"even\"><td>#DataProperties</td><td>" + nrOfDataProperties + "</td></tr>" +
				"<tr class=\"odd\"><td>#Individuals</td><td>" + nrOfIndividuals + "</td></tr>" +
				"<tr class=\"even\"><td>OWL2 Profile</td><td>" + profile.getName() + "</td></tr>" +
				"<tr class=\"odd\"><td>Consistent</td><td>" + kb.isConsistent() + "</td></tr>";
		if(kb.isConsistent()){
			htmlTable += "<tr class=\"even\"><td>Coherent</td><td>" + kb.isCoherent() + "</td></tr>";
			if(!kb.isCoherent()){
				htmlTable += "<tr class=\"odd\"><td>#Unsatisfiable Classes</td><td>" + kb.getReasoner().getUnsatisfiableClasses().getEntitiesMinusBottom().size() + "</td></tr>";
			}
					
		}
		
		htmlTable += "</table>";
		kbInfo.setCaption("OWL Ontology");
		kbInfo.setValue(htmlTable);
	}

	/* (non-Javadoc)
	 * @see com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener.ViewChangeEvent)
	 */
	@Override
	public void enter(ViewChangeEvent event) {System.out.println("enter " + this);
		handleURLRequestParameters();
	}
	
	private void handleURLRequestParameters(){
		String endpointURL = VaadinService.getCurrentRequest().getParameter(URLParameters.ENDPOINT_URL);
		String defaultGraph = VaadinService.getCurrentRequest().getParameter(URLParameters.DEFAULT_GRAPH);
		String ontologyURI = VaadinService.getCurrentRequest().getParameter(URLParameters.ONTOLOGY_URL);
		if(endpointURL != null){
//			try {
//				SparqlEndpoint endpoint = new SparqlEndpoint(new URL(endpointURL), defaultGraph);
//				
//				boolean isOnline = ORESession.getKnowledgebaseManager().isOnline(endpoint);
//				if(isOnline){
//					ORESession.getKnowledgebaseManager().setKnowledgebase(new SPARQLEndpointKnowledgebase(endpoint));
//				}
//			} catch (MalformedURLException e) {
//				onSetSPARQLEndpoint(endpointURL, defaultGraph);
//			}
			onSetSPARQLEndpoint(endpointURL, defaultGraph);
		} else if(ontologyURI != null){
			onLoadOntologyFromURI(ontologyURI);
		}
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#knowledgebaseChanged(org.aksw.ore.model.Knowledgebase)
	 */
	@Override
	public void knowledgebaseChanged(Knowledgebase knowledgebase) {
		final KnowledgebaseAnalyzationDialog dialog = new KnowledgebaseAnalyzationDialog();
		ORESession.getKnowledgebaseManager().addListener(dialog);
		UI.getCurrent().addWindow(dialog);
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				ORESession.getKnowledgebaseManager().analyzeKnowledgebase();
				ORESession.getKnowledgebaseManager().removeListener(dialog);
				UI.getCurrent().access(new Runnable() {
					
					@Override
					public void run() {
						dialog.close();
					}
				});
			}
		}).start();
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#knowledgebaseAnalyzed(org.aksw.ore.model.Knowledgebase)
	 */
	@Override
	public void knowledgebaseAnalyzed(Knowledgebase knowledgebase) {
		UI.getCurrent().access(new Runnable() {
			
			@Override
			public void run() {
				refresh();
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#knowledgebaseStatusChanged(org.aksw.ore.model.Knowledgebase)
	 */
	@Override
	public void knowledgebaseStatusChanged(Knowledgebase knowledgebase) {
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#message(java.lang.String)
	 */
	@Override
	public void message(String message) {
	}

	

}
