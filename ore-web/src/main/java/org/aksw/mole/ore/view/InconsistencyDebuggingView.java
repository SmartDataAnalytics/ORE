package org.aksw.mole.ore.view;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.mole.ore.ExplanationManager2;
import org.aksw.mole.ore.ExplanationManagerListener;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.explanation.api.ExplanationType;
import org.aksw.mole.ore.widget.ExplanationOptionsPanel;
import org.aksw.mole.ore.widget.ExplanationTable;
import org.aksw.mole.ore.widget.ExplanationsPanel;
import org.aksw.mole.ore.widget.RepairPlanTable;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.vaadin.appfoundation.view.AbstractView;
import org.vaadin.sasha.portallayout.PortalLayout;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

public class InconsistencyDebuggingView extends AbstractView<HorizontalLayout> implements ExplanationManagerListener{
	
	private ExplanationOptionsPanel optionsPanel;
	
	private HorizontalLayout mainPanel;
	
	private RepairPlanTable repairPlanTable;
	private ExplanationsPanel explanationsPanel;
	
	private int currentLimit = 0;
	private ExplanationType currentExplanationType = ExplanationType.REGULAR;
	private Set<ExplanationTable> tables = new HashSet<ExplanationTable>();
	private Set<OWLAxiom> selectedAxioms = new HashSet<OWLAxiom>();
	private Map<ExplanationTable, Property.ValueChangeListener> table2Listener = new HashMap<ExplanationTable, Property.ValueChangeListener>();
	
	public InconsistencyDebuggingView() {
		super(new HorizontalLayout());
		initUI();
	}

	public void activated(Object... params) {
		UserSession.getRepairManager().addListener(repairPlanTable);
		UserSession.getExplanationManager().addListener(this);
		rebuiltData();
		showExplanations();
	}

	public void deactivated(Object... params) {
		UserSession.getRepairManager().removeListener(repairPlanTable);
		UserSession.getExplanationManager().removeListener(this);
	}
	
	private void initUI(){
		mainPanel = getContent();
		mainPanel.setSizeFull();
		
		//right side
		VerticalSplitPanel rightSide = new VerticalSplitPanel();
		rightSide.setSizeFull();
		mainPanel.addComponent(rightSide);
		mainPanel.setExpandRatio(rightSide, 1.0f);
		//show explanations in the top of the right side
		Component explanationsPanel = createExplanationsPanel();
		rightSide.addComponent(explanationsPanel);
		//show repair plan  in the bottom of the right side
		HorizontalLayout bottomRightSide = new HorizontalLayout();
		bottomRightSide.setSizeFull();
		Component repairPlanPanel = createRepairPlanPanel();
		bottomRightSide.addComponent(repairPlanPanel);
		rightSide.addComponent(bottomRightSide);
		rightSide.setSplitPosition(70);
		
		
//		reset();
	}
	
	private Component createExplanationsPanel(){
		PortalLayout explanationsPortal = new PortalLayout();
		explanationsPortal.setSizeFull();
		
		VerticalLayout rightPanel = new VerticalLayout();
		rightPanel.setSizeFull();
		rightPanel.setCaption("Explanations");
		explanationsPortal.addComponent(rightPanel);
		explanationsPortal.setClosable(rightPanel, false);
		explanationsPortal.setCollapsible(rightPanel, false);
		
		//put the options in the header of the portal
		optionsPanel = new ExplanationOptionsPanel();
		optionsPanel.setWidth(null);
		explanationsPortal.setHeaderComponent(rightPanel, optionsPanel);

		//wrapper for scrolling
		Panel panel = new Panel();
		panel.setSizeFull();
		rightPanel.addComponent(panel);
		rightPanel.setExpandRatio(panel, 1.0f);
		
		explanationsPanel = new ExplanationsPanel();
		panel.addComponent(explanationsPanel);
		
		return explanationsPortal;
	}
	
	private Component createRepairPlanPanel(){
		PortalLayout l = new PortalLayout();
		l.setSizeFull();
		
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setCaption("Repair Plan");
		wrapper.setSizeFull();
		l.addComponent(wrapper);
		
		repairPlanTable = new RepairPlanTable();
		wrapper.addComponent(repairPlanTable);
		wrapper.setExpandRatio(repairPlanTable, 1.0f);
		
		Button executeRepairButton = new Button("Apply");
		executeRepairButton.setHeight(null);
		executeRepairButton.addListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				UserSession.getRepairManager().execute();
				rebuiltData();
			}
		});
		wrapper.addComponent(executeRepairButton);
		wrapper.setComponentAlignment(executeRepairButton, Alignment.MIDDLE_RIGHT);
		
		return l;
		
	}
	
	private void rebuiltData(){
		clearExplanations();
		selectedAxioms.clear();
		tables.clear();
		table2Listener.clear();
		UserSession.getExplanationManager().clearCache();
		
	}
	
	public void reset(){
		optionsPanel.reset();
		clearExplanations();
	}
	
	private void clearExplanations(){
		for(ExplanationTable t : tables){
			explanationsPanel.removeComponent(t);
		}
		explanationsPanel.requestRepaint();
	}
	
	private void showExplanations() {
		clearExplanations();

		ExplanationManager2 expMan = UserSession.getExplanationManager();

		Set<Explanation<OWLAxiom>> explanations = expMan.getInconsistencyExplanations();

		for (Explanation<OWLAxiom> explanation : explanations) {
			final ExplanationTable t = new ExplanationTable(explanation, selectedAxioms);
			UserSession.getRepairManager().addListener(t);
//			t.setCaption(((OWLSubClassOfAxiom) explanation.getEntailment()).getSubClass().toString());
			explanationsPanel.addComponent(t);
			t.addListener(new Property.ValueChangeListener() {

				{
					table2Listener.put(t, this);
				}

				@Override
				public void valueChange(ValueChangeEvent event) {
					selectedAxioms.removeAll(t.getExplanation().getAxioms());
					selectedAxioms.addAll((Collection<? extends OWLAxiom>) event.getProperty().getValue());
					onAxiomSelectionChanged();
				}
			});
			tables.add(t);
		}

	}
	
	private void onAxiomSelectionChanged(){
//		propagateAxiomSelection();
		//we have to remove here all listeners because
		for(Entry<ExplanationTable, Property.ValueChangeListener> e : table2Listener.entrySet()){
			e.getKey().removeListener(e.getValue());
		}
		UserSession.getRepairManager().clearRepairPlan();
		UserSession.getRepairManager().addAxiomsToRemove(selectedAxioms);
		for(Entry<ExplanationTable, Property.ValueChangeListener> e : table2Listener.entrySet()){
			e.getKey().addListener(e.getValue());
		}
	}
	
	private void propagateAxiomSelection(){
//		selectedAxioms.clear();
//		for(ExplanationTable t : tables){
//			selectedAxioms.addAll((Collection<? extends OWLAxiom>) t.getValue());
//		}
		for(ExplanationTable t : tables){
			t.removeListener(table2Listener.get(t));
			t.selectAxioms(selectedAxioms);
			t.addListener(table2Listener.get(t));
		}
		System.out.println("Propagating axiom selection...");
		
	}

	@Override
	public void explanationLimitChanged(int explanationLimit) {
		if(currentLimit != explanationLimit){
			currentLimit = explanationLimit;
		}
		showExplanations();
	}

	@Override
	public void explanationTypeChanged(ExplanationType type) {
		if(currentExplanationType != type){
			currentExplanationType = type;
		}
		showExplanations();
	}
	

}
