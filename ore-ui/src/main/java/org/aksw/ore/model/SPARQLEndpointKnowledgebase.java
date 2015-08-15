package org.aksw.ore.model;

import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class SPARQLEndpointKnowledgebase implements Knowledgebase{
	
	//we use this to store the applied changes and publish them later to the SPARQL endpoint or dump them as SPARUL
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	
	private SparqlEndpointKS endpoint;
	
	private SPARQLKnowledgebaseStats stats;
	
	public SPARQLEndpointKnowledgebase(SparqlEndpointKS endpoint) {
		this.endpoint = endpoint;
		
		manager = OWLManager.createOWLOntologyManager();
		try {
			ontology = manager.createOntology();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public boolean canLearn() {
		return true;
	}

	@Override
	public boolean canDebug() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.ore.model.Knowledgebase#canValidate()
	 */
	@Override
	public boolean canValidate() {
		return true;
	}
	
	public SparqlEndpointKS getEndpoint() {
		return endpoint;
	}
	
	/**
	 * Returns the ontology which is used to hold axioms which are added during the enrichment process.
	 * @return the ontology
	 */
	public OWLOntology getBaseOntology() {
		return ontology;
	}
	
	/**
	 * @param stats the stats to set
	 */
	public void setStats(SPARQLKnowledgebaseStats stats) {
		this.stats = stats;
	}
	
	/**
	 * @return the stats
	 */
	public SPARQLKnowledgebaseStats getStats() {
		return stats;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SPARQL Endpoint at " + endpoint.getEndpoint().getURL().toString();
	}
}
