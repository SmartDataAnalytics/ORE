package org.aksw.mole.ore.sparql.generator;

import org.semanticweb.owlapi.model.AxiomType;

public interface SPARQLBasedAxiomGenerator extends AxiomGenerator {

	AxiomType getAxiomType();

	void setLimit(int limit);

}
