/**
 * 
 */
package org.aksw.ore.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.aksw.mole.ore.repository.tones.TONESRepository;
import org.aksw.mole.ore.sparql.SPARULTranslator;
import org.aksw.ore.ORESession;
import org.aksw.ore.component.FileUploadDialog;
import org.aksw.ore.component.KnowledgebaseAnalyzationDialog;
import org.aksw.ore.component.KnowledgebaseChangesTable;
import org.aksw.ore.component.LoadFromURIDialog;
import org.aksw.ore.component.OntologyRepositoryDialog;
import org.aksw.ore.component.SPARQLEndpointDialog;
import org.aksw.ore.component.WhitePanel;
import org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener;
import org.aksw.ore.model.Knowledgebase;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.aksw.ore.model.SPARQLEndpointKnowledgebase;
import org.aksw.ore.model.SPARQLKnowledgebaseStats;
import org.aksw.ore.util.URLParameters;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.profiles.OWL2Profile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.vaadin.hene.popupbutton.PopupButton;

import com.google.common.base.Joiner;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.StreamResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

/**
 * @author Lorenz Buehmann
 *
 */
public class KnowledgebaseView extends VerticalLayout implements View, KnowledgebaseLoadingListener {
	
	private Label kbInfo;
	private OntologyRepositoryDialog repositoryDialog;
	private KnowledgebaseChangesTable table;
	private VerticalLayout kbInfoPanel;
	private VerticalLayout changesPanel;
	private Button applyChangesButton;
	private Label noKBLabel;
	
	public KnowledgebaseView() {
		setSizeFull();
		setSpacing(true);
		setMargin(true);
		
		addComponent(createButtons());
		
		Component kbInfoPanel = createKnowledgeBaseInfo();
		addComponent(kbInfoPanel);
		
		addComponent(createChangesPanel());
		
		setExpandRatio(kbInfoPanel, 1f);
		
		ORESession.getKnowledgebaseManager().addListener(this);
		ORESession.getKnowledgebaseManager().addListener(table);
		
		refresh();
		
		showNoKBInfo();
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
		VerticalLayout layout = new VerticalLayout();
//		layout.setSizeFull();
		layout.setWidth("100%");
		layout.setHeight(null);
//		layout.setSizeUndefined();
		
		Label label = new Label("<h3>Set up knowledge base:</h3>", ContentMode.HTML);
//		label.setHeight(null);
		layout.addComponent(label);
		
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth(null);
//		buttons.setSizeFull();
//		buttons.setHeight(null);
//		buttons.setSizeUndefined();
//		buttons.setStyleName("kb-view");
//		buttons.setStyleName("caption-below-icon");
		layout.addComponent(buttons);
//		layout.setExpandRatio(buttons, 1f);
		
//		Button ontologyButton = new Button("OWL Ontology");
//		ontologyButton.addStyleName("ontology-button");
//		buttons.addComponent(ontologyButton);
//		buttons.setComponentAlignment(ontologyButton, Alignment.MIDDLE_RIGHT);
		
		//OWL Ontology
		PopupButton ontologyButton = new PopupButton("OWL Ontology");
		ontologyButton.addStyleName("ontology-button");
		ThemeResource icon = new ThemeResource("img/owl-ontology.jpg");
		ontologyButton.setIcon(icon);
//		ontologyButton.addStyleName("borderless");
		ontologyButton.setHeight("140px");
//		ontologyButton.setHeight("100%");
		buttons.addComponent(ontologyButton);
		buttons.setComponentAlignment(ontologyButton, Alignment.MIDDLE_RIGHT);
		
		VerticalLayout popupLayout = new VerticalLayout();
		popupLayout.setSpacing(true);
		ontologyButton.setContent(popupLayout);
		Button button = new Button("From file", new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onLoadOntologyFromFile();
			}
		});
		popupLayout.addComponent(button);
		button.setWidth("100%");
		button = new Button("From URI", new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onLoadOntologyFromURI();
			}
		});
		button.setWidth("100%");
		popupLayout.addComponent(button);
		popupLayout.addComponent(new Button("Ontology repository", new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onLoadOntologyFromRepository();
			}
		}));
		
		//SPARQL endpoint
		Button endpointButton = new NativeButton("SPARQL Endpoint", new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onSetSPARQLEndpoint();
			}
		});
		endpointButton.setIcon(new ThemeResource("img/sparql-128.png"));
//		endpointButton.setHeight("140px");
//		endpointButton.addStyleName("borderless");
//		endpointButton.setHeight("100%");
		buttons.addComponent(endpointButton);
		buttons.setComponentAlignment(endpointButton, Alignment.MIDDLE_LEFT);
		
		return layout;
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
		dialog.onLoadOntology();
	}
	
	private void onLoadOntologyFromRepository(){
		if(repositoryDialog == null){
			TONESRepository repository = new TONESRepository();
			repository.initialize();
			repositoryDialog = new OntologyRepositoryDialog(repository);
		}
		getUI().addWindow(repositoryDialog);
	}
	
	private void onSaveOntology(){
		StreamResource res = new StreamResource(new StreamSource() {
			
			@Override
			public InputStream getStream() {
				Knowledgebase knowledgebase = ORESession.getKnowledgebaseManager().getKnowledgebase();
				if(knowledgebase != null){
					if(knowledgebase instanceof OWLOntologyKnowledgebase){
						final OWLOntology ontology = ((OWLOntologyKnowledgebase) knowledgebase).getOntology();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ByteArrayInputStream bais;
						try {
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
		downloader.extend(applyChangesButton);
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
			kbInfo.setVisible(true);
			noKBLabel.setVisible(false);
			if(knowledgebase instanceof OWLOntologyKnowledgebase){
				visualizeOntology((OWLOntologyKnowledgebase) knowledgebase);
				applyChangesButton.setDescription("Export modified ontology.");
				onSaveOntology();
			} else {
				visualizeSPARQLEndpoint((SPARQLEndpointKnowledgebase) knowledgebase);
				applyChangesButton.setDescription("Export changes as SPARQL Update statements.");
			}
		}
		changesPanel.setVisible(false);
	}
	
	private Component createChangesPanel(){
		changesPanel = new VerticalLayout();
		changesPanel.setSizeFull();
		Label label = new Label("<h3>Changes:</h3>", ContentMode.HTML);
		changesPanel.addComponent(label);
		
		table = new KnowledgebaseChangesTable();
		table.setSizeFull();
		changesPanel.addComponent(table);
		changesPanel.setExpandRatio(table, 1f);
		
		applyChangesButton = new Button("Export");
		applyChangesButton.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onExport();
			}
		});
		changesPanel.addComponent(applyChangesButton);
		changesPanel.setComponentAlignment(applyChangesButton, Alignment.MIDDLE_RIGHT);
		
		return changesPanel;
	}
	
	private Component createKnowledgeBaseInfo(){
		kbInfoPanel = new VerticalLayout();
		kbInfoPanel.setSizeFull();
		
		Label label = new Label("<h3>Current knowledge base:<h3>", ContentMode.HTML);
		label.setHeight(null);
		kbInfoPanel.addComponent(label);
		
		kbInfo = new Label("</br>");
		kbInfo.setContentMode(ContentMode.HTML);
		kbInfoPanel.addComponent(kbInfo);
		kbInfo.setVisible(false);
		
		kbInfoPanel.setExpandRatio(kbInfo, 1f);
		
		return kbInfoPanel;
	}
	
	private void showNoKBInfo(){
		kbInfo.setVisible(false);
		noKBLabel = new Label(FontAwesome.FROWN_O.getHtml() + "<span class='text'>You haven't set up a knowledge base, yet!</span>", ContentMode.HTML);
		noKBLabel.setStyleName("no-kb-label");
//		noKBLabel.setSizeFull();
		kbInfoPanel.addComponent(noKBLabel);
		kbInfoPanel.setExpandRatio(noKBLabel, 1f);
	}
	
	private void onExport(){
		if(ORESession.getKnowledgebaseManager().getKnowledgebase() instanceof SPARQLEndpointKnowledgebase){
			onDumpSPARUL();
		} else {
			
		}
	}
	
	private void onDumpSPARUL(){
		try {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = man.createOntology();
			SPARULTranslator translator = new SPARULTranslator(man, ontology, false);
			Set<OWLOntologyChange> changes = ORESession.getKnowledgebaseManager().getChanges();
			if(!changes.isEmpty()){
				VerticalLayout content = new VerticalLayout();
				String sparulString = translator.translate(changes);
				content.addComponent(new Label(sparulString, ContentMode.PREFORMATTED));
				final Window window = new Window("SPARQL Update statements", content);
				window.setWidth("1000px");
				window.setHeight("400px");
				window.center();
				window.addCloseListener(new CloseListener() {
					
					@Override
					public void windowClose(CloseEvent e) {
						getUI().removeWindow(window);
					}
				});
				getUI().addWindow(window);
				window.focus();
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
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
		kbInfoPanel.setCaption("SPARQL Endpoint");
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
		kbInfoPanel.setCaption("OWL Ontology");
		kbInfo.setValue(htmlTable);
	}

	/* (non-Javadoc)
	 * @see com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener.ViewChangeEvent)
	 */
	@Override
	public void enter(ViewChangeEvent event) {
		handleURLRequestParameters();
	}
	
	private void handleURLRequestParameters(){
		Map<String, String[]> parameterMap = new TreeMap<String, String[]>(String.CASE_INSENSITIVE_ORDER);
		parameterMap.putAll(VaadinService.getCurrentRequest().getParameterMap());
		
		String endpointURL = parameterMap.containsKey(URLParameters.ENDPOINT_URL) ? parameterMap.get(URLParameters.ENDPOINT_URL)[0] : null;
		String defaultGraph = parameterMap.containsKey(URLParameters.DEFAULT_GRAPH) ? parameterMap.get(URLParameters.DEFAULT_GRAPH)[0] : null;
		String ontologyURI = parameterMap.containsKey(URLParameters.ONTOLOGY_URL) ? parameterMap.get(URLParameters.ONTOLOGY_URL)[0] : null;
		
		if(endpointURL != null){
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
		onAnalyzeKnowledgebase();
	}
	
	private void onAnalyzeKnowledgebase(){
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

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#knowledgebaseModified(java.util.List)
	 */
	@Override
	public void knowledgebaseModified(Set<OWLOntologyChange> changes) {
		changesPanel.setVisible(!changes.isEmpty());
	}

}
