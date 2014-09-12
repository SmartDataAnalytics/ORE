/**
 * 
 */
package org.aksw.ore.util;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.dllearner.kb.sparql.SparqlEndpoint;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

/**
 * @author Lorenz Buehmann
 *
 */
public class CKANEndpoints {
	
	public static void getEndpoints(){
		QueryExecutionFactory qef = new QueryExecutionFactoryHttp(
				"http://semantic.ckan.net/sparql",
				Sets.newHashSet("http://datahub.io"));
		qef = new QueryExecutionFactoryCacheEx(qef, CacheUtilsH2.createCacheFrontend("ckan", false, TimeUnit.DAYS.toMillis(7)));
		
		String query = "Select Distinct ?o {\n" + 
				"  ?s\n" + 
				"    <http://www.w3.org/ns/dcat#accessURL> ?o ;\n" + 
				"    <http://purl.org/dc/terms/format> [\n" + 
				"      <http://www.w3.org/2000/01/rdf-schema#label> \"api/sparql\"\n" + 
				"    ]\n" + 
				"  .\n" + 
				"}";
		
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		while(rs.hasNext()){
			QuerySolution qs = rs.next();
			String url = qs.getResource("o").getURI();
			System.out.println(url);
			
			try {
				getGraphs(url);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		qe.close();
	}
	
	public static void getGraphs(String endpointURL){
		System.out.println(endpointURL);
		QueryExecutionFactory qef = new QueryExecutionFactoryHttp(endpointURL);
		qef = new QueryExecutionFactoryCacheEx(qef, CacheUtilsH2.createCacheFrontend("ckan", false, TimeUnit.DAYS.toMillis(7)));
		
		String query = "SELECT DISTINCT ?g WHERE {GRAPH ?g {?s ?p ?o.}}";
		
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		while(rs.hasNext()){
			QuerySolution qs = rs.next();
			String uri = qs.getResource("g").getURI();
			System.out.println(uri);
			
		}
		qe.close();
	}
	
	public static void main(String[] args) throws Exception {
		getEndpoints();
	}

}
