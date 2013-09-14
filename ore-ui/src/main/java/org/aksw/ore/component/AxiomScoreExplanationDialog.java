package org.aksw.ore.component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.Set;

import org.aksw.ore.ORESession;
import org.aksw.ore.util.ScoreExplanationPattern;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.KBElement;
import org.semanticweb.owlapi.model.AxiomType;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.ExternalResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class AxiomScoreExplanationDialog extends Window{
	
	private DecimalFormat df = new DecimalFormat("0.00%");
	
	public AxiomScoreExplanationDialog(AxiomType axiomType, EvaluatedAxiom axiom) {
		super("Explanation why " + axiom.getAxiom().toString() + " was learned.");
		setHeight("600px");
		setWidth("400px");
		setCloseShortcut(KeyCode.ESCAPE, null);
		setClosable(false);
		
		VerticalLayout mainLayout = new VerticalLayout();
		setContent(mainLayout);
		
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		content.setHeight(null);
		
		Panel p = new Panel(content);
		p.setSizeFull();
		mainLayout.addComponent(p);
		mainLayout.setExpandRatio(p, 1f);
		
		//show the total score of the axiom
		Label label = new Label("<h2>Score: " + df.format(axiom.getScore().getAccuracy()) + "</h2>", ContentMode.HTML);
		content.addComponent(label);
		content.setExpandRatio(label, 0f);
		
		//show the explanation text
		label = new Label(ScoreExplanationPattern.getAccuracyDescription(axiomType, axiom), ContentMode.HTML);
		label.setWidth("90%");
		content.addComponent(label);
		content.setExpandRatio(label, 0f);
		
		//get the positive and negative examples
		Set<KBElement> positives = ORESession.getEnrichmentManager().getPositives(axiomType, axiom);
		Set<KBElement> negatives = ORESession.getEnrichmentManager().getNegatives(axiomType, axiom);
		
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
				positiveExamplesTable.setWidth("90%");
				positiveExamplesTable.setHeight("100%");
				positiveExamplesTable.addContainerProperty("element", String.class, null);
				positiveExamplesTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
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
				negativeExamplesTable.setWidth("90%");
				negativeExamplesTable.setHeight("100%");
				negativeExamplesTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
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
				content.addComponent(negativeExamplesTable);
				content.setExpandRatio(negativeExamplesTable, 1f);
			}
		}
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
	    mainLayout.addComponent(footer);
	}
	

}
