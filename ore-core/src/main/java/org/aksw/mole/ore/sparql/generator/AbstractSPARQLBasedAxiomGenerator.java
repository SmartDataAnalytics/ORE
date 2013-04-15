package org.aksw.mole.ore.sparql.generator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;

import org.dllearner.core.AbstractAxiomLearningAlgorithm;
import org.dllearner.kb.LocalModelBasedSparqlEndpointKS;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;

public abstract class AbstractSPARQLBasedAxiomGenerator implements SPARQLBasedAxiomGenerator{
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractAxiomLearningAlgorithm.class);

	protected AxiomType axiomType;
	protected int limit = 1000;
	protected int cnt;
	protected SparqlEndpointKS ks;
	protected ExtractionDBCache cache;
	protected OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
	
	public AbstractSPARQLBasedAxiomGenerator(SparqlEndpointKS ks) {
		this.ks = ks;
	}
	
	public AbstractSPARQLBasedAxiomGenerator(SparqlEndpointKS ks, ExtractionDBCache cache) {
		this.ks = ks;
		this.cache = cache;
	}
	
	@Override
	public AxiomType getAxiomType() {
		return axiomType;
	}
	
	@Override
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	protected OWLOntology convert(Model model){
		OWLOntology ontology = null;
		ByteArrayOutputStream baos = null;
		ByteArrayInputStream bais = null;
		try {
			baos = new ByteArrayOutputStream();
			model.write(baos, "TURTLE");
			bais = new ByteArrayInputStream(baos.toByteArray());
			ontology = ontologyManager.loadOntologyFromOntologyDocument(bais);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		} finally {
			try {
				baos.close();
				bais.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ontology;
	}
	
	protected Model executeConstructQuery(String queryString) {
		return executeConstructQuery(QueryFactory.create(queryString));
	}
	
	protected Model executeConstructQuery(Query query) {
		Model model = null;
		logger.debug("Sending query\n{} ...", query);
		if(ks.isRemote()){
			SparqlEndpoint endpoint = ((SparqlEndpointKS) ks).getEndpoint();
			ExtractionDBCache cache = ks.getCache();
			if(cache != null){
				try {
					model = cache.executeConstructQuery(endpoint, query.toString());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} else {
				QueryEngineHTTP queryExecution = new QueryEngineHTTP(endpoint.getURL().toString(),
						query);
				queryExecution.setDefaultGraphURIs(endpoint.getDefaultGraphURIs());
				queryExecution.setNamedGraphURIs(endpoint.getNamedGraphURIs());
				try {
					model = queryExecution.execConstruct();
					logger.debug("Got " + model.size() + " triples.");
					return model;
				} catch (QueryExceptionHTTP e) {
					if(e.getCause() instanceof SocketTimeoutException){
						logger.warn("Got timeout");
					} else {
						logger.error("Exception executing query", e);
					}
					model = ModelFactory.createDefaultModel();
				}
			}
			
		} else {
			QueryExecution queryExecution = QueryExecutionFactory.create(query, ((LocalModelBasedSparqlEndpointKS)ks).getModel());
			model = queryExecution.execConstruct();
		}
		return model;
	}


}
