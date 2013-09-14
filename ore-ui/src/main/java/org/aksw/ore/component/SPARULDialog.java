/**
 * 
 */
package org.aksw.ore.component;

import java.util.List;

import org.aksw.mole.ore.sparql.SPARULTranslator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


/**
 * @author Lorenz Buehmann
 *
 */
public class SPARULDialog extends Window{
	
	public SPARULDialog(List<OWLOntologyChange> changes) {
		super("SPARUL Statements");
		setWidth("1000px");
		setHeight("400px");
		center();
		
		try {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = man.createOntology();
			SPARULTranslator translator = new SPARULTranslator(man, ontology, false);
			
			String sparulString = translator.translate(changes);
			
			VerticalLayout content = new VerticalLayout();
			setContent(content);
			
			content.addComponent(new Label(sparulString, ContentMode.PREFORMATTED));
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
	}

}
