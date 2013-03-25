package org.aksw.mole.ore.widget;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;

import org.aksw.mole.ore.ExplanationManager2;
import org.aksw.mole.ore.view.ApplicationView;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.kb.OWLAPIOntology;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import com.clarkparsia.owlapi.explanation.PelletExplanation;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class OntologyInitializationDialog extends Window implements ReasonerProgressMonitor{
	
	private ProgressIndicator progressBar;
	private Label statusLabel;
	
	private Button okButton;
	private Button cancelButton;
	
	private OWLReasoner reasoner;
	
	private ApplicationView view;
	
	public OntologyInitializationDialog(ApplicationView view) {
		super("Initializing...");
		setModal(true);
		setWidth("300px");
		
		this.view = view;
		
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setSpacing(true);
		mainLayout.setMargin(true);
		setContent(mainLayout);
		
		progressBar = new ProgressIndicator();
		progressBar.setPollingInterval(100);
		progressBar.setIndeterminate(false);
		progressBar.setHeight(null);
		mainLayout.addComponent(progressBar);
		
		statusLabel = new Label(" ");
		statusLabel.setHeight(null);
		mainLayout.addComponent(statusLabel);
		
		HorizontalLayout buttonlayout = new HorizontalLayout();
		mainLayout.addComponent(buttonlayout);
		
		cancelButton = new Button("Cancel");
		cancelButton.addListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onCancelButtonPressed();
			}
		});
		buttonlayout.addComponent(cancelButton);
		
		okButton = new Button("Ok");
		okButton.addListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onOkButtonPressed();
			}
		});
		okButton.setEnabled(false);
		buttonlayout.addComponent(okButton);
		
		
	}
	
	@Override
	public void reasonerTaskBusy() {
		progressBar.setIndeterminate(true);
	}

	@Override
	public void reasonerTaskProgressChanged(int value, int max) {
		progressBar.setValue((double)value/max);
	}

	@Override
	public void reasonerTaskStarted(String task) {
		progressBar.setValue(0);
		statusLabel.setValue(task + "...");
	}

	@Override
	public void reasonerTaskStopped() {
		progressBar.setIndeterminate(false);
		progressBar.setValue(1);
	}
	
	private void onOkButtonPressed(){
		close();
	}
	
	private void onCancelButtonPressed(){
		reasoner.interrupt();
	}
	
	public static void main(String[] args) throws Exception{
		PelletExplanation.setup();
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = man.loadOntology(IRI.create("http://dl-learner.svn.sourceforge.net/viewvc/dl-learner/trunk/examples/swore/swore.rdf?revision=2312"));
		OWLReasonerConfiguration conf = new SimpleConfiguration(new ConsoleProgressMonitor());
		PelletReasoner reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(ontology, conf);
		OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY, InferenceType.CLASS_ASSERTIONS);
		man.addOntologyChangeListener(reasoner);
		System.out.println(reasoner.isConsistent());
		
		KnowledgeSource ks = new OWLAPIOntology(ontology);
		org.dllearner.reasoning.PelletReasoner pReasoner = new org.dllearner.reasoning.PelletReasoner(Collections.singleton(ks));
		pReasoner.init();
		
		OWLOntology newOnt = pReasoner.getOWLAPIOntologies();
		man.saveOntology(newOnt, new RDFXMLOntologyFormat(), new FileOutputStream(new File("inc.owl")));
		System.out.println(reasoner.getUnsatisfiableClasses());
		ExplanationManager2 expMan = new ExplanationManager2(pReasoner.getReasoner(), PelletReasonerFactory.getInstance());
		System.out.println(expMan.getInconsistencyExplanations());
//		System.out.println(expMan.getRootUnsatisfiableClasses());
		System.out.println(ontology.getLogicalAxiomCount());
		OWLAxiom ax = factory.getOWLSubClassOfAxiom(factory.getOWLClass(IRI.create("http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#Koala")),
				factory.getOWLClass(IRI.create("http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#Marsupials")));
		man.removeAxiom(ontology, ax);
		System.out.println(ontology.getLogicalAxiomCount());
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		System.out.println(reasoner.getUnsatisfiableClasses());
	}
	
}
