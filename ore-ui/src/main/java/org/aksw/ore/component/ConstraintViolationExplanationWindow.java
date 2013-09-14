/**
 * 
 */
package org.aksw.ore.component;

import org.semanticweb.owlapi.model.OWLAxiom;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * @author Lorenz Buehmann
 *
 */
public class ConstraintViolationExplanationWindow extends Window{
	
	/**
	 * 
	 */
	public ConstraintViolationExplanationWindow(OWLAxiom constraint, String explanationString) {
		VerticalLayout l = new VerticalLayout();
	    l.setSpacing(true);

	    setCaption(constraint.toString());
	    setContent(l);
	    center();
	    setCloseShortcut(KeyCode.ESCAPE, null);
	    setResizable(false);
	    setClosable(false);

	    addStyleName("no-vertical-drag-hints");
	    addStyleName("no-horizontal-drag-hints");

	    Label explanation = new Label(explanationString);
	    l.addComponent(explanation);
	    
	    HorizontalLayout footer = new HorizontalLayout();
	    footer.addStyleName("footer");
	    footer.setWidth("100%");
	    footer.setMargin(true);

	    Button ok = new Button("Close");
	    ok.addStyleName("wide");
	    ok.addStyleName("default");
	    ok.addClickListener(new ClickListener() {
	        @Override
	        public void buttonClick(ClickEvent event) {
	            close();
	        }
	    });
	    footer.addComponent(ok);
	    footer.setComponentAlignment(ok, Alignment.TOP_RIGHT);
	    l.addComponent(footer);
	}

	

}
