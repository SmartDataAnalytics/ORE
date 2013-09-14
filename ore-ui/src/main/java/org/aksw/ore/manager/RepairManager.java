package org.aksw.ore.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;

public class RepairManager {
	
	public interface RepairManagerListener {
		void repairPlanChanged();
		void repairPlanExecuted();
	}
	
	private Collection<OWLOntologyChange> repairPlan;
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	
	private Collection<RepairManagerListener> listeners = new HashSet<RepairManagerListener>();
	
	public RepairManager(OWLOntology ontology) {
		this.ontology = ontology;
		this.manager = ontology.getOWLOntologyManager();
		
		repairPlan = new HashSet<OWLOntologyChange>();
	}
	
	public void clearRepairPlan(){
		repairPlan.clear();
	}
	
	public Collection<OWLAxiom> getAxiomsScheduledToRemove(){
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		for(OWLOntologyChange change: repairPlan){
			if(change instanceof RemoveAxiom){
				axioms.add(change.getAxiom());
			}
		}
		return axioms;
	}
	
	public void addAxiomsToRemove(Collection<OWLAxiom> axioms){
		Collection<OWLOntologyChange> changes = new HashSet<OWLOntologyChange>();
		for(OWLAxiom ax : axioms){
			changes.add(new RemoveAxiom(ontology, ax));
		}
		addToRepairPlan(changes);
	}
	
	public void setAxiomsToRemove(Collection<OWLAxiom> axioms){
		removeChanges(RemoveAxiom.class);
		Collection<OWLOntologyChange> changes = new HashSet<OWLOntologyChange>();
		for(OWLAxiom ax : axioms){
			changes.add(new RemoveAxiom(ontology, ax));
		}
		addToRepairPlan(changes);
	}
	
	private void removeChanges(Class<? extends OWLOntologyChange> changeType){
		for (Iterator<OWLOntologyChange> iterator = repairPlan.iterator(); iterator.hasNext();) {
			OWLOntologyChange type = (OWLOntologyChange) iterator.next();
			if(type.getClass().equals(changeType)){
				iterator.remove();
			}
		}
	}
	
	
	public void addAxiomsToAdd(Collection<OWLAxiom> axioms){
		Collection<OWLOntologyChange> changes = new HashSet<OWLOntologyChange>();
		for(OWLAxiom ax : axioms){
			changes.add(new AddAxiom(ontology, ax));
		}
		addToRepairPlan(changes);
	}
	
	public void addToRepairPlan(Collection<OWLOntologyChange> changes){
		repairPlan.addAll(changes);
		fireRepairPlanChanged();
	}
	
	public void addToRepairPlan(OWLOntologyChange change){
		repairPlan.add(change);
		fireRepairPlanChanged();
	}
	
	public void removeFromRepairPlan(Collection<OWLOntologyChange> changes){
		repairPlan.removeAll(changes);
		fireRepairPlanChanged();
	}
	
	public void removeFromRepairPlan(OWLOntologyChange change){
		repairPlan.remove(change);
		fireRepairPlanChanged();
	}
	
	public Collection<OWLOntologyChange> getRepairPlan() {
		return repairPlan;
	}
	
	public void execute(){
		manager.applyChanges(new ArrayList<OWLOntologyChange>(repairPlan));
		repairPlan.clear();
		fireRepairPlanExecuted();
	}
	
	public Collection<OWLOntologyChange> reverse(Collection<OWLOntologyChange> changes){
		Collection<OWLOntologyChange> reversedChanges = new HashSet<OWLOntologyChange>();
		for(OWLOntologyChange change : changes){
			reversedChanges.add(reverse(change));
		}
		return reversedChanges;
	}
	
	public OWLOntologyChange reverse(OWLOntologyChange change){
		OWLOntologyChange reversedChange;
		if(change instanceof RemoveAxiom){
			reversedChange = new AddAxiom(change.getOntology(), change.getAxiom());
		} else {
			reversedChange = new RemoveAxiom(change.getOntology(), change.getAxiom());
		}
		return reversedChange;
	}
	
	public void addListener(RepairManagerListener l){
		listeners.add(l);
	}
	
	public void removeListener(RepairManagerListener l){
		listeners.remove(l);
	}
	
	private void fireRepairPlanChanged(){
		for(RepairManagerListener l :listeners){
			l.repairPlanChanged();
		}
	}
	
	private void fireRepairPlanExecuted(){
		for(RepairManagerListener l :listeners){
			l.repairPlanExecuted();
		}
	}
	

}
