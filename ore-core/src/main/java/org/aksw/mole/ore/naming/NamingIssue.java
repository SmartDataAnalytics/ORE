/**
 * 
 */
package org.aksw.mole.ore.naming;

/**
 * @author Lorenz Buehmann
 *
 */
public class NamingIssue {
	
	private String subClass;
	private String superClass;
	
	private RenamingInstruction renamingInstruction;
	
	public NamingIssue(String subClass, String superClass, RenamingInstruction renamingInstruction) {
		this.subClass = subClass;
		this.superClass = superClass;
		this.renamingInstruction = renamingInstruction;
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
	
	/**
	 * @return the renamingInstruction
	 */
	public RenamingInstruction getRenamingInstruction() {
		return renamingInstruction;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((renamingInstruction == null) ? 0 : renamingInstruction.hashCode());
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
		NamingIssue other = (NamingIssue) obj;
		if (renamingInstruction == null) {
			if (other.renamingInstruction != null)
				return false;
		} else if (!renamingInstruction.equals(other.renamingInstruction))
			return false;
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
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SubClass=" + subClass + ";SuperClass=" + superClass + ";Renaming=" + renamingInstruction;
	}

}
