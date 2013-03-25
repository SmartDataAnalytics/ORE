package org.aksw.mole.ore.explanation.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

public class AxiomUsageChecker {
	
	private OWLOntology ontology;
	private Map<OWLAxiom, Set<OWLAxiom>> axiom2UsageMap;
	
	public AxiomUsageChecker(OWLOntology ontology){
		this.ontology = ontology;
		axiom2UsageMap = new HashMap<OWLAxiom, Set<OWLAxiom>>();
	}

	private Set<OWLAxiom> computeUsage(OWLAxiom axiom) {
		Set<OWLAxiom> usage = new HashSet<OWLAxiom>();
		for(OWLEntity ent : axiom.getSignature()){
			for(OWLAxiom ax : ontology.getLogicalAxioms()){
				if(ax.getSignature().contains(ent)){
					usage.add(ax);
				}
			}
		}
		
		return usage;
	}
	
	public Set<OWLAxiom> getUsage(OWLAxiom axiom){
		Set<OWLAxiom> usage = axiom2UsageMap.get(axiom);
		if(usage == null){
			usage = computeUsage(axiom);
			axiom2UsageMap.put(axiom, usage);
		}
		return usage;
	}
}
