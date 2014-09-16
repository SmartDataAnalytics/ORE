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

import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.learningproblems.AxiomScore;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNaryPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyCharacteristicAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

public class ScoreExplanationPattern {
	
	private static String patternFile = "score_explanations.txt";
	
	private static Map<AxiomType, String> axiomType2Pattern;
	private static String genericPattern = "Total: $total\n Positive: $pos";
	
	public static void init(){
		axiomType2Pattern = new HashMap<AxiomType, String>();
		
		DataInputStream in = null;
		try {
			in = new DataInputStream(PatOMatPatternLibrary.class.getClassLoader().getResourceAsStream(patternFile));
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = br.readLine()) != null) {
				if(!line.startsWith("#")){
					String[] split = line.split("\\|");
					try {
						AxiomType axiomType = AxiomType.getAxiomType(split[0].trim());
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
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public static String getExplanationPattern(AxiomType axiomType){
		return axiomType2Pattern.get(axiomType);
	}
	
	public static String getGenericPattern() {
		return genericPattern;
	}
	
	public static String getAccuracyDescription(AxiomType axiomType, EvaluatedAxiom evAxiom){
		AxiomScore score = (AxiomScore) evAxiom.getScore();
		String explanationPattern = ScoreExplanationPattern.getExplanationPattern(axiomType);
		if(explanationPattern == null || explanationPattern.isEmpty()){
			explanationPattern = ScoreExplanationPattern.getGenericPattern();
		}
		explanationPattern = explanationPattern.replace("$total", "<b>" + String.valueOf(score.getTotalNrOfExamples()) + "</b>");
		explanationPattern = explanationPattern.replace("$pos", "<b>" + String.valueOf(score.getNrOfPositiveExamples()) + "</b>");
		OWLAxiom axiom = evAxiom.getAxiom();
		String prop1 = null;
		String prop2 = null;
		String cls1 = null;
		String cls2 = null;
		String datatype = null;
		if (axiom instanceof OWLObjectPropertyCharacteristicAxiom) {
			prop1 = ((OWLObjectPropertyCharacteristicAxiom)axiom).getProperty().asOWLObjectProperty().toStringID();
		} else if (axiom instanceof OWLSubObjectPropertyOfAxiom) {
			prop1 = ((OWLSubObjectPropertyOfAxiom)axiom).getSubProperty().toString();
			prop2 = ((OWLSubObjectPropertyOfAxiom)axiom).getSuperProperty().toString();
		} else if (axiom instanceof OWLDisjointObjectPropertiesAxiom) {
			Set<OWLObjectPropertyExpression> properties = ((OWLDisjointObjectPropertiesAxiom) axiom).getProperties();
			Iterator<OWLObjectPropertyExpression> iter = properties.iterator();
			prop1 = iter.next().toString();
			prop2 = iter.next().toString();
		} else if (axiom instanceof OWLNaryPropertyAxiom) {
			Collection<OWLPropertyExpression> properties = ((OWLNaryPropertyAxiom)axiom).getProperties();
			Iterator<OWLPropertyExpression> iter = properties.iterator();
			prop1 = iter.next().toString();
			prop2 = iter.next().toString();
		} else if (axiom instanceof OWLObjectPropertyDomainAxiom) {
			prop1 = ((OWLObjectPropertyDomainAxiom)axiom).getProperty().toString();
			cls1 = ((OWLObjectPropertyDomainAxiom)axiom).getDomain().toString();
		} else if (axiom instanceof OWLObjectPropertyRangeAxiom) {
			prop1 = ((OWLObjectPropertyRangeAxiom)axiom).getProperty().toString();
			cls1 = ((OWLObjectPropertyRangeAxiom)axiom).getRange().toString();
		} else if (axiom instanceof OWLSubDataPropertyOfAxiom) {
			prop1 = ((OWLSubDataPropertyOfAxiom)axiom).getSubProperty().toString();
			prop2 = ((OWLSubDataPropertyOfAxiom)axiom).getSuperProperty().toString();
		} else if (axiom instanceof OWLFunctionalDataPropertyAxiom) {
			prop1 = ((OWLFunctionalDataPropertyAxiom)axiom).getProperty().asOWLDataProperty().toStringID();
		} else if (axiom instanceof OWLDataPropertyDomainAxiom) {
			prop1 = ((OWLDataPropertyDomainAxiom)axiom).getProperty().toString();
			cls1 = ((OWLDataPropertyDomainAxiom)axiom).getDomain().toString();
		} else if (axiom instanceof OWLDataPropertyRangeAxiom) {
			prop1 = ((OWLDataPropertyRangeAxiom)axiom).getProperty().toString();
			datatype = ((OWLDataPropertyRangeAxiom)axiom).getRange().toString();
		} else if (axiom instanceof OWLSubClassOfAxiom) {
			cls1 = ((OWLSubClassOfAxiom)axiom).getSubClass().toString();
			cls2 = ((OWLSubClassOfAxiom)axiom).getSuperClass().toString();
		} else if (axiom instanceof OWLDisjointClassesAxiom) {
			Collection<OWLClassExpression> classes = ((OWLDisjointClassesAxiom)axiom).getClassExpressions();
			Iterator<OWLClassExpression> iter = classes.iterator();
			cls1 = iter.next().toString();
			cls2 = iter.next().toString();
		}
		if(prop1 != null){
			explanationPattern = explanationPattern.replace("$prop1", "<b>" + prop1 + "</b>");
		}
		if(prop2 != null){
			explanationPattern = explanationPattern.replace("$prop2", "<b>" + prop2 + "</b>");
		}
		if(cls1 != null){
			explanationPattern = explanationPattern.replace("$cls1", "<b>" + cls1 + "</b>");
		}
		if(cls2 != null){
			explanationPattern = explanationPattern.replace("$cls2", "<b>" + cls2 + "</b>");
		}
		if(datatype != null){
			explanationPattern = explanationPattern.replace("$datatype", "<b>" + datatype + "</b>");
		}
		explanationPattern = explanationPattern.replaceAll("(\\s)([x,y,z])([\\s|,|.])", "$1<i>$2</i>$3");
		
		
		return explanationPattern;
	}
}
