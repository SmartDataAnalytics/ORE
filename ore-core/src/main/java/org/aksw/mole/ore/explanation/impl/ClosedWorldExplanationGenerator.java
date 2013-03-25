package org.aksw.mole.ore.explanation.impl;

import java.util.HashSet;
import java.util.Set;

import org.aksw.mole.ore.explanation.api.Explanation;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.core.owl.Axiom;
import org.dllearner.core.owl.ClassAssertionAxiom;
import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.Intersection;
import org.dllearner.core.owl.Union;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.reasoning.FastInstanceChecker;
import org.dllearner.utilities.owl.DLLearnerDescriptionConvertVisitor;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class ClosedWorldExplanationGenerator {
	
	private FastInstanceChecker reasoner;
	
	public ClosedWorldExplanationGenerator(FastInstanceChecker reasoner) {
		this.reasoner = reasoner;
	}
	
	/**
	 * Returns a set of explanations why the individual belongs to the class by using
	 * the closed world assumption. Note that currently only class assertion axioms created by splitting in top-down
	 * direction union and intersection constructs are generated.
	 * @param desc
	 * @param ind
	 * @return
	 */
	public Set<Explanation> getEntailmentExplanation(Description desc, Individual ind){
		Set<Explanation> explanations = new HashSet<Explanation>();
		explanations.add(new ExplanationImpl(OWLAPIConverter.getOWLAPIAxiom(new ClassAssertionAxiom(desc, ind)), new HashSet<OWLAxiom>()));
		explainEntailment(desc, ind, explanations);
		return explanations;
	}
	
	private Set<Explanation> explainEntailment(Description desc, Individual ind, Set<Explanation> existingExplanations){
		if(desc instanceof Union){
			Set<Explanation> tmp = new HashSet<Explanation>();
			for(Description child : desc.getChildren()){
				if(reasoner.hasType(child, ind)){
					tmp.addAll(explainEntailment(child, ind, new HashSet<Explanation>(existingExplanations)));
				}
			}
			existingExplanations.clear();
			existingExplanations.addAll(tmp);
		} else if(desc instanceof Intersection){
			for(Description child : desc.getChildren()){
				if(reasoner.hasType(child, ind)){
					explainEntailment(child, ind, existingExplanations);
				}
			}
		} else {
			Set<Explanation> tmp = new HashSet<Explanation>();
			for(Explanation exp : existingExplanations){
				Set<OWLAxiom> axioms = new HashSet(exp.getAxioms());
				axioms.add(OWLAPIConverter.getOWLAPIAxiom(new ClassAssertionAxiom(desc, ind)));
				Explanation newExp = new ExplanationImpl(exp.getEntailment(), axioms);
				tmp.add(newExp);
			}
			existingExplanations.clear();
			existingExplanations.addAll(tmp);
			
		}
		return existingExplanations;
	}
	
	public Set<Explanation> getNonEntailmentExplanation(Description desc, Individual ind){
		Set<Explanation> explanations = new HashSet<Explanation>();
		explanations.add(new ExplanationImpl(OWLAPIConverter.getOWLAPIAxiom(new ClassAssertionAxiom(desc, ind)), new HashSet<OWLAxiom>()));
		explainNonEntailment(desc, ind, explanations);
		return explanations;
	}
	
	private Set<Explanation> explainNonEntailment(Description desc, Individual ind, Set<Explanation> existingExplanations){
		if(desc instanceof Union){
			Set<Explanation> tmp = new HashSet<Explanation>();
			for(Description child : desc.getChildren()){
				if(reasoner.hasType(child, ind)){
					tmp.addAll(explainEntailment(child, ind, new HashSet<Explanation>(existingExplanations)));
				}
			}
			existingExplanations.clear();
			existingExplanations.addAll(tmp);
		} else if(desc instanceof Intersection){
			for(Description child : desc.getChildren()){
				if(reasoner.hasType(child, ind)){
					explainEntailment(child, ind, existingExplanations);
				}
			}
		} else {
			Set<Explanation> tmp = new HashSet<Explanation>();
			for(Explanation exp : existingExplanations){
				Set<OWLAxiom> axioms = new HashSet(exp.getAxioms());
				axioms.add(OWLAPIConverter.getOWLAPIAxiom(new ClassAssertionAxiom(desc, ind)));
				Explanation newExp = new ExplanationImpl(exp.getEntailment(), axioms);
				tmp.add(newExp);
			}
			existingExplanations.clear();
			existingExplanations.addAll(tmp);
			
		}
		return existingExplanations;
	}
	
	public static void main(String[] args) throws Exception{
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory f = man.getOWLDataFactory();
		OWLOntology ont = man.createOntology();
		
		PrefixManager pm = new DefaultPrefixManager("http://example.org/");
		OWLClass a = f.getOWLClass("A", pm);
		OWLClass b = f.getOWLClass("B", pm);
		OWLClass c = f.getOWLClass("C", pm);
		OWLObjectProperty r = f.getOWLObjectProperty("r", pm);
		OWLIndividual x = f.getOWLNamedIndividual("x", pm);
		OWLIndividual y = f.getOWLNamedIndividual("y", pm);
		OWLIndividual z = f.getOWLNamedIndividual("z", pm);
		
		OWLAxiom ax = f.getOWLClassAssertionAxiom(a, x);
		man.addAxiom(ont, ax);
		
		ax = f.getOWLClassAssertionAxiom(b, x);
		man.addAxiom(ont, ax);
		
		ax = f.getOWLObjectPropertyAssertionAxiom(r, y, z);
		man.addAxiom(ont, ax);
		
		KnowledgeSource ks = new OWLAPIOntology(ont);
		ks.init();
		
		FastInstanceChecker checker = new FastInstanceChecker(ks);
		checker.init();
		
		OWLClassExpression expr1 = f.getOWLObjectIntersectionOf(
				a, 
				f.getOWLObjectAllValuesFrom(
						r, 
						c));
		
		OWLClassExpression expr2 = f.getOWLObjectIntersectionOf(
				a, 
				f.getOWLObjectUnionOf(
						f.getOWLObjectAllValuesFrom(
								r, 
								c),
						b)
				);
		
		OWLReasoner reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(ont);
		System.out.println(reasoner.isEntailed(f.getOWLClassAssertionAxiom(expr1, x)));
		
		Description d1 = DLLearnerDescriptionConvertVisitor.getDLLearnerDescription(expr1);
		Description d2 = DLLearnerDescriptionConvertVisitor.getDLLearnerDescription(expr2);
		Individual i = new Individual(x.asOWLNamedIndividual().toStringID());
		System.out.println(checker.hasType(d1, i));
		System.out.println(checker.hasType(d2, i));
		
		ClosedWorldExplanationGenerator expGen = new ClosedWorldExplanationGenerator(checker);
		expGen.getEntailmentExplanation(d1, i);
		expGen.getEntailmentExplanation(d2, i);
	}

}
