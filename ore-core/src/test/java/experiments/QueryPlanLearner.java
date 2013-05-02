package experiments;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.sparql.AxiomGenerationTracker;
import org.aksw.mole.ore.sparql.TimeOutException;
import org.aksw.mole.ore.sparql.generator.AxiomGenerator;
import org.aksw.mole.ore.sparql.generator.SPARQLBasedInconsistencyFinder;
import org.aksw.mole.ore.util.HermiTReasonerFactory;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owl.explanation.api.ConsoleExplanationProgressMonitor;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationProgressMonitor;
import org.semanticweb.owl.explanation.impl.blackbox.Configuration;
import org.semanticweb.owl.explanation.impl.blackbox.EntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.ConsistencyEntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.InconsistentOntologyExplanationGeneratorFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;

public class QueryPlanLearner {

	private SparqlEndpointKS ks;
	
	private OWLOntologyManager manager;
	private OWLDataFactory dataFactory;
	private OWLReasonerFactory reasonerFactory;
	
	private Set<String> linkedDataNamespaces = Sets.newHashSet("http://www.w3.org/2003/01/geo/wgs84_pos#");
	private boolean useLinkedData = true;
	private boolean stopIfInconsistencyFound = true;
	private long maxExecutionTimeInSeconds = TimeUnit.MINUTES.toSeconds(1);
	private SPARQLBasedInconsistencyFinder inconsistencyFinder;
	private AxiomGenerationTracker tracker = new AxiomGenerationTracker();
	
	private ExplanationGeneratorFactory<OWLAxiom> explanationGeneratorFactory;
	private ExplanationProgressMonitor<OWLAxiom> explanationProgressMonitor = null;//new ConsoleExplanationProgressMonitor<OWLAxiom>();
	private boolean useModularization = true;
	private OWLAxiom inconsistencyEntailment;
	
	private int maxNrOfIterations = 5;
	
	public QueryPlanLearner(SparqlEndpointKS ks) {
		this.ks = ks;
		
		//decide which reasoner we use here
		reasonerFactory = PelletReasonerFactory.getInstance();
//		reasonerFactory = new HermiTReasonerFactory();
		
		//create and configure the inconsistent fragment extraction algorithm
		inconsistencyFinder = new SPARQLBasedInconsistencyFinder(ks, reasonerFactory);
		inconsistencyFinder.setUseLinkedData(useLinkedData);
		inconsistencyFinder.setLinkedDataNamespaces(linkedDataNamespaces);
		inconsistencyFinder.setStopIfInconsistencyFound(stopIfInconsistencyFound);
		inconsistencyFinder.setMaximumRuntime(maxExecutionTimeInSeconds, TimeUnit.SECONDS);
		inconsistencyFinder.setAxiomGenerationTracker(tracker);
		
		//create the generator for explanations
		EntailmentCheckerFactory<OWLAxiom> checkerFactory = new ConsistencyEntailmentCheckerFactory(reasonerFactory, Long.MAX_VALUE);
		Configuration<OWLAxiom> configuration = new Configuration<OWLAxiom>(checkerFactory);
		explanationGeneratorFactory = new InconsistentOntologyExplanationGeneratorFactory(reasonerFactory, Long.MAX_VALUE);
		manager = OWLManager.createOWLOntologyManager();
		dataFactory = manager.getOWLDataFactory();
		inconsistencyEntailment = dataFactory.getOWLSubClassOfAxiom(dataFactory.getOWLThing(), dataFactory.getOWLNothing());
	}
	
	public void start(){
		//basic workflow: 
		//loop
		//1. Compute an inconsistent fragment
		//2. Compute all explanations for inconsistency in that fragment
		//3. Track by which query the axioms in the explanation were (first) generated
		//4. Ignore axioms
		
		try {
			int iteration = 1;
			Set<OWLAxiom> axiomsToIgnore = new HashSet<OWLAxiom>();
			do{
				//compute an inconsistent fragment
				Set<OWLAxiom> inconsistentFragment = inconsistencyFinder.getInconsistentFragment();
				
				//compute the explanations for inconsistency in that fragment
				Set<Explanation<OWLAxiom>> explanations = computeExplanations(inconsistentFragment, 10);
				
				//compute the frequency of each axiom in the set of explanations by simply adding all axioms to a multiset
				Multiset<OWLAxiom> axiomsMultiset = computeAxiomFrequency(explanations);
				
				//track the kind of query generator, which was used to generate each of the axioms occurring in the explanations
				for (OWLAxiom axiom : axiomsMultiset.elementSet()) {
					AxiomGenerator generator = tracker.generatedFirstBy(axiom);
//					System.out.println(axiom + ": " + generator.getClass().getSimpleName());
				}
				
				//compute the set of axioms which have to be ignored in the next iteration
				axiomsToIgnore.addAll(axiomsMultiset.elementSet());
				
				//set the axioms as ignored
				inconsistencyFinder.setAxiomsToIgnore(axiomsToIgnore);
			} while (++iteration < maxNrOfIterations);
			
		} catch (TimeOutException e) {
			System.err.println("Got timeout.");
		}
	}
	
	private Multiset<OWLAxiom> computeAxiomFrequency(Set<Explanation<OWLAxiom>> explanations){
		SortedMultiset<OWLAxiom> axiomsMultiset = TreeMultiset.create();
		for (Explanation<OWLAxiom> explanation : explanations) {
			axiomsMultiset.addAll(explanation.getAxioms());
		}
		ImmutableMultiset<OWLAxiom> sortedByFrequencyMultiset = Multisets.copyHighestCountFirst(axiomsMultiset);
		return sortedByFrequencyMultiset;
	}
	
	private void save(Set<OWLAxiom> axioms){
		try {
			manager.saveOntology(manager.createOntology(axioms), new TurtleOntologyFormat(), new FileOutputStream("inconsistent-fragment.ttl"));
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private Set<Explanation<OWLAxiom>> computeExplanations(Set<OWLAxiom> axioms, int limit){
		ExplanationGenerator<OWLAxiom> explanationGenerator = explanationGeneratorFactory.createExplanationGenerator(axioms, explanationProgressMonitor);
		Set<Explanation<OWLAxiom>> explanations = explanationGenerator.getExplanations(inconsistencyEntailment, limit);
		return explanations;
	}
	
	public static void main(String[] args) throws Exception {
		ToStringRenderer.getInstance().setRenderer(new DLSyntaxObjectRenderer());
		SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia(), new ExtractionDBCache("cache"));
		QueryPlanLearner learner = new QueryPlanLearner(ks);
		learner.start();
	}

}