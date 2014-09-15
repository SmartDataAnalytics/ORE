package org.aksw.mole.ore.explanation.impl;

import java.util.HashSet;
import java.util.Set;

import org.aksw.mole.ore.explanation.api.Explanation;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.reasoning.FastInstanceChecker;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class ClosedWorldExplanationGenerator {
	
	private FastInstanceChecker reasoner;
	OWLDataFactory df = new OWLDataFactoryImpl();
	
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
	public Set<Explanation> getEntailmentExplanation(OWLClassExpression desc, OWLIndividual ind){
		Set<Explanation> explanations = new HashSet<Explanation>();
		explanations.add(new ExplanationImpl(df.getOWLClassAssertionAxiom(desc, ind), new HashSet<OWLAxiom>()));
		explainEntailment(desc, ind, explanations);
		return explanations;
	}
	
	private Set<Explanation> explainEntailment(OWLClassExpression desc, OWLIndividual ind, Set<Explanation> existingExplanations){
		if(desc instanceof OWLObjectUnionOf){
			Set<Explanation> tmp = new HashSet<Explanation>();
			for(OWLClassExpression child : ((OWLObjectUnionOf) desc).getOperands()){
				if(reasoner.hasType(child, ind)){
					tmp.addAll(explainEntailment(child, ind, new HashSet<Explanation>(existingExplanations)));
				}
			}
			existingExplanations.clear();
			existingExplanations.addAll(tmp);
		} else if(desc instanceof OWLObjectIntersectionOf){
			for(OWLClassExpression child : ((OWLObjectIntersectionOf) desc).getOperands()){
				if(reasoner.hasType(child, ind)){
					explainEntailment(child, ind, existingExplanations);
				}
			}
		} else {
			Set<Explanation> tmp = new HashSet<Explanation>();
			for(Explanation exp : existingExplanations){
				Set<OWLAxiom> axioms = new HashSet<OWLAxiom>(exp.getAxioms());
				axioms.add(df.getOWLClassAssertionAxiom(desc, ind));
				Explanation newExp = new ExplanationImpl(exp.getEntailment(), axioms);
				tmp.add(newExp);
			}
			existingExplanations.clear();
			existingExplanations.addAll(tmp);
			
		}
		return existingExplanations;
	}
	
	public Set<Explanation> getNonEntailmentExplanation(OWLClassExpression desc, OWLIndividual ind){
		Set<Explanation> explanations = new HashSet<Explanation>();
		explanations.add(new ExplanationImpl(df.getOWLClassAssertionAxiom(desc, ind), new HashSet<OWLAxiom>()));
		explainNonEntailment(desc, ind, explanations);
		return explanations;
	}
	
	private Set<Explanation> explainNonEntailment(OWLClassExpression desc, OWLIndividual ind, Set<Explanation> existingExplanations){
		if(desc instanceof OWLObjectUnionOf){
			Set<Explanation> tmp = new HashSet<Explanation>();
			for(OWLClassExpression child : ((OWLObjectUnionOf) desc).getOperands()){
				if(reasoner.hasType(child, ind)){
					tmp.addAll(explainEntailment(child, ind, new HashSet<Explanation>(existingExplanations)));
				}
			}
			existingExplanations.clear();
			existingExplanations.addAll(tmp);
		} else if(desc instanceof OWLObjectIntersectionOf){
			for(OWLClassExpression child : ((OWLObjectIntersectionOf) desc).getOperands()){
				if(reasoner.hasType(child, ind)){
					explainEntailment(child, ind, existingExplanations);
				}
			}
		} else {
			Set<Explanation> tmp = new HashSet<Explanation>();
			for(Explanation exp : existingExplanations){
				Set<OWLAxiom> axioms = new HashSet<OWLAxiom>(exp.getAxioms());
				axioms.add(df.getOWLClassAssertionAxiom(desc, ind));
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
		
		System.out.println(checker.hasType(expr1, x));
		System.out.println(checker.hasType(expr2, x));
		
		ClosedWorldExplanationGenerator expGen = new ClosedWorldExplanationGenerator(checker);
		expGen.getEntailmentExplanation(expr1, x);
		expGen.getEntailmentExplanation(expr2, x);
	}

}
