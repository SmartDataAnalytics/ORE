package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class SymmetricPropertyAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public SymmetricPropertyAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?p a owl:SymmetricProperty.} " +
				"WHERE " +
				"{?p a owl:SymmetricProperty.}";
	}
	
	
}
