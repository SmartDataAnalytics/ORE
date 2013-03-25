package org.aksw.mole.ore.widget;

import org.aksw.mole.ore.KnowledgebaseManager;
import org.aksw.mole.ore.OREApplication;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.model.OWLOntologyKnowledgebase;
import org.aksw.mole.ore.repository.OntologyRepository;
import org.aksw.mole.ore.repository.OntologyRepositoryEntry;
import org.aksw.mole.ore.task.BackgroundTask;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.github.wolfie.refresher.Refresher;
import com.github.wolfie.refresher.Refresher.RefreshListener;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class OntologyRepositoryDialog extends Window {

	private OntologyRepository repository;

	private Button okButton;
	private Button cancelButton;
	
	private Table table;
	
	private Refresher refresher = new Refresher();
	private OWLOntology ontology;

	public OntologyRepositoryDialog(OntologyRepository repository) {
		this.repository = repository;

		setCaption(repository.getName());
		setWidth("1200px");
		setHeight("800px");
		center();
		initUI();
		
		addComponent(refresher);
		refresher.addListener(new RefreshListener() {
			
			@Override
			public void refresh(Refresher source) {
				if(ontology != null){
					refresher.setRefreshInterval(0);
					onOntologyLoaded();
				}
			}
		});
	}
	
	private void onOntologyLoaded(){
		((OREApplication)getApplication()).getAppView().onTaskFinished();
		UserSession.getKnowledgebaseManager().setKnowledgebase(new OWLOntologyKnowledgebase(ontology));
		close();
	}

	private void initUI() {
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		setContent(main);

		// table
		table = new Table();
		// table.setHeight("200px");
		table.setPageLength(0);
		table.setSelectable(true);
		table.setImmediate(true);
		table.addContainerProperty("name", String.class, null);
		table.addContainerProperty("uri", String.class, null);
		table.addListener(new Property.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				okButton.setEnabled(event.getProperty().getValue() != null);
			}
		});
		for (OntologyRepositoryEntry entry : repository.getEntries()) {
			table.addItem(new Object[] { entry.getOntologyShortName(), entry.getPhysicalURI() }, entry);
		}

		// buttons
		okButton = new Button("Ok");
		okButton.addListener(new Button.ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				final String taskName = "Loading ontology...";
				final OWLOntologyManager man = OWLManager.createOWLOntologyManager();
				
				man.addOntologyLoaderListener(UserSession.getKnowledgebaseManager());
					new Thread(new BackgroundTask(getApplication(), taskName){
						@Override
						public void updateUIBefore() {
							((OREApplication)getApplication()).getAppView().onTaskStarted(taskName);
						}

						@Override
						public void runInBackground() {
							OntologyRepositoryEntry entry = (OntologyRepositoryEntry) table.getValue();
							try {
								ontology = man.loadOntology(IRI.create(entry.getPhysicalURI()));
							} catch (OWLOntologyCreationException e) {
								e.printStackTrace();
								getApplication().getMainWindow().showNotification("Error loading ontology", "Could not load the selected ontology.", Notification.TYPE_ERROR_MESSAGE);
							}
						}

						@Override
						public void updateUIAfter() {
							
						}
						
					}).start();
					
					
				
			}
		});
		okButton.setEnabled(false);
		cancelButton = new Button("Cancel");
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

		VerticalLayout l = new VerticalLayout();
		l.setSizeUndefined();
		l.addComponent(table);
		Panel p = new Panel();
		p.setSizeFull();
		p.setContent(l);
		main.addComponent(p);
		main.setExpandRatio(p, 1f);
		main.addComponent(buttonLayout);
	}
}
