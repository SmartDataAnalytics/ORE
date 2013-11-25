package org.aksw.mole.ore.sparql.trivial_old;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.mole.ore.sparql.TimeOutException;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class InverseFunctionalityBasedInconsistencyFinder extends AbstractTrivialInconsistencyFinder {
	
	private boolean initialized;
	private Set<OWLObjectProperty> propertyCandidates;
	
	private ParameterizedSparqlString template = new ParameterizedSparqlString("SELECT * WHERE {?s1 ?p ?o. ?s2 ?p ?o. FILTER(?s1 != ?s2)}");

	public InverseFunctionalityBasedInconsistencyFinder(SparqlEndpointKS ks) {
		super(ks);
	}
	
	private void init(){
		if(!initialized){
			propertyCandidates = generatePropertyCandidates();
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
	
	private Set<OWLObjectProperty> generatePropertyCandidates(){
		fireTraceMessage("Searching for property candidates...");
		Set<OWLObjectProperty> candidates = new TreeSet<>();
		
		String query = "SELECT ?p WHERE {?p a <http://www.w3.org/2002/07/owl#InverseFunctionalProperty>.}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			candidates.add(dataFactory.getOWLObjectProperty(IRI.create(qs.getResource("p").getURI())));
		}
		qe.close();
		fireTraceMessage("Found " + candidates.size() + " property candidates.");
		return candidates;
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

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.AbstractTrivialInconsistencyFinder#run()
	 */
	@Override
	public void run(boolean resume) {
		explanations = new HashSet<>();
		fireInfoMessage("Analyzing inverse functionality...");
		init();
		
		int i = 0;
		int total = propertyCandidates.size();
		for (Iterator<OWLObjectProperty> iterator = propertyCandidates.iterator(); iterator.hasNext();) {
			if(!terminationCriteriaSatisfied()){
				OWLObjectProperty property = iterator.next();
				fireTraceMessage("Checking " + property.toStringID());
				
				template.setIri("p", property.toStringID());
				Query query = template.asQuery();
				QueryExecution qe = qef.createQueryExecution(query);
				ResultSet rs = qe.execSelect();
				QuerySolution qs;
				OWLIndividual subject1;
				OWLIndividual subject2;
				OWLIndividual object;
				OWLAxiom ax1;
				OWLAxiom ax2;
				OWLAxiom ax3;
				Set<OWLAxiom> justification;
				while (rs.hasNext()) {
					qs = rs.next();
					subject1 = dataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("s1").getURI()));
					subject2 = dataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("s2").getURI()));
					object = dataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("o").getURI()));
					ax1 = dataFactory.getOWLObjectPropertyAssertionAxiom(property, subject1, object);
					ax2 = dataFactory.getOWLObjectPropertyAssertionAxiom(property, subject2, object);
					ax3 = dataFactory.getOWLDifferentIndividualsAxiom(subject1, subject2);
					justification = Sets.newHashSet(ax1, ax2, ax3, dataFactory.getOWLInverseFunctionalObjectPropertyAxiom(property));
					explanations.add(new Explanation<OWLAxiom>(inconsistencyEntailment, justification));
				}
				qe.close();
				iterator.remove();
				fireProgressUpdate(++i, total);
			}
		}
		allExplanations.addAll(explanations);
	}
}
