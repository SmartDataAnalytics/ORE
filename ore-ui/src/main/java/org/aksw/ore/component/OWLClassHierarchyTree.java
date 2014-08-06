/**
 * 
 */
package org.aksw.ore.component;

import org.aksw.ore.util.ClassHierarchyContainer;
import org.semanticweb.owlapi.model.OWLClass;

import com.vaadin.ui.Component;
import com.vaadin.ui.Tree;

/**
 * @author Lorenz Buehmann
 *
 */
public class OWLClassHierarchyTree extends Tree{
	
	
	public OWLClassHierarchyTree() {
		setItemCaptionPropertyId(ClassHierarchyContainer.LABEL_PROPERTY);
		setImmediate(true);
		setSizeFull();
		setMultiSelect(false);
		
		//unsatisfiable classes are printed red
		setItemStyleGenerator(new ItemStyleGenerator() {
			
			@Override
			public String getStyle(Tree source, Object itemId) {
				boolean unsatisfiable = (Boolean) getItem(itemId).getItemProperty(ClassHierarchyContainer.UNSATISFIABLE_PROPERTY).getValue();
				int nrOfInstances = (Integer) getItem(itemId).getItemProperty(ClassHierarchyContainer.INSTANCES_SIZE_PROPERTY).getValue();
				if(unsatisfiable){
					return "unsatisfiable-class";
				} else if(nrOfInstances < 2){
					return "empty-class";
				}
				return null;
			}
		});
		
		setItemDescriptionGenerator(new ItemDescriptionGenerator() {
			
			@Override
			public String generateDescription(Component source, Object itemId, Object propertyId) {
				OWLClass cls = (OWLClass)itemId;
				return cls.toStringID();
			}
		});
		
		// avoid selection of empty classes
//		addValueChangeListener(new ValueChangeListener() {
//			Object previous = null;
//
//			@Override
//			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event) {
//				Object itemId = event.getProperty().getValue();
//				Item item = getItem(itemId);
//				if (!hasChildren(getValue())) {
//					setValue(previous);
//				} else {
//					previous = getValue();
//				}
//			}
//		});
	}
	
	public OWLClass getSelectedClass(){
		return (OWLClass) getValue();
	}
}
