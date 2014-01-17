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
public class TBoxAxiomConstraintViolation implements ConstraintViolation {
	
	
	private OWLIndividual individual;
	private OWLAxiom constraint;

	public TBoxAxiomConstraintViolation(OWLAxiom constraint, OWLIndividual individual) {
		this.constraint = constraint;
		this.individual = individual;
	}
	
	/**
	 * @return the constraint
	 */
	public OWLAxiom getConstraint() {
		return constraint;
	}
	
	/**
	 * @return the individual
	 */
	public OWLIndividual getIndividual() {
		return individual;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return constraint + ":" + individual;
	}

}
