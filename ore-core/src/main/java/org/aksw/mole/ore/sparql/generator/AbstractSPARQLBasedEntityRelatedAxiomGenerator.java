package org.aksw.mole.ore.sparql.generator;

import java.util.HashSet;
import java.util.Set;

import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;

public abstract class AbstractSPARQLBasedEntityRelatedAxiomGenerator<T extends OWLEntity> extends AbstractSPARQLBasedAxiomGenerator implements SPARQLBasedEntityRelatedAxiomGenerator<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractSPARQLBasedEntityRelatedAxiomGenerator.class);
	protected ParameterizedSparqlString queryTemplate;
	protected Set<T> allAxiomsLoaded = new HashSet<T>();
	
	public AbstractSPARQLBasedEntityRelatedAxiomGenerator(SparqlEndpointKS ks) {
		super(ks);
	}
	
	@Override
	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	public Set<OWLAxiom> nextAxioms(T entity) {
		return nextAxioms(entity, limit, cnt);
	}

	@Override
	public Set<OWLAxiom> nextAxioms(T entity, int limit) {
		return nextAxioms(entity, limit, cnt);
	}

	@Override
	public Set<OWLAxiom> nextAxioms(T entity, int limit, int offset) {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		if(hasNext(entity)){
			queryTemplate.setIri("var", entity.getIRI().toString());
			Query q = queryTemplate.asQuery();
			q.setLimit(limit);
			q.setOffset(offset);
			Model model = executeConstructQuery(q);
			OWLOntology ontology = convert(model);
			cnt += limit;
			if(model.size() < limit){
				allAxiomsLoaded.add(entity);
			}
			axioms.addAll(ontology.getLogicalAxioms());
		} else {
			logger.warn("All axioms already loaded. Skipping.");
		}
		return axioms;
	}
	
	@Override
	public boolean hasNext(T entity) {
		return allAxiomsLoaded.contains(entity);
	}
}
