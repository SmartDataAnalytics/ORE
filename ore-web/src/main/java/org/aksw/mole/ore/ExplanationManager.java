package org.aksw.mole.ore;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aksw.mole.ore.explanation.api.Explanation;
import org.aksw.mole.ore.explanation.api.ExplanationType;
import org.aksw.mole.ore.explanation.formatter.ExplanationFormatter;
import org.aksw.mole.ore.explanation.formatter.ExplanationFormatter.FormattedExplanation;
import org.aksw.mole.ore.explanation.impl.AxiomUsageChecker;
import org.aksw.mole.ore.explanation.impl.CachingExplanationGenerator;
import org.aksw.mole.ore.explanation.impl.PelletExplanationGenerator;
import org.aksw.mole.ore.explanation.impl.laconic.LaconicExplanationGenerator;
import org.aksw.mole.ore.explanation.impl.laconic.RemainingAxiomPartsGenerator;
import org.aksw.mole.ore.rootderived.RootClassFinder;
import org.aksw.mole.ore.rootderived.StructureBasedRootClassFinder;
import org.apache.log4j.Logger;
import org.dllearner.core.owl.Individual;
import org.dllearner.learningproblems.EvaluatedDescriptionClass;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.clarkparsia.owlapi.explanation.GlassBoxExplanation;
import com.clarkparsia.owlapi.explanation.util.ExplanationProgressMonitor;

public class ExplanationManager {
	
	private final Logger logger = Logger.getLogger(ExplanationManager.class);
	
	private CachingExplanationGenerator regularExplanationGenerator;
	private CachingExplanationGenerator laconicExplanationGenerator;
	
	private RootClassFinder rootClassFinder;
	
	private ExplanationType explanationType = ExplanationType.REGULAR;
	private int explanationLimit = 1;
	
	private OWLOntology ontology;
	private OWLReasoner reasoner;
	private OWLReasonerFactory reasonerFactory;
	private OWLOntologyManager manager;
	private OWLDataFactory dataFactory = new OWLDataFactoryImpl();
	
	private AxiomUsageChecker usageChecker;
	private ExplanationFormatter formatter;
	
	private Collection<ExplanationManagerListener> listeners = new HashSet<ExplanationManagerListener>();
	
	static{setup();}
	
	public ExplanationManager(OWLReasoner reasoner){
		this.reasoner = reasoner;
		this.ontology = reasoner.getRootOntology();
		
		PelletExplanationGenerator pelletExpGen = new PelletExplanationGenerator(ontology);
		regularExplanationGenerator = new CachingExplanationGenerator(pelletExpGen);
		
		LaconicExplanationGenerator laconicExpGen = new LaconicExplanationGenerator(reasoner);
		laconicExplanationGenerator = new CachingExplanationGenerator(laconicExpGen);
		
		rootClassFinder = new StructureBasedRootClassFinder(reasoner);
		
		usageChecker = new AxiomUsageChecker(ontology);
		formatter = new ExplanationFormatter();
	}
	
	public static void setup() {
		GlassBoxExplanation.setup();
	}
	
	public Set<OWLClass> getUnsatisfiableClasses(){
		Set<OWLClass> unsatClasses = new HashSet<OWLClass>();
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
	
	public Set<Explanation> getExplanations(OWLAxiom entailment, ExplanationType type, int limit){
		logger.info("Computing max. " + limit + " "  + type + " explanations for " + entailment + "...");
		long startTime = System.currentTimeMillis();
		Set<Explanation> explanations;
		if(type == ExplanationType.REGULAR){
			explanations = regularExplanationGenerator.getExplanations(entailment, limit);
		} else {
			explanations = laconicExplanationGenerator.getExplanations(entailment, limit);
		}
		logger.info("Got " + explanations.size() + " explanations in " + (System.currentTimeMillis()-startTime) + "ms.");
		return explanations;
	}
	
	public Set<Explanation> getUnsatisfiabilityExplanations(OWLClass cls){
		OWLAxiom entailment = dataFactory.getOWLSubClassOfAxiom(cls, dataFactory.getOWLNothing());
		return getExplanations(entailment, explanationType, explanationLimit);
	}
	
	public Set<Explanation> getUnsatisfiabilityExplanations(OWLClass cls, ExplanationType type, int limit){
		OWLAxiom entailment = dataFactory.getOWLSubClassOfAxiom(cls, dataFactory.getOWLNothing());
		return getExplanations(entailment, type, limit);
	}
	
	public Set<Explanation> getUnsatisfiabilityExplanations(OWLClass cls, ExplanationType type){
		return getUnsatisfiabilityExplanations(cls, type, Integer.MAX_VALUE);
	}
	
	public Set<Explanation> getInconsistencyExplanations(){
		return getInconsistencyExplanations(explanationType, explanationLimit);
	}
	
	public Set<Explanation> getInconsistencyExplanations(ExplanationType type, int limit){
		OWLAxiom entailment = dataFactory.getOWLSubClassOfAxiom(dataFactory.getOWLThing(), dataFactory.getOWLNothing());
		return getExplanations(entailment, type, limit);
	}
	
	public Set<Explanation> getInconsistencyExplanations(ExplanationType type){
		return getInconsistencyExplanations(type, Integer.MAX_VALUE);
	}
	
	public Set<Explanation> getClassAssertionExplanations(Individual ind, EvaluatedDescriptionClass evalDesc){
		OWLAxiom entailment = dataFactory.getOWLClassAssertionAxiom(OWLAPIConverter.getOWLAPIDescription(evalDesc.getDescription()), dataFactory.getOWLNamedIndividual(IRI.create(ind.getName())));
		return getExplanations(entailment, explanationType, explanationLimit);
	}
	
	public FormattedExplanation format(Explanation explanation){
		return formatter.getFormattedExplanation(explanation);
	}
	
	public int getAxiomFrequency(OWLAxiom axiom, ExplanationType explanationType){
		if(explanationType == ExplanationType.REGULAR){
			return regularExplanationGenerator.getAxiomFrequency(axiom);
		} else {
			return laconicExplanationGenerator.getAxiomFrequency(axiom);
		}
	}
	
	public int getAxiomFrequency(OWLAxiom axiom){
		if(explanationType == ExplanationType.REGULAR){
			return regularExplanationGenerator.getAxiomFrequency(axiom);
		} else {
			return laconicExplanationGenerator.getAxiomFrequency(axiom);
		}
	}
	
	public int getMaxAxiomFrequency(ExplanationType explanationType){
		if(explanationType == ExplanationType.REGULAR){
			return regularExplanationGenerator.getMaxAxiomFrequency();
		} else {
			return laconicExplanationGenerator.getMaxAxiomFrequency();
		}
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
	
	public void setExplanationProgressMonitor(ExplanationProgressMonitor expProgressMon) {
		regularExplanationGenerator.setProgressMonitor(expProgressMon);
		laconicExplanationGenerator.setProgressMonitor(expProgressMon);
	}
	
	public void clearCache(){
		logger.info("Clear cache");
		regularExplanationGenerator.clear();
		laconicExplanationGenerator.clear();
	}
	
	public void refreshRootClassFinder() {
		((StructureBasedRootClassFinder)rootClassFinder).refresh();
	}
	
	public int getAxiomUsageCount(OWLAxiom axiom){
		return usageChecker.getUsage(axiom).size();
	}
	
	public int getMaxAxiomUsage(ExplanationType explanationType){
		int maxUsage = -1;
		Set<Explanation> explanations;
		if(explanationType == ExplanationType.REGULAR){
			explanations = regularExplanationGenerator.getAllComputedExplanations();
		} else {
			explanations = laconicExplanationGenerator.getAllComputedExplanations();
		}
		for(Explanation exp : explanations){
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
	
	public static void main(String[] args) throws Exception {
		OWLOntology ont = OWLManager.createOWLOntologyManager().loadOntology(IRI.create("http://owl.cs.manchester.ac.uk/repository/download?ontology=http://www.co-ode.org/ontologies/pizza/pizza.owl&format=OWL/XML"));
		System.out.println(ont);
	}
	
}
