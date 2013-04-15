package org.aksw.mole.ore.sparql.trivial;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.sparql.InconsistencyFinder;
import org.aksw.mole.ore.sparql.TimeOutException;
import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;

public class AbstractTrivialInconsistencyFinder extends AbstractSPARQLBasedAxiomGenerator implements InconsistencyFinder {
	
	protected String query;
	protected String filter = "";
	protected Map<String, Integer> filterToOffset = new HashMap<String, Integer>();
	
	public AbstractTrivialInconsistencyFinder(SparqlEndpointKS ks) {
		super(ks);
		filterToOffset.put("", 0);
	}

	@Override
	public Set<OWLAxiom> getInconsistentFragment() throws TimeOutException {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		//add filter if exist
		String queryString = query;
		if(filter != null && !filter.isEmpty()){
			queryString = query.trim();
			queryString = query.substring(0, query.lastIndexOf('}'));
			queryString += filter + "}";
		}
		System.out.println(queryString);
		Query q = QueryFactory.create(queryString);
		q.setLimit(limit);
		int offset = filterToOffset.containsKey(filter) ? filterToOffset.get(filter) : 0;
		q.setOffset(offset);
		Model model = executeConstructQuery(q);
		OWLOntology ontology = convert(model);
		axioms.addAll(ontology.getLogicalAxioms());
		filterToOffset.put(filter, offset+limit);
		return axioms;
	}

	@Override
	public void setMaximumRuntime(long duration, TimeUnit timeUnit) {
	}

	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
	}
}
