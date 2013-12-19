package org.aksw.mole.ore.validation.constraint;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.mole.ore.util.PermutationsOfN;
import org.dllearner.utilities.owl.OWLClassExpressionToSPARQLConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.DataRangeType;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;

public class OWLAxiomConstraintToSPARQLConverter implements OWLAxiomVisitor{
	
	List<AxiomType> subjectObjectAxiomTypes = Arrays.asList(new AxiomType[]{
			AxiomType.ASYMMETRIC_OBJECT_PROPERTY,
			
	});
	
	private String rootVar = "?x";
	private String sparql;
	private OWLClassExpressionConstraintToSPARQLConverter expressionConstraintConverter;
	private OWLClassExpressionToSPARQLConverter expressionConverter;
	private boolean ignoreGenericTypeStatements = true;
	
	private String targetSubjectVar = "?s"; 
	private String targetObjectVar = "?o";
	
	public String convert(String rootVariable, OWLAxiom axiom){
		this.rootVar = rootVariable;
		sparql = "";
		expressionConstraintConverter = new OWLClassExpressionConstraintToSPARQLConverter(ignoreGenericTypeStatements);
		expressionConverter = new OWLClassExpressionToSPARQLConverter();
		axiom.accept(this);
		return sparql;
	}
	
	public Query asQuery(String rootVariable, OWLAxiom axiom){
		String queryString = "SELECT DISTINCT " + rootVariable + " WHERE {";
		queryString += convert(rootVariable, axiom);
		queryString += "}";
		return QueryFactory.create(queryString, Syntax.syntaxARQ);
	}
	
	public Query asQuery(OWLAxiom axiom){
		String queryString = "SELECT DISTINCT ";
		if(subjectObjectAxiomTypes.contains(axiom.getAxiomType())){
			queryString += targetSubjectVar + targetObjectVar;
		} else {
			queryString += targetSubjectVar;
		}
		queryString += " WHERE {";
		queryString += convert(targetSubjectVar, axiom);
		queryString += "}";
		return QueryFactory.create(queryString, Syntax.syntaxARQ);
	}
	
	/**
	 * @param targetSubjectVar the targetSubjectVar to set
	 */
	public void setTargetSubjectVar(String targetSubjectVar) {
		this.targetSubjectVar = targetSubjectVar;
	}
	
	/**
	 * @param targetObjectVar the targetObjectVar to set
	 */
	public void setTargetObjectVar(String targetObjectVar) {
		this.targetObjectVar = targetObjectVar;
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
		String subClassPattern = expressionConverter.convert(rootVar, subClass);
		OWLClassExpression superClass = axiom.getSuperClass();
		String superClassPattern = expressionConstraintConverter.convert(rootVar, superClass, subClassPattern);
		if(!(superClass instanceof OWLObjectIntersectionOf)){
			sparql += subClassPattern;
		}
		sparql += superClassPattern;
	}

	@Override
	public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLObjectProperty().toStringID();
		sparql += 	targetSubjectVar + " <" + propertyURI + "> " + targetObjectVar + "." +
					targetObjectVar + " <" + propertyURI + "> " + targetSubjectVar;
	}

	@Override
	public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLObjectProperty().toStringID();
		sparql += 	targetSubjectVar + " <" + propertyURI + "> " + targetSubjectVar;
	}

	@Override
	public void visit(OWLDisjointClassesAxiom axiom) {
		//get all subsets of size 2 because we have to check for pairs of disjoint classes if there are instances of both
		List<OWLClassExpression> disjointClasses = axiom.getClassExpressionsAsList();
		List<List<OWLClassExpression>> subsets = PermutationsOfN.getSubsetsOfSizeN(disjointClasses, 2);
		if(subsets.size() == 1){
			for (OWLClassExpression cls : subsets.get(0)) {
				sparql += 	targetSubjectVar + " a <" + cls.asOWLClass().toStringID() + ">.";  
			}
		} else {
			for (int i = 0; i < subsets.size(); i++) {
				sparql += "{";
				for (OWLClassExpression cls : subsets.get(i)) {
					sparql += 	targetSubjectVar + " a <" + cls.asOWLClass().toStringID() + ">.";  
				}
				sparql += "}";
				if(i < subsets.size()-1)
					sparql += " UNION ";
			}
		}
	}

	@Override
	public void visit(OWLDataPropertyDomainAxiom axiom) {
		OWLClassExpression domain = axiom.getDomain();
		OWLDataProperty property = axiom.getProperty().asOWLDataProperty();
		sparql += rootVar + " <" + property.toStringID() + "> ?o." +
				"FILTER NOT EXISTS {" + rootVar + " a <" + domain.asOWLClass().toStringID() + ">.}"; 
	}

	@Override
	public void visit(OWLObjectPropertyDomainAxiom axiom) {
		OWLClassExpression domain = axiom.getDomain();
		OWLObjectProperty property = axiom.getProperty().asOWLObjectProperty();
		sparql += targetSubjectVar + " <" + property.toStringID() + "> ?o." +
				"FILTER NOT EXISTS {" + rootVar + " a <" + domain.asOWLClass().toStringID() + ">.}"; 
	}

	@Override
	public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
		// get all subsets of size 2 because we have to check for pairs of
		// equivalent properties if there are subject and objects not connected by both
		List<OWLObjectPropertyExpression> equivalentProperties = Lists.newArrayList(axiom.getProperties());
		List<List<OWLObjectPropertyExpression>> subsets = PermutationsOfN.getSubsetsOfSizeN(equivalentProperties, 2);
		if (subsets.size() == 1) {
			OWLObjectProperty dp1 = subsets.get(0).get(0).asOWLObjectProperty();
			OWLObjectProperty dp2 = subsets.get(0).get(1).asOWLObjectProperty();
			sparql += "{" + targetSubjectVar + " <" + dp1.toStringID() + "> " + targetObjectVar + "."
					+ " FILTER NOT EXISTS{" + targetSubjectVar + " <" + dp2.toStringID() + "> " + targetObjectVar + ".}}";
			sparql += " UNION ";
			sparql += "{" + targetSubjectVar + " <" + dp2.toStringID() + "> " + targetObjectVar + "."
					+ " FILTER NOT EXISTS{" + targetSubjectVar + " <" + dp1.toStringID() + "> " + targetObjectVar + ".}}";

		} else {
			for (int i = 0; i < subsets.size(); i++) {
				OWLObjectProperty dp1 = subsets.get(i).get(0).asOWLObjectProperty();
				OWLObjectProperty dp2 = subsets.get(i).get(1).asOWLObjectProperty();
				sparql += "{" + targetSubjectVar + " <" + dp1.toStringID() + "> " + targetObjectVar + "."
						+ " FILTER NOT EXISTS{" + targetSubjectVar + " <" + dp2.toStringID() + "> " + targetObjectVar + ".}}";
				sparql += " UNION ";
				sparql += "{" + targetSubjectVar + " <" + dp2.toStringID() + "> " + targetObjectVar + "."
						+ " FILTER NOT EXISTS{" + targetSubjectVar + " <" + dp1.toStringID() + "> " + targetObjectVar + ".}}";
				if (i < subsets.size() - 1)
					sparql += " UNION ";
			}
		}
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
		OWLClassExpression range = axiom.getRange();
		OWLObjectProperty property = axiom.getProperty().asOWLObjectProperty();
		sparql += targetSubjectVar + " <" + property.toStringID() + "> " + targetObjectVar + "." +
				"FILTER NOT EXISTS {?o a <" + range.asOWLClass().toStringID() + ">.}"; 
	}

	@Override
	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLObjectProperty().toStringID();
		sparql += 	targetSubjectVar + " <" + propertyURI + "> ?o1." +
				targetSubjectVar + " <" + propertyURI + "> ?o2." +
					"FILTER(str(?o1) != str(?o2))";
	}

	@Override
	public void visit(OWLSubObjectPropertyOfAxiom axiom) {
		OWLObjectProperty subProp = axiom.getSubProperty().asOWLObjectProperty();
		OWLObjectProperty superProp = axiom.getSuperProperty().asOWLObjectProperty();
		sparql += rootVar + " <" + subProp.toStringID() + "> ?o. " 
				+ "FILTER NOT EXISTS{" + rootVar + " <" + superProp.toStringID() + "> ?o.}";
	}

	@Override
	public void visit(OWLDisjointUnionAxiom axiom) {
	}

	@Override
	public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLObjectProperty().toStringID();
		sparql += 	rootVar + " <" + propertyURI + "> ?o." +
					"FILTER NOT EXISTS { ?o <" + propertyURI + "> " + rootVar + "}";
	}

	@Override
	public void visit(OWLDataPropertyRangeAxiom axiom) {
		OWLDataRange range = axiom.getRange();
		if(range.getDataRangeType() == DataRangeType.DATATYPE){
			OWLDataProperty property = axiom.getProperty().asOWLDataProperty();
			sparql += rootVar + " <" + property.toStringID() + "> ?o." +
					"FILTER (DATATYPE(?o) != <" + range.asOWLDatatype().toStringID() + ">)";
		} else {
			throw new IllegalArgumentException("Datarange " + range + " not supported yet.");
		}
		
	}

	@Override
	public void visit(OWLFunctionalDataPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLDataProperty().toStringID();
		sparql += 	rootVar + " <" + propertyURI + "> ?o1." +
					rootVar + " <" + propertyURI + "> ?o2." +
					"FILTER(str(?o1) != str(?o2))";
	}

	@Override
	public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
		// get all subsets of size 2 because we have to check for pairs of
				// equivalent properties if there are subject and objects not connected by both
		List<OWLDataPropertyExpression> equivalentProperties =  Lists.newArrayList(axiom.getProperties());
		List<List<OWLDataPropertyExpression>> subsets = PermutationsOfN.getSubsetsOfSizeN(equivalentProperties, 2);
		if (subsets.size() == 1) {
			OWLDataProperty dp1 = subsets.get(0).get(0).asOWLDataProperty();
			OWLDataProperty dp2 = subsets.get(0).get(1).asOWLDataProperty();
			sparql += "{" + rootVar + " <" + dp1.toStringID() + "> ?o. " 
					+ "FILTER NOT EXISTS{" + rootVar + " <" + dp2.toStringID() + "> ?o.}}";
			sparql += " UNION ";
			sparql += "{" + rootVar + " <" + dp2.toStringID() + "> ?o. " 
					+ "FILTER NOT EXISTS{" + rootVar + " <" + dp1.toStringID() + "> ?o.}}";

		} else {
			for (int i = 0; i < subsets.size(); i++) {
				OWLDataProperty dp1 = subsets.get(i).get(0).asOWLDataProperty();
				OWLDataProperty dp2 = subsets.get(i).get(1).asOWLDataProperty();
				sparql += "{" + rootVar + " <" + dp1.toStringID() + "> ?o. " 
						+ "FILTER NOT EXISTS{" + rootVar + " <" + dp2.toStringID() + "> ?o.}}";
				sparql += " UNION ";
				sparql += "{" + rootVar + " <" + dp2.toStringID() + "> ?o. " 
						+ "FILTER NOT EXISTS{" + rootVar + " <" + dp1.toStringID() + "> ?o.}}";
				if (i < subsets.size() - 1)
					sparql += " UNION ";
			}
		}
	}

	@Override
	public void visit(OWLClassAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLEquivalentClassesAxiom axiom) {
		// get all subsets of size 2 because we have to check for pairs of
		// equivalent classes if there are instances not contained in both
		List<OWLClassExpression> equivalentClasses = axiom.getClassExpressionsAsList();
		List<List<OWLClassExpression>> subsets = PermutationsOfN.getSubsetsOfSizeN(equivalentClasses, 2);
		if (subsets.size() == 1) {
			OWLClass cls1 = subsets.get(0).get(0).asOWLClass();
			OWLClass cls2 = subsets.get(0).get(1).asOWLClass();
			sparql += "{" + rootVar + " a <" + cls1.toStringID() + ">. "
					+ "FILTER NOT EXISTS{" + rootVar + " a <" + cls2.toStringID() + ">.}}";
			sparql += " UNION ";
			sparql += "{" + rootVar + " a <" + cls2.toStringID() + ">. "
					+ "FILTER NOT EXISTS{" + rootVar + " a <" + cls1.toStringID() + ">.}}";
				
		} else {
			for (int i = 0; i < subsets.size(); i++) {
				OWLClass cls1 = subsets.get(i).get(0).asOWLClass();
				OWLClass cls2 = subsets.get(i).get(1).asOWLClass();
				sparql += "{" + rootVar + " a <" + cls1.toStringID() + ">. "
						+ "FILTER NOT EXISTS{" + rootVar + " a <" + cls2.toStringID() + ">.}}";
				sparql += " UNION ";
				sparql += "{" + rootVar + " a <" + cls2.toStringID() + ">. "
						+ "FILTER NOT EXISTS{" + rootVar + " a <" + cls1.toStringID() + ">.}}";
				if(i < subsets.size()-1)
					sparql += " UNION ";
			}
		}
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
		sparql += 	rootVar + " <" + propertyURI + "> " + rootVar;
	}

	@Override
	public void visit(OWLSubDataPropertyOfAxiom axiom) {
		OWLDataProperty subProp = axiom.getSubProperty().asOWLDataProperty();
		OWLDataProperty superProp = axiom.getSuperProperty().asOWLDataProperty();
		sparql += rootVar + " <" + subProp.toStringID() + "> ?o. " 
				+ "FILTER NOT EXISTS{" + rootVar + " <" + superProp.toStringID() + "> ?o.}";
	}

	@Override
	public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLObjectProperty().toStringID();
		sparql += 	"?s1 <" + propertyURI + "> " + rootVar +
					"?s2 <" + propertyURI + "> " + rootVar +
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
		OWLAxiomConstraintToSPARQLConverter converter = new OWLAxiomConstraintToSPARQLConverter();
		
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
