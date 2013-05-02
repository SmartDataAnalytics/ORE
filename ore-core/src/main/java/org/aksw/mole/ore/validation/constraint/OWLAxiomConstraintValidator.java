package org.aksw.mole.ore.validation.constraint;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class OWLAxiomConstraintValidator {

	public OWLAxiomConstraintValidator() {
	}
	
	public void validate(SparqlEndpoint endpoint, OWLOntology constraintAxioms){
		validate(endpoint, constraintAxioms.getLogicalAxioms());
	}
	
	public void validate(SparqlEndpoint endpoint, Collection<? extends OWLAxiom> constraintAxioms){
		OWLAxiomConstraintToSPARQLConverter converter = new OWLAxiomConstraintToSPARQLConverter();
		for(OWLAxiom axiom : constraintAxioms){
			Query query = converter.asQuery("?x", axiom);System.out.println(query);
			QueryEngineHTTP qe = new QueryEngineHTTP(endpoint.getURL().toString(), query);
			ResultSet rs = qe.execSelect();
			while(rs.hasNext()){
				System.out.println(rs.next().get("x"));
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		SparqlEndpoint endpoint = new SparqlEndpoint(new URL("http://lod2.wolterskluwer.de/virtuoso/sparql"));
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = man.loadOntologyFromOntologyDocument(new File(args[0]));
		
		OWLAxiomConstraintValidator validator = new OWLAxiomConstraintValidator();
		validator.validate(endpoint, ontology);
	}

}
