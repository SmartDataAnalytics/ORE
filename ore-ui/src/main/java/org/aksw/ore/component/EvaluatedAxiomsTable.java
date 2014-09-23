package org.aksw.ore.component;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.aksw.ore.ORESession;
import org.aksw.ore.rendering.Renderer;
import org.aksw.ore.util.AxiomScoreExplanationGenerator;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.learningproblems.AxiomScore;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNaryPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyCharacteristicAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

import com.google.common.collect.Sets;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

public class EvaluatedAxiomsTable extends Table{
	
	private DecimalFormat df = new DecimalFormat("0.00%");
	
	private Set<Object> selectedObjects = new HashSet<Object>();
	
	private AxiomType axiomType;
	private Collection<EvaluatedAxiom<OWLAxiom>> axioms;
	
	private Renderer renderer = ORESession.getRenderer();
	
	public EvaluatedAxiomsTable(final AxiomType axiomType, Collection<EvaluatedAxiom<OWLAxiom>> axioms) {
		this.axiomType = axiomType;
		this.axioms = axioms;
		
		addStyleName("enrichment-axioms-table");
		addStyleName(ValoTheme.TABLE_BORDERLESS);
		setSizeFull();
		setPageLength(0);
		setHeight(null);
//		setColumnWidth("Selected", 30);
//		setColumnWidth("Accuracy", 100);
		setColumnExpandRatio("Axiom", 1.0f);
		setSelectable(true);
        setMultiSelect(true);
        setImmediate(true);
		
		IndexedContainer container = new IndexedContainer();
		container.addContainerProperty("Selected", CheckBox.class, null);
		container.addContainerProperty("Accuracy", Double.class, null);
		container.addContainerProperty("Axiom", OWLAxiom.class, null);
		setContainerDataSource(container);
		
		Set<String> renderedAxioms = Sets.newHashSetWithExpectedSize(axioms.size());
		final Set<EvaluatedAxiom<OWLAxiom>> axiomsToBePrefixed = Sets.newHashSetWithExpectedSize(axioms.size());
		for (EvaluatedAxiom<OWLAxiom> axiom : axioms) {
			String s = renderer.render(axiom.getAxiom());
			if(!renderedAxioms.add(s)){
				axiomsToBePrefixed.add(axiom);
			}
		}
		
		addGeneratedColumn("Selected", new ColumnGenerator() {
			
			@Override
			public Object generateCell(Table source, final Object itemId, Object columnId) {
				if ("Selected".equals(columnId)) {
					CheckBox cb = new CheckBox();
					cb.addValueChangeListener(new ValueChangeListener() {
						
						@Override
						public void valueChange(com.vaadin.data.Property.ValueChangeEvent event) {
							boolean selected = (Boolean) event.getProperty().getValue();
							if(selected){
								selectedObjects.add(itemId);
							} else {
								selectedObjects.remove(itemId);
							}
							EvaluatedAxiomsTable.this.setValue(selectedObjects);
							
						}
					});
					return cb;
				}
				return null;
			}
		});
		
		addGeneratedColumn("Accuracy", new ColumnGenerator() {
			
			@Override
			public Object generateCell(Table source, final Object itemId, Object columnId) {
				
				HorizontalLayout cell = new HorizontalLayout();
				cell.setSizeFull();
				cell.setWidthUndefined();
//				cell.setSpacing(true);
				
				Label accuracyLabel = new Label(df.format(((EvaluatedAxiom<OWLAxiom>) itemId).getScore().getAccuracy()));
				cell.addComponent(accuracyLabel);
				cell.setExpandRatio(accuracyLabel, 1f);
				cell.setComponentAlignment(accuracyLabel, Alignment.MIDDLE_RIGHT);
				
				Button explain = new Button("?");
				explain.setHeight(null);
				explain.addStyleName(ValoTheme.BUTTON_LINK);
				explain.setWidthUndefined();
				explain.setDescription("Explain the score.");
				explain.addClickListener(new Button.ClickListener() {
					
					@Override
					public void buttonClick(ClickEvent event) {
						AxiomScoreExplanationDialog dialog = new AxiomScoreExplanationDialog(axiomType, (EvaluatedAxiom<OWLAxiom>)itemId);
						UI.getCurrent().addWindow(dialog);
						dialog.center();
					}
				});
				cell.addComponent(explain);
				cell.setComponentAlignment(explain, Alignment.MIDDLE_RIGHT);
				
				return cell;
			}
		});
		
		addGeneratedColumn("Axiom", new ColumnGenerator() {
			
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				if ("Axiom".equals(columnId)) {
					if(itemId instanceof EvaluatedAxiom){
						OWLAxiom axiom = ((EvaluatedAxiom) itemId).getAxiom();
						String s = renderer.renderHTML(axiom);
//						if(axiomsToBePrefixed.contains(((EvaluatedAxiom) itemId))){
//							s = renderer.renderPrefixed(axiom, Syntax.MANCHESTER);
//						} else {
//							s = renderer.render(axiom, Syntax.MANCHESTER);
//						}
						Label l = new Label(s, ContentMode.HTML);
						l.setWidth(null);
						l.setDescription(renderer.render(axiom));
						return l;
		        	}
				}
				return null;
			}
		});
		
		setColumnHeader("Selected", "");
		
		Item item;
		for(EvaluatedAxiom<OWLAxiom> ax : axioms){
			item = container.addItem(ax);
			item.getItemProperty("Accuracy").setValue(ax.getScore().getAccuracy());
			item.getItemProperty("Axiom").setValue(ax.getAxiom());
		}
		
		setItemDescriptionGenerator(new ItemDescriptionGenerator() {                             
			public String generateDescription(Component source, Object itemId, Object propertyId) {
			    if(propertyId == null){
			        return "";//"Row description "+ itemId;
			    } else if(propertyId.equals("Accuracy")) {
			    	EvaluatedAxiom<OWLAxiom> evAxiom = (EvaluatedAxiom<OWLAxiom>)itemId;
			        return AxiomScoreExplanationGenerator.getAccuracyDescription(evAxiom, ORESession.getRenderer());
			    } 
//			    else if(propertyId.equals("Axiom")){
//			    	Axiom axiom = ((EvaluatedAxiom) itemId).getAxiom();
//			    	return renderer.render(axiom, Syntax.MANCHESTER, true);
//			    }
			    return null;
			}
		});
	};
	
	@Override
	protected String formatPropertyValue(Object rowId, Object colId,
			Property property) {
		Object value = property.getValue();
		if(value instanceof Double){
			return df.format(((Double)property.getValue()).doubleValue());
		}
		return super.formatPropertyValue(rowId, colId, property);
	}
	
	public Set<Object> getSelectedObjects() {
		return selectedObjects;
	}
	
	public static void main(String[] args) {
		System.out.println(Pattern.compile("(\\s)([x,y,z])([\\s|,|.])").matcher(" ,").replaceAll("$1<i>$2</i>$3"));
	}

}
