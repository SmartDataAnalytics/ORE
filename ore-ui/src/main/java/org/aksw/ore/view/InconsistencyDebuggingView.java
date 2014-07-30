/**
 * 
 */
package org.aksw.ore.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.mole.ore.explanation.api.ExplanationType;
import org.aksw.ore.ORESession;
import org.aksw.ore.component.ConfigurablePanel;
import org.aksw.ore.component.ExplanationOptionsPanel;
import org.aksw.ore.component.ExplanationProgressDialog;
import org.aksw.ore.component.ExplanationTable;
import org.aksw.ore.component.ExplanationsPanel;
import org.aksw.ore.component.RepairPlanTable;
import org.aksw.ore.component.WhitePanel;
import org.aksw.ore.manager.ExplanationManager;
import org.aksw.ore.manager.ExplanationManagerListener;
import org.aksw.ore.manager.ExplanationProgressMonitorExtended;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

/**
 * @author Lorenz Buehmann
 *
 */
public class InconsistencyDebuggingView extends VerticalSplitPanel implements View, Refreshable, ExplanationProgressMonitorExtended<OWLAxiom>, ExplanationManagerListener{
	
	private ExplanationOptionsPanel optionsPanel;
	private RepairPlanTable repairPlanTable;
	private ExplanationsPanel explanationsPanel;
	
	ExplanationManager expMan;
	ExplanationProgressDialog progressDialog;
	
	private Set<OWLAxiom> selectedAxioms = new HashSet<OWLAxiom>();
	private List<ExplanationTable> explanationTables = new ArrayList<ExplanationTable>();
	private Map<ExplanationTable, Property.ValueChangeListener> table2Listener = new HashMap<ExplanationTable, Property.ValueChangeListener>();
	
	private int currentLimit = 1;
	private ExplanationType currentExplanationType = ExplanationType.REGULAR;
	
	private final OWLClass OWL_THING = new OWLDataFactoryImpl().getOWLThing();
	
	public InconsistencyDebuggingView() {
		addStyleName("dashboard-view");
		setSizeFull();
		
		// show explanations in the top of the panel
		Component explanationsPanel = createExplanationsPanel();
		setFirstComponent(explanationsPanel);
		
		// show repair plan in the bottom of the panel
		Component repairPlanPanel = createRepairPlanPanel();
		setSecondComponent(repairPlanPanel);
	}
	private Component createExplanationsPanel(){
		VerticalLayout l = new VerticalLayout();
		l.setSizeFull();
		l.setCaption("Explanations");
		
		explanationsPanel = new ExplanationsPanel();
		explanationsPanel.setCaption("Explanations");
		
		//put the options in the header of the portal
		optionsPanel = new ExplanationOptionsPanel(explanationsPanel);
		optionsPanel.setWidth(null);
		l.addComponent(optionsPanel);
		
		//wrapper for scrolling
		Panel panel = new Panel(explanationsPanel);
		panel.setSizeFull();
		l.addComponent(panel);
		l.setExpandRatio(panel, 1.0f);
		
		WhitePanel configurablePanel = new WhitePanel(explanationsPanel);
		configurablePanel.addComponent(optionsPanel);
		return configurablePanel;
	}
	
	private Component createRepairPlanPanel(){
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setCaption("Repair Plan");
		wrapper.setSizeFull();
		
		repairPlanTable = new RepairPlanTable();
		wrapper.addComponent(repairPlanTable);
		wrapper.setExpandRatio(repairPlanTable, 1.0f);
		
		Button executeRepairButton = new Button("Execute");
		executeRepairButton.setHeight(null);
		executeRepairButton.addClickListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				ORESession.getRepairManager().execute();
			}
		});
		wrapper.addComponent(executeRepairButton);
		wrapper.setComponentAlignment(executeRepairButton, Alignment.MIDDLE_RIGHT);
		
		return new WhitePanel(wrapper);
		
	}

	/* (non-Javadoc)
	 * @see com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener.ViewChangeEvent)
	 */
	@Override
	public void enter(ViewChangeEvent event) {
		ORESession.getExplanationManager().addExplanationProgressMonitor(this);
	}
	
	/* (non-Javadoc)
	 * @see com.vaadin.ui.AbstractComponent#attach()
	 */
	@Override
	public void attach() {
		super.attach();
		ExplanationManager expMan = ORESession.getExplanationManager();
		expMan.setExplanationLimit(currentLimit);
		ORESession.getRepairManager().addListener(repairPlanTable);
		ORESession.getExplanationManager().addListener(this);
		computeExplanations();
	}
	
	/* (non-Javadoc)
	 * @see com.vaadin.ui.AbstractComponent#detach()
	 */
	@Override
	public void detach() {
		super.detach();
		ORESession.getRepairManager().removeListener(repairPlanTable);
		ORESession.getExplanationManager().removeListener(this);
//		ORESession.getExplanationManager().removeExplanationProgressMonitor(this);
	}
	
	private void computeExplanations() {
		clearVisibleExplanations();
		progressDialog = new ExplanationProgressDialog(Collections.singleton(OWL_THING), currentLimit);
		ORESession.getExplanationManager().addExplanationProgressMonitor(progressDialog);
		getUI().addWindow(progressDialog);
		new Thread(new Runnable() {

			@Override
			public void run() {
				if(!progressDialog.isCancelled()){
					try {
						ORESession.getExplanationManager().getInconsistencyExplanations(ExplanationType.REGULAR, currentLimit);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} 
				ORESession.getExplanationManager().removeExplanationProgressMonitor(progressDialog);
			}
		}).start();
	}
	
	private void clearVisibleExplanations(){
		explanationsPanel.removeAllComponents();
		explanationTables.clear();
		table2Listener.clear();
	}
	
	private void showExplanation(final Explanation<OWLAxiom> explanation) {
		try {
			final ExplanationTable t = new ExplanationTable(explanation, selectedAxioms);
			if(explanation.getEntailment() != null){
				t.setCaption(((OWLSubClassOfAxiom)explanation.getEntailment()).getSubClass().toString());
			}
		t.addValueChangeListener(new Property.ValueChangeListener() {
			{table2Listener.put(t, this);}
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				selectedAxioms.removeAll(t.getExplanation().getAxioms());
				selectedAxioms.addAll((Collection<OWLAxiom>) event.getProperty().getValue());
				onAxiomSelectionChanged();
			}
		});
			ORESession.getRepairManager().addListener(t);
			ConfigurablePanel c = new ConfigurablePanel(t);
			c.setHeight(null);
			explanationsPanel.addComponent(c);
			explanationTables.add(t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void onAxiomSelectionChanged(){
//		propagateAxiomSelection();
		//we have to remove here all listeners because
		for(Entry<ExplanationTable, Property.ValueChangeListener> e : table2Listener.entrySet()){
			e.getKey().removeValueChangeListener(e.getValue());
		}
//		UserSession.getRepairManager().clearRepairPlan();
		ORESession.getRepairManager().setAxiomsToRemove(selectedAxioms);
		for(Entry<ExplanationTable, Property.ValueChangeListener> e : table2Listener.entrySet()){
			e.getKey().addValueChangeListener(e.getValue());
		}
	}
	
	private void showExplanations(final Set<Explanation<OWLAxiom>> explanations) {
		for (Explanation<OWLAxiom> exp : explanations) {
			showExplanation(exp);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.semanticweb.owl.explanation.api.ExplanationProgressMonitor#foundExplanation(org.semanticweb.owl.explanation.api.ExplanationGenerator, org.semanticweb.owl.explanation.api.Explanation, java.util.Set)
	 */
	@Override
	public void foundExplanation(final ExplanationGenerator<OWLAxiom> arg0, final Explanation<OWLAxiom> explanation,
			final Set<Explanation<OWLAxiom>> allExplanations) {
//		System.out.println(explanation);
		UI.getCurrent().access(new Runnable() {
	        @Override
	        public void run() {
	        	showExplanation(explanation);
	        }
	    });
	}
	

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.explanation.api.ExplanationProgressMonitor#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.ExplanationProgressMonitorExtended#allExplanationsFound(java.util.Set)
	 */
	@Override
	public void allExplanationsFound(final Set<Explanation<OWLAxiom>> allExplanations) {
		UI.getCurrent().access(new Runnable() {
	        @Override
	        public void run() {
//	        	showExplanations(allExplanations);
	        }
	    });
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.ExplanationManagerListener#explanationLimitChanged(int)
	 */
	@Override
	public void explanationLimitChanged(int explanationLimit) {
		if(currentLimit != explanationLimit){
			currentLimit = explanationLimit;
		}
		computeExplanations();
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.ExplanationManagerListener#explanationTypeChanged(org.aksw.mole.ore.explanation.api.ExplanationType)
	 */
	@Override
	public void explanationTypeChanged(ExplanationType type) {
		if(currentExplanationType != type){
			currentExplanationType = type;
		}
		computeExplanations();
	}
	/* (non-Javadoc)
	 * @see org.aksw.ore.view.Refreshable#refreshRendering()
	 */
	@Override
	public void refreshRendering() {
		for (ExplanationTable explanationTable : explanationTables) {
			explanationTable.refreshRowCache();
		}
		repairPlanTable.refreshRowCache();
	}

	
}
