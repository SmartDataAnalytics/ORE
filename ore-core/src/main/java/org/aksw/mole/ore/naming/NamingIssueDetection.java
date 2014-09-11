/**
 * 
 */
package org.aksw.mole.ore.naming;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.aksw.commons.util.Pair;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author Lorenz Buehmann
 *
 */
public class NamingIssueDetection {
	
	private static final Logger logger = LoggerFactory.getLogger(NamingIssueDetection.class);
	
	private StanfordCoreNLP pipeline;
	private SemanticHeadFinder headFinder;
	private IDictionary dict;
	
	private static final String regex = "(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])";
	private final IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
	private static final OWLClass OWL_THING = new OWLDataFactoryImpl().getOWLThing();
	
	//only subclasses that consist of mutiple tokens are considered
	private boolean ignoreSingleTokenSubClasses = true;

	
	public NamingIssueDetection(String wordnetDirectory) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, parse");
		props.put("ssplit.isOneSentence","true");
		pipeline = new StanfordCoreNLP(props);
		
		headFinder = new SemanticHeadFinder();
		
		dict = new Dictionary(new File(wordnetDirectory));
		try {
			dict.open();
		} catch (IOException e) {
			logger.error("WordNet initialization failed.", e);
		}
	}
	
	/**
	 * @param ignoreSingleTokenSubClasses the ignoreSingleTokenSubClasses to set
	 */
	public void setIgnoreSingleTokenSubClasses(boolean ignoreSingleTokenSubClasses) {
		this.ignoreSingleTokenSubClasses = ignoreSingleTokenSubClasses;
	}
	
	public Set<NamingIssue> detectNonExactMatchingDirectChildIssues(Model model){
		long start = System.currentTimeMillis();
		
		//get SubClass - SuperClass pairs via SPARQL query
		QueryExecution qe = QueryExecutionFactory.create(
				"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> PREFIX owl:<http://www.w3.org/2002/07/owl#> "
						+ "SELECT * WHERE {?sub a owl:Class .?sup a owl:Class .?sub rdfs:subClassOf ?sup}", model);
		ResultSet rs = qe.execSelect();

		Set<Pair<String, String>> subClassSuperClassPairs = new HashSet<Pair<String, String>>();
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();

			String subClass = qs.getResource("sub").getURI();
			String superClass = qs.getResource("sup").getURI();

			subClassSuperClassPairs.add(new Pair<String, String>(subClass, superClass));
		}
		qe.close();

		//compute non matching pairs
		Set<NamingIssue> nonMatchingChildren = computeNonExactMatchingChildren(subClassSuperClassPairs); 
		long end = System.currentTimeMillis();
		logger.info("Operation took " + (end - start) + "ms");
		
		return nonMatchingChildren;
	}
	
	public Set<NamingIssue> detectNonExactMatchingDirectChildIssues(OWLOntology ontology){
		long start = System.currentTimeMillis();

		//get SubClass - SuperClass pairs
		Set<Pair<String, String>> subClassSuperClassPairs = new HashSet<Pair<String, String>>();
		Set<OWLSubClassOfAxiom> axioms = ontology.getAxioms(AxiomType.SUBCLASS_OF);
		for (OWLSubClassOfAxiom ax : axioms) {

			OWLClassExpression subClass = ax.getSubClass();
			OWLClassExpression superClass = ax.getSuperClass();

			if (!subClass.isAnonymous() && !superClass.isAnonymous()) {
				subClassSuperClassPairs.add(new Pair<String, String>(subClass.asOWLClass().toStringID(), superClass
						.asOWLClass().toStringID()));
			}
		}

		//compute non matching pairs
		Set<NamingIssue> nonMatchingChildren = computeNonExactMatchingChildren(subClassSuperClassPairs); 
		long end = System.currentTimeMillis();
		logger.info("Operation took " + (end - start) + "ms");
		
		return nonMatchingChildren;
	}
	
	public Set<NamingIssue> detectNonMatchingChildIssues(OWLOntology ontology, boolean directChild){
		long start = System.currentTimeMillis();
		
		//get SubClass - SuperClass pairs
		Set<NamingIssue> nonMatchingChildren = new HashSet<NamingIssue>();
		if(directChild){
			Set<OWLSubClassOfAxiom> axioms = ontology.getAxioms(AxiomType.SUBCLASS_OF);
			for (OWLSubClassOfAxiom ax : axioms) {

				OWLClassExpression subClass = ax.getSubClass();
				OWLClassExpression superClass = ax.getSuperClass();

				if (!subClass.isAnonymous() && !superClass.isAnonymous()) {
					String subClassURI = subClass.asOWLClass().toStringID();
					String superClassURI = superClass.asOWLClass().toStringID();
					
					if(ignoreSingleTokenSubClasses && !singleToken(subClassURI)){
						String subClassHead = getHeadNoun(subClassURI);
						String superClassHead = getHeadNoun(superClassURI);
						
						boolean matching = subClassHead.equals(superClassHead) || isHypernymOf(superClassHead, subClassHead);
						
						if (!matching) {
							String newClassURI = buildNewURI(subClassURI, superClassHead);
							nonMatchingChildren.add(new NamingIssue(subClassURI, superClassURI, new RenamingInstruction(subClassURI, newClassURI)));
						}
					}
				}
			}
		} else {
			OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
			OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);

			Set<OWLClass> classes = ontology.getClassesInSignature();
			
			for (OWLClass cls : classes) {
				Set<OWLClass> superClasses = reasoner.getSuperClasses(cls, false).getFlattened();
				superClasses.remove(OWL_THING);
				for (OWLClass superClass : superClasses) {
					String subClassURI = cls.asOWLClass().toStringID();
					String superClassURI = superClass.asOWLClass().toStringID();
					
					if(ignoreSingleTokenSubClasses && !singleToken(subClassURI)){
						String subClassHead = getHeadNoun(subClassURI);
						String superClassHead = getHeadNoun(superClassURI);
						
						boolean matching = subClassHead.equals(superClassHead) || isHypernymOf(superClassHead, subClassHead);
						
						if (!matching) {
							String newClassURI = buildNewURI(subClassURI, superClassHead);
							nonMatchingChildren.add(new NamingIssue(subClassURI, superClassURI, new RenamingInstruction(subClassURI, newClassURI)));
						}
					}
				}
			}
		}
		
		long end = System.currentTimeMillis();
		logger.info("Operation took " + (end - start) + "ms");
		
		return nonMatchingChildren;
	}
	
	private String buildNewURI(String subClassURI, String superClassHead){
		String shortForm = sfp.getShortForm(IRI.create(subClassURI));
		String newShortForm = shortForm + edu.stanford.nlp.util.StringUtils.capitalize(superClassHead);
		String newClassURI = subClassURI.replace(shortForm, newShortForm);
		return newClassURI;
	}
	
	private Set<NamingIssue> computeNonExactMatchingChildren(Set<Pair<String, String>> allPairs){
		Set<NamingIssue> namingIssues = new HashSet<NamingIssue>();
		
		for (Pair<String,String> pair : allPairs) {
			String subClassURI = pair.getKey();
			String superClassURI = pair.getValue();
			
			if(ignoreSingleTokenSubClasses && !singleToken(subClassURI)){
				String subClassHead = getHeadNoun(subClassURI);
				String superClassHead = getHeadNoun(superClassURI);
				
				if(!subClassHead.equals(superClassHead)){
					String newClassURI = buildNewURI(subClassURI, superClassHead);
					namingIssues.add(new NamingIssue(subClassURI, superClassURI, new RenamingInstruction(subClassURI, newClassURI)));
				}
			}
		}
		
		return namingIssues;
	}
	
	private boolean matchingHeadNoun(String subClass, String superClass){
		String subClassHead = getHeadNoun(subClass);
		String superClassHead = getHeadNoun(superClass);
		
		return subClassHead.equals(superClassHead) || isHypernymOf(superClassHead, subClassHead);
	}
	
	private boolean exactMatchingHeadNoun(String subClass, String superClass){
		String subClassHead = getHeadNoun(subClass);
		String superClassHead = getHeadNoun(superClass);
		
		return subClassHead.equals(superClassHead);
	}
	
	private boolean singleToken(String uri){
		return sfp.getShortForm(IRI.create(uri)).split(regex).length == 1;
	}
	
	/**
	 * Lexicalize URI by using the short form and split into tokens.
	 * 
	 * @param uri
	 * @return
	 */
	private String[] lexicalize(String uri) {
		// get short form
		String s = sfp.getShortForm(IRI.create(uri));

		// split by camel case
		String[] tokens = s.split(regex);

		// to lower case
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			if (token.length() == 1 || (Character.isUpperCase(token.charAt(0)) && !Character.isUpperCase(token.charAt(1)))) {
				tokens[i] = token.toLowerCase();
			}
		}

		return tokens;
	}
	
	private String getHeadNoun(String uri){
		String[] tokens = lexicalize(uri);
		
		//if we have multiple tokens, get the head noun
		String head;
		if(tokens.length > 1){
			head = Joiner.on(" ").join(tokens);
			
			Annotation document = new Annotation(head);
			pipeline.annotate(document);
			
			CoreMap sentence = document.get(SentencesAnnotation.class).get(0);
			Tree tree = sentence.get(TreeAnnotation.class);
			
			Tree headTree = headFinder.determineHead(tree);
			//we assume that the last occurring NN is the head noun
			List<Tree> leaves = headTree.getLeaves();
			head = leaves.get(leaves.size()-1).label().value();
		} else {
			head = tokens[0];
		}
		return head;
	}
	/**
	 * Returns TRUE if word1 is hypernym of word2
	 * @param word1
	 * @param word2
	 * @return
	 */
	private boolean isHypernymOf(String word1, String word2){
		//get hypernyms of word2
		Set<String> hypernyms = getHypernyms(word2, POS.NOUN, true, 5);
//		logger.info("Hypernyms of " + word2 + ":" + hypernyms);
		return hypernyms.contains(word1);
	}
	 
	private Set<String> getHypernyms(String word, POS posTag, boolean firstSenseOnly, int maxIterations) {

		Set<String> hypernyms = new HashSet<String>();

		IIndexWord iIndexWordRoot = dict.getIndexWord(word, posTag);
		if (iIndexWordRoot == null) {
			return hypernyms; // no senses found
		}
		
		List<IWordID> todo = iIndexWordRoot.getWordIDs();
		
		int iterations = 0;
		
		while(iterations++ < maxIterations && !todo.isEmpty()){
			List<IWordID> tmp = Lists.newArrayList();
			// iterate over senses
			for (IWordID iWordId : todo) {
				IWord iWord1 = dict.getWord(iWordId);
				ISynset iSynset = iWord1.getSynset();

				// multiple hypernym chains are possible for a synset
				for (ISynsetID iSynsetId : iSynset.getRelatedSynsets(Pointer.HYPERNYM)) {
					List<IWord> iWords = dict.getSynset(iSynsetId).getWords();
					for (IWord iWord2 : iWords) {
						String lemma = iWord2.getLemma().replace('_', ' ');
						hypernyms.add(lemma); 
						tmp.add(iWord2.getID());
					}
				}

				if (firstSenseOnly) {
					break;
				}
			}
			todo = tmp;
		}

		return hypernyms;
	}
	
	public static void main(String[] args) throws Exception {
		String url = "https://raw.github.com/structureddynamics/Bibliographic-Ontology-BIBO/master/bibo.xml.owl";
		NamingIssueDetection namingIssueDetection = new NamingIssueDetection("/opt/wordnet");
		
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
		model.read(url, null);
		
		Set<NamingIssue> nonMatchingChildren = namingIssueDetection.detectNonExactMatchingDirectChildIssues(model);
		System.out.println(nonMatchingChildren);
		
		nonMatchingChildren = namingIssueDetection.detectNonExactMatchingDirectChildIssues(model);
		System.out.println(nonMatchingChildren);
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = man.loadOntology(IRI.create(url));
		
		nonMatchingChildren = namingIssueDetection.detectNonExactMatchingDirectChildIssues(ontology);
		System.out.println(nonMatchingChildren);
		
		nonMatchingChildren = namingIssueDetection.detectNonExactMatchingDirectChildIssues(ontology);
		System.out.println(nonMatchingChildren);
		
		nonMatchingChildren = namingIssueDetection.detectNonMatchingChildIssues(ontology, false);
		System.out.println(nonMatchingChildren);
		
		nonMatchingChildren = namingIssueDetection.detectNonMatchingChildIssues(ontology, true);
		System.out.println(nonMatchingChildren);
	}

}
