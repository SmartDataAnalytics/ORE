package org.aksw.mole.ore.sparql.trivial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.sparql.AxiomGenerationTracker;
import org.aksw.mole.ore.sparql.InconsistencyFinder;
import org.aksw.mole.ore.sparql.TimeOutException;
import org.aksw.mole.ore.sparql.generator.AxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.OWLAxiom;

public class TrivialInconsistencyFinder implements InconsistencyFinder {
	
	private List<InconsistencyFinder> finders = new ArrayList<InconsistencyFinder>();
	private SparqlEndpointKS ks;
	private AxiomGenerationTracker tracker;
	
	public TrivialInconsistencyFinder(SparqlEndpointKS ks) {
		this.ks = ks;
		finders.add(new FunctionalityBasedInconsistencyFinder(ks));
		finders.add(new InverseFunctionalityBasedInconsistencyFinder(ks));
		finders.add(new AsymmetryBasedInconsistencyFinder(ks));
		finders.add(new IrreflexivityBasedInconsistencyFinder(ks));
		finders.add(new DisjointnessBasedInconsistencyFinder(ks));
	}
	
	@Override
	public Set<OWLAxiom> getInconsistentFragment() {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		for(InconsistencyFinder checker : finders){
			try {
				Set<OWLAxiom> inconsistentFragment = checker.getInconsistentFragment();
				axioms.addAll(inconsistentFragment);
				if(tracker != null){
					tracker.track(checker, axioms);
				}
			} catch (TimeOutException e) {
				e.printStackTrace();
			}
		}
		return axioms;
	}

	@Override
	public void setMaximumRuntime(long duration, TimeUnit timeUnit) {
	}

	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		for (InconsistencyFinder finder : finders) {
			finder.setAxiomsToIgnore(axiomsToIgnore);
		}
	}
	
	public void setAxiomGenerationTracker(AxiomGenerationTracker tracker){
		this.tracker = tracker;
	}

	@Override
	public int compareTo(AxiomGenerator other) {
		return getClass().getName().compareTo(other.getClass().getName());
	}

}
