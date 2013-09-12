package experiments;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheCoreEx;
import org.aksw.jena_sparql_api.cache.extra.CacheCoreH2;
import org.aksw.jena_sparql_api.cache.extra.CacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheExImpl;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.mole.ore.util.PrefixedShortFromProvider;
import org.aksw.mole.ore.validation.constraint.OWLAxiomConstraintReportCreator;
import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.utilities.owl.OWLClassExpressionToSPARQLConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyRange;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

public class WoltersKluwerUseCase {
	
	private static final Logger logger = Logger.getLogger(WoltersKluwerUseCase.class.getName());
	private SparqlEndpoint endpoint;
	private OWLOntology constraintOntology;
	private OWLObjectRenderer renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
	private String allowedResourceNamespace = "http://resource.wolterskluwer.de/";
	
	public WoltersKluwerUseCase() {
		new File("wkd").mkdir();
		renderer.setShortFormProvider(new PrefixedShortFromProvider());
		
		
	}
	
	public void run(SparqlEndpoint endpoint, OWLOntology constraints){
		OWLAxiomConstraintReportCreator constraintValidator = new OWLAxiomConstraintReportCreator(endpoint, new ExtractionDBCache("wkd-cache"));
		OWLClassExpressionToSPARQLConverter converter = new OWLClassExpressionToSPARQLConverter();
		QueryExecutionFactory qef = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
		try {
			long timeToLive = TimeUnit.DAYS.toMillis(30);
			CacheCoreEx cacheBackend = CacheCoreH2.create("wkd-cache", timeToLive, true);
			CacheEx cacheFrontend = new CacheExImpl(cacheBackend);
			qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		
		//get violating resources for each constraint
		Map<OWLAxiom, Model> constraint2Violations = constraintValidator.validate(endpoint, constraints, allowedResourceNamespace);
		// for each constraint and each violating resource get the violations
		for (Entry<OWLAxiom, Model> entry : constraint2Violations.entrySet()) {
			OWLAxiom constraint = entry.getKey();
			Model violations = entry.getValue();
			asHTML(constraint, violations);
		}
		//create simple HTML overview page
		StringBuilder html = new StringBuilder();
		html.append("<html><body>\n");
		html.append("<table>\n");
		html.append("<thead><th>Rule</th><th>Nr. of Violations</th></thead>\n");
		for (Entry<OWLAxiom, Model> entry : constraint2Violations.entrySet()) {
			OWLSubClassOfAxiom constraint = (OWLSubClassOfAxiom) entry.getKey();
			Model violations = entry.getValue();
			OWLClassExpression subClass = constraint.getSubClass();
			//get total count
			QueryExecution qe = qef.createQueryExecution(converter.asQuery("?x", subClass, true));
			ResultSet rs = qe.execSelect();
			int totalCount = rs.next().getLiteral("cnt").getInt();
			String renderedString = (subClass.isAnonymous() ? renderer.render(constraint) : renderer.render(subClass));
			html.append("<tr><td><a href=\"" + filename(constraint) + "\">" + renderedString + "</a></td>" +
					"<td align=\"right\">" + violations.listSubjects().toSet().size() + "/" + totalCount + "</td></tr>\n");
		}
		html.append("<table></body></html>");
		try {
			Files.write(html, new File("wkd/wkd.html"), Charsets.UTF_8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void asHTML(OWLAxiom constraint, Model violations){
		OWLSubClassOfAxiom axiom = (OWLSubClassOfAxiom) constraint;
		OWLClassExpression subClass = axiom.getSubClass();
		OWLClassExpression superClass = axiom.getSuperClass();
		
		StringBuilder html = new StringBuilder();
		html.append("<html><body>\n");
		html.append("<p>").append(renderer.render(constraint)).append("</p>\n");
		html.append("<table border=\"1\">\n");
		
		Set<OWLClassExpression> conjunctSet = new TreeSet<OWLClassExpression>(superClass.asConjunctSet());
		
		Multimap<Resource, OWLClassExpression> missing = HashMultimap.create();
		for (Resource subject : violations.listSubjects().toSet()) {
			for (Statement st : violations.listStatements(subject, null, (RDFNode)null).toSet()){
				if(st.getPredicate().equals(RDF.type)){
					for (OWLClassExpression conjunct : conjunctSet) {
						if(!conjunct.isAnonymous() && st.getObject().asResource().getURI().equals(conjunct.asOWLClass().toStringID())){
							missing.put(subject, conjunct);
							break;
						}
					}
				} else {
					boolean done = false;
					for (OWLClassExpression conjunct : conjunctSet) {
						Set<String> propertyURIs = new HashSet<String>();
						if(conjunct instanceof OWLRestriction){
							propertyURIs.add(((OWLRestriction) conjunct).getProperty().getSignature().iterator().next().toStringID());
						} else if(conjunct instanceof OWLObjectUnionOf){
							for (OWLObjectProperty op : conjunct.getObjectPropertiesInSignature()) {
								propertyURIs.add(op.toStringID());
							}
							for (OWLDataProperty dp : conjunct.getDataPropertiesInSignature()) {
								propertyURIs.add(dp.toStringID());
							}
						}
						if(!propertyURIs.isEmpty() && propertyURIs.contains(st.getPredicate().getURI())){
							missing.put(subject, conjunct);done = true;
							break;
						} 
					}
					if(!done){
						System.err.println(st);
					}
				}
			}
		}
		//create header
		html.append("<tr><th></th><th></th>");
		for (OWLClassExpression conjunct : conjunctSet) {
			html.append("<th>" + renderer.render(conjunct) + "</th>");
		}
		html.append("</tr>\n");
		//fill for each resource the missing information
		int i = 1;
		for (Entry<Resource, Collection<OWLClassExpression>> entry : missing.asMap().entrySet()) {
			html.append("<tr>");
			html.append("<td>" + i++ + "</td>");
			Resource resource = entry.getKey();
			html.append("<td>" + resource + "</td>");
			Collection<OWLClassExpression> value = entry.getValue();
			for (OWLClassExpression conjunct : conjunctSet) {
				String columnEntry;
				if(value.contains(conjunct)){
					columnEntry = "X";
				} else {
					columnEntry = "";
				}
				html.append("<td align=\"center\">" + columnEntry+ "</td>");
			}
			html.append("</tr>\n");
		}
		
		html.append("</table>\n");
		html.append("</body></html>");
		try {
			Files.write(html, new File("wkd/" + filename(constraint)), Charsets.UTF_8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String filename(OWLAxiom axiom){
		String filename;
		if(axiom.isOfType(AxiomType.SUBCLASS_OF)){
			OWLSubClassOfAxiom subClassOfAxiom = (OWLSubClassOfAxiom) axiom;
			OWLClassExpression subClass = subClassOfAxiom.getSubClass();
			if(subClass.isAnonymous()){
				filename = renderer.render(axiom);
			} else {
				filename = subClass.asOWLClass().toStringID();
			}
		} else {
			filename = renderer.render(axiom);
		}
		filename = encode(filename) + ".html";
		return filename;
	}
	
	private String encode(String s){
		s = s.replace("/", "+");
		s = s.replace(":", "+");
		s = s.replace(" ", "+");
		return s;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		SparqlEndpoint endpoint = new SparqlEndpoint(new URL("http://lod2.wolterskluwer.de/virtuoso/sparql"));
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology constraintOntology = man.loadOntologyFromOntologyDocument(new File(args[0]));
		
		new WoltersKluwerUseCase().run(endpoint, constraintOntology);
	}

}
