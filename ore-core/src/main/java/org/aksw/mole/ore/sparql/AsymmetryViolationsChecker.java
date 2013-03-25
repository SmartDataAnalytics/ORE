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

public class AsymmetryViolationsChecker extends ConsistencyViolationsChecker{
	
	
	private static final Logger logger = Logger.getLogger(AsymmetryViolationsChecker.class.getName());
	
	public AsymmetryViolationsChecker(SparqlEndpoint endpoint) {
		super(endpoint);
	}
	
	public AsymmetryViolationsChecker(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		super(endpoint, cache);
	}

	@Override
	public Set<OWLAxiom> getViolatingAxioms() {
		logger.info("Checking for functionality violations...");
		
		String query = "SELECT * WHERE {?p a <http://www.w3.org/2002/07/owl#AsymmetricProperty>. ?s ?p ?o. ?o ?p ?s. }";
		ResultSet rs = executeSelect(query);
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			RDFNode subject = qs.get("s");
			RDFNode predicate = qs.get("p");
			RDFNode object = qs.get("o");
			
			OWLObjectProperty property = dataFactory.getOWLObjectProperty(IRI.create(predicate.toString()));
			OWLIndividual subj = dataFactory.getOWLNamedIndividual(IRI.create(subject.toString()));
			OWLIndividual obj = dataFactory.getOWLNamedIndividual(IRI.create(object.toString()));
			violatingAxioms.add(dataFactory.getOWLObjectPropertyAssertionAxiom(property, subj, obj));
			violatingAxioms.add(dataFactory.getOWLObjectPropertyAssertionAxiom(property, obj, subj));
			violatingAxioms.add(dataFactory.getOWLAsymmetricObjectPropertyAxiom(property));
		}
		
		return violatingAxioms;
	}
	
	
}
