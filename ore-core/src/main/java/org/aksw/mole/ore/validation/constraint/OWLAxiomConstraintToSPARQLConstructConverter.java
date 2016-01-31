package org.aksw.mole.ore.validation.constraint;


import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import org.dllearner.utilities.owl.OWLClassExpressionToSPARQLConverter;
import org.dllearner.utilities.owl.VariablesMapping;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public class OWLAxiomConstraintToSPARQLConstructConverter implements OWLAxiomVisitor{
	
	private String root = "?x";
	private String sparql;
	private OWLClassExpressionConstraintToSPARQLConstructQueryConverter expressionConstraintConverter;
	private OWLClassExpressionToSPARQLConverter expressionConverter;
	private boolean ignoreGenericTypeStatements = true;
	private String allowedResourceNamespace;
	
	public String convert(String rootVariable, OWLAxiom axiom){
		this.root = rootVariable;
		sparql = "";
		VariablesMapping mapping = new VariablesMapping();
		expressionConstraintConverter = new OWLClassExpressionConstraintToSPARQLConstructQueryConverter(mapping, ignoreGenericTypeStatements);
		expressionConverter = new OWLClassExpressionToSPARQLConverter(mapping);
		axiom.accept(this);
		return sparql;
	}
	
	public Query asQuery(String rootVariable, OWLAxiom axiom){
		String queryString = convert(rootVariable, axiom);
		return QueryFactory.create(queryString, Syntax.syntaxARQ);
	}
	
	public Query asQuery(String rootVariable, OWLAxiom axiom, String allowedResourceNamespace){
		this.allowedResourceNamespace = allowedResourceNamespace;
		String queryString = convert(rootVariable, axiom);
		return QueryFactory.create(queryString, Syntax.syntaxARQ);
	}

	@Override
	public void visit(OWLAnnotationAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {
	}

	@Override
	public void visit(OWLAnnotationPropertyDomainAxiom axiom) {
	}

	@Override
	public void visit(OWLAnnotationPropertyRangeAxiom axiom) {
	}

	@Override
	public void visit(OWLDeclarationAxiom axiom) {
	}

	@Override
	public void visit(OWLSubClassOfAxiom axiom) {
		OWLClassExpression subClass = axiom.getSubClass();
		String subClassPattern = expressionConverter.convert(root, subClass);
		OWLClassExpression superClass = axiom.getSuperClass();
		String superClassPattern = expressionConstraintConverter.convert(root, superClass, subClassPattern, allowedResourceNamespace);
		sparql += superClassPattern;
	}

	@Override
	public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
	}

	@Override
	public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
	}

	@Override
	public void visit(OWLDisjointClassesAxiom axiom) {
	}

	@Override
	public void visit(OWLDataPropertyDomainAxiom axiom) {
	}

	@Override
	public void visit(OWLObjectPropertyDomainAxiom axiom) {
	}

	@Override
	public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
	}

	@Override
	public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLDifferentIndividualsAxiom axiom) {
	}

	@Override
	public void visit(OWLDisjointDataPropertiesAxiom axiom) {
	}

	@Override
	public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
	}

	@Override
	public void visit(OWLObjectPropertyRangeAxiom axiom) {
	}

	@Override
	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLObjectProperty().toStringID();
		sparql += 	"CONSTRUCT {" +
					root + " <" + propertyURI + "> ?o1." +
					root + " <" + propertyURI + "> ?o2.}" +
					"WHERE {" +
					root + " <" + propertyURI + "> ?o1." +
					root + " <" + propertyURI + "> ?o2." +
					"FILTER(?o1 != ?o2)}";
	}

	@Override
	public void visit(OWLSubObjectPropertyOfAxiom axiom) {
	}

	@Override
	public void visit(OWLDisjointUnionAxiom axiom) {
	}

	@Override
	public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
	}

	@Override
	public void visit(OWLDataPropertyRangeAxiom axiom) {
	}

	@Override
	public void visit(OWLFunctionalDataPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLDataProperty().toStringID();
		sparql += 	"CONSTRUCT {" +
					root + " <" + propertyURI + "> ?o1." +
					root + " <" + propertyURI + "> ?o2.}" +
					"WHERE {" +
					root + " <" + propertyURI + "> ?o1." +
					root + " <" + propertyURI + "> ?o2." +
					"FILTER(?o1 != ?o2)}";
	}

	@Override
	public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
	}

	@Override
	public void visit(OWLClassAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLEquivalentClassesAxiom axiom) {
	}

	@Override
	public void visit(OWLDataPropertyAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
	}

	@Override
	public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLObjectProperty().toStringID();
		sparql += 	"CONSTRUCT {" +
					root + " <" + propertyURI + "> " + root +
					"WHERE {" +
					root + " <" + propertyURI + "> " + root +
					"}";
	}

	@Override
	public void visit(OWLSubDataPropertyOfAxiom axiom) {
	}

	@Override
	public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLObjectProperty().toStringID();
		sparql += 	"CONSTRUCT {" +
					"?s1 <" + propertyURI + "> " + root +
					"?s2 <" + propertyURI + "> " + root +
					"} WHERE {" +
					"?s1 <" + propertyURI + "> " + root +
					"?s2 <" + propertyURI + "> " + root +
					"FILTER(?s1 != ?s2)}";
	}

	@Override
	public void visit(OWLSameIndividualAxiom axiom) {
	}

	@Override
	public void visit(OWLSubPropertyChainOfAxiom axiom) {
	}

	@Override
	public void visit(OWLInverseObjectPropertiesAxiom axiom) {
	}

	@Override
	public void visit(OWLHasKeyAxiom axiom) {
	}

	@Override
	public void visit(OWLDatatypeDefinitionAxiom axiom) {
	}

	@Override
	public void visit(SWRLRule rule) {
	}
	
	public static void main(String[] args) throws Exception {
		ToStringRenderer.getInstance().setRenderer(new DLSyntaxObjectRenderer());
		OWLAxiomConstraintToSPARQLConstructConverter converter = new OWLAxiomConstraintToSPARQLConstructConverter();
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
		PrefixManager pm = new DefaultPrefixManager("http://dbpedia.org/ontology/");
		
		OWLClass clsA = df.getOWLClass("A", pm);
		OWLClass clsB = df.getOWLClass("B", pm);
		OWLClass clsC = df.getOWLClass("C", pm);
		
		OWLObjectProperty propR = df.getOWLObjectProperty("r", pm);
		OWLObjectProperty propS = df.getOWLObjectProperty("s", pm);
		
		OWLDataProperty dpT = df.getOWLDataProperty("t", pm);
		OWLDataRange booleanRange = df.getBooleanOWLDatatype();
		OWLLiteral lit = df.getOWLLiteral(1);
		
		OWLIndividual indA = df.getOWLNamedIndividual("a", pm);
		OWLIndividual  indB = df.getOWLNamedIndividual("b", pm);
		
		String rootVar = "?x";
		//NAMEDCLASS
		OWLClassExpression subClass = clsA;
		OWLClassExpression superClass = clsB;
		OWLAxiom axiom = df.getOWLSubClassOfAxiom(subClass, superClass);
		String query = converter.asQuery(rootVar, axiom).toString();
		System.out.println(axiom + "\n" + query);
		//EXISTENTIAL RESTRICTION
		superClass = df.getOWLObjectSomeValuesFrom(propR, clsB);
		axiom = df.getOWLSubClassOfAxiom(subClass, superClass);
		query = converter.asQuery(rootVar, axiom).toString();
		System.out.println(axiom + "\n" + query);
		//INTERSECTION
		superClass = df.getOWLObjectIntersectionOf(
				df.getOWLObjectSomeValuesFrom(propR, clsB),
				clsB);
		axiom = df.getOWLSubClassOfAxiom(subClass, superClass);
		query = converter.asQuery(rootVar, axiom).toString();
		System.out.println(axiom + "\n" + query);
		//UNION
		superClass = df.getOWLObjectUnionOf(
				clsB,
				clsC);
		axiom = df.getOWLSubClassOfAxiom(subClass, superClass);
		query = converter.asQuery(rootVar, axiom).toString();
		System.out.println(axiom + "\n" + query);
		//HAS VALUE
		superClass = df.getOWLObjectHasValue(propR, indA);
		axiom = df.getOWLSubClassOfAxiom(subClass, superClass);
		query = converter.asQuery(rootVar, axiom).toString();
		System.out.println(axiom + "\n" + query);
		//UNIVERSAL RESTRICTION
		superClass = df.getOWLObjectAllValuesFrom(propR, clsB);
		axiom = df.getOWLSubClassOfAxiom(subClass, superClass);
		query = converter.asQuery(rootVar, axiom).toString();
		System.out.println(axiom + "\n" + query);
		// ONE OF
		superClass = df.getOWLObjectOneOf(indA, indB);
		axiom = df.getOWLSubClassOfAxiom(subClass, superClass);
		query = converter.asQuery(rootVar, axiom).toString();
		System.out.println(axiom + "\n" + query);
		//existential restriction with one of filler
		superClass = df.getOWLObjectSomeValuesFrom(propR, df.getOWLObjectOneOf(indA, indB));
		axiom = df.getOWLSubClassOfAxiom(subClass, superClass);
		query = converter.asQuery(rootVar, axiom).toString();
		System.out.println(axiom + "\n" + query);
		
		
		
	}

}
