/**
 * 
 */
package org.aksw.mole.ore.validation.constraint;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Lorenz Buehmann
 *
 */
public class SubjectViolation implements ConstraintViolation{
	
	private OWLAxiom constraintAxiom;
	private String subject;
	
	public SubjectViolation(OWLAxiom constraintAxiom, String subject) {
		this.constraintAxiom = constraintAxiom;
		this.subject = subject;
	}
	
	/**
	 * @return the constraintAxiom
	 */
	public OWLAxiom getConstraint() {
		return constraintAxiom;
	}
	
	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constraintAxiom == null) ? 0 : constraintAxiom.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubjectViolation other = (SubjectViolation) obj;
		if (constraintAxiom == null) {
			if (other.constraintAxiom != null)
				return false;
		} else if (!constraintAxiom.equals(other.constraintAxiom))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Violation: " + subject;
	}

}
