package org.aksw.ore.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.mole.ore.explanation.ExplanationCache;
import org.aksw.mole.ore.explanation.api.ExplanationType;
import org.aksw.mole.ore.explanation.formatter.ExplanationFormatter2;
import org.aksw.mole.ore.explanation.formatter.ExplanationFormatter2.FormattedExplanation;
import org.aksw.mole.ore.explanation.impl.AxiomUsageChecker;
import org.aksw.mole.ore.explanation.impl.laconic.RemainingAxiomPartsGenerator;
import org.aksw.mole.ore.rootderived.RootClassFinder;
import org.aksw.mole.ore.rootderived.StructureBasedRootClassFinder;
import org.apache.log4j.Logger;
import org.dllearner.core.owl.Individual;
import org.dllearner.learningproblems.EvaluatedDescriptionClass;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationException;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorInterruptedException;
import org.semanticweb.owl.explanation.api.ExplanationProgressMonitor;
import org.semanticweb.owl.explanation.impl.blackbox.Configuration;
import org.semanticweb.owl.explanation.impl.blackbox.EntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.BlackBoxExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.InconsistentOntologyExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.SatisfiabilityEntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.laconic.LaconicExplanationGeneratorFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

import com.clarkparsia.owlapi.explanation.GlassBoxExplanation;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class ExplanationManager implements ExplanationProgressMonitor<OWLAxiom>{
	
	private final Logger logger = Logger.getLogger(ExplanationManager.class);
	
	private RootClassFinder rootClassFinder;
	
	private ExplanationType explanationType = ExplanationType.REGULAR;
	private int explanationLimit = 1;
	
	private ExplanationGeneratorFactory<OWLAxiom> regularExplanationGeneratorFactory;
	private ExplanationGeneratorFactory<OWLAxiom> laconicExplanationGeneratorFactory;
	private ExplanationGeneratorFactory<OWLAxiom> regularInconsistencyExplanationGeneratorFactory;
	private ExplanationGeneratorFactory<OWLAxiom> laconicInconsistencyExplanationGeneratorFactory;
	
	private Map<ExplanationType, ExplanationCache> explanationType2Cache = new HashMap<ExplanationType, ExplanationCache>();
	
	private OWLOntology ontology;
	private OWLReasoner reasoner;
	private OWLReasonerFactory reasonerFactory;
	private OWLOntologyManager manager;
	private OWLDataFactory dataFactory = new OWLDataFactoryImpl();
	
	private AxiomUsageChecker usageChecker;
	private ExplanationFormatter2 formatter;
	
	private ExplanationType currentExplanationType;
	
	private Collection<ExplanationManagerListener> listeners = new HashSet<ExplanationManagerListener>();

	private List<ExplanationProgressMonitorExtended<OWLAxiom>> explanationProgressMonitors = new ArrayList<ExplanationProgressMonitorExtended<OWLAxiom>>();

	private Set<Explanation<OWLAxiom>> currentlyFoundExplanations;
	
	static{setup();}
	
	public ExplanationManager(OWLReasoner mainReasoner, OWLReasonerFactory reasonerFactory){
		this.reasoner = mainReasoner;
		this.ontology = reasoner.getRootOntology();
		this.reasonerFactory = reasonerFactory;
		
		regularExplanationGeneratorFactory = createExplanationGeneratorFactory(ExplanationType.REGULAR);
		laconicExplanationGeneratorFactory = createExplanationGeneratorFactory(ExplanationType.LACONIC);
		regularInconsistencyExplanationGeneratorFactory = new InconsistentOntologyExplanationGeneratorFactory(reasonerFactory, Long.MAX_VALUE);
		laconicInconsistencyExplanationGeneratorFactory = new LaconicExplanationGeneratorFactory<OWLAxiom>(new InconsistentOntologyExplanationGeneratorFactory(reasonerFactory, Long.MAX_VALUE));
		
		for(ExplanationType type : ExplanationType.values()){
			explanationType2Cache.put(type, new ExplanationCache(type));
		}
		
		rootClassFinder = new StructureBasedRootClassFinder(reasoner);
		
		usageChecker = new AxiomUsageChecker(ontology);
		formatter = new ExplanationFormatter2();
	}
	
	private ExplanationGeneratorFactory<OWLAxiom> createExplanationGeneratorFactory(ExplanationType type){
		boolean useModularization = true;
		EntailmentCheckerFactory<OWLAxiom> checkerFactory = new SatisfiabilityEntailmentCheckerFactory(reasonerFactory, useModularization);
		Configuration<OWLAxiom> configuration = new Configuration<OWLAxiom>(checkerFactory);
		ExplanationGeneratorFactory<OWLAxiom> explanationGeneratorFactory = new BlackBoxExplanationGeneratorFactory<OWLAxiom>(configuration);
		if(type == ExplanationType.LACONIC){
			return new LaconicExplanationGeneratorFactory<OWLAxiom>(explanationGeneratorFactory);
		}
		return explanationGeneratorFactory;
	}
	
	public static void setup() {
		GlassBoxExplanation.setup();
	}
	
	/**
	 * @param reasoner the reasoner to set
	 */
	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
		this.ontology = reasoner.getRootOntology();
	}
	
	/**
	 * @param reasonerFactory the reasonerFactory to set
	 */
	public void setReasonerFactory(OWLReasonerFactory reasonerFactory) {
		this.reasonerFactory = reasonerFactory;
	}
	
	public Set<OWLClass> getUnsatisfiableClasses(){
		Set<OWLClass> unsatClasses = new TreeSet<OWLClass>();
		unsatClasses.addAll(getDerivedUnsatisfiableClasses());
		unsatClasses.addAll(getRootUnsatisfiableClasses());
		return unsatClasses;
	}
	
	public Set<OWLClass> getRootUnsatisfiableClasses(){
		return rootClassFinder.getRootUnsatisfiableClasses();
	}
	
	public Set<OWLClass> getDerivedUnsatisfiableClasses(){
		return rootClassFinder.getDerivedUnsatisfiableClasses();
	}
	
	public Set<Explanation<OWLAxiom>> getExplanations(OWLAxiom entailment, ExplanationType type, int limit){
		this.currentExplanationType = type;
		logger.info("Computing max. " + limit + " "  + type + " explanations for " + entailment + "...");
		long startTime = System.currentTimeMillis();
		ExplanationCache cache = explanationType2Cache.get(type);
		Set<Explanation<OWLAxiom>> explanations = cache.getExplanations(entailment, limit);
		System.out.println(entailment + ":" + explanations);
		if(explanations == null || (explanations.size() < limit && !cache.allExplanationsFound(entailment))){
			ExplanationGeneratorFactory<OWLAxiom> explanationGeneratorFactory = getExplanationGeneratorFactory();
			try {
				explanations = explanationGeneratorFactory.createExplanationGenerator(ontology, this).getExplanations(entailment, limit);
			} catch (ExplanationException e) {
				if(e instanceof ExplanationGeneratorInterruptedException){
					explanations = currentlyFoundExplanations;
				} else {
					e.printStackTrace();
				}
			}
			cache.addExplanations(entailment, explanations);
			if(explanations.size() < limit){
				cache.setAllExplanationsFound(entailment);
			}
		}
		logger.info("Got " + explanations.size() + " explanations in " + (System.currentTimeMillis()-startTime) + "ms.");
		fireAllExplanationsFound(explanations);
		return explanations;
	}
	
	public Set<Explanation<OWLAxiom>> getUnsatisfiabilityExplanations(OWLClass cls){
		return getUnsatisfiabilityExplanations(cls, explanationType, Integer.MAX_VALUE);
	}
	
	public Set<Explanation<OWLAxiom>> getUnsatisfiabilityExplanations(OWLClass cls, int limit){
		return getUnsatisfiabilityExplanations(cls, explanationType, limit);
	}
	
	public Set<Explanation<OWLAxiom>> getUnsatisfiabilityExplanations(OWLClass cls, ExplanationType type, int limit){
		OWLAxiom entailment = dataFactory.getOWLSubClassOfAxiom(cls, dataFactory.getOWLNothing());
		return getExplanations(entailment, type, limit);
	}
	
	public Set<Explanation<OWLAxiom>> getUnsatisfiabilityExplanations(OWLClass cls, ExplanationType type){
		return getUnsatisfiabilityExplanations(cls, type, Integer.MAX_VALUE);
	}
	
	public Set<Explanation<OWLAxiom>> getInconsistencyExplanations(){
		return getInconsistencyExplanations(explanationType, explanationLimit);
	}
	
	public Set<Explanation<OWLAxiom>> getInconsistencyExplanations(ExplanationType type, int limit){
		OWLAxiom entailment = dataFactory.getOWLSubClassOfAxiom(dataFactory.getOWLThing(), dataFactory.getOWLNothing());
		return getExplanations(entailment, type, limit);
	}
	
	public Set<Explanation<OWLAxiom>> getInconsistencyExplanations(ExplanationType type){
		return getInconsistencyExplanations(type, Integer.MAX_VALUE);
	}
	
	public Set<Explanation<OWLAxiom>> getClassAssertionExplanations(Individual ind, EvaluatedDescriptionClass evalDesc){
		OWLAxiom entailment = dataFactory.getOWLClassAssertionAxiom(OWLAPIConverter.getOWLAPIDescription(evalDesc.getDescription()), dataFactory.getOWLNamedIndividual(IRI.create(ind.getName())));
		return getExplanations(entailment, explanationType, explanationLimit);
	}
	
	private ExplanationGeneratorFactory<OWLAxiom> getExplanationGeneratorFactory(){
		if(reasoner.isConsistent()){
			if(explanationType == ExplanationType.REGULAR){
				return regularExplanationGeneratorFactory;
			}
			return laconicExplanationGeneratorFactory;
		} else {
			if(explanationType == ExplanationType.REGULAR){
				return regularInconsistencyExplanationGeneratorFactory;
			}
			return laconicInconsistencyExplanationGeneratorFactory;
		}
	}
	
	public FormattedExplanation format(Explanation<OWLAxiom> explanation){
		return formatter.getFormattedExplanation(explanation);
	}
	
	public int getAxiomFrequency(OWLAxiom axiom, ExplanationType explanationType){
		ExplanationCache cache = explanationType2Cache.get(explanationType);
		return cache.getAxiomFrequency(axiom);
	}
	
	public int getAxiomFrequency(OWLAxiom axiom){
		return getAxiomFrequency(axiom, explanationType);
	}
	
	public int getMaxAxiomFrequency(ExplanationType explanationType){
		ExplanationCache cache = explanationType2Cache.get(explanationType);
		return cache.getMaxAxiomFrequency();
	}
	
	public double getAxiomRelevanceScore(OWLAxiom axiom, ExplanationType explanationType){
		int frequency = getAxiomFrequency(axiom, explanationType);
		int maxFrequency = getMaxAxiomFrequency(explanationType);
		int usage = getAxiomUsageCount(axiom);
		int maxUsage = getMaxAxiomUsage(explanationType);
		double synRelevance = 0.5 * ( (double)frequency/(double)maxFrequency
				+ 1.0 - (double)usage/(double)maxUsage);
		return Math.round(synRelevance * 100)/100d;
	}
	
	public double getAxiomRelevanceScore(OWLAxiom axiom){
		int frequency = getAxiomFrequency(axiom, explanationType);
		int maxFrequency = getMaxAxiomFrequency(explanationType);
		int usage = getAxiomUsageCount(axiom);
		int maxUsage = getMaxAxiomUsage(explanationType);
		double synRelevance = 0.5 * ( (double)frequency/(double)maxFrequency
				+ 1.0 - (double)usage/(double)maxUsage);
		return Math.round(synRelevance * 100)/100d;
	}
	
	public void setExplanationType(ExplanationType explanationType){
		this.explanationType = explanationType;
		fireExplanationTypeChanged();
	}
	
	public Map<OWLAxiom, Set<OWLAxiom>> getRemainingAxiomParts(OWLAxiom axiom){
		RemainingAxiomPartsGenerator gen = new RemainingAxiomPartsGenerator(ontology, new OWLDataFactoryImpl());
		return gen.getRemainingAxiomParts(axiom);
	}
	
	public void addExplanationProgressMonitor(ExplanationProgressMonitorExtended<OWLAxiom> explanationProgressMonitor) {
		explanationProgressMonitors.add(explanationProgressMonitor);
	}
	
	public void removeExplanationProgressMonitor(ExplanationProgressMonitorExtended<OWLAxiom> explanationProgressMonitor) {
		explanationProgressMonitors.remove(explanationProgressMonitor);
	}
	
	public void clearCache(){
		logger.info("Clear cache");
		for(ExplanationCache cache : explanationType2Cache.values()){
			cache.clear();
		}
	}
	
	public void refreshRootClassFinder() {
		((StructureBasedRootClassFinder)rootClassFinder).refresh();
	}
	
	public int getAxiomUsageCount(OWLAxiom axiom){
		return usageChecker.getUsage(axiom).size();
	}
	
	public int getMaxAxiomUsage(ExplanationType explanationType){
		int maxUsage = -1;
		ExplanationCache cache = explanationType2Cache.get(explanationType);
		Set<Explanation<OWLAxiom>> explanations = cache.getAllComputedExplanations();
		for(Explanation<OWLAxiom> exp : explanations){
			for(OWLAxiom ax : exp.getAxioms()){
				int usage = usageChecker.getUsage(ax).size();{
					if(usage > maxUsage){
						maxUsage = usage;
					}
				}
			}
		}
		return maxUsage;
	}
	
	public void setExplanationLimit(int explanationLimit){
		this.explanationLimit = explanationLimit;
		fireExplanationLimitChanged();
	}
	
	public boolean isAxiomPart(OWLAxiom axiom){
		return !ontology.containsAxiom(axiom, true);
	}
	
	public Set<OWLAxiom> getAxiomUsage(OWLAxiom axiom){
		return usageChecker.getUsage(axiom);
	}
	
	public boolean isConsistent(){
		return reasoner.isConsistent();
	}
	
	public void addListener(ExplanationManagerListener l){
		listeners.add(l);
	}
	
	public void removeListener(ExplanationManagerListener l){
		listeners.remove(l);
	}
	
	private void fireExplanationLimitChanged(){
		for(ExplanationManagerListener l : listeners){
			l.explanationLimitChanged(explanationLimit);
		}
	}
	
	private void fireExplanationTypeChanged(){
		for(ExplanationManagerListener l : listeners){
			l.explanationTypeChanged(explanationType);
		}
	}
	
	private void fireAllExplanationsFound(Set<Explanation<OWLAxiom>> explanations){
		for (ExplanationProgressMonitorExtended<OWLAxiom> mon : explanationProgressMonitors) {
			mon.allExplanationsFound(explanations);
		}
	}
	
	public static void main(String[] args) throws Exception {
		ToStringRenderer.getInstance().setRenderer(new DLSyntaxObjectRenderer());
		String ontologyURL = "http://owl.cs.manchester.ac.uk/repository/download?ontology=http://www.co-ode.org/ontologies/pizza/pizza.owl&format=OWL/XML";
		ontologyURL = "file:/home/me/work/ORE_old/ore-core/dataset/BioPortal/inconsistent/Influenza+Ontology";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = man.getOWLDataFactory();
		OWLOntology ontology = man.loadOntology(IRI.create(ontologyURL));
		System.out.println(ontology.getLogicalAxiomCount());
		OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		
		ExplanationManager manager = new ExplanationManager(reasoner, reasonerFactory);
		manager.setExplanationLimit(2);
		Set<Explanation<OWLAxiom>> inconsistencyExplanations = manager.getInconsistencyExplanations();
		for (Explanation<OWLAxiom> explanation : inconsistencyExplanations) {
			for (OWLAxiom axiom : explanation.getAxioms()) {
				System.out.println(axiom);
				for(OWLOntology importedOntology : ontology.getImports()){
					if(importedOntology.containsAxiom(axiom, true)){
						System.out.println(importedOntology);
					} 
				}
				
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.explanation.api.ExplanationProgressMonitor#foundExplanation(org.semanticweb.owl.explanation.api.ExplanationGenerator, org.semanticweb.owl.explanation.api.Explanation, java.util.Set)
	 */
	@Override
	public void foundExplanation(ExplanationGenerator<OWLAxiom> expGen, Explanation<OWLAxiom> explanation,
			Set<Explanation<OWLAxiom>> allExplanations) {
		this.currentlyFoundExplanations = allExplanations;
		for (ExplanationProgressMonitor<OWLAxiom> mon : explanationProgressMonitors) {
			mon.foundExplanation(expGen, explanation, allExplanations);
		}
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.explanation.api.ExplanationProgressMonitor#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		for (ExplanationProgressMonitor<OWLAxiom> mon : explanationProgressMonitors) {
			if(mon.isCancelled() == true){
				return true;
			}
		}
		return false;
	}
	
}
