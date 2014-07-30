/**
 * 
 */
package org.aksw.ore.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.aksw.ore.ORESession;
import org.aksw.ore.rendering.Renderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;

/**
 * @author Lorenz Buehmann
 *
 */
public class AssertedClassHierarchyContainer extends HierarchicalContainer{
	
	private static final OWLClass OWL_THING = new OWLClassImpl(OWLRDFVocabulary.OWL_THING.getIRI());
	private static final OWLClass OWL_NOTHING = new OWLClassImpl(OWLRDFVocabulary.OWL_NOTHING.getIRI());
	
	public static final String LABEL_PROPERTY = "label";
	public static final String UNSATISFIABLE_PROPERTY = "unsatisfiable";
	public static final String INSTANCES_SIZE_PROPERTY = "instances";
	
	private OWLReasoner reasoner;
	private Renderer renderer = ORESession.getRenderer();
	private Map<OWLClass, OWLClass> cls2OriginalCls = new HashMap<OWLClass, OWLClass>();
	
	
	private Set<OWLClass> unsatisfiableClasses;
	
	
	public AssertedClassHierarchyContainer(OWLOntology ontology) {
		reasoner = new StructuralReasonerFactory().createNonBufferingReasoner(ontology);
		unsatisfiableClasses = ORESession.getOWLReasoner().getUnsatisfiableClasses().getEntitiesMinusBottom();
		
		addContainerProperty(LABEL_PROPERTY, String.class, null);
		addContainerProperty(UNSATISFIABLE_PROPERTY, Boolean.class, false);
		addContainerProperty(INSTANCES_SIZE_PROPERTY, Integer.class, false);
		
		//start with owl:Thing
		addSubClasses(OWL_THING);
	}
	
	private void addSubClasses(OWLClass cls){
		Set<OWLClass> subClasses;
		if(cls2OriginalCls.containsKey(cls)){
			subClasses = reasoner.getSubClasses(cls2OriginalCls.get(cls), true).getFlattened();
		} else {
			subClasses = reasoner.getSubClasses(cls, true).getFlattened();
		}
		//remove trivial subclasses
		subClasses.remove(cls);
		subClasses.remove(OWL_NOTHING);
		
		setChildrenAllowed(cls, !subClasses.isEmpty());
		
		for (OWLClass subClass : subClasses) {
			OWLClass id = subClass;
			Item item = addItem(subClass);
			int i = 0;
			while(item == null){//class was already added
				id = new OWLClassImpl(IRI.create(subClass.toStringID() + i++));
				item = addItem(id);
			}
			if(!subClass.equals(id)){
				cls2OriginalCls.put(id, subClass);
			}
			
			item.getItemProperty(LABEL_PROPERTY).setValue(renderer.render(subClass));
			item.getItemProperty(UNSATISFIABLE_PROPERTY).setValue(unsatisfiableClasses.contains(subClass));
			item.getItemProperty(INSTANCES_SIZE_PROPERTY).setValue(reasoner.getInstances(subClass, false).getFlattened().size());
			
			setParent(id, cls);
			addSubClasses(id);
		}
	}
}
