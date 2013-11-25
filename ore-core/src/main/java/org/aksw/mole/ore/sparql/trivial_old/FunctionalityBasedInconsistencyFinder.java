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
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class FunctionalityBasedInconsistencyFinder extends AbstractTrivialInconsistencyFinder {
	
	private boolean initialized;
	private Set<OWLDataProperty> propertyCandidates;
	
	private ParameterizedSparqlString template = new ParameterizedSparqlString("SELECT * WHERE {?s ?p ?o1. ?s ?p ?o2. FILTER(?o1 != ?o2)}");

	public FunctionalityBasedInconsistencyFinder(SparqlEndpointKS ks) {
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
	
	private Set<OWLDataProperty> generatePropertyCandidates(){
		fireTraceMessage("Searching for property candidates...");
		Set<OWLDataProperty> candidates = new TreeSet<>();
		
		String query = "SELECT ?p WHERE {?p a <http://www.w3.org/2002/07/owl#FunctionalProperty>. ?p a <http://www.w3.org/2002/07/owl#DatatypeProperty>.}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			candidates.add(dataFactory.getOWLDataProperty(IRI.create(qs.getResource("p").getURI())));
		}
		qe.close();
		fireTraceMessage("Found " + candidates.size() + " property candidates.");
		return candidates;
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
		explanations = new HashSet<>();
		fireInfoMessage("Analyzing functionality...");
		init();
		
		int i = 0;
		int total = propertyCandidates.size();
		for (Iterator<OWLDataProperty> iterator = propertyCandidates.iterator(); iterator.hasNext();) {
			OWLDataProperty property = iterator.next();
			if(!terminationCriteriaSatisfied()){
				fireTraceMessage("Checking " + property.toStringID());
				template.setIri("p", property.toStringID());
				Query query = template.asQuery();
				QueryExecution qe = qef.createQueryExecution(query);
				ResultSet rs = qe.execSelect();
				QuerySolution qs;
				OWLIndividual subject;
				OWLLiteral object1;
				OWLLiteral object2;
				OWLAxiom ax1;
				OWLAxiom ax2;
				Set<OWLAxiom> justification;
				int cnt = 0;
				while (rs.hasNext()) {
					qs = rs.next();
					subject = dataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("s").getURI()));
					object1 = getOWLLiteral(qs.getLiteral("o1"));
					object2 = getOWLLiteral(qs.getLiteral("o2"));
					if(!object1.equals(object2)){
						ax1 = dataFactory.getOWLDataPropertyAssertionAxiom(property, subject, object1);
						ax2 = dataFactory.getOWLDataPropertyAssertionAxiom(property, subject, object2);
						justification = Sets.newHashSet(ax1, ax2, dataFactory.getOWLFunctionalDataPropertyAxiom(property));
						explanations.add(new Explanation<OWLAxiom>(inconsistencyEntailment, justification));
						cnt++;
					}
				}
				qe.close();
				iterator.remove();
				fireProgressUpdate(++i, total);
				fireTraceMessage("Found " + cnt + " conflicts.");
			}
		}
		allExplanations.addAll(explanations);
	}
}
