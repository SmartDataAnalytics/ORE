package org.aksw.mole.ore.explanation.api;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public interface ExplanationGeneratorFactory {
	
	public ExplanationGenerator createExplanationGenerator(OWLReasoner reasoner);
	
	public ExplanationGenerator createExplanationGenerator(OWLReasoner reasoner, Set<OWLOntology> ontologies);
	
	public ExplanationGenerator createExplanationGenerator(OWLOntology ontology);
	
	public ExplanationGenerator createExplanationGenerator(Set<OWLOntology> ontologies);
	
}
