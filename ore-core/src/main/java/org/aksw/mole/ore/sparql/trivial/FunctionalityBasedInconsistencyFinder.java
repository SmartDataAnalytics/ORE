package org.aksw.mole.ore.sparql.trivial;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.mole.ore.sparql.TimeOutException;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLProperty;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class FunctionalityBasedInconsistencyFinder extends AbstractTrivialInconsistencyFinder {
	
	public FunctionalityBasedInconsistencyFinder(SparqlEndpointKS ks) {
		super(ks);
		
		query = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"CONSTRUCT " +
				"{?p a owl:FunctionalProperty. ?s ?p ?o1. ?s ?p ?o2.} " +
				"WHERE " +
				"{?p a owl:FunctionalProperty. ?s ?p ?o1. ?s ?p ?o2. FILTER(?o1 != ?o2)} ";
		
		
//		query = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
//				"CONSTRUCT " +
//				"{?p a owl:FunctionalProperty. ?s ?p ?o1. ?s ?p ?o2. ?p a ?type.} " +
//				"WHERE " +
//				"{?p a owl:FunctionalProperty. ?s ?p ?o1. ?s ?p ?o2. FILTER(?o1 != ?o2)" +
//				"OPTIONAL{?p a ?type. FILTER(?type=owl:ObjectProperty || ?type=owl:DatatypeProperty)}" +
//				"} ";
	}
	
	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		Set<OWLAxiom> functionalityAxioms = AxiomType.getAxiomsOfTypes(axiomsToIgnore, AxiomType.FUNCTIONAL_OBJECT_PROPERTY, AxiomType.FUNCTIONAL_DATA_PROPERTY);
		Set<OWLProperty> properties = new TreeSet<OWLProperty>();
		for (OWLAxiom axiom : functionalityAxioms) {
			properties.addAll(axiom.getObjectPropertiesInSignature());
			properties.addAll(axiom.getDataPropertiesInSignature());
		}
		if(!properties.isEmpty()){
			filter = "FILTER(?p NOT IN(";
			for (OWLProperty property : properties) {
				filter += "<" + property.toStringID() + ">" + ",";
			}
			filter = filter.substring(0, filter.length()-1);
			filter += "))";
		}
	}
	
	@Override
	public Set<OWLAxiom> getInconsistentFragment() throws TimeOutException {
		//we override it because the OWL API is currently not smart enough to parse correctly and always 
		//handles the properties as AnnotationProperty if not explicitly stated otherwise
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		//add filter if exist
		String queryString = query;
		if(filter != null && !filter.isEmpty()){
			queryString = query.trim();
			queryString = query.substring(0, query.lastIndexOf('}'));
			queryString += filter + "}";
		}
		System.out.println(queryString);
		Query q = QueryFactory.create(queryString);
		q.setLimit(limit);
		int offset = filterToOffset.containsKey(filter) ? filterToOffset.get(filter) : 0;
		q.setOffset(offset);
		Model model = executeConstructQuery(q);
		OWLOntology ontology = convert(model);
		
		Set<OWLAxiom> oldAxioms = ontology.getAxioms();
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		OWLDataFactory factory = new OWLDataFactoryImpl();
		Set<OWLObjectProperty> objectProperties = new HashSet<OWLObjectProperty>();
		Set<OWLDataProperty> dataProperties = new HashSet<OWLDataProperty>();
		for (OWLAxiom axiom : oldAxioms) {
			if(axiom.getAxiomType() == AxiomType.ANNOTATION_ASSERTION){
				OWLAnnotationAssertionAxiom annotation = (OWLAnnotationAssertionAxiom)axiom;
				OWLIndividual subject = factory.getOWLNamedIndividual((IRI) annotation.getSubject());
				if(annotation.getValue() instanceof OWLLiteral) {
                    OWLLiteral value = (OWLLiteral) annotation.getValue();
                    OWLDataProperty property = factory.getOWLDataProperty(annotation.getProperty().getIRI());
                    dataProperties.add(property);
                    newAxioms.add(factory.getOWLDataPropertyAssertionAxiom(property, subject, value));
				} else {
					OWLIndividual object = factory.getOWLNamedIndividual((IRI) annotation.getValue());
					OWLObjectProperty property = factory.getOWLObjectProperty(annotation.getProperty().getIRI());
					objectProperties.add(property);
                    newAxioms.add(factory.getOWLObjectPropertyAssertionAxiom(property, subject, object));
				}
			}
		}
		for(OWLAxiom axiom : AxiomType.getAxiomsOfTypes(oldAxioms, AxiomType.FUNCTIONAL_OBJECT_PROPERTY)){
			OWLObjectProperty property = ((OWLFunctionalObjectPropertyAxiom)axiom).getProperty().asOWLObjectProperty();
			newAxioms.add(factory.getOWLFunctionalObjectPropertyAxiom(property));
		}
		
		for(OWLAxiom axiom : AxiomType.getAxiomsOfTypes(oldAxioms, AxiomType.FUNCTIONAL_DATA_PROPERTY)){
			OWLDataProperty property = ((OWLFunctionalDataPropertyAxiom)axiom).getProperty().asOWLDataProperty();
			newAxioms.add(factory.getOWLFunctionalDataPropertyAxiom(property));
		}
		return newAxioms;
	}
}
