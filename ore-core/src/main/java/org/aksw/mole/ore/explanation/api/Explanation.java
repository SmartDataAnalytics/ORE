package org.aksw.mole.ore.explanation.api;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

public interface Explanation {
	
	public OWLAxiom getEntailment();
	
	public Set<OWLAxiom> getAxioms();
	

}
