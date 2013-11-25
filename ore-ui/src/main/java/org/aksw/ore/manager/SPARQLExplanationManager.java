package org.aksw.ore.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aksw.mole.ore.explanation.ExplanationCache;
import org.aksw.mole.ore.explanation.api.ExplanationType;
import org.aksw.mole.ore.explanation.formatter.ExplanationFormatter2;
import org.aksw.mole.ore.explanation.formatter.ExplanationFormatter2.FormattedExplanation;
import org.aksw.mole.ore.explanation.impl.laconic.RemainingAxiomPartsGenerator;
import org.apache.log4j.Logger;
import org.dllearner.core.owl.Individual;
import org.dllearner.learningproblems.EvaluatedDescriptionClass;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationException;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorInterruptedException;
import org.semanticweb.owl.explanation.impl.blackbox.Configuration;
import org.semanticweb.owl.explanation.impl.blackbox.EntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.BlackBoxExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.InconsistentOntologyExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.SatisfiabilityEntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.laconic.LaconicExplanationGeneratorFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.clarkparsia.owlapi.explanation.GlassBoxExplanation;

public class SPARQLExplanationManager {
	
	private final Logger logger = Logger.getLogger(SPARQLExplanationManager.class);
	
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
	
	private ExplanationFormatter2 formatter;
	
	private ExplanationType currentExplanationType;
	
	private Collection<ExplanationManagerListener> listeners = new HashSet<ExplanationManagerListener>();

	protected final OWLAxiom inconsistencyEntailment = dataFactory.getOWLSubClassOfAxiom(dataFactory.getOWLThing(), dataFactory.getOWLNothing());
	

	private Set<Explanation<OWLAxiom>> currentlyFoundExplanations;
	
	static{setup();}
	
	public SPARQLExplanationManager(OWLReasoner mainReasoner, OWLReasonerFactory reasonerFactory){
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
		
		formatter = new ExplanationFormatter2(ontology.getOWLOntologyManager());
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
	
	/**
	 * @return the explanationLimit
	 */
	public int getExplanationLimit() {
		return explanationLimit;
	}
	
	/**
	 * @return the explanationType
	 */
	public ExplanationType getExplanationType() {
		return explanationType;
	}
	
	public Set<Explanation<OWLAxiom>> getExplanations(OWLAxiom entailment, ExplanationType type, int limit){
		this.currentExplanationType = type;
		logger.info("Computing max. " + limit + " "  + type + " explanations for " + entailment + "...");
		long startTime = System.currentTimeMillis();
		ExplanationCache cache = explanationType2Cache.get(type);
		Set<Explanation<OWLAxiom>> explanations = cache.getExplanations(entailment, limit);
		if(explanations == null || (explanations.size() < limit && !cache.allExplanationsFound(entailment))){
			ExplanationGeneratorFactory<OWLAxiom> explanationGeneratorFactory = getExplanationGeneratorFactory();
			try {
				explanations = explanationGeneratorFactory.createExplanationGenerator(ontology).getExplanations(entailment, limit);
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
		return explanations;
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
	
	public void setExplanations(Set<Explanation<OWLAxiom>> explanations){
		ExplanationCache cache = explanationType2Cache.get(explanationType);
		cache.addExplanations(inconsistencyEntailment, explanations);
	}
	
	public int getAxiomFrequency(OWLAxiom axiom){
		return getAxiomFrequency(axiom, explanationType);
	}
	
	public int getMaxAxiomFrequency(ExplanationType explanationType){
		ExplanationCache cache = explanationType2Cache.get(explanationType);
		return cache.getMaxAxiomFrequency();
	}
	
	public void setExplanationType(ExplanationType explanationType){
		this.explanationType = explanationType;
		fireExplanationTypeChanged();
	}
	
	public Map<OWLAxiom, Set<OWLAxiom>> getRemainingAxiomParts(OWLAxiom axiom){
		RemainingAxiomPartsGenerator gen = new RemainingAxiomPartsGenerator(ontology, new OWLDataFactoryImpl());
		return gen.getRemainingAxiomParts(axiom);
	}
	
	public void clearCache(){
		logger.info("Clear cache");
		for(ExplanationCache cache : explanationType2Cache.values()){
			cache.clear();
		}
	}
	
	public void setExplanationLimit(int explanationLimit){
		this.explanationLimit = explanationLimit;
		fireExplanationLimitChanged();
	}
	
	public boolean isAxiomPart(OWLAxiom axiom){
		return !ontology.containsAxiom(axiom, true);
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
}
