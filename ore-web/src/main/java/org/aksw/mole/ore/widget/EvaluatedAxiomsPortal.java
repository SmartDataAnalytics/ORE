package org.aksw.mole.ore.widget;

import java.util.Collection;

import org.dllearner.core.EvaluatedAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.vaadin.sasha.portallayout.PortalLayout;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;

public class EvaluatedAxiomsPortal extends PortalLayout{
	
	public EvaluatedAxiomsPortal(AxiomType axiomType, Collection<EvaluatedAxiom> axioms) {
		setWidth("100%");
        setMargin(true);
        
        Table table = new EvaluatedAxiomsTable(axiomType, axioms);
        table.setCaption(axiomType.getName());
        addComponent(table);
        
        HorizontalLayout header = new HorizontalLayout();
        header.addComponent(new CheckBox());
        setHeaderComponent(table, header);
        setClosable(table, false);
	}

}
