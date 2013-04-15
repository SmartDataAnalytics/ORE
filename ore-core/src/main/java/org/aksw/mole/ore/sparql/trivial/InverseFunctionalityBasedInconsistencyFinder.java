package org.aksw.mole.ore.sparql.trivial;

import java.util.Set;
import java.util.TreeSet;

import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public class InverseFunctionalityBasedInconsistencyFinder extends AbstractTrivialInconsistencyFinder {
	
	public InverseFunctionalityBasedInconsistencyFinder(SparqlEndpointKS ks) {
		super(ks);
		
		query = "CONSTRUCT " +
				"{?p a <http://www.w3.org/2002/07/owl#InverseFunctionalProperty>. ?s1 ?p ?o. ?s2 ?p ?o.} " +
				"WHERE " +
				"{?p a <http://www.w3.org/2002/07/owl#InverseFunctionalProperty>. ?s1 ?p ?o. ?s2 ?p ?o. FILTER(?s1 != ?s2)} ";
	}
	
	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		Set<OWLAxiom> functionalityAxioms = AxiomType.getAxiomsOfTypes(axiomsToIgnore, AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY);
		Set<OWLObjectProperty> properties = new TreeSet<OWLObjectProperty>();
		for (OWLAxiom axiom : functionalityAxioms) {
			properties.addAll(axiom.getObjectPropertiesInSignature());
		}
		if(!properties.isEmpty()){
			filter = "FILTER(?p NOT IN(";
			for (OWLObjectProperty property : properties) {
				filter += "<" + property.toStringID() + ">" + ",";
			}
			filter = filter.substring(0, filter.length()-1);
			filter += "))";
			System.out.println(filter);
		}
	}
}
