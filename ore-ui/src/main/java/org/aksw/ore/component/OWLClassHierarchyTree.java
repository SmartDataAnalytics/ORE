/**
 * 
 */
package org.aksw.ore.component;

import org.aksw.ore.util.AssertedClassHierarchyContainer;
import org.semanticweb.owlapi.model.OWLClass;

import com.vaadin.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tree;

/**
 * @author Lorenz Buehmann
 *
 */
public class OWLClassHierarchyTree extends Tree{
	
	
	public OWLClassHierarchyTree() {
		setItemCaptionPropertyId(AssertedClassHierarchyContainer.LABEL_PROPERTY);
		setImmediate(true);
		setSizeFull();
		setMultiSelect(false);
		
		//unsatisfiable classes are printed red
		setItemStyleGenerator(new ItemStyleGenerator() {
			
			@Override
			public String getStyle(Tree source, Object itemId) {
				boolean unsatisfiable = (Boolean) getItem(itemId).getItemProperty(AssertedClassHierarchyContainer.UNSATISFIABLE_PROPERTY).getValue();
				int nrOfInstances = (Integer) getItem(itemId).getItemProperty(AssertedClassHierarchyContainer.INSTANCES_SIZE_PROPERTY).getValue();
				if(unsatisfiable){
					return "unsatisfiable-class";
				} else if(nrOfInstances < 3){
					return "empty-class";
				}
				return null;
			}
		});
		
		setItemDescriptionGenerator(new ItemDescriptionGenerator() {
			
			@Override
			public String generateDescription(Component source, Object itemId, Object propertyId) {
				return itemId.toString();
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
