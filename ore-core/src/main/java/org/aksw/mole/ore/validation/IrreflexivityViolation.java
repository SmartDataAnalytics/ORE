package org.aksw.mole.ore.validation;

import org.aksw.mole.ore.util.HTML;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public class IrreflexivityViolation implements Violation{
	
	private OWLIndividual individual;
	private OWLObjectProperty property;

	public IrreflexivityViolation(OWLObjectProperty property, OWLIndividual individual) {
		this.property = property;
		this.individual = individual;
	}
	
	public OWLObjectProperty getProperty() {
		return property;
	}
	
	public OWLIndividual getIndividual() {
		return individual;
	}
	
	@Override
	public String toString() {
		return property.toStringID() + "(" + individual.toStringID() + ", " + individual.toStringID() + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((individual == null) ? 0 : individual.hashCode());
		result = prime * result + ((property == null) ? 0 : property.hashCode());
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
		IrreflexivityViolation other = (IrreflexivityViolation) obj;
		if (individual == null) {
			if (other.individual != null)
				return false;
		} else if (!individual.equals(other.individual))
			return false;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		return true;
	}
	
	@Override
	public String asHTML() {
		return HTML.asLink(property.toStringID()) + "(" + HTML.asLink(individual.toStringID()) + ", " + HTML.asLink(individual.toStringID()) + ")";
	}
	
	
	
	

}
