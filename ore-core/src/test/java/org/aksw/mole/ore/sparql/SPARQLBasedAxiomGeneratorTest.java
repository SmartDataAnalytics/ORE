package org.aksw.mole.ore.sparql;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.aksw.mole.ore.sparql.generator.SPARQLBasedEntityRelatedAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.SPARQLBasedGeneralAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.entity.ClassAssertionAxiomForClassGenerator;
import org.aksw.mole.ore.sparql.generator.entity.PropertyAssertionAxiomForPropertyGenerator;
import org.aksw.mole.ore.sparql.generator.entity.SubClassOfAxiomForClassGenerator;
import org.aksw.mole.ore.sparql.generator.generic.AsymmetricPropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.ClassAssertionAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.DataPropertyDomainAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.DataPropertyRangeAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.DisjointClassesAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.DomainAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.EquivalentClassesAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.EquivalentPropertiesAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.FunctionalPropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.InverseFunctionalPropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.InverseOfAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.IrreflexivePropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.ObjectPropertyDomainAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.ObjectPropertyRangeAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.PropertyAssertionAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.RangeAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.ReflexivePropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.SubClassOfAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.SubPropertyOfAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.SymmetricPropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.TransitivePropertyAxiomGenerator;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLProperty;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class SPARQLBasedAxiomGeneratorTest {
	
	private final OWLDataFactory dataFactory = new OWLDataFactoryImpl();
	private SparqlEndpointKS ks;
	private List<Class<? extends SPARQLBasedGeneralAxiomGenerator>> generalAxiomGenerators = 
			new ArrayList<Class<? extends SPARQLBasedGeneralAxiomGenerator>>();
	private List<Class<? extends SPARQLBasedEntityRelatedAxiomGenerator<OWLClass>>> classRelatedAxiomGenerators =
			new ArrayList<Class<? extends SPARQLBasedEntityRelatedAxiomGenerator<OWLClass>>>();
	private List<Class<? extends SPARQLBasedEntityRelatedAxiomGenerator<OWLProperty>>> propertyRelatedAxiomGenerators =
			new ArrayList<Class<? extends SPARQLBasedEntityRelatedAxiomGenerator<OWLProperty>>>();

	@Before
	public void setUp() throws Exception {
		ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia());
		
		//TBox axioms
		generalAxiomGenerators.add(SubClassOfAxiomGenerator.class);
		generalAxiomGenerators.add(EquivalentClassesAxiomGenerator.class);
		generalAxiomGenerators.add(DisjointClassesAxiomGenerator.class);
		//RBox axioms
		generalAxiomGenerators.add(SubPropertyOfAxiomGenerator.class);
		generalAxiomGenerators.add(EquivalentPropertiesAxiomGenerator.class);
		generalAxiomGenerators.add(DomainAxiomGenerator.class);
		generalAxiomGenerators.add(ObjectPropertyDomainAxiomGenerator.class);
		generalAxiomGenerators.add(DataPropertyDomainAxiomGenerator.class);
		generalAxiomGenerators.add(RangeAxiomGenerator.class);
		generalAxiomGenerators.add(ObjectPropertyRangeAxiomGenerator.class);
		generalAxiomGenerators.add(DataPropertyRangeAxiomGenerator.class);
		generalAxiomGenerators.add(FunctionalPropertyAxiomGenerator.class);
		generalAxiomGenerators.add(InverseFunctionalPropertyAxiomGenerator.class);
		generalAxiomGenerators.add(SymmetricPropertyAxiomGenerator.class);
		generalAxiomGenerators.add(AsymmetricPropertyAxiomGenerator.class);
		generalAxiomGenerators.add(ReflexivePropertyAxiomGenerator.class);
		generalAxiomGenerators.add(IrreflexivePropertyAxiomGenerator.class);
		generalAxiomGenerators.add(TransitivePropertyAxiomGenerator.class);
		generalAxiomGenerators.add(InverseOfAxiomGenerator.class);
		//ABox axioms
		generalAxiomGenerators.add(ClassAssertionAxiomGenerator.class);
		generalAxiomGenerators.add(PropertyAssertionAxiomGenerator.class);
		
		classRelatedAxiomGenerators.add(ClassAssertionAxiomForClassGenerator.class);
		classRelatedAxiomGenerators.add(SubClassOfAxiomForClassGenerator.class);
		
		propertyRelatedAxiomGenerators.add(PropertyAssertionAxiomForPropertyGenerator.class);
	}

	@Test
	public void testNextAxioms() {
		for (Class<? extends SPARQLBasedGeneralAxiomGenerator> cls : generalAxiomGenerators) {
			try {
				SPARQLBasedGeneralAxiomGenerator generator = cls.getConstructor(SparqlEndpointKS.class).newInstance(ks);
				Set<OWLAxiom> axioms = generator.nextAxioms();
				System.out.println("Found " + axioms.size() + " axioms using " + generator.getClass().getSimpleName() + 
						(!axioms.isEmpty() ? ", e.g. " + axioms.iterator().next() : "."));
				System.out.println("All axioms loaded: " + !generator.hasNext());
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Test
	public void testNextAxiomsForClass() {
		OWLClass cls = dataFactory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Book"));
		for (Class<? extends SPARQLBasedEntityRelatedAxiomGenerator<OWLClass>> generatorClass : classRelatedAxiomGenerators) {
			try {
				SPARQLBasedEntityRelatedAxiomGenerator<OWLClass> generator = generatorClass.getConstructor(SparqlEndpointKS.class).newInstance(ks);
				Set<OWLAxiom> axioms = generator.nextAxioms(cls);
				System.out.println("Found " + axioms.size() + " axioms using " + generator.getClass().getSimpleName() + 
						(!axioms.isEmpty() ? ", e.g. " + axioms.iterator().next() : "."));
				System.out.println("All axioms loaded: " + !generator.hasNext(cls));
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testNextAxiomsForProperty() {
		OWLProperty prop = dataFactory.getOWLObjectProperty(IRI.create("http://dbpedia.org/ontology/birthPlace"));
		for (Class<? extends SPARQLBasedEntityRelatedAxiomGenerator<OWLProperty>> generatorClass : propertyRelatedAxiomGenerators) {
			try {
				SPARQLBasedEntityRelatedAxiomGenerator<OWLProperty> generator = generatorClass.getConstructor(SparqlEndpointKS.class).newInstance(ks);
				Set<OWLAxiom> axioms = generator.nextAxioms(prop);
				System.out.println("Found " + axioms.size() + " axioms using " + generator.getClass().getSimpleName() + 
						(!axioms.isEmpty() ? ", e.g. " + axioms.iterator().next() : "."));
				System.out.println("All axioms loaded: " + !generator.hasNext(prop));
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
	}

}
