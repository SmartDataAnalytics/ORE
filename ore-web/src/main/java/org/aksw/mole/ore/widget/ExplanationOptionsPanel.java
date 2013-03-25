package org.aksw.mole.ore.widget;

import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.explanation.api.ExplanationType;
import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;

@SuppressWarnings("serial")
public class ExplanationOptionsPanel extends HorizontalLayout implements Property.ValueChangeListener{
	
	private CheckBox computeFineExplanationsCheckbox;
	private CheckBox limitExplanationCountCheckbox;
	private IntStepper limitSpinner;
	
	public ExplanationOptionsPanel() {
		setSpacing(true);
		addStyleName("explanation-options");
		
		computeFineExplanationsCheckbox = new CheckBox("Show fine grained explanations");
		computeFineExplanationsCheckbox.addListener(this);
		computeFineExplanationsCheckbox.setImmediate(true);
		computeFineExplanationsCheckbox.setWidth(null);
		addComponent(computeFineExplanationsCheckbox);
		setComponentAlignment(computeFineExplanationsCheckbox, Alignment.MIDDLE_CENTER);
		
		limitExplanationCountCheckbox = new CheckBox("Limit explanation count to");
		limitExplanationCountCheckbox.setValue(true);
		limitExplanationCountCheckbox.addListener(this);
		limitExplanationCountCheckbox.setImmediate(true);
		limitExplanationCountCheckbox.setWidth(null);
		
		limitSpinner = new IntStepper();
		limitSpinner.setImmediate(true);
		limitSpinner.setValue(1);
		limitSpinner.setStepAmount(1);
		limitSpinner.setMinValue(1);
		limitSpinner.addListener(this);
		
		HorizontalLayout l = new HorizontalLayout();
		l.addComponent(limitExplanationCountCheckbox);
		l.addComponent(limitSpinner);
		l.setExpandRatio(limitSpinner, 1f);
		l.setComponentAlignment(limitSpinner, Alignment.MIDDLE_LEFT);
		l.setComponentAlignment(limitExplanationCountCheckbox, Alignment.MIDDLE_CENTER);
		addComponent(l);
		
	}
	
	public void reset(){
		computeFineExplanationsCheckbox.removeListener(this);
		limitExplanationCountCheckbox.removeListener(this);
		limitSpinner.removeListener(this);
		computeFineExplanationsCheckbox.setValue(false);
		limitExplanationCountCheckbox.setValue(true);
		limitSpinner.setValue(1);
		computeFineExplanationsCheckbox.addListener(this);
		limitExplanationCountCheckbox.addListener(this);
		limitSpinner.addListener(this);
	}

	@Override
	public void valueChange(ValueChangeEvent event) {
		if(event.getProperty() == computeFineExplanationsCheckbox){
			UserSession.getExplanationManager().setExplanationType((Boolean)event.getProperty().getValue() ? ExplanationType.LACONIC : ExplanationType.REGULAR);
		} else if(event.getProperty() == limitExplanationCountCheckbox){
			boolean limitExplanations = (Boolean)event.getProperty().getValue();
			limitSpinner.setEnabled(limitExplanations);
			if(limitExplanations){
				UserSession.getExplanationManager().setExplanationLimit((Integer)limitSpinner.getValue());
			} else {
				UserSession.getExplanationManager().setExplanationLimit(-1);
				limitSpinner.setEnabled(false);
			}
		} else if(event.getProperty() == limitSpinner){
			UserSession.getExplanationManager().setExplanationLimit((Integer)event.getProperty().getValue());
			
		}
		
	}

}
