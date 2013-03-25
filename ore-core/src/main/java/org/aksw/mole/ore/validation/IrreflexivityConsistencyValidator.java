package org.aksw.mole.ore.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class IrreflexivityConsistencyValidator extends SPARQLConsistencyValidator<IrreflexivityViolation, ObjectProperty>{
	
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
	
	public Collection<IrreflexivityViolation> getViolations(ObjectProperty op){
		queryTemplate.clearParams();
		queryTemplate.setIri("p", op.getURI().toString());
		
		Set<IrreflexivityViolation> violations = new HashSet<IrreflexivityViolation>();
		
		ResultSet rs = executeSelect(queryTemplate.asQuery());
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			violations.add(new IrreflexivityViolation(op, new Individual(qs.getResource("s").getURI())));
		}
		return violations;
	}
	
	public static void main(String[] args) {
		SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();
		ObjectProperty op = new ObjectProperty("http://dbpedia.org/ontology/bandMember");
		IrreflexivityConsistencyValidator validator = new IrreflexivityConsistencyValidator(endpoint);
		System.out.println("Consistent: " + validator.isConsistent(op));
		Collection<IrreflexivityViolation> violations = validator.getViolations(op);
		System.out.println(violations.size());
		for(IrreflexivityViolation v : violations){
			System.out.println(v);
		}
	}

}
