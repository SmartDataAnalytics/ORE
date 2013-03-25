package org.aksw.mole.ore.widget;

import java.util.Collection;
import java.util.SortedSet;

import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.core.owl.Thing;

import com.vaadin.data.Container.Hierarchical;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.IndexedContainer;

public class ClassHierarchyContainer2 extends HierarchicalContainer{
	
	private AbstractReasonerComponent reasoner;
	private Description root = Thing.instance;
	
	public ClassHierarchyContainer2(AbstractReasonerComponent reasoner) {
		super();
		this.reasoner = reasoner;
		addItem(root);
	}
	

//	@Override
//	public int size() {
//		return reasoner.getAtomicConceptsList().size();
//	}
//
//
	@Override
	public Collection<?> getChildren(Object parent) {
		System.out.println("Computing children...");
		try {
			Collection<?> children = super.getChildren(parent);
			if(children == null){System.out.println("Compute new");
				SortedSet<Description> subClasses = reasoner.getSubClasses((Description)parent);
				System.out.println("Sub classes" + subClasses);
				for(Description sub : subClasses){
					Item i = addItem(sub);
					setChildrenAllowed(i, true);
					
					setParent(sub, parent);
				}
			} else {
				System.out.println("From cache: " + children);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return super.getChildren(parent);
	}
//
//	@Override
//	public Object getParent(Object itemId) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Collection<?> rootItemIds() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public boolean setParent(Object itemId, Object newParentId) throws UnsupportedOperationException {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public boolean areChildrenAllowed(Object itemId) {
//		return !reasoner.getSubClasses((NamedClass)itemId).isEmpty();
//	}
//
//	@Override
//	public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) throws UnsupportedOperationException {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public boolean isRoot(Object itemId) {
//		return true;
//	}
//
//	@Override
//	public boolean hasChildren(Object itemId) {
//		return !reasoner.getSubClasses((NamedClass)itemId).isEmpty();
//	}
//
//	@Override
//	public boolean removeItem(Object itemId) throws UnsupportedOperationException {
//		// TODO Auto-generated method stub
//		return false;
//	}

}
