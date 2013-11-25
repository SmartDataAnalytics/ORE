/**
 * 
 */
package org.aksw.ore.model;

import org.dllearner.core.owl.NamedClass;

/**
 * @author Lorenz Buehmann
 *
 */
public class LearningSetting {
	
	private NamedClass classToDescribe;
	private int maxNrOfResults;
	private int maxExecutionTimeInSeconds;
	
	private double noise;
	private double threshold;
	
	private boolean useUniversalQuantifier;
	private boolean useExistentialQuantifier;
	private boolean useNegation;
	private boolean useHasValue;
	private boolean useCardinality;
	
	private int cardinalityLimit;

	public LearningSetting(NamedClass classToDescribe, int maxNrOfResults, int maxExecutionTimeInSeconds, double noise,
			double threshold, boolean useUniversalQuantifier, boolean useExistentialQuantifier, boolean useNegation,
			boolean useHasValue, boolean useCardinality, int cardinalityLimit) {
		super();
		this.classToDescribe = classToDescribe;
		this.maxNrOfResults = maxNrOfResults;
		this.maxExecutionTimeInSeconds = maxExecutionTimeInSeconds;
		this.noise = noise;
		this.threshold = threshold;
		this.useUniversalQuantifier = useUniversalQuantifier;
		this.useExistentialQuantifier = useExistentialQuantifier;
		this.useNegation = useNegation;
		this.useHasValue = useHasValue;
		this.useCardinality = useCardinality;
		this.cardinalityLimit = cardinalityLimit;
	}
	
	public void setClassToDescribe(NamedClass classToDescribe) {
		this.classToDescribe = classToDescribe;
	}

	public NamedClass getClassToDescribe() {
		return classToDescribe;
	}

	public int getMaxNrOfResults() {
		return maxNrOfResults;
	}

	public int getMaxExecutionTimeInSeconds() {
		return maxExecutionTimeInSeconds;
	}

	public double getNoise() {
		return noise;
	}

	public double getThreshold() {
		return threshold;
	}

	public boolean isUseUniversalQuantifier() {
		return useUniversalQuantifier;
	}

	public boolean isUseExistentialQuantifier() {
		return useExistentialQuantifier;
	}

	public boolean isUseNegation() {
		return useNegation;
	}

	public boolean isUseHasValue() {
		return useHasValue;
	}

	public boolean isUseCardinality() {
		return useCardinality;
	}

	public int getCardinalityLimit() {
		return cardinalityLimit;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cardinalityLimit;
		result = prime * result + ((classToDescribe == null) ? 0 : classToDescribe.hashCode());
		result = prime * result + maxExecutionTimeInSeconds;
		result = prime * result + maxNrOfResults;
		long temp;
		temp = Double.doubleToLongBits(noise);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(threshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (useCardinality ? 1231 : 1237);
		result = prime * result + (useExistentialQuantifier ? 1231 : 1237);
		result = prime * result + (useHasValue ? 1231 : 1237);
		result = prime * result + (useNegation ? 1231 : 1237);
		result = prime * result + (useUniversalQuantifier ? 1231 : 1237);
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
		LearningSetting other = (LearningSetting) obj;
		if (cardinalityLimit != other.cardinalityLimit)
			return false;
		if (classToDescribe == null) {
			if (other.classToDescribe != null)
				return false;
		} else if (!classToDescribe.equals(other.classToDescribe))
			return false;
		if (maxExecutionTimeInSeconds != other.maxExecutionTimeInSeconds)
			return false;
		if (maxNrOfResults != other.maxNrOfResults)
			return false;
		if (Double.doubleToLongBits(noise) != Double.doubleToLongBits(other.noise))
			return false;
		if (Double.doubleToLongBits(threshold) != Double.doubleToLongBits(other.threshold))
			return false;
		if (useCardinality != other.useCardinality)
			return false;
		if (useExistentialQuantifier != other.useExistentialQuantifier)
			return false;
		if (useHasValue != other.useHasValue)
			return false;
		if (useNegation != other.useNegation)
			return false;
		if (useUniversalQuantifier != other.useUniversalQuantifier)
			return false;
		return true;
	}
}
