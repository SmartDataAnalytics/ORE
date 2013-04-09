package org.aksw.mole.ore.widget;

import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.model.Knowledgebase;
import org.aksw.mole.ore.model.OWLOntologyKnowledgebase;
import org.aksw.mole.ore.model.SPARQLEndpointKnowledgebase;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.profiles.OWL2Profile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class KnowledgebaseInfoPanel extends VerticalLayout{
	
	public KnowledgebaseInfoPanel() {
		setSizeFull();
	}
	
	public void refresh(){
		System.out.println("Refreshing KB metrics");
		removeAllComponents();
		Knowledgebase kb = UserSession.getKnowledgebaseManager().getKnowledgebase();
		System.out.println(kb);
		if(kb != null){
			if(kb instanceof OWLOntologyKnowledgebase){
				visualizeOntology((OWLOntologyKnowledgebase) kb);
			} else if(kb instanceof SPARQLEndpointKnowledgebase){
				visualizeSPARQLEndpoint(((SPARQLEndpointKnowledgebase) kb).getEndpoint());
			}
			
		} 
	}
	
	private void visualizeSPARQLEndpoint(SparqlEndpoint endpoint){
		String htmlTable = 
				"<table>" +
		"<th colspan=\"2\"><b>SPARQL Endpoint</b></th>" +
				"<tr class=\"even\"><td>URL</td><td>" + endpoint.getURL().toString() + "</td></tr>";
		if(!endpoint.getDefaultGraphURIs().isEmpty()){
			htmlTable += "<tr class=\"odd\"><td>Default Graph URI</td><td>" + endpoint.getDefaultGraphURIs().iterator().next() + "</td></tr>";
		}
		htmlTable += "</table>";	
		Label label = new Label(htmlTable, Label.CONTENT_XHTML);
		label.addStyleName("sparql-metrics-table");
		label.setWidth(null);
		addComponent(label);
	}
	
	private void visualizeOntology(OWLOntologyKnowledgebase kb){
		OWLOntology ontology = kb.getOntology();
		int nrOfClasses = ontology.getClassesInSignature(true).size();
		int nrOfObjectProperties = ontology.getObjectPropertiesInSignature(true).size();
		int nrOfDataProperties = ontology.getDataPropertiesInSignature(true).size();
		int nrOfIndividuals = ontology.getIndividualsInSignature(true).size();
		OWLProfileReport report = new OWL2Profile().checkOntology(ontology);
		OWLProfile profile = report.getProfile();
		
		String htmlTable = 
				"<table>" +
				"<th colspan=\"2\"><b>OWL Ontology</b></th>" +
				"<tr class=\"even\"><td>#Classes</td><td>" + nrOfClasses + "</td></tr>" +
				"<tr class=\"odd\"><td>#ObjectProperties</td><td>" + nrOfObjectProperties + "</td></tr>" +
				"<tr class=\"even\"><td>#DataProperties</td><td>" + nrOfDataProperties + "</td></tr>" +
				"<tr class=\"odd\"><td>#Individuals</td><td>" + nrOfIndividuals + "</td></tr>" +
				"<tr class=\"even\"><td>OWL2 Profile</td><td>" + profile.getName() + "</td></tr>" +
				"<tr class=\"odd\"><td>Consistent</td><td>" + kb.isConsistent() + "</td></tr>";
		if(kb.isConsistent()){
			htmlTable += "<tr class=\"even\"><td>Coherent</td><td>" + kb.isCoherent() + "</td></tr>";
			if(!kb.isCoherent()){
				htmlTable += "<tr class=\"odd\"><td>#Unsatisfiable Classes</td><td>" + kb.getReasoner().getUnsatisfiableClasses().getEntitiesMinusBottom().size() + "</td></tr>";
			}
					
		}
		
		htmlTable += "</table>";
		
		Label label = new Label(htmlTable, Label.CONTENT_XHTML);
		label.addStyleName("ontology-metrics-table");
		label.setWidth(null);
		addComponent(label);
	}

}
