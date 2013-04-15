package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class FunctionalPropertyAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public FunctionalPropertyAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?p a owl:FunctionalProperty. ?p a ?type.} " +
				"WHERE " +
//				"{?p a owl:FunctionalProperty.}";
				"{?p a owl:FunctionalProperty. OPTIONAL{?p a ?type. FILTER(?type=owl:ObjectProperty || ?type=owl:DatatypeProperty)}}";
	}
	
	
}
