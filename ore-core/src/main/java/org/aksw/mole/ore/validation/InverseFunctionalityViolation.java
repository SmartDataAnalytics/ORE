package org.aksw.mole.ore.validation;

import org.aksw.mole.ore.util.HTML;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.KBElement;
import org.dllearner.core.owl.Property;

public class InverseFunctionalityViolation implements Violation{
	
	private Property property;
	private KBElement object;
	private Individual subject1;
	private Individual subject2;

	public InverseFunctionalityViolation(Property property, KBElement object, Individual subject1, Individual subject2) {
		this.property = property;
		this.object = object;
		this.subject1 = subject1;
		this.subject2 = subject2;
	}
	
	public Property getProperty() {
		return property;
	}
	
	public KBElement getObject() {
		return object;
	}
	
	public Individual getSubject1() {
		return subject1;
	}
	
	public Individual getSubject2() {
		return subject2;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("------------------------------------------------------------------------\n");
		sb.append(property.getName() + "(" + subject1.toString() + ", " + object.toString() + ")\n"); 
		sb.append(property.getName() + "(" + subject2.toString() + ", " + object.toString() + ")");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result + ((property == null) ? 0 : property.hashCode());
		result = prime * result + ((subject1 == null) ? 0 : subject1.hashCode());
		result = prime * result + ((subject2 == null) ? 0 : subject2.hashCode());
//		return result;
		return prime * object.hashCode() + property.hashCode() + subject1.hashCode() + subject2.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InverseFunctionalityViolation other = (InverseFunctionalityViolation) obj;
		if (object == null) {
			if (other.object != null)
				return false;
		} else if (!object.equals(other.object))
			return false;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		if (!(subject1.equals(other.subject1) || subject1.equals(other.subject2)))
			return false;
		if (!(subject2.equals(other.subject1) || subject2.equals(other.subject2)))
			return false;
		return true;
	}
	
	@Override
	public String asHTML() {
		StringBuilder sb = new StringBuilder();
		sb.append("------------------------------------------------------------------------\n");
		sb.append(HTML.asLink(property.getName()) + "(" + HTML.asLink(subject1.toString()) + ", " + HTML.asLink(object.toString()) + ")\n"); 
		sb.append(HTML.asLink(property.getName()) + "(" + HTML.asLink(subject2.toString()) + ", " + HTML.asLink(object.toString()) + ")");
		return sb.toString();
	}

	
	

}
