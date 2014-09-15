package org.aksw.mole.ore.validation;

import org.aksw.mole.ore.util.HTML;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public class AsymmetryViolation implements Violation{
	
	private OWLObjectProperty property;
	private OWLIndividual individual1;
	private OWLIndividual individual2;

	public AsymmetryViolation(OWLObjectProperty property, OWLIndividual individual1, OWLIndividual individual2) {
		this.property = property;
		this.individual1 = individual1;
		this.individual2 = individual2;
	}
	
	public OWLObjectProperty getProperty() {
		return property;
	}
	
	public OWLIndividual getIndividual1() {
		return individual1;
	}
	
	public OWLIndividual getIndividual2() {
		return individual2;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("------------------------------------------------------------------------\n");
		if(individual1.equals(individual2)){
			sb.append(property.toStringID() + "(" + individual1.toStringID() + ", " + individual2.toStringID() + ")");
		} else {
			sb.append(property.toStringID() + "(" + individual1.toStringID() + ", " + individual2.toStringID() + ")");
			sb.append("\n");
			sb.append(property.toStringID() + "(" + individual2.toStringID() + ", " + individual1.toStringID() + ")");
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
			sb.append(HTML.asLink(property.toStringID()) + "(" + HTML.asLink(individual1.toStringID()) + ", " + HTML.asLink(individual2.toStringID()) + ")");
		} else {
			sb.append(HTML.asLink(property.toStringID()) + "(" + HTML.asLink(individual1.toStringID()) + ", " + HTML.asLink(individual2.toStringID()) + ")");
			sb.append("\n");
			sb.append(HTML.asLink(property.toStringID()) + "(" + HTML.asLink(individual2.toStringID()) + ", " + HTML.asLink(individual1.toStringID()) + ")");
		}
		return sb.toString();
	}
	
	

}
