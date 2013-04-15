package org.aksw.mole.ore.sparql.generator;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

public interface SPARQLBasedGeneralAxiomGenerator extends AxiomGenerator{
	
	void setLimit(int limit);

	Set<OWLAxiom> nextAxioms();

	Set<OWLAxiom> nextAxioms(int limit);

	Set<OWLAxiom> nextAxioms(int limit, int offset);
	
	boolean hasNext();
}
