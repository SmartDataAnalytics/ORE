package org.aksw.mole.ore.sparql.generator.generic;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedGeneralAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;

public class DisjointClassesAxiomGenerator extends AbstractSPARQLBasedGeneralAxiomGenerator{
	

	public DisjointClassesAxiomGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		query = "PREFIX owl: <http://www.w3.org/2002/07/owl#>" +
				"CONSTRUCT " +
				"{?cls1 owl:disjointWith ?cls2.} " +
				"WHERE " +
				"{?cls1 owl:disjointWith ?cls2.}";
	}
	
	
}
