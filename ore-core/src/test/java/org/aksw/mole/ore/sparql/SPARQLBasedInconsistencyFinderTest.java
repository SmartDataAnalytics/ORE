package org.aksw.mole.ore.sparql;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.sparql.generator.SPARQLBasedInconsistencyFinder;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.Sets;

public class SPARQLBasedInconsistencyFinderTest {

	private SparqlEndpointKS ks;
	private OWLReasonerFactory reasonerFactory;
	private Set<String> linkedDataNamespaces = Sets.newHashSet("http://www.w3.org/2003/01/geo/wgs84_pos#");
	private boolean useLinkedData = true;
	private boolean stopIfInconsistencyFound = false;
	private long maxExecutionTimeInSeconds = 60;
	
	@Before
	public void setUp() throws Exception {
		ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia(), new ExtractionDBCache("cache"));
//		ks = new SparqlEndpointKS(new SparqlEndpoint(new URL("http://lod.openlinksw.com/sparql"), "http://dbpedia.org"));
		reasonerFactory = PelletReasonerFactory.getInstance();
	}

	@Test
	public void testGetInconsistentFragment() {
		SPARQLBasedInconsistencyFinder inconsistencyFinder = new SPARQLBasedInconsistencyFinder(ks, reasonerFactory);
		inconsistencyFinder.setUseLinkedData(useLinkedData );
		inconsistencyFinder.setLinkedDataNamespaces(linkedDataNamespaces);
		inconsistencyFinder.setStopIfInconsistencyFound(stopIfInconsistencyFound);
		inconsistencyFinder.setMaximumRuntime(maxExecutionTimeInSeconds, TimeUnit.SECONDS);
		try {
			Set<OWLAxiom> inconsistentFragment = inconsistencyFinder.getInconsistentFragment();
			System.out.println("Got inconsistent fragment containing " + inconsistentFragment.size() + " axioms.");
		} catch (TimeOutException e) {
			System.err.println("Got timeout.");
		}
	}

}
