/**
 * 
 */
package org.aksw.ore.model;

import java.util.Set;

import org.semanticweb.owlapi.change.OWLOntologyChangeData;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeVisitor;
import org.semanticweb.owlapi.model.OWLOntologyChangeVisitorEx;

/**
 * @author Lorenz Buehmann
 *
 */
public class EntityRenaming extends OWLOntologyChange{

	private OWLEntity from;
	private OWLEntity to;
	private String fromString;
	private String toString;

	/**
	 * @param ont
	 */
	public EntityRenaming(OWLOntology ont) {
		super(ont);
		
	}
	
	/**
	 * @param ont
	 */
	public EntityRenaming(OWLOntology ont, OWLEntity from, OWLEntity to) {
		super(ont);
		this.from = from;
		this.to = to;
	}
	
	/**
	 * @param ont
	 */
	public EntityRenaming(OWLOntology ont, String fromString, String toString) {
		super(ont);
		this.fromString = fromString;
		this.toString = toString;
	}
	
	/**
	 * @return the from
	 */
	public OWLEntity getFrom() {
		return from;
	}
	
	/**
	 * @return the to
	 */
	public OWLEntity getTo() {
		return to;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLOntologyChange#accept(org.semanticweb.owlapi.model.OWLOntologyChangeVisitor)
	 */
	@Override
	public void accept(OWLOntologyChangeVisitor arg0) {
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLOntologyChange#accept(org.semanticweb.owlapi.model.OWLOntologyChangeVisitorEx)
	 */
	@Override
	public <O> O accept(OWLOntologyChangeVisitorEx<O> arg0) {
		return null;
	}

	@Override
	public OWLOntologyChange reverseChange() {
		return new EntityRenaming(getOntology(), to, from);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLOntologyChange#getAxiom()
	 */
	@Override
	public OWLAxiom getAxiom() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLOntologyChange#getChangeData()
	 */
	@Override
	public OWLOntologyChangeData getChangeData() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLOntologyChange#getSignature()
	 */
	@Override
	public Set<OWLEntity> getSignature() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLOntologyChange#isAddAxiom()
	 */
	@Override
	public boolean isAddAxiom() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLOntologyChange#isAxiomChange()
	 */
	@Override
	public boolean isAxiomChange() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLOntologyChange#isImportChange()
	 */
	@Override
	public boolean isImportChange() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Rename "  + fromString + " To " + toString;
	}

}
