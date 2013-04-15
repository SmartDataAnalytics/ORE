package org.aksw.mole.ore.sparql.trivial;

import java.util.List;
import java.util.Set;

import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;

public class DisjointnessBasedInconsistencyFinder extends AbstractTrivialInconsistencyFinder {
	
	public DisjointnessBasedInconsistencyFinder(SparqlEndpointKS ks) {
		super(ks);
		
		query = "CONSTRUCT " +
				"{?cls1 <http://www.w3.org/2002/07/owl#disjointWith> ?cls2. ?s a ?cls1. ?s a ?cls2.} " +
				"WHERE " +
				"{?cls1 <http://www.w3.org/2002/07/owl#disjointWith> ?cls2. ?s a ?cls1. ?s a ?cls2.} ";
	}
	
	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		Set<OWLAxiom> disjointnessAxioms = AxiomType.getAxiomsOfTypes(axiomsToIgnore, AxiomType.DISJOINT_CLASSES);
		if(!disjointnessAxioms.isEmpty()){
			filter = "FILTER(";
			String s = "";
			for (OWLAxiom axiom : disjointnessAxioms) {
				if(!s.isEmpty()){
					s += " && ";
				}
				for (OWLDisjointClassesAxiom dis : ((OWLDisjointClassesAxiom)axiom).asPairwiseAxioms()) {
					List<OWLClassExpression> operands = dis.getClassExpressionsAsList();
					OWLClass cls1 = operands.get(0).asOWLClass();
					OWLClass cls2 = operands.get(1).asOWLClass();
					s += "(";
					s += "(?cls1!=<" + cls1.toStringID() + "> && ?cls2!= <" + cls2.toStringID() + ">)";
					s += " || ";
					s += "(?cls1!=<" + cls2.toStringID() + "> && ?cls2!= <" + cls1.toStringID() + ">)";
					s += ")";
				}
			}
			filter += s;
			filter += ")";
			System.out.println(filter);
		}
	}

	
}
