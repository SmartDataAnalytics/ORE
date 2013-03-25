package org.aksw.mole.ore.explanation.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.aksw.mole.ore.explanation.api.Explanation;
import org.aksw.mole.ore.explanation.api.ExplanationGenerator;
import org.aksw.mole.ore.explanation.api.ExplanationGeneratorFactory;
import org.mindswap.pellet.utils.SetUtils;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.clarkparsia.owlapi.explanation.util.ExplanationProgressMonitor;

public class CachingExplanationGenerator implements ExplanationGenerator{
	
	private ExplanationGeneratorFactory explanationGeneratorFactory;
	private ExplanationGenerator expGen;
	private Map<OWLAxiom, Set<Explanation>> axiom2ExplanationsMap = Collections.synchronizedMap(new HashMap<OWLAxiom, Set<Explanation>>());
	private Map<OWLAxiom, Integer> lastRequestedSizeMap = Collections.synchronizedMap(new HashMap<OWLAxiom, Integer>());
	private Map<OWLAxiom, Set<Explanation>> axiom2ModuleTable = Collections.synchronizedMap(new Hashtable<OWLAxiom, Set<Explanation>>());
	
	private Map<OWLAxiom, Integer> axiom2FrequencyMap = Collections.synchronizedMap(new HashMap<OWLAxiom, Integer>());
	private int maxFrequency = -1;
	
	public CachingExplanationGenerator(ExplanationGeneratorFactory explanationGeneratorFactory){
		this.explanationGeneratorFactory = explanationGeneratorFactory;
	}
	
	public CachingExplanationGenerator(ExplanationGenerator expGen){
		this.expGen = expGen;
	}
	
	//TODO make selection deterministic, i.e. use TreeSet with Comparable
	@Override
	public Explanation getExplanation(OWLAxiom entailment) {
		return getExplanations(entailment, 1).iterator().next();
	}

	@Override
	public Set<Explanation> getExplanations(OWLAxiom entailment) {
		return getExplanations(entailment, -1);
	}

	@Override
	public Set<Explanation> getExplanations(OWLAxiom entailment, int limit) {
		Set<Explanation> oldExplanations = axiom2ExplanationsMap.get(entailment);
		Set<Explanation> explanations = axiom2ExplanationsMap.get(entailment);
		Integer lastRequestedSize = lastRequestedSizeMap.get(entailment);
		if(lastRequestedSize == null){
            lastRequestedSize = Integer.valueOf(0);
		}
		if((explanations == null) || ((lastRequestedSize.intValue() < limit) && (lastRequestedSize.intValue() != -1))){
			if(limit == -1){
				explanations = expGen.getExplanations(entailment);
			} else {
				explanations = expGen.getExplanations(entailment, limit);
				//if there are no new explanations, we can set the value to -1, so next time there will 
				//not be tried again to compute more explanations
				if(explanations.size() < limit){
					limit = -1;
				}
			}
			axiom2ExplanationsMap.put(entailment, explanations);
			lastRequestedSizeMap.put(entailment, Integer.valueOf(limit));
			
			if(oldExplanations != null){
				updateFrequencyValues(SetUtils.difference(explanations, oldExplanations));
			} else {
				updateFrequencyValues(explanations);
			}
		}
		return explanations;
	}
	
	public void clear(){
		axiom2ExplanationsMap.clear();
		lastRequestedSizeMap.clear();
		axiom2ModuleTable.clear();
		axiom2FrequencyMap.clear();
		maxFrequency = -1;
	}
	
	private void updateFrequencyValues(Set<Explanation> explanations){
		Integer frequency;
		for(Explanation exp : explanations){
			for(OWLAxiom ax : exp.getAxioms()){
				frequency = axiom2FrequencyMap.get(ax);
				if(frequency == null){
					frequency = Integer.valueOf(1);
				} else {
					frequency = Integer.valueOf(frequency.intValue() + 1);
				}
				if(frequency > maxFrequency){
					maxFrequency = frequency.intValue();
				}
				axiom2FrequencyMap.put(ax, frequency);
			}
		}
	}
	
	public int getAxiomFrequency(OWLAxiom ax){
		return axiom2FrequencyMap.get(ax);
	}
	
	public int getMaxAxiomFrequency(){
		return maxFrequency;
	}
	
	public Set<Explanation> getAllComputedExplanations(){
		Set<Explanation> allExplanations = new HashSet<Explanation>();
		for(Set<Explanation> explanations : axiom2ExplanationsMap.values()){
			allExplanations.addAll(explanations);
		}
		return allExplanations;
	}
	
	@Override
	public void setProgressMonitor(ExplanationProgressMonitor expProgressMon) {
		expGen.setProgressMonitor(expProgressMon);
	}
	
	

}
