/**
 * 
 */
package org.aksw.ore.manager;

import java.util.Set;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationProgressMonitor;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Lorenz Buehmann
 *
 */
public interface ExplanationProgressMonitorExtended<E> extends ExplanationProgressMonitor<E>{
	void allExplanationsFound(Set<Explanation<OWLAxiom>> allExplanations);
}
