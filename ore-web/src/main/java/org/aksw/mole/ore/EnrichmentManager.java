package org.aksw.mole.ore;

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.aksw.mole.ore.exception.OREException;
import org.aksw.mole.ore.model.ResourceType;
import org.aksw.mole.ore.rendering.KeywordColorMap;
import org.aksw.mole.ore.util.UnsortedManchesterSyntaxRendererImpl;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
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
import org.dllearner.algorithms.properties.ObjectPropertyDomainAxiomLearner;
import org.dllearner.algorithms.properties.ObjectPropertyRangeAxiomLearner;
import org.dllearner.algorithms.properties.ReflexiveObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.SubDataPropertyOfAxiomLearner;
import org.dllearner.algorithms.properties.SubObjectPropertyOfAxiomLearner;
import org.dllearner.algorithms.properties.SymmetricObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.TransitiveObjectPropertyAxiomLearner;
import org.dllearner.core.AbstractAxiomLearningAlgorithm;
import org.dllearner.core.AnnComponentManager;
import org.dllearner.core.AxiomLearningAlgorithm;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.core.LearningAlgorithm;
import org.dllearner.core.config.ConfigHelper;
import org.dllearner.core.owl.DatatypeProperty;
import org.dllearner.core.owl.Entity;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SPARQLTasks;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.utilities.PrefixCCMap;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;

public class EnrichmentManager {
	
	private List<Class<? extends LearningAlgorithm>> objectPropertyAlgorithms;
	private List<Class<? extends LearningAlgorithm>> dataPropertyAlgorithms;
	private List<Class<? extends LearningAlgorithm>> classAlgorithms;
	
	private BidiMap<AxiomType, Class<? extends LearningAlgorithm>> axiomType2Class;
	
	private UnsortedManchesterSyntaxRendererImpl manchesterSyntaxRenderer = new UnsortedManchesterSyntaxRendererImpl();// ManchesterOWLSyntaxOWLObjectRendererImpl();
	private KeywordColorMap colorMap = new KeywordColorMap();
	
	private DecimalFormat df = new DecimalFormat("##0.0");
	
	private SparqlEndpoint endpoint;
	private SPARQLReasoner reasoner;
	private ExtractionDBCache cache;
	private ResourceType resourceType;
	
	private int maxExecutionTimeInSeconds = 10;
	private double threshold = 0.75;
	private int maxNrOfReturnedAxioms = 5;
	private boolean useInference;
	
	private List<AxiomType> axiomTypes;
	
	private List<EvaluatedAxiom> learnedAxioms;
	
	private Entity currentEntity;
	private LearningAlgorithm currentAlgorithm;
	
	private boolean isRunning = false;
	
	public EnrichmentManager(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		this.endpoint = endpoint;
		this.cache = cache;
		
		reasoner = new SPARQLReasoner(new SparqlEndpointKS(endpoint));
		reasoner.setCache(cache);
		
		objectPropertyAlgorithms = new LinkedList<Class<? extends LearningAlgorithm>>();
		objectPropertyAlgorithms.add(DisjointObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(EquivalentObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(SubObjectPropertyOfAxiomLearner.class);
		objectPropertyAlgorithms.add(FunctionalObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(InverseFunctionalObjectPropertyAxiomLearner.class);
		objectPropertyAlgorithms.add(ObjectPropertyDomainAxiomLearner.class);
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
		axiomType2Class.put(AxiomType.OBJECT_PROPERTY_DOMAIN, ObjectPropertyDomainAxiomLearner.class);
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
	
	public Collection<AxiomType> getAxiomTypes(ResourceType type){
		List<AxiomType> types = new ArrayList<AxiomType>();
		
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
	
	public List<EvaluatedAxiom> getEvaluatedAxioms(String resourceURI, ResourceType resourceType, AxiomType axiomType, int maxExecutionTimeInSeconds, double threshold, boolean useInference) throws OREException{
		return getEvaluatedAxioms(resourceURI, resourceType, Collections.singletonList(axiomType), maxExecutionTimeInSeconds, threshold, useInference);
	}
	
	public List<EvaluatedAxiom> getEvaluatedAxioms(String resourceURI, ResourceType resourceType, List<AxiomType> axiomTypes, int maxExecutionTimeInSeconds, double threshold, boolean useInference) throws OREException{
		this.axiomTypes = axiomTypes;
		if(!isRunning){
			isRunning = true;
			learnedAxioms = new ArrayList<EvaluatedAxiom>();
			
			try {
				// common helper objects
				SPARQLTasks st = new SPARQLTasks(endpoint);
				
				currentEntity = getEntity(resourceURI, resourceType);
				if(currentEntity == null){
					currentEntity = st.guessResourceType(resourceURI, true);
				} 
				
				SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
				ks.init();
				
				//check if endpoint supports SPARQL 1.1
				boolean supportsSPARQL_1_1 = st.supportsSPARQL_1_1();
				ks.setSupportsSPARQL_1_1(supportsSPARQL_1_1);
				
				if(useInference && !reasoner.isPrepared()){
					System.out.print("Precomputing subsumption hierarchy ... ");
					long startTime = System.currentTimeMillis();
					reasoner.prepareSubsumptionHierarchy();
					System.out.println("done in " + (System.currentTimeMillis() - startTime) + " ms");
				}
				
				if(currentEntity instanceof ObjectProperty) {
					runObjectPropertyAlgorithms(ks, (ObjectProperty) currentEntity);
				} else if(currentEntity instanceof DatatypeProperty) {
					runDataPropertyAlgorithms(ks, (DatatypeProperty) currentEntity);
				} else if(currentEntity instanceof NamedClass) {
					runClassLearningAlgorithms(ks, (NamedClass) currentEntity);				
				} else {
					throw new OREException(new Exception("The type " + currentEntity.getClass() + " of resource " + currentEntity + " cannot be handled by this enrichment tool."));
				}
			} catch (ComponentInitException e) {
				e.printStackTrace();
				throw new OREException();
			}
			isRunning = false;
		}
		
		
		return learnedAxioms;
	}
	
	public List<EvaluatedAxiom> getEvaluatedAxioms2(String resourceURI, ResourceType resourceType, AxiomType axiomType, int maxExecutionTimeInSeconds, double threshold, boolean useInference) throws OREException{
			List<EvaluatedAxiom> learnedAxioms = new ArrayList<EvaluatedAxiom>();
			
			try {
				// common helper objects
				SPARQLTasks st = new SPARQLTasks(endpoint);
				
				Entity currentEntity = getEntity(resourceURI, resourceType);
				if(currentEntity == null){
					currentEntity = st.guessResourceType(resourceURI, true);
				} 
				
				SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
				ks.init();
				
				//check if endpoint supports SPARQL 1.1
				boolean supportsSPARQL_1_1 = st.supportsSPARQL_1_1();
				ks.setSupportsSPARQL_1_1(supportsSPARQL_1_1);
				
				if(useInference && !reasoner.isPrepared()){
					System.out.print("Precomputing subsumption hierarchy ... ");
					long startTime = System.currentTimeMillis();
					reasoner.prepareSubsumptionHierarchy();
					System.out.println("done in " + (System.currentTimeMillis() - startTime) + " ms");
				}
				learnedAxioms = applyLearningAlgorithm2(axiomType2Class.get(axiomType), ks, currentEntity);
				
			} catch (ComponentInitException e) {
				e.printStackTrace();
				throw new OREException();
			}
		
		
		return learnedAxioms;
	}
	
	public List<EvaluatedAxiom> getEvaluatedAxioms2(String resourceURI, AxiomType axiomType) throws OREException{
		List<EvaluatedAxiom> learnedAxioms = new ArrayList<EvaluatedAxiom>();
		
		try {
			// common helper objects
			SPARQLTasks st = new SPARQLTasks(endpoint);
			
			Entity currentEntity = getEntity(resourceURI, resourceType);
			if(currentEntity == null){
				currentEntity = st.guessResourceType(resourceURI, true);
			} 
			
			SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
			ks.init();
			
			//check if endpoint supports SPARQL 1.1
			boolean supportsSPARQL_1_1 = st.supportsSPARQL_1_1();
			ks.setSupportsSPARQL_1_1(supportsSPARQL_1_1);
			
			if(useInference && !reasoner.isPrepared()){
				System.out.print("Precomputing subsumption hierarchy ... ");
				long startTime = System.currentTimeMillis();
				reasoner.prepareSubsumptionHierarchy();
				System.out.println("done in " + (System.currentTimeMillis() - startTime) + " ms");
			}
			learnedAxioms = applyLearningAlgorithm2(axiomType2Class.get(axiomType), ks, currentEntity);
			
		} catch (ComponentInitException e) {
			e.printStackTrace();
			throw new OREException();
		}
	
	
	return learnedAxioms;
}
	
	public List<EvaluatedAxiom> getEvaluatedAxioms(String resourceURI, ResourceType resourceType, int maxExecutionTimeInSeconds, double threshold, boolean useInference) throws OREException{
		if(!isRunning){
			isRunning = true;
			learnedAxioms = new ArrayList<EvaluatedAxiom>();
			
			try {
				// common helper objects
				SPARQLTasks st = new SPARQLTasks(endpoint);
				
				currentEntity = getEntity(resourceURI, resourceType);
				if(currentEntity == null){
					currentEntity = st.guessResourceType(resourceURI, true);
				} 
				
				SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
				ks.init();
				
				//check if endpoint supports SPARQL 1.1
				boolean supportsSPARQL_1_1 = st.supportsSPARQL_1_1();
				ks.setSupportsSPARQL_1_1(supportsSPARQL_1_1);
				
				if(useInference && !reasoner.isPrepared()){
					System.out.print("Precomputing subsumption hierarchy ... ");
					long startTime = System.currentTimeMillis();
					reasoner.prepareSubsumptionHierarchy();
					System.out.println("done in " + (System.currentTimeMillis() - startTime) + " ms");
				}
				
				if(currentEntity instanceof ObjectProperty) {
					runObjectPropertyAlgorithms(ks, (ObjectProperty) currentEntity);
				} else if(currentEntity instanceof DatatypeProperty) {
					runDataPropertyAlgorithms(ks, (DatatypeProperty) currentEntity);
				} else if(currentEntity instanceof NamedClass) {
					runClassLearningAlgorithms(ks, (NamedClass) currentEntity);				
				} else {
					throw new OREException(new Exception("The type " + currentEntity.getClass() + " of resource " + currentEntity + " cannot be handled by this enrichment tool."));
				}
			} catch (ComponentInitException e) {
				e.printStackTrace();
				throw new OREException();
			}
			isRunning = false;
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
	
	private List<EvaluatedAxiom> applyLearningAlgorithm2(Class<? extends LearningAlgorithm> algorithmClass, SparqlEndpointKS ks, Entity entity) throws ComponentInitException {
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
		return learnedAxioms;
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
