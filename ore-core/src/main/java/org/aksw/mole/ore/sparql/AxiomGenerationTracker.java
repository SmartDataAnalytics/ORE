package org.aksw.mole.ore.sparql;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.aksw.mole.ore.sparql.generator.AxiomGenerator;
import org.semanticweb.owlapi.model.OWLAxiom;


public class AxiomGenerationTracker {

	Map<AxiomGenerator, Map<Long, Set<OWLAxiom>>> tracking = new HashMap<AxiomGenerator, Map<Long,Set<OWLAxiom>>>();
	
	public void track(AxiomGenerator generator, Set<OWLAxiom> axioms){
		if(!axioms.isEmpty()){
			Map<Long, Set<OWLAxiom>> timestampWithAxioms = tracking.get(generator);
			if(timestampWithAxioms == null){
				timestampWithAxioms = new LinkedHashMap<Long, Set<OWLAxiom>>();
				tracking.put(generator, timestampWithAxioms);
			}
			timestampWithAxioms.put(System.currentTimeMillis(), axioms);
		}
	}
	
	public AxiomGenerator generatedFirstBy(OWLAxiom axiom){
		SortedMap<Long, AxiomGenerator> timestampWithGenerator = generatedByOrdered(axiom);
		return timestampWithGenerator.get(timestampWithGenerator.firstKey());
	}
	
	public SortedMap<Long, AxiomGenerator> generatedByOrdered(OWLAxiom axiom){
		SortedMap<Long, AxiomGenerator> timestampWithGenerator = new TreeMap<Long, AxiomGenerator>();
		for (Entry<AxiomGenerator, Map<Long, Set<OWLAxiom>>> entry : tracking
				.entrySet()) {
			AxiomGenerator generator = entry.getKey();
			Map<Long, Set<OWLAxiom>> timestampWithAxioms = entry.getValue();
			for (Entry<Long, Set<OWLAxiom>> entry2 : timestampWithAxioms.entrySet()) {
				Long timestamp = entry2.getKey();
				Set<OWLAxiom> axioms = entry2.getValue();
				if(axioms.contains(axiom)){
					timestampWithGenerator.put(timestamp, generator);
					break;
				}
			}
		}
		return timestampWithGenerator;
	}

}
