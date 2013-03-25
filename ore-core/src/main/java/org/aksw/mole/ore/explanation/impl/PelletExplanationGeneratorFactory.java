package org.aksw.mole.ore.explanation.impl;

import java.util.Set;

import org.aksw.mole.ore.explanation.api.ExplanationGenerator;
import org.aksw.mole.ore.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class PelletExplanationGeneratorFactory implements
		ExplanationGeneratorFactory {

	@Override
	public ExplanationGenerator createExplanationGenerator(OWLReasoner reasoner) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExplanationGenerator createExplanationGenerator(
			OWLReasoner reasoner, Set<OWLOntology> ontologies) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExplanationGenerator createExplanationGenerator(OWLOntology ontology) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExplanationGenerator createExplanationGenerator(
			Set<OWLOntology> ontologies) {
		// TODO Auto-generated method stub
		return null;
	}

}
