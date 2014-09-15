package org.aksw.mole.ore.validation.constraint;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class OWLAxiomConstraintValidator {
	
	private static final Logger logger = Logger.getLogger(OWLAxiomConstraintValidator.class);
	
	private SparqlEndpoint endpoint;
	private ExtractionDBCache cache;

	public OWLAxiomConstraintValidator(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		this.endpoint = endpoint;
		this.cache = cache;
	}
	
	public OWLAxiomConstraintValidator(SparqlEndpoint endpoint) {
		this(endpoint, null);
	}
	
	public Multimap<OWLAxiom, String> validate(SparqlEndpoint endpoint, OWLOntology constraintAxioms){
		return validate(endpoint, constraintAxioms.getLogicalAxioms());
	}
	
	public Multimap<OWLAxiom, String> validate(SparqlEndpoint endpoint, Collection<? extends OWLAxiom> constraintAxioms){
		Multimap<OWLAxiom, String> constraint2ViolatingResources = HashMultimap.create();
		OWLAxiomConstraintToSPARQLConverter converter = new OWLAxiomConstraintToSPARQLConverter();
		for(OWLAxiom axiom : constraintAxioms){
			Query query = converter.asQuery("?x", axiom);
			logger.info("Validating constraint\n" + axiom + "\nby executing\n" + query);
			ResultSet rs = executeSelectQuery(query);
			QuerySolution qs;
			RDFNode node;
			while(rs.hasNext()){
				qs = rs.next();
				node = qs.get("x");
				if(node.isURIResource()){
					constraint2ViolatingResources.put(axiom, node.asResource().getURI());
				}
			}
		}
		return constraint2ViolatingResources;
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
}
