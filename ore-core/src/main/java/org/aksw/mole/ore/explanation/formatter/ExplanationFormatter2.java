package org.aksw.mole.ore.explanation.formatter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

import uk.ac.manchester.cs.bhig.util.Tree;
import uk.ac.manchester.cs.owl.explanation.ordering.DefaultExplanationOrderer;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationTree;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class ExplanationFormatter2 {
	
	private DefaultExplanationOrderer orderer;
	private LoadingCache<Explanation<OWLAxiom>, FormattedExplanation> cache = CacheBuilder.newBuilder()
		       .maximumSize(100)
		       .expireAfterWrite(10, TimeUnit.MINUTES)
		       .build(
		           new CacheLoader<Explanation<OWLAxiom>, FormattedExplanation>() {
		             public FormattedExplanation load(Explanation<OWLAxiom> explanation){
		               return format(explanation);
		             }
		           });
	
	public ExplanationFormatter2() {
		orderer = new DefaultExplanationOrderer();
	}
	
	public FormattedExplanation getFormattedExplanation(Explanation<OWLAxiom> explanation){
		try {
			return cache.get(explanation);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private FormattedExplanation format(Explanation<OWLAxiom> explanation){
		ExplanationTree tree = orderer.getOrderedExplanation(explanation.getEntailment(), explanation.getAxioms());tree.dump(new PrintWriter(System.out), 2);
		List<OWLAxiom> orderedAxioms = new ArrayList<OWLAxiom>();
		Map<OWLAxiom, Integer> axiom2Indention = new HashMap<OWLAxiom, Integer>();
		fill(tree, orderedAxioms, axiom2Indention);
		
		return new FormattedExplanation(explanation, orderedAxioms, axiom2Indention);
	}
	
	private void fill(Tree<OWLAxiom> tree, List<OWLAxiom> orderedAxioms, Map<OWLAxiom, Integer> axiom2Indention) {
        if (!tree.isRoot()) {
        	orderedAxioms.add(tree.getUserObject());
        	axiom2Indention.put(tree.getUserObject(), tree.getPathToRoot().size() - 2);
        }
        for(Tree<OWLAxiom> child : tree.getChildren()) {
            fill(child, orderedAxioms, axiom2Indention);
        }
    }
	
	public class FormattedExplanation {
		
		private Explanation<OWLAxiom> explanation;
		private List<OWLAxiom> orderedAxioms;
		private Map<OWLAxiom, Integer> axiom2Indention;

		public FormattedExplanation(Explanation<OWLAxiom> explanation, List<OWLAxiom> orderedAxioms, Map<OWLAxiom, Integer> axiom2Indention) {
			this.explanation = explanation;
			this.orderedAxioms = orderedAxioms;
			this.axiom2Indention = axiom2Indention;
		}
		
		public Explanation<OWLAxiom> getExplanation() {
			return explanation;
		}
		
		public List<OWLAxiom> getOrderedAxioms() {
			return orderedAxioms;
		}
		
		public int getIndention(OWLAxiom axiom){
			return axiom2Indention.get(axiom);
		}
	}

}
