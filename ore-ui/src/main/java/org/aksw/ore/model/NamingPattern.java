package org.aksw.ore.model;

import java.net.URL;

public class NamingPattern {
	
	private String label;
	private URL url;
	private boolean useReasoning = false;
	private String imageFilename;
	private String description;
	
	public NamingPattern(String label) {
		this(label, null);
	}
	
	public NamingPattern(String label, URL url) {
		this(label, url, false);
	}
	
	public NamingPattern(String label, URL url, boolean useReasoning) {
		this(label, url, useReasoning, null);
	}
	
	public NamingPattern(String label, URL url, boolean useReasoning, String imageFilename) {
		this(label, url, useReasoning, imageFilename, null);
	}
	
	public NamingPattern(String label, URL url, boolean useReasoning, String imageFilename, String description) {
		this.label = label;
		this.url = url;
		this.useReasoning = useReasoning;
		this.imageFilename = imageFilename;
		this.description = description;
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
	
	/**
	 * @return the imageFilename
	 */
	public String getImageFilename() {
		return imageFilename;
	}
	
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
