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

import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.core.owl.AsymmetricObjectPropertyAxiom;
import org.dllearner.core.owl.Axiom;
import org.dllearner.core.owl.DatatypePropertyDomainAxiom;
import org.dllearner.core.owl.DatatypePropertyRangeAxiom;
import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.DisjointClassesAxiom;
import org.dllearner.core.owl.DisjointDatatypePropertyAxiom;
import org.dllearner.core.owl.DisjointObjectPropertyAxiom;
import org.dllearner.core.owl.EquivalentDatatypePropertiesAxiom;
import org.dllearner.core.owl.EquivalentObjectPropertiesAxiom;
import org.dllearner.core.owl.FunctionalDatatypePropertyAxiom;
import org.dllearner.core.owl.FunctionalObjectPropertyAxiom;
import org.dllearner.core.owl.InverseFunctionalObjectPropertyAxiom;
import org.dllearner.core.owl.IrreflexiveObjectPropertyAxiom;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.core.owl.ObjectPropertyDomainAxiom;
import org.dllearner.core.owl.ObjectPropertyRangeAxiom;
import org.dllearner.core.owl.ReflexiveObjectPropertyAxiom;
import org.dllearner.core.owl.SubClassAxiom;
import org.dllearner.core.owl.SubDatatypePropertyAxiom;
import org.dllearner.core.owl.SubObjectPropertyAxiom;
import org.dllearner.core.owl.SymmetricObjectPropertyAxiom;
import org.dllearner.core.owl.TransitiveObjectPropertyAxiom;
import org.dllearner.learningproblems.AxiomScore;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

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
		Axiom axiom = evAxiom.getAxiom();
		OWLAxiom owlAxiom = OWLAPIConverter.getOWLAPIAxiom(axiom);
		String prop1 = null;
		String prop2 = null;
		String cls1 = null;
		String cls2 = null;
		String datatype = null;
		if (axiom instanceof TransitiveObjectPropertyAxiom) {
			prop1 = ((TransitiveObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof FunctionalObjectPropertyAxiom) {
			prop1 = ((FunctionalObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof InverseFunctionalObjectPropertyAxiom) {
			prop1 = ((InverseFunctionalObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof ReflexiveObjectPropertyAxiom) {
			prop1 = ((ReflexiveObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof IrreflexiveObjectPropertyAxiom) {
			prop1 = ((IrreflexiveObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof SymmetricObjectPropertyAxiom) {
			prop1 = ((SymmetricObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof AsymmetricObjectPropertyAxiom) {
			prop1 = ((AsymmetricObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof SubObjectPropertyAxiom) {
			prop1 = ((SubObjectPropertyAxiom)axiom).getSubRole().getName();
			prop2 = ((SubObjectPropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof DisjointObjectPropertyAxiom) {
			prop1 = ((DisjointObjectPropertyAxiom)axiom).getRole().getName();
			prop2 = ((DisjointObjectPropertyAxiom)axiom).getDisjointRole().getName();
		} else if (axiom instanceof EquivalentObjectPropertiesAxiom) {
			Collection<ObjectProperty> properties = ((EquivalentObjectPropertiesAxiom)axiom).getEquivalentProperties();
			Iterator<ObjectProperty> iter = properties.iterator();
			prop1 = iter.next().getName();
			prop2 = iter.next().getName();
		} else if (axiom instanceof ObjectPropertyDomainAxiom) {
			prop1 = ((ObjectPropertyDomainAxiom)axiom).getProperty().getName();
			cls1 = ((ObjectPropertyDomainAxiom)axiom).getDomain().toString();
		} else if (axiom instanceof ObjectPropertyRangeAxiom) {
			prop1 = ((ObjectPropertyRangeAxiom)axiom).getProperty().getName();
			cls1 = ((ObjectPropertyRangeAxiom)axiom).getRange().toString();
		} else if (axiom instanceof SubDatatypePropertyAxiom) {
			prop1 = ((SubDatatypePropertyAxiom)axiom).getSubRole().getName();
			prop2 = ((SubDatatypePropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof EquivalentDatatypePropertiesAxiom) {
			prop1 = ((EquivalentDatatypePropertiesAxiom)axiom).getRole().getName();
			prop2 = ((EquivalentDatatypePropertiesAxiom)axiom).getEquivalentRole().getName();
		} else if (axiom instanceof DisjointDatatypePropertyAxiom) {
			prop1 = ((DisjointDatatypePropertyAxiom)axiom).getRole().getName();
			prop2 = ((DisjointDatatypePropertyAxiom)axiom).getDisjointRole().getName();
		} else if (axiom instanceof FunctionalDatatypePropertyAxiom) {
			prop1 = ((FunctionalDatatypePropertyAxiom)axiom).getRole().getName();
		} else if (axiom instanceof DatatypePropertyDomainAxiom) {
			prop1 = ((DatatypePropertyDomainAxiom)axiom).getProperty().getName();
			cls1 = ((DatatypePropertyDomainAxiom)axiom).getDomain().toString();
		} else if (axiom instanceof DatatypePropertyRangeAxiom) {
			prop1 = ((DatatypePropertyRangeAxiom)axiom).getProperty().getName();
			datatype = ((DatatypePropertyRangeAxiom)axiom).getRange().toString();
		} else if (axiom instanceof SubClassAxiom) {
			cls1 = ((SubClassAxiom)axiom).getSubConcept().toString();
			cls2 = ((SubClassAxiom)axiom).getSuperConcept().toString();
		} else if (axiom instanceof DisjointClassesAxiom) {
			Collection<Description> classes = ((DisjointClassesAxiom)axiom).getDescriptions();
			Iterator<Description> iter = classes.iterator();
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
