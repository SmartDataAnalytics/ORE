/**
 * 
 */
package org.aksw.mole.ore.validation.constraint.spin;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheEx;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.dllearner.kb.sparql.SparqlEndpoint;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author Lorenz Buehmann
 *
 */
public class SPINConstraintValidator {
	
	private QueryExecutionFactory qef;
	
	public SPINConstraintValidator(SparqlEndpoint endpoint) {
		this(endpoint, null);
	}

	public SPINConstraintValidator(SparqlEndpoint endpoint, CacheEx cache) {
		qef = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
		if(cache != null){
			qef = new QueryExecutionFactoryCacheEx(qef, cache);
		}
//		qef = new QueryExecutionFactoryPaginated(qef, 10000);
	}
	
	public SPINConstraintValidator(Model model) {
		qef = new QueryExecutionFactoryModel(model);
	}
	
	public void validateSPINConstraint(String spinConstraint){
		//convert spin constraint to SPARQL query
		String sparqlQuery = SPIN2SPARQLConverter.asSPARQL(spinConstraint);
		
		//execute SPARQL query
		QueryExecution qe = qef.createQueryExecution(sparqlQuery);
		qe.execConstruct();
	}

}
