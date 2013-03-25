package org.aksw.mole.ore.sparql;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

public interface ExpansionStrategy {
	Set<OWLAxiom> doExpansion(Set<OWLAxiom> axioms);
}
