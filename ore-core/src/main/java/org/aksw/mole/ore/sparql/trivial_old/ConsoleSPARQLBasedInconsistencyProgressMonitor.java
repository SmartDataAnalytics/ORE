/**
 * 
 */
package org.aksw.mole.ore.sparql.trivial_old;

import java.util.Set;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Lorenz Buehmann
 *
 */
public class ConsoleSPARQLBasedInconsistencyProgressMonitor implements SPARQLBasedInconsistencyProgressMonitor {

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#inconsistencyFound()
	 */
	@Override
	public void inconsistencyFound() {
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#inconsistencyFound(java.util.Set)
	 */
	@Override
	public void inconsistencyFound(Set<Explanation<OWLAxiom>> explanations) {
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#info(java.lang.String)
	 */
	@Override
	public void info(String message) {
		System.out.println(message);
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#trace(java.lang.String)
	 */
	@Override
	public void trace(String message) {
		System.out.println(message);
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#updateProgress(int, int)
	 */
	@Override
	public void updateProgress(int current, int total) {
		System.out.println(current + "/" + total);
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#numberOfConflictsFound(int)
	 */
	@Override
	public void numberOfConflictsFound(int nrOfConflictsFound) {
		System.out.println("Number of conflicts: " + nrOfConflictsFound);
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#finished()
	 */
	@Override
	public void finished() {
		System.out.println("Finished");
	}

}
