package org.aksw.mole.ore.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLProperty;

import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class FunctionalityConsistencyValidator extends SPARQLConsistencyValidator<FunctionalityViolation, OWLProperty>{
	
	public FunctionalityConsistencyValidator(Model model) {
		super(model);
		queryTemplate = new ParameterizedSparqlString(
				"SELECT * WHERE {" +
				"?s ?p ?o1." +
				"?s ?p ?o2." +	
				"FILTER(?o1 != ?o2)" +
				"}"
				);
		countQueryTemplate = new ParameterizedSparqlString(
				"SELECT (COUNT(*) AS ?cnt) WHERE {" +
				"?s ?p ?o1." +
				"?s ?p ?o2." +	
				"FILTER(?o1 != ?o2)" +
				"}"
				);
	}
	
	public FunctionalityConsistencyValidator(SparqlEndpoint endpoint) {
		this(endpoint, null);
	}
	
	public FunctionalityConsistencyValidator(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		super(endpoint, cache);
		queryTemplate = new ParameterizedSparqlString(
				"SELECT * WHERE {" +
				"?s ?p ?o1." +
				"?s ?p ?o2." +	
				"FILTER(?o1 != ?o2)" +
				"}"
				);
		countQueryTemplate = new ParameterizedSparqlString(
				"SELECT (COUNT(*) AS ?cnt) WHERE {" +
				"?s ?p ?o1." +
				"?s ?p ?o2." +	
				"FILTER(?o1 != ?o2)" +
				"}"
				);
		literalAwareQueryTemplate = new ParameterizedSparqlString(
				"SELECT * WHERE {" +
				"?s ?p ?o1." +
				"?s ?p ?o2." +	
				"FILTER(STR(?o1) != STR(?o2))" +
				"}"
				);
		literalAwareCountQueryTemplate = new ParameterizedSparqlString(
				"SELECT (COUNT(*) AS ?cnt) WHERE {" +
				"?s ?p ?o1." +
				"?s ?p ?o2." +	
				"FILTER(STR(?o1) != STR(?o2))" +
				"}"
				);
		
	}
	
	@Override
	public long getNumberOfViolations(OWLProperty p) {
		//Should always be divided by 2, because result is always redundant
		return super.getNumberOfViolations(p)/2;
	}
	
	@Override
	public Collection<FunctionalityViolation> getViolations(OWLProperty p){
		ParameterizedSparqlString tmp = null;
		if(literalAwareQueryTemplate != null && isXSDDateRange(p)){
			tmp = queryTemplate;
			queryTemplate = literalAwareQueryTemplate;
		}
		queryTemplate.clearParams();
		queryTemplate.setIri("p", p.toStringID());
		
		Set<FunctionalityViolation> violations = new HashSet<FunctionalityViolation>();
		
		ResultSet rs = executeSelect(queryTemplate.asQuery());
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			OWLIndividual subject = new OWLNamedIndividualImpl(IRI.create(qs.getResource("s").getURI()));
			OWLObject object1 = parseNode(qs.get("o1"));
			OWLObject object2 = parseNode(qs.get("o2"));
			
			violations.add(new FunctionalityViolation(p, subject, object1, object2));
		}
		if(tmp != null){
			queryTemplate = tmp;
		}
		return violations;
	}

}
