package org.aksw.mole.ore.impact;

import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLOntologyChange;

public interface ImpactChecker {
	
	Set<OWLOntologyChange> getImpact(List<OWLOntologyChange> changes);

}
