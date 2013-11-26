package org.aksw.mole.ore.sparql.trivial_old;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.mole.ore.sparql.InconsistencyFinder;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

public class SPARQLBasedTrivialInconsistencyFinder extends AbstractTrivialInconsistencyFinder {
	
	private List<AbstractTrivialInconsistencyFinder> finders = new ArrayList<AbstractTrivialInconsistencyFinder>();
	
	public SPARQLBasedTrivialInconsistencyFinder(SparqlEndpointKS ks) {
		super(ks);
		finders.add(new FunctionalityBasedInconsistencyFinder(ks));
//		finders.add(new InverseFunctionalityBasedInconsistencyFinder(ks));
		finders.add(new AsymmetryBasedInconsistencyFinder(ks));
		finders.add(new IrreflexivityBasedInconsistencyFinder(ks));
		finders.add(new DisjointnessBasedInconsistencyFinder(ks));
	}
	
	public Set<Explanation<OWLAxiom>> getExplanations() {
		Set<Explanation<OWLAxiom>> explanations = new HashSet<>();
		for(AbstractTrivialInconsistencyFinder checker : finders){
			explanations.addAll(checker.getExplanations());
		}
		return explanations;
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

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.AbstractTrivialInconsistencyFinder#run()
	 */
	@Override
	public void run(boolean resume) {
		for(AbstractTrivialInconsistencyFinder checker : finders){
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
	
	public static void main(String[] args) throws Exception {
		SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia(), "cache");
		SPARQLBasedTrivialInconsistencyFinder incFinder = new SPARQLBasedTrivialInconsistencyFinder(ks);
		incFinder.setStopIfInconsistencyFound(false);
		incFinder.addProgressMonitor(new SPARQLBasedInconsistencyProgressMonitor() {
			
			@Override
			public void trace(String message) {
				System.out.println(message);
			}
			
			@Override
			public boolean isCancelled() {
				return false;
			}
			
			@Override
			public void info(String message) {
				System.out.println(message);
			}
			
			@Override
			public void inconsistencyFound(Set<Explanation<OWLAxiom>> explanations) {
			}
			
			@Override
			public void inconsistencyFound() {
			}

			@Override
			public void updateProgress(int current, int total) {
				System.out.println(current + "/" + total);
			}

			@Override
			public void numberOfConflictsFound(int nrOfConflictsFound) {
				System.out.println("Conflicts found: " + nrOfConflictsFound);
			}
		});
		incFinder.run();
		System.out.println(incFinder.getExplanations().size());
	}
}
