package org.aksw.mole.ore.sparql.trivial_old;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.sparql.InconsistencyFinder;
import org.aksw.mole.ore.sparql.TimeOutException;
import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.hp.hpl.jena.rdf.model.Literal;

public abstract class AbstractTrivialInconsistencyFinder extends AbstractSPARQLBasedAxiomGenerator implements InconsistencyFinder {
	
	protected String query;
	protected String filter = "";
	protected Map<String, Integer> filterToOffset = new HashMap<String, Integer>();
	protected OWLDataFactory dataFactory = new OWLDataFactoryImpl();
	protected boolean stopIfInconsistencyFound = true;
	protected Set<Explanation<OWLAxiom>> explanations = new HashSet<>();
//	protected Set<Explanation<OWLAxiom>> allExplanations = new HashSet<>();
	
	private List<SPARQLBasedInconsistencyProgressMonitor> progressMonitors = new ArrayList<SPARQLBasedInconsistencyProgressMonitor>();
	
	protected final OWLAxiom inconsistencyEntailment = dataFactory.getOWLSubClassOfAxiom(dataFactory.getOWLThing(), dataFactory.getOWLNothing());
	
	protected boolean stop = false;
	
	private boolean applyUniqueNameAssumption = false;
	
	public AbstractTrivialInconsistencyFinder(SparqlEndpointKS ks) {
		super(ks);
		filterToOffset.put("", 0);
	}
	
	public AbstractTrivialInconsistencyFinder(SparqlEndpointKS ks, Set<Explanation<OWLAxiom>> explanations) {
		super(ks);
		this.explanations = explanations;
		filterToOffset.put("", 0);
	}
	
	public void run(){
		run(false);
	}
	public abstract void run(boolean cont);
	
	public void stop(){
		stop = true;
	}
	
	protected boolean terminationCriteriaSatisfied(){
		return stop || isCancelled() || (stopIfInconsistencyFound && !explanations.isEmpty());
	}
	
	protected boolean isCancelled(){
		boolean cancelled = false;
		for (SPARQLBasedInconsistencyProgressMonitor mon : progressMonitors) {
			cancelled = cancelled || mon.isCancelled();
		}
		return cancelled;
	}

	@Override
	public Set<OWLAxiom> getInconsistentFragment() throws TimeOutException {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		return axioms;
	}
	
	protected OWLLiteral getOWLLiteral(Literal lit){
		OWLLiteral literal = null;
		if(lit.getDatatypeURI() != null){
			OWLDatatype datatype = dataFactory.getOWLDatatype(IRI.create(lit.getDatatypeURI()));
			literal = dataFactory.getOWLLiteral(lit.getLexicalForm(), datatype);
		} else {
			if(lit.getLanguage() != null){
				literal = dataFactory.getOWLLiteral(lit.getLexicalForm(), lit.getLanguage());
			} else {
				literal = dataFactory.getOWLLiteral(lit.getLexicalForm());
			}
		}
		return literal;
	}
	
	public Set<Explanation<OWLAxiom>> getExplanations(){
		return explanations;
	}
	
	public void addProgressMonitor(SPARQLBasedInconsistencyProgressMonitor mon) {
		progressMonitors.add(mon);
	}

	public void removeProgressMonitor(SPARQLBasedInconsistencyProgressMonitor mon) {
		progressMonitors.remove(mon);
	}
	
	protected void fireInconsistencyFound(Set<Explanation<OWLAxiom>> explanations) {
		for (SPARQLBasedInconsistencyProgressMonitor mon : progressMonitors) {
			mon.inconsistencyFound(explanations);
		}
	}
	
	protected void fireInfoMessage(String message) {
		for (SPARQLBasedInconsistencyProgressMonitor mon : progressMonitors) {
			mon.info(message);
		}
	}
	
	protected void fireTraceMessage(String message) {
		for (SPARQLBasedInconsistencyProgressMonitor mon : progressMonitors) {
			mon.trace(message);
		}
	}
	
	protected void fireProgressUpdate(int current, int total) {
		for (SPARQLBasedInconsistencyProgressMonitor mon : progressMonitors) {
			mon.updateProgress(current, total);
		}
	}
	
	protected void fireNumberOfConflictsFound(int nrOfConflictsFound) {
		for (SPARQLBasedInconsistencyProgressMonitor mon : progressMonitors) {
			mon.numberOfConflictsFound(nrOfConflictsFound);
		}
	}
	
	protected void fireFinished() {
		for (SPARQLBasedInconsistencyProgressMonitor mon : progressMonitors) {
			mon.finished();
		}
	}
	
	/**
	 * @param stopIfInconsistencyFound the stopIfInconsistencyFound to set
	 */
	public void setStopIfInconsistencyFound(boolean stopIfInconsistencyFound) {
		this.stopIfInconsistencyFound = stopIfInconsistencyFound;
	}

	@Override
	public void setMaximumRuntime(long duration, TimeUnit timeUnit) {
	}

	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
	}

	/**
	 * @return the applyUniqueNameAssumption
	 */
	public boolean isApplyUniqueNameAssumption() {
		return applyUniqueNameAssumption;
	}

	/**
	 * @param applyUniqueNameAssumption the applyUniqueNameAssumption to set
	 */
	public void setApplyUniqueNameAssumption(boolean applyUniqueNameAssumption) {
		this.applyUniqueNameAssumption = applyUniqueNameAssumption;
	}
}
