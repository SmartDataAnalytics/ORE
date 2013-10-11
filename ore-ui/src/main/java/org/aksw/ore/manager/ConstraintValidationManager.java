/**
 * 
 */
package org.aksw.ore.manager;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheCoreEx;
import org.aksw.jena_sparql_api.cache.extra.CacheCoreH2;
import org.aksw.jena_sparql_api.cache.extra.CacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheExImpl;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.mole.ore.validation.constraint.OWLAxiomConstraintToSPARQLConstructConverter;
import org.aksw.mole.ore.validation.constraint.OWLAxiomConstraintToSPARQLConverter;
import org.dllearner.kb.LocalModelBasedSparqlEndpointKS;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * @author Lorenz Buehmann
 *
 */
public class ConstraintValidationManager {
	
	
	private QueryExecutionFactory qef;
	private Model model;

	public ConstraintValidationManager(SparqlEndpointKS ks, CacheEx cache) {
		if(ks.isRemote()){
			SparqlEndpoint endpoint = ks.getEndpoint();
			qef = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
			if(cache != null){
				qef = new QueryExecutionFactoryCacheEx(qef, cache);
			}
			qef = new QueryExecutionFactoryPaginated(qef, 10000);
			
		} else {
			qef = new QueryExecutionFactoryModel(((LocalModelBasedSparqlEndpointKS)ks).getModel());
		}
	
	}
	
	public Set<String> validateWithExplanations(OWLAxiom constraint){
		Set<String> violations = new HashSet<String>();
		OWLAxiomConstraintToSPARQLConstructConverter conv = new OWLAxiomConstraintToSPARQLConstructConverter();
		OWLAxiomConstraintToSPARQLConverter conv2 = new OWLAxiomConstraintToSPARQLConverter();
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
	
	public String getViolationExplanation(OWLAxiom constraint, String uri){
		StringBuilder explanation = new StringBuilder();
		StmtIterator iter = model.listStatements(model.createResource(uri), null, (RDFNode)null);
		Statement st;
		while(iter.hasNext()){
			st = iter.next();
			explanation.append(st.toString() + "\n");
		}
		iter.close();
		return explanation.toString();
	}

	public Set<String> validate(OWLAxiom constraint){
		Set<String> violations = new HashSet<String>();
		OWLAxiomConstraintToSPARQLConverter conv = new OWLAxiomConstraintToSPARQLConverter();
		Query query = conv.asQuery("?s", constraint);query.setLimit(100);
		System.out.println(query);
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			violations.add(qs.getResource("s").getURI());
		}
		qe.close();
		return violations;
	}
}
