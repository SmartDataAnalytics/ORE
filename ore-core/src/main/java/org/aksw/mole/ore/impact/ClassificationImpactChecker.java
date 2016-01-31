package org.aksw.mole.ore.impact;

import com.clarkparsia.modularity.IncremantalReasonerFactory;
import com.clarkparsia.modularity.IncrementalReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.OWLAPIConfigProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassificationImpactChecker extends AbstractImpactChecker {

	private static final Logger logger = Logger.getLogger(ClassificationImpactChecker.class.getName());

	private IncrementalReasoner reasoner;
	protected OWLOntology ontology;
	protected OWLOntologyManager manager;
	protected OWLDataFactory factory;

	public ClassificationImpactChecker(IncrementalReasoner reasoner) {
		this.reasoner = reasoner;
		this.ontology = reasoner.getRootOntology();
		this.manager = ontology.getOWLOntologyManager();
		this.factory = manager.getOWLDataFactory();
	}

	@Override
	public Set<OWLOntologyChange> computeImpact(List<OWLOntologyChange> changes) {
		if (logger.isDebugEnabled()) {
			logger.debug("Computing classification based impact for\n" + changes);
		}

		long start = System.currentTimeMillis();
		Set<OWLOntologyChange> impact = new HashSet<OWLOntologyChange>();

		Set<OWLAxiom> entailmentsBefore = new HashSet<OWLAxiom>();
		Set<OWLAxiom> entailmentsAfter = new HashSet<OWLAxiom>();

		try {
			Set<OWLClass> unsatClasses = reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
			Set<OWLClass> subClasses;
			Set<OWLClass> equivalentClasses;

			for (OWLClass cl : ontology.getClassesInSignature()) {
				if (!isCanceled()) {
					if (!unsatClasses.contains(cl) && !cl.isOWLThing()) {
						subClasses = reasoner.getSubClasses(cl, false).getFlattened();
						equivalentClasses = reasoner.getEquivalentClasses(cl).getEntitiesMinus(cl);
						if (logger.isTraceEnabled()) {
							logger.trace("Subclasses for " + cl + " before applying changes:\n" + subClasses);
						}
						if (logger.isTraceEnabled()) {
							logger.trace("Equivalent classes for " + cl + " before applying changes:\n"
									+ equivalentClasses);
						}

						for (OWLClass sub : subClasses) {
							if (!sub.isOWLNothing() && !unsatClasses.contains(sub)) {
								entailmentsBefore.add(factory.getOWLSubClassOfAxiom(sub, cl));
							}
						}
						for (OWLClass equ : equivalentClasses) {
							if (!equ.isOWLNothing() && !unsatClasses.contains(equ)) {
								entailmentsBefore.add(factory.getOWLEquivalentClassesAxiom(equ, cl));
							}
						}
					}

				}
			}

			manager.applyChanges(changes);
			if(isCanceled()){
				return impact;
			}
			reasoner.classify();
			unsatClasses = reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();

			for (OWLClass cl : ontology.getClassesInSignature()) {
				if(isCanceled()){
					return impact;
				}
				if (!unsatClasses.contains(cl) && !cl.isOWLThing()) {
					subClasses = reasoner.getSubClasses(cl, false).getFlattened();
					equivalentClasses = reasoner.getEquivalentClasses(cl).getEntitiesMinus(cl);
					if (logger.isTraceEnabled()) {
						logger.trace("Subclasses for " + cl + " before applying changes:\n" + subClasses);
					}
					if (logger.isTraceEnabled()) {
						logger.trace("Equivalent classes for " + cl + " before applying changes:\n" + equivalentClasses);
					}

					for (OWLClass sub : subClasses) {
						if (!sub.isOWLNothing() && !unsatClasses.contains(sub)) {
							entailmentsAfter.add(factory.getOWLSubClassOfAxiom(sub, cl));
						}
					}
					for (OWLClass equ : equivalentClasses) {
						if (!equ.isOWLNothing() && !unsatClasses.contains(equ)) {
							entailmentsAfter.add(factory.getOWLEquivalentClassesAxiom(equ, cl));
						}
					}
				}

			}
			SetView<OWLAxiom> lostEntailments = Sets.difference(entailmentsBefore, entailmentsAfter);
			if (logger.isTraceEnabled()) {
				logger.trace("Lost entailments:\n" + lostEntailments);
			}
			SetView<OWLAxiom> addedEntailments = Sets.difference(entailmentsAfter, entailmentsBefore);
			if (logger.isTraceEnabled()) {
				logger.trace("Added entailments:\n" + addedEntailments);
			}

			for (OWLOntologyChange change : changes) {
				if (change instanceof RemoveAxiom) {
					lostEntailments.remove(change.getAxiom());
				}
			}
			for (OWLAxiom ax : lostEntailments) {
				impact.add(new RemoveAxiom(ontology, ax));
			}
			for (OWLAxiom ax : addedEntailments) {
				impact.add(new AddAxiom(ontology, ax));
			}
			manager.applyChanges(getInverseChanges(changes));
		} catch (OWLOntologyChangeException e) {
			logger.error(e);
		} catch (Exception e) {
			logger.error(e);
		}
		long end = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug("Operation took " + (end - start) + "ms");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Impact:\n" + impact);
		}
		return impact;
	}
	
	public static void main(String[] args) throws Exception{
		
		String ontologyURL = "http://owl.cs.manchester.ac.uk/repository/download?ontology=http://www.cs.manchester.ac.uk/owl/ontologies/tambis-patched.owl";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = man.getOWLDataFactory();
		OWLOntology ontology = man.loadOntology(IRI.create(ontologyURL));
		OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		IncrementalReasoner ic = IncremantalReasonerFactory.getInstance().createNonBufferingReasoner(ontology);
		ClassificationImpactChecker impactChecker = new ClassificationImpactChecker(ic);
		String axiomString = "metal EquivalentTo: chemical and (atomic-number some integer) and (atomic-number exactly 1 Thing)";
		BidirectionalShortFormProvider bidiShortFormProvider = new org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter(
				man, man.getImportsClosure(ontology), new SimpleShortFormProvider());
		OWLEntityChecker entityChecker = new ShortFormEntityChecker(bidiShortFormProvider);
		ManchesterOWLSyntaxParser parser = new ManchesterOWLSyntaxParserImpl(new OWLAPIConfigProvider(), dataFactory);
		parser.setOWLEntityChecker(entityChecker);
		parser.setDefaultOntology(ontology);
//		parser.parseOntology(ontology);
		OWLAxiom axiom = parser.parseAxiom();
		OWLOntologyChange change = new RemoveAxiom(ontology, axiom);
		impactChecker.getImpact(Collections.singletonList(change));
	}

}
