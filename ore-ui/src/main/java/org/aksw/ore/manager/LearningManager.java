package org.aksw.ore.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.ore.cache.LearningResultsCache;
import org.aksw.ore.model.LearningSetting;
import org.apache.log4j.Logger;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
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
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class LearningManager {
	
	
	private static final Logger logger = Logger.getLogger(LearningManager.class.getName());
	
	public enum LearningType {
		EQUIVALENT, SUPER
	};

	private LearningType learningType = LearningType.EQUIVALENT;

	private ClassLearningProblem lp;
	private CELOE la;
	private PelletReasoner reasoner;

	private LearningSetting learningSetting;

	private boolean learningInProgress = false;
	
	private boolean prepared = false;
	private boolean preparing;
	
	public List<EvaluatedDescriptionClass> result;

	public LearningResultsCache learningCache = new LearningResultsCache();

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
			reasoner.realise();
			reasoner.dematerialise();
			logger.info("...done in " + (System.currentTimeMillis()-startTime) + "ms.");
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
	
	public void setClass2Describe(NamedClass nc) {
		learningSetting.setClassToDescribe(nc);
	}

	public NamedClass getClass2Describe() {
		return learningSetting.getClassToDescribe();
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
	
	public static void main(String[] args) throws Exception {
		
		String ontologyURL = "http://www.cs.ox.ac.uk/isg/ontologies/UID/00082.owl";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = man.getOWLDataFactory();
		OWLOntology ontology = man.loadOntology(IRI.create(ontologyURL));
		OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		PelletReasoner pelletReasoner = new PelletReasoner((com.clarkparsia.pellet.owlapiv3.PelletReasoner)reasoner);
		pelletReasoner.init();
		LearningManager learningManager = new LearningManager(pelletReasoner);
		learningManager.setClass2Describe(new NamedClass("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#DeprecationReason"));
		learningManager.prepareLearning();
		learningManager.startLearning();
	}
	
}
