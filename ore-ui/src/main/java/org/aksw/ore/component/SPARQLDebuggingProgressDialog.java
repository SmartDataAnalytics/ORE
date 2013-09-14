/**
 * 
 */
package org.aksw.ore.component;

import java.util.ArrayList;

import org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder.SPARQLBasedInconsistencyProgressMonitor;
import org.aksw.ore.ORESession;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author Lorenz Buehmann
 *
 */
public class SPARQLDebuggingProgressDialog extends ProgressDialog implements SPARQLBasedInconsistencyProgressMonitor{

	private OWLOntology fragment;
	
	public SPARQLDebuggingProgressDialog() {
		super();
		setCaption("Searching for inconsistency...");
		fragment = ORESession.getSparqlBasedInconsistencyFinder().getReasoner().getRootOntology();
		setMessage("</br></br></br></br>");
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.SPARQLBasedInconsistencyFinder.SPARQLBasedInconsistencyProgressMonitor#fragmentExpanded()
	 */
	@Override
	public void fragmentExpanded() {
		getUI().access(new Runnable() {
			
			@Override
			public void run() {
				StringBuilder sb = new StringBuilder();
				sb.append("<b>Fragment size:</b> " + fragment.getLogicalAxiomCount() + " logical axioms</br>");
				sb.append("<b>TBox size:</b> " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
						new ArrayList<AxiomType<?>>(AxiomType.TBoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms</br>");
				sb.append("<b>RBox size:</b> " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
						new ArrayList<AxiomType<?>>(AxiomType.RBoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms</br>");
				sb.append("<b>ABox size:</b> " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
						new ArrayList<AxiomType<?>>(AxiomType.ABoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms</br>");
				
				setMessage(sb.toString());
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.SPARQLBasedInconsistencyFinder.SPARQLBasedInconsistencyProgressMonitor#inconsistencyFound()
	 */
	@Override
	public void inconsistencyFound() {
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.SPARQLBasedInconsistencyFinder.SPARQLBasedInconsistencyProgressMonitor#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
}
