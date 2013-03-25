package org.aksw.mole.ore.explanation.impl;

import java.util.Set;
import java.util.TreeSet;

import org.aksw.mole.ore.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

public class ExplanationImpl implements Explanation {
	
	private OWLAxiom entailment;
	private Set<OWLAxiom> axioms;
	
	
	public ExplanationImpl(OWLAxiom entailment, Set<OWLAxiom> axioms){
		this.entailment = entailment;
		this.axioms = axioms;
	}

	public OWLAxiom getEntailment() {
		return entailment;
	}

	public Set<OWLAxiom> getAxioms() {
		return axioms;
	}
	
//	@Override
//	public boolean equals(Object o) {
//		if(o == this){
//			return true;
//		}
//		if(!(o instanceof Explanation)){
//			return false;
//		}
//		Explanation exp = (Explanation)o;
//		return exp.getEntailment().equals(entailment) && exp.getAxioms().equals(axioms);
//	}
//
//	@Override
//	public int hashCode() {
//		return entailment.hashCode() + axioms.hashCode();
//	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((axioms == null) ? 0 : axioms.hashCode());
		result = prime * result + ((entailment == null) ? 0 : entailment.hashCode());
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
		ExplanationImpl other = (ExplanationImpl) obj;
		if (axioms == null) {
			if (other.axioms != null)
				return false;
		} else if (!axioms.equals(other.axioms))
			return false;
		if (entailment == null) {
			if (other.entailment != null)
				return false;
		} else if (!entailment.equals(other.entailment))
			return false;
		return true;
	}

	@Override
	public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Explanation [");
        sb.append(entailment);
        sb.append("]\n");
        if(axioms.isEmpty()){
        	sb.append("<Empty>\n");
        } else {
        	int i = 1;
        	for(OWLAxiom ax : (new TreeSet<OWLAxiom>(axioms))){
                sb.append("\t");
                sb.append(i++);
                sb.append(")");
                sb.append(ax);
                sb.append("\n");
            }
        }
        return sb.toString();
	}

}
