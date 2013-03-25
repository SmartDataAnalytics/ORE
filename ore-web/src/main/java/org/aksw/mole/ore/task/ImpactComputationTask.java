package org.aksw.mole.ore.task;

import java.util.ArrayList;
import java.util.Set;

import org.aksw.mole.ore.UserSession;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import com.vaadin.Application;

public class ImpactComputationTask extends BackgroundTask{
	
	private Set<OWLOntologyChange> impact;
	
	public ImpactComputationTask(Set<OWLOntologyChange> repairPlan, Application app, String taskName, ProgressListener... listeners) {
		super(app, taskName, listeners);
	}

	@Override
	public void runInBackground() {
		impact = UserSession.getImpactManager().getImpact(new ArrayList<OWLOntologyChange>(UserSession.getRepairManager().getRepairPlan()));
	}
	
	@Override
	public void updateUIBefore() {
		
	}

	@Override
	public void updateUIAfter() {
		
	}
	
	@Override
	protected void cancel() throws IllegalStateException {
		super.cancel();
//		UserSession.getImpactManager().cancel();
	}
	

}
