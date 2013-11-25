/**
 * 
 */
package org.aksw.ore.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.ore.model.LearningSetting;
import org.dllearner.learningproblems.EvaluatedDescriptionClass;

/**
 * @author Lorenz Buehmann
 *
 */
public class LearningResultsCache {
	
	Map<LearningSetting, List<EvaluatedDescriptionClass>> cache = new HashMap<LearningSetting, List<EvaluatedDescriptionClass>>(30);
	
	public List<EvaluatedDescriptionClass> getEvaluatedDescriptions(LearningSetting learningSetting){
		return cache.get(learningSetting);
	}
	
	public void putEvaluatedDescriptions(LearningSetting learningSetting, List<EvaluatedDescriptionClass> descriptions){
		cache.put(learningSetting, descriptions);
	}

}
