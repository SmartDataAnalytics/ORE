package org.aksw.mole.ore.validation.constraint;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheFrontend;
import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import com.google.common.collect.Maps;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class OWLAxiomConstraintReportCreator {
	
	private static final Logger logger = Logger.getLogger(OWLAxiomConstraintReportCreator.class);
	
	private SparqlEndpoint endpoint;
	private ExtractionDBCache cache;

	public OWLAxiomConstraintReportCreator(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		this.endpoint = endpoint;
		this.cache = cache;
	}
	
	public OWLAxiomConstraintReportCreator(SparqlEndpoint endpoint) {
		this(endpoint, null);
	}
	
	public Map<OWLAxiom, Model> validate(SparqlEndpoint endpoint, OWLOntology constraintAxioms){
		return validate(endpoint, constraintAxioms, null);
	}
	
	public Map<OWLAxiom, Model> validate(SparqlEndpoint endpoint, OWLOntology constraintAxioms, String resourceNamespace){
		return validate(endpoint, constraintAxioms.getLogicalAxioms(), resourceNamespace);
	}
	
	public Map<OWLAxiom, Model> validate(SparqlEndpoint endpoint, Collection<? extends OWLAxiom> constraintAxioms){
		return validate(endpoint, constraintAxioms, null);
	}
	
	public Map<OWLAxiom, Model> validate(SparqlEndpoint endpoint, Collection<? extends OWLAxiom> constraintAxioms, String resourceNamespace){
		Map<OWLAxiom, Model> constraint2Violations = Maps.newTreeMap();
		OWLAxiomConstraintToSPARQLConstructConverter converter = new OWLAxiomConstraintToSPARQLConstructConverter();
		for(OWLAxiom axiom : constraintAxioms){
			Query query = converter.asQuery("?x", axiom, resourceNamespace);
			logger.info("Validating constraint\n" + axiom + "\nby executing\n" + query);
			Model model = executeConstructQuery(query);
			constraint2Violations.put(axiom, model);
		}
		return constraint2Violations;
	}
	
	private ResultSet executeSelectQuery(Query query) {
		ResultSet rs;
		if(cache != null){
			rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, query.toString()));
		} else {
			QueryEngineHTTP queryExecution = new QueryEngineHTTP(endpoint.getURL().toString(),
					query);
			queryExecution.setDefaultGraphURIs(endpoint.getDefaultGraphURIs());
			queryExecution.setNamedGraphURIs(endpoint.getNamedGraphURIs());
			rs = queryExecution.execSelect();
		}
		return rs;
	}
	
	protected Model executeConstructQuery(String queryString) {
		return executeConstructQuery(QueryFactory.create(queryString));
	}
	
	private Model executeConstructQuery(Query query){
		QueryExecutionFactory qef = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
			long timeToLive = TimeUnit.DAYS.toMillis(30);
			CacheFrontend cacheFrontend = CacheUtilsH2.createCacheFrontend("cache", true, timeToLive);
			qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);
		qef = new QueryExecutionFactoryPaginated(qef, 10000);
		QueryExecution qe = qef.createQueryExecution(query);
		Model model = qe.execConstruct();
		return model;
	}
}
