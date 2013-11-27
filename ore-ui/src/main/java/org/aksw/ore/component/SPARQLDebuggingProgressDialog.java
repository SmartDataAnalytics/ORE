/**
 * 
 */
package org.aksw.ore.component;

import java.util.Set;

import org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

/**
 * @author Lorenz Buehmann
 *
 */
public class SPARQLDebuggingProgressDialog extends ProgressDialog implements SPARQLBasedInconsistencyProgressMonitor{

	private OWLOntology fragment;
	private Label nrOfConflictsLabel;
	
	public SPARQLDebuggingProgressDialog() {
		super("Searching for inconsistency...", true);
//		fragment = ORESession.getSparqlBasedInconsistencyFinder().getReasoner().getRootOntology();
		setMessage("</br></br></br></br>");
		nrOfConflictsLabel = new Label("Number of conflicts:");
		content.addComponent(nrOfConflictsLabel);
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.SPARQLBasedInconsistencyFinder.SPARQLBasedInconsistencyProgressMonitor#fragmentExpanded()
	 */
//	@Override
//	public void fragmentExpanded() {
//		getUI().access(new Runnable() {
//			
//			@Override
//			public void run() {
//				StringBuilder sb = new StringBuilder();
//				sb.append("<b>Fragment size:</b> " + fragment.getLogicalAxiomCount() + " logical axioms</br>");
//				sb.append("<b>TBox size:</b> " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
//						new ArrayList<AxiomType<?>>(AxiomType.TBoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms</br>");
//				sb.append("<b>RBox size:</b> " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
//						new ArrayList<AxiomType<?>>(AxiomType.RBoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms</br>");
//				sb.append("<b>ABox size:</b> " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
//						new ArrayList<AxiomType<?>>(AxiomType.ABoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms</br>");
//				
//				setMessage(sb.toString());
//			}
//		});
//	}

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

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.SPARQLBasedInconsistencyProgressMonitor#inconsistencyFound(java.util.Set)
	 */
	@Override
	public void inconsistencyFound(Set<Explanation<OWLAxiom>> explanations) {
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#info(java.lang.String)
	 */
	@Override
	public void info(String message) {
		setMessage(message);
		setTraceMessage("");
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#trace(java.lang.String)
	 */
	@Override
	public void trace(String message) {
		setTraceMessage(message);
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#updateProgress(int, int)
	 */
	@Override
	public void updateProgress(int current, int total) {
		setProgress(current, total);
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#numberOfConflictsFound(int)
	 */
	@Override
	public void numberOfConflictsFound(final int nrOfConflictsFound) {
		UI.getCurrent().access(new Runnable() {
			
			@Override
			public void run() {
				nrOfConflictsLabel.setValue("Number of conflicts:" + nrOfConflictsFound);
			}
		});
		
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedInconsistencyProgressMonitor#finished()
	 */
	@Override
	public void finished() {
		
	}

	
}
