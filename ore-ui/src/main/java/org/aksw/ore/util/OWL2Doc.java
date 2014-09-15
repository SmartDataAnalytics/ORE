/**
 * 
 */
package org.aksw.ore.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.semanticweb.owlapi.model.AxiomType;

/**
 * @author Lorenz Buehmann
 *
 */
public class OWL2Doc {
	
	private static final String DOCUMENTATION_URL = "http://www.w3.org/TR/owl2-syntax/";
	private Document doc;
	
	private static Map<AxiomType, String> axiomType2anchor = new HashMap<AxiomType, String>();
	
	static {
		axiomType2anchor.put(AxiomType.SUB_OBJECT_PROPERTY, "Object_Subproperties");
		axiomType2anchor.put(AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "Equivalent_Object_Properties");
		axiomType2anchor.put(AxiomType.DISJOINT_OBJECT_PROPERTIES, "Disjoint_Object_Properties");
		axiomType2anchor.put(AxiomType.INVERSE_OBJECT_PROPERTIES, "Inverse_Object_Properties_2");
		axiomType2anchor.put(AxiomType.OBJECT_PROPERTY_DOMAIN, "Object_Property_Domain");
		axiomType2anchor.put(AxiomType.OBJECT_PROPERTY_RANGE, "Object_Property_Range");
		axiomType2anchor.put(AxiomType.FUNCTIONAL_OBJECT_PROPERTY, "Functional_Object_Properties");
		axiomType2anchor.put(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY, "Inverse-Functional_Object_Properties");
		axiomType2anchor.put(AxiomType.REFLEXIVE_OBJECT_PROPERTY, "Reflexive_Object_Properties");
		axiomType2anchor.put(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY, "Irreflexive_Object_Properties");
		axiomType2anchor.put(AxiomType.ASYMMETRIC_OBJECT_PROPERTY, "Asymmetric_Object_Properties");
		axiomType2anchor.put(AxiomType.SYMMETRIC_OBJECT_PROPERTY, "Symmetric_Object_Properties");
		axiomType2anchor.put(AxiomType.TRANSITIVE_OBJECT_PROPERTY, "Transitive_Object_Properties");
		
		axiomType2anchor.put(AxiomType.SUB_DATA_PROPERTY, "Data_Subproperties");
		axiomType2anchor.put(AxiomType.EQUIVALENT_DATA_PROPERTIES, "Equivalent_Data_Properties");
		axiomType2anchor.put(AxiomType.DISJOINT_DATA_PROPERTIES, "Disjoint_Data_Properties");
		axiomType2anchor.put(AxiomType.DATA_PROPERTY_DOMAIN, "Data_Property_Domain");
		axiomType2anchor.put(AxiomType.DATA_PROPERTY_RANGE, "Data_Property_Range");
		axiomType2anchor.put(AxiomType.FUNCTIONAL_DATA_PROPERTY, "Functional_Data_Properties");
		
		axiomType2anchor.put(AxiomType.SUBCLASS_OF, "Subclass_Axioms");
		axiomType2anchor.put(AxiomType.EQUIVALENT_CLASSES, "Equivalent_Classes");
		axiomType2anchor.put(AxiomType.DISJOINT_CLASSES, "Disjoint_Classes");
		axiomType2anchor.put(AxiomType.DISJOINT_UNION, "Disjoint_Union_of_Class_Expressions");
	}
	
	public OWL2Doc() {
		
		try {
			URL url = new URL(DOCUMENTATION_URL);
			doc = Jsoup.parse(url, 60000);
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getDoc(AxiomType axiomType){
		
		String anchorID = axiomType2anchor.get(axiomType);
		
		String documentation = "";
		// we start from the anchor node
		Element el = doc.getElementById(anchorID);
		
		// order is: header, description, (optional) subclass expression, grammar, example
		
		// get the description
		el = el.nextElementSibling().nextElementSibling();
		documentation += el.html();
		
		// get the example
		while(!el.hasClass("anexample")){
			el = el.nextElementSibling();
		}
		Iterator<Element> children = el.children().listIterator();
		while (children.hasNext()) {
			Element child = (Element) children.next();
			if(child.hasClass("rdf")){
				child.remove();
			} 
		}
		documentation += el.html();
		
		return documentation;
	}
	
	public static void main(String[] args) throws Exception {
		OWL2Doc doc = new OWL2Doc();
		String documentation = doc.getDoc(AxiomType.FUNCTIONAL_OBJECT_PROPERTY);
		System.out.println(documentation);
		
	}

}
