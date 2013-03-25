package org.aksw.mole.ore.widget;

import java.util.Set;

import org.aksw.mole.ore.model.Individual;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Table;

public class IndividualsTable extends Table {
	
	private BeanItemContainer<Individual> container = new BeanItemContainer<Individual>(Individual.class);
	
	public IndividualsTable() {
		super();
		setContainerDataSource(container);
		setVisibleColumns(new Object[]{"label"});
		setColumnHeaderMode(COLUMN_HEADER_MODE_HIDDEN);
	}
	
	public IndividualsTable(String caption) {
		super(caption);
		setContainerDataSource(container);
	}
	
	public void setIndividuals(Set<Individual> individuals){
		container.addAll(individuals);
	}

}
