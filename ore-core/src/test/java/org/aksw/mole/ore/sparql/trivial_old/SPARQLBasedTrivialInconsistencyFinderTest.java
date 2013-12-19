/**
 * 
 */
package org.aksw.mole.ore.sparql.trivial_old;

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.Set;

import org.dllearner.kb.LocalModelBasedSparqlEndpointKS;
import org.dllearner.kb.SparqlEndpointKS;
import org.junit.Test;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.hp.hpl.jena.ontology.CardinalityRestriction;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * @author Lorenz Buehmann
 *
 */
public class SPARQLBasedTrivialInconsistencyFinderTest {
	
	SparqlEndpointKS ks;
	private SPARQLBasedTrivialInconsistencyFinder incFinder;
	private static final String NS = "http://example.org/";
	
	public SPARQLBasedTrivialInconsistencyFinderTest() {
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		model.setNsPrefix("ex", NS);
		OntClass clsA = model.createClass(NS + "A");
		OntClass clsB = model.createClass(NS + "B");
		OntClass clsA1 = model.createClass(NS + "A1");
		OntClass clsA2 = model.createClass(NS + "A2");
		OntClass clsB1 = model.createClass(NS + "B1");
		OntClass clsB2 = model.createClass(NS + "B2");
		Property prop = model.createProperty(NS, "p");
		clsA.addSubClass(clsA1);
		clsA1.addSubClass(clsA2);
		clsB.addSubClass(clsB1);
		clsB1.addSubClass(clsB2);
		clsA.addDisjointWith(clsB);
		Individual ind = model.createIndividual(NS + "a", clsA2);
		ind.addOntClass(clsB);
		Individual ind2 = model.createIndividual(NS + "b", clsA2);
		
		//x a [rdf:type owl:Restriction; owl:onProperty P; owl:cardinality 0^^xsd:integer.] x P o
		CardinalityRestriction restriction = model.createCardinalityRestriction(null, prop, 0);
		ind.addOntClass(restriction);
		ind.addProperty(prop, ind2);
//		
//		StringWriter sw = new StringWriter();
//		model.write(sw, "TURTLE");
//		System.out.println(sw.toString());
		
		ks = new LocalModelBasedSparqlEndpointKS(model);
		
		incFinder = new SPARQLBasedTrivialInconsistencyFinder(ks);
		incFinder.setStopIfInconsistencyFound(false);
		incFinder.addProgressMonitor(new ConsoleSPARQLBasedInconsistencyProgressMonitor());
	}

	/**
	 * Test method for {@link org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedTrivialInconsistencyFinder#run(boolean)}.
	 */
	@Test
	public void testRun() {
		incFinder.run();
	}

	/**
	 * Test method for {@link org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedTrivialInconsistencyFinder#getExplanations()}.
	 */
	@Test
	public void testGetExplanations() {
		incFinder.run();
		Set<Explanation<OWLAxiom>> explanations = incFinder.getExplanations();
		for (Explanation<OWLAxiom> explanation : explanations) {
			System.out.println(explanation.getAxioms());
		}
	}

}
