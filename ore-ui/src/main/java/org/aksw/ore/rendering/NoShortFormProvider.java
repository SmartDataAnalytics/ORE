/**
 * 
 */
package org.aksw.ore.rendering;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.ShortFormProvider;

/**
 * Returns the IRI instead of the short form when an OWL entity is rendered.
 * @author Lorenz Buehmann
 *
 */
public class NoShortFormProvider implements ShortFormProvider {
	
	@Override
	public String getShortForm(OWLEntity entity) {
		return entity.toStringID();
	}

	@Override
	public void dispose() {
	}
}
