/**
 * 
 */
package org.aksw.ore.model;

import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

/**
 * @author Lorenz Buehmann
 * 
 */
public class SPARQLKnowledgebaseStats {
	int owlClassCnt = -1;
	int owlObjectPropertyCnt = -1;
	int owlDataPropertyCnt = -1;
	private Set<String> classes;
	private Set<String> objectProperties;
	private Set<String> dataProperties;

	public SPARQLKnowledgebaseStats(int owlClassCnt, int owlObjectPropertyCnt, int owlDataPropertyCnt) {
		this.owlClassCnt = owlClassCnt;
		this.owlObjectPropertyCnt = owlObjectPropertyCnt;
		this.owlDataPropertyCnt = owlDataPropertyCnt;
	}
	
	public SPARQLKnowledgebaseStats(Set<String> classes, Set<String> objectProperties, Set<String> dataProperties) {
		this.classes = classes;
		this.objectProperties = objectProperties;
		this.dataProperties = dataProperties;
		
		this.owlClassCnt = classes.size();
		this.owlObjectPropertyCnt = objectProperties.size();
		this.owlDataPropertyCnt = dataProperties.size();
	}

	/**
	 * @return the owlClassCnt
	 */
	public int getOwlClassCnt() {
		return owlClassCnt;
	}

	/**
	 * @return the owlObjectPropertyCnt
	 */
	public int getOwlObjectPropertyCnt() {
		return owlObjectPropertyCnt;
	}

	/**
	 * @return the owlDataPropertyCnt
	 */
	public int getOwlDataPropertyCnt() {
		return owlDataPropertyCnt;
	}
	
	/**
	 * @return the classes
	 */
	public Set<String> getClasses() {
		return classes;
	}
	
	/**
	 * @return the objectProperties
	 */
	public Set<String> getObjectProperties() {
		return objectProperties;
	}
	
	/**
	 * @return the dataProperties
	 */
	public Set<String> getDataProperties() {
		return dataProperties;
	}
	
	public static void main(String[] args) throws Exception {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI iri = IRI.create("http://dbpedia.org/resource/Patricia_Cornwell");
		OWLOntology onto = manager.loadOntology(iri);
		System.out.println(onto.getLogicalAxioms());
		PelletReasoner r  = PelletReasonerFactory.getInstance().createNonBufferingReasoner(onto);
		r.prepareReasoner();
		r.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass class1 = factory.getOWLClass(IRI.create("http://dbpedia.org/class/yago/WritersFromFlorida"));
		System.out.println(r.getSuperClasses(class1, false));
	}
}
