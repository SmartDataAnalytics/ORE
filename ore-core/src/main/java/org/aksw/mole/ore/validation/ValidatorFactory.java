package org.aksw.mole.ore.validation;

import static org.semanticweb.owlapi.model.AxiomType.ASYMMETRIC_OBJECT_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.FUNCTIONAL_DATA_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.FUNCTIONAL_OBJECT_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.IRREFLEXIVE_OBJECT_PROPERTY;

import org.dllearner.core.owl.Property;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

public class ValidatorFactory {
	
	private SparqlEndpoint endpoint;
	private ExtractionDBCache cache;
	
	public ValidatorFactory(SparqlEndpoint endpoint) {
		this(endpoint, null);
	}
	
	public ValidatorFactory(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		this.endpoint = endpoint;
		this.cache = cache;
	}
	
	public SPARQLConsistencyValidator<? extends Violation, ? extends Property> createValidator(AxiomType<? extends OWLAxiom> axiomType){
		if(axiomType == FUNCTIONAL_OBJECT_PROPERTY || axiomType == FUNCTIONAL_DATA_PROPERTY){
			return new FunctionalityConsistencyValidator(endpoint, cache);
		} else if(axiomType == INVERSE_FUNCTIONAL_OBJECT_PROPERTY){
			return new InverseFunctionalityConsistencyValidator(endpoint, cache);
		} else if(axiomType == ASYMMETRIC_OBJECT_PROPERTY){
			return new AsymmetryConsistencyValidator(endpoint, cache);
		} else if(axiomType == IRREFLEXIVE_OBJECT_PROPERTY){
			return new IrreflexivityConsistencyValidator(endpoint, cache);
		} else {
			throw new IllegalArgumentException("Axiom type " + axiomType.getName() + " is not supported.");
		}
	}

}
