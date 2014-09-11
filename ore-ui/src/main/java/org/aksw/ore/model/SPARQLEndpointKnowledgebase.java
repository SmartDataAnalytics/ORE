package org.aksw.ore.model;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.extra.CacheBackend;
import org.aksw.jena_sparql_api.cache.extra.CacheFrontend;
import org.aksw.jena_sparql_api.cache.extra.CacheFrontendImpl;
import org.aksw.jena_sparql_api.cache.h2.CacheCoreH2;
import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
import org.aksw.ore.OREConfiguration;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class SPARQLEndpointKnowledgebase implements Knowledgebase{
	
	//we use this to store the applied changes and publish them later to the SPARQL endpoint or dump them as SPARUL
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	
	private SparqlEndpoint endpoint;
	private CacheFrontend cache;
	
	private SPARQLKnowledgebaseStats stats;
	
	public SPARQLEndpointKnowledgebase(SparqlEndpoint endpoint) {
		this.endpoint = endpoint;
		
		manager = OWLManager.createOWLOntologyManager();
		try {
			ontology = manager.createOntology();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
		//create the cache
		long timeToLive = TimeUnit.DAYS.toMillis(30);
		cache = CacheUtilsH2.createCacheFrontend(OREConfiguration.getCacheDirectory(), true, timeToLive);
		
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
	
	public SparqlEndpoint getEndpoint() {
		return endpoint;
	}
	
	/**
	 * @return the cache
	 */
	public CacheFrontend getCache() {
		return cache;
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
		return "SPARQL Endpoint at " + endpoint.getURL().toString();
	}
}
