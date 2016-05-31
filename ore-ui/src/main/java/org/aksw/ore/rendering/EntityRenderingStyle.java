package org.aksw.ore.rendering;

public enum EntityRenderingStyle {
	SHORT_FORM("Short Form"), URI("URI"), LABEL("Label");
	
	private String label;
	private String description;
	
	EntityRenderingStyle(String label) {
		this(label, label);
	}

	EntityRenderingStyle(String label, String description) {
		this.label = label;
		this.description = description;
	}
	
	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return label;
	}
	

}