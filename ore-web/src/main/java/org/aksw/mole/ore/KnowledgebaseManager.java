package org.aksw.mole.ore;

import java.util.ArrayList;
import java.util.List;

import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.mole.ore.model.Knowledgebase;
import org.aksw.mole.ore.model.OWLOntologyKnowledgebase;
import org.aksw.mole.ore.model.SPARQLEndpointKnowledgebase;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.hp.hpl.jena.query.QueryExecution;

public class KnowledgebaseManager implements OWLOntologyLoaderListener{
	
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
		this.knowledgebase = knowledgebase;
		fireKnowledgebaseChanged();
		analyzeKnowledgebase();
	}
	
	public Knowledgebase getKnowledgebase() {
		return knowledgebase;
	}
	
	private void analyzeKnowledgebase(){
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
		UserSession.setReasoner(reasoner);
		UserSession.initialize(knowledgebase);
		
		fireKnowledgebaseAnalyzed();
	}
	
	public void updateStatus(){
		if(knowledgebase instanceof OWLOntologyKnowledgebase){
			((OWLOntologyKnowledgebase) knowledgebase).updateStatus();
			fireKnowledgebaseStatusChanged();
		}
	}
	
	public void addListener(KnowledgebaseLoadingListener l){
		listeners.add(l);
	}
	
	public void removeListener(KnowledgebaseLoadingListener l){
		listeners.remove(l);
	}
	
	private void message(String message){
		for (KnowledgebaseLoadingListener l : listeners) {
			l.message(message);
		}
	}
	
	private void fireKnowledgebaseChanged(){
		for (KnowledgebaseLoadingListener l : listeners) {
			l.knowledgebaseChanged(knowledgebase);
		}
	}
	
	private void fireKnowledgebaseAnalyzed(){
		for (KnowledgebaseLoadingListener l : listeners) {
			l.knowledgebaseAnalyzed(knowledgebase);
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
