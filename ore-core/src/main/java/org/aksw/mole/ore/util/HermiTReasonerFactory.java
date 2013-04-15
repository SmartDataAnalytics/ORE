package org.aksw.mole.ore.util;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.IllegalConfigurationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

public class HermiTReasonerFactory implements OWLReasonerFactory{

	@Override
	public String getReasonerName() {
		return "HermiT";
	}

	@Override
	public OWLReasoner createNonBufferingReasoner(OWLOntology ontology) {
		Configuration conf = new Configuration();
		conf.ignoreUnsupportedDatatypes = true;
		return new Reasoner(conf, ontology);
	}

	@Override
	public OWLReasoner createReasoner(OWLOntology ontology) {
		return null;
	}

	@Override
	public OWLReasoner createNonBufferingReasoner(OWLOntology ontology, OWLReasonerConfiguration config)
			throws IllegalConfigurationException {
		return null;
	}

	@Override
	public OWLReasoner createReasoner(OWLOntology ontology, OWLReasonerConfiguration config)
			throws IllegalConfigurationException {
		return null;
	}


}
