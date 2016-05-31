/**
 * 
 */
package org.aksw.ore.component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.SpinnerListModel;

import org.aksw.ore.ORESession;
import org.aksw.ore.manager.EnrichmentManager.EnrichmentProgressListener;
import org.dllearner.core.AxiomLearningProgressMonitor;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * @author Lorenz Buehmann
 *
 */
public class EnrichmentProgressDialog extends Window implements EnrichmentProgressListener, AxiomLearningProgressMonitor{
	
	private volatile boolean cancelled = false;
	private Label message;
	
	private Map<AxiomType<OWLAxiom>, Label> axiomType2Label;
	
	private List<AxiomType<OWLAxiom>> pendingAxiomTypes;
	private Collection<AxiomType<? extends OWLAxiom>> finishedAxiomTypes;
	private GridLayout grid;

	public EnrichmentProgressDialog(List<AxiomType<OWLAxiom>> axiomTypes) {
		super("Computing axioms...");
		this.pendingAxiomTypes = axiomTypes;
		finishedAxiomTypes = new HashSet<>(axiomTypes.size());
		
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
        axiomType2Label = new HashMap<>(axiomTypes.size());
        grid = new GridLayout(2, axiomTypes.size());
        grid.setWidth("100%");
        for (int i = 0; i < axiomTypes.size(); i++) {
			AxiomType<OWLAxiom> axiomType = axiomTypes.get(i);
			grid.addComponent(new Label(axiomType.getName()), 0, i);
			Label spinner = new Label();
			spinner.setWidthUndefined();
			spinner.addStyleName(ValoTheme.LABEL_SPINNER);
			grid.addComponent(spinner, 1, i);
			grid.setComponentAlignment(spinner, Alignment.MIDDLE_RIGHT);
			axiomType2Label.put(axiomType, spinner);
		}
        l.addComponent(grid);
//        for (AxiomType<OWLAxiom> axiomType : axiomTypes) {
//        	message = new Label(
//                    axiomType.getName() + "...");// + FontAwesome.SPINNER.getHtml(), ContentMode.HTML);
//            message.setImmediate(true);
//            grid.addComponent(message);
//            message.addStyleName(ValoTheme.LABEL_SPINNER);
////            message.addStyleName(ValoTheme.LABEL_SPINNER);
//            l.addComponent(message);
//            axiomType2Label.put(axiomType, message);
//		}
        

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
	public void onEnrichmentStarted(AxiomType<? extends OWLAxiom> axiomType) {
		
	}


	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.EnrichmentManager.EnrichmentProgressListener#onEnrichmentFinished(org.semanticweb.owlapi.model.AxiomType)
	 */
	@Override
	public void onEnrichmentFinished(final AxiomType<? extends OWLAxiom> axiomType) {
		finishedAxiomTypes.add(axiomType);
		
		UI.getCurrent().access(new Runnable() {
			
			@Override
			public void run() {
				Label label = axiomType2Label.get(axiomType);
				label.removeStyleName(ValoTheme.LABEL_SPINNER);
				label.setIcon(FontAwesome.THUMBS_UP);
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
	public void onEnrichmentFailed(AxiomType<? extends OWLAxiom> axiomType) {
	}

	/* (non-Javadoc)
	 * @see org.dllearner.core.AxiomLearningProgressMonitor#learningStarted(org.semanticweb.owlapi.model.AxiomType)
	 */
	@Override
	public void learningStarted(AxiomType<? extends OWLAxiom> axiomType) {
	}

	/* (non-Javadoc)
	 * @see org.dllearner.core.AxiomLearningProgressMonitor#learningStopped(org.semanticweb.owlapi.model.AxiomType)
	 */
	@Override
	public void learningStopped(final AxiomType<? extends OWLAxiom> axiomType) {
		finishedAxiomTypes.add(axiomType);
		
		UI.getCurrent().access(new Runnable() {
			
			@Override
			public void run() {
				Label label = axiomType2Label.get(axiomType);
				label.removeStyleName(ValoTheme.LABEL_SPINNER);
				label.setIcon(FontAwesome.SMILE_O);
				if(finishedAxiomTypes.size() == pendingAxiomTypes.size()){
					close();
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.dllearner.core.AxiomLearningProgressMonitor#learningProgressChanged(org.semanticweb.owlapi.model.AxiomType, int, int)
	 */
	@Override
	public void learningProgressChanged(AxiomType<? extends OWLAxiom> axiomType, int value, int max) {
	}

	/* (non-Javadoc)
	 * @see org.dllearner.core.AxiomLearningProgressMonitor#learningTaskBusy(org.semanticweb.owlapi.model.AxiomType)
	 */
	@Override
	public void learningTaskBusy(AxiomType<? extends OWLAxiom> axiomType) {
	}

	/* (non-Javadoc)
	 * @see org.dllearner.core.AxiomLearningProgressMonitor#learningFailed(org.semanticweb.owlapi.model.AxiomType)
	 */
	@Override
	public void learningFailed(final AxiomType<? extends OWLAxiom> axiomType) {
		finishedAxiomTypes.add(axiomType);
		
		UI.getCurrent().access(new Runnable() {
			
			@Override
			public void run() {
				Label label = axiomType2Label.get(axiomType);
				label.removeStyleName(ValoTheme.LABEL_SPINNER);
				label.setIcon(FontAwesome.FROWN_O);
				if(finishedAxiomTypes.size() == pendingAxiomTypes.size()){
					close();
				}
			}
		});
	}
}
