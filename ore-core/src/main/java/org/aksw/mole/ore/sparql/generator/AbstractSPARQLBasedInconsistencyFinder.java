/**
 * 
 */
package org.aksw.mole.ore.sparql.generator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.sparql.AxiomGenerationTracker;
import org.aksw.mole.ore.sparql.InconsistencyFinder;
import org.aksw.mole.ore.sparql.LinkedDataDereferencer;
import org.aksw.mole.ore.sparql.TimeOutException;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.google.common.base.Stopwatch;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

/**
 * @author Lorenz Buehmann
 *
 */
public abstract class AbstractSPARQLBasedInconsistencyFinder implements InconsistencyFinder{
	
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
	}
	
	Stopwatch stopWatch = new Stopwatch();
	Monitor consistencyMonitor = MonitorFactory.getTimeMonitor("consistency checks");
	

	private List<SPARQLBasedInconsistencyProgressMonitor> progressMonitors = new ArrayList<SPARQLBasedInconsistencyProgressMonitor>();

	public abstract Set<OWLAxiom> getInconsistentFragment() throws TimeOutException;

	protected volatile boolean stop = false;
	private long maxRuntimeInMilliseconds = TimeUnit.MINUTES.toMillis(1);
	protected boolean stopIfInconsistencyFound = true;
	protected boolean useLinkedData = true;
	protected Set<String> linkedDataNamespaces = new HashSet<String>();
	protected LinkedDataDereferencer linkedDataDereferencer = new LinkedDataDereferencer();
	
	protected OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	protected OWLOntology fragment;
	private Set<OWLAxiom> axiomsToIgnore = new HashSet<OWLAxiom>();
	protected AxiomGenerationTracker tracker;
	
	protected SparqlEndpointKS ks;
	protected OWLReasonerFactory reasonerFactory;
	protected OWLReasoner reasoner;

	/**
	 * @return the reasoner
	 */
	public OWLReasoner getReasoner() {
		return reasoner;
	}

	public void reset() {
		//create empty ontology to which retrieved axioms are added
		try {
			fragment = manager.createOntology();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
		//create the reasoner which is used to check for consistency
		reasoner = reasonerFactory.createNonBufferingReasoner(fragment);
	}

	@Override
	public void setMaximumRuntime(long duration, TimeUnit timeUnit) {
		this.maxRuntimeInMilliseconds = timeUnit.toMillis(duration);
	}

	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		this.axiomsToIgnore = axiomsToIgnore;
		manager.removeAxioms(fragment, axiomsToIgnore);
	}

	public void setLinkedDataNamespaces(Set<String> linkedDataNamespaces) {
		this.linkedDataNamespaces = linkedDataNamespaces;
	}

	public void setUseLinkedData(boolean useLinkedData) {
		this.useLinkedData = useLinkedData;
	}

	public void setStopIfInconsistencyFound(boolean stopIfInconsistencyFound) {
		this.stopIfInconsistencyFound = stopIfInconsistencyFound;
	}

	public void stop() {
		this.stop = true;
	}

	public void setAxiomGenerationTracker(AxiomGenerationTracker tracker) {
		this.tracker = tracker;
	}

	protected void addAxioms(AxiomGenerator generator, Set<OWLAxiom> axioms) {
		if(tracker != null){
			tracker.track(generator, axioms);
		}
		Set<OWLAxiom> axiomsWithoutIgnored = new HashSet<OWLAxiom>(axioms);
		axiomsWithoutIgnored.removeAll(axiomsToIgnore);
		manager.addAxioms(fragment, axiomsWithoutIgnored);
		fireFragmentExpanded();
	}

	protected boolean timeExpired() {
		boolean timeOut = stopWatch.elapsed(TimeUnit.MILLISECONDS) >= maxRuntimeInMilliseconds;
		return timeOut;
	}

	protected boolean terminationCriteriaSatisfied() throws TimeOutException {
		if(stop || isCancelled()){
			return true;
		} else {
			//check for timeout
			boolean timeOut = timeExpired();
			if(timeOut){
				consistencyMonitor.start();
				boolean consistent = isConsistent();
				consistencyMonitor.stop();
				if(!consistent){
					fireInconsistencyFound();
					return true;
				}
				throw new TimeOutException();
			} else if(stopIfInconsistencyFound){
				//check for consistency
				consistencyMonitor.start();
				boolean consistent = isConsistent();
				consistencyMonitor.stop();
				if(!consistent){
					fireInconsistencyFound();
				}
				return !consistent;
			}
		}
		return false;
	}
	
	protected abstract boolean isConsistent();

	protected boolean isCancelled() {
		boolean cancelled = false;
		for (SPARQLBasedInconsistencyProgressMonitor mon : progressMonitors) {
			cancelled = cancelled | mon.isCancelled();
		}
		return cancelled;
	}

	public void addProgressMonitor(SPARQLBasedInconsistencyProgressMonitor mon) {
		progressMonitors.add(mon);
	}

	public void removeProgressMonitor(SPARQLBasedInconsistencyProgressMonitor mon) {
		progressMonitors.remove(mon);
	}

	protected void fireInconsistencyFound() {
		for (SPARQLBasedInconsistencyProgressMonitor mon : progressMonitors) {
			mon.inconsistencyFound();
		}
	}

	protected void fireFragmentExpanded() {
		for (SPARQLBasedInconsistencyProgressMonitor mon : progressMonitors) {
			mon.fragmentExpanded();
		}
	}

	@Override
	public int compareTo(AxiomGenerator o) {
		return getClass().getName().compareTo(o.getClass().getName());
	}

}