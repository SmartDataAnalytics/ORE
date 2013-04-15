package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class RangeAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public RangeAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
				"CONSTRUCT " +
				"{?p rdfs:range ?range. ?p a ?type} " +
				"WHERE " +
				"{?p rdfs:range ?range. ?p a ?type}";
	}
	
	
}
