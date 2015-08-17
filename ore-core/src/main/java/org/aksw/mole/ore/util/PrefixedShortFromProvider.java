package org.aksw.mole.ore.util;

import org.dllearner.algorithms.qtl.util.PrefixCCPrefixMapping;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.ShortFormProvider;

public class PrefixedShortFromProvider implements ShortFormProvider{
	
	public PrefixedShortFromProvider() {
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getShortForm(OWLEntity entity) {
		return PrefixCCPrefixMapping.Full.shortForm(entity.toStringID());
	}

}
