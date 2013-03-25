package org.aksw.mole.ore.model;

import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntax;

public class AxiomType {
	
	private String type;
	
	public AxiomType() {
	}
	
	public AxiomType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	public static List<AxiomType> getClassAxiomTypes(){
		List<AxiomType> types = new ArrayList<AxiomType>();
		types.add(new AxiomType("SubClassOf"));
		types.add(new AxiomType("EquivalentClasses"));
		types.add(new AxiomType("DisjointClasses"));
		return types;
	}
	
	public static List<AxiomType> getObjectPropertyAxiomTypes(){
		List<AxiomType> types = new ArrayList<AxiomType>();
		types.add(new AxiomType("SubObjectPropertyOf"));
		types.add(new AxiomType("EquivalentObjectProperties"));
		types.add(new AxiomType("DisjointObjectProperties"));
		types.add(new AxiomType("ObjectPropertyDomain"));
		types.add(new AxiomType("ObjectPropertyRange"));
		types.add(new AxiomType("FunctionalObjectProperty"));
		types.add(new AxiomType("InverseFunctionalObjectProperty"));
		types.add(new AxiomType("ReflexiveObjectProperty"));
		types.add(new AxiomType("IrreflexiveObjectProperty"));
		types.add(new AxiomType("SymmetricObjectProperty"));
		types.add(new AxiomType("AsymmetricObjectProperty"));
		types.add(new AxiomType("TransitiveObjectProperty"));
		return types;
	}
	
	public static List<AxiomType> getDataPropertyAxiomTypes(){
		List<AxiomType> types = new ArrayList<AxiomType>();
		types.add(new AxiomType("SubDataPropertyOf"));
		types.add(new AxiomType("EquivalentDataProperties"));
		types.add(new AxiomType("DisjointDataProperties"));
		types.add(new AxiomType("DataPropertyDomain"));
		types.add(new AxiomType("DataPropertyRange"));
		types.add(new AxiomType("FunctionalDataProperty"));
		return types;
	}
	
	public static List<AxiomType> getAllAxiomTypes(){
		List<AxiomType> types = new ArrayList<AxiomType>();
		types.addAll(getClassAxiomTypes());
		types.addAll(getObjectPropertyAxiomTypes());
		types.addAll(getDataPropertyAxiomTypes());
		return types;
	}
	
	public static List<AxiomType> getGoldMinerAxiomTypes(){
		List<AxiomType> types = new ArrayList<AxiomType>();
		/*
		 * c_sub_c=true
c_and_c_sub_c=true
c_sub_exists_p_c=true
exists_p_c_sub_c=true
exists_p_T_sub_c=true
exists_pi_T_sub_c=true
p_sub_p=true
p_chain_p_sub_p=true
c_dis_c=false
		 */
		types.add(new AxiomType("C" + DLSyntax.SUBCLASS + "D"));
		types.add(new AxiomType("C" + DLSyntax.AND + "D" + DLSyntax.SUBCLASS + "E"));
		types.add(new AxiomType(DLSyntax.EXISTS + "r.C" + DLSyntax.SUBCLASS + "D"));
		types.add(new AxiomType(DLSyntax.EXISTS + "r." + DLSyntax.TOP + DLSyntax.SUBCLASS + "D"));
		types.add(new AxiomType(DLSyntax.TOP + "" + DLSyntax.SUBCLASS + DLSyntax.EXISTS + "r.C"));
		types.add(new AxiomType("r" + DLSyntax.SUBCLASS + "s"));
		types.add(new AxiomType("r \u2218 s" + DLSyntax.SUBCLASS + "t"));
		types.add(new AxiomType("C" + DLSyntax.AND + "D" + DLSyntax.SUBCLASS + DLSyntax.BOTTOM));
		return types;
	}
	
	@Override
	public String toString() {
		return getType();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AxiomType other = (AxiomType) obj;
		if (getType() == null) {
			if (other.getType() != null)
				return false;
		} else if (!getType().equals(other.getType()))
			return false;
		return true;
	}

}
