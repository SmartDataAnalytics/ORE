package org.aksw.mole.ore.widget;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.aksw.mole.ore.util.Renderer;
import org.aksw.mole.ore.util.Renderer.Syntax;
import org.aksw.mole.ore.util.ScoreExplanationPattern;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.core.owl.AsymmetricObjectPropertyAxiom;
import org.dllearner.core.owl.Axiom;
import org.dllearner.core.owl.DatatypePropertyDomainAxiom;
import org.dllearner.core.owl.DatatypePropertyRangeAxiom;
import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.DisjointClassesAxiom;
import org.dllearner.core.owl.DisjointDatatypePropertyAxiom;
import org.dllearner.core.owl.DisjointObjectPropertyAxiom;
import org.dllearner.core.owl.EquivalentDatatypePropertiesAxiom;
import org.dllearner.core.owl.EquivalentObjectPropertiesAxiom;
import org.dllearner.core.owl.FunctionalDatatypePropertyAxiom;
import org.dllearner.core.owl.FunctionalObjectPropertyAxiom;
import org.dllearner.core.owl.InverseFunctionalObjectPropertyAxiom;
import org.dllearner.core.owl.IrreflexiveObjectPropertyAxiom;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.core.owl.ObjectPropertyDomainAxiom;
import org.dllearner.core.owl.ObjectPropertyRangeAxiom;
import org.dllearner.core.owl.ReflexiveObjectPropertyAxiom;
import org.dllearner.core.owl.SubClassAxiom;
import org.dllearner.core.owl.SubDatatypePropertyAxiom;
import org.dllearner.core.owl.SubObjectPropertyAxiom;
import org.dllearner.core.owl.SymmetricObjectPropertyAxiom;
import org.dllearner.core.owl.TransitiveObjectPropertyAxiom;
import org.dllearner.learningproblems.AxiomScore;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.profiles.OWLProfile;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.BaseTheme;

public class EvaluatedAxiomsTable extends Table{
	
	private DecimalFormat df = new DecimalFormat("0.00%");
	private Set<Object> selectedObjects = new HashSet<Object>();
	private AxiomType axiomType;
	
	private Renderer renderer = new Renderer();
	
	public EvaluatedAxiomsTable(final AxiomType axiomType, Collection<EvaluatedAxiom> axioms) {
		this.axiomType = axiomType;
		
		setSizeFull();
		setPageLength(0);
		setHeight(null);
		setColumnWidth("Selected", 30);
		setColumnWidth("Accuracy", 100);
		setColumnExpandRatio("Axiom", 1.0f);
		setSelectable(true);
        setMultiSelect(true);
        setImmediate(true);
		
		IndexedContainer container = new IndexedContainer();
		container.addContainerProperty("Selected", CheckBox.class, null);
		container.addContainerProperty("Accuracy", Double.class, null);
		container.addContainerProperty("Axiom", String.class, null);
		setContainerDataSource(container);
		
		addGeneratedColumn("Selected", new ColumnGenerator() {
			
			@Override
			public Object generateCell(Table source, final Object itemId, Object columnId) {
				if ("Selected".equals(columnId)) {
					CheckBox cb = new CheckBox();
					cb.addListener(new ValueChangeListener() {
						
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
				cell.setWidth("100%");
				cell.addStyleName("tweet");
				
				Label accuracyLabel = new Label(df.format(((EvaluatedAxiom) itemId).getScore().getAccuracy()));
				accuracyLabel.setDescription(getAccuracyDescription((EvaluatedAxiom)itemId));
				cell.addComponent(accuracyLabel);
				
				VerticalLayout buttons = new VerticalLayout();
				buttons.setHeight("100%");
				buttons.addStyleName("buttons");
				Button posExampleButton = new Button();
				posExampleButton.setIcon(new ThemeResource("images/question_mark_icon.png"));
				posExampleButton.addStyleName(BaseTheme.BUTTON_LINK);
				posExampleButton.setDescription("Explain the score.");
				posExampleButton.addListener(new Button.ClickListener() {
					
					@Override
					public void buttonClick(ClickEvent event) {
						getApplication().getMainWindow().addWindow(new AxiomScoreExplanationDialog(axiomType, (EvaluatedAxiom)itemId));
					}
				});
				buttons.addComponent(posExampleButton);
				cell.addComponent(buttons);
				
				return cell;
			}
		});
		
		addGeneratedColumn("Axiom", new ColumnGenerator() {
			
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				if ("Axiom".equals(columnId)) {
					if(itemId instanceof EvaluatedAxiom){
						Axiom axiom = ((EvaluatedAxiom) itemId).getAxiom();
						Label l = new Label(renderer.render(axiom, Syntax.MANCHESTER), Label.CONTENT_XHTML);
						l.setDescription(renderer.render(axiom, Syntax.MANCHESTER, true));
						return l;
		        	}
				}
				return null;
			}
		});
		
		setColumnHeader("Selected", "");
		
		Item item;
		for(EvaluatedAxiom ax : axioms){
			item = container.addItem(ax);
			item.getItemProperty("Accuracy").setValue(ax.getScore().getAccuracy());
			item.getItemProperty("Axiom").setValue(ax.getAxiom());
		}
		
		setItemDescriptionGenerator(new ItemDescriptionGenerator() {                             
			public String generateDescription(Component source, Object itemId, Object propertyId) {
			    if(propertyId == null){
			        return "Row description "+ itemId;
			    } else if(propertyId.equals("Accuracy")) {
			        return getAccuracyDescription((EvaluatedAxiom)itemId);
			    } 
//			    else if(propertyId.equals("Axiom")){
//			    	Axiom axiom = ((EvaluatedAxiom) itemId).getAxiom();
//			    	return renderer.render(axiom, Syntax.MANCHESTER, true);
//			    }
			    return null;
			}
		});
	};
	
	private String getAccuracyDescription(EvaluatedAxiom evAxiom){
		AxiomScore score = (AxiomScore) evAxiom.getScore();
		String explanationPattern = ScoreExplanationPattern.getExplanationPattern(axiomType);
		if(explanationPattern == null || explanationPattern.isEmpty()){
			explanationPattern = ScoreExplanationPattern.getGenericPattern();
		}
		explanationPattern = explanationPattern.replace("$total", "<b>" + String.valueOf(score.getTotalNrOfExamples()) + "</b>");
		explanationPattern = explanationPattern.replace("$pos", "<b>" + String.valueOf(score.getNrOfPositiveExamples()) + "</b>");
		Axiom axiom = evAxiom.getAxiom();
		OWLAxiom owlAxiom = OWLAPIConverter.getOWLAPIAxiom(axiom);
		String prop1 = null;
		String prop2 = null;
		String cls1 = null;
		String cls2 = null;
		String datatype = null;
		if (axiom instanceof TransitiveObjectPropertyAxiom) {
			prop1 = ((TransitiveObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof FunctionalObjectPropertyAxiom) {
			prop1 = ((FunctionalObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof InverseFunctionalObjectPropertyAxiom) {
			prop1 = ((InverseFunctionalObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof ReflexiveObjectPropertyAxiom) {
			prop1 = ((ReflexiveObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof IrreflexiveObjectPropertyAxiom) {
			prop1 = ((IrreflexiveObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof SymmetricObjectPropertyAxiom) {
			prop1 = ((SymmetricObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof AsymmetricObjectPropertyAxiom) {
			prop1 = ((AsymmetricObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof SubObjectPropertyAxiom) {
			prop1 = ((SubObjectPropertyAxiom)axiom).getSubRole().getName();
			prop2 = ((SubObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof DisjointObjectPropertyAxiom) {
			prop1 = ((DisjointObjectPropertyAxiom)axiom).getRole().getName();
			prop2 = ((DisjointObjectPropertyAxiom)axiom).getDisjointRole().getName();
		} else if (axiom instanceof EquivalentObjectPropertiesAxiom) {
			Collection<ObjectProperty> properties = ((EquivalentObjectPropertiesAxiom)axiom).getEquivalentProperties();
			Iterator<ObjectProperty> iter = properties.iterator();
			prop1 = iter.next().getName();
			prop2 = iter.next().getName();
		} else if (axiom instanceof ObjectPropertyDomainAxiom) {
			prop1 = ((ObjectPropertyDomainAxiom)axiom).getProperty().getName();
			cls1 = ((ObjectPropertyDomainAxiom)axiom).getDomain().toString();
		} else if (axiom instanceof ObjectPropertyRangeAxiom) {
			prop1 = ((ObjectPropertyRangeAxiom)axiom).getProperty().getName();
			cls1 = ((ObjectPropertyRangeAxiom)axiom).getRange().toString();
		} else if (axiom instanceof SubDatatypePropertyAxiom) {
			prop1 = ((SubDatatypePropertyAxiom)axiom).getSubRole().getName();
			prop2 = ((SubDatatypePropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof EquivalentDatatypePropertiesAxiom) {
			prop1 = ((EquivalentDatatypePropertiesAxiom)axiom).getRole().getName();
			prop2 = ((EquivalentDatatypePropertiesAxiom)axiom).getEquivalentRole().getName();
		} else if (axiom instanceof DisjointDatatypePropertyAxiom) {
			prop1 = ((DisjointDatatypePropertyAxiom)axiom).getRole().getName();
			prop2 = ((DisjointDatatypePropertyAxiom)axiom).getDisjointRole().getName();
		} else if (axiom instanceof FunctionalDatatypePropertyAxiom) {
			prop1 = ((FunctionalDatatypePropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof DatatypePropertyDomainAxiom) {
			prop1 = ((DatatypePropertyDomainAxiom)axiom).getProperty().getName();
			cls1 = ((DatatypePropertyDomainAxiom)axiom).getDomain().toString();
		} else if (axiom instanceof DatatypePropertyRangeAxiom) {
			prop1 = ((DatatypePropertyRangeAxiom)axiom).getProperty().getName();
			datatype = ((DatatypePropertyRangeAxiom)axiom).getRange().toString();
		} else if (axiom instanceof SubClassAxiom) {
			cls1 = ((SubClassAxiom)axiom).getSubConcept().toString();
			cls2 = ((SubClassAxiom)axiom).getSuperConcept().toString();
		} else if (axiom instanceof DisjointClassesAxiom) {
			Collection<Description> classes = ((DisjointClassesAxiom)axiom).getDescriptions();
			Iterator<Description> iter = classes.iterator();
			cls1 = iter.next().toString();
			cls2 = iter.next().toString();
		}
		if(prop1 != null){
			explanationPattern = explanationPattern.replace("$prop1", "<b>" + prop1 + "</b>");
		}
		if(prop2 != null){
			explanationPattern = explanationPattern.replace("$prop2", "<b>" + prop2 + "</b>");
		}
		if(cls1 != null){
			explanationPattern = explanationPattern.replace("$cls1", "<b>" + cls1 + "</b>");
		}
		if(cls2 != null){
			explanationPattern = explanationPattern.replace("$cls2", "<b>" + cls2 + "</b>");
		}
		if(datatype != null){
			explanationPattern = explanationPattern.replace("$datatype", "<b>" + datatype + "</b>");
		}
		explanationPattern = explanationPattern.replaceAll("(\\s)([x,y,z])([\\s|,|.])", "$1<i>$2</i>$3");
		
		
		return explanationPattern;
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
