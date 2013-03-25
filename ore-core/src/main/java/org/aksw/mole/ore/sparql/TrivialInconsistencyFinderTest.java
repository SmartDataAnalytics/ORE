package org.aksw.mole.ore.sparql;

import java.util.Set;

import junit.framework.Assert;

import org.dllearner.kb.sparql.SparqlEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class TrivialInconsistencyFinderTest {
	
	private SparqlEndpoint endpoint;

	@Before
	public void setUp() throws Exception {
		endpoint = SparqlEndpoint.getEndpointDBpedia();
	}

	@Test
	public void testGetInconsistentFragment() throws OWLOntologyCreationException {
		InconsistencyFinder finder = new TrivialInconsistencyFinder(endpoint);
		Set<OWLAxiom> axioms  = finder.getInconsistentFragment();
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = man.createOntology(axioms);
		OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		boolean consistent = reasoner.isConsistent();
		System.out.println(consistent);
		Assert.assertTrue((axioms.isEmpty() && consistent || (!axioms.isEmpty() && ! consistent)));
	}

}
