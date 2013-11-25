package org.aksw.mole.ore.sparql.trivial;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.mole.ore.sparql.TimeOutException;
import org.apache.log4j.Logger;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLProperty;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class OptimizedFunctionalityBasedInconsistencyFinder extends AbstractTrivialInconsistencyFinder {
	
	
	private static final Logger logger = Logger.getLogger(OptimizedFunctionalityBasedInconsistencyFinder.class
			.getName());
	
	private Set<OWLDataProperty> candidates;
	private boolean initialized = false;
	private ParameterizedSparqlString template;

	public OptimizedFunctionalityBasedInconsistencyFinder(SparqlEndpointKS ks) {
		super(ks);
		
//		template = new ParameterizedSparqlString(
//				"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
//				"CONSTRUCT " +
//				"{?s ?p ?o1. ?s ?p ?o2.} " +
//				"WHERE " +
//				"{?s ?p ?o1. ?s ?p ?o2. FILTER(?o1 != ?o2)} ");
		
		template = new ParameterizedSparqlString("SELECT * WHERE {?s ?p ?o1. ?s ?p ?o2. FILTER(?o1 != ?o2)}");
	}
	
	private void init() {
		if(!initialized){
			logger.info("Generating functionality violation candidates...");
			fireMessage("Analyzing functionality...");
			candidates = new TreeSet<>();
			String query = "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
					+ "SELECT ?p WHERE {?p a owl:FunctionalProperty. ?p a owl:DatatypeProperty.}";
			QueryExecution qe = qef.createQueryExecution(query);
			ResultSet rs = qe.execSelect();
			QuerySolution qs;
			while (rs.hasNext()) {
				qs = rs.next();
				candidates.add(dataFactory.getOWLDataProperty(IRI.create(qs.getResource("p").getURI())));
			}
			qe.close();
			initialized = true;
			logger.info("Found " + candidates.size() + " candidates.");
			fireMessage("Found " + candidates.size() + " possible candidates for conflicts.");
		}
	}
	
	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		Set<OWLAxiom> functionalityAxioms = AxiomType.getAxiomsOfTypes(axiomsToIgnore, AxiomType.FUNCTIONAL_OBJECT_PROPERTY, AxiomType.FUNCTIONAL_DATA_PROPERTY);
		Set<OWLProperty> properties = new TreeSet<OWLProperty>();
		for (OWLAxiom axiom : functionalityAxioms) {
			properties.addAll(axiom.getObjectPropertiesInSignature());
			properties.addAll(axiom.getDataPropertiesInSignature());
		}
		if(!properties.isEmpty()){
			filter = "FILTER(?p NOT IN(";
			for (OWLProperty property : properties) {
				filter += "<" + property.toStringID() + ">" + ",";
			}
			filter = filter.substring(0, filter.length()-1);
			filter += "))";
		}
	}
	
	@Override
	public Set<OWLAxiom> getInconsistentFragment() throws TimeOutException {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		init();
		for (Iterator<OWLDataProperty> iterator = candidates.iterator(); iterator.hasNext();) {
			OWLDataProperty prop = iterator.next();
			logger.info("Analyzing candidate: " + prop);
			fireMessage("Checking " + prop);
			template.clearParams();
			template.setIri("p", prop.toStringID());
			
			Query query = template.asQuery();
//			Model model = executeConstructQuery(query);
//			if(!model.isEmpty()){
//				model.add(model.getResource(prop.toStringID()), RDF.type, OWL.DatatypeProperty);
//				OWLOntology ontology = convert(model);
//				axioms.addAll(ontology.getLogicalAxioms());
//				axioms.add(dataFactory.getOWLFunctionalDataPropertyAxiom(prop));
//			}
			QueryExecution qe = qef.createQueryExecution(query);
			ResultSet rs = qe.execSelect();
			logger.info("done");
			QuerySolution qs;
			OWLIndividual subject;
			OWLLiteral object1;
			OWLLiteral object2;
			OWLAxiom ax1;
			OWLAxiom ax2;
			Set<OWLAxiom> justification;
			while (rs.hasNext()) {
				qs = rs.next();
				subject = dataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("s").getURI()));
				object1 = getOWLLiteral(qs.getLiteral("o1"));
				object2 = getOWLLiteral(qs.getLiteral("o2"));
				if(!object1.equals(object2)){
					ax1 = dataFactory.getOWLDataPropertyAssertionAxiom(prop, subject, object1);
					ax2 = dataFactory.getOWLDataPropertyAssertionAxiom(prop, subject, object2);
					justification = Sets.newHashSet(ax1, ax2, dataFactory.getOWLFunctionalDataPropertyAxiom(prop));
//					axioms.addAll(justification);
					explanations.add(new Explanation<OWLAxiom>(inconsistencyEntailment, justification));
				}
			}
			qe.close();
			iterator.remove();
			if(stopIfInconsistencyFound && !explanations.isEmpty()){
				break;
			}
		}

		return axioms;
	}
}
