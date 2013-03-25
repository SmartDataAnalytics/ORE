package org.aksw.mole.ore.validation;

import org.dllearner.core.owl.Property;

public interface ConsistencyValidator<T extends Violation, P extends Property> extends Validator<T, P>{
	boolean isConsistent(P property);
}
