package org.aksw.mole.ore.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.aksw.mole.ore.EnrichmentManager;
import org.aksw.mole.ore.OREApplication;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.exception.OREException;
import org.aksw.mole.ore.model.ResourceType;
import org.aksw.mole.ore.sparql.SPARULTranslator;
import org.aksw.mole.ore.widget.EvaluatedAxiomsTable;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.vaadin.addon.customfield.FieldWrapper;
import org.vaadin.appfoundation.view.AbstractView;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroup;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroupItemComponent;
import org.vaadin.jonatan.contexthelp.ContextHelp;
import org.vaadin.risto.stepper.IntStepper;
import org.vaadin.sasha.portallayout.PortalLayout;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
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
import com.vaadin.ui.Panel;
import com.vaadin.ui.Slider;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

import de.tobiasdemuth.vaadinworker.VaadinWorker;
import de.tobiasdemuth.vaadinworker.executorserviceprovider.ContextExecutorServiceProvider;
import de.tobiasdemuth.vaadinworker.ui.BackgroundExecutor;

public class EnrichmentView extends AbstractView<HorizontalSplitPanel>{
	
	private TextField resourceURIField;
	private CheckBox autoDetectBox;
	private FieldWrapper<ResourceType> resourceTypeField;
	private CheckBox useInferenceBox;
	private IntStepper maxExecutionTimeSpinner;
	private IntStepper maxNrOfReturnedAxiomsSpinner;
	private Slider thresholdSlider;
	
	private Table axiomTypesTable;
	private BeanItemContainer<AxiomType> axiomTypesContainer;
	private AxiomTypesField axiomTypesField;
	
	private Button startButton;
	private Button stopButton;
	
	private PortalLayout axiomsPortal;
	private Panel axiomsPanel;
	private VerticalLayout progressPanel;
	
	private List<AxiomType> pendingAxiomTypes;
	
	private Set<EvaluatedAxiomsTable> tables;

	public EnrichmentView() {
		super(new HorizontalSplitPanel());
		initUI();
	}
	
	private void initUI(){
		getContent().setSizeFull();
		getContent().setSplitPosition(25);
		
		Component leftPortal = createLeftPortal();
		getContent().addComponent(leftPortal);
		
		VerticalLayout rightSide = new VerticalLayout();
		rightSide.setSizeFull();
		rightSide.setSpacing(true);
		getContent().addComponent(rightSide);
		
		Component rightPortal = createRightPortal();
		rightSide.addComponent(rightPortal);
		rightSide.setExpandRatio(rightPortal, 1f);
		
		Button dumpSPARULButton = new Button("Dump as SPARUL");
		dumpSPARULButton.setHeight(null);
		dumpSPARULButton.setImmediate(true);
		dumpSPARULButton.addListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onDumpSPARUL();
			}
		});
		rightSide.addComponent(dumpSPARULButton);
		rightSide.setComponentAlignment(dumpSPARULButton, Alignment.MIDDLE_CENTER);
		
		resourceURIField.focus();
	}
	
	private Component createLeftPortal(){
		PortalLayout portal = new PortalLayout();
		portal.setSizeFull();
		VerticalLayout leftSide = createLeftSide();
		portal.addComponent(leftSide);
		portal.setCollapsible(leftSide, false);
		portal.setClosable(leftSide, false);
		
		return portal;
	}
	
	private Component createRightPortal(){
		axiomsPortal = new PortalLayout();
		axiomsPortal.setSizeFull();
		axiomsPortal.setMargin(true);
		
		axiomsPanel = new Panel();
		axiomsPanel.setSizeFull();
		axiomsPanel.setContent(axiomsPortal);
		
		VerticalLayout titleWrapper = new VerticalLayout();
		titleWrapper.setSizeFull();
		titleWrapper.setCaption("Result");
		titleWrapper.addComponent(axiomsPanel);
		
		PortalLayout portal = new PortalLayout();
		portal.setSizeFull();
		portal.addComponent(titleWrapper);
		portal.setCollapsible(titleWrapper, false);
		portal.setClosable(titleWrapper, false);
		
		return portal;
	}
	
	private VerticalLayout createLeftSide(){
		VerticalLayout leftSide = new VerticalLayout();
		leftSide.setSizeFull();
		leftSide.setCaption("Options");
		
		Panel p = new Panel();
		p.setSizeFull();
		leftSide.addComponent(p);
		
		Component configForm = createConfigForm();
		p.addComponent(configForm);
		
		Component progressPanel = createProgressPanel();
		p.addComponent(progressPanel);
		
		return leftSide;
	}
	
	private Component createConfigForm(){
		Form form = new Form();
		form.getLayout().setWidth("100%");
		form.setWidth("100%");
		((FormLayout)form.getLayout()).setSpacing(true);
		
		resourceURIField = new TextField();
		resourceURIField.setWidth("90%");
		resourceURIField.setCaption("Resource URI");resourceURIField.setValue("http://dbpedia.org/ontology/Bridge");
		form.addField("Resource URI", resourceURIField);
		
		autoDetectBox = new CheckBox();
		autoDetectBox.setValue(true);
		autoDetectBox.setCaption("Detect automatically");
		autoDetectBox.setImmediate(true);
		autoDetectBox.addListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				onResourceTypeChanged();
			}
		});
		form.addField("auto detect", autoDetectBox);
		
		resourceTypeField = new ResourceTypeField();
		resourceTypeField.setEnabled(!(Boolean) autoDetectBox.getValue());
		form.addField("resource type", resourceTypeField);
		
		useInferenceBox = new CheckBox();
		useInferenceBox.setCaption("Inference");
		form.addField("inference", useInferenceBox);
		
		maxExecutionTimeSpinner = new IntStepper();
		maxExecutionTimeSpinner.setValue(10);
		maxExecutionTimeSpinner.setStepAmount(1);
		maxExecutionTimeSpinner.setWidth("90%");
		maxExecutionTimeSpinner.setCaption("Max. execution time");
		form.addField("max execution time", maxExecutionTimeSpinner);
		
		maxNrOfReturnedAxiomsSpinner = new IntStepper();
		maxNrOfReturnedAxiomsSpinner.setValue(10);
		maxNrOfReturnedAxiomsSpinner.setStepAmount(1);
		maxNrOfReturnedAxiomsSpinner.setWidth("90%");
		maxNrOfReturnedAxiomsSpinner.setCaption("Max. returned axioms");
		form.addField("max returned axioms", maxNrOfReturnedAxiomsSpinner);
		
		thresholdSlider = new Slider(1, 100);
		thresholdSlider.setWidth("90%");
		thresholdSlider.setImmediate(true);
		thresholdSlider.setCaption("Threshold");
		form.addField("threshold", thresholdSlider);
		
		axiomTypesField = new AxiomTypesField();
		form.addField("axiom types", axiomTypesField);
		
		ContextHelp contextHelp = UserSession.getContextHelp();
		contextHelp.addHelpForComponent(resourceURIField, "TODO");
		contextHelp.addHelpForComponent(autoDetectBox, "TODO");
		contextHelp.addHelpForComponent(useInferenceBox, "TODO");
		contextHelp.addHelpForComponent(maxExecutionTimeSpinner, "TODO");
		contextHelp.addHelpForComponent(thresholdSlider, "TODO");
		contextHelp.addHelpForComponent(axiomTypesTable, "TODO");
		
		startButton = new Button("Start");
		startButton.setDisableOnClick(true);
		startButton.addListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onLearning();
			}
		});
//		form.getFooter().addComponent(startButton);
		
		stopButton = new Button("Stop");
		stopButton.addListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				
			}
		});
		HorizontalLayout buttonBar = new HorizontalLayout();
		buttonBar.setWidth(null);
		buttonBar.setSpacing(true);
		buttonBar.addComponent(startButton); 
		buttonBar.addComponent(stopButton);
		buttonBar.setComponentAlignment(startButton, Alignment.MIDDLE_CENTER);
		form.getFooter().addComponent(buttonBar);
		((HorizontalLayout)form.getFooter()).setWidth("100%");
		((HorizontalLayout)form.getFooter()).setComponentAlignment(buttonBar, Alignment.MIDDLE_CENTER);
		
//		HorizontalLayout footerLayout = new HorizontalLayout();
//		footerLayout.addComponent(buttonBar);
//		footerLayout.setWidth("100%");	 // centering - 3A
//		footerLayout.setComponentAlignment(buttonBar, Alignment.BOTTOM_CENTER);	 // centering - 3B
//		form.setFooter(footerLayout);
		
		autoDetectBox.setValue(false);
		
		return form;
	}
	
	private Component createProgressPanel(){
		progressPanel = new VerticalLayout();
		progressPanel.setHeight("100%");
		progressPanel.setSpacing(true);
		
		return progressPanel;
	}
	
	private void onLearning(){
		axiomsPortal.removeAllComponents();
		progressPanel.removeAllComponents();
		enableLearning(false);
		tables = new HashSet<EvaluatedAxiomsTable>();
		
		final EnrichmentManager man = UserSession.getEnrichmentManager();
		
		String resourceURI = (String) resourceURIField.getValue();
		
		pendingAxiomTypes = new ArrayList<AxiomType>(axiomTypesField.getSelectedAxiomsTypes());
		
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
				Label label = new Label("Entity <b>" + resourceURI + "</b> is processed as " + resourceType.toString() + ".", Label.CONTENT_XHTML);
				label.addStyleName("entity-autodetect-info-label");
				progressPanel.addComponent(label);
				pendingAxiomTypes.retainAll(man.getAxiomTypes(resourceType));
			} catch (OREException e) {
				e.printStackTrace();
			}
		}
		
		BackgroundExecutor executor = new BackgroundExecutor(new ContextExecutorServiceProvider());
		progressPanel.addComponent(executor);
		
		for(final AxiomType axiomType : pendingAxiomTypes){
			executor.submit(new LearnAxiomWorker((OREApplication) getApplication(), axiomType, resourceURI, man, this));
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
				String sparulString = translator.translate(changes);
				final Window window = new Window("SPARUL statements");
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
	
	public void showTable(AxiomType axiomType, List<EvaluatedAxiom> axioms){
		if(!axioms.isEmpty()){
			EvaluatedAxiomsTable table = new EvaluatedAxiomsTable(axiomType, axioms);
			table.setWidth("90%");
			String axiomName = axiomType.getName();
			if(axiomType.equals(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY)){
				axiomName = "IrreflexiveObjectProperty";
			}
			table.setCaption(axiomName + " Axioms");
			tables.add(table);
			HorizontalLayout header = new HorizontalLayout();
			axiomsPortal.addComponent(table);
			axiomsPortal.setHeaderComponent(table, header);
			axiomsPortal.setClosable(table, false);
		}
	}
	
	private ResourceType getSelectedResourceType(){
		if((Boolean) autoDetectBox.getValue()){
			return ResourceType.UNKNOWN;
		} else {
			return resourceTypeField.getValue();
		}
	}
	
	private void onResourceTypeChanged(){
		resourceTypeField.setEnabled(!(Boolean) autoDetectBox.getValue());
		ResourceType type = getSelectedResourceType();
		axiomTypesField.updateVisibleAxiomTypes(UserSession.getEnrichmentManager().getAxiomTypes(type));
	}
	
	private void enableLearning(boolean enable){
		startButton.setEnabled(enable);
		stopButton.setEnabled(!enable);
	}
	


	@Override
	public void activated(Object... params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deactivated(Object... params) {
		// TODO Auto-generated method stub
		
	}
	
	public void reset(){
		axiomsPortal.removeAllComponents();
		resourceURIField.setValue("");
		autoDetectBox.setValue(true);
		resourceTypeField.setValue(ResourceType.CLASS);
		startButton.setEnabled(false);
		try {
			thresholdSlider.setValue(70);
		} catch (ValueOutOfBoundsException e) {
			e.printStackTrace();
		}
	}
	
	private class AxiomTypesField extends FieldWrapper<AxiomType>{
		
		private Set<AxiomType> selectedAxiomsTypes = new HashSet<AxiomType>();
		private Collection<AxiomType> visibleAxiomTypes;
		
		public AxiomTypesField() {
			super(new Table(), null, AxiomType.class);
			setCaption("Axiom types");
			VerticalLayout main = new VerticalLayout();
			//(de)select all checkbox
			CheckBox allCheckBox = new CheckBox("All");
			allCheckBox.addStyleName("select-all-axiomtypes-checkbox");
			allCheckBox.setImmediate(true);
			allCheckBox.addListener(new Property.ValueChangeListener() {
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
			main.addComponent(allCheckBox);
			//table
			axiomTypesTable = new Table();
			axiomTypesTable.setHeight("300px");
			axiomTypesTable.setWidth("90%");
			axiomTypesTable.setImmediate(true);
			axiomTypesTable.setPageLength(10);
			axiomTypesTable.setColumnHeaderMode(Table.COLUMN_HEADER_MODE_HIDDEN);
			axiomTypesContainer = new BeanItemContainer<AxiomType>(AxiomType.class);
			this.visibleAxiomTypes = UserSession.getEnrichmentManager().getAxiomTypes(ResourceType.UNKNOWN);
			axiomTypesContainer.addAll(visibleAxiomTypes);
			axiomTypesTable.setContainerDataSource(axiomTypesContainer);
			axiomTypesTable.addGeneratedColumn("selected", new ColumnGenerator() {
				
				@Override
				public Object generateCell(Table source, final Object itemId, Object columnId) {
					CheckBox box = new CheckBox();
					box.setValue(selectedAxiomsTypes.contains((AxiomType) itemId));
					box.setImmediate(true);
					box.addListener(new Property.ValueChangeListener() {
						@Override
						public void valueChange(Property.ValueChangeEvent event) {
							if((Boolean) event.getProperty().getValue()){
								selectedAxiomsTypes.add((AxiomType) itemId);
							} else {
								selectedAxiomsTypes.remove((AxiomType) itemId);
							}
							startButton.setEnabled(!selectedAxiomsTypes.isEmpty());
						}
					});
					return box;
				}
			});
			axiomTypesTable.setVisibleColumns(new String[] {"selected", "name"});
			main.addComponent(axiomTypesTable);
			main.setExpandRatio(axiomTypesTable, 1f);
			
			
			setCompositionRoot(main);
		}
		
		public void updateVisibleAxiomTypes(Collection<AxiomType> visibleAxiomTypes){
			this.visibleAxiomTypes = visibleAxiomTypes;
			selectedAxiomsTypes.retainAll(visibleAxiomTypes);
			axiomTypesContainer.removeAllItems();
			axiomTypesContainer.addAll(visibleAxiomTypes);
		}
		
		public Set<AxiomType> getSelectedAxiomsTypes() {
			return selectedAxiomsTypes;
		}
	}
	
	private class ResourceTypeField extends FieldWrapper<ResourceType>{
		
		public ResourceTypeField() {
			super(new FlexibleOptionGroup(), null, ResourceType.class);
			setCaption("Resource type");
			FlexibleOptionGroup resourceTypeGroup = (FlexibleOptionGroup) getWrappedField();
			resourceTypeGroup.setImmediate(true);
			resourceTypeGroup.addItem(ResourceType.CLASS);
			resourceTypeGroup.addItem(ResourceType.OBJECT_PROPERTY);
			resourceTypeGroup.addItem(ResourceType.DATA_PROPERTY);
			resourceTypeGroup.setValue(ResourceType.CLASS);
			resourceTypeGroup.addListener(new ValueChangeListener() {
				@Override
				public void valueChange(
						com.vaadin.data.Property.ValueChangeEvent event) {
					onResourceTypeChanged();
					
				}
			});
			
			HorizontalLayout optionGroupLayout = new HorizontalLayout();
			for (Iterator<FlexibleOptionGroupItemComponent> iter = resourceTypeGroup.getItemComponentIterator(); iter.hasNext();) {
				FlexibleOptionGroupItemComponent comp = iter.next();
				optionGroupLayout.addComponent(comp);
				Label captionLabel = new Label();
				captionLabel.setIcon(comp.getIcon());
				captionLabel.setCaption(comp.getCaption());
				optionGroupLayout.addComponent(captionLabel);
			}
			setCompositionRoot(optionGroupLayout);
		}
		
	}
	
	class LearnAxiomWorker extends VaadinWorker{
		
		private AxiomType axiomType;
		private String resourceURI;
		private EnrichmentManager man;
		private List<EvaluatedAxiom> learnedAxioms;
		private EnrichmentView view;
		
		public LearnAxiomWorker(OREApplication app, AxiomType axiomType, String resourceURI, EnrichmentManager man, EnrichmentView view) {
			super(app);
			this.axiomType = axiomType;
			this.resourceURI = resourceURI;
			this.man = man;
			this.view = view;
			setIndeterminate(true);
		}
		
		@Override
		public void runInBackground() {
			updateProgress(0, "Computing " + axiomType.getName() + " axioms...");
			try {
				learnedAxioms = man.getEvaluatedAxioms2(resourceURI, axiomType);
			} catch (OREException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void updateUI() {
			view.showTable(axiomType, learnedAxioms);
			pendingAxiomTypes.remove(axiomType);
			view.enableLearning(pendingAxiomTypes.isEmpty());
		}
		
		
		
	}

}
