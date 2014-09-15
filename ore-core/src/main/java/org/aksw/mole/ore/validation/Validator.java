package org.aksw.mole.ore.validation;

import java.util.Collection;

import org.semanticweb.owlapi.model.OWLProperty;

public interface Validator<T extends Violation, P extends OWLProperty> {
	
	Collection<T> getViolations(P property);
}
