package org.aksw.mole.ore.validation;

import org.semanticweb.owlapi.model.OWLProperty;

public interface ConsistencyValidator<T extends Violation, P extends OWLProperty> extends Validator<T, P>{
	boolean isConsistent(P property);
}
