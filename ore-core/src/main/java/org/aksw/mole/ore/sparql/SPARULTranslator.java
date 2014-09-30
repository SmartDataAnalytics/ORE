package org.aksw.mole.ore.sparql;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.coode.owlapi.rdf.model.AbstractTranslator;
import org.coode.owlapi.rdf.model.RDFLiteralNode;
import org.coode.owlapi.rdf.model.RDFNode;
import org.coode.owlapi.rdf.model.RDFResourceNode;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.change.RemoveAxiomData;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class SPARULTranslator extends AbstractTranslator<RDFNode, RDFResourceNode, RDFResourceNode, RDFLiteralNode> {

	private StringBuilder triplesString;
	
	public SPARULTranslator(OWLOntologyManager manager, OWLOntology ontology,
			boolean useStrongTyping) {
		super(manager, ontology, useStrongTyping);
		// TODO Auto-generated constructor stub
	}
	
	public String translate(OWLOntologyChange change){
		triplesString = new StringBuilder();
		return translate(Collections.singleton(change));
	}
	
	public String translate(Collection<OWLOntologyChange> changes){
		return translate(changes, false);
	}
	
	public String translate(Collection<OWLOntologyChange> changes, boolean compact){
		String s = "";
		
		// removed axioms
		Collection<OWLOntologyChange> tmp = new HashSet<OWLOntologyChange>();
		for(OWLOntologyChange change : changes){
			if(change.isRemoveAxiom()){
				tmp.add(change);
			}
		}
		s += translate(tmp, RemoveAxiom.class, compact);
		
		s += s.isEmpty() ? "" : "\n";
		
		// added axioms
		tmp = new HashSet<OWLOntologyChange>();
		for(OWLOntologyChange change : changes){
			if(change.isAddAxiom()){
				tmp.add(change);
			}
		}
		s += translate(tmp, AddAxiom.class, compact);
		
		return s;
	}
	
	public String translate(Collection<OWLOntologyChange> changes, Class<? extends OWLOntologyChange> type){
		return translate(changes, type, false);
	}
	
	public String translate(Collection<OWLOntologyChange> changes, Class<? extends OWLOntologyChange> type, boolean compact){
		if(changes.isEmpty()){
			return "";
		}
		triplesString = new StringBuilder();
		
		for(OWLOntologyChange change : changes){
			change.getAxiom().accept(this);
		}
		
		if(compact){
			Model m = ModelFactory.createDefaultModel();
			m.read(new ByteArrayInputStream(triplesString.toString().getBytes()), null, "TURTLE");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			m.write(baos, "TTL", null);
			return (type == RemoveAxiom.class ? "DELETE DATA" : "INSERT DATA") + "{\n" + new String(baos.toByteArray()) + "}";
		} else {
			return (type == RemoveAxiom.class ? "DELETE DATA" : "INSERT DATA") + "{\n" + triplesString.toString() + "}";
		}
	}

	@Override
	protected void addTriple(RDFResourceNode subject, RDFResourceNode pred,
			RDFNode object) {
		triplesString.append(subject).append(" ").append(pred).append(" ").append(object).append(" .\n");
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
			return new RDFLiteralNode(literal.getLiteral(), literal.getDatatype().getIRI());
		} else {
			return new RDFLiteralNode(literal.getLiteral(), literal.getLang());
		}
		
	}

	public static void main(String[] args) throws Exception {
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
		OWLOntology ontology = man.createOntology();
		DefaultPrefixManager pm = new DefaultPrefixManager("http://ex.org/");
		OWLObjectProperty op = df.getOWLObjectProperty("p", pm);
		OWLDataProperty dp = df.getOWLDataProperty("t", pm);
		OWLClass clsA = df.getOWLClass("A", pm);
		OWLClass clsB = df.getOWLClass("B", pm);
		OWLIndividual indA = df.getOWLNamedIndividual("a", pm);
		OWLIndividual indB = df.getOWLNamedIndividual("b", pm);
		OWLLiteral lit = df.getOWLLiteral("1934-12-10", OWL2Datatype.XSD_DATE_TIME);
		
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		
//		changes.addAll(man.addAxiom(ontology, df.getOWLObjectPropertyDomainAxiom(op, clsA)));
//		changes.addAll(man.addAxiom(ontology, df.getOWLObjectPropertyDomainAxiom(op, clsB)));
//		changes.addAll(man.addAxiom(ontology, df.getOWLFunctionalObjectPropertyAxiom(op)));
//		changes.addAll(man.addAxiom(ontology, df.getOWLObjectPropertyAssertionAxiom(op, indA, indB)));
		changes.add(new RemoveAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp, indA, lit)));
		
		System.out.println(new SPARULTranslator(man, ontology, false).translate(changes, true));
		
	}

}
