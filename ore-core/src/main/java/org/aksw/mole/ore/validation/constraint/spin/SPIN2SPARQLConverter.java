/**
 * 
 */
package org.aksw.mole.ore.validation.constraint.spin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;

/**
 * @author Lorenz Buehmann
 *
 */
public class SPIN2SPARQLConverter {
	
	private static final String SERVICE_URL = "http://spinservices.org:8080/spin/sparqlmotion";
	
	public static String asSPARQL(String spin){
		try {
			String requestString = SERVICE_URL + "?id=spin2sparql" + "&rdf=" + URLEncoder.encode(spin, "UTF-8") + "&format=turtle";
			URL url = new URL(requestString);
			String query = IOUtils.toString(url.openStream());
			return query;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String asSPIN(String sparqlQuery){
		try {
			String requestString = SERVICE_URL + "?id=sparql2spin" + "&text=" + URLEncoder.encode(sparqlQuery, "UTF-8") + "&format=turtle";
			URL url = new URL(requestString);
			String query = IOUtils.toString(url.openStream());
			return query;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) throws Exception {
		String query = "SELECT ?s WHERE {?s a <http://dbpedia.org/ontology/Book>.} LIMIT 10";
		String spin = asSPIN(query);
		System.out.println(spin);
		query = asSPARQL(spin);
		System.out.println(query);
	}

}
