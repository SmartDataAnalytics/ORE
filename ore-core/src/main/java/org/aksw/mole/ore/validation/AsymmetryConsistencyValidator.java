package org.aksw.mole.ore.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.core.owl.Property;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class AsymmetryConsistencyValidator extends SPARQLConsistencyValidator<AsymmetryViolation, ObjectProperty>{
	
	private boolean ignoreReflexivity = false;
	
	public AsymmetryConsistencyValidator(Model model) {
		super(model);
		queryTemplate = new ParameterizedSparqlString(
				"SELECT * WHERE {" +
				"?s ?p ?o." +
				"?o ?p ?s." +
				"}"
				);
		countQueryTemplate = new ParameterizedSparqlString(
				"SELECT (COUNT(*) AS ?cnt) WHERE {" +
				"?s ?p ?o." +
				"?o ?p ?s." +	
				"}"
				);
	}
	
	public AsymmetryConsistencyValidator(SparqlEndpoint endpoint) {
		this(endpoint, null);
	}
	
	public AsymmetryConsistencyValidator(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		super(endpoint, cache);
		queryTemplate = new ParameterizedSparqlString(
				"SELECT * WHERE {" +
				"?s ?p ?o." +
				"?o ?p ?s." +
				"}"
				);
		countQueryTemplate = new ParameterizedSparqlString(
				"SELECT (COUNT(*) AS ?cnt) WHERE {" +
				"?s ?p ?o." +
				"?o ?p ?s." +	
				"}"
				);
	}
	
	public Collection<AsymmetryViolation> getViolations(ObjectProperty op){
		queryTemplate.clearParams();
		queryTemplate.setIri("p", op.getURI().toString());
		
		Set<AsymmetryViolation> violations = new HashSet<AsymmetryViolation>();
		
		ResultSet rs = executeSelect(queryTemplate.asQuery());
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			violations.add(new AsymmetryViolation(op, new Individual(qs.getResource("s").getURI()), new Individual(qs.getResource("o").getURI())));
		}
		return violations;
	}
	
	@Override
	public long getNumberOfViolations(Property p){
		//count number of reflexive triples
		ParameterizedSparqlString s = new ParameterizedSparqlString("SELECT (COUNT(*) AS ?cnt) WHERE {?s ?p ?s.}");
		s.setIri("p", p.getURI().toString());
		Query q = s.asQuery();
		ResultSet rs = executeSelect(q);
		long reflexiveCnt = rs.next().getLiteral("cnt").getLong(); 
		//count number of asymmetric violations 
		countQueryTemplate.clearParams();
		countQueryTemplate.setIri("p", p.getURI().toString());
		long cnt = -1;
		rs = executeSelect(countQueryTemplate.asQuery());
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			cnt = qs.getLiteral("cnt").getLong();
		}
		//real number of violations is (reflexiveCnt+(cnt-reflexiveCnt)/2)
		cnt = reflexiveCnt + (cnt - reflexiveCnt)/2;
		return cnt;
	}
	
	public static void main(String[] args) {
		SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();
		ObjectProperty op = new ObjectProperty("http://dbpedia.org/ontology/servingRailwayLine");
		AsymmetryConsistencyValidator validator = new AsymmetryConsistencyValidator(endpoint);
		System.out.println("Consistent: " + validator.isConsistent(op));
		System.out.println("#Violations: " + validator.getNumberOfViolations(op));
		Collection<AsymmetryViolation> violations = validator.getViolations(op);
		System.out.println(violations.size());
		for(AsymmetryViolation v : violations){
			System.out.println(v);
		}
	}

}
