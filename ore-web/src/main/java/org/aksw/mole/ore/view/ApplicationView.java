package org.aksw.mole.ore.view;

import java.io.ByteArrayOutputStream;

import org.aksw.mole.ore.KnowledgebaseManager.KnowledgebaseLoadingListener;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.model.Knowledgebase;
import org.aksw.mole.ore.model.OWLOntologyKnowledgebase;
import org.aksw.mole.ore.model.SPARQLEndpointKnowledgebase;
import org.aksw.mole.ore.repository.OntologyRepository;
import org.aksw.mole.ore.repository.bioportal.BioPortalRepository;
import org.aksw.mole.ore.repository.tones.TONESRepository;
import org.aksw.mole.ore.task.BackgroundTask;
import org.aksw.mole.ore.util.DynamicStreamResource;
import org.aksw.mole.ore.widget.Connect2SPARQLEndpointDialog;
import org.aksw.mole.ore.widget.LoadFromURIDialog;
import org.aksw.mole.ore.widget.OntologyRepositoryDialog;
import org.aksw.mole.ore.widget.ProgressWindow;
import org.aksw.mole.ore.widget.UploadDialog;
import org.apache.commons.lang.StringEscapeUtils;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.vaadin.appfoundation.view.View;
import org.vaadin.appfoundation.view.ViewContainer;
import org.vaadin.appfoundation.view.ViewHandler;

import com.github.wolfie.refresher.Refresher;
import com.github.wolfie.refresher.Refresher.RefreshListener;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.vaadin.Application;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;

public class ApplicationView extends VerticalLayout implements ViewContainer, KnowledgebaseLoadingListener{
	
	private MenuBar menuBar = new MenuBar();
	
	private HorizontalLayout kbInfoPanel;
	private VerticalLayout mainPanel;
	private HorizontalLayout footerPanel = new HorizontalLayout();
	
	private View currentView;
	
	private MenuItem debuggingMenuItem;
	private MenuItem debugNamingProblemsMenuItem;
	private MenuItem debugLogicalProblemsMenuItem;
	private MenuItem enrichmentMenuItem;
	private MenuItem saveKnowledgebaseMenuItem;
	
	private boolean taskFinished = false;
	private ProgressWindow progressWindow;
	private Refresher refresher = new Refresher();
	
	public ApplicationView() {
		setSizeFull();
		
		Component header = createHeader();
		addComponent(header);
		
		initMenu();
		
		kbInfoPanel = new HorizontalLayout();
		kbInfoPanel.setWidth("100%");
		kbInfoPanel.addComponent(new Label("<b>Knowledge Base:</b>", Label.CONTENT_XHTML));
		addComponent(kbInfoPanel);
		
		mainPanel = new VerticalLayout();
		mainPanel.setSizeFull();
		mainPanel.setMargin(true);
		addComponent(mainPanel);
		setExpandRatio(mainPanel, 1f);
		
		createFooter();
		//register the views in this view container
		ViewHandler.addView(WelcomeView.class, this);
		ViewHandler.addView(LearningView.class, this);
		ViewHandler.addView(DebuggingView.class, this);
		ViewHandler.addView(InconsistencyDebuggingView.class, this);
		ViewHandler.addView(SPARQLDebuggingView.class, this);
		ViewHandler.addView(EnrichmentView.class, this);
		ViewHandler.addView(PatOMatView.class, this);
		
		//set default view
		ViewHandler.activateView(WelcomeView.class);
		
		refresher.setEnabled(false);
		refresher.setRefreshInterval(0);
		addComponent(refresher);
	}
	
	public void onTaskStarted(String task){
		refresher.setEnabled(true);
		refresher.setRefreshInterval(1000);
		progressWindow = new ProgressWindow(task);
		getApplication().getMainWindow().addWindow(progressWindow);
		progressWindow.center();
	}
	
	public void onTaskFinished(){
		refresher.setEnabled(false);
		refresher.setRefreshInterval(0);
		getApplication().getMainWindow().removeWindow(progressWindow);
	}
	
	private Component createHeader(){
		CustomLayout header = new CustomLayout("header");
		return header;
	}
	
	
	private void createFooter(){
//		footerPanel.setHeight("100%");
		footerPanel.setWidth("100%");
		Label dummy = new Label("");
		footerPanel.addComponent(dummy);
		footerPanel.setExpandRatio(dummy, 1.0f);
		
		ProgressIndicator progressBar = new ProgressIndicator();
		progressBar.setWidth(null);
		progressBar.setEnabled(false);
		footerPanel.addComponent(progressBar);
		footerPanel.setComponentAlignment(progressBar, Alignment.MIDDLE_RIGHT);
		addComponent(footerPanel);
	}
	
	/**
	 * Initializes the menu.
	 */
	private void initMenu(){
		MenuItem kb = menuBar.addItem("Knowledge Base", null);
		kb.addItem("From file...", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				onLoadFromFile();
			}
		});
		
		kb.addItem("From URI...", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				onLoadFromURI();
			}
		});
		
		MenuItem repository = kb.addItem("From repository...", null);
		
		repository.addItem("TONES", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				onLoadFromRepository(new TONESRepository());
			}
		});
		
		repository.addItem("BioPortal", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				onLoadFromRepository(new BioPortalRepository());
			}
		});
		
		kb.addItem("SPARQL endpoint...", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				onSPARQLEndpoint();
			}
		});
		
		saveKnowledgebaseMenuItem = kb.addItem("Save ontology...", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				onSaveOntology();
			}
		});
		
		MenuItem action = menuBar.addItem("Action", null);
		debuggingMenuItem = action.addItem("Debugging", null);
		
		debugLogicalProblemsMenuItem = debuggingMenuItem.addItem("Logical errors", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				onDebugging();
			}
		});
		
		debugNamingProblemsMenuItem = debuggingMenuItem.addItem("Naming problems", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				onPatOMat();
			}
		});
		
		enrichmentMenuItem = action.addItem("Enrichment", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				onLearning();
			}
		});
		
//		MenuItem help = menuBar.addItem("Help", new Command() {
//			@Override
//			public void menuSelected(MenuItem selectedItem) {
//				onHelp();
//			}
//		});
		
		menuBar.setWidth("100%");
		addComponent(menuBar);
		
		enableDebugging(false);
		enableEnrichment(false);
		saveKnowledgebaseMenuItem.setEnabled(false);
	}
	
	@Override
	public void attach() {
		super.attach();

//		((OWLOntologyKnowledgebase)UserSession.getOreManager().getKnowledgebase()).getOntology().getOWLOntologyManager().addOntologyLoaderListener(this);
		UserSession.getKnowledgebaseManager().addListener(this);
	}

	@Override
	public void detach() {
		super.detach();

//		((OWLOntologyKnowledgebase)UserSession.getOreManager().getKnowledgebase()).getOntology().getOWLOntologyManager().removeOntologyLoaderListener(this);
		UserSession.getKnowledgebaseManager().removeListener(this);
	}
	
	public void activate(View view) {
		mainPanel.replaceComponent((Component) currentView, (Component) view);
		currentView = view;
	}

	public void deactivate(View view) {
		
	}
	
	private void onLoadFromFile(){
		getWindow().addWindow(new UploadDialog());
	}
	
	private void onLoadFromURI(){
		getWindow().addWindow(new LoadFromURIDialog());
	}
	
	public void onLoadFromURI(String ontologyURI){
		getWindow().addWindow(new LoadFromURIDialog(ontologyURI));
	}
	
	private void onLoadFromRepository(final OntologyRepository repository){
		final String taskName = "Loading ontologies from " + repository.getName() + " repository...";
		new Thread(new BackgroundTask(getApplication(), taskName){
			@Override
			public void updateUIBefore() {
				onTaskStarted(taskName);
			}

			@Override
			public void runInBackground() {
				repository.initialize();
			}

			@Override
			public void updateUIAfter() {
				onTaskFinished();
				getWindow().addWindow(new OntologyRepositoryDialog(repository));
			}
			
		}).start();
	}
	
	private void onSaveOntology(){
		Knowledgebase kb = UserSession.getKnowledgebaseManager().getKnowledgebase();
		if(kb instanceof OWLOntologyKnowledgebase){
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				((OWLOntologyKnowledgebase) kb).getOntology().getOWLOntologyManager().saveOntology(((OWLOntologyKnowledgebase) kb).getOntology(),
						new RDFXMLOntologyFormat(), baos);
			} catch (OWLOntologyStorageException e) {
				e.printStackTrace();
			}
			final DynamicStreamResource streamResource = new DynamicStreamResource(baos.toByteArray(),
					"ontology.owl", DynamicStreamResource.MIME_TYPE_RDF_XML, getApplication());

			streamResource.setCacheTime(5 * 1000); // no cache (<=0) does not work
													// with IE8, (in milliseconds)
			getApplication().getMainWindow().open(streamResource, "_blank");
		} else if(kb instanceof SPARQLEndpointKnowledgebase){
			
		}
		
	}
	
	public void onSPARQLEndpoint(){
		getWindow().addWindow(new Connect2SPARQLEndpointDialog(this));
	}
	
	public void onSPARQLEndpoint(String endpointURL, String defaultGraphURI){
		getWindow().addWindow(new Connect2SPARQLEndpointDialog(this, endpointURL, defaultGraphURI));
	}
	
	private void onHelp(){
		ViewHandler.activateView(HelpView.class);
	}
	
	private void onDebugging(){
		Knowledgebase kb = UserSession.getKnowledgebaseManager().getKnowledgebase();
		if(kb instanceof OWLOntologyKnowledgebase){
			if(((OWLOntologyKnowledgebase) kb).isConsistent()){
				ViewHandler.activateView(DebuggingView.class);
			} else {
				ViewHandler.activateView(InconsistencyDebuggingView.class);
			}
		} else if(kb instanceof SPARQLEndpointKnowledgebase){
			ViewHandler.activateView(SPARQLDebuggingView.class);
		}
			
	}
	
	private void onLearning(){
		Knowledgebase kb = UserSession.getKnowledgebaseManager().getKnowledgebase();
		if(kb instanceof OWLOntologyKnowledgebase){
			ViewHandler.activateView(LearningView.class);
		} else if(kb instanceof SPARQLEndpointKnowledgebase){
			ViewHandler.activateView(EnrichmentView.class);
		}
	}
	
	private void onPatOMat(){
		ViewHandler.activateView(PatOMatView.class);
	}
	
	public void enableSPARQLMenus(boolean enable){
		debuggingMenuItem.setEnabled(enable);
		debugLogicalProblemsMenuItem.setEnabled(enable);
		enrichmentMenuItem.setEnabled(enable);
	}
	
	public void enableOntologyMenus(boolean enable){
		debuggingMenuItem.setEnabled(enable);
		debugLogicalProblemsMenuItem.setEnabled(enable);
		debugNamingProblemsMenuItem.setEnabled(enable);
		enrichmentMenuItem.setEnabled(enable);
	}
	
	public void enableDebugging(boolean enable){
		debuggingMenuItem.setEnabled(enable);
		debugLogicalProblemsMenuItem.setEnabled(enable);
		debugNamingProblemsMenuItem.setEnabled(enable);
	}
	
	public void enableEnrichment(boolean enable){
		enrichmentMenuItem.setEnabled(enable);
	}
	
	public void enableLogicalDebugging(boolean enable){
		debuggingMenuItem.setEnabled(enable);
		debugLogicalProblemsMenuItem.setEnabled(enable);
	}
	
	public void enablePatOMat(boolean enable){
		debugNamingProblemsMenuItem.setEnabled(enable);
	}
	
	@Override
	public void knowledgebaseChanged(Knowledgebase knowledgebase) {
		kbInfoPanel.removeAllComponents();
		String kbInfo = "<b>Knowledge base:</b>";
		if(knowledgebase instanceof SPARQLEndpointKnowledgebase){
			kbInfo += "SPARQL endpoint - URL: ";
		} else if(knowledgebase instanceof OWLOntologyKnowledgebase){
			kbInfo += "OWL ontology - ID: " + StringEscapeUtils.escapeHtml(((OWLOntologyKnowledgebase)knowledgebase).getOntology().getOntologyID().toString());
		}
		Label kbInfoLabel = new Label(kbInfo, Label.CONTENT_XHTML);
		kbInfoPanel.addComponent(kbInfoLabel);
	}
	
	@Override
	public void knowledgebaseStatusChanged(Knowledgebase knowledgebase) {
		((WelcomeView)ViewHandler.getViewItem(WelcomeView.class).getView()).refresh();
		enableActions(knowledgebase);
	}

	@Override
	public void knowledgebaseAnalyzed(Knowledgebase knowledgebase) {
		reset(knowledgebase);
		((WelcomeView)ViewHandler.getViewItem(WelcomeView.class).getView()).refresh();
		enableActions(knowledgebase);
		
		ViewHandler.activateView(WelcomeView.class);
	}
	
	private void enableActions(Knowledgebase knowledgebase){
		enableEnrichment(knowledgebase.canLearn());
		enableLogicalDebugging(knowledgebase.canDebug());
		if(knowledgebase instanceof OWLOntologyKnowledgebase){
			if(((OWLOntologyKnowledgebase) knowledgebase).isConsistent()){
				debuggingMenuItem.setEnabled(true);
				enablePatOMat(true);
			}
			saveKnowledgebaseMenuItem.setEnabled(true);
			
		} else if(knowledgebase instanceof SPARQLEndpointKnowledgebase){
			enablePatOMat(false);
			saveKnowledgebaseMenuItem.setEnabled(false);
		}
	}
	
	private void reset(Knowledgebase knowledgebase){
		if(knowledgebase instanceof OWLOntologyKnowledgebase){
			((DebuggingView)ViewHandler.getViewItem(DebuggingView.class).getView()).reset();
			((InconsistencyDebuggingView)ViewHandler.getViewItem(InconsistencyDebuggingView.class).getView()).reset();
			((PatOMatView)ViewHandler.getViewItem(PatOMatView.class).getView()).reset();
			((LearningView)ViewHandler.getViewItem(LearningView.class).getView()).reset();
		} else if(knowledgebase instanceof SPARQLEndpointKnowledgebase){
			((EnrichmentView)ViewHandler.getViewItem(EnrichmentView.class).getView()).reset();
			((SPARQLDebuggingView)ViewHandler.getViewItem(SPARQLDebuggingView.class).getView()).reset();
		}
	}

	@Override
	public void message(String message) {
		System.out.println(message);
	}
}
