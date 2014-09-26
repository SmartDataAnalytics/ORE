package org.aksw.ore.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.aksw.ore.rendering.Renderer;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.learningproblems.AxiomScore;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNaryPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyCharacteristicAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

public class AxiomScoreExplanationGenerator {
	
	private static String patternFile = "score_explanations.txt";
	
	private static Map<AxiomType<OWLAxiom>, String> axiomType2Pattern;
	private static String genericPattern = "Total: $total\n Positive: $pos";
	
	private static boolean initialized = false;
	
	private static Renderer entityRenderer = new Renderer();
	
	public static void init(){
		axiomType2Pattern = new HashMap<AxiomType<OWLAxiom>, String>();

		try (DataInputStream in = new DataInputStream(PatOMatPatternLibrary.class.getClassLoader().getResourceAsStream(patternFile))){

			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = br.readLine()) != null) {
				if(!line.startsWith("#")){
					String[] split = line.split("\\|");
					try {
						AxiomType<OWLAxiom> axiomType = (AxiomType<OWLAxiom>) AxiomType.getAxiomType(split[0].trim());
						String pattern = genericPattern;
						if(split.length == 2){
							pattern = split[1].trim();
						}
						axiomType2Pattern.put(axiomType, pattern);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			initialized = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getAccuracyDescription(EvaluatedAxiom<OWLAxiom> evAxiom){
		return getAccuracyDescription(evAxiom, entityRenderer);
	}
	
	public static String getAccuracyDescription(EvaluatedAxiom<OWLAxiom> evAxiom, Renderer entityRenderer){
		if(!initialized){
			init();
		}
		AxiomScore score = (AxiomScore) evAxiom.getScore();
		OWLAxiom axiom = evAxiom.getAxiom();
		
		String explanationPattern = axiomType2Pattern.get((AxiomType<OWLAxiom>) axiom.getAxiomType());
		if(explanationPattern == null || explanationPattern.isEmpty()){
			explanationPattern = genericPattern;
		}
		
		// set the number of total and positive examples
		explanationPattern = explanationPattern.replace("$total", "<b>" + String.valueOf(score.getTotalNrOfExamples()) + "</b>");
		explanationPattern = explanationPattern.replace("$pos", "<b>" + String.valueOf(score.getNrOfPositiveExamples()) + "</b>");
		
		// get the involved the entities
		OWLEntity prop1 = null;
		OWLEntity prop2 = null;
		OWLEntity cls1 = null;
		OWLEntity cls2 = null;
		OWLEntity datatype = null;
		if (axiom instanceof OWLObjectPropertyCharacteristicAxiom) {
			prop1 = ((OWLObjectPropertyCharacteristicAxiom)axiom).getProperty().asOWLObjectProperty();
		} else if (axiom instanceof OWLSubObjectPropertyOfAxiom) {
			prop1 = ((OWLSubObjectPropertyOfAxiom)axiom).getSubProperty().asOWLObjectProperty();
			prop2 = ((OWLSubObjectPropertyOfAxiom)axiom).getSuperProperty().asOWLObjectProperty();
		} else if (axiom instanceof OWLDisjointObjectPropertiesAxiom) {
			Set<OWLObjectPropertyExpression> properties = ((OWLDisjointObjectPropertiesAxiom) axiom).getProperties();
			Iterator<OWLObjectPropertyExpression> iter = properties.iterator();
			prop1 = iter.next().asOWLObjectProperty();
			prop2 = iter.next().asOWLObjectProperty();
		} else if (axiom instanceof OWLNaryPropertyAxiom) {
			Collection<OWLPropertyExpression> properties = ((OWLNaryPropertyAxiom)axiom).getProperties();
			Iterator<OWLPropertyExpression> iter = properties.iterator();
			OWLPropertyExpression p1 = iter.next();
			OWLPropertyExpression p2 = iter.next();
			prop1 = p1.isDataPropertyExpression() ? ((OWLDataProperty)p1) : ((OWLObjectProperty)p1);
			prop2 = p2.isDataPropertyExpression() ? ((OWLDataProperty)p2) : ((OWLObjectProperty)p2);
		} else if (axiom instanceof OWLObjectPropertyDomainAxiom) {
			prop1 = ((OWLObjectPropertyDomainAxiom)axiom).getProperty().asOWLObjectProperty();
			cls1 = ((OWLObjectPropertyDomainAxiom)axiom).getDomain().asOWLClass();
		} else if (axiom instanceof OWLObjectPropertyRangeAxiom) {
			prop1 = ((OWLObjectPropertyRangeAxiom)axiom).getProperty().asOWLObjectProperty();
			cls1 = ((OWLObjectPropertyRangeAxiom)axiom).getRange().asOWLClass();
		} else if (axiom instanceof OWLSubDataPropertyOfAxiom) {
			prop1 = ((OWLSubDataPropertyOfAxiom)axiom).getSubProperty().asOWLDataProperty();
			prop2 = ((OWLSubDataPropertyOfAxiom)axiom).getSuperProperty().asOWLDataProperty();
		} else if (axiom instanceof OWLFunctionalDataPropertyAxiom) {
			prop1 = ((OWLFunctionalDataPropertyAxiom)axiom).getProperty().asOWLDataProperty();
		} else if (axiom instanceof OWLDataPropertyDomainAxiom) {
			prop1 = ((OWLDataPropertyDomainAxiom)axiom).getProperty().asOWLDataProperty();
			cls1 = ((OWLDataPropertyDomainAxiom)axiom).getDomain().asOWLClass();
		} else if (axiom instanceof OWLDataPropertyRangeAxiom) {
			prop1 = ((OWLDataPropertyRangeAxiom)axiom).getProperty().asOWLDataProperty();
			datatype = ((OWLDataPropertyRangeAxiom)axiom).getRange().asOWLDatatype();
		} else if (axiom instanceof OWLSubClassOfAxiom) {
			cls1 = ((OWLSubClassOfAxiom)axiom).getSubClass().asOWLClass();
			cls2 = ((OWLSubClassOfAxiom)axiom).getSuperClass().asOWLClass();
		} else if (axiom instanceof OWLDisjointClassesAxiom) {
			Collection<OWLClassExpression> classes = ((OWLDisjointClassesAxiom)axiom).getClassExpressions();
			Iterator<OWLClassExpression> iter = classes.iterator();
			cls1 = iter.next().asOWLClass();
			cls2 = iter.next().asOWLClass();
		}
		
		// render the entities
		String prop1String = prop1 != null ? prop1.toStringID() : null;
		String prop2String = prop2 != null ? prop2.toStringID() : null;
		String cls1String = cls1 != null ? cls1.toStringID() : null;
		String cls2String = cls2 != null ? cls2.toStringID() : null;
		String datatypeString = datatype != null ? datatype.toStringID() : null;
		if(entityRenderer != null){
			prop1String = prop1 != null ? entityRenderer.render(prop1) : null;
			prop2String = prop2 != null ? entityRenderer.render(prop2) : null;
			cls1String = cls1 != null ? entityRenderer.render(cls1) : null;
			cls2String = cls2 != null ? entityRenderer.render(cls2) : null;
			datatypeString = datatype != null ? entityRenderer.render(datatype) : null;
		}
		
		// fill the slots with the entities
		if(prop1 != null){
			explanationPattern = explanationPattern.replace("$prop1", "<b>" + prop1String + "</b>");
		}
		if(prop2 != null){
			explanationPattern = explanationPattern.replace("$prop2", "<b>" + prop2String + "</b>");
		}
		if(cls1 != null){
			explanationPattern = explanationPattern.replace("$cls1", "<b>" + cls1String + "</b>");
		}
		if(cls2 != null){
			explanationPattern = explanationPattern.replace("$cls2", "<b>" + cls2String + "</b>");
		}
		if(datatype != null){
			explanationPattern = explanationPattern.replace("$datatype", "<b>" + datatypeString + "</b>");
		}
		explanationPattern = explanationPattern.replaceAll("(\\s)([x,y,z])([\\s|,|.])", "$1<i>$2</i>$3");
		
		
		return explanationPattern;
	}
}
