package org.aksw.mole.ore.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class IrreflexivityConsistencyValidator extends SPARQLConsistencyValidator<IrreflexivityViolation, OWLObjectProperty>{
	
	public IrreflexivityConsistencyValidator(Model model) {
		super(model);
		queryTemplate = new ParameterizedSparqlString(
				"SELECT * WHERE {" +
				"?s ?p ?s." +
				"}"
				);
		countQueryTemplate = new ParameterizedSparqlString(
				"SELECT (COUNT(*) AS ?cnt) WHERE {" +
				"?s ?p ?s." +
				"}"
				);
	}
	
	public IrreflexivityConsistencyValidator(SparqlEndpoint endpoint) {
		this(endpoint, null);
	}
	
	public IrreflexivityConsistencyValidator(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		super(endpoint, cache);
		queryTemplate = new ParameterizedSparqlString(
				"SELECT * WHERE {" +
				"?s ?p ?s." +
				"}"
				);
		countQueryTemplate = new ParameterizedSparqlString(
				"SELECT (COUNT(*) AS ?cnt) WHERE {" +
				"?s ?p ?s." +
				"}"
				);
	}
	
	public Collection<IrreflexivityViolation> getViolations(OWLObjectProperty op){
		queryTemplate.clearParams();
		queryTemplate.setIri("p", op.toStringID());
		
		Set<IrreflexivityViolation> violations = new HashSet<IrreflexivityViolation>();
		
		ResultSet rs = executeSelect(queryTemplate.asQuery());
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			violations.add(new IrreflexivityViolation(op, new OWLNamedIndividualImpl(IRI.create(qs.getResource("s").getURI()))));
		}
		return violations;
	}

}
