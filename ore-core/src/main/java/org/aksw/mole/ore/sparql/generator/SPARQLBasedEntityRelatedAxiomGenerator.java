package org.aksw.mole.ore.sparql.generator;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

public interface SPARQLBasedEntityRelatedAxiomGenerator<T extends OWLEntity> extends AxiomGenerator{
	
	void setLimit(int limit);

	Set<OWLAxiom> nextAxioms(T entity);

	Set<OWLAxiom> nextAxioms(T entity, int limit);

	Set<OWLAxiom> nextAxioms(T entity, int limit, int offset);
	
	boolean hasNext(T entity);
}
