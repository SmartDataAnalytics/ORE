/**
 * 
 */
package org.aksw.mole.ore.naming;

/**
 * @author Lorenz Buehmann
 *
 */
public class RenamingInstruction {
	
	private String originalURI;
	private String newURI;
	
	public RenamingInstruction(String originalURI, String newURI) {
		this.originalURI = originalURI;
		this.newURI = newURI;
	}
	
	/**
	 * @return the originalURI
	 */
	public String getOriginalURI() {
		return originalURI;
	}
	
	/**
	 * @return the newURI
	 */
	public String getNewURI() {
		return newURI;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return originalURI + "-->" + newURI;
	}

}
