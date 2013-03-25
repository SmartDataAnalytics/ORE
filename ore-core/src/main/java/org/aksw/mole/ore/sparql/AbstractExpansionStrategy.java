package org.aksw.mole.ore.sparql;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public abstract class AbstractExpansionStrategy implements ExpansionStrategy{
	
	private static final List<AxiomType> classExpressionAxiomTypes = Arrays.asList(new AxiomType[]{
			AxiomType.SUBCLASS_OF, AxiomType.EQUIVALENT_CLASSES, AxiomType.DISJOINT_CLASSES, AxiomType.DISJOINT_UNION});
	
	private static final List<AxiomType> propertyAxiomTypes = Arrays.asList(new AxiomType[]{
			AxiomType.OBJECT_PROPERTY_DOMAIN, AxiomType.DATA_PROPERTY_DOMAIN, 
			AxiomType.OBJECT_PROPERTY_RANGE, AxiomType.DATA_PROPERTY_RANGE,
			AxiomType.EQUIVALENT_OBJECT_PROPERTIES, AxiomType.EQUIVALENT_DATA_PROPERTIES,
			AxiomType.SUB_OBJECT_PROPERTY, AxiomType.SUB_DATA_PROPERTY,
			AxiomType.INVERSE_OBJECT_PROPERTIES
	});
	
	
	private static final Logger logger = Logger.getLogger(AbstractExpansionStrategy.class.getName());
	
	protected SparqlEndpoint endpoint;
	protected ExtractionDBCache cache;
	protected OWLDataFactory dataFactory = new OWLDataFactoryImpl();
	protected OWLOntologyManager manager;
	protected OWLOntology ontology;
	
	public AbstractExpansionStrategy(SparqlEndpoint endpoint, ExtractionDBCache cache, OWLOntology existingSchema) {
		this.endpoint = endpoint;
		this.cache = cache;
		
		manager = OWLManager.createOWLOntologyManager();
		dataFactory = manager.getOWLDataFactory();
		
		if(existingSchema == null){
			try {
				ontology = manager.createOntology(IRI.create("http://ore.aksw.org/extractedFragement/"));
			} catch (OWLOntologyCreationException e) {
				e.printStackTrace();
			}
		} else {
			this.ontology = existingSchema;
		}
	}
	
	public AbstractExpansionStrategy(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		this(endpoint, cache, null);
	}
	
	public AbstractExpansionStrategy(SparqlEndpoint endpoint) {
		this(endpoint, null);
	}
	
	protected Set<OWLClassAssertionAxiom> retrieveClassAssertionAxioms(OWLClass cls, int limit, int offset){
		logger.trace("Retrieving ClassAssertion axioms for class " + cls);
		
		String query = String.format("SELECT * WHERE {?s a <%s>} LIMIT %d OFFSET %d", cls.toStringID(), limit, offset);
		ResultSet rs = executeSelect(query);
		
		Set<OWLClassAssertionAxiom> axioms = new HashSet<OWLClassAssertionAxiom>();
		
		QuerySolution qs;
		RDFNode subject;
		OWLNamedIndividual individual;
		while(rs.hasNext()){
			qs = rs.next();
			subject = qs.getResource("?s");
			if(subject.isURIResource()){
				individual = dataFactory.getOWLNamedIndividual(IRI.create(subject.toString()));
				axioms.add(dataFactory.getOWLClassAssertionAxiom(cls, individual));
			} else {
				logger.warn("Ignoring triple " + qs);
			}
		}
		logger.trace("Found " + axioms.size() + " axioms.");
		return axioms;
	}
	
	protected Set<OWLObjectPropertyAssertionAxiom> retrieveObjectPropertyAssertionAxioms(OWLObjectProperty property, int limit, int offset){
		logger.trace("Retrieving axioms for property " + property);
		Set<OWLObjectPropertyAssertionAxiom> axioms = new HashSet<OWLObjectPropertyAssertionAxiom>();
		
		String query = String.format("SELECT ?s ?o WHERE {?s <%s> ?o.} LIMIT %d OFFSET %d", property.toStringID(), limit, offset);
		
		ResultSet rs = executeSelect(query);
		QuerySolution qs;
		RDFNode subject;
		RDFNode object;
		OWLObjectPropertyAssertionAxiom axiom;
		while(rs.hasNext()){
			qs = rs.next();
			subject = qs.get("s");
			object = qs.get("o");
			if(subject.isURIResource() && object.isURIResource()){
				axiom = dataFactory.getOWLObjectPropertyAssertionAxiom(
						property, 
						dataFactory.getOWLNamedIndividual(IRI.create(subject.asResource().getURI())), 
						dataFactory.getOWLNamedIndividual(IRI.create(object.asResource().getURI())));
				axioms.add(axiom);
			} else {
				logger.warn("Ignoring triple " + qs);
			}
		}
		logger.trace("Found " + axioms.size() + " axioms.");
		return axioms;
	}
	
	protected Set<OWLDataPropertyAssertionAxiom> retrieveDataPropertyAssertionAxioms(OWLDataProperty property, int limit, int offset){
		logger.trace("Retrieving axioms for property " + property);
		Set<OWLDataPropertyAssertionAxiom> axioms = new HashSet<OWLDataPropertyAssertionAxiom>();
		
		String query = String.format("SELECT ?s ?o WHERE {?s <%s> ?o.} LIMIT %d OFFSET %d", property.toStringID(), limit, offset);
		
		ResultSet rs = executeSelect(query);
		QuerySolution qs;
		RDFNode subject;
		RDFNode object;
		OWLDataPropertyAssertionAxiom axiom;
		while(rs.hasNext()){
			qs = rs.next();
			subject = qs.get("s");
			object = qs.get("o");
			if(subject.isURIResource() && object.isLiteral()){
				axiom = dataFactory.getOWLDataPropertyAssertionAxiom(
						property, 
						dataFactory.getOWLNamedIndividual(IRI.create(subject.asResource().getURI())), 
						getOWLLiteral(object.asLiteral()));
				axioms.add(axiom);
			} else {
				logger.warn("Ignoring triple " + qs);
			}
		}
		logger.trace("Found " + axioms.size() + " axioms.");
		return axioms;
	}
	
	protected Set<OWLAxiom> retrieveClassExpressionsAxioms(AxiomType axiomType, int limit, int offset){
		logger.trace("Retrieving " + axiomType + " axioms");
		
		if(classExpressionAxiomTypes.contains(axiomType)){
			throw new IllegalArgumentException("Axiom type " + axiomType + " is not a class expression axiom type.");
		}
		String predicate = null;
		if(axiomType == AxiomType.SUBCLASS_OF){
			predicate = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
		} else if(axiomType == AxiomType.EQUIVALENT_CLASSES){
			predicate = "http://www.w3.org/2002/07/owl#equivalentClass";
		} else if(axiomType == AxiomType.DISJOINT_CLASSES){
			predicate = "http://www.w3.org/2002/07/owl#disjointWith";
		} else if(axiomType == AxiomType.DISJOINT_UNION){
			predicate = "http://www.w3.org/2002/07/owl#disjointUnionOf";
		}
		String query = String.format("SELECT * WHERE {?s <%s> ?o.}", predicate);
		
		ResultSet rs = executeSelect(query);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution qs;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodeObject;
		OWLClass cls1;
		OWLClass cls2;
		while(rs.hasNext()){
			qs = rs.next();
			
			rdfNodeSubject = qs.getResource("s");
			rdfNodeObject = qs.getResource("o");
			
			//skip if solution contains blank node
			if(rdfNodeSubject.isURIResource() && rdfNodeObject.isURIResource()){
				cls1 = dataFactory.getOWLClass(IRI.create(rdfNodeSubject.toString()));
				cls2 = dataFactory.getOWLClass(IRI.create(rdfNodeObject.toString()));
				
				if(axiomType == AxiomType.SUBCLASS_OF){
					axioms.add(dataFactory.getOWLSubClassOfAxiom(cls1, cls2));
				} else if(axiomType == AxiomType.EQUIVALENT_CLASSES){
					axioms.add(dataFactory.getOWLEquivalentClassesAxiom(cls1, cls2));
				} else if(axiomType == AxiomType.DISJOINT_CLASSES){
					axioms.add(dataFactory.getOWLDisjointClassesAxiom(cls1, cls2));
				} else if(axiomType == AxiomType.DISJOINT_UNION){
					//TODO parse DISJOINT UNION axiom
				}
			} else {
				logger.warn("Ignoring triple " + qs);
			}
		}
		logger.trace("Found " + axioms.size() + " axioms.");
		return axioms;
	}
	
	protected Set<OWLAxiom> retrievePropertyAxioms(AxiomType axiomType, int limit, int offset){
		logger.trace("Retrieving " + axiomType + " axioms");
		
		if(propertyAxiomTypes.contains(axiomType)){
			throw new IllegalArgumentException("Axiom type " + axiomType + " is not a property axiom type.");
		}
		String predicate = null;
		if(axiomType == AxiomType.OBJECT_PROPERTY_DOMAIN || axiomType == AxiomType.DATA_PROPERTY_DOMAIN){
			predicate = "http://www.w3.org/2000/01/rdf-schema#domain";
		} else if(axiomType == AxiomType.OBJECT_PROPERTY_RANGE || axiomType == AxiomType.DATA_PROPERTY_RANGE){
			predicate = "http://www.w3.org/2000/01/rdf-schema#range";
		} else if(axiomType == AxiomType.SUB_OBJECT_PROPERTY || axiomType == AxiomType.SUB_DATA_PROPERTY){
			predicate = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";
		} else if(axiomType == AxiomType.EQUIVALENT_OBJECT_PROPERTIES || axiomType == AxiomType.EQUIVALENT_DATA_PROPERTIES){
			predicate = "http://www.w3.org/2002/07/owl#equivalentProperty";
		} else if(axiomType == AxiomType.INVERSE_OBJECT_PROPERTIES){
			predicate = "http://www.w3.org/2002/07/owl#inverseOf";
		} 
		String query = String.format("SELECT * WHERE {?s <%s> ?o.}", predicate);
		
		ResultSet rs = executeSelect(query);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution qs;
		RDFNode subject;
		RDFNode object;
		
		while(rs.hasNext()){
			qs = rs.next();
			
			subject = qs.getResource("s");
			object = qs.getResource("o");
			
			//skip if solution contains blank node
			if(subject.isURIResource() && object.isURIResource()){
				if(axiomType == AxiomType.OBJECT_PROPERTY_DOMAIN){
					OWLClass domain = dataFactory.getOWLClass(IRI.create(object.toString()));
					OWLObjectProperty property = dataFactory.getOWLObjectProperty(IRI.create(subject.toString()));
					axioms.add(dataFactory.getOWLObjectPropertyDomainAxiom(property, domain));
				} else if(axiomType == AxiomType.OBJECT_PROPERTY_RANGE){
					OWLClass range = dataFactory.getOWLClass(IRI.create(object.toString()));
					OWLObjectProperty property = dataFactory.getOWLObjectProperty(IRI.create(subject.toString()));
					axioms.add(dataFactory.getOWLObjectPropertyRangeAxiom(property, range));
				} else if(axiomType == AxiomType.SUB_OBJECT_PROPERTY){
					OWLObjectProperty subProperty = dataFactory.getOWLObjectProperty(IRI.create(subject.toString()));
					OWLObjectProperty superProperty = dataFactory.getOWLObjectProperty(IRI.create(object.toString()));
					axioms.add(dataFactory.getOWLSubObjectPropertyOfAxiom(subProperty, superProperty));
				} else if(axiomType == AxiomType.EQUIVALENT_OBJECT_PROPERTIES){
					OWLObjectProperty equivProperty1 = dataFactory.getOWLObjectProperty(IRI.create(subject.toString()));
					OWLObjectProperty equivProperty2 = dataFactory.getOWLObjectProperty(IRI.create(object.toString()));
					axioms.add(dataFactory.getOWLEquivalentObjectPropertiesAxiom(equivProperty1, equivProperty2));
				} else if(axiomType == AxiomType.INVERSE_OBJECT_PROPERTIES){
					OWLObjectProperty property = dataFactory.getOWLObjectProperty(IRI.create(subject.toString()));
					OWLObjectProperty inverseProperty = dataFactory.getOWLObjectProperty(IRI.create(object.toString()));
					axioms.add(dataFactory.getOWLInverseObjectPropertiesAxiom(property, inverseProperty));
				} else if(axiomType == AxiomType.DATA_PROPERTY_DOMAIN){
					OWLClass domain = dataFactory.getOWLClass(IRI.create(object.toString()));
					OWLDataProperty property = dataFactory.getOWLDataProperty(IRI.create(subject.toString()));
					axioms.add(dataFactory.getOWLDataPropertyDomainAxiom(property, domain));
				} else if(axiomType == AxiomType.DATA_PROPERTY_RANGE){
					OWLDataRange range = dataFactory.getOWLDatatype(IRI.create(object.toString()));
					OWLDataProperty property = dataFactory.getOWLDataProperty(IRI.create(subject.toString()));
					axioms.add(dataFactory.getOWLDataPropertyRangeAxiom(property, range));
				} else if(axiomType == AxiomType.SUB_DATA_PROPERTY){
					OWLDataProperty subProperty = dataFactory.getOWLDataProperty(IRI.create(subject.toString()));
					OWLDataProperty superProperty = dataFactory.getOWLDataProperty(IRI.create(object.toString()));
					axioms.add(dataFactory.getOWLSubDataPropertyOfAxiom(subProperty, superProperty));
				} else if(axiomType == AxiomType.EQUIVALENT_DATA_PROPERTIES){
					OWLDataProperty equivProperty1 = dataFactory.getOWLDataProperty(IRI.create(subject.toString()));
					OWLDataProperty equivProperty2 = dataFactory.getOWLDataProperty(IRI.create(object.toString()));
					axioms.add(dataFactory.getOWLEquivalentDataPropertiesAxiom(equivProperty1, equivProperty2));
				}
			} else {
				logger.warn("Ignoring triple " + qs);
			}
		}
		logger.trace("Found " + axioms.size() + " axioms.");
		return axioms;
	}
	
	protected OWLLiteral getOWLLiteral(Literal lit){
		OWLLiteral literal = null;
		if(lit.getDatatypeURI() != null){
			IRI datatypeIRI = IRI.create(lit.getDatatypeURI());
			if(OWL2Datatype.isBuiltIn(datatypeIRI)){
				OWL2Datatype datatype = OWL2Datatype.getDatatype(datatypeIRI);
				literal = dataFactory.getOWLLiteral(lit.getLexicalForm(), datatype);
			} else {
				literal = dataFactory.getOWLLiteral(lit.getLexicalForm(), OWL2Datatype.RDF_PLAIN_LITERAL);
			}
		} else {
			if(lit.getLanguage() != null){
				literal = dataFactory.getOWLLiteral(lit.getLexicalForm(), lit.getLanguage());
			} else {
				literal = dataFactory.getOWLLiteral(lit.getLexicalForm());
			}
		}
		return literal;
	}
	
	protected Set<OWLAxiom> retrieveAxioms(OWLObjectProperty prop, int limit, int offset){
		logger.trace("Retrieving schema axioms for object property " + prop);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		String query = String.format("SELECT * WHERE {<%s> ?p ?o.} LIMIT %d OFFSET %d", prop.toStringID(), limit, offset);
		
		ResultSet rs = executeSelect(query);
		QuerySolution qs;
		RDFNode rdfNodePredicate;
		RDFNode rdfNodeObject;
		while(rs.hasNext()){
			qs = rs.next();
			rdfNodePredicate = qs.get("p");
			rdfNodeObject = qs.get("o");
			//skip if object is a blank node
			if(rdfNodeObject.isURIResource()){
				if(rdfNodePredicate.equals(RDF.type)){
					if(rdfNodeObject.equals(OWL.TransitiveProperty)){
						axioms.add(dataFactory.getOWLTransitiveObjectPropertyAxiom(prop));
					} else if(rdfNodeObject.equals(OWL.FunctionalProperty)){
						axioms.add(dataFactory.getOWLFunctionalObjectPropertyAxiom(prop));
					} else if(rdfNodeObject.equals(OWL.InverseFunctionalProperty)){
						axioms.add(dataFactory.getOWLInverseFunctionalObjectPropertyAxiom(prop));
					} else if(rdfNodeObject.equals(OWL.SymmetricProperty)){
						axioms.add(dataFactory.getOWLSymmetricObjectPropertyAxiom(prop));
					} else if(rdfNodeObject.equals(OWL2.AsymmetricProperty)){
						axioms.add(dataFactory.getOWLAsymmetricObjectPropertyAxiom(prop));
					} else if(rdfNodeObject.equals(OWL2.ReflexiveProperty)){
						axioms.add(dataFactory.getOWLReflexiveObjectPropertyAxiom(prop));
					} else if(rdfNodeObject.equals(OWL2.IrreflexiveProperty)){
						axioms.add(dataFactory.getOWLIrreflexiveObjectPropertyAxiom(prop));
					}
				} else if(rdfNodePredicate.equals(RDFS.subPropertyOf)){
					axioms.add(dataFactory.getOWLSubObjectPropertyOfAxiom(prop,
							dataFactory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
				} else if(rdfNodePredicate.equals(OWL.inverseOf)){
					axioms.add(dataFactory.getOWLInverseObjectPropertiesAxiom(prop,
							dataFactory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
				} else if(rdfNodePredicate.equals(RDFS.domain)){
					axioms.add(dataFactory.getOWLObjectPropertyDomainAxiom(prop,
							dataFactory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
				} else if(rdfNodePredicate.equals(RDFS.range)){
					axioms.add(dataFactory.getOWLObjectPropertyRangeAxiom(prop,
							dataFactory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
				} else if(rdfNodePredicate.equals(OWL.equivalentProperty)){
					axioms.add(dataFactory.getOWLEquivalentObjectPropertiesAxiom(prop,
							dataFactory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
				}
			} else {
				logger.warn("Ignoring triple " + qs);
			}
			
		}
		logger.trace("Found " + axioms.size() + " axioms.");
		return axioms;
	}
	
	protected Set<OWLAxiom> getAxiomsFromLinkedDataSource(IRI iri){
		logger.trace("Trying to get informations from linked data uri " + iri);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		try{
			axioms.addAll(manager.loadOntology(iri).getLogicalAxioms());
		} catch (Exception e){
			logger.error(e);
			logger.trace("No linked data retrieved.");
		}
		logger.trace("Got " + axioms.size() + " logical axioms from linked data source.");
		return axioms;
	}
	
	protected boolean isObjectProperty(String propertyURI){
		if(ontology.getObjectPropertiesInSignature().contains(dataFactory.getOWLObjectProperty(IRI.create(propertyURI)))){
			return true;
		}
		if(ontology.getDataPropertiesInSignature().contains(dataFactory.getOWLDataProperty(IRI.create(propertyURI)))){
			return false;
		}
		logger.trace("Checking if property " + propertyURI + " is ObjectProperty");
		
		//first check if triple ($prop rdf:type owl:ObjectProperty) is in the knowledge base
		String query = String.format("ASK {<%s> a <http://www.w3.org/2002/07/owl#ObjectProperty>.}", propertyURI);
		boolean isObjectProperty = executeAsk(query);
		if(isObjectProperty){
			return true;
		}
		
		//we check if triple ($prop rdf:type owl:DataProperty) is in the knowledge base
		query = String.format("ASK {<%s> a <http://www.w3.org/2002/07/owl#DatatypeProperty>.}", propertyURI);
		boolean isDataProperty = executeAsk(query);
		if(isDataProperty){
			return false;
		}
		
		//we check a sample of 10 triples
		query = String.format("SELECT * WHERE {?s <%s> ?o.} LIMIT 10", propertyURI);
		ResultSet rs = executeSelect(query);
		
		isObjectProperty = false;
		isDataProperty = false;
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			
			RDFNode object = qs.get("o");
			isObjectProperty = object.isResource();
			isDataProperty = object.isLiteral();
		}
		return isObjectProperty && !isDataProperty;
	}
	
	protected ResultSet executeSelect(String query){
		return executeSelect(QueryFactory.create(query, Syntax.syntaxARQ));
	}
	
	protected ResultSet executeSelect(Query query){
		ResultSet rs = null;
		if(cache != null){
			rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, query.toString()));
		} else {
			QueryEngineHTTP qe = new QueryEngineHTTP(endpoint.getURL().toString(), query);
			for(String uri : endpoint.getDefaultGraphURIs()){
				qe.addDefaultGraph(uri);
			}
			rs = qe.execSelect();
		}
		return rs;
	}
	
	protected boolean executeAsk(String query){
		QueryEngineHTTP qe = new QueryEngineHTTP(endpoint.getURL().toString(), query);
		for(String uri : endpoint.getDefaultGraphURIs()){
			qe.addDefaultGraph(uri);
		}
		return qe.execAsk();
	}

}
