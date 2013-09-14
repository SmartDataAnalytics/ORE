package org.aksw.ore.manager;

import org.aksw.mole.ore.explanation.api.ExplanationType;


public interface ExplanationManagerListener {
	
	void explanationLimitChanged(int explanationLimit);
	void explanationTypeChanged(ExplanationType type);
	

}
