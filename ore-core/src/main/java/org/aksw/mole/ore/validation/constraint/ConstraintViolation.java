/**
 * 
 */
package org.aksw.mole.ore.validation.constraint;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Lorenz Buehmann
 *
 */
public interface ConstraintViolation {

	OWLAxiom getConstraint();
}
