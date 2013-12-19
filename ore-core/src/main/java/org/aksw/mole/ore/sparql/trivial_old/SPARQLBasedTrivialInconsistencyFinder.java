package org.aksw.mole.ore.sparql.trivial_old;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.aksw.mole.ore.sparql.InconsistencyFinder;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.OWLAxiom;

public class SPARQLBasedTrivialInconsistencyFinder extends AbstractTrivialInconsistencyFinder {
	
	/**
	 * The different basic types of inconsistency reasons. Note that inverse functionality can only lead to inconsistency
	 * if the Unique Name Assumption holds.
	 * @author Lorenz Buehmann
	 *
	 */
	enum InconsistencyType {
		FUNCTIONALITY, IRREFLEXIVITY, ASYMMETRY, INVERSE_FUNCTIONALITY, DISJOINTNESS
	}
	
	private List<AbstractTrivialInconsistencyFinder> incFinders = new ArrayList<AbstractTrivialInconsistencyFinder>();
	
	private boolean completed = false;
	
	public SPARQLBasedTrivialInconsistencyFinder(SparqlEndpointKS ks) {
		this(ks, InconsistencyType.values());
	}
	
	public SPARQLBasedTrivialInconsistencyFinder(SparqlEndpointKS ks, InconsistencyType... inconsistencyTypes) {
		super(ks);
		setInconsistencyTypes(inconsistencyTypes);
	}
	
	public void setInconsistencyTypes(InconsistencyType... inconsistencyTypes){
		incFinders.clear();
		for (InconsistencyType inconsistencyType : inconsistencyTypes) {
			switch (inconsistencyType) {
			case DISJOINTNESS:incFinders.add(new DisjointnessBasedInconsistencyFinder(ks, getExplanations()));break;
			case FUNCTIONALITY:incFinders.add(new FunctionalityBasedInconsistencyFinder(ks, getExplanations()));break;
			case INVERSE_FUNCTIONALITY:incFinders.add(new InverseFunctionalityBasedInconsistencyFinder(ks, getExplanations()));break;
			case IRREFLEXIVITY:incFinders.add(new IrreflexivityBasedInconsistencyFinder(ks, getExplanations()));break;
			case ASYMMETRY:incFinders.add(new AsymmetryBasedInconsistencyFinder(ks, getExplanations()));break;
			default:
				break;
			}
		}
		incFinders.add(new PropertyRestrictionBasedInconsistencyFinder(ks));
	}
	
//	/**
//	 * Return all found explanations for inconsistency.
//	 */
//	public Set<Explanation<OWLAxiom>> getExplanations() {
//		Set<Explanation<OWLAxiom>> explanations = new HashSet<>();
//		for(AbstractTrivialInconsistencyFinder checker : incFinders){
//			explanations.addAll(checker.getExplanations());
//		}
//		return explanations;
//	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.AbstractTrivialInconsistencyFinder#setApplyUniqueNameAssumption(boolean)
	 */
	@Override
	public void setApplyUniqueNameAssumption(boolean applyUniqueNameAssumption) {
		super.setApplyUniqueNameAssumption(applyUniqueNameAssumption);
		for (AbstractTrivialInconsistencyFinder incFinder : incFinders) {
			incFinder.setApplyUniqueNameAssumption(applyUniqueNameAssumption);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#setStopIfInconsistencyFound(boolean)
	 */
	@Override
	public void setStopIfInconsistencyFound(boolean stopIfInconsistencyFound) {
		super.setStopIfInconsistencyFound(stopIfInconsistencyFound);
		for (InconsistencyFinder incFinder : incFinders) {
			incFinder.setStopIfInconsistencyFound(stopIfInconsistencyFound);
		}
	}
	
	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		super.setAxiomsToIgnore(axiomsToIgnore);
		for (InconsistencyFinder finder : incFinders) {
			finder.setAxiomsToIgnore(axiomsToIgnore);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#addProgressMonitor(org.aksw.mole.ore.sparql.SPARQLBasedInconsistencyProgressMonitor)
	 */
	@Override
	public void addProgressMonitor(SPARQLBasedInconsistencyProgressMonitor mon) {
		super.addProgressMonitor(mon);
		for (AbstractTrivialInconsistencyFinder incFinder : incFinders) {
			incFinder.addProgressMonitor(mon);
		}
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.AbstractTrivialInconsistencyFinder#run()
	 */
	@Override
	public void run(boolean resume) {
		for(AbstractTrivialInconsistencyFinder checker : incFinders){
			//apply inverse functionality checker only if UNA is applied
			if(!(checker instanceof InverseFunctionalityBasedInconsistencyFinder) || isApplyUniqueNameAssumption()){
				try {
					checker.run(resume);
					fireNumberOfConflictsFound(checker.getExplanations().size());
					if(checker.terminationCriteriaSatisfied()){
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
		if(!terminationCriteriaSatisfied()){
			fireFinished();
			completed = true;
		}
	}
	
	/**
	 * @return the completed
	 */
	public boolean isCompleted() {
		return completed;
	}
	
	public static void main(String[] args) throws Exception {
		SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia(), "cache");
		SPARQLBasedTrivialInconsistencyFinder incFinder = new SPARQLBasedTrivialInconsistencyFinder(ks);
		incFinder.setStopIfInconsistencyFound(false);
		incFinder.setApplyUniqueNameAssumption(true);
		incFinder.addProgressMonitor(new ConsoleSPARQLBasedInconsistencyProgressMonitor());
		incFinder.run();
		System.out.println(incFinder.getExplanations().size());
		System.out.println(incFinder.getExplanations().iterator().next().getAxioms());
	}
}
