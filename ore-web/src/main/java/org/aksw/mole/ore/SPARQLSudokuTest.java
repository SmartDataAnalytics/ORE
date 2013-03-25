package org.aksw.mole.ore;

import java.io.ByteArrayInputStream;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class SPARQLSudokuTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Model model = ModelFactory.createDefaultModel();
		String turtle = 
				"@prefix ex: <http://example.org/> ." +
				"ex:field ex:allowed \"1\"^^<http://www.w3.org/2001/XMLSchema#int>,"+
				"\"2\"^^<http://www.w3.org/2001/XMLSchema#int>,"+
				"\"3\"^^<http://www.w3.org/2001/XMLSchema#int>,"+
				"\"4\"^^<http://www.w3.org/2001/XMLSchema#int>.";
		model.read(new ByteArrayInputStream(turtle.getBytes()), null, "TURTLE");
		
		String query = 
				"PREFIX ex:<http://example.org/>"+
"SELECT ?w11 ?w12 ?w13 ?w14" +
	"?w21 ?w22 ?w23 ?w24"+
	"?w31 ?w32 ?w33 ?w34"+
	"?w41 ?w42 ?w43 ?w44"+
"WHERE {"+
"ex:field ex:allowed ?w11, ?w12, ?w13, ?w14, "+
		    "?w21, ?w22, ?w23, ?w24, "+
		    "?w31, ?w32, ?w33, ?w34, "+
		    "?w41, ?w42, ?w43, ?w44 ."+
"FILTER("+
  "(?w11 != ?w12) && (?w11 != ?w13) && (?w11 != ?w14) && "+
  "(?w12 != ?w13) && (?w12 != ?w14) &&"+
  "(?w13 != ?w14) &&"+
  "(?w21 != ?w22) && (?w21 != ?w23) && (?w21 != ?w24) &&"+
  "(?w22 != ?w23) && (?w22 != ?w24) &&"+
  "(?w23 != ?w24) &&"+
  "(?w31 != ?w32) && (?w31 != ?w33) && (?w31 != ?w34) &&"+
  "(?w32 != ?w33) && (?w32 != ?w34) &&"+
  "(?w33 != ?w34) &&"+
  "(?w41 != ?w42) && (?w41 != ?w43) && (?w41 != ?w44) &&"+
  "(?w42 != ?w43) && (?w42 != ?w44) &&"+
  "(?w43 != ?w44) &&"+
  "(?w11 != ?w21) && (?w11 != ?w31) && (?w11 != ?w41) &&"+
  "(?w21 != ?w31) && (?w21 != ?w41) &&"+
  "(?w31 != ?w41) &&"+
 "(?w12 != ?w22) && (?w12 != ?w32) && (?w12 != ?w42) &&"+
  "(?w22 != ?w32) && (?w22 != ?w42) &&"+
  "(?w32 != ?w42) &&"+
  "(?w13 != ?w23) && (?w13 != ?w33) && (?w13 != ?w43) &&"+
  "(?w23 != ?w33) && (?w23 != ?w43) &&"+
  "(?w33 != ?w43) &&"+
  "(?w14 != ?w24) && (?w14 != ?w34) && (?w14 != ?w44) &&"+
  "(?w24 != ?w34) && (?w24 != ?w44) &&"+
  "(?w34 != ?w44) &&"+
  "(?w11 != ?w22) && (?w12 != ?w21 ) &&"+
  "(?w13 != ?w24) && (?w23 != ?w14 ) &&"+
 "(?w31 != ?w42) && (?w32 != ?w41 ) &&"+
  "(?w33 != ?w44) && (?w34 != ?w43 ) &&"+
  "(?w14 = \"3\"^^<http://www.w3.org/2001/XMLSchema#int>) &&"+
  "(?w24 = \"4\"^^<http://www.w3.org/2001/XMLSchema#int>) &&"+
  "(?w31 = \"2\"^^<http://www.w3.org/2001/XMLSchema#int>) &&"+
  "(?w41 = \"3\"^^<http://www.w3.org/2001/XMLSchema#int>)"+
")"+
"}";

		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet rs = qe.execSelect();
		while(rs.hasNext()){
			System.out.println(rs.next());
		}
	}

}
