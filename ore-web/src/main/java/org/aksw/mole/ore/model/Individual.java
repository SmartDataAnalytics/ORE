package org.aksw.mole.ore.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Individual implements Serializable{

	private static final long serialVersionUID = -7807431193578167468L;
	
	private String label;
	@Id
	private String iri;
	
	public Individual() {
		// TODO Auto-generated constructor stub
	}

	public Individual(String label, String iri) {
		super();
		this.label = label;
		this.iri = iri;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getIri() {
		return iri;
	}

	public void setIri(String iri) {
		this.iri = iri;
	}

}
