/**
 * 
 */
package org.aksw.ore.manager;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.mole.ore.validation.constraint.ConstraintViolation;
import org.aksw.mole.ore.validation.constraint.OWLAxiomConstraintToSPARQLConstructConverter;
import org.aksw.mole.ore.validation.constraint.OWLAxiomConstraintToSPARQLConverter;
import org.aksw.mole.ore.validation.constraint.SubjectObjectViolation;
import org.aksw.mole.ore.validation.constraint.SubjectViolation;
import org.apache.log4j.Logger;
import org.dllearner.kb.LocalModelBasedSparqlEndpointKS;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author Lorenz Buehmann
 *
 */
public class ConstraintValidationManager {
	
	
	private static final Logger logger = Logger.getLogger(ConstraintValidationManager.class.getName());
	
	private QueryExecutionFactory qef;
	private Model model;
	
	OWLAxiomConstraintToSPARQLConstructConverter conv = new OWLAxiomConstraintToSPARQLConstructConverter();
	OWLAxiomConstraintToSPARQLConverter conv2 = new OWLAxiomConstraintToSPARQLConverter();

	public ConstraintValidationManager(SparqlEndpointKS ks) {
		if(ks.isRemote()){
			SparqlEndpoint endpoint = ks.getEndpoint();
			qef = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
			qef = new QueryExecutionFactoryCacheEx(qef, ks.getCache());
//			qef = new QueryExecutionFactoryPaginated(qef, 10000);
			
		} else {
			qef = new QueryExecutionFactoryModel(((LocalModelBasedSparqlEndpointKS)ks).getModel());
		}
	
	}
	
	public Set<String> validateWithExplanations(OWLAxiom constraint){
		Set<String> violations = new HashSet<String>();
		
		Query query = conv.asQuery("?s", constraint);query.setLimit(100);
		System.out.println(query);
		QueryExecution qe = qef.createQueryExecution(query);
		model = qe.execConstruct();
		qe.close();
		//get the instances out of the model
		query = conv2.asQuery("?s", constraint);
		qe = new QueryExecutionFactoryModel(model).createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			violations.add(qs.getResource("s").getURI());
		}
		qe.close();
		return violations;
	}
	
	public Set<ConstraintViolation> getViolatingResources(OWLAxiom constraint){
		logger.info("Validating axiom " + constraint);
		Set<ConstraintViolation> violations = new HashSet<ConstraintViolation>();
		
		Query query = conv2.asQuery(constraint, "?s", "?o");
		logger.info("Running query\n" + query);
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		boolean subjectObject = OWLAxiomConstraintToSPARQLConverter.isSubjectObjectBasedConstraint(constraint);
		while(rs.hasNext()){
			qs = rs.next();
			if(subjectObject){
				violations.add(new SubjectObjectViolation(
						constraint, 
						qs.getResource("s").getURI(), 
						qs.getResource("o").getURI()));
			} else {
				violations.add(new SubjectViolation(
						constraint, 
						qs.getResource("s").getURI()));
			}
			
		}
		qe.close();
		return violations;
	}
	
	public Set<ConstraintViolation> getViolatingResources(OWLAxiom constraint, int limit){
		logger.info("Validating axiom " + constraint);
		Set<ConstraintViolation> violations = new HashSet<ConstraintViolation>();
		
		Query query = conv2.asQuery(constraint, "?s", "?o");
		query.setLimit(limit);
		logger.info("Running query\n" + query);
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		boolean subjectObject = OWLAxiomConstraintToSPARQLConverter.isSubjectObjectBasedConstraint(constraint);
		while(rs.hasNext()){
			qs = rs.next();
			if(subjectObject){
				violations.add(new SubjectObjectViolation(
						constraint, 
						qs.getResource("s").getURI(), 
						qs.getResource("o").getURI()));
			} else {
				violations.add(new SubjectViolation(
						constraint, 
						qs.getResource("s").getURI()));
			}
			
		}
		qe.close();
		return violations;
	}
	
	public String getViolationExplanation(OWLAxiom constraint, String uri){
		StringBuilder explanation = new StringBuilder();
		StringWriter sw = new StringWriter();
		Query query = conv.asQuery("?s", constraint);
		ParameterizedSparqlString ps = new ParameterizedSparqlString(query.toString());
		ps.setIri("s", uri);
		QueryExecution qe = qef.createQueryExecution(ps.asQuery());
		Model explanationModel = qe.execConstruct();
		explanationModel.write(sw, "TURTLE");
		return sw.toString();
//		StmtIterator iter = model.listStatements(model.createResource(uri), null, (RDFNode)null);
//		Statement st;
//		while(iter.hasNext()){
//			st = iter.next();
//			explanation.append(st.asTriple().toString() + "\n");
//		}
//		iter.close();
//		
//		return explanation.toString();
	}

	public static void main(String[] args) throws Exception {
		ConstraintValidationManager constraintMan = new ConstraintValidationManager(new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia()));
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
		PrefixManager pm = new DefaultPrefixManager("http://dbpedia.org/ontology/");

		OWLClass clsA = df.getOWLClass("A", pm);
		OWLClass clsB = df.getOWLClass("B", pm);
		OWLClass clsC = df.getOWLClass("C", pm);
		OWLClass clsPerson = df.getOWLClass("Person", pm);
		OWLClass clsPlace = df.getOWLClass("Place", pm);

		OWLObjectProperty propR = df.getOWLObjectProperty("birthPlace", pm);
		OWLObjectProperty propS = df.getOWLObjectProperty("team", pm);
		OWLObjectProperty propBirthPlace = df.getOWLObjectProperty("birthPlace", pm);
		OWLDataProperty propBirthDate = df.getOWLDataProperty("birthDate", pm);

		OWLDataProperty dpT = df.getOWLDataProperty("t", pm);
		OWLDataRange booleanRange = df.getBooleanOWLDatatype();
		OWLLiteral lit = df.getOWLLiteral(1);

		OWLIndividual indA = df.getOWLNamedIndividual("a", pm);
		OWLIndividual indB = df.getOWLNamedIndividual("b", pm);
		
		OWLAxiom axiom = df.getOWLDisjointObjectPropertiesAxiom(propR, propS);
		Set<ConstraintViolation> violations = constraintMan.getViolatingResources(axiom);
		System.out.println(violations);
		
		axiom = df.getOWLAsymmetricObjectPropertyAxiom(propS);
		violations = constraintMan.getViolatingResources(axiom);
		System.out.println(violations);
		
		axiom = df.getOWLSubClassOfAxiom(clsPerson, df.getOWLObjectIntersectionOf(
				df.getOWLObjectSomeValuesFrom(propBirthPlace, clsPlace),
				df.getOWLDataSomeValuesFrom(propBirthDate, df.getRDFPlainLiteral()))
				);
		System.out.println(new OWLAxiomConstraintToSPARQLConstructConverter().asQuery("?s", axiom));
		violations = constraintMan.getViolatingResources(axiom, 10);
		System.out.println(violations);
		System.out.println(constraintMan.getViolationExplanation(axiom, "http://dbpedia.org/resource/Alec_Reid"));
	}
}
