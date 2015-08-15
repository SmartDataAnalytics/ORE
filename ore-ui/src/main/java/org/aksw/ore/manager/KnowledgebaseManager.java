package org.aksw.ore.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.ore.ORESession;
import org.aksw.ore.model.Knowledgebase;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.aksw.ore.model.SPARQLEndpointKnowledgebase;
import org.aksw.ore.model.SPARQLKnowledgebaseStats;
import org.apache.log4j.Logger;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class KnowledgebaseManager implements OWLOntologyLoaderListener{
	
	private static final Logger logger = Logger.getLogger(KnowledgebaseManager.class.getName());
	
	public interface KnowledgebaseLoadingListener{
		void knowledgebaseChanged(Knowledgebase knowledgebase);
		void knowledgebaseAnalyzed(Knowledgebase knowledgebase);
		void knowledgebaseStatusChanged(Knowledgebase knowledgebase);
		void message(String message);
		void knowledgebaseModified(Set<OWLOntologyChange> changes);
	}
	
	private Knowledgebase knowledgebase;
	private OWLReasoner reasoner;
	
	private Set<OWLOntologyChange> changes;
	
	private final List<KnowledgebaseLoadingListener> listeners = new ArrayList<KnowledgebaseLoadingListener>();
	
	public void setKnowledgebase(Knowledgebase knowledgebase) {
		logger.debug("Set knowledgebase to " + knowledgebase);
		this.knowledgebase = knowledgebase;
		changes = Sets.newLinkedHashSet();
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
			analyzeSPARQLEndpoint((SPARQLEndpointKnowledgebase) knowledgebase);
		}
//		ORESession.setReasoner(reasoner);
		ORESession.initialize(knowledgebase);
		
		fireKnowledgebaseAnalyzed();
		
//		ORESession.initialize(knowledgebase);
	}
	
	/**
	 * Compute some statistics about the SPARQL endpoint.
	 * TODO This should be made configurable in the UI as it is probably based on SPARQL queries and, thus,
	 * might be also a question of performance.
	 */
	private void analyzeSPARQLEndpoint(SPARQLEndpointKnowledgebase kb){
		SparqlEndpointKS ks = kb.getEndpoint();
		QueryExecutionFactory qef = ks.getQueryExecutionFactory();
		
//		//get number of OWL classes
//		String query = "SELECT (COUNT(?s) AS ?cnt) WHERE {?s a <http://www.w3.org/2002/07/owl#Class>.}";
//		int clsCnt = qef.createQueryExecution(query).execSelect().next().getLiteral("cnt").getInt();
//		//get number of OWL object properties
//		query = "SELECT (COUNT(?s) AS ?cnt) WHERE {?s a <http://www.w3.org/2002/07/owl#ObjectProperty>.}";
//		int opCnt = qef.createQueryExecution(query).execSelect().next().getLiteral("cnt").getInt();
//		//get number of OWL data properties
//		query = "SELECT (COUNT(?s) AS ?cnt) WHERE {?s a <http://www.w3.org/2002/07/owl#DatatypeProperty>.}";
//		int dpCnt = qef.createQueryExecution(query).execSelect().next().getLiteral("cnt").getInt();
		
		//pre load entities
		//get OWL classes
		message("Loading classes...");
		String query = "SELECT ?s WHERE {?s a <http://www.w3.org/2002/07/owl#Class>.}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		Set<String> classes = asSet(rs, "s");
		
		//get OWL object properties
		message("Loading object properties...");
		query = "SELECT ?s WHERE {?s a <http://www.w3.org/2002/07/owl#ObjectProperty>.}";
		qe = qef.createQueryExecution(query);
		rs = qe.execSelect();
		Set<String> objectProperties = asSet(rs, "s");
		
		//get OWL data properties
		message("Loading data properties...");
		query = "SELECT ?s WHERE {?s a <http://www.w3.org/2002/07/owl#DatatypeProperty>.}";
		qe = qef.createQueryExecution(query);
		rs = qe.execSelect();
		Set<String> dataProperties = asSet(rs, "s");
		
		qe.close();
		
		kb.setStats(new SPARQLKnowledgebaseStats(classes, objectProperties, dataProperties));
	}
	
	private Set<String> asSet(ResultSet rs, String targetVar){
		Set<String> result = new TreeSet<String>();
		
		while(rs.hasNext()){
			QuerySolution qs = rs.next();
			result.add(qs.get(targetVar).toString());
		}
		
		return result;
	}
	
	public void addChange(OWLOntologyChange change){
		changes.add(change);
		fireKnowledgebaseModified();
	}
	
	public void removeChange(OWLOntologyChange change){
		changes.remove(change);
		fireKnowledgebaseModified();
	}
	
	public void addChanges(Collection<OWLOntologyChange> changes){
		this.changes.addAll(changes);
		fireKnowledgebaseModified();
	}
	
	/**
	 * @return the changes
	 */
	public Set<OWLOntologyChange> getChanges() {
		return changes;
	}
	
	public void updateStatus(){
		if(knowledgebase instanceof OWLOntologyKnowledgebase){
			((OWLOntologyKnowledgebase) knowledgebase).updateStatus();
			fireKnowledgebaseStatusChanged();
		}
	}
	
	public void addListener(KnowledgebaseLoadingListener l){
		synchronized(listeners){
			listeners.add(l);
		}
	}
	
	public void removeListener(KnowledgebaseLoadingListener l){
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
		logger.info("Knowledge base changed.");
		synchronized(listeners){
			for (KnowledgebaseLoadingListener l : new ArrayList<KnowledgebaseLoadingListener>(listeners)) {
				l.knowledgebaseChanged(knowledgebase);
			}
		}
	}
	
	private void fireKnowledgebaseAnalyzed(){//System.out.println("Fire KB analyzed");
		logger.info("Knowledge base analyzed.");
		synchronized (listeners) {
			for (KnowledgebaseLoadingListener l : new ArrayList<KnowledgebaseLoadingListener>(listeners)) {
				l.knowledgebaseAnalyzed(knowledgebase);
			}
		}
	}
	
	private void fireKnowledgebaseStatusChanged(){
		logger.info("Knowledge base status changed.");
		for (KnowledgebaseLoadingListener l : listeners) {
			l.knowledgebaseStatusChanged(knowledgebase);
		}
	}
	
	private void fireKnowledgebaseModified(){
		logger.info("Knowledge base modified.");
		for (KnowledgebaseLoadingListener l : listeners) {
			l.knowledgebaseModified(changes);
		}
	}

	@Override
	public void startedLoadingOntology(LoadingStartedEvent event) {
		if(event.isImported()){
			logger.info("Loading import from " + event.getDocumentIRI());
            message("Loading import " + event.getDocumentIRI() + "...");
		}
	}

	@Override
	public void finishedLoadingOntology(LoadingFinishedEvent event) {
		if(!event.isImported()){
			message("...loaded ontology");
			logger.info("Loaded ontology");
		} else {
			message("...loaded import");
			logger.info("Loaded import " + event.getDocumentIRI());
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
