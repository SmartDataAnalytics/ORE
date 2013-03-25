import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;


public class OLiaCoherenceTest {

	private static final String OLIA_BASE = "http://purl.org/olia/";
	
	private List<String> oliaReferenceModels = Arrays.asList(new String[]{
		"olia",
		"system",
		"olia-top"
	});
	
	private List<String> englishModels = Arrays.asList(new String[]{
			"brown",
			"connexor",
			"eagles",
			"genia",
//			"http://nl.ijs.si/ME/owl/msd-en.owl",
			"penn",
			"penn-syntax",
			"qtag",
			"stanford",
			"susa"
	});
	
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private OWLReasoner reasoner;
	private OWLReasonerFactory reasonerFactory;
	private OWLDataFactory dataFactory;
	
	@Before
	public void setUp() throws Exception {
		manager = OWLManager.createOWLOntologyManager();
		Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
		for(String model : oliaReferenceModels){
			String iri = model.startsWith("http://") ? model : (OLIA_BASE + model + ".owl");
			OWLOntology ont = manager.loadOntology(IRI.create(iri));
			ontologies.add(ont);
		}
		for(String model : englishModels){
			//load the ANNOTAITON MODEL
			String iri = model.startsWith("http://") ? model : (OLIA_BASE + model + ".owl");
			OWLOntology ont = manager.loadOntology(IRI.create(iri));
			ontologies.add(ont);
			//load the corresponding LINKING MODEL
			iri = model.startsWith("http://") ? model : (OLIA_BASE + model + "-link.rdf");
			ont = manager.loadOntology(IRI.create(iri));
			ontologies.add(ont);
		}
		ontology = manager.createOntology(IRI.create("http://purl.org/olia/merged.owl"), ontologies);
		dataFactory = manager.getOWLDataFactory();
		reasonerFactory = PelletReasonerFactory.getInstance();
		reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY, InferenceType.CLASS_ASSERTIONS);
		
	}

	@Test
	public void testCoherency() {
		System.out.println(reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom());
	}

}
