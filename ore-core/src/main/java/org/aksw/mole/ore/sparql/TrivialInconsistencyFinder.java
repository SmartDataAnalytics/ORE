package org.aksw.mole.ore.sparql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.OWLAxiom;

public class TrivialInconsistencyFinder implements InconsistencyFinder{
	
	private SparqlEndpoint endpoint;
	private ExtractionDBCache cache;
	
	private List<ConsistencyViolationsChecker> checkerList = new ArrayList<ConsistencyViolationsChecker>();
	
	public TrivialInconsistencyFinder(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		this.endpoint = endpoint;
		this.cache = cache;
		
		checkerList.add(new FunctionalityViolationsChecker(endpoint, cache));
	}
	
	public TrivialInconsistencyFinder(SparqlEndpoint endpoint) {
		this(endpoint, null);
	}
	
	@Override
	public Set<OWLAxiom> getInconsistentFragment() {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		for(ConsistencyViolationsChecker checker : checkerList){
			axioms.addAll(checker.getViolatingAxioms());
		}
		return axioms;
	}

}
