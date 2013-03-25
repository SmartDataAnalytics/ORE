package org.aksw.mole.ore.validation;

import org.aksw.mole.ore.util.HTML;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.ObjectProperty;

public class AsymmetryViolation implements Violation{
	
	private ObjectProperty property;
	private Individual individual1;
	private Individual individual2;

	public AsymmetryViolation(ObjectProperty property, Individual individual1, Individual individual2) {
		this.property = property;
		this.individual1 = individual1;
		this.individual2 = individual2;
	}
	
	public ObjectProperty getProperty() {
		return property;
	}
	
	public Individual getIndividual1() {
		return individual1;
	}
	
	public Individual getIndividual2() {
		return individual2;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("------------------------------------------------------------------------\n");
		if(individual1.equals(individual2)){
			sb.append(property.getName() + "(" + individual1.getName() + ", " + individual2.getName() + ")");
		} else {
			sb.append(property.getName() + "(" + individual1.getName() + ", " + individual2.getName() + ")");
			sb.append("\n");
			sb.append(property.getName() + "(" + individual2.getName() + ", " + individual1.getName() + ")");
		}
		
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((individual1 == null) ? 0 : individual1.hashCode());
		result = prime * result + ((individual2 == null) ? 0 : individual2.hashCode());
		result = prime * result + ((property == null) ? 0 : property.hashCode());
//		return result;
		return prime * (property.hashCode() + individual1.hashCode() + individual2.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AsymmetryViolation other = (AsymmetryViolation) obj;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		if(!(individual1.equals(other.individual1) || individual1.equals(other.individual2)))
			return false;
		if(!(individual2.equals(other.individual2) || individual2.equals(other.individual1)))
			return false;
		return true;
	}
	
	@Override
	public String asHTML() {
		StringBuilder sb = new StringBuilder();
		sb.append("------------------------------------------------------------------------\n");
		if(individual1.equals(individual2)){
			sb.append(HTML.asLink(property.getName()) + "(" + HTML.asLink(individual1.getName()) + ", " + HTML.asLink(individual2.getName()) + ")");
		} else {
			sb.append(HTML.asLink(property.getName()) + "(" + HTML.asLink(individual1.getName()) + ", " + HTML.asLink(individual2.getName()) + ")");
			sb.append("\n");
			sb.append(HTML.asLink(property.getName()) + "(" + HTML.asLink(individual2.getName()) + ", " + HTML.asLink(individual1.getName()) + ")");
		}
		return sb.toString();
	}
	
	

}
