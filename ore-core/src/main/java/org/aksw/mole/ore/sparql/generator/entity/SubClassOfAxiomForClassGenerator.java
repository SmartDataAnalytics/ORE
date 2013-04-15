package org.aksw.mole.ore.sparql.generator.entity;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedEntityRelatedAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.OWLClass;

import com.hp.hpl.jena.query.ParameterizedSparqlString;

public class SubClassOfAxiomForClassGenerator extends AbstractSPARQLBasedEntityRelatedAxiomGenerator<OWLClass>{
	

	public SubClassOfAxiomForClassGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		queryTemplate = new ParameterizedSparqlString(
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?var rdfs:subClassOf ?sup.} " +
				"WHERE " +
				"{?var rdfs:subClassOf ?sup.}"
				);
	}
	
	
}
