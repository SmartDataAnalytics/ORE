/**
 * 
 */
package org.aksw.mole.ore.sparql;

import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheFrontend;
import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.dllearner.kb.LocalModelBasedSparqlEndpointKS;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;

import com.hp.hpl.jena.query.ResultSet;

/**
 * @author Lorenz Buehmann
 *
 */
public class InconsistencyEstimator {
	
	protected QueryExecutionFactory qef;
	
	public InconsistencyEstimator(SparqlEndpointKS ks, String cacheDirectory) {
		if(ks.isRemote()){
			SparqlEndpoint endpoint = ks.getEndpoint();
			qef = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
			if(cacheDirectory != null){
					long timeToLive = TimeUnit.DAYS.toMillis(30);
					CacheFrontend cacheFrontend = CacheUtilsH2.createCacheFrontend(cacheDirectory, true, timeToLive);
					qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);
			}
		} else {
			qef = new QueryExecutionFactoryModel(((LocalModelBasedSparqlEndpointKS)ks).getModel());
		}
	}
	
	public boolean supportsInconsistency(){
		//contains disjoint classes
		String query = "SELECT * WHERE {?cls1 owl:disjointWith ?cls2.} LIMIT 1";
		ResultSet rs = qef.createQueryExecution(query).execSelect();
		if(rs.hasNext()){
			return true;
		}
		//contains differentFrom
		query = "SELECT * WHERE {?ind1 owl:differentFrom ?ind2.} LIMIT 1";
		rs = qef.createQueryExecution(query).execSelect();
		if(rs.hasNext()){
			return true;
		}
		// contains AllDifferent
		query = "SELECT * WHERE {?ind1 owl:AllDifferent ?ind2.} LIMIT 1";
		rs = qef.createQueryExecution(query).execSelect();
		if (rs.hasNext()) {
			return true;
		}
		
		//contains owl:complementOf
		query = "SELECT * WHERE {?cls1 owl:complementOf ?cls2.} LIMIT 1";
		rs = qef.createQueryExecution(query).execSelect();
		if (rs.hasNext()) {
			return true;
		}
		// contains functional dataproperties
		query = "SELECT * WHERE {?s a owl:FunctionalProperty. ?s a owl:DatatypeProperty.} LIMIT 1";
		rs = qef.createQueryExecution(query).execSelect();
		if (rs.hasNext()) {
//			return true;
		}
		
		return false;
	}
	
	public static void main(String[] args) throws Exception {
		InconsistencyEstimator inconsistencyEstimator = new InconsistencyEstimator(new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia()), "cache");
		System.out.println(inconsistencyEstimator.supportsInconsistency());
	}

}
