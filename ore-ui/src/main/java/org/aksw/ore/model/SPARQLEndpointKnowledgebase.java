package org.aksw.ore.model;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.extra.CacheCoreEx;
import org.aksw.jena_sparql_api.cache.extra.CacheCoreH2;
import org.aksw.jena_sparql_api.cache.extra.CacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheExImpl;
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
	private CacheEx cache;
	
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
		try {
			long timeToLive = TimeUnit.DAYS.toMillis(30);
			CacheCoreEx cacheBackend = CacheCoreH2.create(true, OREConfiguration.getCacheDirectory(), "sparql", timeToLive, true);
			cache = new CacheExImpl(cacheBackend);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
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
	
	public SparqlEndpoint getEndpoint() {
		return endpoint;
	}
	
	/**
	 * @return the cache
	 */
	public CacheEx getCache() {
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
