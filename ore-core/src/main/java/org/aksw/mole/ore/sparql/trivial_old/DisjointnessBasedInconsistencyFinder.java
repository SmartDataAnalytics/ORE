package org.aksw.mole.ore.sparql.trivial_old;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.mole.ore.sparql.TimeOutException;
import org.apache.log4j.Logger;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class DisjointnessBasedInconsistencyFinder extends AbstractTrivialInconsistencyFinder {
	
	enum State {
		CLASS_ASSERTION , DOMAIN_VIOLATION , RANGE_VIOLATION;
	}
	
	
	private static final Logger logger = Logger
			.getLogger(DisjointnessBasedInconsistencyFinder.class.getName());
	
	private Set<OWLDisjointClassesAxiom> disjointnessAxioms;
	private boolean initialized = false;
	private State currentState = State.CLASS_ASSERTION;
	
	private Set<OWLDisjointClassesAxiom> assertedDisjointClassesAxioms;
	
	private Multimap<OWLDisjointClassesAxiom, OWLDisjointClassesAxiom> sourceAxioms = HashMultimap.create();
	
	Set<OWLDisjointClassesAxiom> allDisjointClassesAxioms = new TreeSet<>();

	private Map<OWLClass, Set<OWLSubClassOfAxiom>> path2Root;
	
	public DisjointnessBasedInconsistencyFinder(SparqlEndpointKS ks) {
		super(ks);
		
		currentState = State.CLASS_ASSERTION;
	}
	
	/**
	 * @param ks
	 */
	public DisjointnessBasedInconsistencyFinder(SparqlEndpointKS ks, Set<Explanation<OWLAxiom>> explanations) {
		super(ks, explanations);
	}
	
	private void init(){
		if(!initialized){
			disjointnessAxioms = getDisjointnessAxioms();
			initialized = true;
		}
	}

	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial_old.AbstractTrivialInconsistencyFinder#run()
	 */
	@Override
	public void run(boolean resume) {
//		explanations = new HashSet<>();
		fireInfoMessage("Analyzing disjointness...");
		init();
		
		int i = 0;
		int total = disjointnessAxioms.size();
		for (OWLDisjointClassesAxiom ax : disjointnessAxioms) {
			if(!terminationCriteriaSatisfied()){
				fireTraceMessage("Analyzing candidate: " + ax);
				//check A(x),B(x)
				if(!terminationCriteriaSatisfied() && currentState == State.CLASS_ASSERTION){
					analyzeClassAssertionViolation(ax);
					currentState = State.DOMAIN_VIOLATION;
				}
				
				//check A(x), Dom(p, B), p(x,y)
				if(!terminationCriteriaSatisfied() && currentState == State.DOMAIN_VIOLATION){
					analyzeDomainViolation(ax);
					currentState = State.RANGE_VIOLATION;
				}
				
				//check A(x), Ran(p, B), p(y,x)
				if(!terminationCriteriaSatisfied() && currentState == State.RANGE_VIOLATION){
					analyzeRangeViolation(ax);
					currentState = State.CLASS_ASSERTION;
				}
				fireProgressUpdate(++i, total);
				fireNumberOfConflictsFound(explanations.size());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.trivial.AbstractTrivialInconsistencyFinder#getInconsistentFragment()
	 */
	@Override
	public Set<OWLAxiom> getInconsistentFragment() throws TimeOutException {
		return null;
	}
	
	private void analyzeClassAssertionViolation(OWLDisjointClassesAxiom axiom){
		List<OWLClassExpression> classes = axiom.getClassExpressionsAsList();
		OWLClass cls1 = classes.get(0).asOWLClass();
		OWLClass cls2 = classes.get(1).asOWLClass();
		Set<OWLAxiom> disjointnessExplanation = getDisjointnessAxiomExplanation(axiom);
		String query = "SELECT ?x WHERE {?x a <" + cls1.toStringID() + ">. ?x a <" + cls2.toStringID() + ">}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			OWLIndividual ind = dataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("x").getURI()));
			OWLAxiom ax1 = dataFactory.getOWLClassAssertionAxiom(cls1, ind);
			OWLAxiom ax2 = dataFactory.getOWLClassAssertionAxiom(cls2, ind);
			Set<OWLAxiom> justification = Sets.newHashSet(ax1, ax2);
			justification.addAll(disjointnessExplanation);
			explanations.add(new Explanation<OWLAxiom>(inconsistencyEntailment, justification));
		}
//		allExplanations.addAll(explanations);
		qe.close();
	}
	
	private void analyzeDomainViolation(OWLDisjointClassesAxiom axiom){
		List<OWLClassExpression> classes = axiom.getClassExpressionsAsList();
		OWLClass cls1 = classes.get(0).asOWLClass();
		OWLClass cls2 = classes.get(1).asOWLClass();
		Set<String> propertiesWithDomain = getPropertiesWithDomain(cls2);
		if(!propertiesWithDomain.isEmpty()){
			Set<OWLAxiom> disjointnessExplanation = getDisjointnessAxiomExplanation(axiom);
			String filter = "FILTER(?p IN (" + Joiner.on(',').join(propertiesWithDomain) +  "))";
//			query = "SELECT ?p ?x ?y WHERE {?x a <" + cls1.toStringID() + ">. ?p rdfs:domain <" + cls2.toStringID() + ">. ?x ?p ?y}";
			query = "SELECT ?p ?x ?y WHERE {?x a <" + cls1.toStringID() + ">. ?x ?p ?y. " + filter + "}";
			QueryExecution qe = qef.createQueryExecution(query);
			ResultSet rs = qe.execSelect();
			QuerySolution qs;
			while (rs.hasNext()) {
				qs = rs.next();
				OWLIndividual indX = dataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("x").getURI()));
				OWLAxiom ax1 = dataFactory.getOWLClassAssertionAxiom(cls1, indX);
				OWLAxiom ax2;
				OWLAxiom ax3;
				if(qs.get("y").isURIResource()){
					OWLObjectProperty prop = dataFactory.getOWLObjectProperty(IRI.create(qs.getResource("p").getURI()));
					ax2 = dataFactory.getOWLObjectPropertyDomainAxiom(prop, cls2);
					OWLIndividual indY = dataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("y").getURI()));
					ax3 = dataFactory.getOWLObjectPropertyAssertionAxiom(prop, indX, indY);
				} else {
					OWLDataProperty prop = dataFactory.getOWLDataProperty(IRI.create(qs.getResource("p").getURI()));
					ax2 = dataFactory.getOWLDataPropertyDomainAxiom(prop, cls2);
					OWLLiteral lit = getOWLLiteral(qs.getLiteral("y"));
					ax3 = dataFactory.getOWLDataPropertyAssertionAxiom(prop, indX, lit);
				}
				Set<OWLAxiom> justification = Sets.newHashSet(ax1, ax2, ax3);
				justification.addAll(disjointnessExplanation);
				explanations.add(new Explanation<OWLAxiom>(inconsistencyEntailment, justification));
			}
			qe.close();
//			allExplanations.addAll(explanations);
		}
	}
	
	private void analyzeRangeViolation(OWLDisjointClassesAxiom axiom){
		List<OWLClassExpression> classes = axiom.getClassExpressionsAsList();
		OWLClass cls1 = classes.get(0).asOWLClass();
		OWLClass cls2 = classes.get(1).asOWLClass();
		Set<String> propertiesWithRange = getPropertiesWithRange(cls2);
		if(!propertiesWithRange.isEmpty()){
			Set<OWLAxiom> disjointnessExplanation = getDisjointnessAxiomExplanation(axiom);
			String filter = "FILTER(?p IN (" + Joiner.on(',').join(propertiesWithRange) +  "))";
			query = "SELECT ?p ?x ?y WHERE {?x a <" + cls1.toStringID() + ">. ?y ?p ?x. " + filter + "}";
//			query = "SELECT ?p ?x ?y WHERE {?x a <" + cls1.toStringID() + ">. ?p rdfs:range <" + cls2.toStringID() + ">. ?y ?p ?x}";
			QueryExecution qe = qef.createQueryExecution(query);
			ResultSet rs = qe.execSelect();
			QuerySolution qs;
			while (rs.hasNext()) {
				qs = rs.next();
				OWLIndividual indX = dataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("x").getURI()));
				OWLIndividual indY = dataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("y").getURI()));
				OWLObjectProperty prop = dataFactory.getOWLObjectProperty(IRI.create(qs.getResource("p").getURI()));
				OWLAxiom ax1 = dataFactory.getOWLClassAssertionAxiom(cls1, indX);
				OWLAxiom ax2 = dataFactory.getOWLObjectPropertyAssertionAxiom(prop, indY, indX);
				OWLAxiom ax3 = dataFactory.getOWLObjectPropertyRangeAxiom(prop, cls2);
				Set<OWLAxiom> justification = Sets.newHashSet(ax1, ax2, ax3);
				justification.addAll(disjointnessExplanation);
				explanations.add(new Explanation<OWLAxiom>(inconsistencyEntailment, justification));
			}
			qe.close();
//			allExplanations.addAll(explanations);
		}
	}
	
	private Set<OWLAxiom> getDisjointnessAxiomExplanation(OWLDisjointClassesAxiom axiom){
		List<OWLClassExpression> classes = axiom.getClassExpressionsAsList();
		OWLClass cls1 = classes.get(0).asOWLClass();
		OWLClass cls2 = classes.get(1).asOWLClass();
		Set<OWLAxiom> axioms = new HashSet<>();
		if(assertedDisjointClassesAxioms.contains(axiom)){
			axioms.add(axiom);
		} else {
			if(path2Root.containsKey(cls1)){
				axioms.addAll(path2Root.get(cls1));
			}
			if(path2Root.containsKey(cls2)){
				axioms.addAll(path2Root.get(cls2));
			}
			axioms.addAll(sourceAxioms.get(axiom));
		}
		return axioms;
	}
	
	private Set<String> getPropertiesWithDomain(OWLClass domain){
		Set<String> properties = new TreeSet<>();
		
		String query = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> SELECT ?p WHERE {?p rdfs:domain <" + domain.toStringID() + ">.}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			properties.add("<" + qs.getResource("p").getURI() + ">");
		}
		qe.close();
		return properties;
	}
	
	private Set<String> getPropertiesWithRange(OWLClass range){
		Set<String> properties = new TreeSet<>();
		
		String query = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> "
				+ "PREFIX owl:<http://www.w3.org/2002/07/owl#>"
				+ "SELECT ?p WHERE {?p a owl:ObjectProperty. ?p rdfs:range <" + range.toStringID() + ">.}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			properties.add("<" + qs.getResource("p").getURI() + ">");
		}
		qe.close();
		return properties;
	}
	
	private Set<OWLDisjointClassesAxiom> getDisjointnessAxioms() {
		logger.info("Generating disjointness candidates...");
//		Set<OWLDisjointClassesAxiom> allDisjointClassesAxioms = new TreeSet<>();
		assertedDisjointClassesAxioms = new HashSet<>();
		String query = "SELECT ?cls1 ?cls2 WHERE {?cls1 <http://www.w3.org/2002/07/owl#disjointWith> ?cls2}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			if (qs.get("cls1").isURIResource() && qs.get("cls2").isURIResource()) {
				assertedDisjointClassesAxioms.add(dataFactory.getOWLDisjointClassesAxiom(
						dataFactory.getOWLClass(IRI.create(qs.getResource("cls1").getURI())),
						dataFactory.getOWLClass(IRI.create(qs.getResource("cls2").getURI()))));
			}

		}
		qe.close();
		logger.info("Found " + assertedDisjointClassesAxioms.size() + " asserted disjointness axioms.");
		//get subclasses for each class contained in disjointness axiom, and based on this create the 'inferred' axioms
		for (OWLDisjointClassesAxiom ax : assertedDisjointClassesAxioms) {
			buildDerivedAxioms(ax);
			allDisjointClassesAxioms.add(ax);
		}
		logger.info("Found overall "  + allDisjointClassesAxioms.size() + " disjointness candidates.");
		return allDisjointClassesAxioms;
	}
	
	private Set<OWLDisjointClassesAxiom> getDisjointnessAxiomsOld() {
		logger.info("Generating disjointness candidates...");
		Set<OWLDisjointClassesAxiom> allDisjointClassesAxioms = new TreeSet<>();
		assertedDisjointClassesAxioms = new HashSet<>();
		String query = "SELECT ?cls1 ?cls2 WHERE {?cls1 <http://www.w3.org/2002/07/owl#disjointWith> ?cls2}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			if (qs.get("cls1").isURIResource() && qs.get("cls2").isURIResource()) {
				assertedDisjointClassesAxioms.add(dataFactory.getOWLDisjointClassesAxiom(
						dataFactory.getOWLClass(IRI.create(qs.getResource("cls1").getURI())),
						dataFactory.getOWLClass(IRI.create(qs.getResource("cls2").getURI()))));
			}

		}
		qe.close();
		logger.info("Found " + assertedDisjointClassesAxioms.size() + " asserted disjointness axioms.");
		//get subclasses for each class contained in disjointness axiom, and based on this create the 'inferred' axioms
		for (OWLDisjointClassesAxiom ax : assertedDisjointClassesAxioms) {
			List<OWLClassExpression> classes = ax.getClassExpressionsAsList();
			OWLClass cls1 = classes.get(0).asOWLClass();
			OWLClass cls2 = classes.get(1).asOWLClass();
			//get subclasses of cls1
			getSubsumptionDown(cls1);
			getSubsumptionDown(cls2);
			Set<OWLClass> subClasses1 = new HashSet<>();
			query = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> SELECT ?sub WHERE {?sub rdfs:subClassOf* <" + cls1.toStringID() + ">}";
			qe = qef.createQueryExecution(query);
			rs = qe.execSelect();
			while (rs.hasNext()) {
				qs = rs.next();
				if(qs.get("sub").isURIResource()){
					subClasses1.add(dataFactory.getOWLClass(IRI.create(qs.getResource("sub").getURI())));
				}
			}
			qe.close();
			//get subclasses of cls2
			Set<OWLClass> subClasses2 = new HashSet<>();
			query = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> SELECT ?sub WHERE {?sub rdfs:subClassOf* <" + cls2.toStringID() + ">}";
			qe = qef.createQueryExecution(query);
			rs = qe.execSelect();
			while (rs.hasNext()) {
				qs = rs.next();
				if(qs.get("sub").isURIResource()){
					subClasses2.add(dataFactory.getOWLClass(IRI.create(qs.getResource("sub").getURI())));
				}
			}
			qe.close();
			//add 'inferred' axioms
			for (OWLClass sub1 : subClasses1) {
				for (OWLClass sub2 : subClasses2) {
					allDisjointClassesAxioms.add(dataFactory.getOWLDisjointClassesAxiom(sub1, sub2));
				}
			}
			allDisjointClassesAxioms.add(ax);
		}
		logger.info("Found overall "  + allDisjointClassesAxioms.size() + " disjointness candidates.");
		return allDisjointClassesAxioms;
	}
	
	private void buildDerivedAxioms(OWLDisjointClassesAxiom axiom){
		List<OWLClassExpression> classes = axiom.getClassExpressionsAsList();
		OWLClass cls1 = classes.get(0).asOWLClass();
		OWLClass cls2 = classes.get(1).asOWLClass();
		
		path2Root = new HashMap<>();
		//get subclasses of cls1
		Set<OWLClass> subClasses1 = buildSubsumptionDown(cls1, new HashSet<OWLSubClassOfAxiom>());
		subClasses1.add(cls1);
		//get subclasses of cls2
		Set<OWLClass> subClasses2 = buildSubsumptionDown(cls2, new HashSet<OWLSubClassOfAxiom>());
		subClasses2.add(cls2);
		
		OWLDisjointClassesAxiom newAxiom;
		for (OWLClass sub1 : subClasses1) {
			for (OWLClass sub2 : subClasses2) {
				newAxiom = dataFactory.getOWLDisjointClassesAxiom(sub1, sub2);
				sourceAxioms.put(newAxiom, axiom);
				allDisjointClassesAxioms.add(newAxiom);
			}
		}
	}
	
	private Set<OWLClass> buildSubsumptionDown(OWLClass cls, Set<OWLSubClassOfAxiom> path){
		Set<OWLClass> subClasses = new HashSet<>();
		String query = "SELECT ?sub WHERE {?sub <http://www.w3.org/2000/01/rdf-schema#subClassOf> <" + cls.toStringID() + ">.}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			if(qs.get("sub").isURIResource()){
				OWLClass sub = dataFactory.getOWLClass(IRI.create(qs.getResource("sub").getURI()));
				subClasses.add(sub);
				Set<OWLSubClassOfAxiom> newPath = Sets.newHashSet(path);
				newPath.add(dataFactory.getOWLSubClassOfAxiom(sub, cls));
				subClasses.addAll(buildSubsumptionDown(sub, newPath));
				path2Root.put(sub, newPath);
			}
		}
		qe.close();
		return subClasses;
	}
	
	private Set<OWLClass> getSubClasses(OWLClass cls){
		Set<OWLClass> subClasses = new HashSet<>();
		String query = "SELECT ?sub WHERE {?sub <http://www.w3.org/2000/01/rdf-schema#subClassOf> <" + cls.toStringID() + ">.}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			if(qs.get("sub").isURIResource()){
				OWLClass sub = dataFactory.getOWLClass(IRI.create(qs.getResource("sub").getURI()));
				subClasses.add(sub);
			}
		}
		qe.close();
		return subClasses;
	}
	
	private Set<OWLSubClassOfAxiom> getSubsumptionDown(OWLClass cls){
		Set<OWLSubClassOfAxiom> axioms = new HashSet<>();
		String query = "SELECT ?sub WHERE {?sub <http://www.w3.org/2000/01/rdf-schema#subClassOf> <" + cls.toStringID() + ">.}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			if(qs.get("sub").isURIResource()){
				OWLClass sub = dataFactory.getOWLClass(IRI.create(qs.getResource("sub").getURI()));
				axioms.add(dataFactory.getOWLSubClassOfAxiom(sub, cls));
				//recursively get subclasses of subclass
				axioms.addAll(getSubsumptionDown(sub));
			}
		}
		qe.close();
		return axioms;
	}
	
	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		Set<OWLAxiom> disjointnessAxioms = AxiomType.getAxiomsOfTypes(axiomsToIgnore, AxiomType.DISJOINT_CLASSES);
		if(!disjointnessAxioms.isEmpty()){
			filter = "FILTER(";
			String s = "";
			for (OWLAxiom axiom : disjointnessAxioms) {
				if(!s.isEmpty()){
					s += " && ";
				}
				for (OWLDisjointClassesAxiom dis : ((OWLDisjointClassesAxiom)axiom).asPairwiseAxioms()) {
					List<OWLClassExpression> operands = dis.getClassExpressionsAsList();
					OWLClass cls1 = operands.get(0).asOWLClass();
					OWLClass cls2 = operands.get(1).asOWLClass();
					s += "(";
					s += "(?cls1!=<" + cls1.toStringID() + "> && ?cls2!= <" + cls2.toStringID() + ">)";
					s += " || ";
					s += "(?cls1!=<" + cls2.toStringID() + "> && ?cls2!= <" + cls1.toStringID() + ">)";
					s += ")";
				}
			}
			filter += s;
			filter += ")";
			System.out.println(filter);
		}
	}
	
	

	
}
