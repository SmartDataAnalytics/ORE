/**
 * 
 */
package org.aksw.mole.ore.validation.constraint;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Lorenz Buehmann
 *
 */
public class SubjectObjectViolation implements ConstraintViolation{
	
	private OWLAxiom constraintAxiom;
	private String subject;
	private String object;
	
	public SubjectObjectViolation(OWLAxiom constraintAxiom, String subject, String object) {
		this.constraintAxiom = constraintAxiom;
		this.subject = subject;
		this.object = object;
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
	
	/**
	 * @return the object
	 */
	public String getObject() {
		return object;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constraintAxiom == null) ? 0 : constraintAxiom.hashCode());
		result = prime * result + ((object == null) ? 0 : object.hashCode());
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
		SubjectObjectViolation other = (SubjectObjectViolation) obj;
		if (constraintAxiom == null) {
			if (other.constraintAxiom != null)
				return false;
		} else if (!constraintAxiom.equals(other.constraintAxiom))
			return false;
		if (object == null) {
			if (other.object != null)
				return false;
		} else if (!object.equals(other.object))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		return true;
	}
	

}
