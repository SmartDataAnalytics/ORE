package org.aksw.ore.model;

import java.net.URL;

public class NamingPattern {
	
	private int id;
	private String label;
	private URL url;
	private boolean useReasoning = false;
	private String imageFilename;
	private String description;
	
	public NamingPattern(int id, String label) {
		this(id, label, null);
	}
	
	public NamingPattern(int id, String label, URL url) {
		this(id, label, url, false);
	}
	
	public NamingPattern(int id, String label, URL url, boolean useReasoning) {
		this(id, label, url, useReasoning, null);
	}
	
	public NamingPattern(int id, String label, URL url, boolean useReasoning, String imageFilename) {
		this(id, label, url, useReasoning, imageFilename, null);
	}
	
	public NamingPattern(int id, String label, URL url, boolean useReasoning, String imageFilename, String description) {
		this.id = id;
		this.label = label;
		this.url = url;
		this.useReasoning = useReasoning;
		this.imageFilename = imageFilename;
		this.description = description;
	}
	
	/**
	 * @return the id
	 */
	public int getID() {
		return id;
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
