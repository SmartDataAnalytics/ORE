package org.aksw.mole.ore.sparql;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.sparql.generator.AxiomGenerator;
import org.semanticweb.owlapi.model.OWLAxiom;

public interface InconsistencyFinder extends AxiomGenerator{
	/**
	 * Returns an fragment of the knowledge base which is inconsistent, if
	 * exist.
	 * 
	 * @return
	 */
	Set<OWLAxiom> getInconsistentFragment() throws TimeOutException;
	
	/**
	 * Set the maximum runtime for searching for a inconsistent fragment.
	 * @param duration
	 * @param timeUnit
	 */
	void setMaximumRuntime(long duration, TimeUnit timeUnit);
	
	/**
	 * Set the axioms which are ignored during the consistency tests. This set of axioms makes it possible to
	 * find more inconsistency relvant fragments. 
	 * @param duration
	 * @param timeUnit
	 */
	void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore);
	
	/**
	 * The algorithm is supposed to stop as soon as an inconsistency was found.
	 */
	void setStopIfInconsistencyFound(boolean stopIfInconsistencyFound);
}
