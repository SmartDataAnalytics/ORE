package org.aksw.mole.ore.impact;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveAxiom;

public abstract class AbstractImpactChecker implements ImpactChecker{
	
	private volatile boolean canceled = false;

	private volatile boolean finished = false;
	
	private Set<OWLOntologyChange> impact;
	
	protected List<OWLOntologyChange> getInverseChanges(List<OWLOntologyChange> changes){
		List<OWLOntologyChange> inverseChanges = new ArrayList<OWLOntologyChange>(changes.size());
		for(OWLOntologyChange change : changes){
			if(change instanceof RemoveAxiom){
				inverseChanges.add(new AddAxiom(change.getOntology(), change.getAxiom()));
			} else {
				inverseChanges.add(new RemoveAxiom(change.getOntology(), change.getAxiom()));
			}
		}
		return inverseChanges;
	}
	
	@Override
	public Set<OWLOntologyChange> getImpact(List<OWLOntologyChange> changes) {
		finished = false;
		impact = computeImpact(changes);
		finished = true;
		return impact;
	}
	
	public abstract Set<OWLOntologyChange> computeImpact(List<OWLOntologyChange> changes);
	
	public void cancel(){
		canceled = true;
	}
	
	/**
	 * Returns true if cancellation of this VaadinWorker has been requested.
	 * This must not mean that the work has effectively stopped already. Use
	 * <code>isFinished()</code> for checking that.
	 */
	public final boolean isCanceled() {
		return canceled;
	}

	/**
	 * Returns true if the work has effectively stopped, either due to a cancel-
	 * request by the user or because there is no more work to do.
	 */
	public final boolean isFinished() {
		return finished;
	}
	
}
