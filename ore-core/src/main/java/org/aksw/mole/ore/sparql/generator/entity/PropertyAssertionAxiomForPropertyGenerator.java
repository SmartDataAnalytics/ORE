package org.aksw.mole.ore.sparql.generator.entity;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedEntityRelatedAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.OWLProperty;

import com.hp.hpl.jena.query.ParameterizedSparqlString;

public class PropertyAssertionAxiomForPropertyGenerator extends AbstractSPARQLBasedEntityRelatedAxiomGenerator<OWLProperty>{
	

	public PropertyAssertionAxiomForPropertyGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		queryTemplate = new ParameterizedSparqlString(
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?s ?var ?o. ?var a ?type.} " +
				"WHERE " +
				"{?s ?var ?o. ?var a ?type.}"
				);
	}
	
	
}
