package org.aksw.mole.ore.sparql.generator;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.dllearner.core.AbstractAxiomLearningAlgorithm;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;

public abstract class AbstractSPARQLBasedGeneralAxiomGenerator extends AbstractSPARQLBasedAxiomGenerator implements SPARQLBasedGeneralAxiomGenerator{
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractAxiomLearningAlgorithm.class);
	protected String query;
	protected boolean allAxiomsLoaded = false;//if all axioms were loaded
	protected Map<Long, Set<OWLAxiom>> generatedAxioms = new LinkedHashMap<Long, Set<OWLAxiom>>();
	
	public AbstractSPARQLBasedGeneralAxiomGenerator(SparqlEndpointKS ks) {
		super(ks);
	}

	@Override
	public Set<OWLAxiom> nextAxioms() {
		return nextAxioms(limit, cnt);
	}

	@Override
	public Set<OWLAxiom> nextAxioms(int limit) {
		return nextAxioms(limit, cnt);
	}

	@Override
	public Set<OWLAxiom> nextAxioms(int limit, int offset) {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		if(hasNext()){
			Query q = QueryFactory.create(query);
			q.setLimit(limit);
			q.setOffset(offset);
			Model model = executeConstructQuery(q);
			OWLOntology ontology = convert(model);
			cnt += limit;
			if(model.size() < limit){
				allAxiomsLoaded = true;
			}
			axioms.addAll(ontology.getLogicalAxioms());
		} else {
			logger.warn("All axioms already loaded. Skipping.");
		}
		//tracking of generated axioms
		if(!axioms.isEmpty()){
			generatedAxioms.put(System.currentTimeMillis(), axioms);
		}
		return axioms;
	}
	
	@Override
	public boolean hasNext() {
		return !allAxiomsLoaded;
	}
	
	/**
	 * Returns the earliest time point(in milliseconds) when the axiom was generated, otherwise NULL.
	 * @param axiom
	 * @return
	 */
	public Long generatedAt(OWLAxiom axiom){
		for (Entry<Long, Set<OWLAxiom>> entry : generatedAxioms.entrySet()) {
			Long timepoint = entry.getKey();
			Set<OWLAxiom> axioms = entry.getValue();
			if(axioms.contains(axiom)){
				return timepoint;
			}
		}
		return null;
	}
}
