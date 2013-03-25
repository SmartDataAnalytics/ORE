package org.aksw.mole.ore.explanation.impl;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationException;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class CachingExplanationGenerator2<T> implements ExplanationGenerator<T>{
	
	private ExplanationGenerator<T> delegate;
	private Cache<T, Set<Explanation<T>>> cache = CacheBuilder.newBuilder().maximumSize(100).build();
	
	public CachingExplanationGenerator2(ExplanationGenerator<T> explanationGenerator) {
		this.delegate = explanationGenerator;
	}

	@Override
	public Set<Explanation<T>> getExplanations(T entailment) throws ExplanationException {
		return getExplanations(entailment, Integer.MAX_VALUE);
	}

	@Override
	public Set<Explanation<T>> getExplanations(final T entailment, final int limit) throws ExplanationException {
		try {
			return cache.get(entailment, new Callable<Set<Explanation<T>>>() {
			    @Override
			    public Set<Explanation<T>> call() throws ExplanationException {
			      return delegate.getExplanations(entailment, limit);
			    }
			  });
		} catch (ExecutionException e) {
			throw new ExplanationException(e);
		}
	}
	
	public void clear(){
		cache.invalidateAll();
	}
	
	
}
