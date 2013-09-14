/**
 * 
 */
package org.aksw.ore.component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aksw.ore.ORESession;
import org.aksw.ore.manager.ExplanationProgressMonitorExtended;
import org.aksw.ore.manager.EnrichmentManager.EnrichmentProgressListener;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

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
public class EnrichmentProgressDialog extends Window implements EnrichmentProgressListener{
	
	private volatile boolean cancelled = false;
	private Label message;
	
	private Map<AxiomType<OWLAxiom>, Label> axiomType2Label;
	
	private Collection<AxiomType<OWLAxiom>> pendingAxiomTypes;
	private Collection<AxiomType<OWLAxiom>> finishedAxiomTypes;

	public EnrichmentProgressDialog(Collection<AxiomType<OWLAxiom>> axiomTypes) {
		super("Computing axioms...");
		this.pendingAxiomTypes = axiomTypes;
		finishedAxiomTypes = new HashSet<AxiomType<OWLAxiom>>(axiomTypes.size());
		
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
        
        axiomType2Label = new HashMap<AxiomType<OWLAxiom>, Label>(axiomTypes.size());
        for (AxiomType<OWLAxiom> axiomType : axiomTypes) {
        	message = new Label(
                    axiomType.getName() + "...");
            message.setImmediate(true);
            l.addComponent(message);
            axiomType2Label.put(axiomType, message);
		}
        

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
	 * @see com.vaadin.ui.AbstractComponent#attach()
	 */
	@Override
	public void attach() {
		super.attach();
		ORESession.getEnrichmentManager().addEnrichmentProgressListener(this);
	}
	
	/* (non-Javadoc)
	 * @see com.vaadin.ui.AbstractComponent#detach()
	 */
	@Override
	public void detach() {
		super.detach();
		ORESession.getEnrichmentManager().removeEnrichmentProgressListener(this);
	}

	
	/* (non-Javadoc)
	 * @see com.vaadin.ui.Window#close()
	 */
	@Override
	public void close() {
		super.close();
	}


	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.EnrichmentManager.EnrichmentProgressListener#onEnrichmentStarted(org.semanticweb.owlapi.model.AxiomType)
	 */
	@Override
	public void onEnrichmentStarted(AxiomType<OWLAxiom> axiomType) {
	}


	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.EnrichmentManager.EnrichmentProgressListener#onEnrichmentFinished(org.semanticweb.owlapi.model.AxiomType)
	 */
	@Override
	public void onEnrichmentFinished(final AxiomType<OWLAxiom> axiomType) {
		finishedAxiomTypes.add(axiomType);
		
		UI.getCurrent().access(new Runnable() {
			
			@Override
			public void run() {
				axiomType2Label.get(axiomType).setValue(axiomType.getName() + " done.");
				if(finishedAxiomTypes.size() == pendingAxiomTypes.size()){
					close();
				}
			}
		});
	}


	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.EnrichmentManager.EnrichmentProgressListener#onEnrichmentFailed(org.semanticweb.owlapi.model.AxiomType)
	 */
	@Override
	public void onEnrichmentFailed(AxiomType<OWLAxiom> axiomType) {
	}
}
