package org.aksw.ore.manager;

import com.clarkparsia.modularity.IncremantalReasonerFactory;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.aksw.mole.ore.impact.AbstractImpactChecker;
import org.aksw.mole.ore.impact.ClassificationImpactChecker;
import org.aksw.mole.ore.impact.StructuralImpactChecker;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class ImpactManager {
	
	public interface ImpactManagerListener{
		void impactComputationStarted();
		void impactComputationFinished(Set<OWLOntologyChange> impact);
		void impactComputationCanceled();
	}
	
	private final Set<ImpactManagerListener> listeners = new CopyOnWriteArraySet<>();
	
	private static final Logger logger = Logger.getLogger(ImpactManager.class.getName());
	
	private AbstractImpactChecker structuralChecker;
	private AbstractImpactChecker classificationChecker;
	
	private Map<Set<OWLOntologyChange>, Set<OWLOntologyChange>> cache = new HashMap<>();
	
	private volatile boolean canceled = false;
	
	private Set<OWLOntologyChange> impact;
	
	public ImpactManager(OWLReasoner reasoner){
		structuralChecker = new StructuralImpactChecker(reasoner);
		classificationChecker = new ClassificationImpactChecker(IncremantalReasonerFactory.getInstance().createNonBufferingReasoner(reasoner.getRootOntology()));
	}
	
	public void computeImpact(final List<OWLOntologyChange> changes){
		logger.info("Computing impact...");
		fireImpactComputationStarted();
		new Thread(new Runnable() {
			public void run() {
				Set<OWLOntologyChange> impact = new HashSet<>();
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
		cache.put(new HashSet<>(changes), impact);
	}
	
	public boolean isRunning(){
		return !canceled && impact == null;
	}
	
	public Set<OWLOntologyChange> getImpact(List<OWLOntologyChange> changes){
		canceled = false;
		impact = null;
		fireImpactComputationStarted();
		impact = cache.get(new HashSet<>(changes));
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
		
		Set<OWLOntologyChange> impact = new ImpactManager(reasoner).getImpact(Collections.singletonList(ch));
		System.out.println(impact);
	}

}
