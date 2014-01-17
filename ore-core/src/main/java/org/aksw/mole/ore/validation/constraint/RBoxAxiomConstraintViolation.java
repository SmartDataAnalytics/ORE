/**
 * 
 */
package org.aksw.mole.ore.validation.constraint;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;

/**
 * @author Lorenz Buehmann
 *
 */
public class RBoxAxiomConstraintViolation implements ConstraintViolation {
	
	
	private OWLAxiom constraint;
	private OWLIndividual subject;
	private OWLIndividual object;

	public RBoxAxiomConstraintViolation(OWLAxiom constraint, OWLIndividual subject, OWLIndividual object) {
		this.constraint = constraint;
		this.subject = subject;
		this.object = object;
	}
	
	/**
	 * @return the constraint
	 */
	public OWLAxiom getConstraint() {
		return constraint;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return constraint + ":" + subject + ", " + object;
	}

}
