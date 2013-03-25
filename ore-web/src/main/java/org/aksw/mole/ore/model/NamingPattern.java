package org.aksw.mole.ore.model;

import java.net.URL;

public class NamingPattern {
	
	private String label;
	private URL url;
	private boolean useReasoning = false;
	
	public NamingPattern(String label) {
		this(label, null);
	}
	
	public NamingPattern(String label, URL url) {
		this(label, url, false);
	}
	
	public NamingPattern(String label, URL url, boolean useReasoning) {
		this.label = label;
		this.url = url;
		this.useReasoning = useReasoning;
	}
	
	public String getLabel() {
		return label;
	}
	
	public URL getUrl() {
		return url;
	}
	
	public boolean isUseReasoning() {
		return useReasoning;
	}

}
