package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class EquivalentPropertiesAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public EquivalentPropertiesAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?p1 owl:equivalentProperty ?p2.} " +
				"WHERE " +
				"{?p1 owl:equivalentProperty ?p2.}";
	}
	
	
}
