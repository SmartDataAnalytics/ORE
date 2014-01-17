/**
 * 
 */
package org.aksw.mole.ore.validation.constraint;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Lorenz Buehmann
 *
 */
public class ABoxAxiomConstraintViolation implements ConstraintViolation {
	
	
	private OWLAxiom constraint;
	private boolean violated;

	public ABoxAxiomConstraintViolation(OWLAxiom constraint, boolean violated) {
		this.constraint = constraint;
		this.violated = violated;
	}
	
	/**
	 * @return the constraint
	 */
	public OWLAxiom getConstraint() {
		return constraint;
	}
	
	/**
	 * @return the violated
	 */
	public boolean isViolated() {
		return violated;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return constraint + ":" + violated;
	}

}
