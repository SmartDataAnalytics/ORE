package org.aksw.mole.ore.impact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class StructuralImpactChecker extends AbstractImpactChecker {

	protected OWLOntology ontology;
	protected OWLReasoner reasoner;
	protected OWLOntologyManager manager;
	protected OWLDataFactory factory;

	public StructuralImpactChecker(OWLReasoner reasoner) {
		this.reasoner = reasoner;
		this.ontology = reasoner.getRootOntology();
		this.manager = ontology.getOWLOntologyManager();
		this.factory = manager.getOWLDataFactory();
	}

	@Override
	public Set<OWLOntologyChange> computeImpact(List<OWLOntologyChange> changes) {
		Set<OWLOntologyChange> impact = new HashSet<OWLOntologyChange>();

		Map<OWLClass, Set<OWLClass>> subsumptionHierarchyUp = new HashMap<OWLClass, Set<OWLClass>>();
		Map<OWLClass, Set<OWLClass>> subsumptionHierarchyDown = new HashMap<OWLClass, Set<OWLClass>>();

		Set<OWLAxiom> possibleLosts = new HashSet<OWLAxiom>();
		Set<OWLAxiom> realLosts = new HashSet<OWLAxiom>();
		OWLAxiom axiom;
		for (OWLOntologyChange change : changes) {
			if(!isCanceled()){
				if (change instanceof RemoveAxiom) {
					axiom = change.getAxiom();
					if (axiom instanceof OWLSubClassOfAxiom) {
						OWLSubClassOfAxiom subAx = (OWLSubClassOfAxiom) axiom;
						if (subAx.getSubClass() instanceof OWLClass && subAx.getSuperClass() instanceof OWLClass) {
							OWLClass sub = (OWLClass) subAx.getSubClass();
							OWLClass sup = (OWLClass) subAx.getSuperClass();
							Set<OWLClass> descendants = subsumptionHierarchyDown.get(sub);
							if (descendants == null) {
								descendants = reasoner.getSubClasses(sub, true).getFlattened();
								subsumptionHierarchyDown.put(sub, descendants);
							}
							for (OWLClass desc : descendants) {
								Set<OWLClass> ancestors = subsumptionHierarchyUp.get(sub);
								if (ancestors == null) {
									ancestors = reasoner.getSuperClasses(sup, true).getFlattened();
									subsumptionHierarchyUp.put(sup, ancestors);
								}
								for (OWLClass anc : ancestors) {
									if (!anc.isOWLThing() && !desc.isOWLNothing()) {
										OWLSubClassOfAxiom ax = factory.getOWLSubClassOfAxiom(desc, anc);
										possibleLosts.add(ax);
									}
								}
							}
						}
					} else if (axiom instanceof OWLDisjointClassesAxiom) {

						Set<OWLClassExpression> disjointClasses = ((OWLDisjointClassesAxiom) axiom).getClassExpressions();
						boolean complex = false;
						for (OWLClassExpression dis : disjointClasses) {
							if (dis.isAnonymous()) {
								complex = true;
								break;
							}
						}
						if (!complex) {

							List<OWLClassExpression> disjoints = new ArrayList<OWLClassExpression>(disjointClasses);

							for (OWLClassExpression dis : new ArrayList<OWLClassExpression>(disjoints)) {
								if (!dis.isOWLNothing()) {
									disjoints.remove(dis);

									Set<? extends OWLClassExpression> descendants = reasoner.getSubClasses(
											dis.asOWLClass(), true).getFlattened();

									descendants.removeAll(reasoner.getEquivalentClasses(factory.getOWLNothing())
											.getEntities());
									// if (enableImpactUnsat) {
									// descendants.addAll(((OWLClass)
									// dis).getSubClasses(ontology));
									// }
									for (OWLClassExpression desc1 : descendants) {

										if (!desc1.isOWLNothing()) {
											if (!reasoner.getEquivalentClasses((desc1)).contains(factory.getOWLNothing())) {
												for (OWLClassExpression desc2 : disjoints) {

													if (!desc2.equals(desc1)) {
														Set<OWLClassExpression> newDis = new HashSet<OWLClassExpression>();
														newDis.add(desc1);
														newDis.add(desc2);
														OWLDisjointClassesAxiom ax = factory
																.getOWLDisjointClassesAxiom(newDis);
														possibleLosts.add(ax);
													}
												}
											}
										}
									}

									disjoints.add(dis);

								}
							}
							// return result;
						}
					} else if (axiom instanceof OWLObjectPropertyDomainAxiom) {
						OWLObjectPropertyDomainAxiom pd = (OWLObjectPropertyDomainAxiom) axiom;

						if (pd.getDomain() instanceof OWLClass) {
							OWLClass dom = (OWLClass) pd.getDomain();
							Set<OWLClass> superClasses = reasoner.getSuperClasses(dom, true).getFlattened();
							for (OWLClass sup : superClasses) {

								OWLObjectPropertyDomainAxiom ax = factory.getOWLObjectPropertyDomainAxiom(pd.getProperty(),
										sup);
								possibleLosts.add(ax);
							}
						}
					} else if (axiom instanceof OWLDataPropertyDomainAxiom) {
						OWLDataPropertyDomainAxiom pd = (OWLDataPropertyDomainAxiom) axiom;

						if (pd.getDomain() instanceof OWLClass) {
							OWLClass dom = (OWLClass) pd.getDomain();
							Set<OWLClass> superClasses = reasoner.getSuperClasses(dom, true).getFlattened();
							for (OWLClass sup : superClasses) {

								OWLDataPropertyDomainAxiom ax = factory
										.getOWLDataPropertyDomainAxiom(pd.getProperty(), sup);
								possibleLosts.add(ax);
							}
						}
					} else if (axiom instanceof OWLObjectPropertyRangeAxiom) {
						OWLObjectPropertyRangeAxiom pd = (OWLObjectPropertyRangeAxiom) axiom;

						if (pd.getRange() instanceof OWLClass) {
							OWLClass ran = (OWLClass) pd.getRange();
							Set<OWLClass> superClasses = reasoner.getSuperClasses(ran, true).getFlattened();
							for (OWLClass sup : superClasses) {

								OWLObjectPropertyRangeAxiom ax = factory.getOWLObjectPropertyRangeAxiom(pd.getProperty(),
										sup);
								possibleLosts.add(ax);
							}
						}
					}
				}
			}
		}

		for (OWLAxiom ax : possibleLosts) {
			if(!isCanceled()){
				try {
					manager.applyChanges(changes);
					if (!reasoner.isEntailed(ax)) {
						realLosts.add(ax);
					}
					manager.applyChanges(getInverseChanges(changes));
				} catch (OWLOntologyChangeException e) {
					e.printStackTrace();
				}
			}
		}
		for (OWLAxiom lost : realLosts) {
			impact.add(new RemoveAxiom(ontology, lost));
		}
		return impact;
	}

}
