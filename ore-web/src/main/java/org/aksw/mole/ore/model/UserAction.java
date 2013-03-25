package org.aksw.mole.ore.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveAxiom;

public class UserAction implements Comparable<UserAction>{
	
	public enum Task{
		DEBUGGING, ENRICHMENT, PATTERN
	}
	
	private long timestamp;
	private List<OWLOntologyChange> changes;
	private Task task;
	
	public UserAction(long timestamp, List<OWLOntologyChange> changes, Task task) {
		this.timestamp = timestamp;
		this.changes = changes;
		this.task = task;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public List<OWLOntologyChange> getChanges() {
		return changes;
	}
	
	public Task getTask() {
		return task;
	}
	
	public UserAction revert(){
		List<OWLOntologyChange> revertedChanges = new ArrayList<OWLOntologyChange>();
		for(OWLOntologyChange change : changes){
			if(change instanceof RemoveAxiom){
				revertedChanges.add(new AddAxiom(change.getOntology(), change.getAxiom()));
			} else if(change instanceof AddAxiom){
				revertedChanges.add(new RemoveAxiom(change.getOntology(), change.getAxiom()));
			}
		}
		return new UserAction(System.currentTimeMillis(), revertedChanges, task);
	}

	@Override
	public int compareTo(UserAction other) {
		long diff = other.getTimestamp() - timestamp;
		return (diff > 0) ? -1 : 1;
	}

}
