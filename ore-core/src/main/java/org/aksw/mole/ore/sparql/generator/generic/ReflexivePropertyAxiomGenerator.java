package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class ReflexivePropertyAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public ReflexivePropertyAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?p a owl:ReflexiveProperty.} " +
				"WHERE " +
				"{?p a owl:ReflexiveProperty.}";
	}
	
	
}
