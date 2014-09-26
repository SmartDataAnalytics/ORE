package org.aksw.ore.component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.Set;

import org.aksw.ore.ORESession;
import org.aksw.ore.rendering.Renderer;
import org.aksw.ore.util.AxiomScoreExplanationGenerator;
import org.dllearner.core.EvaluatedAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.ExternalResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class AxiomScoreExplanationDialog extends Window{

	private static final DecimalFormat df = new DecimalFormat("0.00%");

	Renderer renderer = ORESession.getRenderer();
	
	public AxiomScoreExplanationDialog(AxiomType<? extends OWLAxiom> axiomType, EvaluatedAxiom<OWLAxiom> axiom) {
		super("Explanation");
		setHeight("600px");
		setWidth("600px");
		setCloseShortcut(KeyCode.ESCAPE);
		
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		content.setSpacing(true);
		content.setMargin(new MarginInfo(false, true, false, true));
		setContent(content);
		
		//show the total score of the axiom
		String renderedAxiom = ORESession.getRenderer().renderHTML(axiom.getAxiom());
		Label label = new Label("<div><h3>Axiom: " + renderedAxiom + "</h3></div>"
				+ "<div><h3>Score:" + df.format(axiom.getScore().getAccuracy()) + "</h3></div>", ContentMode.HTML);
		content.addComponent(label);
		content.setExpandRatio(label, 0f);
		
		//show the explanation text
		label = new Label(AxiomScoreExplanationGenerator.getAccuracyDescription(axiom), ContentMode.HTML);
		content.addComponent(label);
		content.setExpandRatio(label, 0f);
		
		//get the positive and negative examples
		Set<OWLObject> positives = ORESession.getEnrichmentManager().getPositives(axiomType, axiom);
		Set<OWLObject> negatives = ORESession.getEnrichmentManager().getNegatives(axiomType, axiom);
		
		//show the positive examples, if exist
		if(positives.isEmpty()){
			label = new Label("<b>There are no negative examples.</b>", ContentMode.HTML);
			content.addComponent(label);
			content.setExpandRatio(label, 0f);
		} else {
			label = new Label("<b>Positive examples:</b>", ContentMode.HTML);
			content.addComponent(label);
			content.setExpandRatio(label, 0f);
			if(!positives.isEmpty()){
				final Table positiveExamplesTable = new Table();
				positiveExamplesTable.setSizeFull();
				positiveExamplesTable.addContainerProperty("element", String.class, null);
				positiveExamplesTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
				for (OWLObject element : positives) {
					positiveExamplesTable.addItem(element).getItemProperty("element").setValue(element.toString());
				}
				//show the URIs as links
				positiveExamplesTable.addGeneratedColumn("element", new Table.ColumnGenerator() {
		            public Component generateCell(Table source, Object itemId,
		                    Object columnId) {
		                OWLObject element = (OWLObject) itemId;
		                if(element instanceof OWLIndividual){
		                	OWLIndividual ind = (OWLIndividual) element;
		                	String decodedURI = ind.toString();
		                	try {
								decodedURI = URLDecoder.decode(ind.toStringID(), "UTF-8");
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
		                	Link link = new Link(ORESession.getRenderer().render(ind), new ExternalResource(ind.toStringID()));
		                	link.setTargetName("_blank");
			                return link;
		                } else {
		                	return new Label(ORESession.getRenderer().render(element));
		                }
		            }

		        });
				content.addComponent(positiveExamplesTable);
				content.setExpandRatio(positiveExamplesTable, 1f);
			}
		}
		
		//show the negative examples, if exist
		if(negatives.isEmpty()){
			label = new Label("<b>There are no negative examples.</b>", ContentMode.HTML);
			content.addComponent(label);
			content.setExpandRatio(label, 0f);
		} else {
			label = new Label("<b>Negative examples:</b>", ContentMode.HTML);
			content.addComponent(label);
			content.setExpandRatio(label, 0f);
			if(!negatives.isEmpty()){
				Table negativeExamplesTable = new Table();
				negativeExamplesTable.setSizeFull();
				negativeExamplesTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
				negativeExamplesTable.addContainerProperty("element", String.class, null);
				for (OWLObject element : negatives) {
					negativeExamplesTable.addItem(element).getItemProperty("element").setValue(element.toString());
				}
				//show the URIs as links
				negativeExamplesTable.addGeneratedColumn("element", new Table.ColumnGenerator() {
		            public Component generateCell(Table source, Object itemId,
		                    Object columnId) {
		                OWLObject element = (OWLObject) itemId;
		                if(element instanceof OWLIndividual){
		                	OWLIndividual ind = (OWLIndividual) element;
		                	String decodedURI = ind.toStringID();
		                	try {
								decodedURI = URLDecoder.decode(ind.toStringID(), "UTF-8");
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
		                	Link link = new Link(ORESession.getRenderer().render(ind), new ExternalResource(ind.toStringID()));
		                	link.setTargetName("_blank");
			                return link;
		                } else {
		                	return new Label(ORESession.getRenderer().render(element));
		                }
		            }

		        });
				content.addComponent(negativeExamplesTable);
				content.setExpandRatio(negativeExamplesTable, 1f);
			}
		}

	    Button closeButton = new Button("Close");
	    closeButton.addStyleName("wide");
	    closeButton.addStyleName("default");
	    closeButton.addClickListener(new ClickListener() {
	        @Override
	        public void buttonClick(ClickEvent event) {
	            close();
	        }
	    });
	    content.addComponent(closeButton);
	    content.setComponentAlignment(closeButton, Alignment.MIDDLE_CENTER);
	}
	

}
