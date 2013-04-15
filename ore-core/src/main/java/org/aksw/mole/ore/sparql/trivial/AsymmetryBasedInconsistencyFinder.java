package org.aksw.mole.ore.sparql.trivial;

import java.util.Set;
import java.util.TreeSet;

import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public class AsymmetryBasedInconsistencyFinder extends AbstractTrivialInconsistencyFinder {
	
	public AsymmetryBasedInconsistencyFinder(SparqlEndpointKS ks) {
		super(ks);
		
		query = "CONSTRUCT " +
				"{?p a <http://www.w3.org/2002/07/owl#AsymmetricProperty>. ?s ?p ?o. ?o ?p ?s.} " +
				"WHERE " +
				"{?p a <http://www.w3.org/2002/07/owl#AsymmetricProperty>. ?s ?p ?o. ?o ?p ?s.} ";
	}
	
	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		Set<OWLAxiom> asymmetryAxioms = AxiomType.getAxiomsOfTypes(axiomsToIgnore, AxiomType.ASYMMETRIC_OBJECT_PROPERTY);
		Set<OWLObjectProperty> properties = new TreeSet<OWLObjectProperty>();
		for (OWLAxiom axiom : asymmetryAxioms) {
			properties.addAll(axiom.getObjectPropertiesInSignature());
		}
		if(!properties.isEmpty()){
			filter = "FILTER(?p NOT IN(";
			for (OWLObjectProperty property : properties) {
				filter += "<" + property.toStringID() + ">" + ",";
			}
			filter = filter.substring(0, filter.length()-1);
			filter += "))";
		}
	}
}
