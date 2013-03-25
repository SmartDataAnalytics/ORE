package org.aksw.mole.ore.model;

public enum ResourceType {
	UNKNOWN("unknown"), CLASS("Class"), OBJECT_PROPERTY("Object Property"), DATA_PROPERTY("Data Property");

	private String name;
	
	private ResourceType(String name){
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
