package org.aksw.ore.component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.aksw.mole.ore.explanation.formatter.ExplanationFormatter2.FormattedExplanation;
import org.aksw.ore.ORESession;
import org.aksw.ore.manager.RepairManager.RepairManagerListener;
import org.aksw.ore.rendering.Renderer;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class SPARQLBasedExplanationTable extends Table implements RepairManagerListener{
	
	enum Columns{
		SELECTED, FREQUENCY, AXIOM
	}
	
	private Explanation<OWLAxiom> explanation;
	private Set<Object> selectedObjects = new HashSet<>();
	private FormattedExplanation formattedExplanation;
	
	public SPARQLBasedExplanationTable(Explanation<OWLAxiom> explanation, Set<OWLAxiom> selectedAxioms) {
		this(explanation, selectedAxioms, true);
	}
	
	public SPARQLBasedExplanationTable(Explanation<OWLAxiom> explanation, Set<OWLAxiom> selectedAxioms, final boolean formatted) {
		this.explanation = explanation;
		selectedObjects.addAll(selectedAxioms);
		
		addStyleName("explanation-table");
		addStyleName(ValoTheme.TABLE_BORDERLESS);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);
		
		setWidth("100%");
		setPageLength(0);
		setHeight(null);
		setSelectable(true);
        setMultiSelect(true);
        setImmediate(true);
        
        setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		
		IndexedContainer container = new IndexedContainer();
		container.addContainerProperty(Columns.SELECTED, CheckBox.class, null);
		container.addContainerProperty(Columns.AXIOM, OWLAxiom.class, null);
		container.addContainerProperty(Columns.FREQUENCY, Integer.class, null);
		setContainerDataSource(container);
		
		addGeneratedColumn(Columns.SELECTED, new ColumnGenerator() {
			
			@Override
			public Object generateCell(Table source, final Object itemId, Object columnId) {
				if (Columns.SELECTED.equals(columnId)) {
					CheckBox cb = new CheckBox();
					cb.addStyleName(ValoTheme.CHECKBOX_SMALL);
					cb.setValue(selectedObjects.contains(itemId));
					cb.setImmediate(true);
					cb.addValueChangeListener(new ValueChangeListener() {
						
						@Override
						public void valueChange(com.vaadin.data.Property.ValueChangeEvent event) {
							boolean selected = (Boolean) event.getProperty().getValue();
							if(selected){
								selectedObjects.add(itemId);
							} else {
								selectedObjects.remove(itemId);
							}
							SPARQLBasedExplanationTable.this.setValue(selectedObjects);
							
						}
					});
					SPARQLBasedExplanationTable.this.getItem(itemId).getItemProperty(Columns.SELECTED).setValue(cb);
					return cb;
				}
				return null;
			}
		});
		//column alignment, numbers always right aligned
		setColumnAlignment(Columns.FREQUENCY, Align.RIGHT);
		
		//render axiom column colored
		addGeneratedColumn(Columns.AXIOM, new ColumnGenerator() {
			
			private Renderer renderer = ORESession.getRenderer();
			
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				if (Columns.AXIOM.equals(columnId)) {
					if(itemId instanceof OWLAxiom){
						OWLAxiom ax = ((OWLAxiom) itemId);
						String indention = "";
						if(formatted){
							for(int i = 0; i < formattedExplanation.getIndention(ax); i++){
								indention += "&nbsp;";
							}
						}
						return new Label(indention + renderer.renderHTML(ax), ContentMode.HTML);
		        	}
				}
				return null;
			}
		});
		
		setColumnHeader(Columns.SELECTED, "");
		setColumnHeader(Columns.AXIOM, "Axiom");
		setColumnHeader(Columns.FREQUENCY, "Frequency");
		
		Collection<OWLAxiom> axioms;
		if(formatted){
			formattedExplanation = ORESession.getSPARQLExplanationManager().format(explanation);
			axioms = formattedExplanation.getOrderedAxioms();
		} else {
			axioms = explanation.getAxioms();
		}
		Item item;
		for(OWLAxiom axiom : axioms){
			item = container.addItem(axiom);
//			item.getItemProperty(Columns.AXIOM).setValue(axiom);
			item.getItemProperty(Columns.FREQUENCY).setValue(ORESession.getSPARQLExplanationManager().getAxiomFrequency(axiom));
		}
		
		setValue(selectedObjects);
		
		setItemDescriptionGenerator(new ItemDescriptionGenerator() {
			
			@Override
			public String generateDescription(Component source, Object itemId, Object propertyId) {
				if(propertyId != null && propertyId.equals(Columns.FREQUENCY)){
					return "The axiom occurs in " + getItem(itemId).getItemProperty(Columns.FREQUENCY).getValue() + " explanations.";
				}
				return null;
			}
		});
		
		setColumnExpandRatio(Columns.AXIOM, 1.0f);
	}
	
	public void selectAxiom(OWLAxiom axiom){
		selectedObjects.add(axiom);
		setValue(selectedObjects);
	}
	
	public void selectAxioms(Collection<OWLAxiom> axioms){
		selectedObjects.clear();
		selectedObjects.addAll(explanation.getAxioms());
		selectedObjects.retainAll(axioms);
		for(OWLAxiom ax : explanation.getAxioms()){
			((CheckBox)getItem(ax).getItemProperty(Columns.SELECTED).getValue()).setValue(axioms.contains(ax));
		}
//		setValue(axioms);
	}

	public Explanation<OWLAxiom> getExplanation() {
		return explanation;
	}
	
	public Set<OWLAxiom> getSelectedAxioms(){
		return (Set<OWLAxiom>)getValue();
	}
	
	
	/* (non-Javadoc)
	 * @see com.vaadin.ui.Table#getColumnHeader(java.lang.Object)
	 */
	@Override
	public String getColumnHeader(Object propertyId) {
		if(propertyId.equals(Columns.FREQUENCY)){
//			return ("<a title='The number of explanations in which the axiom occurs.'>" + propertyId + "</a>").replace("'","\"");
			return ("<span title='The number of explanations in which the axiom occurs.'>" + propertyId + "</span>").replace("'","\"");
		}
		return super.getColumnHeader(propertyId);
	}

	@Override
	public void repairPlanChanged() {
		selectAxioms(ORESession.getRepairManager().getAxiomsScheduledToRemove());
	}

	@Override
	public void repairPlanExecuted() {
		// TODO Auto-generated method stub
		
	}

}
