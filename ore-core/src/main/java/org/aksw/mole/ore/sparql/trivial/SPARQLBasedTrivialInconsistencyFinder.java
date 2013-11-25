package org.aksw.mole.ore.sparql.trivial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.mole.ore.sparql.InconsistencyFinder;
import org.aksw.mole.ore.sparql.SPARQLBasedInconsistencyProgressMonitor;
import org.aksw.mole.ore.sparql.TimeOutException;
import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class SPARQLBasedTrivialInconsistencyFinder extends AbstractSPARQLBasedInconsistencyFinder {
	
	private List<AbstractTrivialInconsistencyFinder> finders = new ArrayList<AbstractTrivialInconsistencyFinder>();
	
	public SPARQLBasedTrivialInconsistencyFinder(SparqlEndpointKS ks) {
		this.ks = ks;
//		finders.add(new FunctionalityBasedInconsistencyFinder(ks));
		finders.add(new OptimizedFunctionalityBasedInconsistencyFinder(ks));
//		finders.add(new InverseFunctionalityBasedInconsistencyFinder(ks));
		finders.add(new AsymmetryBasedInconsistencyFinder(ks));
		finders.add(new IrreflexivityBasedInconsistencyFinder(ks));
//		finders.add(new DisjointnessBasedInconsistencyFinder(ks));
		finders.add(new OptimizedDisjointnessBasedInconsistencyFinder(ks));
		
		reset();
	}
	
	@Override
	public Set<OWLAxiom> getInconsistentFragment() {
		for(AbstractTrivialInconsistencyFinder checker : finders){
			try {
				Set<OWLAxiom> inconsistentFragment = checker.getInconsistentFragment();
				addAxioms(checker, inconsistentFragment);
				if(tracker != null){
					tracker.track(checker, inconsistentFragment);
				}
				if(terminationCriteriaSatisfied()){
					fireInconsistencyFound(checker.getExplanations());
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return fragment.getAxioms();
	}
	
	public Set<Explanation<OWLAxiom>> getExplanations() {
		Set<Explanation<OWLAxiom>> explanations = new HashSet<>();
		for(AbstractTrivialInconsistencyFinder checker : finders){
			explanations.addAll(checker.getExplanations());
		}
		return explanations;
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#terminationCriteriaSatisfied()
	 */
	@Override
	protected boolean terminationCriteriaSatisfied() throws TimeOutException {
		if(stop || isCancelled()){
			return true;
		} else {
			//check for timeout
			boolean timeOut = timeExpired();
			if(timeOut){
				boolean consistent = fragment.getLogicalAxioms().isEmpty();
				if(!consistent){
					fireInconsistencyFound();
					return true;
				}
				throw new TimeOutException();
			} else if(stopIfInconsistencyFound){
				//check for consistency
				boolean consistent = fragment.getLogicalAxioms().isEmpty();
				if(!consistent){
					fireInconsistencyFound();
				}
				return !consistent;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#setStopIfInconsistencyFound(boolean)
	 */
	@Override
	public void setStopIfInconsistencyFound(boolean stopIfInconsistencyFound) {
		super.setStopIfInconsistencyFound(stopIfInconsistencyFound);
		for (InconsistencyFinder incFinder : finders) {
			incFinder.setStopIfInconsistencyFound(stopIfInconsistencyFound);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#isConsistent()
	 */
	@Override
	protected boolean isConsistent() {
		return fragment.getLogicalAxioms().isEmpty();
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#reset()
	 */
	@Override
	public void reset() {
		try {
			fragment = manager.createOntology();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		super.setAxiomsToIgnore(axiomsToIgnore);
		for (InconsistencyFinder finder : finders) {
			finder.setAxiomsToIgnore(axiomsToIgnore);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#addProgressMonitor(org.aksw.mole.ore.sparql.SPARQLBasedInconsistencyProgressMonitor)
	 */
	@Override
	public void addProgressMonitor(SPARQLBasedInconsistencyProgressMonitor mon) {
		super.addProgressMonitor(mon);
		for (AbstractTrivialInconsistencyFinder incFinder : finders) {
			incFinder.addProgressMonitor(mon);
		}
	}
}
