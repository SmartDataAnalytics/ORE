package org.aksw.ore.component;

import java.util.Set;

import org.aksw.ore.ORESession;
import org.aksw.ore.rendering.Renderer;
import org.semanticweb.owlapi.model.OWLIndividual;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Table;

public class IndividualsTable extends Table {
	
	private BeanItemContainer<OWLIndividual> container = new BeanItemContainer<>(OWLIndividual.class);
	private Renderer renderer = ORESession.getRenderer();
	
	public IndividualsTable() {
		super();
//		setContainerDataSource(container);
		addContainerProperty("name", String.class, null);
		setVisibleColumns("name");
		setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
	}
	
	public IndividualsTable(String caption) {
		super(caption);
		setContainerDataSource(container);
	}
	
	public void setIndividuals(Set<OWLIndividual> individuals){
//		container.addAll(individuals);
		container.removeAllItems();
		for (OWLIndividual individual : individuals) {
			addItem(individual).getItemProperty("name").setValue(renderer.render(individual));
		}
	}

}
