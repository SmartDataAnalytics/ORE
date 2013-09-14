/**
 * 
 */
package org.aksw.ore.component;

import java.util.HashSet;
import java.util.Set;

import org.aksw.ore.ORESession;
import org.aksw.ore.manager.ExplanationProgressMonitorExtended;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * @author Lorenz Buehmann
 *
 */
public class ExplanationProgressDialog extends Window implements ExplanationProgressMonitorExtended<OWLAxiom>{
	
	private volatile boolean cancelled = false;
	private Label message;
	private int maxNrOfExplanations;
	private Set<OWLClass> unsatisfiableClasses;

	public ExplanationProgressDialog(Set<OWLClass> unsatisfiableClasses, int maxNrOfExplanations) {
		super("Computing at most " + maxNrOfExplanations + " explanations...");
		
		this.unsatisfiableClasses = new HashSet<OWLClass>(unsatisfiableClasses);
		this.maxNrOfExplanations = maxNrOfExplanations;
		
		VerticalLayout l = new VerticalLayout();
        l.setWidth("400px");
        l.setMargin(true);
        l.setSpacing(true);
        setContent(l);
        
        setModal(true);
        setResizable(false);
        setDraggable(false);
        addStyleName("dialog");
        setClosable(false);

        message = new Label();
        message.setImmediate(true);
        l.addComponent(message);

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidth("100%");
        buttons.setSpacing(true);
        l.addComponent(buttons);


        Button cancel = new Button("Stop");
        cancel.addStyleName("small");
        cancel.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	cancelled = true;
                close();
            }
        });
        buttons.addComponent(cancel);
        buttons.setComponentAlignment(cancel, Alignment.MIDDLE_CENTER);

        cancel.focus();
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.explanation.api.ExplanationProgressMonitor#foundExplanation(org.semanticweb.owl.explanation.api.ExplanationGenerator, org.semanticweb.owl.explanation.api.Explanation, java.util.Set)
	 */
	@Override
	public void foundExplanation(ExplanationGenerator<OWLAxiom> expGen, final Explanation<OWLAxiom> explanation,
			final Set<Explanation<OWLAxiom>> allExplanations) {
		final OWLClass cls = ((OWLSubClassOfAxiom)explanation.getEntailment()).getSubClass().asOWLClass();
		if(allExplanations.size() <= maxNrOfExplanations){
			unsatisfiableClasses.remove(cls);
		}
		UI.getCurrent().access(new Runnable() {
			
			@Override
			public void run() {
				message.setValue("Found " + allExplanations.size() + " explanation" + 
			(allExplanations.size() == 1 ? "" : "s") + " for " + cls.toString());
				if(unsatisfiableClasses.isEmpty()){
					close();
				}
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see org.semanticweb.owl.explanation.api.ExplanationProgressMonitor#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.ExplanationProgressMonitorExtended#allExplanationsFound()
	 */
	@Override
	public void allExplanationsFound(Set<Explanation<OWLAxiom>> allExplanations) {
		if(!allExplanations.isEmpty()){
			final OWLClass cls = ((OWLSubClassOfAxiom)allExplanations.iterator().next().getEntailment()).getSubClass().asOWLClass();
			if(allExplanations.size() == maxNrOfExplanations){
				unsatisfiableClasses.remove(cls);
			}
			UI.getCurrent().access(new Runnable() {
				
				@Override
				public void run() {
					if(unsatisfiableClasses.isEmpty()){
						close();
					}
				}
			});
		}
	}

}
