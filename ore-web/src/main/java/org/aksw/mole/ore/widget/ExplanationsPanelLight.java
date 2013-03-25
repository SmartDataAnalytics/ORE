package org.aksw.mole.ore.widget;

import java.util.Collection;
import java.util.Collections;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import com.vaadin.ui.Panel;

public class ExplanationsPanelLight extends Panel{
	
	public ExplanationsPanelLight() {
		setImmediate(true);
	}
	
	public void showExplanations(Collection<Explanation<OWLAxiom>> explanations){
		removeAllComponents();
		for(Explanation<OWLAxiom> exp : explanations){
			addExplanation(exp);
		}
	}
	
	public void addExplanation(Explanation<OWLAxiom> explanation){
		ExplanationTable t = new ExplanationTable(explanation, Collections.<OWLAxiom>emptySet());
		addComponent(t);
		t.setCaption(((OWLSubClassOfAxiom)explanation.getEntailment()).getSubClass().toString());
	}
	
	public void addExplanations(Collection<Explanation<OWLAxiom>> explanations){
		for(Explanation<OWLAxiom> exp : explanations){
			addExplanation(exp);
		}
	}
	
	public void clear(){
		removeAllComponents();
	}

}
