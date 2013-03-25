package org.aksw.mole.ore.validation;

import java.util.Collection;

import org.dllearner.core.owl.Property;

public interface Validator<T extends Violation, P extends Property> {
	
	Collection<T> getViolations(P property);
}
