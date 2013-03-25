package org.aksw.mole.ore.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.dllearner.core.owl.DatatypeProperty;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.KBElement;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.core.owl.Property;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class FunctionalityConsistencyValidator extends SPARQLConsistencyValidator<FunctionalityViolation, Property>{
	
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
	public long getNumberOfViolations(Property p) {
		//Should always be divided by 2, because result is always redundant
		return super.getNumberOfViolations(p)/2;
	}
	
	public Collection<FunctionalityViolation> getViolations(Property p){
		ParameterizedSparqlString tmp = null;
		if(literalAwareQueryTemplate != null && isXSDDateRange(p)){
			tmp = queryTemplate;
			queryTemplate = literalAwareQueryTemplate;
		}
		queryTemplate.clearParams();
		queryTemplate.setIri("p", p.getURI().toString());
		
		Set<FunctionalityViolation> violations = new HashSet<FunctionalityViolation>();
		
		ResultSet rs = executeSelect(queryTemplate.asQuery());
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			Individual subject = new Individual(qs.getResource("s").getURI());
			KBElement object1 = parseNode(qs.get("o1"));
			KBElement object2 = parseNode(qs.get("o2"));
			
			violations.add(new FunctionalityViolation(p, subject, object1, object2));
		}
		if(tmp != null){
			queryTemplate = tmp;
		}
		return violations;
	}
	
	public static void main(String[] args) {
		SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();
		ObjectProperty op = new ObjectProperty("http://dbpedia.org/ontology/productionStartDate");
		FunctionalityConsistencyValidator validator = new FunctionalityConsistencyValidator(endpoint, new ExtractionDBCache("cache"));
		System.out.println("Consistent: " + validator.isConsistent(op));
		Collection<FunctionalityViolation> violations = validator.getViolations(op);
		System.out.println(violations.size());
		for(FunctionalityViolation v : violations){
			System.out.println(v);
		}
		System.out.println(validator.getNumberOfViolations(op));
		
		DatatypeProperty dp = new DatatypeProperty("http://dbpedia.org/ontology/closed");
		System.out.println("Consistent: " + validator.isConsistent(dp));
		violations = validator.getViolations(dp);
		System.out.println(violations.size());
		for(FunctionalityViolation v : violations){
			System.out.println(v);
		}
		
//		System.out.println("Consistent: " + validator.isConsistent(op));
//		violations = validator.getViolations(op);
//		System.out.println(violations.size());
//		for(FunctionalityViolation v : violations){
//			System.out.println(v);
//		}
	}

}
