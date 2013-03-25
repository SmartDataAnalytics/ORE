package org.aksw.mole.ore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.Intersection;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.core.owl.Union;
import org.dllearner.learningproblems.ClassLearningProblem;
import org.dllearner.learningproblems.EvaluatedDescriptionClass;
import org.dllearner.reasoning.PelletReasoner;
import org.dllearner.refinementoperators.RhoDRDown;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class LearningManager {
	
	public enum LearningType {
		EQUIVALENT, SUPER
	};

	private LearningType learningType = LearningType.EQUIVALENT;

	private ClassLearningProblem lp;
	private CELOE la;
	private PelletReasoner reasoner;

	private NamedClass class2Describe;

	private int maxExecutionTimeInSeconds = 10;
	private double noisePercentage;
	private double threshold;
	private int maxNrOfResults = 10;
	private int minInstanceCount;
	private boolean useExistentialQuantifier;
	private boolean useUniversalQuantifier;
	private boolean useNegation;
	private boolean useHasValue;
	private boolean useCardinality;
	private int cardinalityLimit;

	private boolean learningInProgress = false;
	
	private boolean prepared = false;
	private boolean preparing;
	
	public List<EvaluatedDescriptionClass> result;


	public LearningManager(PelletReasoner reasoner) {
		this.reasoner = reasoner;
	}

	public void setLearningType(LearningType learningType) {
		this.learningType = learningType;
	}

	public LearningType getLearningType() {
		return learningType;
	}
	
	public PelletReasoner getReasoner(){
		return reasoner;
	}
	
	public void prepareLearning(){
		preparing = true;
		if(!prepared){
			System.out.println("Initializing internal reasoner...");
			long startTime = System.currentTimeMillis();
			prepared = true;
			reasoner.realise();
			reasoner.dematerialise();
			System.out.println("...done in " + (System.currentTimeMillis()-startTime) + "ms.");
		}
		initLearningProblem();
		initLearningAlgorithm();
		preparing = false;
	}
	
	public List<NamedClass> getClasses(){
		reasoner.realise();
		reasoner.dematerialise();
		List<NamedClass> classes = new ArrayList<NamedClass>();
		for(NamedClass nc : reasoner.getAtomicConceptsList()){
			if(reasoner.getIndividuals(nc).size() >= 2){
				classes.add(nc);
			}
		}
		return classes;
	}

	public void initLearningProblem() {
		System.out.println("Initializing learning problem...");
		long startTime = System.currentTimeMillis();
		lp = new ClassLearningProblem(reasoner);
		try {
			lp.setEquivalence(learningType == LearningType.EQUIVALENT);
			lp.setClassToDescribe(new NamedClass(class2Describe.toString()));
			lp.setCheckConsistency(false);
			lp.init();
		} catch (ComponentInitException e) {
			e.printStackTrace();
		} 
		System.out.println("...done in " + (System.currentTimeMillis()-startTime) + "ms.");
	}

	public void initLearningAlgorithm() {
		System.out.println("Initializing learning algorithm...");
		long startTime = System.currentTimeMillis();
		la = new CELOE(lp, reasoner);
		
		RhoDRDown op = new RhoDRDown();
		op.setReasoner(reasoner);
		op.setUseNegation(useNegation);
		op.setUseHasValueConstructor(useHasValue);
		op.setUseAllConstructor(useUniversalQuantifier);
		op.setUseExistsConstructor(useExistentialQuantifier);
		op.setUseCardinalityRestrictions(useCardinality);
		op.setCardinalityLimit(cardinalityLimit);
		try {
			op.init();
		} catch (ComponentInitException e1) {
			e1.printStackTrace();
		}
		la.setOperator(op);
		
		la.setMaxExecutionTimeInSeconds(maxExecutionTimeInSeconds);
		la.setNoisePercentage(noisePercentage);
		la.setMaxNrOfResults(maxNrOfResults);

		try {
			la.init();
		} catch (ComponentInitException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		System.out.println("...done in " + (System.currentTimeMillis()-startTime) + "ms.");
		
	}

	public boolean learnAsynchronously() {
		if (learningInProgress) {
			return false;
		}
//		initLearningProblem();
//		initLearningAlgorithm();
		learningInProgress = true;

		Thread currentLearningThread = new Thread(new LearningRunner(), "Learning Thread");
		currentLearningThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread thread, Throwable throwable) {

			}
		});
		currentLearningThread.start();
		return true;
	}
	
	public boolean startLearning() {
		if (learningInProgress) {
			return false;
		}
//		initLearningProblem();
//		initLearningAlgorithm();
		learningInProgress = true;

		try {
			la.start();
		} finally {
			learningInProgress = false;
		}
		return true;
	}

	public boolean stopLearning() {
		if (!learningInProgress) {
			return false;
		}
		la.stop();
		learningInProgress = false;

		return true;
	}
	
	public boolean isLearning(){
		return la.isRunning();
	}
	
	public boolean isPreparing() {
		return preparing;
	}

	@SuppressWarnings("unchecked")
	public synchronized List<EvaluatedDescriptionClass> getCurrentlyLearnedDescriptions() {
		if (la != null) {
			result = Collections.unmodifiableList((List<EvaluatedDescriptionClass>) la
					.getCurrentlyBestEvaluatedDescriptions(maxNrOfResults, threshold, true));
		} else {
			result = Collections.emptyList();
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized List<EvaluatedDescriptionClass> getCurrentlyLearnedDescriptions(NamedClass class2Describe, int maxNrOfResults, double treshold) {
		if (la != null) {
			result = Collections.unmodifiableList((List<EvaluatedDescriptionClass>) la
					.getCurrentlyBestEvaluatedDescriptions(maxNrOfResults, threshold, true));
		} else {
			result = Collections.emptyList();
		}
		return result;
	}
	
	
	public void setClass2Describe(NamedClass nc) {
		class2Describe = nc;
	}

	public NamedClass getClass2Describe() {
		return class2Describe;
	}

	public void setMaxExecutionTimeInSeconds(int maxExecutionTimeInSeconds) {
		this.maxExecutionTimeInSeconds = maxExecutionTimeInSeconds;
	}
	
	public int getMaxExecutionTimeInSeconds() {
		return maxExecutionTimeInSeconds;
	}

	public void setNoisePercentage(double noisePercentage) {
		this.noisePercentage = noisePercentage;
	}

	public void setMaxNrOfResults(int maxNrOfResults) {
		this.maxNrOfResults = maxNrOfResults;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	
	public void setMinInstanceCount(int minInstanceCount){
		this.minInstanceCount = minInstanceCount;
	}
	
	public int getMinInstanceCount(){
		return minInstanceCount;
	}
	
	public void setUseExistentialQuantifier(boolean useExistentialQuantifier) {
		this.useExistentialQuantifier = useExistentialQuantifier;
	}

	public void setUseUniversalQuantifier(boolean useUniversalQuantifier) {
		this.useUniversalQuantifier = useUniversalQuantifier;
	}

	public void setUseNegation(boolean useNegation) {
		this.useNegation = useNegation;
	}

	public void setUseHasValue(boolean useHasValue) {
		this.useHasValue = useHasValue;
	}

	public void setUseCardinality(boolean useCardinality) {
		this.useCardinality = useCardinality;
	}

	public void setCardinalityLimit(int cardinalityLimit) {
		this.cardinalityLimit = cardinalityLimit;
	}

	public boolean isRunning(){
		return la.isRunning();
	}

	private class LearningRunner implements Runnable {

		@Override
		public void run() {
			try {
				la.start();
			} finally {
				learningInProgress = false;
			}
		}
	}
	
	public Set<Individual> getFalsePositives(int index){
		return result.get(index).getAdditionalInstances();
	}
	
	public Set<Individual> getFalseNegatives(int index){
		return result.get(index).getNotCoveredInstances();
	}
	
	/**
	 * Retrieves description parts that might cause inconsistency - for negative examples only.
	 * @param ind
	 * @param desc
	 */
	public Set<Description> getNegCriticalDescriptions(Individual ind, Description desc){
		
		Set<Description> criticals = new HashSet<Description>();
		List<Description> children = desc.getChildren();
		
		if(reasoner.hasType(desc, ind)){
			
			if(children.size() >= 2){
				
				if(desc instanceof Intersection){
					for(Description d: children){
						criticals.addAll(getNegCriticalDescriptions(ind, d));
					}
				} else if(desc instanceof Union){
					for(Description d: children){
						if(reasoner.hasType(d, ind)){
							criticals.addAll(getNegCriticalDescriptions(ind, d));
						}
					}
				}
			} else{
				criticals.add(desc);
			}
		}
		
		return criticals;
	}
	
}
