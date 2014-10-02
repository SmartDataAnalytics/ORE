package org.aksw.ore.component;

import java.util.Arrays;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Slider;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.VerticalLayout;

public class LearningOptionsPanel extends VerticalLayout{
	
	public enum Profile {
		CUSTOM("Custom"), OWL_2("OWL 2 DL"), OWL_EL("OWL 2 EL");
		
		private String name;

		private Profile(String name){
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		@Override
		public String toString() {
			return name;
		}
	}
	
	public enum OWLConstruct {
		HAS_VALUE("Value"), EXISTS("Some"), ALL("Only"), NEGATION("Not"), MIN_MAX("Min/Max");
		
		private String name;
		private OWLConstruct(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		@Override
		public String toString() {
			return name;
		}
	}
	
	private IntStepper maxExecutionTimeSpinner;
	private IntStepper maxNrOfResultsSpinner;
	private Slider noiseSlider;
	private Slider thresholdSlider;
	
	private OptionGroup profileOptions;
	private OptionGroup constructorOptions;
	
	public LearningOptionsPanel() {
		setCaption("Options");
		setSizeFull();
		setHeight(null);
		setSpacing(true);
//		setMargin(true);
		addStyleName("enrichment-options");
		
		maxExecutionTimeSpinner = new IntStepper();
		maxExecutionTimeSpinner.setStepAmount(1);
		maxExecutionTimeSpinner.setMinValue(1);
		maxExecutionTimeSpinner.setWidth("100%");
		maxExecutionTimeSpinner.setImmediate(true);
		maxExecutionTimeSpinner.setCaption("Max. execution time in s");
		maxExecutionTimeSpinner.setDescription("The maximum execution time of the algorithm in seconds.");
		addComponent(maxExecutionTimeSpinner);
		
		maxNrOfResultsSpinner = new IntStepper();
		maxNrOfResultsSpinner.setStepAmount(1);
		maxNrOfResultsSpinner.setMinValue(1);
		maxNrOfResultsSpinner.setWidth("100%");
		maxNrOfResultsSpinner.setImmediate(true);
		maxNrOfResultsSpinner.setCaption("Max. number of results");
		maxNrOfResultsSpinner.setDescription("Sets the maximum number of results one is interested in." +
				" (Setting this to a lower value may increase performance as the learning algorithm has to store/evaluate/beautify less descriptions).");
		addComponent(maxNrOfResultsSpinner);
		
		noiseSlider = new Slider(0, 100);
		noiseSlider.setWidth("100%");
		noiseSlider.setImmediate(true);
		noiseSlider.setCaption("Noise in %");
		noiseSlider.setDescription("The (approximated) percentage of noise within the examples");
		addComponent(noiseSlider);
		
		thresholdSlider = new Slider(0, 100);
		thresholdSlider.setWidth("100%");
		thresholdSlider.setImmediate(true);
		thresholdSlider.setCaption("Threshold in %");
		thresholdSlider.setDescription("Minimum accuracy. All class expressions with lower accuracy are disregarded. " +
				"Specify a value between 0% and 100%. Use 0% if you do not want this filter to be active. ");
		addComponent(thresholdSlider);
		
		VerticalLayout profileForm = new VerticalLayout();
		profileForm.setDescription("Choose which constructs are allowed in the generated class expressions.");
		profileForm.setWidth("100%");
//		profileForm.setCaption("OWL Profile");
		profileForm.setSpacing(true);
//		profileForm.setMargin(true);
		
		profileOptions = new OptionGroup("OWL Profile");
		profileOptions.setImmediate(true);
		profileOptions.setContainerDataSource(new BeanItemContainer<Profile>(Profile.class, Arrays.asList(Profile.values())));
		profileOptions.setItemCaptionPropertyId("name");
		profileOptions.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		profileOptions.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				onProfileChanged();
			}
		});
		profileForm.addComponent(profileOptions);
		profileForm.setComponentAlignment(profileOptions, Alignment.BOTTOM_LEFT);
		
		constructorOptions = new OptionGroup("Used OWL Constructors");
		constructorOptions.setContainerDataSource(new BeanItemContainer<OWLConstruct>(OWLConstruct.class, Arrays.asList(OWLConstruct.values())));
		constructorOptions.setItemCaptionPropertyId("name");
		constructorOptions.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		constructorOptions.setMultiSelect(true);
		constructorOptions.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
		profileForm.addComponent(constructorOptions);
		profileForm.setComponentAlignment(constructorOptions, Alignment.MIDDLE_CENTER);
		
		addComponent(profileForm);
		
		setComponentAlignment(maxExecutionTimeSpinner, Alignment.MIDDLE_CENTER);
		setComponentAlignment(maxNrOfResultsSpinner, Alignment.MIDDLE_CENTER);
		setComponentAlignment(noiseSlider, Alignment.MIDDLE_CENTER);
		setComponentAlignment(thresholdSlider, Alignment.MIDDLE_CENTER);
		setComponentAlignment(profileForm, Alignment.MIDDLE_CENTER);
		
		reset();
	}
	
	public int getMaxNrOfResults(){
		return (Integer) maxNrOfResultsSpinner.getValue();
	}
	
	public int getMaxExecutionTimeInSeconds(){
		return (Integer) maxExecutionTimeSpinner.getValue();
	}
	
	public double getNoiseInPercentage(){
		return (Double) noiseSlider.getValue();
	}
	
	public double getThresholdInPercentage(){
		return (Double) thresholdSlider.getValue();
	}
	
	public boolean useUniversalQuantifier(){
		return constructorOptions.isSelected(OWLConstruct.ALL);
	}
	
	public boolean useExistentialQuantifier(){
		return constructorOptions.isSelected(OWLConstruct.EXISTS);
	}
	
	public boolean useNegation(){
		return constructorOptions.isSelected(OWLConstruct.NEGATION);
	}
	
	public boolean useHasValueQuantifier(){
		return constructorOptions.isSelected(OWLConstruct.HAS_VALUE);
	}
	
	public boolean useCardinalityRestriction(){
		return constructorOptions.isSelected(OWLConstruct.MIN_MAX);
	}
	
	public int getCardinalityLimit(){
		return 5;
	}
	
	public void reset(){
		maxExecutionTimeSpinner.setValue(10);
		maxNrOfResultsSpinner.setValue(10);
		try {
			thresholdSlider.setValue(80d);
		} catch (ValueOutOfBoundsException e) {
			e.printStackTrace();
		}
		profileOptions.setValue(Profile.CUSTOM);
//		cardinalitySpinner.setValue(1);
	}
	
	private void onProfileChanged(){
		Profile profile = (Profile) profileOptions.getValue();
		if(profile == Profile.CUSTOM){
			constructorOptions.select(OWLConstruct.ALL);
			constructorOptions.select(OWLConstruct.EXISTS);
			constructorOptions.select(OWLConstruct.MIN_MAX);
			constructorOptions.unselect(OWLConstruct.NEGATION);
			constructorOptions.unselect(OWLConstruct.HAS_VALUE);
		} else if(profile == Profile.OWL_2){
			constructorOptions.select(OWLConstruct.ALL);
			constructorOptions.select(OWLConstruct.EXISTS);
			constructorOptions.select(OWLConstruct.MIN_MAX);
			constructorOptions.select(OWLConstruct.NEGATION);
			constructorOptions.select(OWLConstruct.HAS_VALUE);
		} else if(profile == Profile.OWL_EL){
			constructorOptions.unselect(OWLConstruct.ALL);
			constructorOptions.select(OWLConstruct.EXISTS);
			constructorOptions.unselect(OWLConstruct.MIN_MAX);
			constructorOptions.unselect(OWLConstruct.NEGATION);
			constructorOptions.unselect(OWLConstruct.HAS_VALUE);
		}
	}

}
