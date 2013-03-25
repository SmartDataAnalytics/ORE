package org.aksw.mole.ore.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class URLParameters {
	
	public static final String ENDPOINT_URL = "endpoint_url";
	public static final String DEFAULT_GRAPH_URI = "default_graph_uri";
	
	public static final String ONTOLOGY_URI = "ontology_uri";
	
	public static final String ACTION = "action";
	public static final String ENRICHMENT = "enrichment";
	public static final String DEBUGGING = "debugging";
	public static final String NAMING = "naming";
	
	public static String decode(String encodedString){
		String decodedString = null;
		try {
			decodedString = URLDecoder.decode(encodedString, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return decodedString;
	}

}
