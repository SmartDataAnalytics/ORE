package org.aksw.mole.ore;

import org.aksw.mole.ore.explanation.api.ExplanationType;


public interface ExplanationManagerListener {
	
	void explanationLimitChanged(int explanationLimit);
	void explanationTypeChanged(ExplanationType type);
	

}
