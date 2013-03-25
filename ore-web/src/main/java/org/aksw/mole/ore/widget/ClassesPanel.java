package org.aksw.mole.ore.widget;

import com.vaadin.data.util.FilesystemContainer;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

public class ClassesPanel extends VerticalLayout{
	
	private Tree classesTree;
	private HierarchicalContainer container = new HierarchicalContainer();
	
	public ClassesPanel() {
		initUI();
	}
	
	private void initUI(){
		classesTree = new Tree();
		classesTree.setContainerDataSource(container);
	}
	
	public void setClasses(HierarchicalContainer container){
		this.container = container;
	}

}
