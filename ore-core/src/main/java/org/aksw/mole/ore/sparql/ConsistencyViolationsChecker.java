package org.aksw.mole.ore.sparql;

import java.util.HashSet;
import java.util.Set;

import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public abstract class ConsistencyViolationsChecker {
	
	private SparqlEndpoint endpoint;
	private ExtractionDBCache cache;
	protected Set<OWLAxiom> violatingAxioms = new HashSet<OWLAxiom>();
	protected OWLDataFactory dataFactory = new OWLDataFactoryImpl();
	
	public ConsistencyViolationsChecker(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		this.endpoint = endpoint;
		this.cache = cache;
	}
	
	public ConsistencyViolationsChecker(SparqlEndpoint endpoint) {
		this(endpoint, null);
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
	
	public abstract Set<OWLAxiom> getViolatingAxioms();
}
