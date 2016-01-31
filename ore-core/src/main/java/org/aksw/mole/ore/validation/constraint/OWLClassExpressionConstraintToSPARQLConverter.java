package org.aksw.mole.ore.validation.constraint;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class OWLClassExpressionConstraintToSPARQLConverter implements OWLClassExpressionVisitor, OWLPropertyExpressionVisitor, OWLDataRangeVisitor{
	
	private String sparql = "";
	private Stack<String> variables = new Stack<String>();
	private Map<OWLEntity, String> variablesMapping;
	
	private int classCnt = 0;
	private int propCnt = 0;
	private int indCnt = 0;
	
	private OWLDataFactory df = new OWLDataFactoryImpl();
	
	private Map<Integer, Boolean> intersection;
	private String existingPattern;
	private boolean ignoreGenericTypeStatements = true;
	
	public OWLClassExpressionConstraintToSPARQLConverter(boolean ignoreGenericTypeStatements) {
		this.ignoreGenericTypeStatements = ignoreGenericTypeStatements;
	}
	
	public OWLClassExpressionConstraintToSPARQLConverter() {
	}

	public String convert(String rootVariable, OWLClassExpression expr){
		reset();
		variables.push(rootVariable);
		expr.accept(this);
		return sparql;
	}
	
	public String convert(String rootVariable, OWLClassExpression expr, String existingPattern){
		this.existingPattern = existingPattern;
		reset();
		variables.push(rootVariable);
		expr.accept(this);
		return sparql;
	}
	
	public String convert(String rootVariable, OWLPropertyExpression expr){
		variables.push(rootVariable);
		sparql = "";
		expr.accept(this);
		return sparql;
	}
	
	public Query asQuery(String rootVariable, OWLClassExpression expr){
		String queryString = "SELECT DISTINCT " + rootVariable + " WHERE {";
		queryString += convert(rootVariable, expr);
		queryString += "}";
		return QueryFactory.create(queryString, Syntax.syntaxARQ);
	}
	
	private void reset(){
		variables.clear();
		classCnt = 0;
		propCnt = 0;
		indCnt = 0;
		sparql = "";
		intersection = new HashMap<Integer, Boolean>();
	}
	
	private String getVariable(OWLEntity entity){
		String var = variablesMapping.get(entity);
		if(var == null){
			if(entity.isOWLClass()){
				var = "?cls" + classCnt++;
			} else if(entity.isOWLObjectProperty() || entity.isOWLDataProperty()){
				var = "?p" + propCnt++;
			} else if(entity.isOWLNamedIndividual()){
				var = buildIndividualVariable();
			} 
			variablesMapping.put(entity, var);
		}
		return var;
	}
	
	private String buildIndividualVariable(){
		return "?s" + indCnt++;
	}
	
	private int modalDepth(){
		return variables.size();
	}
	
	private boolean inIntersection(){
		return intersection.containsKey(modalDepth()) ? intersection.get(modalDepth()) : false;
	}
	
	private void enterIntersection(){
		intersection.put(modalDepth(), true);
	}
	
	private void leaveIntersection(){
		intersection.remove(modalDepth());
	}
	
	private String triple(String subject, String predicate, String object){
		return (subject.startsWith("?") ? subject : "<" + subject + ">") + " " + 
				(predicate.startsWith("?") || predicate.equals("a") ? predicate : "<" + predicate + ">") + " " +
				(object.startsWith("?") ? object : "<" + object + ">") + ".\n";
	}
	
	private String triple(String subject, String predicate, OWLLiteral object){
		return (subject.startsWith("?") ? subject : "<" + subject + ">") + " " + 
				(predicate.startsWith("?") || predicate.equals("a") ? predicate : "<" + predicate + ">") + " " +
				render(object) + ".\n";
	}
	
	private String triple(String subject, String predicate, OWLIndividual object){
		return (subject.startsWith("?") ? subject : "<" + subject + ">") + " " + 
				(predicate.startsWith("?") || predicate.equals("a") ? predicate : "<" + predicate + ">") + " " +
				"<" + object.toStringID() + ">.\n";
	}
	
	private String render(OWLLiteral literal){
		return literal +"^^<" + literal.getDatatype().toStringID() + ">";
	}

	@Override
	public void visit(OWLObjectProperty property) {
	}

	@Override
	public void visit(OWLObjectInverseOf property) {
	}

	@Override
	public void visit(OWLDataProperty property) {
	}

	@Override
	public void visit(@Nonnull OWLAnnotationProperty owlAnnotationProperty) {
	}

	@Override
	public void visit(OWLClass ce) {
		sparql += "FILTER NOT EXISTS{";
		sparql += triple(variables.peek(), "a", ce.toStringID());
		sparql += "}";
	}

	@Override
	public void visit(OWLObjectIntersectionOf ce) {
		enterIntersection();
		List<OWLClassExpression> operands = ce.getOperandsAsList();
		for (int i = 0; i < operands.size()-1; i++) {
			sparql += "{";
			if(modalDepth() == 1 && existingPattern != null){
				sparql += existingPattern;
			}
			operands.get(i).accept(this);
			sparql += "}";
			sparql += " UNION ";
		}
		sparql += "{";
		if(modalDepth() == 1 && existingPattern != null){
			sparql += existingPattern;
		}
		operands.get(operands.size()-1).accept(this);
		sparql += "}";
		leaveIntersection();
	}

	@Override
	public void visit(OWLObjectUnionOf ce) {
		List<OWLClassExpression> operands = ce.getOperandsAsList();
		for (OWLClassExpression operand : operands) {
			operand.accept(this);
		}
	}

	@Override
	public void visit(OWLObjectComplementOf ce) {
		String subject = variables.peek();
		if(!inIntersection() && modalDepth() == 1){
			sparql += triple(subject, "?p", "?o");
		} 
		ce.getOperand().accept(this);
	}

	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		String objectVariable = buildIndividualVariable();
		OWLObjectPropertyExpression propertyExpression = ce.getProperty();
		sparql += "FILTER NOT EXISTS{";
		if(propertyExpression.isAnonymous()){
			//property expression is inverse of a property
			sparql += triple(objectVariable, propertyExpression.getNamedProperty().toStringID(), variables.peek());
		} else {
			sparql += triple(variables.peek(), propertyExpression.getNamedProperty().toStringID(), objectVariable);
		}
		OWLClassExpression filler = ce.getFiller();
		if(filler.isAnonymous()){
			variables.push(objectVariable);
			filler.accept(this);
			variables.pop();
		} else {
			if(ignoreGenericTypeStatements && !filler.isOWLThing()){
				sparql += triple(objectVariable, "a", filler.asOWLClass().toStringID());
			}
		}
		sparql += "}";
		
	}

	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
//		String subject = variables.peek();
//		String objectVariable = buildIndividualVariable();
//		OWLObjectPropertyExpression propertyExpression = ce.getProperty();
//		String predicate = propertyExpression.getNamedProperty().toStringID();
//		OWLClassExpression filler = ce.getFiller();
//		if(propertyExpression.isAnonymous()){
//			//property expression is inverse of a property
//			sparql += triple(objectVariable, predicate, variables.peek());
//		} else {
//			sparql += triple(variables.peek(), predicate, objectVariable);
//		}
//		
//		String var = buildIndividualVariable();
//		sparql += "{SELECT " + subject + " (COUNT(" + var + ") AS ?cnt1) WHERE {";
//		sparql += triple(subject, predicate, var);
//		variables.push(var);
//		filler.accept(this);
//		variables.pop();
//		sparql += "} GROUP BY " + subject + "}";
//		
//		var = buildIndividualVariable();
//		sparql += "{SELECT " + subject + " (COUNT(" + var + ") AS ?cnt2) WHERE {";
//		sparql += triple(subject, predicate, var);
//		sparql += "} GROUP BY " + subject + "}";
//		
//		sparql += "FILTER(?cnt1=?cnt2)";
		OWLObjectPropertyExpression propertyExpression = ce.getProperty();
		OWLClassExpression classExpression = ce.getFiller();
		df.getOWLObjectSomeValuesFrom(propertyExpression, df.getOWLObjectComplementOf(classExpression)).accept(this);
		
	}

	@Override
	public void visit(OWLObjectHasValue ce) {
		OWLObjectPropertyExpression propertyExpression = ce.getProperty();
		OWLIndividual value = ce.getValue();
		sparql += "FILTER NOT EXISTS{";
		if(propertyExpression.isAnonymous()){
			//property expression is inverse of a property
			sparql += triple(value.toStringID(), propertyExpression.getNamedProperty().toStringID(), variables.peek());
		} else {
			sparql += triple(variables.peek(), propertyExpression.getNamedProperty().toStringID(), value.toStringID());
		}
		sparql += "}";
	}

	@Override
	public void visit(OWLObjectMinCardinality ce) {
		String subjectVariable = variables.peek();
		String objectVariable = buildIndividualVariable();
		OWLObjectPropertyExpression propertyExpression = ce.getProperty();
		int cardinality = ce.getCardinality();
		sparql += "{SELECT " + subjectVariable + " WHERE {";
		if(propertyExpression.isAnonymous()){
			//property expression is inverse of a property
			sparql += triple(objectVariable, propertyExpression.getNamedProperty().toStringID(), subjectVariable);
		} else {
			sparql += triple(subjectVariable, propertyExpression.getNamedProperty().toStringID(), objectVariable);
		}
		OWLClassExpression filler = ce.getFiller();
		if(filler.isAnonymous()){
			String var = buildIndividualVariable();
			variables.push(var);
			sparql += triple(objectVariable, "a", var);
			filler.accept(this);
			variables.pop();
		} else {
			sparql += triple(objectVariable, "a", filler.asOWLClass().toStringID());
		}
		
		sparql += "} GROUP BY " + subjectVariable + " HAVING(COUNT(" + objectVariable + ")<" + cardinality + ")}";
		
	}

	@Override
	public void visit(OWLObjectExactCardinality ce) {
		String subjectVariable = variables.peek();
		String objectVariable = buildIndividualVariable();
		OWLObjectPropertyExpression propertyExpression = ce.getProperty();
		int cardinality = ce.getCardinality();
		sparql += "{SELECT " + subjectVariable + " WHERE {";
		if(propertyExpression.isAnonymous()){
			//property expression is inverse of a property
			sparql += triple(objectVariable, propertyExpression.getNamedProperty().toStringID(), subjectVariable);
		} else {
			sparql += triple(subjectVariable, propertyExpression.getNamedProperty().toStringID(), objectVariable);
		}
		OWLClassExpression filler = ce.getFiller();
		if(filler.isAnonymous()){
			String var = buildIndividualVariable();
			variables.push(var);
			sparql += triple(objectVariable, "a", var);
			filler.accept(this);
			variables.pop();
		} else {
			sparql += triple(objectVariable, "a", filler.asOWLClass().toStringID());
		}
		
		sparql += "} GROUP BY " + subjectVariable + " HAVING(COUNT(" + objectVariable + ")!=" + cardinality + ")}";
	}

	@Override
	public void visit(OWLObjectMaxCardinality ce) {
		String subjectVariable = variables.peek();
		String objectVariable = buildIndividualVariable();
		OWLObjectPropertyExpression propertyExpression = ce.getProperty();
		int cardinality = ce.getCardinality();
		sparql += "{SELECT " + subjectVariable + " WHERE {";
		if(propertyExpression.isAnonymous()){
			//property expression is inverse of a property
			sparql += triple(objectVariable, propertyExpression.getNamedProperty().toStringID(), subjectVariable);
		} else {
			sparql += triple(subjectVariable, propertyExpression.getNamedProperty().toStringID(), objectVariable);
		}
		OWLClassExpression filler = ce.getFiller();
		if(filler.isAnonymous()){
			String var = buildIndividualVariable();
			variables.push(var);
			sparql += triple(objectVariable, "a", var);
			filler.accept(this);
			variables.pop();
		} else {
			sparql += triple(objectVariable, "a", filler.asOWLClass().toStringID());
		}
		
		sparql += "} GROUP BY " + subjectVariable + " HAVING(COUNT(" + objectVariable + ")>" + cardinality + ")}";
	}

	@Override
	public void visit(OWLObjectHasSelf ce) {
		String subject = variables.peek();
		OWLObjectPropertyExpression property = ce.getProperty();
		sparql += triple(subject, property.getNamedProperty().toStringID(), subject);
	}

	@Override
	public void visit(OWLObjectOneOf ce) {
		String subject = variables.peek();
		if(modalDepth() == 1){
			sparql += triple(subject, "?p", "?o");
		} 
		sparql += "FILTER(" + subject + " NOT IN (";
		String values = "";
		for (OWLIndividual ind : ce.getIndividuals()) {
			if(!values.isEmpty()){
				values += ",";
			}
			values += "<" + ind.toStringID() + ">";
		}
		sparql += values;
		sparql +=  "))"; 
		
	}

	@Override
	public void visit(OWLDataSomeValuesFrom ce) {
		String objectVariable = buildIndividualVariable();
		OWLDataPropertyExpression propertyExpression = ce.getProperty();
		sparql += "FILTER NOT EXISTS{";
		sparql += triple(variables.peek(), propertyExpression.asOWLDataProperty().toStringID(), objectVariable);
		OWLDataRange filler = ce.getFiller();
		variables.push(objectVariable);
		filler.accept(this);
		variables.pop();
		sparql += "}";
	}

	@Override
	public void visit(OWLDataAllValuesFrom ce) {
		String subject = variables.peek();
		String objectVariable = buildIndividualVariable();
		OWLDataPropertyExpression propertyExpression = ce.getProperty();
		String predicate = propertyExpression.asOWLDataProperty().toStringID();
		OWLDataRange filler = ce.getFiller();
		sparql += triple(variables.peek(), predicate, objectVariable);
		
		String var = buildIndividualVariable();
		sparql += "{SELECT " + subject + " (COUNT(" + var + ") AS ?cnt1) WHERE {";
		sparql += triple(subject, predicate, var);
		variables.push(var);
		filler.accept(this);
		variables.pop();
		sparql += "} GROUP BY " + subject + "}";
		
		var = buildIndividualVariable();
		sparql += "{SELECT " + subject + " (COUNT(" + var + ") AS ?cnt2) WHERE {";
		sparql += triple(subject, predicate, var);
		sparql += "} GROUP BY " + subject + "}";
		
		sparql += "FILTER(?cnt1=?cnt2)";
	}

	@Override
	public void visit(OWLDataHasValue ce) {
		OWLDataPropertyExpression propertyExpression = ce.getProperty();
		OWLLiteral value = ce.getValue();
		sparql += "FILTER NOT EXISTS{";
		sparql += triple(variables.peek(), propertyExpression.asOWLDataProperty().toStringID(), value);
		sparql += "}";
	}

	@Override
	public void visit(OWLDataMinCardinality ce) {
		String subjectVariable = variables.peek();
		String objectVariable = buildIndividualVariable();
		OWLDataPropertyExpression propertyExpression = ce.getProperty();
		int cardinality = ce.getCardinality();
		sparql += "{SELECT " + subjectVariable + " WHERE {";
		sparql += triple(subjectVariable, propertyExpression.asOWLDataProperty().toStringID(), objectVariable);
		OWLDataRange filler = ce.getFiller();
		variables.push(objectVariable);
		filler.accept(this);
		variables.pop();
		
		sparql += "} GROUP BY " + subjectVariable + " HAVING(COUNT(" + objectVariable + ")>=" + cardinality + ")}";
	}

	@Override
	public void visit(OWLDataExactCardinality ce) {
		String subjectVariable = variables.peek();
		String objectVariable = buildIndividualVariable();
		OWLDataPropertyExpression propertyExpression = ce.getProperty();
		int cardinality = ce.getCardinality();
		sparql += "{SELECT " + subjectVariable + " WHERE {";
		sparql += triple(subjectVariable, propertyExpression.asOWLDataProperty().toStringID(), objectVariable);
		OWLDataRange filler = ce.getFiller();
		variables.push(objectVariable);
		filler.accept(this);
		variables.pop();
		
		sparql += "} GROUP BY " + subjectVariable + " HAVING(COUNT(" + objectVariable + ")=" + cardinality + ")}";
	}

	@Override
	public void visit(OWLDataMaxCardinality ce) {
		String subjectVariable = variables.peek();
		String objectVariable = buildIndividualVariable();
		OWLDataPropertyExpression propertyExpression = ce.getProperty();
		int cardinality = ce.getCardinality();
		sparql += "{SELECT " + subjectVariable + " WHERE {";
		sparql += triple(subjectVariable, propertyExpression.asOWLDataProperty().toStringID(), objectVariable);
		OWLDataRange filler = ce.getFiller();
		variables.push(objectVariable);
		filler.accept(this);
		variables.pop();
		
		sparql += "} GROUP BY " + subjectVariable + " HAVING(COUNT(" + objectVariable + ")<=" + cardinality + ")}";
	}
	
	@Override
	public void visit(OWLDatatype node) {
		if(ignoreGenericTypeStatements && !node.isRDFPlainLiteral()){
			sparql += "FILTER(DATATYPE(" + variables.peek() + "=<" + node.getIRI().toString() + ">))";
		}
	}

	@Override
	public void visit(OWLDataOneOf node) {
		String subject = variables.peek();
		if(modalDepth() == 1){
			sparql += triple(subject, "?p", "?o");
		} 
		sparql += "FILTER(" + subject + " IN (";
		String values = "";
		for (OWLLiteral value : node.getValues()) {
			if(!values.isEmpty()){
				values += ",";
			}
			values += render(value);
		}
		sparql += values;
		sparql +=  "))"; 
	}

	@Override
	public void visit(OWLDataComplementOf node) {
	}

	@Override
	public void visit(OWLDataIntersectionOf node) {
	}

	@Override
	public void visit(OWLDataUnionOf node) {
	}

	@Override
	public void visit(OWLDatatypeRestriction node) {
	}
	
	public static void main(String[] args) throws Exception {
		ToStringRenderer.getInstance().setRenderer(new DLSyntaxObjectRenderer());
		OWLClassExpressionConstraintToSPARQLConverter converter = new OWLClassExpressionConstraintToSPARQLConverter();
		
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
		
		OWLClassExpression expr = clsA;
		String query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectSomeValuesFrom(propR, clsB);
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				df.getOWLObjectSomeValuesFrom(propR, clsB),
				clsB);
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectUnionOf(
				clsA,
				clsB);
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectHasValue(propR, indA);
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectAllValuesFrom(propR, clsB);
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectAllValuesFrom(df.getOWLObjectProperty("language", pm), df.getOWLClass("Language", pm));
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectMinCardinality(2, df.getOWLObjectProperty("language", pm), df.getOWLClass("Language", pm));
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				df.getOWLClass("Place", pm),
				df.getOWLObjectMinCardinality(
						2, 
						df.getOWLObjectProperty("language", pm), 
						df.getOWLClass("Language", pm)));
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectOneOf(indA, indB);
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectSomeValuesFrom(propR, df.getOWLObjectOneOf(indA, indB));
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA, 
				df.getOWLObjectHasSelf(propR));
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA, 
				df.getOWLDataSomeValuesFrom(dpT, booleanRange));
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA, 
				df.getOWLDataHasValue(dpT, lit));
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA, 
				df.getOWLDataMinCardinality(2, dpT, booleanRange));
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectComplementOf(clsB);
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA, 
				df.getOWLObjectComplementOf(clsB));
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectSomeValuesFrom(propR, 
				df.getOWLObjectIntersectionOf(
						clsA, 
						df.getOWLObjectComplementOf(clsB)));
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLDataAllValuesFrom(dpT, booleanRange);
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLDataAllValuesFrom(dpT,df.getOWLDataOneOf(lit));
		query = converter.asQuery(rootVar, expr).toString();
		System.out.println(expr + "\n" + query);
		
	}

	
	
}