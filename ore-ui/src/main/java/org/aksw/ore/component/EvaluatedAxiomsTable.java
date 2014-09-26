package org.aksw.ore.component;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.aksw.ore.ORESession;
import org.aksw.ore.rendering.Renderer;
import org.aksw.ore.util.AxiomScoreExplanationGenerator;
import org.dllearner.core.EvaluatedAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

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
	
	enum Columns{
		SELECTED, ACCURACY, AXIOM
	}
	
	private static final DecimalFormat df = new DecimalFormat("0.00%");
	
	private Set<Object> selectedObjects = new HashSet<Object>();
	
	private AxiomType<? extends OWLAxiom> axiomType;
	private Collection<EvaluatedAxiom<OWLAxiom>> axioms;
	
	private Renderer renderer = ORESession.getRenderer();
	
	public EvaluatedAxiomsTable(final AxiomType<? extends OWLAxiom> axiomType, Collection<EvaluatedAxiom<OWLAxiom>> axioms) {
		this.axiomType = axiomType;
		this.axioms = axioms;
		
		addStyleName("enrichment-axioms-table");
		addStyleName(ValoTheme.TABLE_BORDERLESS);
		setWidth("100%");
		setHeightUndefined();
		setPageLength(0);
		setSelectable(true);
        setMultiSelect(true);
        setImmediate(true);
        setColumnCollapsingAllowed(false);
        setColumnReorderingAllowed(false);
		
		IndexedContainer container = new IndexedContainer();
		container.addContainerProperty(Columns.SELECTED, CheckBox.class, null);
		container.addContainerProperty(Columns.ACCURACY, Double.class, null);
		container.addContainerProperty(Columns.AXIOM, OWLAxiom.class, null);
		setContainerDataSource(container);
		
		Set<String> renderedAxioms = Sets.newHashSetWithExpectedSize(axioms.size());
		final Set<EvaluatedAxiom<OWLAxiom>> axiomsToBePrefixed = Sets.newHashSetWithExpectedSize(axioms.size());
		for (EvaluatedAxiom<OWLAxiom> axiom : axioms) {
			String s = renderer.render(axiom.getAxiom());
			if(!renderedAxioms.add(s)){
				axiomsToBePrefixed.add(axiom);
			}
		}
		
		addGeneratedColumn(Columns.SELECTED, new ColumnGenerator() {
			
			@Override
			public Object generateCell(Table source, final Object itemId, Object columnId) {
				if (columnId == Columns.SELECTED) {
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
		
		addGeneratedColumn(Columns.ACCURACY, new ColumnGenerator() {
			
			@Override
			public Object generateCell(Table source, final Object itemId, Object columnId) {
				
				HorizontalLayout cell = new HorizontalLayout();
				cell.setSizeFull();
				cell.setWidthUndefined();
//				cell.setSpacing(true);
				
				Label accuracyLabel = new Label(df.format(((EvaluatedAxiom<OWLAxiom>) itemId).getScore().getAccuracy()));
				accuracyLabel.setWidthUndefined();
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
		
		addGeneratedColumn(Columns.AXIOM, new ColumnGenerator() {
			
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				if (columnId == Columns.AXIOM) {
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
		
		setColumnHeader(Columns.SELECTED, "");
		setColumnHeader(Columns.ACCURACY, "Accuracy");
		setColumnHeader(Columns.AXIOM, "Axiom");
		
		setColumnExpandRatio(Columns.AXIOM, 1f);
		
		Item item;
		for(EvaluatedAxiom<OWLAxiom> ax : axioms){
			item = container.addItem(ax);
			item.getItemProperty(Columns.ACCURACY).setValue(ax.getScore().getAccuracy());
			item.getItemProperty(Columns.AXIOM).setValue(ax.getAxiom());
		}
		
		setItemDescriptionGenerator(new ItemDescriptionGenerator() {                             
			public String generateDescription(Component source, Object itemId, Object propertyId) {
			    if(propertyId == null){
			        return "";//"Row description "+ itemId;
			    } else if(propertyId.equals(Columns.ACCURACY)) {
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
	}
	
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
