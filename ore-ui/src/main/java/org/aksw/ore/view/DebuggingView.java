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
import org.aksw.ore.manager.ExplanationManager;
import org.aksw.ore.manager.ExplanationManagerListener;
import org.aksw.ore.manager.ExplanationProgressMonitorExtended;
import org.apache.log4j.Logger;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.AbstractSelect.ItemDescriptionGenerator;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

/**
 * @author Lorenz Buehmann
 *
 */
public class DebuggingView extends HorizontalSplitPanel implements View, ExplanationProgressMonitorExtended<OWLAxiom>, ExplanationManagerListener{
	
	
	private static final Logger logger = Logger.getLogger(DebuggingView.class.getName());
	
	private ExplanationOptionsPanel optionsPanel;
	private RepairPlanTable repairPlanTable;
	private ExplanationsPanel explanationsPanel;
	
	ExplanationManager expMan;
	ExplanationProgressDialog progressDialog;
	private Table classesTable;
	
	private Set<OWLAxiom> selectedAxioms = new HashSet<OWLAxiom>();
	private List<ExplanationTable> explanationTables = new ArrayList<ExplanationTable>();
	private Map<ExplanationTable, Property.ValueChangeListener> table2Listener = new HashMap<ExplanationTable, Property.ValueChangeListener>();
	
	private int currentLimit = 1;
	private ExplanationType currentExplanationType = ExplanationType.REGULAR;
	
	private boolean firstViewVisit = true;
	
	public DebuggingView() {
		addStyleName("dashboard-view");
		setSizeFull();
		
		classesTable = new Table("Unsatisfiable classes");
		classesTable.addStyleName("unsatisfiable-classes-table");
		classesTable.addContainerProperty("root", String.class, null);
		classesTable.addContainerProperty("class", String.class, null);
		classesTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		classesTable.setSelectable(true);
		classesTable.setMultiSelect(true);
		classesTable.setImmediate(true);
		classesTable.setWidth("100%");
//		classesTable.setPageLength(0);
		classesTable.setHeight("100%");
		classesTable.setColumnExpandRatio("class", 1f);
//		classesTable.addItemClickListener(new ItemClickListener() {
//			@Override
//			public void itemClick(ItemClickEvent event) {
//				computeExplanations((OWLClass) event.getItemId());
//			}
//		});
		classesTable.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				computeExplanations((Set<OWLClass>) event.getProperty().getValue());
			}
		});
//		classesTable.addGeneratedColumn("root", new ColumnGenerator() {
//			
//			@Override
//			public Object generateCell(Table source, Object itemId, Object columnId) {
//				Label l = new Label();
//				OWLClass cls = (OWLClass)itemId;
//				boolean root = ORESession.getExplanationManager().getRootUnsatisfiableClasses().contains(cls);
//				l.setValue((root ? "<span class=\"root-class\"></span>" : ""));
//				l.setContentMode(ContentMode.HTML);
//				return l;
//			}
//		});
		classesTable.setCellStyleGenerator(new Table.CellStyleGenerator() {
			
			@Override
			public String getStyle(Table source, Object itemId, Object propertyId) {
				if(propertyId == null){
					return null;
				} else if(propertyId.equals("root")){
					OWLClass cls = (OWLClass)itemId;
					boolean root = ORESession.getExplanationManager().getRootUnsatisfiableClasses().contains(cls);
					if(root){
						return "root-class";
					} 
				} 
				return null;
			}
		});
		classesTable.setItemDescriptionGenerator(new ItemDescriptionGenerator() {
			ExplanationManager expMan = ORESession.getExplanationManager();
			
			public String generateDescription(Component source, Object itemId, Object propertyId) {
			    if(propertyId == null){
			        return itemId.toString();
			    } else if(propertyId.equals("root")) {
			    	boolean root = expMan.getRootUnsatisfiableClasses().contains(itemId);
			    	if(root){
			    		return "The unsatisfiability of this class is a possible cause for the unsatisfiability of other classes";
			    	}
			    }                                                                       
			    return null;
			}});
		
		classesTable.setVisibleColumns(new String[]{"root", "class"});
//		classesTable.sort(new String[]{"root", "class"}, new boolean[]{false, true});
		
		//the classes table on the left side
		setFirstComponent(new ConfigurablePanel(classesTable));
		//right explanations, repair plan and impact on the right side
		setSecondComponent(createRightSide());
		
		setSplitPosition(30, Unit.PERCENTAGE);
	}
	
	private Component createRightSide(){
		VerticalSplitPanel rightSide = new VerticalSplitPanel();
		rightSide.setSizeFull();
		rightSide.setSplitPosition(75);
		
		//show explanations in the top of the right side
		Component explanationsPanel = createExplanationsPanel();
		rightSide.addComponent(explanationsPanel);
		//show repair plan  in the bottom of the right side
		Component repairPlanPanel = createRepairPlanPanel();
		rightSide.addComponent(repairPlanPanel);
		
		return rightSide;
	}
	
	private Component createExplanationsPanel(){
		VerticalLayout l = new VerticalLayout();
		l.setSizeFull();
		l.setCaption("Explanations");
		
		//put the options in the header of the portal
		optionsPanel = new ExplanationOptionsPanel();
		optionsPanel.setWidth(null);
		l.addComponent(optionsPanel);
		
		
		explanationsPanel = new ExplanationsPanel();
		explanationsPanel.setCaption("Explanations");
		//wrapper for scrolling
		Panel panel = new Panel(explanationsPanel);
		panel.setSizeFull();
		l.addComponent(panel);
		l.setExpandRatio(panel, 1.0f);
		
		ConfigurablePanel configurablePanel = new ConfigurablePanel(explanationsPanel);
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
		
		return new ConfigurablePanel(wrapper);
		
	}

	/* (non-Javadoc)
	 * @see com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener.ViewChangeEvent)
	 */
	@Override
	public void enter(ViewChangeEvent event) {
		if(firstViewVisit){
			ORESession.getExplanationManager().addExplanationProgressMonitor(this);
			loadUnsatisfiableClasses();
			firstViewVisit = false;
		}
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
	
	private void loadUnsatisfiableClasses(){
		logger.info("Loading unsatisfiable classes...");
		
		IndexedContainer c = new IndexedContainer();
		c.addContainerProperty("class", String.class, null);
		c.addContainerProperty("root", String.class, null);
		try {
			ExplanationManager expMan = ORESession.getExplanationManager();
			//first the root classes
			Set<OWLClass> classes = expMan.getRootUnsatisfiableClasses();
			for (OWLClass cls : classes) {
				Item i = c.addItem(cls);
				i.getItemProperty("class").setValue(cls.toString());
//				boolean root = ORESession.getExplanationManager().getRootUnsatisfiableClasses().contains(cls);
//				i.getItemProperty("root").setValue(root ? "R" : "");
			}
			//then the derived classes
			classes = expMan.getDerivedUnsatisfiableClasses();
			for (OWLClass cls : classes) {
				Item i = c.addItem(cls);
				i.getItemProperty("class").setValue(cls.toString());
//				boolean root = ORESession.getExplanationManager().getRootUnsatisfiableClasses().contains(cls);
//				i.getItemProperty("root").setValue(root ? "R" : "");
			}
		} catch (TimeOutException e) {
			e.printStackTrace();
		} catch (InconsistentOntologyException e) {
			e.printStackTrace();
		} catch (ReasonerInterruptedException e) {
			e.printStackTrace();
		}
		classesTable.setContainerDataSource(c);
	}
	
	private void computeExplanations(final Set<OWLClass> unsatClasses) {
		explanationsPanel.removeAllComponents();
		progressDialog = new ExplanationProgressDialog(unsatClasses, currentLimit);
		ORESession.getExplanationManager().addExplanationProgressMonitor(progressDialog);
		getUI().addWindow(progressDialog);
		new Thread(new Runnable() {

			@Override
			public void run() {
				for (OWLClass cls : unsatClasses) {
					if(!progressDialog.isCancelled()){
						try {
							final Set<Explanation<OWLAxiom>> explanations = ORESession.getExplanationManager()
									.getUnsatisfiabilityExplanations(cls, currentLimit);
//							UI.getCurrent().access(new Runnable() {
//								@Override
//								public void run() {
//									showExplanations(explanations);
//								}
//							});
						} catch (Exception e) {
							e.printStackTrace();
						}
					} 
				}
				ORESession.getExplanationManager().removeExplanationProgressMonitor(progressDialog);
				UI.getCurrent().removeWindow(progressDialog);
			}
		}).start();
	}
	
	private void computeExplanations(final OWLClass unsatClass){
		computeExplanations(Collections.singleton(unsatClass));
	}
	
	private void computeExplanations(){
		Set<OWLClass> unsatClasses = (Set<OWLClass>) classesTable.getValue();
		if(unsatClasses != null){
			computeExplanations(unsatClasses);
		}
	}
	
	private void showExplanation2(final Explanation<OWLAxiom> explanation) {
		final Table t = new Table();
		t.addContainerProperty("axiom", String.class, null);
		for (OWLAxiom axiom : explanation.getAxioms()) {
			t.addItem(axiom).getItemProperty("axiom").setValue(axiom.toString());
		}
		t.setWidth("100%");
		t.setImmediate(true);
		t.setSelectable(true);
		t.addStyleName("borderless");
//		t.addStyleName("plain");
		t.setCaption(explanation.getEntailment().toString());
		t.setPageLength(0);
		t.setHeight(null);
		final Set<OWLAxiom> selectedAxioms = new HashSet<OWLAxiom>(explanation.getAxioms());
		selectedAxioms.retainAll(selectedAxioms);
		t.addGeneratedColumn("selected", new Table.ColumnGenerator() {
			@Override
			public Object generateCell(Table source, final Object itemId, Object columnId) {
				final OWLAxiom axiom = (OWLAxiom) itemId;
				boolean selected = selectedAxioms.contains(axiom);
				if(selected){
					t.select(axiom);
				}
				final CheckBox cb = new CheckBox("", selected);
				cb.addValueChangeListener(new Property.ValueChangeListener() {
					@Override
					public void valueChange(Property.ValueChangeEvent event) {
						if (selectedAxioms.contains(axiom)) {
							selectedAxioms.remove(axiom);
							selectedAxioms.remove(axiom);
						} else {
							selectedAxioms.add(axiom);
							selectedAxioms.add(axiom);
						}
						updateSelection();
					}
				});
				return cb;
			}
		});
		t.setColumnHeader("selected", "");
		t.setVisibleColumns(new String[]{"selected", "axiom"});
		ConfigurablePanel c = new ConfigurablePanel(t);
		c.setHeight(null);
		explanationsPanel.addComponent(c);
//		explanationTables.add(t);
		
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
	
	
	private void updateSelection(){
		for (Table t : explanationTables) {
			for (OWLAxiom axiom : selectedAxioms) {
				t.select(axiom);
			}
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
//	        	showExplanation(explanation);
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
	        	showExplanations(allExplanations);
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

	
}
