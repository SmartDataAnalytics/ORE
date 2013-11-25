package org.aksw.mole.ore.sparql;

import java.util.Set;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

public interface SPARQLBasedInconsistencyProgressMonitor{
		/**
		 * This method is called if the underlying is expanded by new axioms returned by axiom generators.
		 */
		void fragmentExpanded();
		/**
		 * This method is called the first time an inconsistency is found, i.e. the retrieved fragment is inconsistent. 
		 */
		void inconsistencyFound();
		boolean isCancelled();
		void inconsistencyFound(Set<Explanation<OWLAxiom>> explanations);
		
		void message(String message);
	}