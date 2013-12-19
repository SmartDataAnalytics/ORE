package org.aksw.ore.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.aksw.jena_sparql_api.cache.extra.CacheEx;
import org.aksw.mole.ore.rendering.KeywordColorMap;
import org.aksw.ore.exception.OREException;
import org.aksw.ore.model.ResourceType;
import org.aksw.ore.util.UnsortedManchesterSyntaxRendererImpl;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.apache.jena.riot.checker.CheckerLiterals;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.dllearner.algorithms.DisjointClassesLearner;
import org.dllearner.algorithms.SimpleSubclassLearner;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.algorithms.properties.AsymmetricObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.DataPropertyDomainAxiomLearner;
import org.dllearner.algorithms.properties.DataPropertyRangeAxiomLearner;
import org.dllearner.algorithms.properties.DisjointDataPropertyAxiomLearner;
import org.dllearner.algorithms.properties.DisjointObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.EquivalentDataPropertyAxiomLearner;
import org.dllearner.algorithms.properties.EquivalentObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.FunctionalDataPropertyAxiomLearner;
import org.dllearner.algorithms.properties.FunctionalObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.InverseFunctionalObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.IrreflexiveObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.ObjectPropertyDomainAxiomLearner2;
import org.dllearner.algorithms.properties.ObjectPropertyRangeAxiomLearner;
import org.dllearner.algorithms.properties.ReflexiveObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.SubDataPropertyOfAxiomLearner;
import org.dllearner.algorithms.properties.SubObjectPropertyOfAxiomLearner;
import org.dllearner.algorithms.properties.SymmetricObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.TransitiveObjectPropertyAxiomLearner;
import org.dllearner.core.AbstractAxiomLearningAlgorithm;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.AnnComponentManager;
import org.dllearner.core.AxiomLearningAlgorithm;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.core.LearningAlgorithm;
import org.dllearner.core.Score;
import org.dllearner.core.config.ConfigHelper;
import org.dllearner.core.owl.Axiom;
import org.dllearner.core.owl.DatatypeProperty;
import org.dllearner.core.owl.Entity;
import org.dllearner.core.owl.EquivalentClassesAxiom;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.KBElement;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.core.owl.SubClassAxiom;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.SPARQLTasks;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.learningproblems.ClassLearningProblem;
import org.dllearner.learningproblems.Heuristics.HeuristicType;
import org.dllearner.reasoning.FastInstanceChecker;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.utilities.Helper;
import org.dllearner.utilities.PrefixCCMap;
import org.dllearner.utilities.datastructures.SortedSetTuple;
import org.dllearner.utilities.examples.AutomaticNegativeExampleFinderSPARQL2;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.dllearner.utilities.owl.OWLEntityTypeAdder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.clarkparsia.owlapiv3.XSD;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class EnrichmentManager {
	
	public interface EnrichmentProgressListener extends EventListener {
		public void onEnrichmentStarted(AxiomType<OWLAxiom> arg);
		public void onEnrichmentFinished(AxiomType<OWLAxiom> arg);
		public void onEnrichmentFailed(AxiomType<OWLAxiom> arg);
	}

	private final List<EnrichmentProgressListener> enrichmentProgressListeners = new LinkedList<EnrichmentProgressListener>();

	public final void addEnrichmentProgressListener(EnrichmentProgressListener listener) {
		synchronized (enrichmentProgressListeners) {
			enrichmentProgressListeners.add(listener);
		}
	}

	public final void removeEnrichmentProgressListener(EnrichmentProgressListener listener) {
		synchronized (enrichmentProgressListeners) {
			enrichmentProgressListeners.remove(listener);
		}
	}

	private void fireEnrichmentStarted(AxiomType<OWLAxiom> arg) {
		synchronized (enrichmentProgressListeners) {
			for (EnrichmentProgressListener listener : enrichmentProgressListeners)
				listener.onEnrichmentStarted(arg);
		}
	}

	private void fireEnrichmentFinished(AxiomType<OWLAxiom> arg) {
		synchronized (enrichmentProgressListeners) {
			for (EnrichmentProgressListener listener : enrichmentProgressListeners)
				listener.onEnrichmentFinished(arg);
		}
	}
	
	private void fireEnrichmentFailed(AxiomType<OWLAxiom> arg) {
		synchronized (enrichmentProgressListeners) {
			for (EnrichmentProgressListener listener : enrichmentProgressListeners)
				listener.onEnrichmentFailed(arg);
		}
	}


	
	enum QueryMode {
		ITERATIVE,
		SINGLE
	}
	
	private List<Class<? extends LearningAlgorithm>> objectPropertyAlgorithms;
	private List<Class<? extends LearningAlgorithm>> dataPropertyAlgorithms;
	private List<Class<? extends LearningAlgorithm>> classAlgorithms;
	
	private BidiMap<AxiomType, Class<? extends LearningAlgorithm>> axiomType2Class;
	
	private UnsortedManchesterSyntaxRendererImpl manchesterSyntaxRenderer = new UnsortedManchesterSyntaxRendererImpl();// ManchesterOWLSyntaxOWLObjectRendererImpl();
	private KeywordColorMap colorMap = new KeywordColorMap();
	
	private DecimalFormat df = new DecimalFormat("##0.0");
	
	private SparqlEndpoint endpoint;
	private SPARQLReasoner reasoner;
	private ResourceType resourceType;
	
	private int maxExecutionTimeInSeconds = 10;
	private double threshold = 0.75;
	private int maxNrOfReturnedAxioms = 5;
	private boolean useInference;
	private QueryMode queryMode = QueryMode.ITERATIVE;
	
	private List<AxiomType> axiomTypes;
	
	private List<EvaluatedAxiom> learnedAxioms;
	
	private Entity currentEntity;
	private LearningAlgorithm currentAlgorithm;
	
	private boolean isRunning = false;
	
	private Map<AxiomType, AbstractAxiomLearningAlgorithm> learningAlgorithmInstances = new HashMap<AxiomType, AbstractAxiomLearningAlgorithm>();
	private int maxNrOfPositiveExamples = 10;
	private int maxNrOfNegativeExamples = 20;
	
	public EnrichmentManager(SparqlEndpoint endpoint, CacheEx cache) {
		this.endpoint = endpoint;
		
		reasoner = new SPARQLReasoner(new SparqlEndpointKS(endpoint), cache);
		
		objectPropertyAlgorithms = new LinkedList<Class<? extends LearningAlgorithm>>();
		objectPropertyAlgorithms.add(DisjointObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(EquivalentObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(SubObjectPropertyOfAxiomLearner.class);
		objectPropertyAlgorithms.add(FunctionalObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(InverseFunctionalObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(ObjectPropertyDomainAxiomLearner2.class);
		objectPropertyAlgorithms.add(ObjectPropertyRangeAxiomLearner.class);
		objectPropertyAlgorithms.add(SymmetricObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(AsymmetricObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(ReflexiveObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(IrreflexiveObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(TransitiveObjectPropertyAxiomLearner.class);

		dataPropertyAlgorithms = new LinkedList<Class<? extends LearningAlgorithm>>();
		dataPropertyAlgorithms.add(DisjointDataPropertyAxiomLearner.class);
		dataPropertyAlgorithms.add(EquivalentDataPropertyAxiomLearner.class);
		dataPropertyAlgorithms.add(FunctionalDataPropertyAxiomLearner.class);
		dataPropertyAlgorithms.add(DataPropertyDomainAxiomLearner.class);
		dataPropertyAlgorithms.add(DataPropertyRangeAxiomLearner.class); 
		dataPropertyAlgorithms.add(SubDataPropertyOfAxiomLearner.class);
		
		classAlgorithms = new LinkedList<Class<? extends LearningAlgorithm>>();
		classAlgorithms.add(DisjointClassesLearner.class);
		classAlgorithms.add(SimpleSubclassLearner.class);
		classAlgorithms.add(CELOE.class);
		
		axiomType2Class = new DualHashBidiMap<AxiomType, Class<? extends LearningAlgorithm>>();
		axiomType2Class.put(AxiomType.SUBCLASS_OF, SimpleSubclassLearner.class);
		axiomType2Class.put(AxiomType.EQUIVALENT_CLASSES, CELOE.class);
		axiomType2Class.put(AxiomType.DISJOINT_CLASSES, DisjointClassesLearner.class);
		axiomType2Class.put(AxiomType.SUB_OBJECT_PROPERTY, SubObjectPropertyOfAxiomLearner.class);
		axiomType2Class.put(AxiomType.EQUIVALENT_OBJECT_PROPERTIES, EquivalentObjectPropertyAxiomLearner.class);
		axiomType2Class.put(AxiomType.DISJOINT_OBJECT_PROPERTIES, DisjointObjectPropertyAxiomLearner.class);
		axiomType2Class.put(AxiomType.OBJECT_PROPERTY_DOMAIN, ObjectPropertyDomainAxiomLearner2.class);
		axiomType2Class.put(AxiomType.OBJECT_PROPERTY_RANGE, ObjectPropertyRangeAxiomLearner.class);
		axiomType2Class.put(AxiomType.FUNCTIONAL_OBJECT_PROPERTY, FunctionalObjectPropertyAxiomLearner.class);
		axiomType2Class.put(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY, InverseFunctionalObjectPropertyAxiomLearner.class);
		axiomType2Class.put(AxiomType.REFLEXIVE_OBJECT_PROPERTY, ReflexiveObjectPropertyAxiomLearner.class);
		axiomType2Class.put(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY, IrreflexiveObjectPropertyAxiomLearner.class);
		axiomType2Class.put(AxiomType.SYMMETRIC_OBJECT_PROPERTY, SymmetricObjectPropertyAxiomLearner.class);
		axiomType2Class.put(AxiomType.ASYMMETRIC_OBJECT_PROPERTY, AsymmetricObjectPropertyAxiomLearner.class);
		axiomType2Class.put(AxiomType.TRANSITIVE_OBJECT_PROPERTY, TransitiveObjectPropertyAxiomLearner.class);
		axiomType2Class.put(AxiomType.SUB_DATA_PROPERTY, SubDataPropertyOfAxiomLearner.class);
		axiomType2Class.put(AxiomType.EQUIVALENT_DATA_PROPERTIES, EquivalentDataPropertyAxiomLearner.class);
		axiomType2Class.put(AxiomType.DISJOINT_DATA_PROPERTIES, DisjointDataPropertyAxiomLearner.class);
		axiomType2Class.put(AxiomType.DATA_PROPERTY_DOMAIN, DataPropertyDomainAxiomLearner.class);
		axiomType2Class.put(AxiomType.DATA_PROPERTY_RANGE, DataPropertyRangeAxiomLearner.class);
		axiomType2Class.put(AxiomType.FUNCTIONAL_DATA_PROPERTY, FunctionalDataPropertyAxiomLearner.class);
		
		loadProperties();
	}
	
	private void loadProperties(){
		try {
			InputStream is = getClass().getResourceAsStream("/enrichment.properties" );
			Properties properties = new Properties();
			properties.load(is);
			queryMode = QueryMode.valueOf(properties.getProperty("query.mode").toUpperCase());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public SparqlEndpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(SparqlEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	public ResourceType getResourceType() {
		return resourceType;
	}

	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}

	public int getMaxExecutionTimeInSeconds() {
		return maxExecutionTimeInSeconds;
	}

	public void setMaxExecutionTimeInSeconds(int maxExecutionTimeInSeconds) {
		this.maxExecutionTimeInSeconds = maxExecutionTimeInSeconds;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public int getMaxNrOfReturnedAxioms() {
		return maxNrOfReturnedAxioms;
	}

	public void setMaxNrOfReturnedAxioms(int maxNrOfReturnedAxioms) {
		this.maxNrOfReturnedAxioms = maxNrOfReturnedAxioms;
	}

	public boolean isUseInference() {
		return useInference;
	}

	public void setUseInference(boolean useInference) {
		this.useInference = useInference;
	}

	public List<AxiomType> getAxiomTypes() {
		return axiomTypes;
	}

	public void setAxiomTypes(List<AxiomType> axiomTypes) {
		this.axiomTypes = axiomTypes;
	}
	
	public List<EvaluatedAxiom> getCurrentlyEvaluatedAxioms(){
		return new ArrayList<EvaluatedAxiom>(learnedAxioms);
	}
	
	public Collection<AxiomType<OWLAxiom>> getAxiomTypes(ResourceType type){
		List<AxiomType<OWLAxiom>> types = new ArrayList<AxiomType<OWLAxiom>>();
		
		List<Class<? extends LearningAlgorithm>> algorithms;
		if(type == ResourceType.CLASS){
			algorithms = classAlgorithms;
		} else if(type == ResourceType.OBJECT_PROPERTY){
			algorithms = objectPropertyAlgorithms;
		} else if(type == ResourceType.DATA_PROPERTY){
			algorithms = dataPropertyAlgorithms;
		} else {
			algorithms = new ArrayList<Class<? extends LearningAlgorithm>>();
			algorithms.addAll(classAlgorithms);
			algorithms.addAll(objectPropertyAlgorithms);
			algorithms.addAll(dataPropertyAlgorithms);
		}
		
		for(Class<? extends LearningAlgorithm> alg : algorithms){
			types.add(axiomType2Class.getKey(alg));
		}
		
		return types;
	}
	
	
	
	public ResourceType getResourceType(String resourceURI) throws OREException{
		SPARQLTasks st = new SPARQLTasks(endpoint);
		currentEntity = st.guessResourceType(resourceURI, true);
		if(currentEntity != null){
			ResourceType resourceType = null;
			if(currentEntity instanceof ObjectProperty) {
				resourceType = ResourceType.OBJECT_PROPERTY;
			} else if(currentEntity instanceof DatatypeProperty) {
				resourceType = ResourceType.DATA_PROPERTY;
			} else if(currentEntity instanceof NamedClass) {
				resourceType = ResourceType.CLASS;	
			} 
			return resourceType;
		} else {
			throw new OREException("Could not detect type of resource");
		}
	}
	
	public List<EvaluatedAxiom> getEvaluatedAxioms2(String resourceURI, ResourceType resourceType,
			AxiomType<OWLAxiom> axiomType, int maxExecutionTimeInSeconds, double threshold, boolean useInference)
			throws OREException {
		fireEnrichmentStarted(axiomType);
		List<EvaluatedAxiom> learnedAxioms = new ArrayList<EvaluatedAxiom>();

		try {
			// common helper objects
			SPARQLTasks st = new SPARQLTasks(endpoint);

			Entity currentEntity = getEntity(resourceURI, resourceType);
			if (currentEntity == null) {
				currentEntity = st.guessResourceType(resourceURI, true);
			}

			SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
			ks.init();

			// check if endpoint supports SPARQL 1.1
			boolean supportsSPARQL_1_1 = st.supportsSPARQL_1_1();
			ks.setSupportsSPARQL_1_1(supportsSPARQL_1_1);

			if (useInference && !reasoner.isPrepared()) {
				System.out.print("Precomputing subsumption hierarchy ... ");
				long startTime = System.currentTimeMillis();
				reasoner.prepareSubsumptionHierarchy();
				System.out.println("done in " + (System.currentTimeMillis() - startTime) + " ms");
			}
			learnedAxioms = applyLearningAlgorithm2(axiomType, ks, currentEntity);
			fireEnrichmentFinished(axiomType);
		} catch (ComponentInitException e) {
			e.printStackTrace();
			fireEnrichmentFailed(axiomType);
			throw new OREException();
		}
		return learnedAxioms;
	}
	
	public List<EvaluatedAxiom> getEvaluatedAxioms2(String resourceURI, AxiomType<OWLAxiom> axiomType)
			throws OREException {
		fireEnrichmentStarted(axiomType);
		List<EvaluatedAxiom> learnedAxioms = new ArrayList<EvaluatedAxiom>();

		try {
			// common helper objects
			SPARQLTasks st = new SPARQLTasks(endpoint);

			Entity currentEntity = getEntity(resourceURI, resourceType);
			if (currentEntity == null) {
				currentEntity = st.guessResourceType(resourceURI, true);
			}

			SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
			ks.init();

			// check if endpoint supports SPARQL 1.1
			boolean supportsSPARQL_1_1 = st.supportsSPARQL_1_1();
			ks.setSupportsSPARQL_1_1(supportsSPARQL_1_1);

			if (useInference && !reasoner.isPrepared()) {
				System.out.print("Precomputing subsumption hierarchy ... ");
				long startTime = System.currentTimeMillis();
				reasoner.prepareSubsumptionHierarchy();
				System.out.println("done in " + (System.currentTimeMillis() - startTime) + " ms");
			}
			learnedAxioms = applyLearningAlgorithm2(axiomType, ks, currentEntity);
			fireEnrichmentFinished(axiomType);
		} catch (ComponentInitException e) {
			e.printStackTrace();
			fireEnrichmentFailed(axiomType);
			throw new OREException();
		}
		return learnedAxioms;
	}
	
	
	@SuppressWarnings("unchecked")
	private void runClassLearningAlgorithms(SparqlEndpointKS ks, NamedClass nc) throws ComponentInitException {
//		System.out.println("Running algorithms for class " + nc);
		for (Class<? extends LearningAlgorithm> algorithmClass : classAlgorithms) {
			if(algorithmClass == CELOE.class) {
			} else {
				applyLearningAlgorithm((Class<AxiomLearningAlgorithm>)algorithmClass, ks, nc);
			}
		}
	}
	
	private void runObjectPropertyAlgorithms(SparqlEndpointKS ks, ObjectProperty property) throws ComponentInitException {
//		System.out.println("Running algorithms for object property " + property);
		for (Class<? extends LearningAlgorithm> algorithmClass : objectPropertyAlgorithms) {
			applyLearningAlgorithm(algorithmClass, ks, property);
		}		
	}
	
	private void runDataPropertyAlgorithms(SparqlEndpointKS ks, DatatypeProperty property) throws ComponentInitException {
//		System.out.println("Running algorithms for data property " + property);
		for (Class<? extends LearningAlgorithm> algorithmClass : dataPropertyAlgorithms) {
			applyLearningAlgorithm(algorithmClass, ks, property);
		}		
	}	
	
	private List<EvaluatedAxiom> applyLearningAlgorithm(Class<? extends LearningAlgorithm> algorithmClass, SparqlEndpointKS ks, Entity entity) throws ComponentInitException {
		if(axiomTypes != null && !axiomTypes.contains(axiomType2Class.getKey(algorithmClass))){
			return Collections.<EvaluatedAxiom>emptyList();
		}
		
		AxiomLearningAlgorithm learner = null;
		try {
			learner = (AxiomLearningAlgorithm)algorithmClass.getConstructor(
					SparqlEndpointKS.class).newInstance(ks);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(classAlgorithms.contains(algorithmClass)) {
			ConfigHelper.configure(learner, "classToDescribe", entity);
		} else {
			ConfigHelper.configure(learner, "propertyToDescribe", entity);
		}
		ConfigHelper.configure(learner, "maxExecutionTimeInSeconds",
				maxExecutionTimeInSeconds);
//		if(reasoner != null){
			((AbstractAxiomLearningAlgorithm)learner).setReasoner(reasoner);
//		}
			((AbstractAxiomLearningAlgorithm)learner).setForceSPARQL_1_0_Mode(queryMode == QueryMode.ITERATIVE);
		learner.init();
		String algName = AnnComponentManager.getName(learner);
		System.out.print("Applying " + algName + " on " + entity + " ... ");
		long startTime = System.currentTimeMillis();
		try {
			learner.start();
		} catch (Exception e) {
			if(e.getCause() instanceof SocketTimeoutException){
				System.out.println("Query timed out (endpoint possibly too slow).");
			} else {
				e.printStackTrace();
			}
		}
		long runtime = System.currentTimeMillis() - startTime;
		System.out.println("done in " + runtime + " ms");
		List<EvaluatedAxiom> learnedAxioms = learner
				.getCurrentlyBestEvaluatedAxioms(maxNrOfReturnedAxioms, threshold);
		System.out.println(prettyPrint(learnedAxioms));
		this.learnedAxioms.addAll(learnedAxioms);
		return learnedAxioms;
	}
	
	private List<EvaluatedAxiom> applyLearningAlgorithm2(AxiomType axiomType, SparqlEndpointKS ks, Entity entity) throws ComponentInitException {
		if(axiomType == AxiomType.EQUIVALENT_CLASSES){
			return applyCELOE(ks, (NamedClass)entity, true);
		} else {
			Class<? extends LearningAlgorithm> algorithmClass = axiomType2Class.get(axiomType);
			AxiomLearningAlgorithm learner = null;
			try {
				learner = (AxiomLearningAlgorithm)algorithmClass.getConstructor(
						SparqlEndpointKS.class).newInstance(ks);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(classAlgorithms.contains(algorithmClass)) {
				ConfigHelper.configure(learner, "classToDescribe", entity);
			} else {
				ConfigHelper.configure(learner, "propertyToDescribe", entity);
			}
			ConfigHelper.configure(learner, "maxExecutionTimeInSeconds",
					maxExecutionTimeInSeconds);
//			if(reasoner != null){
				((AbstractAxiomLearningAlgorithm)learner).setReasoner(reasoner);
//			}
				((AbstractAxiomLearningAlgorithm)learner).setForceSPARQL_1_0_Mode(queryMode == QueryMode.ITERATIVE);
			learner.init();
			String algName = AnnComponentManager.getName(learner);
			System.out.print("Applying " + algName + " on " + entity + " ... ");
			long startTime = System.currentTimeMillis();
			try {
				learner.start();
			} catch (Exception e) {
				if(e.getCause() instanceof SocketTimeoutException){
					System.out.println("Query timed out (endpoint possibly too slow).");
				} else {
					e.printStackTrace();
				}
			}
			long runtime = System.currentTimeMillis() - startTime;
			System.out.println("done in " + runtime + " ms");
			List<EvaluatedAxiom> learnedAxioms = learner
					.getCurrentlyBestEvaluatedAxioms(maxNrOfReturnedAxioms, threshold);
			System.out.println(prettyPrint(learnedAxioms));
			learningAlgorithmInstances.put(axiomType, (AbstractAxiomLearningAlgorithm)learner);
			return learnedAxioms;
		}
	}
	
	private List<EvaluatedAxiom> applyCELOE(SparqlEndpointKS ks, NamedClass nc, boolean equivalence) throws ComponentInitException {
		// get instances of class as positive examples
		System.out.print("finding positives ... ");
		long startTime = System.currentTimeMillis();
		SortedSet<Individual> posExamples = reasoner.getIndividuals(nc, maxNrOfPositiveExamples);
		long runTime = System.currentTimeMillis() - startTime;
		if(posExamples.isEmpty()){
			System.out.println("Skipping CELOE because class " + nc.toString() + " is empty.");
			return Collections.emptyList();
		}
		SortedSet<String> posExStr = Helper.getStringSet(posExamples);
		System.out.println("done (" + posExStr.size()+ " examples found in " + runTime + " ms)");
		
		// use own implementation of negative example finder
		System.out.print("finding negatives ... ");
		startTime = System.currentTimeMillis();
		AutomaticNegativeExampleFinderSPARQL2 finder = new AutomaticNegativeExampleFinderSPARQL2(reasoner, "http://dbpedia.org/ontology");
		SortedSet<Individual> negExamples = finder.getNegativeExamples(nc, posExamples, maxNrOfNegativeExamples);
		SortedSetTuple<Individual> examples = new SortedSetTuple<Individual>(posExamples, negExamples);
		runTime = System.currentTimeMillis() - startTime;
		System.out.println("done (" + negExamples.size()+ " examples found in " + runTime + " ms)");
		
		AbstractReasonerComponent rc;
		KnowledgeSource ksFragment;
		System.out.print("extracting fragment ... ");//com.hp.hpl.jena.shared.impl.JenaParameters.enableEagerLiteralValidation = true;
		startTime = System.currentTimeMillis();
		Model model = getFragmentMultithreaded(ks, Sets.union(posExamples, negExamples));
		filter(model);
		OWLEntityTypeAdder.addEntityTypes(model);
		
		runTime = System.currentTimeMillis() - startTime;
		System.out.println("done (" + model.size()+ " triples found in " + runTime + " ms)");
		OWLOntology ontology = asOWLOntology(model);
		if(reasoner.getClassHierarchy() != null){
			ontology.getOWLOntologyManager().addAxioms(ontology, reasoner.getClassHierarchy().toOWLAPIAxioms());
		}
		ksFragment = new OWLAPIOntology(ontology);
//		ksFragment.init();
		rc = new FastInstanceChecker(ksFragment);
		rc.init();
//		rc.setSubsumptionHierarchy(reasoner.getClassHierarchy());
		
//		for (Individual ind : posExamples) {
//			System.out.println(ResultSetFormatter.asText(com.hp.hpl.jena.query.QueryExecutionFactory.create("SELECT * WHERE {<" + ind.getName() + "> ?p ?o. OPTIONAL{?o a ?o_type}}",model).execSelect()));
//		}
		
        ClassLearningProblem lp = new ClassLearningProblem(rc);
		lp.setClassToDescribe(nc);
        lp.setEquivalence(equivalence);
        lp.setHeuristic(HeuristicType.FMEASURE);
        lp.setUseApproximations(false);
        lp.setMaxExecutionTimeInSeconds(10);
        lp.init();

        CELOE la = new CELOE(lp, rc);
        la.setMaxExecutionTimeInSeconds(10);
        la.setNoisePercentage(25);
        la.setMaxNrOfResults(100);
        la.init();
//        ((RhoDRDown)la.getOperator()).setUseNegation(false);
        startTime = System.currentTimeMillis();
        System.out.print("running CELOE (for " + (equivalence ? "equivalent classes" : "sub classes") + ") ... ");
        la.start();
        runTime = System.currentTimeMillis() - startTime;
        System.out.println("done in " + runTime + " ms");	

        // convert the result to axioms (to make it compatible with the other algorithms)
        List<? extends EvaluatedDescription> learnedDescriptions = la.getCurrentlyBestEvaluatedDescriptions(threshold);
        List<EvaluatedAxiom> learnedAxioms = new LinkedList<EvaluatedAxiom>();
        for(EvaluatedDescription learnedDescription : learnedDescriptions) {
        	Axiom axiom;
        	if(equivalence) {
        		axiom = new EquivalentClassesAxiom(nc, learnedDescription.getDescription());
        	} else {
        		axiom = new SubClassAxiom(nc, learnedDescription.getDescription());
        	}
        	Score score = lp.computeScore(learnedDescription.getDescription());
        	learnedAxioms.add(new EvaluatedAxiom(axiom, score)); 
        }
        System.out.println(prettyPrint(learnedAxioms));	
        	
		return learnedAxioms;
	}
	
	private void filter(Model model) {
		// filter out triples with String literals, as therein often occur
		// some syntax errors and they are not relevant for learning
		List<Statement> statementsToRemove = new ArrayList<Statement>();
		List<Statement> statementsToAdd = new ArrayList<Statement>();
		for (Iterator<Statement> iter = model.listStatements().toList().iterator(); iter.hasNext();) {
			Statement st = iter.next();
			RDFNode subject = st.getSubject();
			RDFNode object = st.getObject();
			
			if(object.isAnon()){
				if(!model.listStatements(object.asResource(), null, (RDFNode)null).hasNext()){
					statementsToRemove.add(st);
				}
			} else if(st.getPredicate().equals(RDF.type) && 
					(object.equals(RDFS.Class.asNode()) || object.equals(OWL.Class.asNode()) || object.equals(RDFS.Literal.asNode()))){
				//remove statements like <x a owl:Class>
					statementsToRemove.add(st);
			} else {
				// fix URIs with spaces
				Resource newSubject = (Resource) subject;
				RDFNode newObject = object;
				boolean validTriple = true;
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
				if (object.isLiteral()) {
					Literal lit = object.asLiteral();
					if (lit.getDatatype() == null || lit.getDatatype().equals(XSD.STRING)) {
						newObject = model.createLiteral("shortened", "en");
					}
					validTriple = CheckerLiterals.checkLiteral(object.asNode(), ErrorHandlerFactory.errorHandlerNoLogging, 1l, 1l);
				}
				if(validTriple){
					statementsToAdd.add(model.createStatement(newSubject, st.getPredicate(), newObject));
				}
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
			try {
				model.write(new FileOutputStream("parse-error.ttl"), "TURTLE", null);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	
	private Model getFragmentMultithreaded(final SparqlEndpointKS ks, Set<Individual> individuals){
		Model model = ModelFactory.createDefaultModel();
		ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<Model>> futures = new ArrayList<Future<Model>>();
		for (final Individual ind : individuals) {
			futures.add(threadPool.submit(new Callable<Model>() {
				@Override
				public Model call() throws Exception {
					ConciseBoundedDescriptionGenerator cbdGen = new ConciseBoundedDescriptionGeneratorImpl(ks.getEndpoint(), "enrichment-cache", 2);
					return cbdGen.getConciseBoundedDescription(ind.getName());
				}
			}));
		}
		for (Future<Model> future : futures) {
			try {
				model.add(future.get());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		threadPool.shutdown();
		return model;
	}
	
	public Set<KBElement> getPositives(AxiomType axiomType, EvaluatedAxiom axiom){
		AbstractAxiomLearningAlgorithm la = learningAlgorithmInstances.get(axiomType);
		Set<KBElement> positiveExamples = la.getPositiveExamples(axiom);
		return positiveExamples;
	}

	public Set<KBElement> getNegatives(AxiomType axiomType, EvaluatedAxiom axiom){
		AbstractAxiomLearningAlgorithm la = learningAlgorithmInstances.get(axiomType);
		Set<KBElement> negativeExamples = la.getNegativeExamples(axiom);
		return negativeExamples;
	}
	

	private Entity getEntity(String resourceURI, ResourceType resourceType){
		Entity entity = null;
		if(resourceType == ResourceType.CLASS){
			entity = new NamedClass(resourceURI);
		} else if(resourceType == ResourceType.OBJECT_PROPERTY){
			entity = new ObjectProperty(resourceURI);
		} else if(resourceType == ResourceType.DATA_PROPERTY){
			entity = new DatatypeProperty(resourceURI);
		}
		return entity;
	}
	
	private String prettyPrint(List<EvaluatedAxiom> learnedAxioms) {
		String str = "suggested axioms and their score in percent:\n";
		if(learnedAxioms.isEmpty()) {
			return "  no axiom suggested\n";
		} else {
			for (EvaluatedAxiom learnedAxiom : learnedAxioms) {
				str += " " + prettyPrint(learnedAxiom) + "\n";
			}		
		}
		return str;
	}
	
	private String prettyPrint(EvaluatedAxiom axiom) {
		double acc = axiom.getScore().getAccuracy() * 100;
		String accs = df.format(acc);
		if(accs.length()==3) { accs = "  " + accs; }
		if(accs.length()==4) { accs = " " + accs; }
		String str =  accs + "%\t" + axiom.getAxiom().toManchesterSyntaxString(null, PrefixCCMap.getInstance());
		return str;
	}
	
	
	public String render(OWLAxiom value, int depth){
		String renderedString = manchesterSyntaxRenderer.render(value, OWLAPIConverter.getOWLAPIEntity(currentEntity));
		StringTokenizer st = new StringTokenizer(renderedString);
		StringBuffer bf = new StringBuffer();
		
		bf.append("<html>");
		
		for(int i = 0; i < depth; i++){
			bf.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		}
		
		String token;
		while(st.hasMoreTokens()){
			token = st.nextToken();
			String color = "black";
			
			boolean isReserved = false;
			String c = colorMap.get(token);
			if(c != null){
				isReserved = true;
				color = c;
			}
			if(isReserved){
				bf.append("<b><font color=" + color + ">" + token + " </font></b>");
			} else {
				bf.append(" " + token + " ");
			}
		}
		bf.append("</html>");
		renderedString = bf.toString();
		try {
			renderedString = URLDecoder.decode(renderedString, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return renderedString;
	}
}
