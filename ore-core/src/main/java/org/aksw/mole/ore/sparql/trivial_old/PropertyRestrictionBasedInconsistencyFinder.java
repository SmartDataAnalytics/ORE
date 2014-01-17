package org.aksw.mole.ore.sparql.trivial_old;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.mole.ore.sparql.TimeOutException;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class PropertyRestrictionBasedInconsistencyFinder extends AbstractTrivialInconsistencyFinder {
	
	private boolean initialized;
	private Set<OWLEntity> propertyCandidates;
	
	private ParameterizedSparqlString template = new ParameterizedSparqlString("SELECT * WHERE {?s ?p ?o1. ?s ?p ?o2. FILTER(?o1 != ?o2)}");

	public PropertyRestrictionBasedInconsistencyFinder(SparqlEndpointKS ks) {
		super(ks);
	}
	
	/**
	 * @param ks
	 */
	public PropertyRestrictionBasedInconsistencyFinder(SparqlEndpointKS ks, Set<Explanation<OWLAxiom>> explanations) {
		super(ks, explanations);
	}
	
	private void init(){
		if(!initialized){
			
			initialized = true;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.AbstractTrivialInconsistencyFinder#getInconsistentFragment()
	 */
	@Override
	public Set<OWLAxiom> getInconsistentFragment() throws TimeOutException {
		return null;
		
	}
	
	private void detectZeroCardinalityRestrictions(){
		String query = "PREFIX owl:<http://www.w3.org/2002/07/owl#> "
				+ "SELECT * WHERE {?x owl:cardinality \"0\"^^<http://www.w3.org/2001/XMLSchema#integer> . ?x owl:onProperty ?p .?u a ?x .?u ?p ?y .}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			System.out.println(qs.getResource("u").getURI());
		}
		qe.close();
	}
	
	private void detectZeroMaxCardinalityRestrictions(){
		String query = "PREFIX owl:<http://www.w3.org/2002/07/owl#> "
				+ "SELECT * WHERE {?x owl:maxCardinality \"0\"^^<http://www.w3.org/2001/XMLSchema#integer> . ?x owl:onProperty ?p .?u a ?x .?u ?p ?y .}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			System.out.println(qs.getResource("u").getURI());
		}
		qe.close();
	}
	
	
	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		Set<OWLAxiom> asymmetryAxioms = AxiomType.getAxiomsOfTypes(axiomsToIgnore, AxiomType.FUNCTIONAL_DATA_PROPERTY);
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

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.AbstractTrivialInconsistencyFinder#run()
	 */
	@Override
	public void run(boolean resume) {
		fireInfoMessage("Analyzing property restrictions...");
		init();
		
		detectZeroCardinalityRestrictions();
	}
}
