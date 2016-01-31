package experiments;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.*;
import org.aksw.mole.ore.sparql.AxiomGenerationDistribution;
import org.aksw.mole.ore.sparql.AxiomGenerationTracker;
import org.aksw.mole.ore.sparql.TimeOutException;
import org.aksw.mole.ore.sparql.generator.AxiomGenerator;
import org.aksw.mole.ore.sparql.generator.SPARQLBasedInconsistencyFinder;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationProgressMonitor;
import org.semanticweb.owl.explanation.impl.blackbox.Configuration;
import org.semanticweb.owl.explanation.impl.blackbox.EntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.ConsistencyEntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.InconsistentOntologyExplanationGeneratorFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
		
		// optimisation:
		// option 1: reinforcement learning
		// option 2: save all fragments and remove while inconsistent
		// 2a: random removal, 2b: minimal fragment / diagnosis
		Set<AxiomGenerationDistribution> distributions = new HashSet<AxiomGenerationDistribution>();
//		Map<AxiomGenerator,Double> 
		// option 3: percentage of axiom type in justifications
		Multiset<String> axiomGenerators = TreeMultiset.create(); 
		
//		boolean useOption1 = false;
		boolean useOption2 = false;
		boolean useOption3 = true;
		
		try {
			int iteration = 1;
			Set<OWLAxiom> axiomsToIgnore = new HashSet<OWLAxiom>();
			do{
				//compute an inconsistent fragment
				Set<OWLAxiom> inconsistentFragment = inconsistencyFinder.getInconsistentFragment();
				
				// option 2: remove 
				if(useOption2) {
					Set<OWLAxiom> inconsistentFragmentCopy = new TreeSet<OWLAxiom>(inconsistentFragment);
					removeWhileInconsistent(inconsistentFragmentCopy);
					distributions.add(new AxiomGenerationDistribution(tracker, inconsistentFragmentCopy));
				}
				
				//compute the explanations for inconsistency in that fragment
				Set<Explanation<OWLAxiom>> explanations = computeExplanations(inconsistentFragment, 10);
				
				//compute the frequency of each axiom in the set of explanations by simply adding all axioms to a multiset
				Multiset<OWLAxiom> axiomsMultiset = computeAxiomFrequency(explanations);
				
				//track the kind of query generator, which was used to generate each of the axioms occurring in the explanations
				for (OWLAxiom axiom : axiomsMultiset.elementSet()) {
					AxiomGenerator generator = tracker.generatedFirstBy(axiom);
					axiomGenerators.add(generator.getClass().getName());
//					generator.
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
		
		// print frequency of axiom generators in justifications (option 3)
		if(useOption3) {
			int total = axiomGenerators.size();
			for(String gen : axiomGenerators.elementSet()) {
				int count = axiomGenerators.count(gen);
				System.out.println(gen + ": " + count + " = " + (count/total) + "%");
			}
		}
		
		// average frequency in obtained fragments (option 2)
		AxiomGenerationDistribution agd = AxiomGenerationDistribution.computeAverage(distributions);
		System.out.println(agd);
		
	}
	
	private void removeWhileInconsistent(Set<OWLAxiom> axioms) {
		OWLAxiom ax;
		do {
			ax = removeRandomElementFromSet(axioms);
		} while(!isConsistent(axioms));
		axioms.add(ax);
	}
	
	private OWLAxiom removeRandomElementFromSet(Set<OWLAxiom> s) {
		int size = s.size();
		int item = new Random().nextInt(size);
		Iterator<OWLAxiom> it = s.iterator();
		int i = 0;
		OWLAxiom axiom;
		while(it.hasNext()) {
			axiom = it.next();
			if (i == item) {
				it.remove();
				return axiom;
			}
		    i = i + 1;
		}
		return null;
	}
	
	private boolean isConsistent(Set<OWLAxiom> axioms) {
		try {
			OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(manager.createOntology(axioms));
			return reasoner.isConsistent();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		return true;//will never happen
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
			manager.saveOntology(manager.createOntology(axioms), new TurtleDocumentFormat(), new FileOutputStream("inconsistent-fragment.ttl"));
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
		SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpediaLiveAKSW(), "cache");
		QueryPlanLearner learner = new QueryPlanLearner(ks);
		learner.start();
	}

}
