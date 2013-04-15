package org.aksw.mole.ore.widget;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.Set;

import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.util.ScoreExplanationPattern;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.KBElement;
import org.semanticweb.owlapi.model.AxiomType;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class AxiomScoreExplanationDialog extends Window{
	
	private DecimalFormat df = new DecimalFormat("0.00%");
	
	public AxiomScoreExplanationDialog(AxiomType axiomType, EvaluatedAxiom axiom) {
		super("Explanation why " + axiom.getAxiom().toString() + " was learned.");
		setHeight("400px");
		setWidth("400px");
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		setContent(mainLayout);
		
		//show the total score of the axiom
		Label label = new Label("<h2>Score: " + df.format(axiom.getScore().getAccuracy()) + "</h2>", Label.CONTENT_XHTML);
		mainLayout.addComponent(label);
		mainLayout.setExpandRatio(label, 0f);
		
		//show the explanation text
		label = new Label(ScoreExplanationPattern.getAccuracyDescription(axiomType, axiom), Label.CONTENT_XHTML);
		label.setWidth("90%");
		mainLayout.addComponent(label);
		mainLayout.setExpandRatio(label, 0f);
		
		//get the positive and negative examples
		Set<KBElement> positives = UserSession.getEnrichmentManager().getPositives(axiomType, axiom);
		Set<KBElement> negatives = UserSession.getEnrichmentManager().getNegatives(axiomType, axiom);
		
		//show the positive examples, if exist
		if(positives.isEmpty()){
			label = new Label("<b>There are no negative examples.</b>", Label.CONTENT_XHTML);
			mainLayout.addComponent(label);
			mainLayout.setExpandRatio(label, 0f);
		} else {
			label = new Label("<b>Positive examples:</b>", Label.CONTENT_XHTML);
			mainLayout.addComponent(label);
			mainLayout.setExpandRatio(label, 0f);
			if(!positives.isEmpty()){
				final Table positiveExamplesTable = new Table();
				positiveExamplesTable.setWidth("90%");
				positiveExamplesTable.setHeight("100%");
				positiveExamplesTable.addContainerProperty("element", String.class, null);
				positiveExamplesTable.setColumnHeaderMode(Table.COLUMN_HEADER_MODE_HIDDEN);
				for (KBElement element : positives) {
					positiveExamplesTable.addItem(element).getItemProperty("element").setValue(element.toString());
				}
				//show the URIs as links
				positiveExamplesTable.addGeneratedColumn("element", new Table.ColumnGenerator() {
		            public Component generateCell(Table source, Object itemId,
		                    Object columnId) {
		                KBElement element = (KBElement) itemId;
		                if(element instanceof Individual){
		                	Individual ind = (Individual) element;
		                	String decodedURI = ind.getName();
		                	try {
								decodedURI = URLDecoder.decode(ind.getName(), "UTF-8");
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
		                	Link link = new Link(decodedURI, new ExternalResource(ind.getName()));
		                	link.setTargetName("_blank");
			                return link;
		                } else {
		                	return new Label(element.toString());
		                }
		            }

		        });
				mainLayout.addComponent(positiveExamplesTable);
				mainLayout.setExpandRatio(positiveExamplesTable, 1f);
			}
		}
		
		//show the negative examples, if exist
		if(negatives.isEmpty()){
			label = new Label("<b>There are no negative examples.</b>", Label.CONTENT_XHTML);
			mainLayout.addComponent(label);
			mainLayout.setExpandRatio(label, 0f);
		} else {
			label = new Label("<b>Negative examples:</b>", Label.CONTENT_XHTML);
			mainLayout.addComponent(label);
			mainLayout.setExpandRatio(label, 0f);
			if(!negatives.isEmpty()){
				Table negativeExamplesTable = new Table();
				negativeExamplesTable.setWidth("90%");
				negativeExamplesTable.setHeight("100%");
				negativeExamplesTable.setColumnHeaderMode(Table.COLUMN_HEADER_MODE_HIDDEN);
				negativeExamplesTable.addContainerProperty("element", String.class, null);
				for (KBElement element : negatives) {
					negativeExamplesTable.addItem(element).getItemProperty("element").setValue(element.toString());
				}
				//show the URIs as links
				negativeExamplesTable.addGeneratedColumn("element", new Table.ColumnGenerator() {
		            public Component generateCell(Table source, Object itemId,
		                    Object columnId) {
		                KBElement element = (KBElement) itemId;
		                if(element instanceof Individual){
		                	Individual ind = (Individual) element;
		                	String decodedURI = ind.getName();
		                	try {
								decodedURI = URLDecoder.decode(ind.getName(), "UTF-8");
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
		                	Link link = new Link(decodedURI, new ExternalResource(ind.getName()));
		                	link.setTargetName("_blank");
			                return link;
		                } else {
		                	return new Label(element.toString());
		                }
		            }

		        });
				mainLayout.addComponent(negativeExamplesTable);
				mainLayout.setExpandRatio(negativeExamplesTable, 1f);
			}
		}
	}
	

}
