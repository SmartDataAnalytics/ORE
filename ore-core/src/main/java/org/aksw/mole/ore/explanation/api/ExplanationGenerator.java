package org.aksw.mole.ore.explanation.api;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

import com.clarkparsia.owlapi.explanation.util.ExplanationProgressMonitor;

public interface ExplanationGenerator {
	
	public Explanation getExplanation(OWLAxiom entailment);
	
	public Set<Explanation> getExplanations(OWLAxiom entailment);
	
	public Set<Explanation> getExplanations(OWLAxiom entailment, int limit);
	
	public void setProgressMonitor(ExplanationProgressMonitor expProgressMon);

}
