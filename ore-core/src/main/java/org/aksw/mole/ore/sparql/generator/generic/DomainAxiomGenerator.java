package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class DomainAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public DomainAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?p rdfs:domain ?dom. ?p a ?type} " +
				"WHERE " +
//				"{?p rdfs:domain ?dom. ?p a ?type}";
				"{?p rdfs:domain ?dom. OPTIONAL{?p a ?type. FILTER(?type=owl:ObjectProperty || ?type=owl:DatatypeProperty)}}";
	}
	
	
}
