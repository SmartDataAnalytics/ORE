package org.aksw.mole.ore.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLProperty;

import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class AsymmetryConsistencyValidator extends SPARQLConsistencyValidator<AsymmetryViolation, OWLObjectProperty>{
	
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
	
	public Collection<AsymmetryViolation> getViolations(OWLObjectProperty op){
		queryTemplate.clearParams();
		queryTemplate.setIri("p", op.toStringID());
		
		Set<AsymmetryViolation> violations = new HashSet<AsymmetryViolation>();
		
		ResultSet rs = executeSelect(queryTemplate.asQuery());
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			violations.add(new AsymmetryViolation(
					op, 
					new OWLNamedIndividualImpl(IRI.create(qs.getResource("s").getURI())), 
					new OWLNamedIndividualImpl(IRI.create(qs.getResource("o").getURI()))));
		}
		return violations;
	}
	
	@Override
	public long getNumberOfViolations(OWLProperty p){
		//count number of reflexive triples
		ParameterizedSparqlString s = new ParameterizedSparqlString("SELECT (COUNT(*) AS ?cnt) WHERE {?s ?p ?s.}");
		s.setIri("p", p.toStringID());
		Query q = s.asQuery();
		ResultSet rs = executeSelect(q);
		long reflexiveCnt = rs.next().getLiteral("cnt").getLong(); 
		//count number of asymmetric violations 
		countQueryTemplate.clearParams();
		countQueryTemplate.setIri("p", p.toStringID());
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
		OWLObjectProperty op = new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/servingRailwayLine"));
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
