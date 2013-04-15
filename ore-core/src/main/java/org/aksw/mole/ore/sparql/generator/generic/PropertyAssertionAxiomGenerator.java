package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class PropertyAssertionAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public PropertyAssertionAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?s ?p ?o. ?p a ?type} " +
				"WHERE " +
//				"{?s ?p ?o. OPTIONAL{?p a ?type. FILTER(?type=owl:ObjectProperty || ?type=owl:DatatypeProperty)}}";
//				"{?s ?p ?o. ?p a ?type. FILTER(?type=owl:ObjectProperty || ?type=owl:DatatypeProperty)}";
				"{{?p a ?type. FILTER(?type=owl:ObjectProperty)} UNION {?p a ?type. FILTER(?type=owl:DatatypeProperty)} ?s ?p ?o .}";
	}
	
	
}
