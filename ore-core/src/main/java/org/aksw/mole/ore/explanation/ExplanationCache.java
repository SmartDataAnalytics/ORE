package org.aksw.mole.ore.explanation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aksw.mole.ore.explanation.api.ExplanationType;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

public class ExplanationCache {
	
	private ExplanationType explanationType;
	private Map<OWLAxiom, Set<Explanation<OWLAxiom>>> cache = new HashMap<OWLAxiom, Set<Explanation<OWLAxiom>>>();
	private Multiset<OWLAxiom> explanationAxioms = HashMultiset.create();

	public ExplanationCache(ExplanationType explanationType) {
		this.explanationType = explanationType;
	}
	
	public ExplanationType getExplanationType() {
		return explanationType;
	}
	
	public Set<Explanation<OWLAxiom>> getExplanations(OWLAxiom entailment){
		return getExplanations(entailment, Integer.MAX_VALUE);
	}
	
	public Set<Explanation<OWLAxiom>> getExplanations(OWLAxiom entailment, int limit){
		Set<Explanation<OWLAxiom>> explanations = cache.get(entailment);
		
		return explanations;
	}
	
	public void addExplanations(OWLAxiom entailment, Set<Explanation<OWLAxiom>> explanations){
		cache.put(entailment, explanations);
		for(Explanation<OWLAxiom> explanation : explanations){
			explanationAxioms.addAll(explanation.getAxioms());
		}
	}
	
	public Set<Explanation<OWLAxiom>> getAllComputedExplanations(){
		Set<Explanation<OWLAxiom>> allExplanations = new HashSet<Explanation<OWLAxiom>>();
		for(Set<Explanation<OWLAxiom>> explanations : cache.values()){
			allExplanations.addAll(explanations);
		}
		return allExplanations;
	}
	
	public int getAxiomFrequency(OWLAxiom axiom){
		int frequency = 0;
		for(Set<Explanation<OWLAxiom>> explanations : cache.values()){
			for(Explanation<OWLAxiom> explanation : explanations){
				if(explanation.contains(axiom)){
					frequency++;
				}
			}
		}
		return frequency;
	}
	
	public int getMaxAxiomFrequency(){
		int max = 0;
		for(Entry<OWLAxiom> entry : explanationAxioms.entrySet()){
			if(entry.getCount() > max){
				max = entry.getCount();
			}
		}
		return max;
	}
	
	public void clear(){
		cache.clear();
	}

}
