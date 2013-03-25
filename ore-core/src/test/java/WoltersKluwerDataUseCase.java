

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.mole.ore.validation.AsymmetryConsistencyValidator;
import org.aksw.mole.ore.validation.ConsistencyValidator;
import org.aksw.mole.ore.validation.FunctionalityConsistencyValidator;
import org.aksw.mole.ore.validation.InverseFunctionalityConsistencyValidator;
import org.aksw.mole.ore.validation.IrreflexivityConsistencyValidator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dllearner.algorithms.DisjointClassesLearner;
import org.dllearner.algorithms.properties.AsymmetricObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.DataPropertyDomainAxiomLearner;
import org.dllearner.algorithms.properties.DataPropertyRangeAxiomLearner;
import org.dllearner.algorithms.properties.FunctionalDataPropertyAxiomLearner;
import org.dllearner.algorithms.properties.FunctionalObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.InverseFunctionalObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.IrreflexiveObjectPropertyAxiomLearner;
import org.dllearner.algorithms.properties.ObjectPropertyDomainAxiomLearner;
import org.dllearner.algorithms.properties.ObjectPropertyRangeAxiomLearner;
import org.dllearner.core.AbstractAxiomLearningAlgorithm;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.core.config.ConfigHelper;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLUnaryPropertyAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

import com.clarkparsia.owlapi.explanation.PelletExplanation;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class WoltersKluwerDataUseCase {
	
	private static final Logger logger = Logger.getLogger(WoltersKluwerDataUseCase.class.getName());
	
	private static String propertyAxiomsFile = "src/test/resources/learnedPropertyAxioms.owl";
	private static String classAxiomsFile = "src/test/resources/learnedClassAxioms.owl";
	private static String alignmentProperty = OWL.sameAs.getURI();//"http://www.w3.org/2004/02/skos/core#exactMatch";
	private static ExtractionDBCache wkCache = new ExtractionDBCache("wk_cache");
	private static ExtractionDBCache dbpediaCache = new ExtractionDBCache("dbpedia_cache");
	private static SparqlEndpoint dbpediaEndpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();
	private static SparqlEndpoint germanDBbpediaEndpoint;
	private static SparqlEndpoint wkEndpoint1;//Arbeitsrechtthesaurus
	private static SparqlEndpoint wkEndpoint2;//Gerichtsthesaurus
	private static SparqlEndpoint wkEndpoint3;
	private static final double threshold = 0.7d;
	
	private static final List<String> blacklist = Arrays.asList(new String[]{
			"http://dbpedia.org/ontology/wikiPageInterLanguageLink",
			"http://dbpedia.org/ontology/wikiPageExternalLink",
			"http://dbpedia.org/ontology/wikiPageRedirects",
			"http://dbpedia.org/ontology/wikiPageWikiLink",
			"http://dbpedia.org/ontology/thumbnail",
			"http://dbpedia.org/ontology/wikiPageDisambiguates"
	});
	
	static{
		try {
			wkEndpoint1 = new SparqlEndpoint(new URL("http://vocabulary.wolterskluwer.de/PoolParty/sparql/arbeitsrecht"));
			wkEndpoint2 = new SparqlEndpoint(new URL("http://vocabulary.wolterskluwer.de/PoolParty/sparql/court"));
			wkEndpoint3 = new SparqlEndpoint(new URL("http://wp7.lod2.eu/virtuoso/sparql"));
			germanDBbpediaEndpoint = new SparqlEndpoint(new URL("http://de.dbpedia.org/sparql"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		PelletExplanation.setup();
		ToStringRenderer.getInstance().setRenderer(new DLSyntaxObjectRenderer());
	}
	
	public WoltersKluwerDataUseCase() {
		
	}
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ComponentInitException {
		Authenticator.setDefault (new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("wkd", "wkdlod2".toCharArray());
			}
		});
		
		
		Logger.getLogger(WoltersKluwerDataUseCase.class).setLevel(Level.DEBUG);
		//1. load all entities to which WK entities are linked
		List<String> alignedEntites = new ArrayList<String>();
		String query = String.format("SELECT DISTINCT ?o WHERE {?s <%s> ?o.}", alignmentProperty);
		ResultSet rs = new QueryEngineHTTP(wkEndpoint3.getURL().toString(), query).execSelect();
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			String uri = qs.getResource("o").getURI();
			//add only entities from DBpedia or German DBpedia
			if(uri.startsWith("http://dbpedia.org") || uri.startsWith("http://de.dbpedia.org")){
				alignedEntites.add(uri);
			}
		}
		logger.info("Loaded " + alignedEntites.size() + " entities.");
		
		//2. for each entity we load all information about it, i.e. we load the model via CONSTRUCT queries
		Model model = ModelFactory.createDefaultModel();
		ConciseBoundedDescriptionGenerator dbpediaCBDGenerator = new ConciseBoundedDescriptionGeneratorImpl(dbpediaEndpoint, dbpediaCache);
		ConciseBoundedDescriptionGenerator germanDBpediaCBDGenerator = new ConciseBoundedDescriptionGeneratorImpl(germanDBbpediaEndpoint, dbpediaCache);
		dbpediaCBDGenerator.setRecursionDepth(1);
		germanDBpediaCBDGenerator.setRecursionDepth(1);
		Model cbd = null;
		for(String entity : alignedEntites){
			if(entity.startsWith("http://dbpedia.org")){
				cbd = dbpediaCBDGenerator.getConciseBoundedDescription(entity);
			} else if(entity.startsWith("http://de.dbpedia.org")){
				cbd = germanDBpediaCBDGenerator.getConciseBoundedDescription(entity);
			}
			logger.info("Got " + cbd.size() + " triples for " + entity);
			model.add(cbd);
		}
		logger.info("Overall model size: " + model.size());
		
		//3. load owl:sameAs closure
		for(Statement st : model.listStatements(null, OWL.sameAs, (RDFNode)null).toSet()){
			cbd.removeAll();
			String sameAsObjectURI = st.getObject().asResource().getURI();
			if(sameAsObjectURI.startsWith("http://dbpedia.org")){
				cbd = dbpediaCBDGenerator.getConciseBoundedDescription(sameAsObjectURI);
			} else if(sameAsObjectURI.startsWith("http://de.dbpedia.org")){
				cbd = germanDBpediaCBDGenerator.getConciseBoundedDescription(sameAsObjectURI);
			}
			logger.info("Got " + cbd.size() + " triples for " + sameAsObjectURI);
			model.add(cbd);
		}
		logger.info("Overall model size: " + model.size());
		
		//4. get all properties in the model
		//allowed namespaces: http://dbpedia.org/ontology/
		Set<String> objectProperties = new TreeSet<String>();
		Set<String> dataProperties = new TreeSet<String>();
		query = "SELECT DISTINCT * WHERE {?s ?p ?o.}";
		QueryExecution qExec = QueryExecutionFactory.create(query, model);
		rs = qExec.execSelect();
		while(rs.hasNext()){
			qs = rs.next();
			String propertyURI = qs.getResource("p").getURI();
			RDFNode object = qs.get("o");
			if(propertyURI.startsWith("http://dbpedia.org/ontology/")){
				if(object.isResource()){
					objectProperties.add(propertyURI);
				} else if(object.isLiteral()){
					dataProperties.add(propertyURI);
				}
			}
		}
		objectProperties.removeAll(blacklist);
		dataProperties.removeAll(blacklist);
		logger.info("Number of object properties: " + objectProperties.size());
		logger.info("Number of data properties: " + dataProperties.size());
		//get all classes in the model
		Set<String> classes = new TreeSet<String>();
		for(RDFNode node : model.listObjectsOfProperty(RDF.type).toList()){
			String classURI = node.asResource().getURI();
			if(classURI.startsWith("http://dbpedia.org/ontology/")){
				classes.add(classURI);
			}
		}
		logger.info("Number of classes: " + classes.size());
		
		//generate the learned schema
		//5. load the schema from file if exist
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		SparqlEndpointKS ks = new SparqlEndpointKS(dbpediaEndpoint);
		SPARQLReasoner reasoner = new SPARQLReasoner(ks, dbpediaCache);
		reasoner.prepareSubsumptionHierarchy();
		reasoner.precomputeClassPopularity();
		OWLOntology learnedPropertyAxioms = null;
		try {
			learnedPropertyAxioms = man.loadOntologyFromOntologyDocument(new FileInputStream(propertyAxiomsFile));
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			logger.warn("Property axioms schema not found. Learning...");
		}
		
		List<AxiomType<? extends OWLAxiom>> propertyCharacteristicAxiomTypes = new ArrayList<AxiomType<? extends OWLAxiom>>();
		propertyCharacteristicAxiomTypes.add(AxiomType.FUNCTIONAL_OBJECT_PROPERTY);
		propertyCharacteristicAxiomTypes.add(AxiomType.FUNCTIONAL_DATA_PROPERTY);
		propertyCharacteristicAxiomTypes.add(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY);
		propertyCharacteristicAxiomTypes.add(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY);
		propertyCharacteristicAxiomTypes.add(AxiomType.ASYMMETRIC_OBJECT_PROPERTY);
		if(learnedPropertyAxioms == null){
			//5a. for each property we learn characteristics
			Set<EvaluatedAxiom> learnedAxioms = new HashSet<EvaluatedAxiom>();
			BiMap<AxiomType<? extends OWLAxiom>, Class<? extends AbstractAxiomLearningAlgorithm>> type2ObjectPropertyAlgorithm = HashBiMap.create();
			type2ObjectPropertyAlgorithm.put(AxiomType.FUNCTIONAL_OBJECT_PROPERTY, FunctionalObjectPropertyAxiomLearner.class);
			type2ObjectPropertyAlgorithm.put(AxiomType.ASYMMETRIC_OBJECT_PROPERTY, AsymmetricObjectPropertyAxiomLearner.class);
			type2ObjectPropertyAlgorithm.put(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY, InverseFunctionalObjectPropertyAxiomLearner.class);
			type2ObjectPropertyAlgorithm.put(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY, IrreflexiveObjectPropertyAxiomLearner.class);
			type2ObjectPropertyAlgorithm.put(AxiomType.OBJECT_PROPERTY_DOMAIN, ObjectPropertyDomainAxiomLearner.class);
			type2ObjectPropertyAlgorithm.put(AxiomType.OBJECT_PROPERTY_RANGE, ObjectPropertyRangeAxiomLearner.class);
			BiMap<AxiomType<? extends OWLAxiom>, Class<? extends AbstractAxiomLearningAlgorithm>> type2DataPropertyAlgorithm = HashBiMap.create();
			type2DataPropertyAlgorithm.put(AxiomType.FUNCTIONAL_DATA_PROPERTY, FunctionalDataPropertyAxiomLearner.class);
			type2DataPropertyAlgorithm.put(AxiomType.DATA_PROPERTY_DOMAIN, DataPropertyDomainAxiomLearner.class);
			type2DataPropertyAlgorithm.put(AxiomType.DATA_PROPERTY_RANGE, DataPropertyRangeAxiomLearner.class);
			Multimap<AxiomType<? extends OWLAxiom>, String> type2Properties = HashMultimap.create();
			
			//set log level to ERROR
			for(Class<? extends org.dllearner.core.AbstractAxiomLearningAlgorithm> algorithmClass : type2ObjectPropertyAlgorithm.values()){
				Logger.getLogger(algorithmClass).setLevel(Level.ERROR);
			}
			Logger.getLogger(FunctionalDataPropertyAxiomLearner.class).setLevel(Level.ERROR);
			//object property axioms
			for(String prop : objectProperties){
				logger.debug(prop);
				for (Entry<AxiomType<? extends OWLAxiom>, Class<? extends AbstractAxiomLearningAlgorithm>> entry : type2ObjectPropertyAlgorithm.entrySet()) {
					AxiomType<? extends OWLAxiom> axiomType = entry.getKey();
					Class<? extends AbstractAxiomLearningAlgorithm> algorithmClass = entry.getValue();
					System.out.println(algorithmClass);
					try {
						AbstractAxiomLearningAlgorithm learner = algorithmClass.getConstructor(
								SparqlEndpointKS.class).newInstance(ks);
						ConfigHelper.configure(learner, "propertyToDescribe", prop);
						learner.setReasoner(reasoner);
						learner.setMaxExecutionTimeInSeconds(60);
						learner.init();
						learner.start();
						List<EvaluatedAxiom> axioms = learner.getCurrentlyBestEvaluatedAxioms(threshold);
						if(!axioms.isEmpty()){
							type2Properties.put(axiomType, prop);
						}
						learnedAxioms.addAll(axioms);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			//data property axioms
			for(String prop : dataProperties){
				logger.debug(prop);
				for (Entry<AxiomType<? extends OWLAxiom>, Class<? extends AbstractAxiomLearningAlgorithm>> entry : type2DataPropertyAlgorithm.entrySet()) {
					AxiomType<? extends OWLAxiom> axiomType = entry.getKey();
					Class<? extends AbstractAxiomLearningAlgorithm> algorithmClass = entry.getValue();
					try {
						AbstractAxiomLearningAlgorithm learner = algorithmClass.getConstructor(
								SparqlEndpointKS.class).newInstance(ks);
						ConfigHelper.configure(learner, "propertyToDescribe", prop);
						learner.setReasoner(reasoner);
						learner.setMaxExecutionTimeInSeconds(60);
						learner.init();
						learner.start();
						List<EvaluatedAxiom> axioms = learner.getCurrentlyBestEvaluatedAxioms(threshold);
						if(!axioms.isEmpty()){
							type2Properties.put(axiomType, prop);
						}
						learnedAxioms.addAll(axioms);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			//Save the learned axioms to avoid relearning every time
			Set<OWLAxiom> owlAxioms = new HashSet<OWLAxiom>();
			for(EvaluatedAxiom evAx : learnedAxioms){
				OWLAxiom ax = OWLAPIConverter.getOWLAPIAxiom(evAx.getAxiom());
				owlAxioms.add(ax);
				
			}
			try {
				learnedPropertyAxioms = man.createOntology(owlAxioms, IRI.create("http://dl-learner.org/enrichment/properties"));
				man.saveOntology(learnedPropertyAxioms, new RDFXMLOntologyFormat(), new FileOutputStream(propertyAxiomsFile));
			} catch (OWLOntologyCreationException e) {
				e.printStackTrace();
			} catch (OWLOntologyStorageException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
//			for(Entry<AxiomType<? extends OWLAxiom>, Collection<String>> entry : type2Properties.asMap().entrySet()){
//				logger.info(entry.getKey().getName() + ": " + entry.getValue().size());
//			}
		}
		for (AxiomType<? extends OWLAxiom> axiomType : propertyCharacteristicAxiomTypes) {
			Collection<String> properties = getProperties(axiomType, learnedPropertyAxioms);
			logger.info(axiomType + ": " + properties.size());
		}
		//learn disjointness axioms if not exist
		OWLOntology learnedClassAxioms = null;
		try {
			learnedClassAxioms = man.loadOntologyFromOntologyDocument(new FileInputStream(classAxiomsFile));
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			logger.warn("Class axioms schema not found. Learning...");
		}
		if(learnedClassAxioms == null){
			Set<EvaluatedAxiom> learnedAxioms = new HashSet<EvaluatedAxiom>();
			//disjoint class axioms
			DisjointClassesLearner learner = new DisjointClassesLearner(ks);
			for(String cls : classes){
				try {
					learner.setClassToDescribe(new NamedClass(cls));
					learner.setReasoner(reasoner);
					learner.setMaxExecutionTimeInSeconds(60);
					learner.init();
					learner.start();
					List<EvaluatedAxiom> axioms = learner.getCurrentlyBestEvaluatedAxioms(threshold);
					learnedAxioms.addAll(axioms);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			//Save the learned axioms to avoid relearning every time
			Set<OWLAxiom> owlAxioms = new HashSet<OWLAxiom>();
			for(EvaluatedAxiom evAx : learnedAxioms){
				OWLAxiom ax = OWLAPIConverter.getOWLAPIAxiom(evAx.getAxiom());
				owlAxioms.add(ax);
			}
			try {
				learnedClassAxioms = man.createOntology(owlAxioms, IRI.create("http://dl-learner.org/enrichment/classes"));
				man.saveOntology(learnedClassAxioms, new RDFXMLOntologyFormat(), new FileOutputStream(classAxiomsFile));
			} catch (OWLOntologyCreationException e) {
				e.printStackTrace();
			} catch (OWLOntologyStorageException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		
		
		//6. check for violations
		//6a Functionality
		ConsistencyValidator v = new FunctionalityConsistencyValidator(model);
		logger.info("Functionality Object Property Violations:");
		for(String prop : getProperties(AxiomType.FUNCTIONAL_OBJECT_PROPERTY, learnedPropertyAxioms)){
			if(!v.isConsistent(new ObjectProperty(prop))){
				logger.info(prop);
			}
		}
		logger.info("Functionality Data Property Violations:");
		for(String prop : getProperties(AxiomType.FUNCTIONAL_DATA_PROPERTY, learnedPropertyAxioms)){
			if(!v.isConsistent(new ObjectProperty(prop))){
				logger.info(prop);
			}
		}
		//6b Asymmetry
		v = new AsymmetryConsistencyValidator(model);
		logger.info("Asymmetry Violations:");
		for(String prop : getProperties(AxiomType.ASYMMETRIC_OBJECT_PROPERTY, learnedPropertyAxioms)){
			if(!v.isConsistent(new ObjectProperty(prop))){
				logger.info(prop);
			}
		}
		//6c Irreflexivity
		v = new IrreflexivityConsistencyValidator(model);
		logger.info("Irreflexivity Violations:");
		for(String prop : getProperties(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY, learnedPropertyAxioms)){
			if(!v.isConsistent(new ObjectProperty(prop))){
				logger.info(prop);
			}
		}
		//6d InverseFunctionality
		v = new InverseFunctionalityConsistencyValidator(model);
		logger.info("Inverse Functionality Violations:");
		for(String prop : getProperties(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY, learnedPropertyAxioms)){
			if(!v.isConsistent(new ObjectProperty(prop))){
				logger.info(prop);
			}
		}
		
		//check for further reasons for inconsistency
		Set<OWLOntology> ontologies = new HashSet<OWLOntology>(Arrays.asList(new OWLOntology[]{learnedPropertyAxioms, learnedClassAxioms}));
		try {
			OWLOntology schema = man.createOntology(IRI.create("http://dl-learner.org/enrichment/schema"), ontologies);
			schema.getOWLOntologyManager().removeAxioms(schema, schema.getAxioms(AxiomType.FUNCTIONAL_OBJECT_PROPERTY));
			schema.getOWLOntologyManager().removeAxioms(schema, schema.getAxioms(AxiomType.FUNCTIONAL_DATA_PROPERTY));
			OWLReasoner owlReasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(schema);
			logger.info("Unsatisfiable classes: "  + owlReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom());
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			model.write(baos, "TURTLE");
			OWLOntology data = man.loadOntologyFromOntologyDocument(new ByteArrayInputStream(baos.toByteArray()));
			
			OWLOntology merge = man.createOntology(IRI.create("http://dl-learner.org/enrichment/all"), new HashSet<OWLOntology>(Arrays.asList(new OWLOntology[]{schema, data})));
			owlReasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(merge);
			logger.info("Consistent KB: " + owlReasoner.isConsistent());
			
			PelletExplanation expGen = new PelletExplanation(merge);
			Set<Set<OWLAxiom>> explanations = expGen.getInconsistencyExplanations();
			for(Set<OWLAxiom> exp : explanations){
				System.out.println(exp);
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	private static Collection<String> getProperties(AxiomType<? extends OWLAxiom> axiomType, OWLOntology ontology){
		Set<String> properties = new TreeSet<String>();
		for(OWLAxiom ax : ontology.getAxioms(axiomType)){
			if(ax instanceof OWLUnaryPropertyAxiom){
				OWLPropertyExpression prop = ((OWLUnaryPropertyAxiom)ax).getProperty();
				properties.add(prop.getSignature().iterator().next().toStringID());
			}
		}
		return properties;
		
	}
	
	
	
	
	

}
