package org.aksw.mole.ore.validation;

import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLProperty;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.vocabulary.XSD;

public abstract class SPARQLConsistencyValidator<T extends Violation, P extends OWLProperty> implements ConsistencyValidator<T, P> {
	
	protected ParameterizedSparqlString queryTemplate;
	protected ParameterizedSparqlString countQueryTemplate;
	protected ParameterizedSparqlString literalAwareQueryTemplate;
	protected ParameterizedSparqlString literalAwareCountQueryTemplate;
	protected SparqlEndpoint endpoint;
	protected ExtractionDBCache cache;
	protected Model model;
	
	protected OWLDataFactory df = new OWLDataFactoryImpl();
	
	public SPARQLConsistencyValidator(Model model) {
		this.model = model;
	}
	
	public SPARQLConsistencyValidator(SparqlEndpoint endpoint) {
		this(endpoint, null);
	}
	
	public SPARQLConsistencyValidator(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		this.endpoint = endpoint;
		this.cache = cache;
	}
	
	protected ResultSet executeSelect(String query, long readTimeout, long connectTimeout){
		ResultSet rs;
		if(model != null){
			rs = QueryExecutionFactory.create(query, model).execSelect();
		} else {
			if(cache == null){
				QueryEngineHTTP qe = new QueryEngineHTTP(endpoint.getURL().toString(), query);
				qe.setTimeout(readTimeout, connectTimeout);
				rs = qe.execSelect();
			} else {
				cache.setMaxExecutionTimeInSeconds((int)readTimeout/1000);
				rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, query));
			}
		}
		return rs;
	}
	
	protected ResultSet executeSelect(String query, long timeout){
		ResultSet rs;
		if(model != null){
			rs = QueryExecutionFactory.create(query, model).execSelect();
		} else {
			if(cache == null){
				QueryEngineHTTP qe = new QueryEngineHTTP(endpoint.getURL().toString(), query);
	//			qe.setTimeout(timeout);
				rs = qe.execSelect();
			} else {
				cache.setMaxExecutionTimeInSeconds((int)timeout/1000);
				rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, query));
			}
		}
		return rs;
	}
	
	protected ResultSet executeSelect(String query){
		return executeSelect(query, 0);
	}
	
	protected ResultSet executeSelect(Query query, long readTimeout, long connectionTimeout){
		return executeSelect(query.toString(), readTimeout, connectionTimeout);
	}
	
	protected ResultSet executeSelect(Query query, long timeout){
		return executeSelect(query.toString(), timeout);
	}
	
	protected ResultSet executeSelect(Query query){
		return executeSelect(query, 0);
	}
	
	public long getNumberOfViolations(OWLProperty p){
		ParameterizedSparqlString tmp = null;
		if(literalAwareCountQueryTemplate != null && isXSDDateRange(p)){
			tmp = countQueryTemplate;
			countQueryTemplate = literalAwareCountQueryTemplate;
		}
		countQueryTemplate.clearParams();
		countQueryTemplate.setIri("p", p.toStringID());
		long cnt = -1;
		ResultSet rs = executeSelect(countQueryTemplate.asQuery());
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			cnt = qs.getLiteral("cnt").getLong();
		}
		if(tmp != null){
			countQueryTemplate = tmp;
		}
		return cnt;
	}
	
	protected OWLLiteral convertLiteral(Literal lit) {
		String datatypeURI = lit.getDatatypeURI();
		OWLLiteral owlLiteral;
		if (datatypeURI == null) {// rdf:PlainLiteral
			owlLiteral = df.getOWLLiteral(lit.getLexicalForm(), lit.getLanguage());
		} else {
			owlLiteral = df.getOWLLiteral(lit.getLexicalForm(), df.getOWLDatatype(IRI.create(datatypeURI)));
		}
		return owlLiteral;
	}
	
	protected boolean isXSDDateRange(OWLProperty p){
		String query = String.format("SELECT ?range WHERE {<%s> <http://www.w3.org/2000/01/rdf-schema#range> ?range}", p.toStringID());
		ResultSet rs = executeSelect(query);
		if(rs.hasNext()){
			if(rs.next().getResource("range").equals(XSD.date)){
				return true;
			}
		}
		return false;
	}
	
	protected OWLObject parseNode(RDFNode node){
		if(node.isURIResource()){
			return new OWLNamedIndividualImpl(IRI.create(node.asResource().getURI()));
		} else if(node.isLiteral()){
			return convertLiteral(node.asLiteral());
		} else {
			throw new UnsupportedOperationException("Can not handle node type: " + node);
		}
	}
	
	public boolean isConsistent(P p){
		ParameterizedSparqlString tmp = null;
		if(literalAwareQueryTemplate != null && isXSDDateRange(p)){
			tmp = queryTemplate;
			queryTemplate = literalAwareQueryTemplate;
		}
		queryTemplate.clearParams();
		queryTemplate.setIri("p", p.toStringID());
		
		Query query = queryTemplate.asQuery();
		query.setLimit(1);
		ResultSet rs = executeSelect(query);
		if(tmp != null){
			queryTemplate = tmp;
		}
		return !rs.hasNext();
	}

}
