package org.aksw.mole.ore.sparql;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

public interface InconsistencyFinder {
	/**
	 * Returns an fragment of the knowledge base which is inconsistent, if exist.
	 * @return
	 */
	Set<OWLAxiom> getInconsistentFragment();
}
