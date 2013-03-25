package org.aksw.mole.ore.validation;

import org.aksw.mole.ore.util.HTML;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.ObjectProperty;

public class IrreflexivityViolation implements Violation{
	
	private Individual individual;
	private ObjectProperty property;

	public IrreflexivityViolation(ObjectProperty property, Individual individual) {
		this.property = property;
		this.individual = individual;
	}
	
	public ObjectProperty getProperty() {
		return property;
	}
	
	public Individual getIndividual() {
		return individual;
	}
	
	@Override
	public String toString() {
		return property.getName() + "(" + individual.getName() + ", " + individual.getName() + ")";
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
		return HTML.asLink(property.getName()) + "(" + HTML.asLink(individual.getName()) + ", " + HTML.asLink(individual.getName()) + ")";
	}
	
	
	
	

}
