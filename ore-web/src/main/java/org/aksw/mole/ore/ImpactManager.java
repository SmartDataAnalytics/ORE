package org.aksw.mole.ore;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.aksw.mole.ore.impact.AbstractImpactChecker;
import org.aksw.mole.ore.impact.ClassificationImpactChecker;
import org.aksw.mole.ore.impact.StructuralImpactChecker;
import org.aksw.mole.ore.task.ImpactComputationTask;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.clarkparsia.modularity.IncrementalClassifier;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class ImpactManager {
	
	public interface ImpactManagerListener{
		void impactComputationStarted();
		void impactComputationFinished(Set<OWLOntologyChange> impact);
		void impactComputationCanceled();
	}
	
	private final Set<ImpactManagerListener> listeners = new CopyOnWriteArraySet<ImpactManager.ImpactManagerListener>();
	
	private static final Logger logger = Logger.getLogger(ImpactManager.class.getName());
	
	private AbstractImpactChecker structuralChecker;
	private AbstractImpactChecker classificationChecker;
	
	private Map<Set<OWLOntologyChange>, Set<OWLOntologyChange>> cache = new HashMap<Set<OWLOntologyChange>, Set<OWLOntologyChange>>();
	
	private volatile boolean canceled = false;
	
	private ImpactComputationTask currentTask;
	private Set<OWLOntologyChange> impact;
	
	public ImpactManager(OWLReasoner reasoner){
		structuralChecker = new StructuralImpactChecker(reasoner);
		classificationChecker = new ClassificationImpactChecker(new IncrementalClassifier(reasoner.getRootOntology()));
	}
	
	public void computeImpact(final List<OWLOntologyChange> changes){
		logger.info("Computing impact...");
		fireImpactComputationStarted();
		new Thread(new Runnable() {
			public void run() {
				Set<OWLOntologyChange> impact = new HashSet<OWLOntologyChange>();
				if(!canceled){
					impact.addAll(structuralChecker.getImpact(changes));
				}
				if(!canceled){
					impact.addAll(classificationChecker.getImpact(changes));
				}
				ImpactManager.this.impact = impact;
			}
		}).start();
		while(isRunning()){
			
		}
		logger.info("Done.");
		cache.put(new HashSet<OWLOntologyChange>(changes), impact);
	}
	
	public boolean isRunning(){
		return !canceled && impact == null;
	}
	
	public Set<OWLOntologyChange> getImpact(List<OWLOntologyChange> changes){
		canceled = false;
		impact = null;
		fireImpactComputationStarted();
		impact = cache.get(new HashSet<OWLOntologyChange>(changes));
		if(impact == null){
			computeImpact(changes);
		}
		fireImpactComputationFinished(impact);
		return impact;
	}
	
	public final void cancel(){
		canceled = true;
		structuralChecker.cancel();
		classificationChecker.cancel();
		fireImpactComputationCanceled();
	}
	
	public boolean isCanceled() {
		return canceled;
	}
	
	public void reset(){
		cache.clear();
	}
	
	public void addListener(ImpactManagerListener listener){
		listeners.add(listener);
	}
	
	public void removeListener(ImpactManagerListener listener){
		listeners.remove(listener);
	}
	
	private void fireImpactComputationStarted(){
		for(ImpactManagerListener l : listeners){
			l.impactComputationStarted();
		}
	}
	
	private void fireImpactComputationFinished(Set<OWLOntologyChange> impact){
		for(ImpactManagerListener l : listeners){
			l.impactComputationFinished(impact);
		}
	}
	
	private void fireImpactComputationCanceled(){
		for(ImpactManagerListener l : listeners){
			l.impactComputationCanceled();
		}
	}
	
	public static void main(String[] args) throws Exception {
//		Logger.getLogger(ClassificationImpactChecker.class).setLevel(Level.DEBUG);
		String ontologyURL = "http://protege.stanford.edu/plugins/owl/owl-library/koala.owl";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = man.getOWLDataFactory();
		OWLOntology ontology = man.loadOntology(IRI.create(ontologyURL));
		OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		  
		OWLOntologyChange ch = new RemoveAxiom(ontology, 
				dataFactory.getOWLSubClassOfAxiom(
						dataFactory.getOWLClass(IRI.create("http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#Koala")),
						dataFactory.getOWLClass(IRI.create("http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#Marsupials"))));
		
		new ImpactManager(reasoner).getImpact(Collections.singletonList(ch));
	}

}
