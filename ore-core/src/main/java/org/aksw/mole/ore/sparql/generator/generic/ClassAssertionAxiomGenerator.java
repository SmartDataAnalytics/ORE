package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class ClassAssertionAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public ClassAssertionAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?s a ?type. ?type a owl:Class} " +
				"WHERE " +
				"{?s a ?type. ?type a owl:Class}";
	}
	
	
}
