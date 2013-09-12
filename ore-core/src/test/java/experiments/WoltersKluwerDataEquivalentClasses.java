package experiments;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

import org.aksw.mole.ore.util.PrefixedShortFromProvider;
import org.apache.log4j.Logger;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.algorithms.pattern.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.dllearner.algorithms.qtl.QTL;
import org.dllearner.algorithms.qtl.cache.QueryTreeCache;
import org.dllearner.algorithms.qtl.datastructures.QueryTree;
import org.dllearner.algorithms.qtl.operations.lgg.EvaluatedQueryTree;
import org.dllearner.algorithms.qtl.operations.lgg.NoiseSensitiveLGG;
import org.dllearner.algorithms.qtl.operations.lgg.NoiseSensitiveLGGMultithreaded;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.core.LearningProblemUnsupportedException;
import org.dllearner.core.Score;
import org.dllearner.core.owl.Axiom;
import org.dllearner.core.owl.EquivalentClassesAxiom;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.learningproblems.ClassLearningProblem;
import org.dllearner.learningproblems.Heuristics.HeuristicType;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.FastInstanceChecker;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.utilities.Helper;
import org.dllearner.utilities.datastructures.Datastructures;
import org.dllearner.utilities.datastructures.SetManipulation;
import org.dllearner.utilities.datastructures.SortedSetTuple;
import org.dllearner.utilities.examples.AutomaticNegativeExampleFinderSPARQL2;
import org.dllearner.utilities.owl.OWLEntityTypeAdder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

public class WoltersKluwerDataEquivalentClasses {

	private static final Logger logger = Logger.getLogger(WoltersKluwerDataEquivalentClasses.class.getName());
	private static ExtractionDBCache wkCache = new ExtractionDBCache("wk_cache");
	private static SparqlEndpoint wkEndpoint1;// Arbeitsrechtthesaurus
	private static SparqlEndpoint wkEndpoint2;// Gerichtsthesaurus
	private static SparqlEndpoint wkEndpoint3;

	
	static {
		try {
			wkEndpoint1 = new SparqlEndpoint(
					new URL("http://vocabulary.wolterskluwer.de/PoolParty/sparql/arbeitsrecht"));
			wkEndpoint2 = new SparqlEndpoint(new URL("http://vocabulary.wolterskluwer.de/PoolParty/sparql/court"));
			wkEndpoint3 = new SparqlEndpoint(new URL("http://lod2.wolterskluwer.de/virtuoso/sparql"));
//			wkEndpoint3 = SparqlEndpoint.getEndpointDBpedia();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private String wkNS = "http://schema.wolterskluwer.de/";
	private final int maxQueryTreeDepth = 0;// 0 means only direct triples
	private final int maxNrOfPositiveExamples = 20;
	private final int minNrOfPositiveExamples = 3;
	private double minAccuracy = 0.4;

	private SparqlEndpointKS ks;
	private SPARQLReasoner reasoner;
	private Set<NamedClass> classes;
	private OWLOntologyManager man = OWLManager.createOWLOntologyManager();
	private OWLObjectRenderer renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
	private NumberFormat df = NumberFormat.getPercentInstance();
	

	public WoltersKluwerDataEquivalentClasses() {
		ks = new SparqlEndpointKS(wkEndpoint3);
		reasoner = new SPARQLReasoner(ks);
		classes = reasoner.getTypes();
//		classes = Sets.newHashSet(new NamedClass("http://schema.wolterskluwer.de/SubjectEditorial"));
		renderer.setShortFormProvider(new PrefixedShortFromProvider());
		
		ToStringRenderer.getInstance().setRenderer(renderer);
		df.setMinimumFractionDigits(1);
	}

	public void learnWithLGG() throws LearningProblemUnsupportedException, OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {
		ToStringRenderer.getInstance().setRenderer(new ManchesterOWLSyntaxOWLObjectRendererImpl());
		OWLDataFactory df = new OWLDataFactoryImpl();
		OWLOntology schema = man.createOntology(IRI.create("http://schema.wolterskluwer.de/constraints/"));
		for (NamedClass cls : classes) {
			if (!cls.getName().startsWith(wkNS))
				continue;
			try {
				// logger.info("Learning description for class " + cls + "...");
				SortedSet<Individual> posExamples = reasoner.getIndividuals(cls, maxNrOfPositiveExamples);
				if(posExamples.size() < minNrOfPositiveExamples){
					logger.warn(cls + "\t contains not at least " + minNrOfPositiveExamples + " asserted instances.");
				} else {
					logger.info(cls + " is " + "(based on " + posExamples.size() + " examples)" + " equivalent to ");
					PosOnlyLP lp = new PosOnlyLP();
					lp.setPositiveExamples(posExamples);

					QTL qtl = new QTL(lp, ks, wkCache);

					qtl.setMaxQueryTreeDepth(maxQueryTreeDepth);
					qtl.init();
					qtl.start();
					String query = qtl.getSPARQLQuery();
					QueryTree<String> lgg = qtl.getLgg();
					OWLClass owlClass = df.getOWLClass(IRI.create(cls.getName()));
					OWLClassExpression classExpression = lgg.asOWLClassExpression();
					// remove the class to learn form the class expressions, as
					// it is trivially contained in
					if (classExpression.isAnonymous()) {
						Set<OWLClassExpression> operands = ((OWLObjectIntersectionOf) classExpression).getOperands();
						operands.remove(owlClass);
						classExpression = df.getOWLObjectIntersectionOf(operands);
					}

					logger.info(classExpression);
					logger.info("##############################################################");
					OWLAxiom axiom = df.getOWLSubClassOfAxiom(owlClass, classExpression);
					// System.out.println(axiom);
					man.addAxiom(schema, axiom);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		man.saveOntology(schema, new TurtleOntologyFormat(), new FileOutputStream("wkd-schema-constraints.ttl"));
	}
	
	public void learnWithNoiseRobustLGG() throws LearningProblemUnsupportedException, OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {
		OWLDataFactory df = new OWLDataFactoryImpl();
		Monitor mon = MonitorFactory.getTimeMonitor("LGG");
		Map<OWLClass, Map<OWLClassExpression, Double>> result = new HashMap<OWLClass, Map<OWLClassExpression,Double>>();
		for (NamedClass cls : classes) {
			if (!cls.getName().startsWith(wkNS)) continue;
//			if (!cls.getName().startsWith("http://dbpedia.org/ontology/")) continue;
//			if(!cls.getName().equals("http://schema.wolterskluwer.de/SubjectEditorial")) continue;
			try {
				OWLClass owlClass = df.getOWLClass(IRI.create(cls.getName()));
				// logger.info("Learning description for class " + cls + "...");
				SortedSet<Individual> posExamples = reasoner.getIndividuals(cls, maxNrOfPositiveExamples);
				if(posExamples.size() < minNrOfPositiveExamples){
					logger.warn(cls + "\t contains not at least " + minNrOfPositiveExamples + " asserted instances.");
				}  else {
					Map<OWLClassExpression, Double> solutionsWithScore = new LinkedHashMap<OWLClassExpression, Double>();
					logger.info(cls + " is " + "(based on " + posExamples.size() + " examples)" + " described by ");
//					NoiseSensitiveLGGMultithreaded<String> lggGenerator = new NoiseSensitiveLGGMultithreaded<String>();
					NoiseSensitiveLGG<String> lggGenerator = new NoiseSensitiveLGG<String>();
					List<QueryTree<String>> posExampleTrees = getQueryTrees(
							new ArrayList<String>(Datastructures.individualSetToStringSet(posExamples)));
					
					mon.start();
					List<EvaluatedQueryTree<String>> solutions = lggGenerator.computeLGG(posExampleTrees);
					mon.stop();
					int i = 1;
					for (EvaluatedQueryTree<String> evaluatedQueryTree : solutions) {
						if(evaluatedQueryTree.getScore() >= minAccuracy){
							logger.info("Solution " + i++);
							QueryTree<String> lgg = evaluatedQueryTree.getTree();
							OWLClassExpression classExpression = lgg.asOWLClassExpression();
							// remove the class to learn from the class expressions, as
							// it is trivially contained in
							if (classExpression.isAnonymous()) {
								Set<OWLClassExpression> operands = ((OWLObjectIntersectionOf) classExpression).getOperands();
								operands.remove(owlClass);
								classExpression = df.getOWLObjectIntersectionOf(operands);
							}
							logger.info(classExpression + "\n(" + evaluatedQueryTree.getScore() + ")");
							solutionsWithScore.put(classExpression, evaluatedQueryTree.getScore());
						}
					}
					result.put(owlClass, solutionsWithScore);
					logger.info("##############################################################");
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		toHTML(result);
		logger.info("t(LGG)_min=" + mon.getMin());
		logger.info("t(LGG)_max=" + mon.getMax());
		logger.info("t(LGG)_total=" + mon.getTotal());
	}
	
	private void toHTML(Map<OWLClass, Map<OWLClassExpression, Double>> result){
		File dir = new File("wkd-suggestions");
		dir.mkdir();
		StringBuilder sb = new StringBuilder();
		sb.append("<table>\n");
		for (Entry<OWLClass, Map<OWLClassExpression, Double>> entry : result
				.entrySet()) {
			OWLClass cls = entry.getKey();
			Map<OWLClassExpression, Double> value = entry.getValue();
			toHTML(cls, value);
			sb.append("<tr><td>").append("<a href=\"" + filename(cls) + "\">" + renderer.render(cls) + "</a>").append("</td></tr>");
		}
		sb.append("</table>");
		try {
			Files.write(sb.toString(), new File(dir, "wkd-suggestions.html"), Charsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void toHTML(OWLClass cls, Map<OWLClassExpression, Double> result){
		File dir = new File("wkd-suggestions");
		dir.mkdir();
		StringBuilder sb = new StringBuilder();
		sb.append(cls).append(" is described by\n");
		sb.append("<table border=\"1\">\n");
		sb.append("<tr><th>Suggestion</td><td>Score</td></tr>\n");
		for (Entry<OWLClassExpression, Double> entry : result
				.entrySet()) {
			OWLClassExpression classExpression = entry.getKey();
			Double value = entry.getValue();
			sb.append("<tr>").append("<td>").append("<pre>" + classExpression + "</pre>").append("</td>").append("<td>").append(df.format(value)).append("</td>").append("</tr>\n");
		}
		sb.append("</table>");
		try {
			Files.write(sb.toString(), new File(dir, filename(cls)), Charsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void learnWithCELOE() throws ComponentInitException {
		for (NamedClass cls : classes) {
			System.out.print("finding positives ... ");
			long startTime = System.currentTimeMillis();
			SortedSet<Individual> posExamples = reasoner.getIndividuals(cls, maxNrOfPositiveExamples);
			long runTime = System.currentTimeMillis() - startTime;
			if (posExamples.isEmpty()) {
				System.out.println("Skipping CELOE because class " + cls.toString() + " is empty.");
				continue;
			}
			SortedSet<String> posExStr = Helper.getStringSet(posExamples);
			System.out.println("done (" + posExStr.size() + " examples found in " + runTime + " ms)");

			AutomaticNegativeExampleFinderSPARQL2 finder = new AutomaticNegativeExampleFinderSPARQL2(ks.getEndpoint());
			SortedSet<String> negExStr = finder.getNegativeExamples(cls.getName(), posExStr);
			negExStr = SetManipulation.stableShrink(negExStr, 20);
			SortedSet<Individual> negExamples = Helper.getIndividualSet(negExStr);
			SortedSetTuple<Individual> examples = new SortedSetTuple<Individual>(posExamples, negExamples);
			
			
			AbstractReasonerComponent rc;
			KnowledgeSource ksFragment;
			System.out.print("extracting fragment ... ");
			startTime = System.currentTimeMillis();
			ConciseBoundedDescriptionGenerator cbdGen = new ConciseBoundedDescriptionGeneratorImpl(ks.getEndpoint(),
					wkCache, maxQueryTreeDepth);
			Model model = ModelFactory.createDefaultModel();
			for (Individual example : examples.getCompleteSet()) {
				Model cbd = cbdGen.getConciseBoundedDescription(example.getName());
				model.add(cbd);
			}
			filter(model);
			OWLEntityTypeAdder.addEntityTypes(model);
			runTime = System.currentTimeMillis() - startTime;
			System.out.println("done (" + model.size() + " triples found in " + runTime + " ms)");
			OWLOntology ontology = asOWLOntology(model);
			ksFragment = new OWLAPIOntology(ontology);
			// ksFragment.init();
			rc = new FastInstanceChecker(ksFragment);
			rc.init();

			ClassLearningProblem lp = new ClassLearningProblem(rc);
			lp.setClassToDescribe(cls);
			lp.setEquivalence(true);
			lp.setHeuristic(HeuristicType.FMEASURE);
			lp.setUseApproximations(false);
			lp.setMaxExecutionTimeInSeconds(10);
			lp.init();

			CELOE la = new CELOE(lp, rc);
			la.setMaxExecutionTimeInSeconds(10);
			la.setNoisePercentage(25);
			la.init();
			startTime = System.currentTimeMillis();
			System.out.print("running CELOE (for equivalent classes... ");
			la.start();
			runTime = System.currentTimeMillis() - startTime;
			System.out.println("done in " + runTime + " ms");

			// convert the result to axioms (to make it compatible with the
			// other algorithms)
			List<? extends EvaluatedDescription> learnedDescriptions = la.getCurrentlyBestEvaluatedDescriptions(0.7);
			List<EvaluatedAxiom> learnedAxioms = new LinkedList<EvaluatedAxiom>();
			for (EvaluatedDescription learnedDescription : learnedDescriptions) {
				Axiom axiom = new EquivalentClassesAxiom(cls, learnedDescription.getDescription());
				Score score = lp.computeScore(learnedDescription.getDescription());
				learnedAxioms.add(new EvaluatedAxiom(axiom, score));
			}
			System.out.println(EvaluatedAxiom.prettyPrint(learnedAxioms));
		}

	}

	private void filter(Model model) {
		// filter out triples with String literals, as there often occur
		// some syntax errors and they are not relevant for learning
		List<Statement> statementsToRemove = new ArrayList<Statement>();
		List<Statement> statementsToAdd = new ArrayList<Statement>();
		for (Iterator<Statement> iter = model.listStatements().toList().iterator(); iter.hasNext();) {
			Statement st = iter.next();
			RDFNode subject = st.getSubject();
			RDFNode object = st.getObject();

			if (object.isAnon()) {
				if (!model.listStatements(object.asResource(), null, (RDFNode) null).hasNext()) {
					statementsToRemove.add(st);
				}
			} else if (st.getPredicate().equals(RDF.type)
					&& (object.equals(RDFS.Class.asNode()) || object.equals(OWL.Class.asNode()) || object
							.equals(RDFS.Literal.asNode()))) {
				// remove statements like <x a owl:Class>
				statementsToRemove.add(st);
			} else if (st.getPredicate().getURI().equals("http://www.metalex.eu/metalex/2008-05-02#fragment")) {//remove wkd:
				statementsToRemove.add(st);
			} else {
				// fix URIs with spaces
				Resource newSubject = (Resource) subject;
				RDFNode newObject = object;
				if (subject.isURIResource()) {
					String uri = subject.asResource().getURI();
					if (uri.contains(" ")) {
						newSubject = model.createResource(uri.replace(" ", ""));
					}
				}
				if (object.isURIResource()) {
					String uri = object.asResource().getURI();
					if (uri.contains(" ")) {
						newObject = model.createResource(uri.replace(" ", ""));
					}
				}
//				if (object.isLiteral()) {
//					Literal lit = object.asLiteral();
//					if (lit.getDatatype() == null || lit.getDatatype().equals(XSD.STRING)) {
//						newObject = model.createLiteral("shortened", "en");
//					}
//				}
				statementsToAdd.add(model.createStatement(newSubject, st.getPredicate(), newObject));
				statementsToRemove.add(st);
			}

		}
		model.remove(statementsToRemove);
		model.add(statementsToAdd);
	}

	private OWLOntology asOWLOntology(Model model) {
		try {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream("bug.ttl");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			model.write(baos, "TURTLE", null);
			model.write(fos, "TURTLE", null);
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = man.loadOntologyFromOntologyDocument(new ByteArrayInputStream(baos.toByteArray()));
			return ontology;
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private List<QueryTree<String>> getQueryTrees(List<String> resources){
		List<QueryTree<String>> trees = new ArrayList<QueryTree<String>>();
		Model model;
		QueryTree<String> tree;
		ConciseBoundedDescriptionGenerator cbdGenerator = new ConciseBoundedDescriptionGeneratorImpl(wkEndpoint3, wkCache);
		cbdGenerator.setRecursionDepth(maxQueryTreeDepth);
		QueryTreeCache treeCache = new QueryTreeCache();
		for(String resource : resources){
			try {
				logger.debug("Generating tree for " + resource);
				model = cbdGenerator.getConciseBoundedDescription(resource);
				filter(model);
				tree = treeCache.getQueryTree(resource, model);
				if(logger.isDebugEnabled()){
					logger.debug("Tree for resource " + resource);
					logger.debug(tree.getStringRepresentation());
					
				}
				trees.add(tree);
			} catch (Exception e) {
				logger.error("Failed to create tree for resource " + resource + ".", e);
			}
		}
		return trees;
	}
	
	private String filename(OWLClass cls){
		String filename = renderer.render(cls);
		filename = encode(filename) + ".html";
		return filename;
	}
	
	private String encode(String s){
		s = s.replace("/", "+");
		s = s.replace(":", "+");
		s = s.replace(" ", "+");
		return s;
	}

	public static void main(String[] args) throws Exception {
		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("wkd", "wkdlod2".toCharArray());
			}
		});

		WoltersKluwerDataEquivalentClasses experiment = new WoltersKluwerDataEquivalentClasses();
//		experiment.learnWithLGG();
//		 experiment.learnWithCELOE();
		experiment.learnWithNoiseRobustLGG();
	}
}
