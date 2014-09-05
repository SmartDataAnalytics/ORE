package org.aksw.ore.component;

import org.aksw.mole.ore.explanation.api.ExplanationType;
import org.aksw.ore.ORESession;
import org.vostok.vaadin.addon.button.spin.NumberModel;
import org.vostok.vaadin.addon.button.spin.SpinButton;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;

@SuppressWarnings("serial")
public class ExplanationOptionsPanel extends HorizontalLayout implements Property.ValueChangeListener{
	
	private CheckBox computeFineExplanationsCheckbox;
	
	private CheckBox showAggregatedViewCheckbox;
	
	private CheckBox limitExplanationCountCheckbox;
	private SpinButton limitSpinner;
	
	public ExplanationOptionsPanel(final ExplanationsPanel explanationsPanel) {
		setSpacing(true);
		addStyleName("explanation-options");
		
		showAggregatedViewCheckbox = new CheckBox("Show aggregated view");
		showAggregatedViewCheckbox.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				explanationsPanel.showAggregatedView((Boolean)event.getProperty().getValue());
			}
		});
		showAggregatedViewCheckbox.setImmediate(true);
		showAggregatedViewCheckbox.setWidth(null);
		showAggregatedViewCheckbox.setDescription("Show the axioms of all recently computed explanations in one single table.");
		addComponent(showAggregatedViewCheckbox);
		setComponentAlignment(showAggregatedViewCheckbox, Alignment.TOP_CENTER);
		
		computeFineExplanationsCheckbox = new CheckBox("Show fine grained explanations");
		computeFineExplanationsCheckbox.addValueChangeListener(this);
		computeFineExplanationsCheckbox.setImmediate(true);
		computeFineExplanationsCheckbox.setWidth(null);
		computeFineExplanationsCheckbox.setDescription("Show explanations which contain only relevant parts of the axioms.");
		addComponent(computeFineExplanationsCheckbox);
		setComponentAlignment(computeFineExplanationsCheckbox, Alignment.TOP_CENTER);
		
		limitExplanationCountCheckbox = new CheckBox("Limit explanation count to");
		limitExplanationCountCheckbox.setValue(true);
		limitExplanationCountCheckbox.setDescription("Show at most a fixed number of explanations per selected unsatisfiable class.");
		limitExplanationCountCheckbox.addValueChangeListener(this);
		limitExplanationCountCheckbox.setImmediate(true);
		limitExplanationCountCheckbox.setWidth(null);
		
		try {
			limitSpinner = new SpinButton(new NumberModel(1, 1, 1, Integer.MAX_VALUE, false, NumberModel.FORMAT_INTEGER));
		} catch (Exception e) {
			e.printStackTrace();
		}
		limitSpinner.setImmediate(true);
//		limitSpinner.setValue(1);
//		limitSpinner.setStepAmount(1);
//		limitSpinner.setMinValue(1);
		limitSpinner.addValueChangeListener(this);
		limitSpinner.setWidth("50px");
		
		HorizontalLayout l = new HorizontalLayout();
		l.addComponent(limitExplanationCountCheckbox);
		l.addComponent(limitSpinner);
		l.setExpandRatio(limitSpinner, 1f);
		l.setComponentAlignment(limitSpinner, Alignment.MIDDLE_LEFT);
		l.setComponentAlignment(limitExplanationCountCheckbox, Alignment.MIDDLE_CENTER);
		addComponent(l);
		setComponentAlignment(l, Alignment.MIDDLE_CENTER);
		
	}
	
	public void reset(){
		computeFineExplanationsCheckbox.removeValueChangeListener(this);
		limitExplanationCountCheckbox.removeValueChangeListener(this);
		limitSpinner.removeValueChangeListener(this);
		computeFineExplanationsCheckbox.setValue(false);
		limitExplanationCountCheckbox.setValue(true);
		limitSpinner.setValue(1);
		computeFineExplanationsCheckbox.addValueChangeListener(this);
		limitExplanationCountCheckbox.addValueChangeListener(this);
		limitSpinner.addValueChangeListener(this);
	}

	@Override
	public void valueChange(ValueChangeEvent event) {
		if(event.getProperty() == computeFineExplanationsCheckbox){
			ORESession.getExplanationManager().setExplanationType((Boolean)event.getProperty().getValue() ? ExplanationType.LACONIC : ExplanationType.REGULAR);
		} else if(event.getProperty() == limitExplanationCountCheckbox){
			boolean limitExplanations = (Boolean)event.getProperty().getValue();
			limitSpinner.setEnabled(limitExplanations);
			if(limitExplanations){
				ORESession.getExplanationManager().setExplanationLimit((Integer)limitSpinner.getValue());
			} else {
				ORESession.getExplanationManager().setExplanationLimit(-1);
				limitSpinner.setEnabled(false);
			}
		} else if(event.getProperty() == limitSpinner){
			ORESession.getExplanationManager().setExplanationLimit((Integer)event.getProperty().getValue());
			
		}
		
	}

}
