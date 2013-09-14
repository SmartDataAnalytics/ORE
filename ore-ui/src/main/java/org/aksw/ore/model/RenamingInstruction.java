package org.aksw.ore.model;

import org.w3c.dom.Node;

public class RenamingInstruction {
	
	private Node node;
	private boolean selected;
	private String originalName;
	private String newName;
	
	public RenamingInstruction(String originalName, String newName, Node node) {
		this.originalName = originalName;
		this.newName = newName;
		this.node = node;
	}
	
	public String getNLRepresentation() {
		return "Rename " + originalName + " to " + newName; 
	}
	
	public String getNLRepresentationHTML() {
		return "Rename " + "<b>" + originalName + "</b> to <b>" + newName + "</b>"; 
	}
	
	public Node getNode() {
		return node;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	

}
