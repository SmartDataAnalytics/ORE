package org.aksw.mole.ore.sparql;

import java.util.Set;

import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class FunctionalityViolationsChecker extends ConsistencyViolationsChecker{
	
	
	private static final Logger logger = Logger.getLogger(FunctionalityViolationsChecker.class.getName());
	
	public FunctionalityViolationsChecker(SparqlEndpoint endpoint) {
		super(endpoint);
	}
	
	public FunctionalityViolationsChecker(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		super(endpoint, cache);
	}

	@Override
	public Set<OWLAxiom> getViolatingAxioms() {
		logger.info("Checking for functionality violations...");
		
		String query = "SELECT * WHERE {?p a <http://www.w3.org/2002/07/owl#FunctionalProperty>. ?s ?p ?o1. ?s ?p ?o2. FILTER(?o1 != ?o2)}";
		ResultSet rs = executeSelect(query);
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			RDFNode subject = qs.get("s");
			RDFNode predicate = qs.get("p");
			RDFNode object1 = qs.get("o1");
			RDFNode object2 = qs.get("o2");
			
			OWLIndividual subj = dataFactory.getOWLNamedIndividual(IRI.create(subject.toString()));
			if(object1.isURIResource() && object2.isURIResource()){
				OWLObjectProperty property = dataFactory.getOWLObjectProperty(IRI.create(predicate.toString()));
				OWLIndividual obj1 = dataFactory.getOWLNamedIndividual(IRI.create(object1.toString()));
				OWLIndividual obj2 = dataFactory.getOWLNamedIndividual(IRI.create(object2.toString()));
				violatingAxioms.add(dataFactory.getOWLObjectPropertyAssertionAxiom(property, subj, obj1));
				violatingAxioms.add(dataFactory.getOWLObjectPropertyAssertionAxiom(property, subj, obj2));
			} else if(object1.isLiteral() && object2.isLiteral()){
				OWLDataProperty property = dataFactory.getOWLDataProperty(IRI.create(predicate.toString()));
				OWLLiteral obj1 = getOWLLiteral(object1.asLiteral());
				OWLLiteral obj2 = getOWLLiteral(object2.asLiteral());
				violatingAxioms.add(dataFactory.getOWLDataPropertyAssertionAxiom(property, subj, obj1));
				violatingAxioms.add(dataFactory.getOWLDataPropertyAssertionAxiom(property, subj, obj2));
			} else {
				logger.warn("Ignoring triple " + qs);
			}
		}
		
		return violatingAxioms;
	}
	
	
}
