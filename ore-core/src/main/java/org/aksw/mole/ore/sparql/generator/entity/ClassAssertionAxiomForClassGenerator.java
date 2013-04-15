package org.aksw.mole.ore.sparql.generator.entity;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedEntityRelatedAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;

import com.hp.hpl.jena.query.ParameterizedSparqlString;

public class ClassAssertionAxiomForClassGenerator extends AbstractSPARQLBasedEntityRelatedAxiomGenerator<OWLClass>{
	

	public ClassAssertionAxiomForClassGenerator(SparqlEndpointKS endpoint) {
		super(endpoint);
		
		axiomType = AxiomType.CLASS_ASSERTION;
		
		queryTemplate = new ParameterizedSparqlString(
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?s a ?var.} " +
				"WHERE " +
				"{?s a ?var.}"
				);
	}
	
	
}
