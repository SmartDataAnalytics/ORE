package org.aksw.mole.ore.validation;

import org.dllearner.core.owl.Datatype;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.KBElement;
import org.dllearner.core.owl.Property;
import org.dllearner.core.owl.TypedConstant;
import org.dllearner.core.owl.UntypedConstant;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;

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

public abstract class SPARQLConsistencyValidator<T extends Violation, P extends Property> implements ConsistencyValidator<T, P> {
	
	protected ParameterizedSparqlString queryTemplate;
	protected ParameterizedSparqlString countQueryTemplate;
	protected ParameterizedSparqlString literalAwareQueryTemplate;
	protected ParameterizedSparqlString literalAwareCountQueryTemplate;
	protected SparqlEndpoint endpoint;
	protected ExtractionDBCache cache;
	protected Model model;
	
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
	
	public long getNumberOfViolations(Property p){
		ParameterizedSparqlString tmp = null;
		if(literalAwareCountQueryTemplate != null && isXSDDateRange(p)){
			tmp = countQueryTemplate;
			countQueryTemplate = literalAwareCountQueryTemplate;
		}
		countQueryTemplate.clearParams();
		countQueryTemplate.setIri("p", p.getURI().toString());
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
	
	protected boolean isXSDDateRange(Property p){
		String query = String.format("SELECT ?range WHERE {<%s> <http://www.w3.org/2000/01/rdf-schema#range> ?range}", p.getURI().toString());
		ResultSet rs = executeSelect(query);
		if(rs.hasNext()){
			if(rs.next().getResource("range").equals(XSD.date)){
				return true;
			}
		}
		return false;
	}
	
	protected KBElement parseNode(RDFNode node){
		if(node.isURIResource()){
			return new Individual(node.asResource().getURI());
		} else if(node.isLiteral()){
			Literal lit = node.asLiteral();
			KBElement object;
			if(lit.getDatatypeURI() != null){
				object = new TypedConstant(lit.getLexicalForm(), new Datatype(lit.getDatatypeURI()));
			} else {
				if(lit.getLanguage() != null){
					object = new UntypedConstant(lit.getLexicalForm(), lit.getLanguage());
				} else {
					object = new UntypedConstant(lit.getLexicalForm());
				}
			}
			return object;
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
		queryTemplate.setIri("p", p.getURI().toString());
		
		Query query = queryTemplate.asQuery();
		query.setLimit(1);
		ResultSet rs = executeSelect(query);
		if(tmp != null){
			queryTemplate = tmp;
		}
		return !rs.hasNext();
	}

}
