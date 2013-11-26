package org.aksw.ore.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.mole.ore.explanation.api.ExplanationType;
import org.aksw.mole.ore.sparql.trivial_old.SPARQLBasedTrivialInconsistencyFinder;
import org.aksw.ore.ORESession;
import org.aksw.ore.component.ConfigurablePanel;
import org.aksw.ore.component.ExplanationOptionsPanel;
import org.aksw.ore.component.ExplanationsPanel;
import org.aksw.ore.component.RepairPlanTable;
import org.aksw.ore.component.SPARQLBasedExplanationTable;
import org.aksw.ore.component.SPARQLDebuggingProgressDialog;
import org.aksw.ore.component.SPARULDialog;
import org.aksw.ore.component.WhitePanel;
import org.aksw.ore.manager.ExplanationManagerListener;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

public class SPARQLDebuggingView extends HorizontalSplitPanel implements View, ExplanationManagerListener, ValueChangeListener{
	
	private ExplanationOptionsPanel optionsPanel;
	private RepairPlanTable repairPlanTable;
	private ExplanationsPanel explanationsPanel;
	
	private Button startButton;
	private Button stopButton;
	
	private CheckBox useLinkedDataCheckBox;
	private CheckBox assumeUNA;
	private CheckBox stopIfInconsistencyFoundCheckBox;
	
	private CheckBox limitExplanationCountCheckbox;
	private IntStepper limitSpinner;
	
	private ListSelect uriList;
	
	private int currentLimit = 0;
	private ExplanationType currentExplanationType = ExplanationType.REGULAR;
	private Set<SPARQLBasedExplanationTable> tables = new HashSet<SPARQLBasedExplanationTable>();
	private Set<OWLAxiom> selectedAxioms = new HashSet<OWLAxiom>();
	private Map<SPARQLBasedExplanationTable, Property.ValueChangeListener> table2Listener = new HashMap<SPARQLBasedExplanationTable, Property.ValueChangeListener>();
	
//	private SPARQLBasedInconsistencyFinder incFinder;
	SPARQLBasedTrivialInconsistencyFinder incFinder;
	Set<Explanation<OWLAxiom>> explanations;
	
	public SPARQLDebuggingView() {
		addStyleName("dashboard-view");
		initUI();
	}
	
	/* (non-Javadoc)
	 * @see com.vaadin.ui.AbstractComponent#attach()
	 */
	@Override
	public void attach() {
		super.attach();
		ORESession.getRepairManager().addListener(repairPlanTable);
		ORESession.getSPARQLExplanationManager().addListener(this);
//		rebuiltData();
	}
	
	/* (non-Javadoc)
	 * @see com.vaadin.ui.AbstractComponent#detach()
	 */
	@Override
	public void detach() {
		super.detach();
		ORESession.getRepairManager().removeListener(repairPlanTable);
		ORESession.getSPARQLExplanationManager().removeListener(this);
	}
	
	private void initUI(){
		setSizeFull();
		setSplitPosition(25);
		
		Component leftSide = createLeftSide();
		addComponent(leftSide);
		
		Component rightSide = createRightSide();
		addComponent(rightSide);
		
		
//		reset();
	}
	
	private Component createLeftSide(){
		VerticalLayout l = new VerticalLayout();
		l.setSizeFull();
		l.setSpacing(true);
		l.setCaption("Options");
		
		Component configForm = createConfigForm();
		l.addComponent(configForm);
		
		return new WhitePanel(l);
	}
	
	private Component createConfigForm(){
		VerticalLayout form = new VerticalLayout();
		form.setSizeFull();
//		form.setSpacing(true);
		form.addStyleName("sparql-debug-options-form");
//		form.setMargin(new MarginInfo(false, false, true, false));
		
		stopIfInconsistencyFoundCheckBox = new CheckBox("Stop if inconsistency found");
		stopIfInconsistencyFoundCheckBox.setValue(true);
		stopIfInconsistencyFoundCheckBox.setImmediate(true);
		stopIfInconsistencyFoundCheckBox.setDescription("If enabled, the algorithm stops immediately once reasons for inconsistency are found.");
		form.addComponent(stopIfInconsistencyFoundCheckBox);
		
		assumeUNA = new CheckBox("Assume Unique Name Assumption");
		assumeUNA.setDescription("If enabled, resources with different names are supposed to be different entities.");
		assumeUNA.setValue(false);
		assumeUNA.setImmediate(true);
		form.addComponent(assumeUNA);
		
		final ModifiableListField uriList = new ModifiableListField();
		
		VerticalLayout linkedDataOptions = new VerticalLayout();
		linkedDataOptions.setSizeFull();
		form.addComponent(linkedDataOptions);
		
		useLinkedDataCheckBox = new CheckBox();
		useLinkedDataCheckBox.setValue(false);
		useLinkedDataCheckBox.setCaption("Use Linked Data");
		useLinkedDataCheckBox.setDescription("If enabled, additional data is fetched by dereferencing the URIs of resources starting with namespaces from the "
				+ "list below.");
		useLinkedDataCheckBox.setImmediate(true);
		useLinkedDataCheckBox.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				uriList.setEnabled((Boolean) event.getProperty().getValue());
			}
		});
		linkedDataOptions.addComponent(useLinkedDataCheckBox);
		linkedDataOptions.addComponent(uriList);
		linkedDataOptions.setExpandRatio(uriList, 1f);
		form.setExpandRatio(linkedDataOptions, 1f);
		
		startButton = new Button("Start");
		startButton.setDescription("Click to start the detection of possible inconsistency causes.");
		startButton.setDisableOnClick(true);
		startButton.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onSearchInconsistency();
			}
		});
		
		form.addComponent(startButton);
		form.setComponentAlignment(startButton, Alignment.MIDDLE_CENTER);
		
		return form;
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
		
		limitExplanationCountCheckbox = new CheckBox("Limit explanation count to");
		limitExplanationCountCheckbox.setValue(true);
		limitExplanationCountCheckbox.addValueChangeListener(this);
		limitExplanationCountCheckbox.setImmediate(true);
		limitExplanationCountCheckbox.setWidth(null);
		
		limitSpinner = new IntStepper();
		limitSpinner.setImmediate(true);
		limitSpinner.setValue(1);
		limitSpinner.setStepAmount(1);
		limitSpinner.setMinValue(1);
		limitSpinner.addValueChangeListener(this);
		
		HorizontalLayout limit = new HorizontalLayout();
		limit.addComponent(limitExplanationCountCheckbox);
		limit.addComponent(limitSpinner);
		limit.setExpandRatio(limitSpinner, 1f);
		limit.setComponentAlignment(limitSpinner, Alignment.MIDDLE_LEFT);
		limit.setComponentAlignment(limitExplanationCountCheckbox, Alignment.MIDDLE_CENTER);
		limit.addStyleName("explanation-options");
//		l.addComponent(limit);
		limit.setWidth(null);
		
		//put the options in the header of the portal
//		optionsPanel = new ExplanationOptionsPanel();
//		optionsPanel.setWidth(null);
//		l.addComponent(optionsPanel);
		
		
		explanationsPanel = new ExplanationsPanel();
		explanationsPanel.setCaption("Explanations");
		explanationsPanel.setHeight(null);
		//wrapper for scrolling
		Panel panel = new Panel(explanationsPanel);
		panel.setSizeFull();
		l.addComponent(panel);
		l.setExpandRatio(panel, 1.0f);
		
		WhitePanel configurablePanel = new WhitePanel(l);
		configurablePanel.addComponent(limit);
		return configurablePanel;
	}
	
	private Component createRepairPlanPanel(){
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setCaption("Repair Plan");
		wrapper.setSizeFull();
		
		repairPlanTable = new RepairPlanTable();
		wrapper.addComponent(repairPlanTable);
		wrapper.setExpandRatio(repairPlanTable, 1.0f);
		
		Button executeRepairButton = new Button("Dump as SPARUL");
		executeRepairButton.setHeight(null);
		executeRepairButton.addClickListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onDumpSPARUL();
//				ORESession.getRepairManager().execute();
				rebuiltData();
			}
		});
		wrapper.addComponent(executeRepairButton);
		wrapper.setComponentAlignment(executeRepairButton, Alignment.MIDDLE_RIGHT);
		
		return new WhitePanel(wrapper);
		
	}
	
	private void rebuiltData(){
		clearExplanations();
		selectedAxioms.clear();
		tables.clear();
		table2Listener.clear();
		ORESession.getExplanationManager().clearCache();
	}
	
	public void reset(){
		optionsPanel.reset();
		startButton.setEnabled(true);
		stopButton.setEnabled(false);
		clearExplanations();
	}
	
	private void clearExplanations(){
		for(SPARQLBasedExplanationTable t : tables){
			explanationsPanel.removeComponent(t);
		}
	}
	
	private void onSearchInconsistency(){
		startButton.setEnabled(false);
		incFinder = ORESession.getSparqlBasedInconsistencyFinder();
		incFinder.setStopIfInconsistencyFound(stopIfInconsistencyFoundCheckBox.getValue());
//		incFinder.setUseLinkedData((Boolean) useLinkedDataCheckBox.getValue());
		Set<String> namespaces = new HashSet<String>();
		for(Object item : uriList.getItemIds()){
			namespaces.add((String)item);
		}
//		incFinder.setLinkedDataNamespaces(namespaces);
		final SPARQLDebuggingProgressDialog progressDialog = new SPARQLDebuggingProgressDialog();
		incFinder.addProgressMonitor(progressDialog);
		getUI().addWindow(progressDialog);
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				incFinder.run();
				explanations = incFinder.getExplanations();
				ORESession.getSPARQLExplanationManager().setExplanations(explanations);
				if(!explanations.isEmpty()){
					UI.getCurrent().access(new Runnable() {
						
						@Override
						public void run() {
								showExplanations();
						}

					});
				}
				
				UI.getCurrent().access(new Runnable() {
					
					@Override
					public void run() {
						incFinder.removeProgressMonitor(progressDialog);
						UI.getCurrent().removeWindow(progressDialog);
						startButton.setEnabled(true);
					}
				});
			}
		});
		t.start();
	}
	

	private void showExplanations() {
		clearExplanations();
		int i = 1;
		for (Explanation<OWLAxiom> explanation : explanations) {
			final SPARQLBasedExplanationTable t = new SPARQLBasedExplanationTable(explanation, selectedAxioms);
			ORESession.getRepairManager().addListener(t);
//			t.setCaption(((OWLSubClassOfAxiom) explanation.getEntailment()).getSubClass().toString());
			explanationsPanel.addComponent(t);
			t.addValueChangeListener(new Property.ValueChangeListener() {

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
			if(i++ == ORESession.getSPARQLExplanationManager().getExplanationLimit()){
				break;
			}
		}
	}
	
	public void onStopSearchingInconsistency() {
		incFinder.stop();
	}
	
//	private void showExplanations() {
//		clearExplanations();
//
//		ExplanationManager expMan = ORESession.getExplanationManager();
//		System.out.println(ORESession.getSparqlBasedInconsistencyFinder().getReasoner().isConsistent());
//		expMan.setReasoner(ORESession.getSparqlBasedInconsistencyFinder().getReasoner());
//
//		Set<Explanation<OWLAxiom>> explanations = expMan.getInconsistencyExplanations();
//
//		for (Explanation<OWLAxiom> explanation : explanations) {
//			final ExplanationTable t = new ExplanationTable(explanation, selectedAxioms);
//			ORESession.getRepairManager().addListener(t);
////			t.setCaption(((OWLSubClassOfAxiom) explanation.getEntailment()).getSubClass().toString());
//			explanationsPanel.addComponent(t);
//			t.addValueChangeListener(new Property.ValueChangeListener() {
//
//				{
//					table2Listener.put(t, this);
//				}
//
//				@Override
//				public void valueChange(ValueChangeEvent event) {
//					selectedAxioms.removeAll(t.getExplanation().getAxioms());
//					selectedAxioms.addAll((Collection<? extends OWLAxiom>) event.getProperty().getValue());
//					onAxiomSelectionChanged();
//				}
//			});
//			tables.add(t);
//		}
//
//	}
//	
	private void onAxiomSelectionChanged(){
//		propagateAxiomSelection();
		//we have to remove here all listeners because
		for(Entry<SPARQLBasedExplanationTable, Property.ValueChangeListener> e : table2Listener.entrySet()){
			e.getKey().removeValueChangeListener(e.getValue());
		}
		ORESession.getRepairManager().clearRepairPlan();
		ORESession.getRepairManager().addAxiomsToRemove(selectedAxioms);
		for(Entry<SPARQLBasedExplanationTable, Property.ValueChangeListener> e : table2Listener.entrySet()){
			e.getKey().addValueChangeListener(e.getValue());
		}
	}
	
	private void onDumpSPARUL(){
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>(ORESession.getRepairManager().getRepairPlan());
		if(!changes.isEmpty()){
			final SPARULDialog window = new SPARULDialog(changes);
			window.addCloseListener(new CloseListener() {
				
				@Override
				public void windowClose(CloseEvent e) {
					UI.getCurrent().removeWindow(window);
				}
			});
			UI.getCurrent().addWindow(window);
			window.focus();
		}
	}
	
	private void propagateAxiomSelection(){
//		selectedAxioms.clear();
//		for(ExplanationTable t : tables){
//			selectedAxioms.addAll((Collection<? extends OWLAxiom>) t.getValue());
//		}
		for(SPARQLBasedExplanationTable t : tables){
			t.removeValueChangeListener(table2Listener.get(t));
			t.selectAxioms(selectedAxioms);
			t.addValueChangeListener(table2Listener.get(t));
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
	
	private class ModifiableListField extends VerticalLayout{
		
		public ModifiableListField() {
//			setSpacing(true);
			setSizeFull();
//			addStyleName("naming-view");
			
			
			uriList = new ListSelect();
	        uriList.setRows(0);
	        uriList.setMultiSelect(true);
	        uriList.setImmediate(true); 
	        uriList.setSizeFull();
	        
	        Button addButton = new Button("Add");
	        addButton.setDescription("Add a new namespace URI to the list below.");
	        addButton.addStyleName("small");
	        addButton.addClickListener(new Button.ClickListener() {
				
				@Override
				public void buttonClick(ClickEvent event) {
					final Window w = new Window("Enter namespace URI");
					w.setWidth("400px");
					w.setHeight(null);
					w.addCloseListener(new CloseListener() {
						@Override
						public void windowClose(CloseEvent e) {
							UI.getCurrent().removeWindow(w);
						}
					});
					final TextField uriInputField = new TextField();
					String urlRegex = "(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?";
					Validator uriValidator = new RegexpValidator(
							urlRegex, "Not a valid URI.");
					uriInputField.addValidator(uriValidator);
					
					Button okButton = new Button("Ok");
			        okButton.addClickListener(new Button.ClickListener() {
						
						@Override
						public void buttonClick(ClickEvent event) {
							String uri = (String) uriInputField.getValue();
							if(uri != null){
								uriList.addItem(uri);
								UI.getCurrent().removeWindow(w);
							}
						}
					});
			        Button cancelButton = new Button("Cancel");
			        cancelButton.addClickListener(new Button.ClickListener() {
						
						@Override
						public void buttonClick(ClickEvent event) {
							UI.getCurrent().removeWindow(w);
						}
					});
			        HorizontalLayout buttonLayout = new HorizontalLayout();
					buttonLayout.setWidth("100%");
					buttonLayout.setHeight(null);
					buttonLayout.addComponent(okButton);
					buttonLayout.addComponent(cancelButton);
					
			        FormLayout form = new FormLayout();
			        form.addComponent(uriInputField);
			        form.addComponent(buttonLayout);
			        
			        w.setContent(form);
			        
			        UI.getCurrent().addWindow(w);
				}
			});
	        addButton.setSizeUndefined();
	        addButton.setWidth("100%");
	        Button removeButton = new Button("Remove");
	        removeButton.setDescription("Remove the selected resources from the list below.");
	        removeButton.addStyleName("small");
	        removeButton.addClickListener(new Button.ClickListener() {
				
				@Override
				public void buttonClick(ClickEvent event) {
					if(uriList.getValue() != null){
						Set<Object> items = (Set<Object>) uriList.getValue();
						for(Object item : items){
							uriList.removeItem(item);
						}
					}
				}
			});
	        removeButton.setSizeUndefined();
	        removeButton.setWidth("100%");
			
			HorizontalLayout buttonLayout = new HorizontalLayout();
//			buttonLayout.addComponent(new Label("Namespaces:", ContentMode.HTML));
			buttonLayout.setWidth("100%");
			buttonLayout.addComponent(addButton);
			buttonLayout.addComponent(removeButton);
//			buttonLayout.addStyleName("toolbar");
			buttonLayout.setComponentAlignment(removeButton, Alignment.BOTTOM_RIGHT);
			buttonLayout.setExpandRatio(addButton, 1f);
			buttonLayout.setExpandRatio(removeButton, 1f);
			
			addComponent(buttonLayout);
			setComponentAlignment(buttonLayout, Alignment.BOTTOM_RIGHT);
			addComponent(uriList);
			
			setExpandRatio(uriList, 1f);
			
		}
		
	}

	/* (non-Javadoc)
	 * @see com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener.ViewChangeEvent)
	 */
	@Override
	public void enter(ViewChangeEvent event) {
	}

	/* (non-Javadoc)
	 * @see com.vaadin.data.Property.ValueChangeListener#valueChange(com.vaadin.data.Property.ValueChangeEvent)
	 */
	@Override
	public void valueChange(ValueChangeEvent event) {
		if(event.getProperty() == limitExplanationCountCheckbox){
			boolean limitExplanations = (Boolean)event.getProperty().getValue();
			limitSpinner.setEnabled(limitExplanations);
			if(limitExplanations){
				ORESession.getSPARQLExplanationManager().setExplanationLimit((Integer)limitSpinner.getValue());
			} else {
				ORESession.getSPARQLExplanationManager().setExplanationLimit(-1);
				limitSpinner.setEnabled(false);
			}
		} else if(event.getProperty() == limitSpinner){
			ORESession.getSPARQLExplanationManager().setExplanationLimit((Integer)event.getProperty().getValue());
			
		}
	}
	

}
