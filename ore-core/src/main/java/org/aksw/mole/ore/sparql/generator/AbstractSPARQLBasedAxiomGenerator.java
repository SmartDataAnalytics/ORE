package org.aksw.mole.ore.sparql.generator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheCoreEx;
import org.aksw.jena_sparql_api.cache.extra.CacheCoreH2;
import org.aksw.jena_sparql_api.cache.extra.CacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheExImpl;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.apache.jena.riot.Lang;
import org.dllearner.core.AbstractAxiomLearningAlgorithm;
import org.dllearner.kb.LocalModelBasedSparqlEndpointKS;
import org.dllearner.kb.SparqlEndpointKS;
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
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;

public abstract class AbstractSPARQLBasedAxiomGenerator implements SPARQLBasedAxiomGenerator{
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractAxiomLearningAlgorithm.class);

	protected AxiomType axiomType;
	protected int limit = 1000;
	protected int cnt;
	protected SparqlEndpointKS ks;
	protected OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
	
	protected QueryExecutionFactory qef;
	
	public AbstractSPARQLBasedAxiomGenerator(SparqlEndpointKS ks) {
		this(ks ,null);
	}
	
	public AbstractSPARQLBasedAxiomGenerator(SparqlEndpointKS ks, String cacheDirectory) {
		this.ks = ks;
		
		if(ks.isRemote()){
			SparqlEndpoint endpoint = ks.getEndpoint();
			qef = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
			if(cacheDirectory != null){
				try {
					long timeToLive = TimeUnit.DAYS.toMillis(30);
					CacheCoreEx cacheBackend = CacheCoreH2.create(cacheDirectory, timeToLive, true);
					CacheEx cacheFrontend = new CacheExImpl(cacheBackend);
					qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
//			qef = new QueryExecutionFactoryPaginated(qef, 10000);
			
		} else {
			qef = new QueryExecutionFactoryModel(((LocalModelBasedSparqlEndpointKS)ks).getModel());
		}
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
			model.write(baos, "RDF/XML");
			bais = new ByteArrayInputStream(baos.toByteArray());
			try {
				model.write(new FileOutputStream("error.ttl"), "RDF/XML");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
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
		QueryExecution qe = null;
		try {
			qe = qef.createQueryExecution(query);
			model = qe.execConstruct();
			logger.debug("Got " + model.size() + " triples.");
			return model;
		} catch (QueryExceptionHTTP e) {
			if(e.getCause() instanceof SocketTimeoutException){
				logger.warn("Got timeout");
			} else {
				logger.error("Exception executing query", e);
			}
			model = ModelFactory.createDefaultModel();
		} finally {
			if(qe != null){
				qe.close();
			}
		}
		return model;
	}
	
	@Override
	public int compareTo(AxiomGenerator other) {
		return getClass().getName().compareTo(other.getClass().getName());
	}


}
