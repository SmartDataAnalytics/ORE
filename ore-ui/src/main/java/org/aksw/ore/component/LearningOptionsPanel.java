package org.aksw.ore.component;

import java.util.Arrays;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Slider;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.VerticalLayout;

public class LearningOptionsPanel extends VerticalLayout{
	
	enum Profile {
		DEFAULT, OWL_2, OWL_EL
	}
	
	private IntStepper maxExecutionTimeSpinner;
	private IntStepper maxNrOfResultsSpinner;
	private Slider noiseSlider;
	private Slider thresholdSlider;
	
	private CheckBox universalQuantifierCheckBox;
	private CheckBox existentialQuantifierCheckBox;
	private CheckBox negationCheckBox;
	private CheckBox hasValueCheckBox;
	private CheckBox cardinalityCheckBox;
	private IntStepper cardinalitySpinner;
	
	private OptionGroup profileOptions;
	
	
	public LearningOptionsPanel() {
		setCaption("Options");
		setSizeFull();
		setHeight(null);
		setSpacing(true);
		addStyleName("enrichment-options");
		setMargin(true);
		
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
		profileForm.setCaption("OWL Profile");
		profileForm.setSpacing(true);
//		profileForm.setMargin(true);
		
		profileOptions = new OptionGroup("", Arrays.asList(Profile.values()));
		profileOptions.setImmediate(true);
		profileOptions.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				onProfileChanged();
			}
		});
		profileForm.addComponent(profileOptions);
		profileForm.setComponentAlignment(profileOptions, Alignment.BOTTOM_LEFT);
		
		VerticalLayout profileDetails = new VerticalLayout();
//		grid.setSpacing(true);
		profileDetails.setMargin(true);
		profileForm.addComponent(profileDetails);
		profileForm.setComponentAlignment(profileDetails, Alignment.MIDDLE_CENTER);
		
		universalQuantifierCheckBox = new CheckBox("only");
		profileDetails.addComponent(universalQuantifierCheckBox);
		
		existentialQuantifierCheckBox = new CheckBox("some");
		profileDetails.addComponent(existentialQuantifierCheckBox);
		
		negationCheckBox = new CheckBox("not");
		profileDetails.addComponent(negationCheckBox);
		
		hasValueCheckBox = new CheckBox("value");
		profileDetails.addComponent(hasValueCheckBox);
		
		cardinalityCheckBox = new CheckBox("min/max with");
		GridLayout cardinalityLayout = new GridLayout(2,1);
//		HorizontalLayout cardinalityLayout = new HorizontalLayout();
		cardinalityLayout.addComponent(cardinalityCheckBox);
//		cardinalityLayout.setComponentAlignment(cardinalityCheckBox, Alignment.MIDDLE_LEFT);
		cardinalityLayout.setSizeUndefined();
		cardinalityLayout.setWidth("100%");
		cardinalityLayout.addStyleName("no-padding");
		cardinalitySpinner = new IntStepper();
		cardinalitySpinner.setStepAmount(1);
		cardinalitySpinner.setMinValue(1);
		cardinalitySpinner.setMaxValue(10);
		cardinalitySpinner.setWidth("50px");
		cardinalitySpinner.setImmediate(true);
		cardinalityLayout.addComponent(cardinalitySpinner);
//		cardinalityLayout.setComponentAlignment(cardinalitySpinner, Alignment.MIDDLE_LEFT);
		profileDetails.addComponent(cardinalityLayout);
		profileDetails.setComponentAlignment(cardinalityLayout, Alignment.MIDDLE_LEFT);
		
		
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
		return (Boolean) universalQuantifierCheckBox.getValue();
	}
	
	public boolean useExistentialQuantifier(){
		return (Boolean) existentialQuantifierCheckBox.getValue();
	}
	
	public boolean useNegation(){
		return (Boolean) negationCheckBox.getValue();
	}
	
	public boolean useHasValueQuantifier(){
		return (Boolean) hasValueCheckBox.getValue();
	}
	
	public boolean useCardinalityRestriction(){
		return (Boolean) cardinalityCheckBox.getValue();
	}
	
	public int getCardinalityLimit(){
		return (Integer) cardinalitySpinner.getValue();
	}
	
	public void reset(){
		maxExecutionTimeSpinner.setValue(10);
		maxNrOfResultsSpinner.setValue(10);
		try {
			thresholdSlider.setValue(80d);
		} catch (ValueOutOfBoundsException e) {
			e.printStackTrace();
		}
		profileOptions.setValue(Profile.DEFAULT);
		cardinalitySpinner.setValue(1);
	}
	
	private void onProfileChanged(){
		Profile profile = (Profile) profileOptions.getValue();
		if(profile == Profile.DEFAULT){
			universalQuantifierCheckBox.setValue(true);
			existentialQuantifierCheckBox.setValue(true);
			negationCheckBox.setValue(false);
			hasValueCheckBox.setValue(false);
			cardinalityCheckBox.setValue(true);
		} else if(profile == Profile.OWL_2){
			universalQuantifierCheckBox.setValue(true);
			existentialQuantifierCheckBox.setValue(true);
			negationCheckBox.setValue(true);
			hasValueCheckBox.setValue(true);
			cardinalityCheckBox.setValue(true);
		} else if(profile == Profile.OWL_EL){
			universalQuantifierCheckBox.setValue(false);
			existentialQuantifierCheckBox.setValue(true);
			negationCheckBox.setValue(false);
			hasValueCheckBox.setValue(false);
			cardinalityCheckBox.setValue(false);
		}
	}

}
