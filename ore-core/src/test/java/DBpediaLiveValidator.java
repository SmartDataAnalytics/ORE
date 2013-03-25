import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.validation.AsymmetryConsistencyValidator;
import org.aksw.mole.ore.validation.FunctionalityConsistencyValidator;
import org.aksw.mole.ore.validation.FunctionalityViolation;
import org.aksw.mole.ore.validation.InverseFunctionalityConsistencyValidator;
import org.aksw.mole.ore.validation.InverseFunctionalityViolation;
import org.aksw.mole.ore.validation.IrreflexivityConsistencyValidator;
import org.aksw.mole.ore.validation.SPARQLConsistencyValidator;
import org.aksw.mole.ore.validation.ValidatorFactory;
import org.aksw.mole.ore.validation.Violation;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.core.owl.Property;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLUnaryPropertyAxiom;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;


public class DBpediaLiveValidator {
	
	private static OWLOntology enrichedOntology;
	private static OWLOntologyManager man;
	
	private static final String evaluationOutputDirectory = "dbpedia_evaluation";
	
	static int i;
	final static SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();
	final static ExtractionDBCache cache = new ExtractionDBCache("cache");
	
	final static boolean irreflexive = true;
	final static boolean asymmetric = true;
	final static boolean functional = true;
	final static boolean inverseFunctional = true;
	static Collection<? extends Violation> violations;
	
	public static void main(String[] args) throws Exception {
		final SparqlEndpoint enrichedEndpoint = new SparqlEndpoint(new URL("http://live.dbpedia.org/sparql"), 
				Collections.singletonList("http://enrichment.dbpedia.org"), Collections.<String>emptyList());
		man = OWLManager.createOWLOntologyManager();
		enrichedOntology = man.loadOntologyFromOntologyDocument(new File("/home/me/work/dbpedia_3.8_enrichment.owl"));
		
		final Map<Property, Double> irreflexiveProperties = loadProperties(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY);
		final Map<Property, Double> asymmetricProperties = loadProperties(AxiomType.ASYMMETRIC_OBJECT_PROPERTY);
		final Map<Property, Double> functionalProperties = loadProperties(AxiomType.FUNCTIONAL_OBJECT_PROPERTY);
		functionalProperties.putAll(loadProperties(AxiomType.FUNCTIONAL_DATA_PROPERTY));
		final Map<Property, Double> inverseFunctionalProperties = loadProperties(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY);
		
		final Map<Property, Collection<? extends Violation>> irreflexiveViolations = new HashMap<Property, Collection<? extends Violation>>();
		final Map<Property, Collection<? extends Violation>> asymmetricViolations = new HashMap<Property, Collection<? extends Violation>>();
		final Map<Property, Collection<? extends Violation>> functionalViolations = new HashMap<Property, Collection<? extends Violation>>();
		final Map<Property, Collection<? extends Violation>> inverseFunctionalViolations = new HashMap<Property, Collection<? extends Violation>>();
		
		final Map<Property, Long> irreflexiveViolationCount = new HashMap<Property, Long>();
		final Map<Property, Long> asymmetricViolationCount = new HashMap<Property, Long>();
		final Map<Property, Long> functionalViolationCount = new HashMap<Property, Long>();
		final Map<Property, Long> inverseFunctionalViolationCount = new HashMap<Property, Long>();
		
		String query;
		ResultSet rs;
		final Set<Property> properties = new HashSet<Property>();
		
		final ValidatorFactory valFac = new ValidatorFactory(endpoint, cache);
		
		
		//1. check for Irreflexivity violations
		if(irreflexive){
			query = "SELECT ?p WHERE {?p a <http://www.w3.org/2002/07/owl#IrreflexiveProperty>.}";
			rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(enrichedEndpoint, query));
			
			while(rs.hasNext()){
				properties.add(new ObjectProperty(rs.next().getResource("p").getURI()));
			}
			System.out.println("Irreflexivity");
			IrreflexivityConsistencyValidator validator = new IrreflexivityConsistencyValidator(endpoint, cache);
			
			for(Property p : properties){
				if(p instanceof ObjectProperty){
					violations = validator.getViolations((ObjectProperty) p);
					if(!violations.isEmpty()){
						irreflexiveViolations.put(p, violations);
						long cnt = validator.getNumberOfViolations(p);
						irreflexiveViolationCount.put(p, cnt);
					}
				}
			}
		}
		
		//2. check for Asymmetry violations
		if(asymmetric){
			query = "SELECT ?p WHERE {?p a <http://www.w3.org/2002/07/owl#AsymmetricProperty>.}";
			rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(enrichedEndpoint, query));
			properties.clear();
			while (rs.hasNext()) {
				properties.add(new ObjectProperty(rs.next().getResource("p").getURI()));
			}
			System.out.println("Asymmetry");
			AsymmetryConsistencyValidator validator2 = new AsymmetryConsistencyValidator(endpoint, cache);
			for (Property p : properties) {
				violations = validator2.getViolations((ObjectProperty) p);
				if (!violations.isEmpty()) {
					asymmetricViolations.put(p, violations);
					long cnt = validator2.getNumberOfViolations(p);
					if(cnt == 0){System.out.println(p);System.out.println(violations);}
					asymmetricViolationCount.put(p, cnt);
				}
			}
		}
		
		ExecutorService threadPool;
		
		//3. check for Functionality violations
		if(functional){
			query = "SELECT ?p WHERE {?p a <http://www.w3.org/2002/07/owl#FunctionalProperty>.}";
			rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(enrichedEndpoint, query));
			properties.clear();
			QuerySolution qs;
			while (rs.hasNext()) {
				qs = rs.next();
				properties.add(new ObjectProperty(qs.getResource("p").getURI()));
			}
//			query = "SELECT ?p WHERE {?p a <http://www.w3.org/2002/07/owl#FunctionalProperty>. ?p a <http://www.w3.org/2002/07/owl#DatatypeProperty>.}";
//			rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(enrichedEndpoint, query));
//			properties.clear();
//			while (rs.hasNext()) {
//				qs = rs.next();
//				properties.add(new DatatypeProperty(qs.getResource("p").getURI()));
//			}
			System.out.println("Functionality");
			threadPool = Executors.newFixedThreadPool(1);
			for (final Property op : properties) {
				threadPool.execute(new Runnable() {
					
					@Override
					public void run() {
//						System.out.println(i++ + "/" + properties.size() + ": " + op);
						FunctionalityConsistencyValidator validator3 = new FunctionalityConsistencyValidator(endpoint, new ExtractionDBCache("cache"));
//						System.out.println("Analysing " + op);
						Collection<FunctionalityViolation> violations = validator3.getViolations(op);
						if (!violations.isEmpty()) {
							functionalViolations.put(op, violations);
							long cnt = validator3.getNumberOfViolations(op);
							functionalViolationCount.put(op, cnt);
						}
					}
				});
			}
			threadPool.shutdown();
			threadPool.awaitTermination(10, TimeUnit.HOURS);
		}
		
		
		
		//4. check for InverseFunctionality violations
		if(inverseFunctional){
			query = "SELECT ?p WHERE {?p a <http://www.w3.org/2002/07/owl#InverseFunctionalProperty>.}";
			rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(enrichedEndpoint, query));
			properties.clear();
			while (rs.hasNext()) {
				properties.add(new ObjectProperty(rs.next().getResource("p").getURI()));
			}
			System.out.println("Inverse Functionality");
			threadPool = Executors.newFixedThreadPool(1);
			
			i = 0;
			for (final Property op : properties) {
				threadPool.execute(new Runnable() {
					
					@Override
					public void run() {
						InverseFunctionalityConsistencyValidator validator4 = new InverseFunctionalityConsistencyValidator(endpoint, cache);
						
//						System.out.println("Analysing " + op);
						if(!op.getName().equals("http://dbpedia.org/ontology/position") && !op.getName().equals("http://dbpedia.org/ontology/routeStartDirection")
								&& !op.getName().equals("http://dbpedia.org/ontology/utcOffset")){//runs infinitely
//							System.out.println(i++ + "/" + properties.size() + ": " + op);
							Collection<InverseFunctionalityViolation> violations = validator4.getViolations((ObjectProperty) op);
							if (!violations.isEmpty()) {
								inverseFunctionalViolations.put(op, violations);
								long cnt = validator4.getNumberOfViolations(op);
								inverseFunctionalViolationCount.put(op, cnt);
							}
						} 
					}
				});
			}
			threadPool.shutdown();
			threadPool.awaitTermination(10, TimeUnit.HOURS);
		}
		
		
		printResults("Irreflexivity", irreflexiveProperties, irreflexiveViolations, irreflexiveViolationCount);
		printResults("Asymmetry", asymmetricProperties, asymmetricViolations, asymmetricViolationCount);
		printResults("Functionality", functionalProperties, functionalViolations, functionalViolationCount);
		printResults("Inverse Functionality", inverseFunctionalProperties, inverseFunctionalViolations, inverseFunctionalViolationCount);
		

		//generate data for manual evaluation, i.e. for each axiom type we generate a random sample of max. 100 properties with an accuracy > 0.95
		final double threshold = 0.95;
		final int maxSampleSize = 100;
		
		List<Property> sample = generateSample(irreflexiveProperties, irreflexiveViolations, threshold, maxSampleSize);
		write2Disk("irreflexive", sample, irreflexiveViolations);
		
		sample = generateSample(asymmetricProperties, asymmetricViolations, threshold, maxSampleSize);
		write2Disk("asymmetric", sample, asymmetricViolations);
		
		sample = generateSample(functionalProperties, functionalViolations, threshold, maxSampleSize);
		write2Disk("functional", sample, functionalViolations);
		
		sample = generateSample(inverseFunctionalProperties, inverseFunctionalViolations, threshold, maxSampleSize);
		write2Disk("inverse_functional", sample, inverseFunctionalViolations);
		
//		generateHTML("irreflexive", irreflexiveProperties, irreflexiveViolations, threshold, maxSampleSize);
		
//		new EvaluationGUI(new File("dbpedia_evaluation/irreflexive.txt"), irreflexiveViolations);
//		new EvaluationGUI(new File("dbpedia_evaluation/functional.txt"), functionalViolations);
//		new EvaluationGUI(new File("dbpedia_evaluation/asymmetric.txt"), asymmetricViolations);
//		new EvaluationGUI(new File("dbpedia_evaluation/inverse_functional.txt"), asymmetricViolations);
		
		write2DB("irreflexive", irreflexiveProperties, irreflexiveViolations, irreflexiveViolationCount, threshold, maxSampleSize);
		write2DB("asymmetric", asymmetricProperties, asymmetricViolations, asymmetricViolationCount, threshold, maxSampleSize);
		write2DB("functional", functionalProperties, functionalViolations, functionalViolationCount, threshold, maxSampleSize);
		write2DB("inverse_functional", inverseFunctionalProperties, inverseFunctionalViolations, inverseFunctionalViolationCount, threshold, maxSampleSize);
	}
	
	private static <T extends Violation> void write2DB(String name, Map<Property, Double> property2Confidence, Map<Property, Collection<? extends Violation>> property2Violations, Map<Property, Long> property2ViolationCount, double threshold, int maxSampleSize){
		List<Property> sample = generateSample(property2Confidence, property2Violations, threshold, maxSampleSize);
		DBManager man = new DBManager();
		man.createTable(name);
		for(Property p : sample){
			Violation violation = property2Violations.get(p).iterator().next();
			man.addEntry(name, p, property2ViolationCount.get(p), violation.toString(), violation.asHTML());
		}
	}
	
	
	private static <T extends Violation> List<Property> generateSample(Map<Property, Double> property2Confidence, Map<Property, Collection<? extends Violation>> property2Violations, double threshold, int maxSampleSize){
		Set<Property> properties = getViolatedPropertiesAboveThreshold(property2Confidence, property2Violations, threshold);
		List<Property> propertiesList = new ArrayList<Property>(new TreeSet<Property>(properties));
		Random rnd = new Random(123);
		Collections.shuffle(propertiesList, rnd);
		List<Property> sample = propertiesList.subList(0, Math.min(properties.size(), maxSampleSize));
		return sample;
	}
	
	private static <T extends Violation> void write2Disk(String name, List<Property> properties, Map<Property, Collection<? extends Violation>> property2Violations){
		try {
			new File(evaluationOutputDirectory).mkdir();
			BufferedWriter out = new BufferedWriter(new FileWriter(evaluationOutputDirectory + File.separator + name + ".txt"));
			for(Property prop : properties){
				out.write(prop.getURI().toString());
				out.write("###");
				out.write(property2Violations.get(prop).iterator().next().toString().replace("-", "").replace("\n", "***"));
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static <T extends Violation> void generateHTML(String name, Map<Property, Double> property2Confidence, Map<Property, Collection<? extends Violation>> property2Violations, double threshold, int maxSampleSize){
		List<Property> properties = generateSample(property2Confidence, property2Violations, threshold, maxSampleSize);
		String html = "<html><body><table rules=\"rows\">\n";
		for(Property prop : properties){
			String violation = property2Violations.get(prop).iterator().next().toString();
			html += "<tr>";
			html += "<td><input type=\"checkbox\" name=\"myTextEditBox\" style=\"margin-left:auto; margin-right:auto;\"></td>";
			html += "<td>";
			html += "<p>" + prop.getName();
			html += "<p><b>Description:</b>" + (getDescription(prop) == null ? "N/A" : getDescription(prop));
			html += "<p><b>Domain:</b>" + (getDomain(prop) == null ? "N/A" : getDomain(prop));
			html += "<p><b>Range:</b>" + (getRange(prop) == null ? "N/A" : getRange(prop));
			html += "<p><b>Sample:</b><br>" + violation;
			html += "</td>";
			html += "</tr>\n";
			
		}
		
		html += "</table></body></html>";
		try {
			new File(evaluationOutputDirectory).mkdir();
			BufferedWriter out = new BufferedWriter(new FileWriter(evaluationOutputDirectory + File.separator + name + ".html"));
			out.write(html);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String getDomain(Property p){
		String s = null;
		String q = String.format("SELECT ?domain WHERE {<%s> rdfs:domain ?domain.}", p.getURI().toString());
		ResultSet rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, q));
		if(rs.hasNext()){
			s = rs.next().getResource("domain").getURI();
		}
		return s;
	}
	
	private static String getRange(Property p){
		String s = null;
		String q = String.format("SELECT ?range WHERE {<%s> rdfs:range ?range.}", p.getURI().toString());
		ResultSet rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, q));
		if(rs.hasNext()){
			s = rs.next().getResource("range").getURI();
		}
		return s;
	}
	
	private static String getDescription(Property p){
		String s = null;
		String q = String.format("SELECT ?desc WHERE {<%s> rdfs:comment ?desc.}", p.getURI().toString());
		ResultSet rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, q));
		if(rs.hasNext()){
			s = rs.next().getLiteral("desc").getLexicalForm();
		}
		return s;
	}
	
	private static Set<Property> getPropertiesAboveThreshold(Map<Property, Double> property2Confidence, double threshold){
		Set<Property> properties = new HashSet<Property>();
		for(Entry<Property, Double> entry : property2Confidence.entrySet()){
			if(entry.getValue() >= threshold){
				properties.add(entry.getKey());
			}
		}
		return properties;
	}
	
	private static <T extends Violation> Set<Property> getViolatedPropertiesAboveThreshold(Map<Property, Double> property2Confidence, Map<Property, Collection<? extends Violation>> property2Violations, double threshold){
		Set<Property> properties = new HashSet<Property>();
		Property prop;
		for(Entry<Property, Double> entry : property2Confidence.entrySet()){
			if(entry.getValue() >= threshold){
				prop = entry.getKey();
				if(property2Violations.containsKey(prop)){
					properties.add(prop);
				}
			}
		}
		return properties;
	}
	
	private static <T extends Violation> void printResults(String type, Map<Property, Double> property2Confidence, 
			Map<Property, Collection<? extends Violation>> property2Violations, 
			Map<Property, Long> property2ViolationCount){
		System.out.println("##########################################################################");
		System.out.println(type);
		System.out.println("Min. Accuracy\t#Violated Properties/#Total Properties");
		for(double min = 0.75; min <= 1.00; min += 0.05){
			int nrOfProperties = 0;
			int nrOfViolatedProperties = 0;
			for(Entry<Property, Double> entry : property2Confidence.entrySet()){
				if(entry.getValue() >= min){
					nrOfProperties++;
					if(property2Violations.containsKey(entry.getKey())){
						nrOfViolatedProperties++;
					}
				}
			}
			System.out.println(((int)(min*100.0))/100.0 + ":\t\t\t" + nrOfViolatedProperties + "/" + nrOfProperties);
		}
		//print minimum and maximum number of violations
		long min = Long.MAX_VALUE;
		long max = 0;
		SortedSet<Property> maxProp = new TreeSet<Property>();
		for(Long value : property2ViolationCount.values()){
			if(value > max){
				max= value;
			} else if(value < min){
				min = value;
			} 
		}
		for(Entry<Property, Long> entry : property2ViolationCount.entrySet()){
			long value = entry.getValue();
			if(value == max){
				maxProp.add(entry.getKey());
			} 
		}
		System.out.println("Min #violations: " + min);
		System.out.println("Max #violations: " + max + "(" + maxProp + ")");
		System.out.println("Sample: ");
		int i = 0;
		for(Entry<Property, Collection<? extends Violation>> entry : property2Violations.entrySet()){
			System.out.println(entry.getValue().iterator().next());
			i++;
			if(i == 5) break;
		}
	}
	
	private static <P extends OWLPropertyExpression<?,?>, T extends OWLUnaryPropertyAxiom<P>> Map<Property, Double> loadProperties(AxiomType<T>... axiomTypes){
		Map<Property, Double> property2Confidence = new HashMap<Property, Double>();
		for(AxiomType<T> axiomType : axiomTypes){
			for(T ax : enrichedOntology.getAxioms(axiomType)){
				String iri = (ax.getProperty() instanceof OWLObjectProperty) ? ((OWLObjectProperty)ax.getProperty()).toStringID() : ((OWLDataProperty)ax.getProperty()).toStringID();
				ObjectProperty op = new ObjectProperty(iri);
				double confidence = ((OWLLiteral)ax.getAnnotations(
						man.getOWLDataFactory().getOWLAnnotationProperty(IRI.create("http://www.dl-learner.org/ontologies/enrichment.owl#confidence"))).iterator().next().getValue()).parseDouble();
				property2Confidence.put(op, confidence);
			}
		}
		return property2Confidence;
	}

}
