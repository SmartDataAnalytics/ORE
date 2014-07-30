package org.aksw.ore.component;

import java.util.Set;

import org.aksw.ore.ORESession;
import org.aksw.ore.rendering.Renderer;
import org.dllearner.core.owl.Individual;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Table;

public class IndividualsTable extends Table {
	
	private BeanItemContainer<Individual> container = new BeanItemContainer<Individual>(Individual.class);
	private Renderer renderer = ORESession.getRenderer();
	
	public IndividualsTable() {
		super();
//		setContainerDataSource(container);
		addContainerProperty("name", String.class, null);
		setVisibleColumns(new Object[]{"name"});
		setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
	}
	
	public IndividualsTable(String caption) {
		super(caption);
		setContainerDataSource(container);
	}
	
	public void setIndividuals(Set<Individual> individuals){
//		container.addAll(individuals);
		container.removeAllItems();
		for (Individual individual : individuals) {
			addItem(individual).getItemProperty("name").setValue(renderer.render(individual));
		}
	}

}
