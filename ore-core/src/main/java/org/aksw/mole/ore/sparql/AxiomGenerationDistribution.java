package org.aksw.mole.ore.sparql;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.mole.ore.sparql.generator.AxiomGenerator;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

/**
 * 
 * Stores the probability distribution of how axiom generators should be applied.
 * 
 * @author Jens Lehmann
 *
 */
public class AxiomGenerationDistribution {

	// axiom generators and their probability of being applied
	private Map<AxiomGenerator,Double> dist;
	

	private DecimalFormat df = new DecimalFormat("0.00");
	
	public AxiomGenerationDistribution(Map<AxiomGenerator,Double> dist) {
		this.dist = dist;
	}
	
	/**
	 * Computes the distribution using a set of axioms and information on how they were
	 * created.
	 * 
	 * @param tracker
	 * @param axioms
	 */
	public AxiomGenerationDistribution(AxiomGenerationTracker tracker, Set<OWLAxiom> axioms) {
		Multiset<AxiomGenerator> axiomGenerators = TreeMultiset.create();
		for(OWLAxiom axiom : axioms) {
			AxiomGenerator generator = tracker.generatedFirstBy(axiom);
			axiomGenerators.add(generator);	
		}

		dist = new HashMap<AxiomGenerator,Double>();
		int total = axiomGenerators.size();
		for(AxiomGenerator gen : axiomGenerators.elementSet()) {
			int count = axiomGenerators.count(gen);
			System.out.println(gen + ": " + count + " = " + (count/(double)total) + "%");
			dist.put(gen, count/(double)total);
		}
	}
	
	/**
	 * Creates a distribution as average of the distributions given as parameter.
	 * 
	 * @param distributions
	 * @return
	 */
	public static AxiomGenerationDistribution computeAverage(Set<AxiomGenerationDistribution> distributions) {
		// step 1: sum up values from all distributions
		Map<AxiomGenerator,Double> dist = new HashMap<AxiomGenerator,Double>();
		for(AxiomGenerationDistribution d : distributions) {
			Map<AxiomGenerator,Double> tmp = d.getDist();
			for(Entry<AxiomGenerator,Double> entry : tmp.entrySet()) {
				AxiomGenerator ag = entry.getKey();
				double val = entry.getValue();
				Double v = dist.get(ag);
				if(v == null) {
					dist.put(ag,  val);
				} else {
					dist.put(ag, val + v);
				}
			}
		}
		// step 2: divide by number of distributions
		int nr = distributions.size();
		for(AxiomGenerator ag : dist.keySet()) {
			dist.put(ag, dist.get(ag)/(double)nr);
		}
		
		return new AxiomGenerationDistribution(dist);
	}

	public Map<AxiomGenerator, Double> getDist() {
		return dist;
	}
	
	public String toString() {
		String str = "";
		for(Entry<AxiomGenerator,Double> entry : dist.entrySet()) {
			str += entry.getKey().getClass().getName() + ": " + df.format(entry.getValue()) + "%\n";
		}
		return str;
	}
	
}
