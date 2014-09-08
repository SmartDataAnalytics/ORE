package org.aksw.ore.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.mole.ore.sparql.SPARULTranslator;
import org.aksw.ore.ORESession;
import org.aksw.ore.component.EnrichmentProgressDialog;
import org.aksw.ore.component.EvaluatedAxiomsTable;
import org.aksw.ore.component.WhitePanel;
import org.aksw.ore.exception.OREException;
import org.aksw.ore.manager.EnrichmentManager;
import org.aksw.ore.model.ResourceType;
import org.aksw.ore.model.SPARQLEndpointKnowledgebase;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Slider;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

public class EnrichmentView extends HorizontalSplitPanel implements View, Refreshable{
	
	private TextField resourceURIField;
	private ResourceTypeField resourceTypeField;
	private CheckBox useInferenceBox;
	private IntStepper maxExecutionTimeSpinner;
	private IntStepper maxNrOfReturnedAxiomsSpinner;
	private Slider thresholdSlider;
	
	private Table axiomTypesTable;
	private BeanItemContainer<AxiomType<OWLAxiom>> axiomTypesContainer;
	private AxiomTypesField axiomTypesField;
	
	private Button startButton;
	
	private VerticalLayout axiomsPanel;
	
	private List<AxiomType<OWLAxiom>> pendingAxiomTypes;
	
	private Set<EvaluatedAxiomsTable> tables;
	private Button addToKbButton;
	private Button dumpSPARULButton;

	public EnrichmentView() {
		initUI();
	}
	
	private void initUI(){
		addStyleName("dashboard-view");
		setSizeFull();
		setSplitPosition(25);
		
		Component leftSide = createLeftSide();
		addComponent(new WhitePanel(leftSide));
		
		VerticalLayout rightSide = new VerticalLayout();
		rightSide.setSizeFull();
		rightSide.setSpacing(true);
		rightSide.setCaption("Learned axioms");
		addComponent(new WhitePanel(rightSide));
		
		Component panel = createAxiomsPanel();
		rightSide.addComponent(panel);
		rightSide.setExpandRatio(panel, 1f);
		
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth(null);
		rightSide.addComponent(buttons);
		rightSide.setComponentAlignment(buttons, Alignment.MIDDLE_RIGHT);
		
		addToKbButton = new Button("Add");
		addToKbButton.setHeight(null);
		addToKbButton.setImmediate(true);
		addToKbButton.setDescription("Add the selected axioms temporarily to the knowledge base.(visible in 'Knowledge Base' view)");
		addToKbButton.addClickListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				OWLOntology ontology = ((SPARQLEndpointKnowledgebase)ORESession.getKnowledgebaseManager().getKnowledgebase()).getBaseOntology();
				List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
				for(EvaluatedAxiomsTable table : tables){
					Set<Object> selectedObjects = table.getSelectedObjects();
					for(Object o : selectedObjects){
						changes.add(new AddAxiom(ontology, OWLAPIConverter.getOWLAPIAxiom(((EvaluatedAxiom)o).getAxiom())));
					}
				}
				if(!changes.isEmpty()){
					ORESession.getRepairManager().addToRepairPlan(changes);
					ORESession.getKnowledgebaseManager().addChanges(changes);
					Notification.show("Applied changes.", Type.TRAY_NOTIFICATION);
				}
			}
		});
		buttons.addComponent(addToKbButton);
		buttons.setComponentAlignment(addToKbButton, Alignment.MIDDLE_RIGHT);
		
		dumpSPARULButton = new Button("Dump as SPARUL");
		dumpSPARULButton.setHeight(null);
		dumpSPARULButton.setImmediate(true);
		dumpSPARULButton.setDescription("Export the selected axioms as SPARQL Update statements.");
		dumpSPARULButton.addClickListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onDumpSPARUL();
			}
		});
		buttons.addComponent(dumpSPARULButton);
		buttons.setComponentAlignment(dumpSPARULButton, Alignment.MIDDLE_RIGHT);
		
//		addToKbButton.setEnabled(false);
//		dumpSPARULButton.setEnabled(false);
		
		resourceURIField.focus();
		
		reset();
//		resourceURIField.setValue("http://dbpedia.org/ontology/birthPlace");
//		resourceTypeField.setResourceType(ResourceType.OBJECT_PROPERTY);
	}
	
	private Component createAxiomsPanel(){
		axiomsPanel = new VerticalLayout();
		axiomsPanel.setSizeFull();
		axiomsPanel.setMargin(true);
		axiomsPanel.setSpacing(true);
		axiomsPanel.addStyleName("enrichment-axioms-panel");
		axiomsPanel.setHeight(null);
		
		Panel panel = new Panel(axiomsPanel);
		panel.setSizeFull();
		return panel;
	}
	
	private VerticalLayout createLeftSide(){
		VerticalLayout leftSide = new VerticalLayout();
		leftSide.setSizeFull();
		leftSide.setCaption("Options");
		leftSide.setSpacing(true);
		leftSide.setMargin(new MarginInfo(false, false, true, false));
		
		Component configForm = createConfigForm();
		Panel p = new Panel();
		p.setSizeFull();
		p.setContent(configForm);
		leftSide.addComponent(p);
		leftSide.setExpandRatio(p, 1f);
		
		startButton = new Button("Start");
		startButton.setDescription("Click to start the learning process.");
//		startButton.setDisableOnClick(true);
		startButton.addClickListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onLearning();
			}
		});
		
		leftSide.addComponent(startButton);
		leftSide.setComponentAlignment(startButton, Alignment.MIDDLE_CENTER);
		
		return leftSide;
	}
	
	private Component createConfigForm(){
		VerticalLayout form = new VerticalLayout();
		form.setWidth("100%");
		form.setHeight(null);
		form.setSizeFull();
		form.setSpacing(true);
		form.addStyleName("enrichment-options");
		
		resourceURIField = new TextField();
		resourceURIField.setInputPrompt("Enter resource URI");
		resourceURIField.setWidth("100%");
		resourceURIField.setCaption("Resource URI");
		resourceURIField.setRequired(true);
		form.addComponent(resourceURIField);
		
		resourceTypeField = new ResourceTypeField();
		form.addComponent(resourceTypeField);
		
		useInferenceBox = new CheckBox();
		useInferenceBox.setCaption("Inference");
		form.addComponent(useInferenceBox);
		
		maxExecutionTimeSpinner = new IntStepper();
		maxExecutionTimeSpinner.setValue(10);
		maxExecutionTimeSpinner.setStepAmount(1);
		maxExecutionTimeSpinner.setWidth("100%");
		maxExecutionTimeSpinner.setCaption("Max. execution time");
		maxExecutionTimeSpinner.setDescription("The maximum runtime in seconds for each particlar axiom type.");
		form.addComponent(maxExecutionTimeSpinner);
		
		maxNrOfReturnedAxiomsSpinner = new IntStepper();
		maxNrOfReturnedAxiomsSpinner.setValue(10);
		maxNrOfReturnedAxiomsSpinner.setStepAmount(1);
		maxNrOfReturnedAxiomsSpinner.setWidth("100%");
		maxNrOfReturnedAxiomsSpinner.setCaption("Max. returned axioms");
		maxNrOfReturnedAxiomsSpinner.setDescription("The maximum number of shown axioms per "
				+ "axiom type with a confidence score above the chosen threshold below.");
		form.addComponent(maxNrOfReturnedAxiomsSpinner);
		
		thresholdSlider = new Slider(1, 100);
		thresholdSlider.setWidth("100%");
		thresholdSlider.setImmediate(true);
		thresholdSlider.setCaption("Threshold");
		thresholdSlider.setDescription("The minimum confidence score for the learned axioms.");
		form.addComponent(thresholdSlider);
		
		axiomTypesField = new AxiomTypesField();
		axiomTypesField.setSizeFull();
		form.addComponent(axiomTypesField);
		form.setExpandRatio(axiomTypesField, 1f);
		
		return form;
	}
	
	private void onLearning(){
		axiomsPanel.removeAllComponents();
		tables = new HashSet<EvaluatedAxiomsTable>();
		
		final EnrichmentManager man = ORESession.getEnrichmentManager();
		
		final String resourceURI = (String) resourceURIField.getValue();
		
		pendingAxiomTypes = new ArrayList<AxiomType<OWLAxiom>>(axiomTypesField.getSelectedAxiomsTypes());
		
		boolean useInference = (Boolean) useInferenceBox.getValue();
		man.setUseInference(useInference);
		
		int maxExecutionTimeInSeconds = (Integer) maxExecutionTimeSpinner.getValue();
		man.setMaxExecutionTimeInSeconds(maxExecutionTimeInSeconds);
		
		int maxNrOfReturnedAxioms = (Integer) maxNrOfReturnedAxiomsSpinner.getValue();
		man.setMaxNrOfReturnedAxioms(maxNrOfReturnedAxioms);
		
		double threshold = (Double) thresholdSlider.getValue()/100;
		man.setThreshold(threshold);
		
		ResourceType resourceType = getSelectedResourceType();
		man.setResourceType(resourceType);
		
		if(resourceType == ResourceType.UNKNOWN){
			try {
				resourceType = man.getResourceType(resourceURI);
				Label label = new Label("Entity <b>" + resourceURI + "</b> is processed as " + resourceType.toString() + ".", ContentMode.HTML);
				label.addStyleName("entity-autodetect-info-label");
				pendingAxiomTypes.retainAll(man.getAxiomTypes(resourceType));
			} catch (OREException e) {
				e.printStackTrace();
			}
		}
		
		EnrichmentProgressDialog progressDialog = new EnrichmentProgressDialog(pendingAxiomTypes);
		getUI().addWindow(progressDialog);
		
		for(final AxiomType<OWLAxiom> axiomType : pendingAxiomTypes){
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						final List<EvaluatedAxiom> learnedAxioms = ORESession.getEnrichmentManager().getEvaluatedAxioms2(resourceURI, axiomType);
						UI.getCurrent().access(new Runnable() {
							
							@Override
							public void run() {
								showTable(axiomType, learnedAxioms);
							}
						});
					} catch (OREException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
	
	private void onDumpSPARUL(){
		try {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = man.createOntology();
			List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
			SPARULTranslator translator = new SPARULTranslator(man, ontology, false);
			for(EvaluatedAxiomsTable table : tables){
				Set<Object> selectedObjects = table.getSelectedObjects();
				for(Object o : selectedObjects){
					changes.add(new AddAxiom(ontology, OWLAPIConverter.getOWLAPIAxiom(((EvaluatedAxiom)o).getAxiom())));
				}
			}
			if(!changes.isEmpty()){
				VerticalLayout content = new VerticalLayout();
				String sparulString = translator.translate(changes, AddAxiom.class);
				content.addComponent(new Label(sparulString, ContentMode.PREFORMATTED));
				final Window window = new Window("SPARQL Update statements", content);
				window.setWidth("1000px");
				window.setHeight("400px");
				window.center();
				window.addCloseListener(new CloseListener() {
					
					@Override
					public void windowClose(CloseEvent e) {
						getUI().removeWindow(window);
					}
				});
				getUI().addWindow(window);
				window.focus();
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}
	
	public void showTable(AxiomType<OWLAxiom> axiomType, List<EvaluatedAxiom> axioms){
		if(!axioms.isEmpty()){
			try {
				EvaluatedAxiomsTable table = new EvaluatedAxiomsTable(axiomType, axioms);
				table.setWidth("100%");
				String axiomName = axiomType.getName();
				if(axiomType.equals(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY)){
					axiomName = "Irreflexive Object Property";
				}
				axiomName = splitCamelCase(axiomName);
				table.setCaption(axiomName + " Axioms");
				tables.add(table);
				WhitePanel c = new WhitePanel(table);
				c.setHeight(null);
				axiomsPanel.addComponent(table);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private String splitCamelCase(String s){
		String split = s.replaceAll("([A-Z][a-z]+)", " $1") // Words beginning with UC
        .replaceAll("([A-Z][A-Z]+)", " $1") // "Words" of only UC
        .replaceAll("([^A-Za-z ]+)", " $1") // "Words" of non-letters
        .trim();
		return split;
	}
	
	private ResourceType getSelectedResourceType(){
		return resourceTypeField.getResourceType();
	}
	
	private void onResourceTypeChanged(){
		ResourceType type = getSelectedResourceType();
		axiomTypesField.updateVisibleAxiomTypes(ORESession.getEnrichmentManager().getAxiomTypes(type));
	}
	
	/* (non-Javadoc)
	 * @see com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener.ViewChangeEvent)
	 */
	@Override
	public void enter(ViewChangeEvent event) {
	}
	
	public void reset(){
		axiomsPanel.removeAllComponents();
		resourceURIField.setValue("");
		resourceTypeField.setResourceType(ResourceType.UNKNOWN);
		resourceTypeField.setResourceType(ResourceType.CLASS);
		startButton.setEnabled(false);
		try {
			thresholdSlider.setValue(70d);
		} catch (ValueOutOfBoundsException e) {
			e.printStackTrace();
		}
	}
	
	private class AxiomTypesField extends VerticalLayout{
		
		private Set<AxiomType<OWLAxiom>> selectedAxiomsTypes = new HashSet<AxiomType<OWLAxiom>>();
		private Collection<AxiomType<OWLAxiom>> visibleAxiomTypes;
		
		public AxiomTypesField() {
			setCaption("Axiom types");
			setDescription("Choose the axiom types for which axiom suggestions will be generated.");
			setSizeFull();
			//(de)select all checkbox
			CheckBox allCheckBox = new CheckBox("All");
			allCheckBox.addStyleName("select-all-axiomtypes-checkbox");
			allCheckBox.setImmediate(true);
			allCheckBox.addValueChangeListener(new Property.ValueChangeListener() {
				@Override
				public void valueChange(Property.ValueChangeEvent event) {
					if((Boolean) event.getProperty().getValue()){
						selectedAxiomsTypes.addAll(visibleAxiomTypes);
					} else {
						selectedAxiomsTypes.clear();
					}
					startButton.setEnabled(!selectedAxiomsTypes.isEmpty());
					axiomTypesTable.refreshRowCache();
				}
			});
			addComponent(allCheckBox);
			//table
			axiomTypesTable = new Table();
			axiomTypesTable.setSizeFull();
			axiomTypesTable.setImmediate(true);
//			axiomTypesTable.setPageLength(0);
			axiomTypesTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
			axiomTypesContainer = new BeanItemContainer<AxiomType<OWLAxiom>>(AxiomType.class);
			this.visibleAxiomTypes = ORESession.getEnrichmentManager().getAxiomTypes(ResourceType.UNKNOWN);
			axiomTypesContainer.addAll(visibleAxiomTypes);
			axiomTypesTable.setContainerDataSource(axiomTypesContainer);
			axiomTypesTable.addGeneratedColumn("selected", new ColumnGenerator() {
				
				@SuppressWarnings("unchecked")
				@Override
				public Object generateCell(Table source, final Object itemId, Object columnId) {
					CheckBox box = new CheckBox();
					box.setValue(selectedAxiomsTypes.contains((AxiomType<OWLAxiom>) itemId));
					box.setImmediate(true);
					box.addValueChangeListener(new Property.ValueChangeListener() {
						@Override
						public void valueChange(Property.ValueChangeEvent event) {
							if((Boolean) event.getProperty().getValue()){
								selectedAxiomsTypes.add((AxiomType<OWLAxiom>) itemId);
							} else {
								selectedAxiomsTypes.remove((AxiomType<OWLAxiom>) itemId);
							}
							startButton.setEnabled(!selectedAxiomsTypes.isEmpty());
						}
					});
					return box;
				}
			});
			axiomTypesTable.setVisibleColumns(new String[] {"selected", "name"});
			addComponent(axiomTypesTable);
			setExpandRatio(axiomTypesTable, 1f);
			
		}
		
		public void updateVisibleAxiomTypes(Collection<AxiomType<OWLAxiom>> visibleAxiomTypes){
			this.visibleAxiomTypes = visibleAxiomTypes;
			selectedAxiomsTypes.retainAll(visibleAxiomTypes);
			axiomTypesContainer.removeAllItems();
			axiomTypesContainer.addAll(visibleAxiomTypes);
		}
		
		public Set<AxiomType<OWLAxiom>> getSelectedAxiomsTypes() {
			return selectedAxiomsTypes;
		}
	}
	
	private class ResourceTypeField extends VerticalLayout{
		
		private CheckBox autoDetectBox;
		private OptionGroup resourceTypeGroup;

		public ResourceTypeField() {
			setCaption("Resource type");
			addStyleName("resource-type");
			setSizeFull();
			setHeight(null);
			
			autoDetectBox = new CheckBox();
			autoDetectBox.setValue(true);
			autoDetectBox.setCaption("Detect automatically");
			autoDetectBox.setImmediate(true);
			autoDetectBox.addValueChangeListener(new ValueChangeListener() {
				@Override
				public void valueChange(ValueChangeEvent event) {
					onResourceTypeChanged();
					if((Boolean) event.getProperty().getValue()){
						resourceTypeGroup.setEnabled(false);
					} else {
						resourceTypeGroup.setEnabled(true);
					}
				}
			});
			addComponent(autoDetectBox);
			
			resourceTypeGroup = new OptionGroup();
			resourceTypeGroup.setImmediate(true);
			resourceTypeGroup.addItem(ResourceType.CLASS);
			resourceTypeGroup.addItem(ResourceType.OBJECT_PROPERTY);
			resourceTypeGroup.addItem(ResourceType.DATA_PROPERTY);
			resourceTypeGroup.setValue(ResourceType.CLASS);
			resourceTypeGroup.addValueChangeListener(new ValueChangeListener() {
				@Override
				public void valueChange(
						com.vaadin.data.Property.ValueChangeEvent event) {
					onResourceTypeChanged();
				}
			});
			addComponent(resourceTypeGroup);
		}
		
		public ResourceType getResourceType(){
			if(autoDetectBox.getValue()){
				return ResourceType.UNKNOWN;
			} else {
				return (ResourceType) resourceTypeGroup.getValue();
			}
		}
		
		public void setResourceType(ResourceType resourceType){
			if(resourceType.equals(ResourceType.UNKNOWN)){
				resourceTypeGroup.setEnabled(false);
				autoDetectBox.setValue(true);
			} else {
				resourceTypeGroup.setEnabled(true);
				resourceTypeGroup.setValue(resourceType);
				autoDetectBox.setValue(false);
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.view.Refreshable#refreshRendering()
	 */
	@Override
	public void refreshRendering() {
		for (EvaluatedAxiomsTable evaluatedAxiomsTable : tables) {
			evaluatedAxiomsTable.refreshRowCache();
		}
	}
}
