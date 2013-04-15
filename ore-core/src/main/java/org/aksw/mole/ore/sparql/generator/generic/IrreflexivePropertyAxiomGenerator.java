package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class IrreflexivePropertyAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public IrreflexivePropertyAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?p a owl:IrreflexiveProperty.} " +
				"WHERE " +
				"{?p a owl:IrreflexiveProperty.}";
	}
	
	
}
