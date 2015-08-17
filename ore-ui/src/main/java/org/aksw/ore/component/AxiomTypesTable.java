/**
 * 
 */
package org.aksw.ore.component;

import static org.semanticweb.owlapi.model.AxiomType.ASYMMETRIC_OBJECT_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.DATA_PROPERTY_DOMAIN;
import static org.semanticweb.owlapi.model.AxiomType.DATA_PROPERTY_RANGE;
import static org.semanticweb.owlapi.model.AxiomType.DISJOINT_CLASSES;
import static org.semanticweb.owlapi.model.AxiomType.DISJOINT_DATA_PROPERTIES;
import static org.semanticweb.owlapi.model.AxiomType.DISJOINT_OBJECT_PROPERTIES;
import static org.semanticweb.owlapi.model.AxiomType.EQUIVALENT_CLASSES;
import static org.semanticweb.owlapi.model.AxiomType.EQUIVALENT_DATA_PROPERTIES;
import static org.semanticweb.owlapi.model.AxiomType.EQUIVALENT_OBJECT_PROPERTIES;
import static org.semanticweb.owlapi.model.AxiomType.FUNCTIONAL_DATA_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.FUNCTIONAL_OBJECT_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.INVERSE_OBJECT_PROPERTIES;
import static org.semanticweb.owlapi.model.AxiomType.IRREFLEXIVE_OBJECT_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.OBJECT_PROPERTY_DOMAIN;
import static org.semanticweb.owlapi.model.AxiomType.OBJECT_PROPERTY_RANGE;
import static org.semanticweb.owlapi.model.AxiomType.REFLEXIVE_OBJECT_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.SUBCLASS_OF;
import static org.semanticweb.owlapi.model.AxiomType.SUB_DATA_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.SUB_OBJECT_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.SYMMETRIC_OBJECT_PROPERTY;
import static org.semanticweb.owlapi.model.AxiomType.TRANSITIVE_OBJECT_PROPERTY;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.aksw.ore.util.OWL2Doc;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import com.google.common.collect.Lists;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

/**
 * @author Lorenz Buehmann
 *
 */
public class AxiomTypesTable extends Table{
	
	 @SuppressWarnings("unchecked")
	 public static final Collection<AxiomType<? extends OWLAxiom>> CLASS_AXIOM_TYPES = new ArrayList<AxiomType<? extends OWLAxiom>>(
	            Arrays.asList(SUBCLASS_OF, EQUIVALENT_CLASSES, DISJOINT_CLASSES
	                    //DISJOINT_UNION, HAS_KEY
	                    ));
	 @SuppressWarnings("unchecked")
	 public static final Collection<AxiomType<? extends OWLAxiom>> OBJECT_PROPERTY_AXIOM_TYPES = new ArrayList<AxiomType<? extends OWLAxiom>>(
	            Arrays.asList(
	            		 SUB_OBJECT_PROPERTY, EQUIVALENT_OBJECT_PROPERTIES, DISJOINT_OBJECT_PROPERTIES,
	                    OBJECT_PROPERTY_DOMAIN, OBJECT_PROPERTY_RANGE,
	                    FUNCTIONAL_OBJECT_PROPERTY, INVERSE_FUNCTIONAL_OBJECT_PROPERTY,
	                    SYMMETRIC_OBJECT_PROPERTY, ASYMMETRIC_OBJECT_PROPERTY,
	                    REFLEXIVE_OBJECT_PROPERTY, IRREFLEXIVE_OBJECT_PROPERTY,
	                    INVERSE_OBJECT_PROPERTIES, 
	                    TRANSITIVE_OBJECT_PROPERTY
	                   
	            		));
	 @SuppressWarnings("unchecked")
	 public static final Collection<AxiomType<? extends OWLAxiom>> DATA_PROPERTY_AXIOM_TYPES = new ArrayList<AxiomType<? extends OWLAxiom>>(
			 Lists.newArrayList(
					 SUB_DATA_PROPERTY, EQUIVALENT_DATA_PROPERTIES, DISJOINT_DATA_PROPERTIES,
					 DATA_PROPERTY_DOMAIN, DATA_PROPERTY_RANGE,
					 FUNCTIONAL_DATA_PROPERTY
	            		));
	 
	private static final AxiomType allAxiomsAxiomType = allAxioms();
	
	private Set<AxiomType<OWLAxiom>> selectedAxiomsTypes = new HashSet<AxiomType<OWLAxiom>>();
	private BeanItemContainer<AxiomType<? extends OWLAxiom>> container;
	
	public AxiomTypesTable(final Property.ValueChangeListener listener) {
		addStyleName("axiomtypes-table");
		addStyleName(ValoTheme.TABLE_BORDERLESS);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);
        
		setSizeFull();
//		setHeightUndefined();
		setImmediate(true);
		setPageLength(0);
		setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		
		container = new BeanItemContainer<AxiomType<? extends OWLAxiom>>(AxiomType.class);
		setContainerDataSource(container);
		
		addGeneratedColumn("selected", new ColumnGenerator() {

			@SuppressWarnings("unchecked")
			@Override
			public Object generateCell(Table source, final Object itemId, Object columnId) {
				CheckBox box = new CheckBox();
				box.addStyleName(ValoTheme.CHECKBOX_SMALL);
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
//						startButton.setEnabled(!selectedAxiomsTypes.isEmpty());
					}
				});
				box.addValueChangeListener(listener);
				return box;
			}
		});
		setVisibleColumns(new String[] {"selected", "name"});
		setColumnExpandRatio("name", 1f);
		setItemDescriptionGenerator(new ItemDescriptionGenerator() {
			
			OWL2Doc doc = new OWL2Doc();
			
			@Override
			public String generateDescription(Component source, Object itemId, Object propertyId) {
				AxiomType axiomType = (AxiomType) itemId;
				if(!axiomType.equals(allAxiomsAxiomType) && !axiomType.getName().equals("All")){
					return doc.getDoc(axiomType);
				}
				return null;
			}
		});
	
	}
	
	public void show(EntityType<? extends OWLEntity> resourceType){
		container.removeAllItems();
//		container.addItem(allAxioms());
		
		if(resourceType == EntityType.CLASS){
			container.addAll(CLASS_AXIOM_TYPES);
		} else if(resourceType == EntityType.OBJECT_PROPERTY){
			container.addAll(OBJECT_PROPERTY_AXIOM_TYPES);
		} else if(resourceType == EntityType.DATA_PROPERTY){
			container.addAll(DATA_PROPERTY_AXIOM_TYPES);
		}
		
//		container.sort(new String[]{"name"}, new boolean[]{true});
	}
	
	private static AxiomType allAxioms(){
		try {
			Constructor<AxiomType> constructor = AxiomType.class.getDeclaredConstructor(int.class, String.class, boolean.class, boolean.class, boolean.class);
			constructor.setAccessible(true);
			AxiomType all = constructor.newInstance(50,"All", false ,false, false);
			return all;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @return the selectedAxiomsTypes
	 */
	public Set<AxiomType<OWLAxiom>> getSelectedAxiomsTypes() {
		return selectedAxiomsTypes;
	}
	
	public void selectAll(boolean select){
		if(select){
			for (AxiomType<? extends OWLAxiom> axiomType : container.getItemIds()) {
				selectedAxiomsTypes.add((AxiomType<OWLAxiom>) axiomType);
			}
		} else {
			selectedAxiomsTypes.clear();
		}
		
		refreshRowCache();
	}

}
