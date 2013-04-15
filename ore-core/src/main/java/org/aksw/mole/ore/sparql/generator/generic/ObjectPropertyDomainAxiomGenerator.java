package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class ObjectPropertyDomainAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public ObjectPropertyDomainAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?p rdfs:domain ?dom. ?p a owl:ObjectProperty} " +
				"WHERE " +
				"{?p rdfs:domain ?dom. ?p a owl:ObjectProperty}";
	}
	
	
}
