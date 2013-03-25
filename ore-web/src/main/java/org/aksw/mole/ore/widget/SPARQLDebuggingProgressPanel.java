package org.aksw.mole.ore.widget;

import org.aksw.mole.ore.model.SPARQLProgress;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class SPARQLDebuggingProgressPanel extends VerticalLayout{
	
	private Label disjointClassProgress;
	private Label subClassProgress;
	private Label equivalentClassProgress;
	
//	private Label disjointPropertyProgress;
	private Label subPropertyProgress;
	private Label equivalentPropertyProgress;
	private Label propertyDomainProgress;
	private Label propertyRangeProgress;
	
	private Label functionalityProgress;
	private Label transitivityProgress;
	
	private String progressBarTemplate = "" +
			"<div style='width: 250px; text-align:center;'>$type</div>" +
			"<div class=\"progress_bar\">" +
			"<strong>&nbsp;</strong>" +
			"<span style=\"width: #width#%;\">&nbsp;</span>" +
			"</div>";
			
			
//			"<div id=\"progress-outer\" >" +
//			"<div id=\"progress-inner\" style='width: #width#%;'>" +
//			"<strong>$value/$max</strong>" +
//			"</div>" +
//			"</div>​";
	
	public SPARQLDebuggingProgressPanel() {
		setSizeFull();
		disjointClassProgress = new Label(progressBarTemplate.replace("#width#","0").replace("$type", "DisjointClass Axioms"),Label.CONTENT_XHTML);
		disjointClassProgress.setWidth(null);
		addComponent(disjointClassProgress);
		setComponentAlignment(disjointClassProgress, Alignment.MIDDLE_CENTER);
		
		subClassProgress = new Label(progressBarTemplate.replace("#width#","0").replace("$type", "SubClassOf Axioms"),Label.CONTENT_XHTML) ;
		subClassProgress.setWidth(null);
		addComponent(subClassProgress);
		setComponentAlignment(subClassProgress, Alignment.MIDDLE_CENTER);
		
		equivalentClassProgress = new Label(progressBarTemplate.replace("#width#","0").replace("$type", "EquivalentClass Axioms"),Label.CONTENT_XHTML) ;
		equivalentClassProgress.setWidth(null);
		addComponent(equivalentClassProgress);
		setComponentAlignment(equivalentClassProgress, Alignment.MIDDLE_CENTER);
		
//		disjointPropertyProgress = new Label(progressBarTemplate.replace("#width#","0"),Label.CONTENT_XHTML);
//		addComponent(disjointPropertyProgress);
		subPropertyProgress = new Label(progressBarTemplate.replace("#width#","0").replace("$type", "SubPropertyOf Axioms"),Label.CONTENT_XHTML) ;
		subPropertyProgress.setWidth(null);
		addComponent(subPropertyProgress);
		setComponentAlignment(subPropertyProgress, Alignment.MIDDLE_CENTER);
		
		equivalentPropertyProgress = new Label(progressBarTemplate.replace("#width#","0").replace("$type", "EquivalentProperty Axioms"),Label.CONTENT_XHTML) ;
		equivalentPropertyProgress.setWidth(null);
		addComponent(equivalentPropertyProgress);
		setComponentAlignment(equivalentPropertyProgress, Alignment.MIDDLE_CENTER);
		
		propertyDomainProgress = new Label(progressBarTemplate.replace("#width#","0").replace("$type", "PropertyDomain Axioms"),Label.CONTENT_XHTML);
		propertyDomainProgress.setWidth(null);
		addComponent(propertyDomainProgress);
		setComponentAlignment(propertyDomainProgress, Alignment.MIDDLE_CENTER);
		
		propertyRangeProgress = new Label(progressBarTemplate.replace("#width#","0").replace("$type", "PropertyRange Axioms"),Label.CONTENT_XHTML) ;
		propertyRangeProgress.setWidth(null);
		addComponent(propertyRangeProgress);
		setComponentAlignment(propertyRangeProgress, Alignment.MIDDLE_CENTER);
		
		functionalityProgress = new Label(progressBarTemplate.replace("#width#","0").replace("$type", "FunctionalProperty Axioms"),Label.CONTENT_XHTML) ;
		functionalityProgress.setWidth(null);
		addComponent(functionalityProgress);
		setComponentAlignment(functionalityProgress, Alignment.MIDDLE_CENTER);
		
		transitivityProgress = new Label(progressBarTemplate.replace("#width#","0").replace("$type", "TransitiveProperty Axioms"),Label.CONTENT_XHTML) ;
		transitivityProgress.setWidth(null);
		addComponent(transitivityProgress);
		setComponentAlignment(transitivityProgress, Alignment.MIDDLE_CENTER);
	}
	
	public void update(SPARQLProgress progress){
		progressBarTemplate = "" +
				"<div style='width: 250px; text-align:center;'>$type</div>" +
				"<div class=\"progress_bar\">" +
				"<strong>$value/$max</strong>" +
				"<span style=\"width: #width#%;\">&nbsp;</span>" +
				"</div>";
		
		disjointClassProgress.setValue(progressBarTemplate.
				replace("#width#", String.valueOf(progress.getDisjointClassMax() == 0 ? 100 : (int)progress.getDisjointClassValue()/(double)progress.getDisjointClassMax() * 100)).
				replace("$value", String.valueOf(progress.getDisjointClassValue())).
				replace("$max", String.valueOf(progress.getDisjointClassMax())).
				replace("$type", "DisjointClass Axioms"));
		subClassProgress.setValue(progressBarTemplate.
				replace("#width#", String.valueOf(progress.getSubClassMax() == 0 ? 100 : (int)progress.getSubClassValue()/(double)progress.getSubClassMax() * 100)).
				replace("$value", String.valueOf(progress.getSubClassValue())).
				replace("$max", String.valueOf(progress.getSubClassMax())).
				replace("$type", "SubClassOf Axioms"));
		equivalentClassProgress.setValue(progressBarTemplate.
				replace("#width#", String.valueOf(progress.getEquivalentClassMax() == 0 ? 100 : (int)progress.getEquivalentClassValue()/(double)progress.getEquivalentClassMax() * 100)).
				replace("$value", String.valueOf(progress.getEquivalentClassValue())).
				replace("$max", String.valueOf(progress.getEquivalentClassMax())).
				replace("$type", "EquivalentClass Axioms"));
		
//		disjointClassProgress.setValue(progressBarTemplate.replace("#width#", String.valueOf((int)progress.getDisjointPropertyValue()/progress.getDisjointPropertyMax())));
		subPropertyProgress.setValue(progressBarTemplate.
				replace("#width#", String.valueOf(progress.getSubPropertyMax() == 0 ? 100 : (int)progress.getSubPropertyValue()/(double)progress.getSubPropertyMax() * 100)).
				replace("$value", String.valueOf(progress.getSubPropertyValue())).
				replace("$max", String.valueOf(progress.getSubPropertyMax())).
				replace("$type", "SubPropertyOf Axioms"));
		equivalentPropertyProgress.setValue(progressBarTemplate.
				replace("#width#", String.valueOf(progress.getEquivalentPropertyMax() == 0 ? 100 : (int)progress.getEquivalentPropertyValue()/(double)progress.getEquivalentPropertyMax() * 100)).
				replace("$value", String.valueOf(progress.getEquivalentPropertyValue())).
				replace("$max", String.valueOf(progress.getEquivalentPropertyMax())).
				replace("$type", "EquivalentProperty Axioms"));
		propertyDomainProgress.setValue(progressBarTemplate.
				replace("#width#", String.valueOf(progress.getPropertyDomainMax() == 0 ? 100 : (int)progress.getPropertyDomainValue()/(double)progress.getPropertyDomainMax() * 100)).
				replace("$value", String.valueOf(progress.getPropertyDomainValue())).
				replace("$max", String.valueOf(progress.getPropertyDomainMax())).
				replace("$type", "PropertyDomain Axioms"));
		propertyRangeProgress.setValue(progressBarTemplate.
				replace("#width#", String.valueOf(progress.getPropertyRangeMax() == 0 ? 100 : (int)progress.getPropertyRangeValue()/(double)progress.getPropertyRangeMax() * 100)).
				replace("$value", String.valueOf(progress.getPropertyRangeValue())).
				replace("$max", String.valueOf(progress.getPropertyRangeMax())).
				replace("$type", "PropertyRange Axioms"));
		
		functionalityProgress.setValue(progressBarTemplate.
				replace("#width#", String.valueOf(progress.getFunctionalPropertyMax() == 0 ? 100 : (int)progress.getFunctionalPropertyValue()/(double)progress.getFunctionalPropertyMax() * 100)).
				replace("$value", String.valueOf(progress.getFunctionalPropertyValue())).
				replace("$max", String.valueOf(progress.getFunctionalPropertyMax())).
				replace("$type", "FunctionalProperty Axioms"));
		
		transitivityProgress.setValue(progressBarTemplate.
				replace("#width#", String.valueOf(progress.getTransitivePropertyMax() == 0 ? 100 : (int)progress.getTransitivePropertyValue()/(double)progress.getTransitivePropertyMax() * 100)).
				replace("$value", String.valueOf(progress.getTransitivePropertyValue())).
				replace("$max", String.valueOf(progress.getTransitivePropertyMax())).
				replace("$type", "TransitiveProperty Axioms"));
		
	}

}
