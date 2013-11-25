package org.aksw.mole.ore.sparql.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.sparql.AxiomGenerationTracker;
import org.aksw.mole.ore.sparql.SPARQLBasedInconsistencyProgressMonitor;
import org.aksw.mole.ore.sparql.TimeOutException;
import org.aksw.mole.ore.sparql.trivial.SPARQLBasedTrivialInconsistencyFinder;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.mindswap.pellet.PelletOptions;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class SPARQLBasedInconsistencyFinder2 extends AbstractSPARQLBasedInconsistencyFinder {
	
	private static final Logger logger = LoggerFactory.getLogger(SPARQLBasedInconsistencyFinder2.class);
	
	private SPARQLBasedTrivialInconsistencyFinder trivialInconsistencyFinder;

	public SPARQLBasedInconsistencyFinder2(SparqlEndpointKS ks) {
		this(ks, PelletReasonerFactory.getInstance());
	}
	
	public SPARQLBasedInconsistencyFinder2(SparqlEndpointKS ks, OWLReasonerFactory reasonerFactory) {
		this.ks = ks;
		this.reasonerFactory = reasonerFactory;
		
		//create a helper which looks for trivial cases for inconsistency
		trivialInconsistencyFinder = new SPARQLBasedTrivialInconsistencyFinder(ks);
		reset();
	}
	
	public Set<Explanation<OWLAxiom>> getExplanationsForTrivialInconsistencies(){
		return trivialInconsistencyFinder.getExplanations();
	}
	
	@Override
	public Set<OWLAxiom> getInconsistentFragment() throws TimeOutException {
		stop = false;
		stopWatch.start();
		//firstly, check for trivial inconsistency cases
		logger.info("Looking for trivial inconsistency cases...");
		Set<OWLAxiom> trivialInconsistentFragment = trivialInconsistencyFinder.getInconsistentFragment();
		if(tracker != null){
			tracker.track(trivialInconsistencyFinder, trivialInconsistentFragment);
		}
		addAxioms(trivialInconsistencyFinder, trivialInconsistentFragment);
		if(terminationCriteriaSatisfied(true)){//!fragment.getLogicalAxioms().isEmpty()){
			logger.info("Found axioms leading to trivial inconsistency.");
			if(stopIfInconsistencyFound){
				logger.info("Early termination.");
			}
			stopWatch.stop();
			return fragment.getAxioms();
		}
		
		//check if there are other possible reasons for inconsistency anyway, i.e. disjoint classes, differentFrom, etc.
		
		stopWatch.stop();
		return fragment.getAxioms();
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#isConsistent()
	 */
	@Override
	protected boolean isConsistent() {
		return reasoner.isConsistent();
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#setAxiomsToIgnore(java.util.Set)
	 */
	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		super.setAxiomsToIgnore(axiomsToIgnore);
		trivialInconsistencyFinder.setAxiomsToIgnore(axiomsToIgnore);
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#addProgressMonitor(org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder.SPARQLBasedInconsistencyProgressMonitor)
	 */
	@Override
	public void addProgressMonitor(SPARQLBasedInconsistencyProgressMonitor mon) {
		super.addProgressMonitor(mon);
		trivialInconsistencyFinder.addProgressMonitor(mon);
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#removeProgressMonitor(org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder.SPARQLBasedInconsistencyProgressMonitor)
	 */
	@Override
	public void removeProgressMonitor(SPARQLBasedInconsistencyProgressMonitor mon) {
		super.removeProgressMonitor(mon);
		trivialInconsistencyFinder.removeProgressMonitor(mon);
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#reset()
	 */
	@Override
	public void reset() {
		super.reset();
		// create an helper which looks for trivial cases for inconsistency
		trivialInconsistencyFinder = new SPARQLBasedTrivialInconsistencyFinder(ks);
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#setStopIfInconsistencyFound(boolean)
	 */
	@Override
	public void setStopIfInconsistencyFound(boolean stopIfInconsistencyFound) {
		super.setStopIfInconsistencyFound(stopIfInconsistencyFound);
		trivialInconsistencyFinder.setStopIfInconsistencyFound(stopIfInconsistencyFound);
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#setAxiomGenerationTracker(org.aksw.mole.ore.sparql.AxiomGenerationTracker)
	 */
	@Override
	public void setAxiomGenerationTracker(AxiomGenerationTracker tracker) {
		super.setAxiomGenerationTracker(tracker);
		trivialInconsistencyFinder.setAxiomGenerationTracker(tracker);
	}
	
	public void showStatistics(){
		logger.info("Fragment size: " + fragment.getLogicalAxiomCount() + " logical axioms");
		logger.info("TBox size: " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
				new ArrayList<AxiomType<?>>(AxiomType.TBoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms");
		logger.info("RBox size: " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
				new ArrayList<AxiomType<?>>(AxiomType.RBoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms");
		logger.info("ABox size: " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
				new ArrayList<AxiomType<?>>(AxiomType.ABoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms");
	}
	
	public static void main(String[] args) throws Exception {
		PelletOptions.INVALID_LITERAL_AS_INCONSISTENCY = false;
		SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia(), "cache");
		SPARQLBasedInconsistencyFinder2 inconsistencyFinder = new SPARQLBasedInconsistencyFinder2(ks);
		inconsistencyFinder.setMaximumRuntime(0, TimeUnit.SECONDS);
		Set<OWLAxiom> inconsistentFragment = inconsistencyFinder.getInconsistentFragment();
//		System.out.println(inconsistentFragment);
		System.out.println(inconsistencyFinder.getExplanationsForTrivialInconsistencies().size());
//		inconsistencyFinder.getInconsistentFragment();
//		System.out.println(inconsistencyFinder.getExplanationsForTrivialInconsistencies().size());
//		inconsistencyFinder.getInconsistentFragment();
//		System.out.println(inconsistencyFinder.getExplanationsForTrivialInconsistencies().size());
//		inconsistencyFinder.getInconsistentFragment();
//		System.out.println(inconsistencyFinder.getExplanationsForTrivialInconsistencies().size());
//		inconsistencyFinder.getInconsistentFragment();
//		System.out.println(inconsistencyFinder.getExplanationsForTrivialInconsistencies().size());
//		inconsistencyFinder.getInconsistentFragment();
//		System.out.println(inconsistencyFinder.getExplanationsForTrivialInconsistencies().size());
	}
}
