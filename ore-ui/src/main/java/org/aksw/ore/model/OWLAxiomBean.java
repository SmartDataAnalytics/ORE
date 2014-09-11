package org.aksw.ore.model;

import org.semanticweb.owlapi.model.OWLAxiom;

public class OWLAxiomBean {
	
	private OWLAxiom axiom;
	private boolean selected;
	
	public OWLAxiomBean() {
	}
	
	public OWLAxiomBean(OWLAxiom axiom) {
		this.axiom = axiom;
	}
	
	public void setAxiom(OWLAxiom axiom) {
		this.axiom = axiom;
	}
	
	public OWLAxiom getAxiom() {
		return axiom;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public boolean isSelected() {
		return selected;
	}
}
