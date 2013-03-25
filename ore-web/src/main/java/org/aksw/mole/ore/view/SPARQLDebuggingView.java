package org.aksw.mole.ore.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.mole.ore.ExplanationManager2;
import org.aksw.mole.ore.ExplanationManagerListener;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.explanation.api.ExplanationType;
import org.aksw.mole.ore.model.SPARQLProgress;
import org.aksw.mole.ore.sparql.IncrementalInconsistencyFinder;
import org.aksw.mole.ore.sparql.SPARULTranslator;
import org.aksw.mole.ore.widget.ExplanationOptionsPanel;
import org.aksw.mole.ore.widget.ExplanationTable;
import org.aksw.mole.ore.widget.ExplanationsPanel;
import org.aksw.mole.ore.widget.RepairPlanTable;
import org.aksw.mole.ore.widget.SPARQLDebuggingProgressPanel;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.vaadin.addon.customfield.FieldWrapper;
import org.vaadin.appfoundation.view.AbstractView;
import org.vaadin.sasha.portallayout.PortalLayout;

import com.github.wolfie.refresher.Refresher;
import com.github.wolfie.refresher.Refresher.RefreshListener;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Form;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

public class SPARQLDebuggingView extends AbstractView<HorizontalSplitPanel> implements ExplanationManagerListener{
	
	private ExplanationOptionsPanel optionsPanel;
	
	private HorizontalSplitPanel mainPanel;
	
	private RepairPlanTable repairPlanTable;
	private ExplanationsPanel explanationsPanel;
	private SPARQLDebuggingProgressPanel progressPanel;
	
	private Button startButton;
	private Button stopButton;
	private CheckBox useLinkedDataCheckBox;
	private ListSelect uriSelect;
	
	private int currentLimit = 0;
	private ExplanationType currentExplanationType = ExplanationType.REGULAR;
	private Set<ExplanationTable> tables = new HashSet<ExplanationTable>();
	private Set<OWLAxiom> selectedAxioms = new HashSet<OWLAxiom>();
	private Map<ExplanationTable, Property.ValueChangeListener> table2Listener = new HashMap<ExplanationTable, Property.ValueChangeListener>();
	
	private IncrementalInconsistencyFinder incFinder;
	private Refresher refresher;
	
	public SPARQLDebuggingView() {
		super(new HorizontalSplitPanel());
		initUI();
	}

	public void activated(Object... params) {
		UserSession.getRepairManager().addListener(repairPlanTable);
		UserSession.getExplanationManager().addListener(this);
		rebuiltData();
	}

	public void deactivated(Object... params) {
		UserSession.getRepairManager().removeListener(repairPlanTable);
		UserSession.getExplanationManager().removeListener(this);
	}
	
	private void initUI(){
		mainPanel = getContent();
		mainPanel.setSizeFull();
		mainPanel.setSplitPosition(25);
		
		Component leftSide = createLeftSide();
		mainPanel.addComponent(leftSide);
		
		Component rightSide = createRightSide();
		mainPanel.addComponent(rightSide);
		
		refresher = new Refresher();
        refresher.addListener(new RefreshListener() {
			
			@Override
			public void refresh(Refresher source) {
				if(!incFinder.isConsistent()){
					source.setEnabled(false);
					showExplanations();
					startButton.setEnabled(true);
					stopButton.setEnabled(false);
					getApplication().getMainWindow().addStyleName("default-cursor");
					getApplication().getMainWindow().removeStyleName("waiting-cursor");
				}
				updateProgress();
			}
		});
        refresher.setRefreshInterval(1000L);
        refresher.setEnabled(false);
		
//		reset();
	}
	
	@Override
	public void attach() {
		super.attach();
		getWindow().addComponent(refresher);
	}
	
	@Override
	public void detach() {
		super.detach();
		getWindow().removeComponent(refresher);
	}
	
	private Component createLeftSide(){
		PortalLayout portal = new PortalLayout();
		portal.setSizeFull();
		
		VerticalLayout leftSide = new VerticalLayout();
		leftSide.setSizeFull();
		leftSide.setSpacing(true);
		leftSide.setCaption("Options");
		
		Panel p = new Panel();
		p.setSizeFull();
		leftSide.addComponent(p);
		
		Component configForm = createConfigForm();
		p.addComponent(configForm);
		
		Component progressPanel = createProgressPanel();
		p.addComponent(progressPanel);
		
		portal.addComponent(leftSide);
		portal.setCollapsible(leftSide, false);
		portal.setClosable(leftSide, false);
		
		return portal;
	}
	
	private Component createConfigForm(){
		Form form = new Form();
		form.getLayout().setWidth("100%");
		form.setWidth("100%");
		((FormLayout)form.getLayout()).setSpacing(true);
		form.addStyleName("sparql-debug-options-form");
		
		final ModifiableListField uriList = new ModifiableListField();
		
		useLinkedDataCheckBox = new CheckBox();
		useLinkedDataCheckBox.setValue(true);
		useLinkedDataCheckBox.setCaption("Use Linked Data");
		useLinkedDataCheckBox.setImmediate(true);
		useLinkedDataCheckBox.addListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				uriList.setEnabled((Boolean) event.getProperty().getValue());
			}
		});
		form.addField("linked data", useLinkedDataCheckBox);
		form.addField("linked data uri", uriList);
		
		
		startButton = new Button("Start");
		startButton.setDisableOnClick(true);
		startButton.addListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onSearchingInconsistency();
			}
		});
//		form.getFooter().addComponent(startButton);
		
		stopButton = new Button("Stop");
		stopButton.addListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onStopSearchingInconsistency();
			}
		});
		HorizontalLayout buttonBar = new HorizontalLayout();
		buttonBar.setWidth(null);
		buttonBar.addComponent(startButton); 
		buttonBar.addComponent(stopButton);
		buttonBar.setComponentAlignment(startButton, Alignment.MIDDLE_CENTER);
		form.getFooter().addComponent(buttonBar);
		((HorizontalLayout)form.getFooter()).setWidth("100%");
		((HorizontalLayout)form.getFooter()).setComponentAlignment(buttonBar, Alignment.MIDDLE_CENTER);
		
		return form;
	}
	
	private Component createProgressPanel(){
		progressPanel = new SPARQLDebuggingProgressPanel();
		progressPanel.setHeight("100%");
		progressPanel.setSpacing(true);
		progressPanel.addStyleName("sparql-debug-progress-form");
		
		return progressPanel;
	}
	
	private Component createRightSide(){
		VerticalSplitPanel rightSide = new VerticalSplitPanel();
		rightSide.setSizeFull();
		
		//show explanations in the top of the right side
		Component explanationsPanel = createExplanationsPanel();
		rightSide.addComponent(explanationsPanel);
		//show repair plan  in the bottom of the right side
		HorizontalLayout bottomRightSide = new HorizontalLayout();
		bottomRightSide.setSizeFull();
		Component repairPlanPanel = createRepairPlanPanel();
		bottomRightSide.addComponent(repairPlanPanel);
		rightSide.addComponent(bottomRightSide);
		rightSide.setSplitPosition(75);
		
		return rightSide;
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
		PortalLayout portal = new PortalLayout();
		portal.setSizeFull();
		
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setCaption("Repair Plan");
		wrapper.setSizeFull();
		portal.addComponent(wrapper);
		portal.setCollapsible(wrapper, false);
		portal.setClosable(wrapper, false);
		
		repairPlanTable = new RepairPlanTable();
		wrapper.addComponent(repairPlanTable);
		wrapper.setExpandRatio(repairPlanTable, 1.0f);
		
		Button executeRepairButton = new Button("Dump as SPARUL");
		executeRepairButton.setHeight(null);
		executeRepairButton.addListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onDumpSPARUL();
//				UserSession.getRepairManager().execute();
				rebuiltData();
			}
		});
		wrapper.addComponent(executeRepairButton);
		wrapper.setComponentAlignment(executeRepairButton, Alignment.MIDDLE_RIGHT);
		
		return portal;
		
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
		startButton.setEnabled(true);
		stopButton.setEnabled(false);
		clearExplanations();
	}
	
	private void clearExplanations(){
		for(ExplanationTable t : tables){
			explanationsPanel.removeComponent(t);
		}
		explanationsPanel.requestRepaint();
	}
	
	private void onSearchingInconsistency(){
		getApplication().getMainWindow().addStyleName("waiting-cursor");
		startButton.setEnabled(false);
		stopButton.setEnabled(true);
		refresher.setEnabled(true);
		incFinder = UserSession.getIncrementalInconsistencyFinder();
		incFinder.setUseLinkedData((Boolean) useLinkedDataCheckBox.getValue());
		Set<String> namespaces = new HashSet<String>();
		for(Object item : uriSelect.getItemIds()){
			namespaces.add((String)item);
		}
		incFinder.setLinkedDataNamespaces(namespaces);
		incFinder.runAsync();
	}
	
	public void onStopSearchingInconsistency() {
		incFinder.stop();
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
	
	private void updateProgress(){
		OWLOntology ontology = incFinder.getOntology();
		SPARQLProgress progress = new SPARQLProgress();
		
		progress.setDisjointClassMax(incFinder.getDisjointWithCount());
		progress.setDisjointClassValue(ontology.getAxiomCount(AxiomType.DISJOINT_CLASSES));
		progress.setSubClassMax(incFinder.getSubClassOfCount());
		progress.setSubClassValue(ontology.getAxiomCount(AxiomType.SUBCLASS_OF));
		progress.setEquivalentClassMax(incFinder.getEquivalentClassCount());
		progress.setEquivalentClassValue(ontology.getAxiomCount(AxiomType.EQUIVALENT_CLASSES));
		
		progress.setSubPropertyMax(incFinder.getSubPropertyOfCount());
		progress.setSubPropertyValue(ontology.getAxiomCount(AxiomType.SUB_OBJECT_PROPERTY) + ontology.getAxiomCount(AxiomType.SUB_DATA_PROPERTY));
		progress.setEquivalentPropertyMax(incFinder.getEquivalentPropertyCount());
		progress.setEquivalentPropertyValue(ontology.getAxiomCount(AxiomType.EQUIVALENT_OBJECT_PROPERTIES) + ontology.getAxiomCount(AxiomType.EQUIVALENT_DATA_PROPERTIES));
		progress.setPropertyDomainMax(incFinder.getDomainCount());
		progress.setPropertyDomainValue(ontology.getAxiomCount(AxiomType.OBJECT_PROPERTY_DOMAIN) + ontology.getAxiomCount(AxiomType.DATA_PROPERTY_DOMAIN));
		progress.setPropertyRangeMax(incFinder.getRangeCount());
		progress.setPropertyRangeValue(ontology.getAxiomCount(AxiomType.OBJECT_PROPERTY_RANGE) + ontology.getAxiomCount(AxiomType.DATA_PROPERTY_RANGE));
		
		progress.setFunctionalPropertyMax(incFinder.getFunctionalCount());
		progress.setFunctionalPropertyValue(ontology.getAxiomCount(AxiomType.FUNCTIONAL_OBJECT_PROPERTY) + ontology.getAxiomCount(AxiomType.FUNCTIONAL_DATA_PROPERTY));
		progress.setTransitivePropertyMax(incFinder.getTransitiveCount());
		progress.setTransitivePropertyValue(ontology.getAxiomCount(AxiomType.TRANSITIVE_OBJECT_PROPERTY));
		
		progressPanel.update(progress);
		
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
	
	private void onDumpSPARUL(){
		try {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = man.createOntology();
			SPARULTranslator translator = new SPARULTranslator(man, ontology, false);
			List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>(UserSession.getRepairManager().getRepairPlan());
			if(!changes.isEmpty()){
				String sparulString = translator.translate(changes);
				final Window window = new Window();
				window.setWidth("1000px");
				window.setHeight("400px");
				window.center();
				window.addComponent(new Label(sparulString, Label.CONTENT_PREFORMATTED));
				window.addListener(new CloseListener() {
					
					@Override
					public void windowClose(CloseEvent e) {
						getApplication().getMainWindow().removeWindow(window);
					}
				});
				getApplication().getMainWindow().addWindow(window);
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
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
	
	private class ModifiableListField extends FieldWrapper<String>{
		
		public ModifiableListField() {
			super(new ListSelect(), null, String.class);
			
			uriSelect = new ListSelect();
			uriSelect.setHeight("100px");
			uriSelect.setWidth("80%");
	        uriSelect.setRows(0);
	        uriSelect.setMultiSelect(true);
	        uriSelect.setImmediate(true); 
	        
	        Button addButton = new Button();
	        addButton.addStyleName("");
	        addButton.setIcon(new ThemeResource("images/add_icon.png"));
	        addButton.addListener(new Button.ClickListener() {
				
				@Override
				public void buttonClick(ClickEvent event) {
					final Window w = new Window("Enter namespace URI");
					w.setWidth("400px");
					w.setHeight(null);
					w.addListener(new CloseListener() {
						@Override
						public void windowClose(CloseEvent e) {
							getApplication().getMainWindow().removeWindow(w);
						}
					});
					final TextField uriInputField = new TextField();
					String urlRegex = "(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?";
					Validator uriValidator = new RegexpValidator(
							urlRegex, "Not a valid URI.");
					uriInputField.addValidator(uriValidator);
					
					Button okButton = new Button("Ok");
			        okButton.addListener(new Button.ClickListener() {
						
						@Override
						public void buttonClick(ClickEvent event) {
							String uri = (String) uriInputField.getValue();
							if(uri != null){
								uriSelect.addItem(uri);
								getApplication().getMainWindow().removeWindow(w);
							}
						}
					});
			        Button cancelButton = new Button("Cancel");
			        cancelButton.addListener(new Button.ClickListener() {
						
						@Override
						public void buttonClick(ClickEvent event) {
							getApplication().getMainWindow().removeWindow(w);
						}
					});
			        HorizontalLayout buttonLayout = new HorizontalLayout();
					buttonLayout.setWidth("100%");
					buttonLayout.setHeight(null);
					buttonLayout.addComponent(okButton);
					buttonLayout.addComponent(cancelButton);
					
			        Form form = new Form();
			        form.addField("uri", uriInputField);
			        form.getFooter().addComponent(buttonLayout);
			        
			        w.addComponent(form);
			        
			        getApplication().getMainWindow().addWindow(w);
				}
			});
	        addButton.setWidth(null);
	        Button removeButton = new Button();
	        removeButton.setIcon(new ThemeResource("images/remove_icon.png"));
	        removeButton.addListener(new Button.ClickListener() {
				
				@Override
				public void buttonClick(ClickEvent event) {
					if(uriSelect.getValue() != null){
						Set<Object> items = (Set<Object>) uriSelect.getValue();
						for(Object item : items){
							uriSelect.removeItem(item);
						}
					}
				}
			});
	        removeButton.setWidth(null);
			
			HorizontalLayout buttonLayout = new HorizontalLayout();
			buttonLayout.setWidth(null);
			buttonLayout.setHeight(null);
			buttonLayout.addComponent(addButton);
			buttonLayout.addComponent(removeButton);
			buttonLayout.addStyleName("toolbar");
			
			VerticalLayout root = new VerticalLayout();
			root.setSizeFull();
			root.addComponent(buttonLayout);
			root.addComponent(uriSelect);
			
			setCompositionRoot(root);
		}
		
	}
	

}
