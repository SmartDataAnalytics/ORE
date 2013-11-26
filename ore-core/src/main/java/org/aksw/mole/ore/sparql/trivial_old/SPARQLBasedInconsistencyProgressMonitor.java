package org.aksw.mole.ore.sparql.trivial_old;

import java.util.Set;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

public interface SPARQLBasedInconsistencyProgressMonitor{
		/**
		 * This method is called the first time an inconsistency is found, i.e. the retrieved fragment is inconsistent. 
		 */
		void inconsistencyFound();
		boolean isCancelled();
		void inconsistencyFound(Set<Explanation<OWLAxiom>> explanations);
		
		void info(String message);
		void trace(String message);
		void updateProgress(int current, int total);
		void numberOfConflictsFound(int nrOfConflictsFound);
	}