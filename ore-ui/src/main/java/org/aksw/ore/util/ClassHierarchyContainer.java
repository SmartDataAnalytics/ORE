/**
 * 
 */
package org.aksw.ore.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.aksw.ore.ORESession;
import org.aksw.ore.rendering.Renderer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;

/**
 * @author Lorenz Buehmann
 *
 */
public class ClassHierarchyContainer extends HierarchicalContainer{
	
	public enum ClassHierarchy {
		ASSERTED, INFERRED
	}
	
	private static final OWLClass OWL_THING = new OWLClassImpl(OWLRDFVocabulary.OWL_THING.getIRI());
	private static final OWLClass OWL_NOTHING = new OWLClassImpl(OWLRDFVocabulary.OWL_NOTHING.getIRI());
	
	public static final String LABEL_PROPERTY = "label";
	public static final String UNSATISFIABLE_PROPERTY = "unsatisfiable";
	public static final String INSTANCES_SIZE_PROPERTY = "instances";
	
	private OWLReasoner reasoner;
	private Renderer renderer = ORESession.getRenderer();
	private Map<OWLClass, OWLClass> cls2OriginalCls = new HashMap<OWLClass, OWLClass>();
	
	private Set<OWLClass> unsatisfiableClasses;
	private boolean inferred;
	
	public ClassHierarchyContainer(OWLOntology ontology, OWLReasonerFactory reasonerFactory) {
		this(reasonerFactory.createNonBufferingReasoner(ontology));
	}
	
	public ClassHierarchyContainer(OWLReasoner reasoner) {
		this.reasoner = reasoner;
		unsatisfiableClasses = ORESession.getOWLReasoner().getUnsatisfiableClasses().getEntitiesMinusBottom();
		
		addContainerProperty(LABEL_PROPERTY, String.class, null);
		addContainerProperty(UNSATISFIABLE_PROPERTY, Boolean.class, false);
		addContainerProperty(INSTANCES_SIZE_PROPERTY, Integer.class, 0);
		
		inferred = !(reasoner instanceof StructuralReasoner);
		
		//start with owl:Thing
		addSubClasses(OWL_THING);
		
		//if inferred show owl:Nothing as explicit node and add all unsatisfiable classes if exist
		if(inferred && !unsatisfiableClasses.isEmpty()){
			Item bottomItem = addItem(OWL_NOTHING);
			bottomItem.getItemProperty(LABEL_PROPERTY).setValue("owl:Nothing");
			for (OWLClass cls : unsatisfiableClasses) {
				Item item = addItem(cls);
				item.getItemProperty(LABEL_PROPERTY).setValue(renderer.render(cls));
				item.getItemProperty(UNSATISFIABLE_PROPERTY).setValue(true);
				item.getItemProperty(INSTANCES_SIZE_PROPERTY).setValue(0);
				setParent(cls, OWL_NOTHING);
				setChildrenAllowed(cls, false);
			}
		}
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
		if(inferred){
			subClasses.removeAll(unsatisfiableClasses);
		}
		
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
	
	public static void main(String[] args) throws Exception {
		
		String ontologyURL = "http://130.88.198.11/2008/iswc-modtut/materials/koala.owl";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = man.getOWLDataFactory();
		OWLOntology ontology = man.loadOntology(IRI.create(ontologyURL));
		OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		
		Node<OWLClass> topNode = reasoner.getTopClassNode();
        print(topNode, reasoner, 0);
	}
	
	private static void print(Node<OWLClass> parent, OWLReasoner reasoner,
            int depth) {
        // We don't want to print out the bottom node (containing owl:Nothing
        // and unsatisfiable classes) because this would appear as a leaf node
        // everywhere
        if (parent.isBottomNode()) {
            return;
        }
        // Print an indent to denote parent-child relationships
        printIndent(depth);
        // Now print the node (containing the child classes)
        printNode(parent);
        for (Node<OWLClass> child : reasoner.getSubClasses(
                parent.getRepresentativeElement(), true)) {
            // Recurse to do the children. Note that we don't have to worry
            // about cycles as there are non in the inferred class hierarchy
            // graph - a cycle gets collapsed into a single node since each
            // class in the cycle is equivalent.
            print(child, reasoner, depth + 1);
        }
    }

    private static void printIndent(int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("    ");
        }
    }

    private static void printNode(Node<OWLClass> node) {
        DefaultPrefixManager pm = new DefaultPrefixManager(
                "http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#");
        // Print out a node as a list of class names in curly brackets
        System.out.print("{");
        for (Iterator<OWLClass> it = node.getEntities().iterator(); it
                .hasNext();) {
            OWLClass cls = it.next();
            // User a prefix manager to provide a slightly nicer shorter name
            System.out.print(pm.getShortForm(cls));
            if (it.hasNext()) {
                System.out.print(" ");
            }
        }
        System.out.println("}");
    }
}
