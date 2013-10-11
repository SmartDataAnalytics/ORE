package org.aksw.ore.manager;

import java.util.ArrayList;
import java.util.List;

import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.ore.ORESession;
import org.aksw.ore.model.Knowledgebase;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.aksw.ore.model.SPARQLEndpointKnowledgebase;
import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.hp.hpl.jena.query.QueryExecution;

public class KnowledgebaseManager implements OWLOntologyLoaderListener{
	
	
	private static final Logger logger = Logger.getLogger(KnowledgebaseManager.class.getName());
	
	public interface KnowledgebaseLoadingListener{
		void knowledgebaseChanged(Knowledgebase knowledgebase);
		void knowledgebaseAnalyzed(Knowledgebase knowledgebase);
		void knowledgebaseStatusChanged(Knowledgebase knowledgebase);
		void message(String message);
	}
	
	private Knowledgebase knowledgebase;
	private OWLReasoner reasoner;
	
	private List<KnowledgebaseLoadingListener> listeners;
	
	public KnowledgebaseManager() {
		listeners = new ArrayList<KnowledgebaseLoadingListener>();
	}
	
	public void setKnowledgebase(Knowledgebase knowledgebase) {
		logger.debug("Set knowledgebase to " + knowledgebase);
		this.knowledgebase = knowledgebase;
//		analyzeKnowledgebase();
		fireKnowledgebaseChanged();
	}
	
	public Knowledgebase getKnowledgebase() {
		return knowledgebase;
	}
	
	public void analyzeKnowledgebase(){
		if(knowledgebase instanceof OWLOntologyKnowledgebase){
			OWLOntology ontology = ((OWLOntologyKnowledgebase) knowledgebase).getOntology();
			
			reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(ontology);
			((OWLOntologyKnowledgebase) knowledgebase).setReasoner(reasoner);
			message("Checking consistency...");
			if(reasoner.isConsistent()){
				((OWLOntologyKnowledgebase) knowledgebase).setConsistent(true);
				message("Classifying ontology...");
				reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
				message("Realizing ontology...");
				reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
				if(!reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().isEmpty()){
					((OWLOntologyKnowledgebase) knowledgebase).setCoherent(false);
				}
			} else {
				((OWLOntologyKnowledgebase) knowledgebase).setConsistent(false);
			}
		} else if(knowledgebase instanceof SPARQLEndpointKnowledgebase){
			
		}
//		ORESession.setReasoner(reasoner);
//		ORESession.initialize(knowledgebase);
		
		fireKnowledgebaseAnalyzed();
		
		ORESession.initialize(knowledgebase);
	}
	
	public void updateStatus(){
		if(knowledgebase instanceof OWLOntologyKnowledgebase){
			((OWLOntologyKnowledgebase) knowledgebase).updateStatus();
			fireKnowledgebaseStatusChanged();
		}
	}
	
	public void addListener(KnowledgebaseLoadingListener l){//System.out.println("Add listener " + l);
		synchronized(listeners){
			listeners.add(l);
		}
	}
	
	public void removeListener(KnowledgebaseLoadingListener l){//System.out.println("Remove listener " + l);
		synchronized(listeners){
			listeners.remove(l);
		}
	}
	
	private void message(String message){
		for (KnowledgebaseLoadingListener l : new ArrayList<KnowledgebaseLoadingListener>(listeners)) {
			l.message(message);
		}
	}
	
	private void fireKnowledgebaseChanged(){//System.out.println("Fire KB changed");
		synchronized(listeners){
			for (KnowledgebaseLoadingListener l : new ArrayList<KnowledgebaseLoadingListener>(listeners)) {
				l.knowledgebaseChanged(knowledgebase);
			}
		}
	}
	
	private void fireKnowledgebaseAnalyzed(){//System.out.println("Fire KB analyzed");
		synchronized (listeners) {
			for (KnowledgebaseLoadingListener l : new ArrayList<KnowledgebaseLoadingListener>(listeners)) {
				l.knowledgebaseAnalyzed(knowledgebase);
			}
		}
	}
	
	private void fireKnowledgebaseStatusChanged(){
		for (KnowledgebaseLoadingListener l : listeners) {
			l.knowledgebaseStatusChanged(knowledgebase);
		}
	}

	@Override
	public void startedLoadingOntology(LoadingStartedEvent event) {
		if(!event.isImported()){
			
		} else {
			message("Loading import " + event.getDocumentIRI() + "...");
		}
		
	}

	@Override
	public void finishedLoadingOntology(LoadingFinishedEvent event) {
		if(!event.isImported()){
			message("...loaded ontology");
		} else {
			message("...loaded import");
		}
	}
	
	public boolean isOnline(SparqlEndpoint endpoint){
		try {
			QueryExecutionFactoryHttp f = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
			QueryExecution qe = f.createQueryExecution("SELECT * WHERE {?s ?p ?o.} LIMIT 1");
			qe.execSelect();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	

}
