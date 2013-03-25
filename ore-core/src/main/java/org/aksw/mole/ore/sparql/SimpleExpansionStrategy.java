package org.aksw.mole.ore.sparql;

import java.util.Set;

import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.OWLAxiom;

public class SimpleExpansionStrategy extends AbstractExpansionStrategy{
	
	public SimpleExpansionStrategy(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		super(endpoint, cache);
	}
	
	public SimpleExpansionStrategy(SparqlEndpoint endpoint) {
		super(endpoint);
	}

	@Override
	public Set<OWLAxiom> doExpansion(Set<OWLAxiom> existingAxioms) {
		// TODO Auto-generated method stub
		return null;
	}

}
