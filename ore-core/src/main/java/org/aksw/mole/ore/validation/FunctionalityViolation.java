package org.aksw.mole.ore.validation;

import org.aksw.mole.ore.util.HTML;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLProperty;

public class FunctionalityViolation implements Violation{
	
	private OWLProperty property;
	private OWLIndividual subject;
	private OWLObject object1;
	private OWLObject object2;

	public FunctionalityViolation(OWLProperty property, OWLIndividual subject, OWLObject object1, OWLObject object2) {
		this.property = property;
		this.subject = subject;
		this.object1 = object1;
		this.object2 = object2;
	}
	
	public OWLProperty getProperty() {
		return property;
	}
	
	public OWLIndividual getSubject() {
		return subject;
	}
	
	public OWLObject getObject1() {
		return object1;
	}
	
	public OWLObject getObject2() {
		return object2;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("------------------------------------------------------------------------\n");
		sb.append(property.toStringID() + "(" + subject.toStringID() + ", " + object1.toString() + ")\n");
		sb.append(property.toStringID() + "(" + subject.toStringID() + ", " + object2.toString() + ")");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		return prime + ((object1 == null) ? 0 : object1.hashCode()) 
				+ ((object2 == null) ? 0 : object2.hashCode()) 
				+ ((property == null) ? 0 : property.hashCode())
				+ ((subject == null) ? 0 : subject.hashCode());
//		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FunctionalityViolation other = (FunctionalityViolation) obj;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		if(!(object1.equals(other.object1) || object1.equals(other.object2)))
			return false;
		if(!(object2.equals(other.object2) || object2.equals(other.object1)))
			return false;
		return true;
	}
	
	@Override
	public String asHTML() {
		StringBuilder sb = new StringBuilder();
		sb.append("------------------------------------------------------------------------\n");
		sb.append(HTML.asLink(property.toStringID()) + "(" + HTML.asLink(subject.toStringID()) + ", " + ((object1 instanceof OWLIndividual) ? HTML.asLink(object1.toString()) : object1.toString()) + ")\n");
		sb.append(HTML.asLink(property.toStringID()) + "(" + HTML.asLink(subject.toStringID()) + ", " + ((object2 instanceof OWLIndividual) ? HTML.asLink(object2.toString()) : object2.toString()) + ")");
		return sb.toString();
	}

	
	

}
