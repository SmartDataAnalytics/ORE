package org.aksw.mole.ore.sparql;

import java.util.Collection;
import java.util.List;

import org.coode.owlapi.rdf.model.AbstractTranslator;
import org.coode.owlapi.rdf.model.RDFLiteralNode;
import org.coode.owlapi.rdf.model.RDFNode;
import org.coode.owlapi.rdf.model.RDFResourceNode;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;

public class SPARULTranslator extends AbstractTranslator<RDFNode, RDFResourceNode, RDFResourceNode, RDFLiteralNode> {

	private StringBuilder sb;
	
	public SPARULTranslator(OWLOntologyManager manager, OWLOntology ontology,
			boolean useStrongTyping) {
		super(manager, ontology, useStrongTyping);
		// TODO Auto-generated constructor stub
	}
	
	public String translate(OWLOntologyChange change){
		sb = new StringBuilder();
		sb.append(change instanceof RemoveAxiom ? "DELETE DATA" : "INSERT DATA");
		sb.append("{");
		change.getAxiom().accept(this);
		sb.append("}");
		sb.append("\n");
		
		return sb.toString();
	}
	
	public String translate(Collection<OWLOntologyChange> changes){
		sb = new StringBuilder();
		for(OWLOntologyChange change : changes){
			sb.append(change instanceof RemoveAxiom ? "DELETE DATA" : "INSERT DATA");
			sb.append("{");
			change.getAxiom().accept(this);
			sb.append("}");
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	public String translate(Collection<OWLOntologyChange> changes, Class<? extends OWLOntologyChange> type){
		sb = new StringBuilder();
		sb.append(type == RemoveAxiom.class ? "DELETE DATA" : "INSERT DATA");
		sb.append("{\n");
		for(OWLOntologyChange change : changes){
			change.getAxiom().accept(this);
			sb.append("\n");
		}
		sb.append("}");
		
		return sb.toString();
	}

	@Override
	protected void addTriple(RDFResourceNode subject, RDFResourceNode pred,
			RDFNode object) {
		sb.append(subject).append(" ").append(pred).append(" ").append(object);
	}

	@Override
	protected RDFResourceNode getAnonymousNode(Object key) {
		return new RDFResourceNode(System.identityHashCode(key));
	}

	@Override
	protected RDFResourceNode getPredicateNode(IRI iri) {
		return new RDFResourceNode(iri);
	}

	@Override
	protected RDFResourceNode getResourceNode(IRI iri) {
		return new RDFResourceNode(iri);
	}

	@Override
	protected RDFLiteralNode getLiteralNode(OWLLiteral literal) {
		if(literal.getDatatype() != null){
			return new RDFLiteralNode(literal.toString(), literal.getDatatype().getIRI());
		} else {
			return new RDFLiteralNode(literal.toString(), literal.getLang());
		}
		
	}


}
