package org.aksw.mole.ore.widget;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.mole.ore.UserSession;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;
import org.vaadin.sasha.portallayout.PortalLayout;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;

public class ExplanationsPanel extends PortalLayout{
	
	private IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
	private Set<ExplanationTable> tables = new HashSet<ExplanationTable>();
	private Set<OWLAxiom> selectedAxioms = new HashSet<OWLAxiom>();
	private Map<ExplanationTable, Property.ValueChangeListener> table2Listener = new HashMap<ExplanationTable, Property.ValueChangeListener>();
	
	private Collection<Explanation<OWLAxiom>> explanations;
	private boolean aggregatedView = false;
	
	public ExplanationsPanel() {
		setImmediate(true);
	}
	
	public void showExplanations(Collection<Explanation<OWLAxiom>> explanations){
		this.explanations = explanations;
		refreshUI();
	}
	
	private void refreshUI(){
		removeAllComponents();
		if(aggregatedView){
			showAggregatedTable();
		} else {
			showSeparateTables();
		}
	}
	
	public void addExplanation(Explanation explanation){
		final ExplanationTable t = new ExplanationTable(explanation, selectedAxioms);
		if(explanation.getEntailment() != null){
//			t.setCaption(((OWLSubClassOfAxiom)explanation.getEntailment()).getSubClass().toString());
			t.setCaption(sfp.getShortForm(((OWLSubClassOfAxiom)explanation.getEntailment()).getSubClass().asOWLClass().getIRI()));
		}
		t.addListener(new Property.ValueChangeListener() {
			{table2Listener.put(t, this);}
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				selectedAxioms.removeAll(t.getExplanation().getAxioms());
				selectedAxioms.addAll((Collection<? extends OWLAxiom>) event.getProperty().getValue());
				onAxiomSelectionChanged();
			}
		});
		tables.add(t);
		UserSession.getRepairManager().addListener(t);
		addComponent(t);
		
	}
	
	public void addExplanations(Collection<Explanation<OWLAxiom>> explanations){
		for(Explanation exp : explanations){
			addExplanation(exp);
		}
	}
	
	public void reset(){
		aggregatedView = false;
		removeAllComponents();
		tables.clear();
		table2Listener.clear();
		selectedAxioms.clear();
		explanations = null;
	}
	
	public void showAggregatedView(boolean aggregatedView) {
		this.aggregatedView = aggregatedView;
		refreshUI();
	}
	
	private void showAggregatedTable(){
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		for(Explanation<OWLAxiom> exp : explanations){
			axioms.addAll(exp.getAxioms());
		}
		Explanation<OWLAxiom> aggregatedExplanation = new Explanation<OWLAxiom>(null, axioms);
		ExplanationTable aggregatedTable = new ExplanationTable(aggregatedExplanation, selectedAxioms, false);
		addComponent(aggregatedTable);
	}
	
	private void showSeparateTables(){
		for(Explanation<OWLAxiom> exp : explanations){
			addExplanation(exp);
		}
	}
	
	private void onAxiomSelectionChanged(){
//		propagateAxiomSelection();
		//we have to remove here all listeners because
		for(Entry<ExplanationTable, Property.ValueChangeListener> e : table2Listener.entrySet()){
			e.getKey().removeListener(e.getValue());
		}
//		UserSession.getRepairManager().clearRepairPlan();
		UserSession.getRepairManager().setAxiomsToRemove(selectedAxioms);
		for(Entry<ExplanationTable, Property.ValueChangeListener> e : table2Listener.entrySet()){
			e.getKey().addListener(e.getValue());
		}
	}

}
