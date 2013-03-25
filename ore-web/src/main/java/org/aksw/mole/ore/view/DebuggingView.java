package org.aksw.mole.ore.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.mole.ore.ExplanationManager2;
import org.aksw.mole.ore.ExplanationManagerListener;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.explanation.api.ExplanationType;
import org.aksw.mole.ore.widget.ClassesTable;
import org.aksw.mole.ore.widget.ExplanationOptionsPanel;
import org.aksw.mole.ore.widget.ExplanationTable;
import org.aksw.mole.ore.widget.ExplanationsPanel;
import org.aksw.mole.ore.widget.ImpactPanel;
import org.aksw.mole.ore.widget.RepairPlanTable;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.vaadin.appfoundation.view.AbstractView;
import org.vaadin.sasha.portallayout.PortalLayout;

import com.clarkparsia.owlapi.explanation.util.ExplanationProgressMonitor;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

public class DebuggingView extends AbstractView<HorizontalSplitPanel> implements ExplanationManagerListener, ExplanationProgressMonitor{
	
	private Component optionsPanel;
	
	private HorizontalSplitPanel mainPanel;
	
	private ClassesTable classesTable;
	private ImpactPanel impactPanel;
	private RepairPlanTable repairPlanTable;
	private ExplanationsPanel explanationsPanel;
	
	private int currentLimit = 0;
	private ExplanationType currentExplanationType = ExplanationType.REGULAR;
	private Set<ExplanationTable> tables = new HashSet<ExplanationTable>();
	private Set<OWLAxiom> selectedAxioms = new HashSet<OWLAxiom>();
	private Set<OWLClass> selectedClasses = new HashSet<OWLClass>();
	private Map<ExplanationTable, Property.ValueChangeListener> table2Listener = new HashMap<ExplanationTable, Property.ValueChangeListener>();
	
	public enum Task{
		COMPUTE_ALL_EXPLANATIONS,
		COMPUTE_DIAGNOSIS
	}
	
	public DebuggingView() {
		super(new HorizontalSplitPanel());
		initUI();
	}

	public void activated(Object... params) {
		UserSession.getRepairManager().addListener(repairPlanTable);
		UserSession.getRepairManager().addListener(classesTable);
		UserSession.getExplanationManager().addListener(this);
//		UserSession.getExplanationManager().setExplanationProgressMonitor(this);
		rebuildData();
//		classesTable.refresh();
	}

	public void deactivated(Object... params) {
		UserSession.getRepairManager().removeListener(repairPlanTable);
		UserSession.getRepairManager().removeListener(classesTable);
		UserSession.getExplanationManager().removeListener(this);
		UserSession.getExplanationManager().setExplanationProgressMonitor(null);
	}
	
	private void initUI(){
		mainPanel = getContent();
		mainPanel.setSizeFull();
		
		//left side
		Component classesPanel = createClassesPanel();
		mainPanel.addComponent(classesPanel);
		
		//right side
		VerticalSplitPanel rightSide = new VerticalSplitPanel();
		rightSide.setSizeFull();
		mainPanel.addComponent(rightSide);
		mainPanel.setSplitPosition(20);
		//show explanations in the top of the right side
		Component explanationsPanel = createExplanationsPanel();
		rightSide.addComponent(explanationsPanel);
		//show repair plan and impact in the bottom of the right side
		HorizontalSplitPanel bottomRightSide = new HorizontalSplitPanel();
		bottomRightSide.setSizeFull();
		Component repairPlanPanel = createRepairPlanPanel();
		bottomRightSide.addComponent(repairPlanPanel);
		Component impactPanel = createImpactPanel();
		bottomRightSide.addComponent(impactPanel);
		rightSide.addComponent(bottomRightSide);
		rightSide.setSplitPosition(70);
		
		
//		reset();
	}
	
	private Component createClassesPanel(){
		PortalLayout classesPortal = new PortalLayout();
		classesPortal.setSizeFull();
		
		classesTable = new ClassesTable();
		classesTable.addListener(new Table.ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				onClassSelectionChanged();
			}
		});
		classesPortal.addComponent(classesTable);
		classesPortal.setClosable(classesTable, false);
		classesPortal.setCollapsible(classesTable, false);
		classesPortal.setLocked(classesTable, true);
		
		HorizontalLayout header = new HorizontalLayout();
		header.setSizeUndefined();
		
		MenuBar menu = new MenuBar();
		MenuItem taskItem = menu.addItem("Task", null);
		taskItem.addItem(Task.COMPUTE_ALL_EXPLANATIONS.name(), new MenuBar.Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				computeAllExplanations();
			}
		});
		header.addComponent(menu);
		header.setComponentAlignment(menu, Alignment.MIDDLE_LEFT);
//		classesPortal.setHeaderComponent(classesTable, header);
		
		return classesPortal;
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
//		explanationsPortal.setHeaderComponent(rightPanel, optionsPanel);
		CheckBox cb = new CheckBox("Show aggregated view");
		cb.setImmediate(true);
		cb.addListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				explanationsPanel.showAggregatedView((Boolean)event.getProperty().getValue());
			}
		});
		HorizontalLayout header = new HorizontalLayout();
		header.addStyleName("explanation-options");
		header.addComponent(cb);
		header.setComponentAlignment(cb, Alignment.MIDDLE_LEFT);
		header.addComponent(optionsPanel);
		explanationsPortal.setHeaderComponent(rightPanel, header);
		
		//wrapper for scrolling
		Panel panel = new Panel();
		panel.setSizeFull();
		rightPanel.addComponent(panel);
		rightPanel.setExpandRatio(panel, 1.0f);
		
		explanationsPanel = new ExplanationsPanel();
		panel.addComponent(explanationsPanel);
		
		return explanationsPortal;
	}
	
	private Component createImpactPanel(){
		PortalLayout l = new PortalLayout();
		l.setSizeFull();
		impactPanel = new ImpactPanel();
		l.addComponent(impactPanel);
		l.setComponentCaption(impactPanel, "Impact");
		return l;
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
				onExecuteRepairPlan();
			}
		});
		wrapper.addComponent(executeRepairButton);
		wrapper.setComponentAlignment(executeRepairButton, Alignment.MIDDLE_RIGHT);
		
		return l;
		
	}
	
	private void onExecuteRepairPlan(){
		UserSession.getRepairManager().execute();
		rebuildData();
	}
	
	private void rebuildData(){
		clearExplanations();
		selectedAxioms.clear();
		selectedClasses = new HashSet<OWLClass>();
		tables.clear();
		table2Listener.clear();
		UserSession.getExplanationManager().clearCache();
	}
	
	private void computeAllExplanations(){
		ExplanationManager2 expMan = UserSession.getExplanationManager();
		List<Explanation<OWLAxiom>> explanations = new ArrayList<Explanation<OWLAxiom>>();
		for(OWLClass cls : expMan.getUnsatisfiableClasses()){
			explanations.addAll(expMan.getUnsatisfiabilityExplanations(cls, currentExplanationType));
		}
		clearExplanations();
		explanationsPanel.showExplanations(explanations);
	}
	
	public void reset(){
		classesTable.removeAllItems();
		explanationsPanel.reset();
//		clearExplanations();
		impactPanel.reset();
		repairPlanTable.reset();
	}
	
	private void onClassSelectionChanged(){
		showExplanations();
	}
	
	private void clearExplanations(){
		for(ExplanationTable t : tables){
			explanationsPanel.removeComponent(t);
		}
		explanationsPanel.requestRepaint();
	}
	
	private void showExplanations(){
//		selectedAxioms.clear();
		
		Set<OWLClass> newSelectedClasses = (Set<OWLClass>) classesTable.getValue();
		System.out.println("new selected classes: " + newSelectedClasses);
		
		boolean clearPanel = !newSelectedClasses.containsAll(selectedClasses);
//		if(clearPanel){
			System.out.println("clear panel");
			clearExplanations();
//		}
		
		Set<OWLClass> diff = new HashSet<OWLClass>(newSelectedClasses);
		diff.removeAll(selectedClasses);
		System.out.println("diff: " + diff);
		
		selectedClasses = newSelectedClasses;
		
		ExplanationManager2 expMan = UserSession.getExplanationManager();
		
		Set<Explanation<OWLAxiom>> explanations = new HashSet<Explanation<OWLAxiom>>();
		for(OWLClass cls : newSelectedClasses){
			explanations.addAll(expMan.getUnsatisfiabilityExplanations(cls));
			explanationsPanel.showExplanations(explanations);
		}
	}
	
	@Override
	public void explanationLimitChanged(int explanationLimit) {
		if(currentLimit != explanationLimit){
//			clearExplanations();
			currentLimit = explanationLimit;
		}
		showExplanations();
	}

	@Override
	public void explanationTypeChanged(ExplanationType type) {
		if(currentExplanationType != type){
//			clearExplanations();
			currentExplanationType = type;
		}
		showExplanations();
	}
	
	/**
	 * Explanation progress monitor.
	 */

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public void foundExplanation(Set<OWLAxiom> axioms) {
		
	}

	@Override
	public void foundAllExplanations() {
		
	}
	

}
