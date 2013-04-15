package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class SubClassOfAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public SubClassOfAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
				"CONSTRUCT " +
				"{?sub rdfs:subClassOf ?sup.} " +
				"WHERE " +
				"{?sub rdfs:subClassOf ?sup.}";
	}
	
	
}
