package org.aksw.mole.ore.sparql;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.riot.Lang;
import org.coode.owlapi.rdf.model.AbstractTranslator;
import org.coode.owlapi.rdf.model.RDFLiteralNode;
import org.coode.owlapi.rdf.model.RDFNode;
import org.coode.owlapi.rdf.model.RDFResourceNode;
import org.coode.owlapi.rdf.model.RDFTriple;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.JenaRuntime;
import com.hp.hpl.jena.n3.N3JenaWriter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.util.ModelUtils;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

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
			return type == RemoveAxiom.class ? "DELETE DATA" : "INSERT DATA" + "{\n" + new String(baos.toByteArray()) + "}";
		} else {
			return type == RemoveAxiom.class ? "DELETE DATA" : "INSERT DATA" + "{\n" + triplesString.toString() + "}";
		}
	}

	@Override
	protected void addTriple(RDFResourceNode subject, RDFResourceNode pred,
			RDFNode object) {
		triplesString.append(subject).append(" ").append(pred).append(" ").append(object).append(".\n");
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

	public static void main(String[] args) throws Exception {
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
		OWLOntology ontology = man.createOntology();
		DefaultPrefixManager pm = new DefaultPrefixManager("http://ex.org/");
		OWLObjectProperty op = df.getOWLObjectProperty("p", pm);
		OWLClass clsA = df.getOWLClass("A", pm);
		OWLClass clsB = df.getOWLClass("B", pm);
		
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		
		changes.addAll(man.addAxiom(ontology, df.getOWLObjectPropertyDomainAxiom(op, clsA)));
		changes.addAll(man.addAxiom(ontology, df.getOWLObjectPropertyDomainAxiom(op, clsB)));
		
		System.out.println(new SPARULTranslator(man, ontology, false).translate(changes, true));
		
	}

}
