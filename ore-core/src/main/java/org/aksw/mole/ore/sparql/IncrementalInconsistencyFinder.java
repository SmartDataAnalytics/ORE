package org.aksw.mole.ore.sparql;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.jamonapi.Monitor;
import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.mole.ore.explanation.api.Explanation;
import org.aksw.mole.ore.explanation.impl.PelletExplanationGenerator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.dllearner.kb.extraction.ExtractionAlgorithm;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.utilities.JamonMonitorLogger;
import org.dllearner.utilities.owl.OWL2SPARULConverter;
import org.mindswap.pellet.PelletOptions;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class IncrementalInconsistencyFinder {
	
	private static Logger logger = Logger.getLogger(IncrementalInconsistencyFinder.class);
	
	private static final String DBPEDIA_PREDICATE_FILTER = "!regex(?predicate, \"http://dbpedia.org/property\")";
	private static final String DBPEDIA_SUBJECT_FILTER = "!regex(?subject, \"http://dbpedia.org/property\")";
	@SuppressWarnings("unused")
	private static final String OWL_OBJECT_FILTER = "!regex(?object, \"http://www.w3.org/2002/07/owl#\")";
	private static final String OWL_SUBJECT_FILTER = "!regex(?subject, \"http://www.w3.org/2002/07/owl#\")";
//	private static final String ENDPOINT_URL = "http://localhost:8890/sparql";
//	private static String DEFAULT_GRAPH_URI = "http://opencyc2.org"; //(version 2.0)
	
	private static int OFFSET = 100;
	private static int AXIOM_COUNT = 100;
	
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private OWLDataFactory factory;
	private OWLReasoner reasoner;
	
	private SPARQLProgressMonitor mon = new SilentSPARQLProgressMonitor();
	
	private boolean consistent = true;
	private boolean useLinkedData;
	private boolean useCache;
	
	@SuppressWarnings("unused")
	private volatile boolean stop;
	
	private Set<String> linkedDataNamespaces;
	
	private SparqlEndpoint endpoint;
	private QueryExecution currentQuery;
	
	private Cache cache;
	
	private Monitor overallMonitor = JamonMonitorLogger.getTimeMonitor(ExtractionAlgorithm.class, "Overall monitor");
	private Monitor queryMonitor = JamonMonitorLogger.getTimeMonitor(ExtractionAlgorithm.class, "Query monitor");
	private Monitor reasonerMonitor = JamonMonitorLogger.getTimeMonitor(ExtractionAlgorithm.class, "Reasoning monitor");
	
	private String cacheDir = "cache";
	
	private int disjointWithCount = Integer.MAX_VALUE; 
	private int functionalCount = Integer.MAX_VALUE; 
	private int inverseFunctionalCount = Integer.MAX_VALUE;  
	private int equivalentClassCount = Integer.MAX_VALUE; 
	private int subClassOfCount = Integer.MAX_VALUE; 
	private int domainCount = Integer.MAX_VALUE;  
	private int rangeCount = Integer.MAX_VALUE;  
	private int subPropertyOfCount = Integer.MAX_VALUE;  
	private int equivalentPropertyCount = Integer.MAX_VALUE; 
	private int inverseOfCount = Integer.MAX_VALUE;  
	private int transitiveCount = Integer.MAX_VALUE;  
	private int classAssertionCount = Integer.MAX_VALUE;  
	private int overallAxiomCount = Integer.MAX_VALUE;
	
	public IncrementalInconsistencyFinder(SparqlEndpoint endpoint){
		this(endpoint, null);
	}
	
	public IncrementalInconsistencyFinder(SparqlEndpoint endpoint, String cacheDir){
		this.endpoint = endpoint;
		this.cacheDir = cacheDir;
		
		PelletOptions.USE_COMPLETION_QUEUE = true;
		PelletOptions.USE_INCREMENTAL_CONSISTENCY = true;
		PelletOptions.USE_SMART_RESTORE = false;
		
		useCache = (cacheDir != null);
		
		linkedDataNamespaces = new HashSet<String>();
		
		manager = OWLManager.createOWLOntologyManager();
		try {
			ontology = manager.createOntology(IRI.create(endpoint.getURL().toString() + "/" + "fragment"));
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		}
		factory = manager.getOWLDataFactory();
		reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(ontology);
	}
	
	
	public void setProgressMonitor(SPARQLProgressMonitor mon){
		this.mon = mon;
	}
	
	public void run(){
		logger.info("Searching for inconsistency on " + endpoint.getURL().toString());
		manager.removeAxioms(ontology, ontology.getAxioms());
		mon.setMessage("Searching...");
	
		stop = false;
		
		consistent = true;
		
		overallMonitor.reset();
		reasonerMonitor.reset();
		queryMonitor.reset();
		overallMonitor.start();
		
		
		if(canCountAxioms()){
			disjointWithCount = getAxiomCountForPredicate(OWL.disjointWith);
			functionalCount = getAxiomCountForObject(OWL.FunctionalProperty);
			inverseFunctionalCount = getAxiomCountForObject(OWL.InverseFunctionalProperty);
			
			//we can stop here if none of the axioms is contained in the knowledge base
			if((disjointWithCount + functionalCount + inverseFunctionalCount) == 0){
				return;
			}
			
			equivalentClassCount = getAxiomCountForPredicate(OWL.equivalentClass);
			subClassOfCount = getAxiomCountForPredicate(RDFS.subClassOf);
			
			domainCount = getAxiomCountForPredicate(RDFS.domain);
			rangeCount = getAxiomCountForPredicate(RDFS.range);
			
			subPropertyOfCount = getAxiomCountForPredicate(RDFS.subPropertyOf);
			equivalentPropertyCount = getAxiomCountForPredicate(OWL.equivalentProperty);
			inverseOfCount = getAxiomCountForPredicate(OWL.inverseOf);
			
			transitiveCount = getAxiomCountForObject(OWL.TransitiveProperty);
			
			classAssertionCount = getAxiomCountForPredicate(RDF.type);
			
			overallAxiomCount = disjointWithCount + equivalentClassCount + subClassOfCount + domainCount + rangeCount 
			+ subPropertyOfCount + equivalentPropertyCount + inverseOfCount + functionalCount
			+ inverseFunctionalCount + transitiveCount + classAssertionCount;
			
			mon.setSize(overallAxiomCount);
		}
		
		
		Set<OWLClass> visitedClasses = new HashSet<OWLClass>();
		Set<OWLNamedIndividual> visitedIndividuals = new HashSet<OWLNamedIndividual>();
		Set<OWLObject> visitedLinkedDataResources = new HashSet<OWLObject>();
		
		Set<OWLAxiom> disjointAxioms = new HashSet<OWLAxiom>();
		Set<OWLAxiom> domainAxioms = new HashSet<OWLAxiom>();
		Set<OWLAxiom> rangeAxioms = new HashSet<OWLAxiom>();
		Set<OWLAxiom> subClassOfAxioms = new HashSet<OWLAxiom>();
		Set<OWLClassAssertionAxiom> classAssertionAxioms = new HashSet<OWLClassAssertionAxiom>();
		
		Set<OWLObjectProperty> functionalObjectProperties = new HashSet<OWLObjectProperty>();
		Set<OWLDataProperty> functionalDataProperties = new HashSet<OWLDataProperty>();
		
		int i = 0;
		while(!mon.isCancelled()){
			//first we expand the ontology schema
			
			//retrieve TBox axioms
			if(ontology.getAxiomCount(AxiomType.DISJOINT_CLASSES) < disjointWithCount){
				disjointAxioms.addAll(retrieveClassExpressionsAxioms(OWL.disjointWith, AXIOM_COUNT, OFFSET * i)); 
				manager.addAxioms(ontology, disjointAxioms);
			}
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			if(ontology.getAxiomCount(AxiomType.FUNCTIONAL_OBJECT_PROPERTY) + ontology.getAxiomCount(AxiomType.FUNCTIONAL_DATA_PROPERTY) < functionalCount){
				Set<OWLAxiom> functPropAxioms = retrievePropertyCharacteristicAxioms(OWL.FunctionalProperty, AXIOM_COUNT, OFFSET * i);
				for(OWLAxiom ax : functPropAxioms){
					if(ax instanceof OWLFunctionalObjectPropertyAxiom){
						functionalObjectProperties.addAll(ax.getObjectPropertiesInSignature());
					} else if(ax instanceof OWLFunctionalDataPropertyAxiom){
						functionalDataProperties.addAll(ax.getDataPropertiesInSignature());
					}
				}
				manager.addAxioms(ontology, functPropAxioms);
			}
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			if(ontology.getAxiomCount(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY) < inverseFunctionalCount){
				manager.addAxioms(ontology, retrievePropertyCharacteristicAxioms(OWL.InverseFunctionalProperty, AXIOM_COUNT, OFFSET * i));
			}
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			if(ontology.getLogicalAxiomCount() == 0){
				return;
			}
			if(ontology.getAxiomCount(AxiomType.EQUIVALENT_CLASSES) < equivalentClassCount){
				manager.addAxioms(ontology, retrieveClassExpressionsAxioms(OWL.equivalentClass, AXIOM_COUNT, OFFSET * i));
			}
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			if(ontology.getAxiomCount(AxiomType.SUBCLASS_OF) < subClassOfCount){
				subClassOfAxioms.addAll(retrieveClassExpressionsAxioms(RDFS.subClassOf, AXIOM_COUNT, OFFSET * i));
				manager.addAxioms(ontology, subClassOfAxioms);
			}
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			//retrieve RBox axioms
			if(ontology.getAxiomCount(AxiomType.OBJECT_PROPERTY_DOMAIN) + ontology.getAxiomCount(AxiomType.DATA_PROPERTY_DOMAIN) < domainCount){
				domainAxioms.addAll(retrievePropertyAxioms(RDFS.domain, AXIOM_COUNT, OFFSET * i));
				manager.addAxioms(ontology, domainAxioms);
			}
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			if(ontology.getAxiomCount(AxiomType.OBJECT_PROPERTY_RANGE) + ontology.getAxiomCount(AxiomType.DATA_PROPERTY_RANGE) < rangeCount){
				rangeAxioms.addAll(retrievePropertyAxioms(RDFS.range, AXIOM_COUNT, OFFSET * i));
				manager.addAxioms(ontology, rangeAxioms);
			}
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			if(ontology.getAxiomCount(AxiomType.SUB_OBJECT_PROPERTY) + ontology.getAxiomCount(AxiomType.SUB_DATA_PROPERTY) < subPropertyOfCount){
				manager.addAxioms(ontology, retrievePropertyAxioms(RDFS.subPropertyOf, AXIOM_COUNT, OFFSET * i));
			}
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			if(ontology.getAxiomCount(AxiomType.EQUIVALENT_OBJECT_PROPERTIES) + ontology.getAxiomCount(AxiomType.EQUIVALENT_DATA_PROPERTIES) < equivalentPropertyCount){
				manager.addAxioms(ontology, retrievePropertyAxioms(OWL.equivalentProperty, AXIOM_COUNT, OFFSET * i));
			}
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			if(ontology.getAxiomCount(AxiomType.INVERSE_OBJECT_PROPERTIES) < inverseOfCount){
				manager.addAxioms(ontology, retrievePropertyAxioms(OWL.inverseOf, AXIOM_COUNT, OFFSET * i));
			}
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			if(ontology.getAxiomCount(AxiomType.TRANSITIVE_OBJECT_PROPERTY) < transitiveCount){
				manager.addAxioms(ontology, retrievePropertyCharacteristicAxioms(OWL.TransitiveProperty, AXIOM_COUNT, OFFSET * i));
			}
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			//removal due to find other inconsistencies not including this axiom
//			manager.removeAxiom(ontology, factory.getOWLDisjointClassesAxiom(
//					factory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Organisation")),
//					factory.getOWLClass(IRI.create("http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing"))
//					));
//			manager.removeAxiom(ontology, factory.getOWLObjectPropertyRangeAxiom(
//					factory.getOWLObjectProperty(IRI.create("http://dbpedia.org/ontology/artist")),
//					factory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Person"))));
//			manager.removeAxiom(ontology, factory.getOWLDisjointClassesAxiom(
//					factory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Person")),
//					factory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Organisation"))));
//			manager.removeAxiom(ontology, factory.getOWLDisjointClassesAxiom(
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4r16i50nS6EdaAAACgyZzFrg")),
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4rCX1yJHS-EdaAAACgyZzFrg"))));
//			manager.removeAxiom(ontology, factory.getOWLDisjointClassesAxiom(
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4r16i50nS6EdaAAACgyZzFrg")),
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4r0y21PnS-EdaAAACgyZzFrg"))));
//			manager.removeAxiom(ontology, factory.getOWLDisjointClassesAxiom(
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4r8LonGrboEduAAADggVbxzQ")),
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx8Ngh4rD2crlmAKQdmLOr88Soi59h4r8LonGrboEduAAADggVbxzQ"))));
//			manager.removeAxiom(ontology, factory.getOWLDisjointClassesAxiom(
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4r8LonGrboEduAAADggVbxzQ")),
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx8Ngh4rD2crlmAKQdmLOr88Soi59h4r8LonGrboEduAAADggVbxzQ"))));
					
			for(OWLAxiom ax : disjointAxioms){
				if(mon.isCancelled()){
					break;
				}
				//retrieve instances contained in both classes
				manager.addAxioms(ontology, retrieveClassAssertionAxiomsForClasses(
						((OWLDisjointClassesAxiom)ax).getClassExpressionsAsList().get(0).asOWLClass(),
						((OWLDisjointClassesAxiom)ax).getClassExpressionsAsList().get(1).asOWLClass(), AXIOM_COUNT));
				mon.setProgress(ontology.getLogicalAxiomCount());
				//retrieve instances contained in first class
				manager.addAxioms(ontology, retrieveClassAssertionAxiomsForClass(
						((OWLDisjointClassesAxiom)ax).getClassExpressionsAsList().get(0).asOWLClass(), AXIOM_COUNT));
				mon.setProgress(ontology.getLogicalAxiomCount());
				//retrieve instances contained in second class
				manager.addAxioms(ontology, retrieveClassAssertionAxiomsForClass(
						((OWLDisjointClassesAxiom)ax).getClassExpressionsAsList().get(1).asOWLClass(), AXIOM_COUNT));
				mon.setProgress(ontology.getLogicalAxiomCount());
				
			}
			if(mon.isCancelled()){
				break;
			}
			if(!checkConsistency()){
				break;
			}
			for(OWLObjectProperty prop : functionalObjectProperties){
				manager.addAxioms(ontology, retrieveAxiomsViolatingFunctionality(prop, AXIOM_COUNT));
			}
			if(mon.isCancelled()){
				break;
			}
			if(!checkConsistency()){
				break;
			}
			for(OWLDataProperty prop : functionalDataProperties){
				manager.addAxioms(ontology, retrieveAxiomsViolatingFunctionality(prop, AXIOM_COUNT));
			}
			if(mon.isCancelled()){
				break;
			}
			if(!checkConsistency()){
				break;
			}
			OWLClass domain;
			for(OWLAxiom ax : domainAxioms){
				if(mon.isCancelled()){
					break;
				}
				domain = ((OWLPropertyDomainAxiom<?>)ax).getDomain().asOWLClass();
				//retrieve instances for the domain class
				manager.addAxioms(ontology, retrieveClassAssertionAxiomsForClass(
						domain, AXIOM_COUNT));
				mon.setProgress(ontology.getLogicalAxiomCount());
				//retrieve property assertions
				if(ax instanceof OWLObjectPropertyDomainAxiom){
					manager.addAxioms(ontology, retrieveObjectPropertyAssertionAxioms(((OWLObjectPropertyDomainAxiom)ax).getProperty().asOWLObjectProperty(), AXIOM_COUNT));
				} else {
					manager.addAxioms(ontology, retrieveDataPropertyAssertionAxioms(((OWLDataPropertyDomainAxiom)ax).getProperty().asOWLDataProperty(), AXIOM_COUNT));
				}
				mon.setProgress(ontology.getLogicalAxiomCount());
				
			}
			if(mon.isCancelled()){
				break;
			}
			if(!checkConsistency()){
				break;
			}
			OWLClass range;
			for(OWLAxiom ax : rangeAxioms){
				if(mon.isCancelled()){
					break;
				}
				if(ax instanceof OWLObjectPropertyRangeAxiom){
					range = ((OWLObjectPropertyRangeAxiom)ax).getRange().asOWLClass();
					//retrieve instances for the range class
					manager.addAxioms(ontology, retrieveClassAssertionAxiomsForClass(
							range, AXIOM_COUNT));
					mon.setProgress(ontology.getLogicalAxiomCount());
					//retrieve property assertions
					manager.addAxioms(ontology, retrieveObjectPropertyAssertionAxioms(((OWLObjectPropertyRangeAxiom)ax).getProperty().asOWLObjectProperty(), AXIOM_COUNT));
					mon.setProgress(ontology.getLogicalAxiomCount());
				}
			}
			if(mon.isCancelled()){
				break;
			}
			if(!checkConsistency()){
				break;
			}
			//retrieve ClassAssertion axioms for each new class
			for(OWLAxiom ax : subClassOfAxioms){
				if(mon.isCancelled()){
					break;
				}
				for(OWLClass cls : ax.getClassesInSignature()){
					if(visitedClasses.contains(cls)){
						continue;
					}
					classAssertionAxioms.addAll(retrieveClassAssertionAxiomsForClass(cls, 50));
					visitedClasses.add(cls);
				}
				
			}
			manager.addAxioms(ontology, classAssertionAxioms);
			mon.setProgress(ontology.getLogicalAxiomCount());
			if(mon.isCancelled()){
				break;
			}
			if(!checkConsistency()){
				break;
			}
			//for each individual in the ClassAssertion axioms found above, we retrieve further informations
			for(OWLClassAssertionAxiom ax : classAssertionAxioms){
				if(mon.isCancelled()){
					break;
				}
				if(visitedIndividuals.contains(ax.getIndividual().asOWLNamedIndividual())){
					continue;
				}
				manager.addAxioms(ontology, retrieveAxiomsForIndividual(ax.getIndividual().asOWLNamedIndividual(), 1000));
				mon.setProgress(ontology.getLogicalAxiomCount());
				visitedIndividuals.add(ax.getIndividual().asOWLNamedIndividual());
				if(!checkConsistency()){
					break;
				}
			}
			if(mon.isCancelled()){
				break;
			}
			if(!checkConsistency()){
				break;
			}
			if(useLinkedData){
				for(OWLClass cls : ontology.getClassesInSignature()){
					if(mon.isCancelled()){
						break;
					}
					if(!linkedDataNamespaces.isEmpty()){
						for(String namespace : linkedDataNamespaces){
							if(cls.toStringID().startsWith(namespace)){
								if(visitedLinkedDataResources.contains(cls)){
									continue;
								}
								manager.addAxioms(ontology, getAxiomsFromLinkedDataSource(cls.getIRI()));
								mon.setProgress(ontology.getLogicalAxiomCount());
								visitedLinkedDataResources.add(cls);
								break;
							}
						}
					} else {
						if(cls.toStringID().contains("#")){
							if(visitedLinkedDataResources.contains(cls)){
								continue;
							}
							manager.addAxioms(ontology, getAxiomsFromLinkedDataSource(cls.getIRI()));
							mon.setProgress(ontology.getLogicalAxiomCount());
							visitedLinkedDataResources.add(cls);
						}
					}
					
				}
				if(mon.isCancelled()){
					break;
				}
				if(!checkConsistency()){
					break;
				}
				for(OWLObjectProperty prop : ontology.getObjectPropertiesInSignature()){
					if(mon.isCancelled()){
						break;
					}
					if(!linkedDataNamespaces.isEmpty()){
						for(String namespace : linkedDataNamespaces){
							if(prop.toStringID().startsWith(namespace)){
								if(visitedLinkedDataResources.contains(prop)){
									continue;
								}
								manager.addAxioms(ontology, getAxiomsFromLinkedDataSource(prop.getIRI()));
								mon.setProgress(ontology.getLogicalAxiomCount());
								visitedLinkedDataResources.add(prop);
								break;
							}
						}
					} else {
						if(prop.toStringID().contains("#")){
							if(visitedLinkedDataResources.contains(prop)){
								continue;
							}
							manager.addAxioms(ontology, getAxiomsFromLinkedDataSource(prop.getIRI()));
							mon.setProgress(ontology.getLogicalAxiomCount());
							visitedLinkedDataResources.add(prop);
						}
					}
				}
				if(mon.isCancelled()){
					break;
				}
				if(!checkConsistency()){
					break;
				}
				for(OWLDataProperty prop : ontology.getDataPropertiesInSignature()){
					if(mon.isCancelled()){
						break;
					}
					if(!linkedDataNamespaces.isEmpty()){
						for(String namespace : linkedDataNamespaces){
							if(prop.toStringID().startsWith(namespace)){
								if(visitedLinkedDataResources.contains(prop)){
									continue;
								}
								manager.addAxioms(ontology, getAxiomsFromLinkedDataSource(prop.getIRI()));
								mon.setProgress(ontology.getLogicalAxiomCount());
								visitedLinkedDataResources.add(prop);
								break;
							}
						}
					} else {
						if(prop.toStringID().contains("#")){
							if(visitedLinkedDataResources.contains(prop)){
								continue;
							}
							manager.addAxioms(ontology, getAxiomsFromLinkedDataSource(prop.getIRI()));
							mon.setProgress(ontology.getLogicalAxiomCount());
							visitedLinkedDataResources.add(prop);
						}
					}
				}
				if(mon.isCancelled()){
					break;
				}
				if(!checkConsistency()){
					break;
				}
			}
			
			
			disjointAxioms.clear();
			domainAxioms.clear();
			rangeAxioms.clear();
			subClassOfAxioms.clear();
			classAssertionAxioms.clear();
			
			i++;
		}
		
		overallMonitor.stop();
		//show some statistics
		showStats();
		
		logger.info("Ontology is consistent: " + reasoner.isConsistent());
	
	}
	
	public void runAsync(){
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				IncrementalInconsistencyFinder.this.run();
			}
		});
		t.start();
	}
	
	private boolean checkConsistency(){
		logger.trace("Checking consistency...");
		reasonerMonitor.start();
		consistent = reasoner.isConsistent();
		reasonerMonitor.stop();
		logger.trace("done in " + reasonerMonitor.getLastValue() + "ms.");
		logger.trace("Ontology is " + (consistent ? "consistent" : "inconsistent"));
		return consistent;
	}
	
	public boolean isConsistent(){
		return consistent;
	}
	
	public OWLOntology getOntology(){
		return ontology;
	}
	
	public OWLReasoner getOWLReasoner(){
		return reasoner;
	}
	
	public void setUseLinkedData(boolean useLinkedData){
		this.useLinkedData = useLinkedData;
	}
	
	public void setUseCache(boolean useCache){
		this.useCache = useCache;
	}
	
	public void setLinkedDataNamespaces(Set<String> namespaces){
		this.linkedDataNamespaces = namespaces;
	}
	
	public void dispose(){
		reasoner.dispose();
	}
	
	public void stop(){
		stop = true;
		if(currentQuery != null){
			currentQuery.abort();
		}
	}
	
	private boolean canCountAxioms() {
		boolean canCount = true;
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpoint.getURL().toString(),
				"SELECT COUNT(*) WHERE {?s <http://test> ?o}");
		for(String defaultGraphURI : endpoint.getDefaultGraphURIs()){
			sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		}
		
		try {
			sparqlQueryExec.execSelect();
		} catch (QueryExceptionHTTP e) {
			canCount = false;
		}
		return canCount;
	}
	
	private int getAxiomCountForPredicate(Property predicate){
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT COUNT(*) WHERE {");
		sb.append("?subject ").append("<").append(predicate).append(">").append(" ?object.");
//		sb.append("FILTER ").append("(").append(DBPEDIA_SUBJECT_FILTER).append(")");
		sb.append("}");
		
		ResultSet rs = executeSelect(sb.toString());
		QuerySolution solution = rs.next();
		return solution.getLiteral(solution.varNames().next().toString()).getInt();
//		return sparqlResults.nextSolution().getLiteral("?callret-0").getInt();
	}
	
	private int getAxiomCountForObject(Resource resource){
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT COUNT(*) WHERE {");
		sb.append("?subject ").append("?predicate ").append("<").append(resource).append(">.");
		sb.append("FILTER ").append("(").append(DBPEDIA_SUBJECT_FILTER).append(")");
		sb.append("}");
		
		ResultSet rs = executeSelect(sb.toString());
		QuerySolution solution = rs.next();
		return solution.getLiteral(solution.varNames().next().toString()).getInt();
//		return sparqlResults.nextSolution().getLiteral("?callret-0").getInt();
	}
	
	private Set<OWLAxiom> retrieveClassExpressionsAxioms(Property property, int limit, int offset){
		logger.trace("Retrieving " + property + " axioms");
		mon.setMessage("Retrieving " + property.getLocalName() + " axioms");
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(property).append(">").append(" ?object.");
//		sb.append("FILTER ").append("(").append(DBPEDIA_SUBJECT_FILTER).append(")");
//		sb.append("FILTER ").append("(").append(OWL_SUBJECT_FILTER).append(")");
		sb.append("}");
//		sb.append(" ORDER BY ").append("?subject");
		sb.append(" LIMIT ").append(limit);
		sb.append(" OFFSET ").append(offset);
		
		ResultSet rs = executeSelect(sb.toString());
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodeObject;
		OWLClass cls1;
		OWLClass cls2;
		while(rs.hasNext()){
			solution = rs.next();
			
			rdfNodeSubject = solution.getResource("?subject");
			rdfNodeObject = solution.getResource("?object");
			if(rdfNodeObject == null || rdfNodeSubject == null){
				continue;
			}
			//skip if solution contains blank node
			if(rdfNodeSubject.isAnon() || rdfNodeObject.isAnon()){
				continue;
			}
			
			cls1 = factory.getOWLClass(IRI.create(rdfNodeSubject.toString()));
			cls2 = factory.getOWLClass(IRI.create(rdfNodeObject.toString()));
			
			if(property.equals(RDFS.subClassOf)){
				axioms.add(factory.getOWLSubClassOfAxiom(cls1, cls2));
			} else if(property.equals(OWL.disjointWith)){
				axioms.add(factory.getOWLDisjointClassesAxiom(cls1, cls2));
			} else if(property.equals(OWL.equivalentClass)){
				axioms.add(factory.getOWLEquivalentClassesAxiom(cls1, cls2));
			}
			
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrievePropertyAxioms(Property property, int limit, int offset){
		logger.trace("Retrieving " + property + " axioms");
		mon.setMessage("Retrieving " + property.getLocalName() + " axioms");
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(property).append(">").append(" ?object.");
		sb.append("FILTER ").append("(").append(DBPEDIA_SUBJECT_FILTER).append(")");
		sb.append("FILTER ").append("(").append(OWL_SUBJECT_FILTER).append(")");
		sb.append("}");
//		sb.append(" ORDER BY ").append("?subject");
		sb.append(" LIMIT ").append(limit);
		sb.append(" OFFSET ").append(offset);
		
		ResultSet rs = executeSelect(sb.toString());
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodeObject;
		boolean isObjectProperty = true;
		while(rs.hasNext() && !mon.isCancelled()){
			solution = rs.next();
			
			rdfNodeSubject = solution.getResource("?subject");
			rdfNodeObject = solution.getResource("?object");
			if(rdfNodeObject == null || rdfNodeSubject == null){
				continue;
			}
			//skip if solution contains blank node
			if(rdfNodeSubject.isAnon() || rdfNodeObject.isAnon()){
				continue;
			}
			
			if(property.equals(OWL.inverseOf)){
				axioms.add(factory.getOWLInverseObjectPropertiesAxiom(
						factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString())),
						factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
			} else	if(property.equals(OWL.equivalentProperty)){
				isObjectProperty = isObjectProperty(rdfNodeSubject.toString());
				if(isObjectProperty){
					axioms.add(factory.getOWLEquivalentObjectPropertiesAxiom(
							factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
				} else {
					axioms.add(factory.getOWLEquivalentDataPropertiesAxiom(
							factory.getOWLDataProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLDataProperty(IRI.create(rdfNodeObject.toString()))));
				}
			} else if(property.equals(RDFS.subPropertyOf)){
				isObjectProperty = isObjectProperty(rdfNodeSubject.toString());
				if(isObjectProperty){
					axioms.add(factory.getOWLSubObjectPropertyOfAxiom(
							factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
				} else {
					axioms.add(factory.getOWLSubDataPropertyOfAxiom(
							factory.getOWLDataProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLDataProperty(IRI.create(rdfNodeObject.toString()))));
				}
			} else if(property.equals(RDFS.domain)){
				isObjectProperty = isObjectProperty(rdfNodeSubject.toString());
				if(isObjectProperty){
					axioms.add(factory.getOWLObjectPropertyDomainAxiom(
							factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
				} else {
					axioms.add(factory.getOWLDataPropertyDomainAxiom(
							factory.getOWLDataProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
				}
			} else if(property.equals(RDFS.range)){
				isObjectProperty = isObjectProperty(rdfNodeSubject.toString());
				if(isObjectProperty){
					axioms.add(factory.getOWLObjectPropertyRangeAxiom(
							factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
				} else {
					axioms.add(factory.getOWLDataPropertyRangeAxiom(
							factory.getOWLDataProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLDatatype(IRI.create(rdfNodeObject.toString()))));
				}
			}
			
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrievePropertyCharacteristicAxioms(Resource characteristic, int limit, int offset){
		logger.trace("Retrieving " + characteristic + " axioms");
		mon.setMessage("Retrieving " + characteristic.getLocalName() + " axioms");
		queryMonitor.start();
		
		String queryString = String.format("SELECT * WHERE {?prop a <%s>. ?prop a ?type.} LIMIT %d OFFSET %d", characteristic.toString(), limit, offset);
		
		ResultSet rs = executeSelect(queryString);
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		String propURI;
		String type;
		while(rs.hasNext() && !mon.isCancelled()){
			solution = rs.nextSolution();
			
			propURI = solution.getResource("prop").getURI();
			type = solution.getResource("type").getURI();
			
			if(type.equals(OWL.ObjectProperty.getURI())){
				OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(propURI));
				if(characteristic.equals(OWL.FunctionalProperty)){
					axioms.add(factory.getOWLFunctionalObjectPropertyAxiom(prop));
				} else if(characteristic.equals(OWL.TransitiveProperty)){
					axioms.add(factory.getOWLTransitiveObjectPropertyAxiom(prop));
				} else if(characteristic.equals(OWL.InverseFunctionalProperty)){
					axioms.add(factory.getOWLInverseFunctionalObjectPropertyAxiom(prop));
				}
			} else if(type.equals(OWL.DatatypeProperty.getURI())){
				OWLDataProperty prop = factory.getOWLDataProperty(IRI.create(propURI));
				if(characteristic.equals(OWL.FunctionalProperty)){
					axioms.add(factory.getOWLFunctionalDataPropertyAxiom(prop));
				} 
			}
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	
	private boolean isObjectProperty(String propertyURI){
		if(ontology.getObjectPropertiesInSignature().contains(factory.getOWLObjectProperty(IRI.create(propertyURI)))){
			return true;
		}
		if(ontology.getDataPropertiesInSignature().contains(factory.getOWLDataProperty(IRI.create(propertyURI)))){
			return false;
		}
		logger.trace("Checking if property " + propertyURI + " is ObjectProperty");
		queryMonitor.start();
		
		//first check if triple ($prop rdf:type owl:ObjectProperty) is in the knowledgebase
		StringBuilder sb = new StringBuilder();
		sb.append("ASK {");
		sb.append("<").append(propertyURI).append("> ").append("<").append(RDF.type).append("> ").append("<").append(OWL.ObjectProperty).append(">");
		sb.append("}");
		boolean isObjectProperty = executeAsk(sb.toString());
		if(isObjectProperty){
			logger.trace("YES.");
			return true;
		}
		
		//we check if triple ($prop rdf:type owl:DataProperty) is in the knowledgebase
		sb = new StringBuilder();
		sb.append("ASK {");
		sb.append("<").append(propertyURI).append("> ").append("<").append(RDF.type).append("> ").append("<").append(OWL.DatatypeProperty).append(">");
		sb.append("}");
		boolean isDataProperty = executeAsk(sb.toString());
		if(isDataProperty){
			logger.trace("NO.");
			return false;
		}
		
		//we check for one triple, and decide if object is an literal
		sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(propertyURI).append("> ").append("?object");
		sb.append("}");
		sb.append("LIMIT 1");
		ResultSet rs = executeSelect(sb.toString());
		
		isObjectProperty = true;
		while(rs.hasNext()){
			QuerySolution solution = rs.next();
			
			RDFNode rdfNodeSubject = solution.get("?object");
			isObjectProperty = !rdfNodeSubject.isLiteral();
		}
		queryMonitor.stop();
		logger.trace((isObjectProperty ? "YES" : "NO"));
		return isObjectProperty;
	}
	
	private Set<OWLClassAssertionAxiom> retrieveClassAssertionAxiomsForClasses(OWLClass cls1, OWLClass cls2, int limit){
		logger.trace("Retrieving ClassAssertion axioms for class " + cls1 + " and " + cls2);
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(RDF.type).append("> ").append("<").append(cls1.toStringID()).append(">.");
		sb.append("?subject ").append("<").append(RDF.type).append("> ").append("<").append(cls2.toStringID()).append(">.");
		sb.append("}");
		sb.append(" LIMIT ").append(limit);
		
		ResultSet rs = executeSelect(sb.toString());
		
		Set<OWLClassAssertionAxiom> axioms = new HashSet<OWLClassAssertionAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		OWLNamedIndividual individual;
		while(rs.hasNext()){
			solution = rs.next();
			
			rdfNodeSubject = solution.getResource("?subject");
			
			individual = factory.getOWLNamedIndividual(IRI.create(rdfNodeSubject.toString()));
			
			axioms.add(factory.getOWLClassAssertionAxiom(cls1, individual));
			axioms.add(factory.getOWLClassAssertionAxiom(cls2, individual));
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrieveObjectPropertyAssertionAxioms(OWLObjectProperty prop, int limit){
		logger.trace("Retrieving ObjectPropertyAssertion axioms for property " + prop);
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(prop.toStringID()).append("> ").append("?object");
		sb.append("}");
		sb.append(" LIMIT ").append(limit);
		
		ResultSet rs = executeSelect(sb.toString());
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodeObject;
		while(rs.hasNext()){
			solution = rs.nextSolution();
			if(solution.get("?object").isLiteral()){
				continue;
			}
			
			rdfNodeSubject = solution.getResource("?subject");
			rdfNodeObject = solution.getResource("?object");
			
			axioms.add(factory.getOWLObjectPropertyAssertionAxiom(
					prop,
					factory.getOWLNamedIndividual(IRI.create(rdfNodeSubject.toString())),
					factory.getOWLNamedIndividual(IRI.create(rdfNodeObject.toString()))));
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrieveDataPropertyAssertionAxioms(OWLDataProperty prop, int limit){
		logger.trace("Retrieving DataPropertyAssertion axioms for property " + prop);
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(prop.toStringID()).append("> ").append("?object");
		sb.append("}");
		sb.append(" LIMIT ").append(limit);
		
		ResultSet rs = executeSelect(sb.toString());
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		OWLLiteral literal = null;
		while(rs.hasNext()){
			solution = rs.next();
			if(solution.get("?object").isResource()){
				continue;
			}
			
			rdfNodeSubject = solution.getResource("?subject");
			
			literal = getOWLLiteral2(solution.getLiteral("?object"));
			
			axioms.add(factory.getOWLDataPropertyAssertionAxiom(
					prop,
					factory.getOWLNamedIndividual(IRI.create(rdfNodeSubject.toString())),
					literal));
			
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrieveAxiomsViolatingFunctionality(OWLObjectProperty prop, int limit){
		logger.trace("Checking functionality violation of object property " + prop);
		String queryString = "SELECT * WHERE {?s <%s> ?o1. ?s <%s> ?o2. FILTER(?o1 != ?o2) } LIMIT %d";
		queryString.replaceAll("%s", prop.toStringID());
		queryString.replace("%d", String.valueOf(limit));
		
		ResultSet rs = executeSelect(queryString);
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		OWLNamedIndividual subject;
		OWLNamedIndividual object1;
		OWLNamedIndividual object2;
		while(rs.hasNext()){
			solution = rs.nextSolution();
			
			subject = factory.getOWLNamedIndividual(IRI.create(solution.getResource("?s").getURI()));
			object1 = factory.getOWLNamedIndividual(IRI.create(solution.getResource("?o1").getURI()));
			object2 = factory.getOWLNamedIndividual(IRI.create(solution.getResource("?o2").getURI()));
			
			axioms.add(factory.getOWLObjectPropertyAssertionAxiom(prop, subject, object1));
			axioms.add(factory.getOWLObjectPropertyAssertionAxiom(prop, subject, object2));
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrieveAxiomsViolatingFunctionality(OWLDataProperty prop, int limit){
		logger.trace("Checking functionality violation of data property " + prop);
		String queryString = String.format("SELECT * WHERE {?s <%s> ?o1. ?s <%s> ?o2. FILTER(?o1 != ?o2) } LIMIT 1000",
				prop.toStringID(), prop.toStringID(), limit);
		queryString.replaceAll("%s", prop.toStringID());
		queryString.replace("%d", String.valueOf(limit));
		
		ResultSet rs = executeSelect(queryString);
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		OWLNamedIndividual subject;
		OWLLiteral object1;
		OWLLiteral object2;
		while(rs.hasNext()){
			solution = rs.next();
			
			subject = factory.getOWLNamedIndividual(IRI.create(solution.getResource("s").getURI()));
			object1 = getOWLLiteral2(solution.getLiteral("o1"));
			object2 = getOWLLiteral2(solution.getLiteral("o2"));
			
			axioms.add(factory.getOWLDataPropertyAssertionAxiom(prop, subject, object1));
			axioms.add(factory.getOWLDataPropertyAssertionAxiom(prop, subject, object2));
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrieveAxiomsViolatingInverseFunctionality(OWLDataProperty prop, int limit){
		logger.trace("Checking inverse functionality violation of data property " + prop);
		String queryString = String.format("SELECT * WHERE {?s1 <%s> ?o. ?s2 <%s> ?o. FILTER(?s1 != ?s2) } LIMIT 1000",
				prop.toStringID(), prop.toStringID(), limit);
		queryString.replaceAll("%s", prop.toStringID());
		queryString.replace("%d", String.valueOf(limit));
		
		ResultSet rs = executeSelect(queryString);
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		OWLNamedIndividual subject1;
		OWLNamedIndividual subject2;
		OWLLiteral object;
		while(rs.hasNext()){
			solution = rs.next();
			
			subject1 = factory.getOWLNamedIndividual(IRI.create(solution.getResource("s1").getURI()));
			subject2 = factory.getOWLNamedIndividual(IRI.create(solution.getResource("s2").getURI()));
			object = getOWLLiteral2(solution.getLiteral("o"));
			
			axioms.add(factory.getOWLDataPropertyAssertionAxiom(prop, subject1, object));
			axioms.add(factory.getOWLDataPropertyAssertionAxiom(prop, subject2, object));
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrieveAxiomsViolatingInverseFunctionality(OWLObjectProperty prop, int limit){
		logger.trace("Checking inverse functionality violation of data property " + prop);
		String queryString = String.format("SELECT * WHERE {?s1 <%s> ?o. ?s2 <%s> ?o. FILTER(?s1 != ?s2) } LIMIT 1000",
				prop.toStringID(), prop.toStringID(), limit);
		queryString.replaceAll("%s", prop.toStringID());
		queryString.replace("%d", String.valueOf(limit));
		
		ResultSet rs = executeSelect(queryString);
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		OWLNamedIndividual subject1;
		OWLNamedIndividual subject2;
		OWLNamedIndividual object;
		while(rs.hasNext()){
			solution = rs.next();
			
			subject1 = factory.getOWLNamedIndividual(IRI.create(solution.getResource("s1").getURI()));
			subject2 = factory.getOWLNamedIndividual(IRI.create(solution.getResource("s2").getURI()));
			object = factory.getOWLNamedIndividual(IRI.create(solution.getResource("o").getURI()));
			
			axioms.add(factory.getOWLObjectPropertyAssertionAxiom(prop, subject1, object));
			axioms.add(factory.getOWLObjectPropertyAssertionAxiom(prop, subject2, object));
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private OWLLiteral getOWLLiteral(Literal lit){
		OWLLiteral literal = null;
		if(lit.getDatatypeURI() != null){
			OWLDatatype datatype = factory.getOWLDatatype(IRI.create(lit.getDatatypeURI()));
			literal = factory.getOWLLiteral(lit.getLexicalForm(), datatype);
		} else {
			if(lit.getLanguage() != null){
				literal = factory.getOWLLiteral(lit.getLexicalForm(), lit.getLanguage());
			} else {
				literal = factory.getOWLLiteral(lit.getLexicalForm());
			}
		}
		return literal;
	}
	
	private OWLLiteral getOWLLiteral2(Literal lit){
		OWLLiteral literal = null;
		if(lit.getDatatypeURI() != null){
			IRI datatypeIRI = IRI.create(lit.getDatatypeURI());
			if(OWL2Datatype.isBuiltIn(datatypeIRI)){
				OWL2Datatype datatype = OWL2Datatype.getDatatype(datatypeIRI);
				literal = factory.getOWLLiteral(lit.getLexicalForm(), datatype);
			} else {
				literal = factory.getOWLLiteral(lit.getLexicalForm(), OWL2Datatype.RDF_PLAIN_LITERAL);
			}
		} else {
			if(lit.getLanguage() != null){
				literal = factory.getOWLLiteral(lit.getLexicalForm(), lit.getLanguage());
			} else {
				literal = factory.getOWLLiteral(lit.getLexicalForm());
			}
		}
		return literal;
	}
	
	private Set<OWLClassAssertionAxiom> retrieveClassAssertionAxiomsForClass(OWLClass cls, int limit){
		logger.trace("Retrieving ClassAssertion axioms for class " + cls);
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(RDF.type).append("> ").append("<").append(cls.toStringID()).append(">.");
		sb.append("}");
//		sb.append(" ORDER BY ").append("?subject");
		sb.append(" LIMIT ").append(limit);
		
		ResultSet rs = executeSelect(sb.toString());
		
		Set<OWLClassAssertionAxiom> axioms = new HashSet<OWLClassAssertionAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		OWLNamedIndividual individual;
		while(rs.hasNext()){
			solution = rs.next();
			
			rdfNodeSubject = solution.getResource("?subject");
			
			individual = factory.getOWLNamedIndividual(IRI.create(rdfNodeSubject.toString()));
			
			axioms.add(factory.getOWLClassAssertionAxiom(cls, individual));
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	@SuppressWarnings("unused")
	private Set<OWLClassAssertionAxiom> retrievePropertyAssertionAxiomsWithFunctionalProperty(int limit){
		queryMonitor.start();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ?s, ?p, COUNT(?o) WHERE {");
		sb.append("?p").append("<").append(RDF.type).append("> ").append("<").append(OWL.FunctionalProperty).append(">.");
		sb.append("?s ?p ?o.");
		sb.append("}");
		sb.append(" HAVING COUNT(?o)>1"); 
		sb.append(" LIMIT ").append(limit);
		
		ResultSet rs = executeSelect(sb.toString());
		
		Set<OWLClassAssertionAxiom> axioms = new HashSet<OWLClassAssertionAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodePredicate;
		while(rs.hasNext()){
			solution = rs.nextSolution();
			
			rdfNodeSubject = solution.getResource("?s");
			rdfNodePredicate = solution.getResource("?p");
			
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	
	
	/**
	 * Get axioms for a given ObjectProperty.
	 * Axiom types: TransitiveProperty, FunctionalProperty, InverseFunctionalProperty, SymmetricProperty, EquivalentProperty,
	 * SubProperty, InverseOf, Domain, Range
	 * 
	 * @param prop
	 * @return
	 */
	@SuppressWarnings("unused")
	private Set<OWLAxiom> retrieveAxiomsForObjectProperty(OWLObjectProperty prop, int limit){
		logger.trace("Retrieving axioms for property " + prop);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("<").append(prop.toStringID()).append("> ").append("?predicate").append(" ?object.}");
		sb.append("LIMIT ").append(limit);
		
		ResultSet rs = executeSelect(sb.toString());
		QuerySolution solution;
		RDFNode rdfNodePredicate;
		RDFNode rdfNodeObject;
		while(rs.hasNext()){
			solution = rs.next();
			rdfNodePredicate = solution.getResource("?predicate");
			rdfNodeObject = solution.get("?object");
			//skip if object is a blank node
			if(rdfNodeObject.isAnon()){
				continue;
			}
			if(rdfNodePredicate.equals(RDF.type)){
				if(rdfNodeObject.equals(OWL.TransitiveProperty)){
					axioms.add(factory.getOWLTransitiveObjectPropertyAxiom(prop));
				} else if(rdfNodeObject.equals(OWL.FunctionalProperty)){
					axioms.add(factory.getOWLFunctionalObjectPropertyAxiom(prop));
				} else if(rdfNodeObject.equals(OWL.InverseFunctionalProperty)){
					axioms.add(factory.getOWLInverseFunctionalObjectPropertyAxiom(prop));
				} else if(rdfNodeObject.equals(OWL.SymmetricProperty)){
					axioms.add(factory.getOWLSymmetricObjectPropertyAxiom(prop));
				}
			} else if(rdfNodePredicate.equals(RDFS.subPropertyOf)){
				axioms.add(factory.getOWLSubObjectPropertyOfAxiom(prop,
						factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(OWL.inverseOf)){
				axioms.add(factory.getOWLInverseObjectPropertiesAxiom(prop,
						factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(RDFS.domain)){
				axioms.add(factory.getOWLObjectPropertyDomainAxiom(prop,
						factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(RDFS.range)){
				axioms.add(factory.getOWLObjectPropertyRangeAxiom(prop,
						factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(OWL.equivalentProperty)){
				axioms.add(factory.getOWLEquivalentObjectPropertiesAxiom(prop,
						factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
			}
			
		}
		if(axioms.isEmpty()){
			axioms.addAll(getAxiomsFromLinkedDataSource(prop.getIRI()));
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}

	/**
	 * Retrieve axioms for a given individual.
	 * Axiom types: SameAs, DifferentFrom, ClassAssertion, ObjectPropertyAssertion, DataPropertyAssertion
	 * @param ind
	 * @return
	 */
	private Set<OWLAxiom> retrieveAxiomsForIndividual(OWLNamedIndividual ind, int limit){
		logger.trace("Retrieving axioms for individual " + ind);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("<").append(ind.toStringID()).append("> ").append("?predicate").append(" ?object.");
		sb.append("FILTER ").append("(").append(DBPEDIA_PREDICATE_FILTER).append(")");
//		sb.append("FILTER ").append("(").append(OWL_OBJECT_FILTER).append(")");
		sb.append("}");
		sb.append(" LIMIT ").append(limit);
		
		ResultSet rs = executeSelect(sb.toString());
		
		QuerySolution solution;
		RDFNode rdfNodePredicate;
		RDFNode rdfNodeObject;
		OWLLiteral literal;
		RDFDatatype datatype;
		while(rs.hasNext()){
			if(mon.isCancelled()){
				break;
			}
			solution = rs.nextSolution();
			rdfNodePredicate = solution.getResource("?predicate");
			rdfNodeObject = solution.get("?object");
			
			//skip if object is a blank node
			if(rdfNodeObject.isAnon()){
				continue;
			}
			if(rdfNodePredicate.equals(RDF.type)){
				axioms.add(factory.getOWLClassAssertionAxiom(factory.getOWLClass(IRI.create(rdfNodeObject.toString())), ind));
			} else if(rdfNodePredicate.equals(OWL.sameAs)){
				axioms.add(factory.getOWLSameIndividualAxiom(ind, factory.getOWLNamedIndividual(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(OWL.differentFrom)){
				axioms.add(factory.getOWLDifferentIndividualsAxiom(ind, factory.getOWLNamedIndividual(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(RDFS.comment)){
				continue;
			} else if(rdfNodePredicate.equals(RDFS.label)){
				continue;
			} else {
				// we check if property is a DataProperty or ObjectProperty
				if(isObjectProperty(rdfNodePredicate.toString())){
					// for ObjectProperties we skip assertions with literals
					if(rdfNodeObject.isLiteral()){
						continue;
					}
					axioms.add(factory.getOWLObjectPropertyAssertionAxiom(
							factory.getOWLObjectProperty(IRI.create(rdfNodePredicate.toString())),
							ind,
							factory.getOWLNamedIndividual(IRI.create(rdfNodeObject.toString()))));
				} else {
					// for DataProperties we skip assertions with resources
					if(!rdfNodeObject.isLiteral()){
						continue;
					}
					axioms.add(getOWLDataPropertyAssertionAxiom(ind, rdfNodePredicate.asResource(), rdfNodeObject.asLiteral()));
				}
				
			}
				
				
			
		}
		if(axioms.isEmpty()){
			axioms.addAll(getAxiomsFromLinkedDataSource(ind.getIRI()));
		}
		queryMonitor.stop();
		logger.trace("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private OWLDataPropertyAssertionAxiom getOWLDataPropertyAssertionAxiom(OWLNamedIndividual subject, Resource predicate, Literal object){
		OWLLiteral literal = getOWLLiteral2(object);
		
		OWLDataProperty property = factory.getOWLDataProperty(IRI.create(predicate.getURI()));
		
		return factory.getOWLDataPropertyAssertionAxiom(property, subject, literal);
	}
	
	
	private Set<OWLAxiom> getAxiomsFromLinkedDataSource(IRI iri){
		logger.trace("Trying to get informations from linked data uri " + iri);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		try{
			OWLObjectProperty prop;
			for(OWLAxiom ax : manager.loadOntology(iri).getLogicalAxioms()){
				if(ax instanceof OWLObjectPropertyDomainAxiom){
					prop = ((OWLObjectPropertyDomainAxiom)ax).getProperty().asOWLObjectProperty();
					if(!isObjectProperty(prop.toStringID())){
						axioms.add(factory.getOWLDataPropertyDomainAxiom(
								factory.getOWLDataProperty(IRI.create(prop.toStringID())),
								((OWLObjectPropertyDomainAxiom)ax).getDomain()));
					} else {
						axioms.add(ax);
					}
				} else {
					axioms.add(ax);
				}
			}
		} catch (Exception e){
			logger.error(e);
			logger.info("No linked data retrieved.");
		}
		logger.trace("Got " + axioms.size() + " logical axioms from linked data source");
		return axioms;
	}
	
	@SuppressWarnings("unused")
	private String getLabel(String uri){
		String label = "";
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("<").append(uri).append("> ").append("<").append(RDFS.label).append("> ").append("?label");
		sb.append("}");
		sb.append(" LIMIT 1");
		
		ResultSet rs = executeSelect(sb.toString());
		
		while(rs.hasNext()){
			label = rs.next().get("?label").toString();
		}
		return label;
	}
	
	
	
	private void showStats(){
		logger.info("###########################STATS###########################");
		logger.info(ontology);
		logger.info("Overall execution time: " + overallMonitor.getTotal() + " ms");
		logger.info("Overall query time: " + queryMonitor.getTotal() + " ms (" + (int)(queryMonitor.getTotal()/overallMonitor.getTotal()*100) + "%)");
		logger.info("Number of queries sent: " + (int)queryMonitor.getHits());
		logger.info("Average query time: " + queryMonitor.getAvg() + " ms");
		logger.info("Longest query time: " + queryMonitor.getMax() + " ms");
		logger.info("Shortest query time: " + queryMonitor.getMin() + " ms");
		logger.info("Overall reasoning time: " + reasonerMonitor.getTotal() + " ms (" + (int)(reasonerMonitor.getTotal()/overallMonitor.getTotal()*100) + "%)");
		logger.info("Number of reasoner calls: " + (int)reasonerMonitor.getHits());
		logger.info("Average reasoning time: " + reasonerMonitor.getAvg() + " ms");
		logger.info("Longest reasoning time: " + reasonerMonitor.getMax() + " ms");
		logger.info("Shortest reasoning time: " + reasonerMonitor.getMin() + " ms");
	}
	
	
	@SuppressWarnings("unused")
	private Query createSimpleSelectSPARQLQuery(String subject, String predicate, String object, String filter, int limit){
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT * WHERE {");
		if(!subject.startsWith("?")){
			sb.append("<").append(subject).append(">");
		} else {
			sb.append(subject);
		}
		sb.append(" ");
		if(!predicate.startsWith("?")){
			sb.append("<").append(predicate).append(">");
		} else {
			sb.append(predicate);
		}
		sb.append(" ");
		if(!object.startsWith("?")){
			sb.append("<").append(object).append(">");
		} else {
			sb.append(object);
		}
//		if(filter != null){
//			sb.append(" FILTER ");
//			sb.append(filter);
//		}
		sb.append("}");
		sb.append(" LIMIT ");
		sb.append(limit);
		Query query = QueryFactory.create(sb.toString());
		return query;
	}
	
	private ResultSet executeSelect(String queryString, int limit, int offset){
		logger.trace("Sending query\n" + queryString);
		long startTime = System.currentTimeMillis();
		ResultSet rs = null;
			if(limit > 0){
				queryString += " LIMIT " + limit;
			}
			if(offset > 0){
				queryString += " OFFSET " + offset;
			}
			QueryExecutionFactory f = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
			if(useCache){
				f = new QueryExecutionFactoryCacheEx(f, CacheUtilsH2.createCacheFrontend(cacheDir, true, TimeUnit.DAYS.toMillis(7)));
			}
			currentQuery = f.createQueryExecution(queryString);
			rs = currentQuery.execSelect();
			currentQuery.close();
			logger.trace("... done in " + (System.currentTimeMillis() - startTime) + "ms");
		return rs;
	}
	
	private ResultSet executeSelect(String queryString){
		return executeSelect(queryString, 0, 0);
	}
	
	private boolean executeAsk(String queryString){
		logger.trace("Sending query\n" + queryString);
		long startTime = System.currentTimeMillis();
		boolean ret = false;
			QueryExecutionFactory f = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
			if(useCache){
				f = new QueryExecutionFactoryCacheEx(f, CacheUtilsH2.createCacheFrontend(cacheDir, true, TimeUnit.DAYS.toMillis(7)));
			}
			currentQuery = f.createQueryExecution(queryString);
			ret = currentQuery.execAsk();
			currentQuery.close();
			logger.trace("... done in " + (System.currentTimeMillis() - startTime) + "ms");
		return ret;
	}

	public int getDisjointWithCount() {
		return disjointWithCount;
	}

	public int getFunctionalCount() {
		return functionalCount;
	}

	public int getInverseFunctionalCount() {
		return inverseFunctionalCount;
	}

	public int getEquivalentClassCount() {
		return equivalentClassCount;
	}

	public int getSubClassOfCount() {
		return subClassOfCount;
	}

	public int getDomainCount() {
		return domainCount;
	}

	public int getRangeCount() {
		return rangeCount;
	}

	public int getSubPropertyOfCount() {
		return subPropertyOfCount;
	}

	public int getEquivalentPropertyCount() {
		return equivalentPropertyCount;
	}

	public int getInverseOfCount() {
		return inverseOfCount;
	}

	public int getTransitiveCount() {
		return transitiveCount;
	}

	public int getClassAssertionCount() {
		return classAssertionCount;
	}

	public int getOverallAxiomCount() {
		return overallAxiomCount;
	}
	
	public static void main(String[] args) throws OWLOntologyCreationException, IOException{
		SimpleLayout layout = new SimpleLayout();
		ConsoleAppender consoleAppender = new ConsoleAppender(layout);
		Logger logger = Logger.getRootLogger();
		logger.removeAllAppenders();
		logger.addAppender(consoleAppender);
		logger.setLevel(Level.INFO);		 
		Logger.getLogger(IncrementalInconsistencyFinder.class).setLevel(Level.INFO);
		
//		SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();
		SparqlEndpoint endpoint = new SparqlEndpoint(new URL("http://factforge.net/sparql"));
		
		IncrementalInconsistencyFinder f = new IncrementalInconsistencyFinder(endpoint, "cache");
		f.setProgressMonitor(new ConsoleSPARQLProgressMonitor());
		f.setUseCache(true);
		f.setUseLinkedData(true);
		f.run();
		OWLOntology ont = f.getOntology();
		PelletExplanationGenerator expGen = new PelletExplanationGenerator(f.getOntology());
		long startTime = System.currentTimeMillis();
		System.out.print("Computing explanation...");
		OWLDataFactory df = OWLManager.getOWLDataFactory();
		Explanation exp = expGen.getExplanation(
				df.getOWLSubClassOfAxiom(
						df.getOWLThing(),
						df.getOWLNothing()));
		System.out.println("done in " + (System.currentTimeMillis()-startTime) + "ms.");
		System.out.println(exp);
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWL2SPARULConverter trans = new OWL2SPARULConverter(ont, false);
		java.util.List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for(OWLAxiom ax : exp.getAxioms()){
			changes.add(new RemoveAxiom(ont, ax));
		}
		System.out.println(trans.translate(changes));
	}
	

}
