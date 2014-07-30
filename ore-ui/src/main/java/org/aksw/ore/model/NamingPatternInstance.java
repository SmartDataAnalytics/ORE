/**
 * 
 */
package org.aksw.ore.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ComparisonChain;

/**
 * @author Lorenz Buehmann
 *
 */
public class NamingPatternInstance implements Comparable<NamingPatternInstance>{
	
	private static final Pattern regexPattern = Pattern.compile("\\?OP1_P=(.*);\\?OP1_A=(.*)");
	
	private String subClass;
	private String superClass;
	
	public NamingPatternInstance(String subClass, String superClass) {
		this.subClass = subClass;
		this.superClass = superClass;
	}
	
	public NamingPatternInstance(String patomatOutput) {
		Matcher matcher = regexPattern.matcher(patomatOutput);
		matcher.find();
		this.subClass = matcher.group(2);
		this.superClass = matcher.group(1);
	}
	
	/**
	 * @return the subClass
	 */
	public String getSubClass() {
		return subClass;
	}
	
	/**
	 * @return the superClass
	 */
	public String getSuperClass() {
		return superClass;
	}
	
	public String asPatOMatString(){
		return "?OP1_P=" + superClass + ";?OP1_A=" + subClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((subClass == null) ? 0 : subClass.hashCode());
		result = prime * result + ((superClass == null) ? 0 : superClass.hashCode());
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
		NamingPatternInstance other = (NamingPatternInstance) obj;
		if (subClass == null) {
			if (other.subClass != null)
				return false;
		} else if (!subClass.equals(other.subClass))
			return false;
		if (superClass == null) {
			if (other.superClass != null)
				return false;
		} else if (!superClass.equals(other.superClass))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(NamingPatternInstance o) {
		return ComparisonChain.start().compare(superClass, o.getSuperClass()).compare(subClass, o.getSubClass()).result();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return asPatOMatString();
	}
}
