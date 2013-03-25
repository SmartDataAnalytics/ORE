package org.aksw.mole.ore.util;

import org.dllearner.utilities.Helper;
import org.dllearner.utilities.PrefixCCMap;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.ShortFormProvider;

public class PrefixedShortFromProvider implements ShortFormProvider{
	
	private PrefixCCMap prefixes;
	
	public PrefixedShortFromProvider() {
		prefixes = PrefixCCMap.getInstance();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getShortForm(OWLEntity entity) {
		return Helper.getAbbreviatedString(entity.toStringID(), null, prefixes);
	}

}
