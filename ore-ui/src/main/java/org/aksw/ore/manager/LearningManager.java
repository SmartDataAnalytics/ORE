package org.aksw.ore.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.ore.cache.LearningResultsCache;
import org.aksw.ore.model.LearningSetting;
import org.apache.log4j.Logger;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.learningproblems.ClassLearningProblem;
import org.dllearner.learningproblems.EvaluatedDescriptionClass;
import org.dllearner.reasoning.FastInstanceChecker;
import org.dllearner.refinementoperators.RhoDRDown;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class LearningManager {
	
	
	private static final Logger logger = Logger.getLogger(LearningManager.class.getName());
	
	public enum LearningType {
		EQUIVALENT, SUPER
	};

	private LearningType learningType = LearningType.EQUIVALENT;

	private ClassLearningProblem lp;
	private CELOE la;
	private FastInstanceChecker reasoner;

	private LearningSetting learningSetting;

	private boolean learningInProgress = false;
	
	private boolean prepared = false;
	private boolean preparing;
	
	public List<EvaluatedDescriptionClass> result;

	public LearningResultsCache learningCache = new LearningResultsCache();
	
	private OWLDataFactory df = new OWLDataFactoryImpl(false, false);

	public LearningManager(FastInstanceChecker reasoner) {
		this.reasoner = reasoner;
	}

	public void setLearningType(LearningType learningType) {
		this.learningType = learningType;
	}

	public LearningType getLearningType() {
		return learningType;
	}
	
	public FastInstanceChecker getReasoner(){
		return reasoner;
	}
	
	/**
	 * @param learningSetting the learningSetting to set
	 */
	public void setLearningSetting(LearningSetting learningSetting) {
		this.learningSetting = learningSetting;
	}
	
	public void prepareLearning(){
		preparing = true;
		//initialization of the reasoner has to be done only once we have a new knowledge base
		//TODO how to handle changes of the ontology?
		if(!prepared){
			logger.info("Initializing internal reasoner...");
			long startTime = System.currentTimeMillis();
			prepared = true;
//			reasoner.realise();
//			reasoner.dematerialise();
			logger.info("...done in " + (System.currentTimeMillis()-startTime) + "ms.");
		}
		initLearningProblem();
		initLearningAlgorithm();
		preparing = false;
	}
	
	public List<OWLClass> getClasses(){
//		reasoner.realise();
//		reasoner.dematerialise();
		List<OWLClass> classes = new ArrayList<OWLClass>();
		for(OWLClass nc : reasoner.getAtomicConceptsList()){
			if(reasoner.getIndividuals(nc).size() >= 2){
				classes.add(nc);
			}
		}
		return classes;
	}
	
	/**
	 * Returns all classes that are direct subclasses of owl:Thing.
	 * @return
	 */
	public SortedSet<OWLClass> getTopLevelClasses(){
		SortedSet<OWLClassExpression> subClasses = reasoner.getSubClasses(df.getOWLThing());
		SortedSet<OWLClass> classes = new TreeSet<OWLClass>();
		for (OWLClassExpression subClass : subClasses) {
			classes.add(subClass.asOWLClass());
		}
		return classes;
	}
	
	/**
	 * Returns all direct subclasses.
	 * @return
	 */
	public SortedSet<OWLClass> getDirectSubClasses(OWLClass cls){
		SortedSet<OWLClassExpression> subClasses = reasoner.getSubClasses(cls);
		SortedSet<OWLClass> classes = new TreeSet<OWLClass>();
		for (OWLClassExpression subClass : subClasses) {
			classes.add(subClass.asOWLClass());
		}
		return classes;
	}

	public void initLearningProblem() {
		logger.info("Initializing learning problem...");
		long startTime = System.currentTimeMillis();
		lp = new ClassLearningProblem(reasoner);
		try {
			lp.setEquivalence(learningType == LearningType.EQUIVALENT);
			lp.setClassToDescribe(learningSetting.getClassToDescribe());
			lp.setCheckConsistency(false);
			lp.init();
		} catch (ComponentInitException e) {
			e.printStackTrace();
		} 
		logger.info("...done in " + (System.currentTimeMillis()-startTime) + "ms.");
	}

	public void initLearningAlgorithm() {
		logger.info("Initializing learning algorithm...");
		long startTime = System.currentTimeMillis();
		la = new CELOE(lp, reasoner);
		
		RhoDRDown op = new RhoDRDown();
		op.setReasoner(reasoner);
		op.setUseNegation(learningSetting.isUseNegation());
		op.setUseHasValueConstructor(learningSetting.isUseHasValue());
		op.setUseAllConstructor(learningSetting.isUseUniversalQuantifier());
		op.setUseExistsConstructor(learningSetting.isUseExistentialQuantifier());
		op.setUseCardinalityRestrictions(learningSetting.isUseCardinality());
		op.setCardinalityLimit(learningSetting.getCardinalityLimit());
		try {
			op.init();
		} catch (ComponentInitException e1) {
			e1.printStackTrace();
		}
		la.setOperator(op);
		
		la.setMaxExecutionTimeInSeconds(learningSetting.getMaxExecutionTimeInSeconds());
		la.setNoisePercentage(learningSetting.getNoise());
		la.setMaxNrOfResults(learningSetting.getMaxNrOfResults());

		try {
			la.init();
		} catch (ComponentInitException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		logger.info("...done in " + (System.currentTimeMillis()-startTime) + "ms.");
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
			learningCache.putEvaluatedDescriptions(learningSetting, getCurrentlyLearnedDescriptions());
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
					.getCurrentlyBestEvaluatedDescriptions(learningSetting.getMaxNrOfResults(), learningSetting.getThreshold(), true));
		} else {
			result = Collections.emptyList();
		}
		return result;
	}
	
	public synchronized List<EvaluatedDescriptionClass> getCurrentlyLearnedDescriptionsCached() {
		return learningCache.getEvaluatedDescriptions(learningSetting);
	}
	
	public synchronized EvaluatedDescription getBestLearnedDescriptions() {
		if (la != null) {
			return la.getCurrentlyBestEvaluatedDescription();
		} 
		return null;
	}
	
	public void setClass2Describe(OWLClass nc) {
		learningSetting.setClassToDescribe(nc);
	}

	public OWLClass getClass2Describe() {
		return learningSetting.getClassToDescribe();
	}
	
	public Set<OWLIndividual> getFalsePositives(int index){
		return result.get(index).getAdditionalInstances();
	}
	
	public Set<OWLIndividual> getFalseNegatives(int index){
		return result.get(index).getNotCoveredInstances();
	}
	
	/**
	 * Retrieves description parts that might cause inconsistency - for negative examples only.
	 * @param ind
	 * @param desc
	 */
	public Set<OWLClassExpression> getNegCriticalDescriptions(OWLIndividual ind, OWLClassExpression desc){
		
		Set<OWLClassExpression> criticals = new HashSet<OWLClassExpression>();
		
		if(reasoner.hasType(desc, ind)){
			
			if(desc instanceof OWLNaryBooleanClassExpression){
				if(desc instanceof OWLObjectIntersectionOf){
					for(OWLClassExpression d: ((OWLObjectIntersectionOf) desc).getOperands()){
						criticals.addAll(getNegCriticalDescriptions(ind, d));
					}
				} else if(desc instanceof OWLObjectUnionOf){
					for(OWLClassExpression d: ((OWLObjectUnionOf) desc).getOperands()){
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
