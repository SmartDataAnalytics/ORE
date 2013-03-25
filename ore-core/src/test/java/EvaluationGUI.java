import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.aksw.mole.ore.validation.Violation;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.core.owl.Property;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;

import com.hp.hpl.jena.query.ResultSet;


public class EvaluationGUI extends JFrame{
	
	private List<Property> properties = new ArrayList<Property>();
	
	private JLabel propertyLabel;
	private JEditorPane sampleLabel;;
	private JCheckBox cb;
	private JButton nextButton;
	private JButton backButton;
	
	private BufferedWriter bw;
	
	private Map<Property, Boolean> property2Value = new LinkedHashMap<Property, Boolean>();
	
	private Map<Property, String> violations = new HashMap<Property, String>();
	
	Stack<Property> stack = new Stack<Property>();
	
	private File evalFile;
	
	private ExtractionDBCache cache = new ExtractionDBCache("cache");
	private SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();

	public EvaluationGUI(File file) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(new Dimension(1200, 300));
		setTitle(file.getName());
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		add(panel);
		
		JPanel content = new JPanel();
		panel.add(content, BorderLayout.NORTH);
		
		propertyLabel = new JLabel();
//		content.add(propertyLabel);
		
		content.add(new JLabel("Axiom is correct:"));
		cb = new JCheckBox();
		content.add(cb);
		
		sampleLabel  = new JEditorPane();
		sampleLabel.setContentType("text/html");
		sampleLabel.setEditable(false);
		sampleLabel.addHyperlinkListener(new HyperlinkListener() {
			
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				URL url = e.getURL();
				try {
					Desktop.getDesktop().browse(URI.create(url.toString().replace("http://dbpedia", "http://live.dbpedia")));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		panel.add(sampleLabel, BorderLayout.CENTER);
		
		JPanel buttons = new JPanel();
		panel.add(buttons, BorderLayout.SOUTH);
		
		backButton = new JButton("Back");
		backButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				showPreviousProperty();
			}
		});
		buttons.add(backButton);
		
		nextButton = new JButton("Next");
		nextButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(e.getActionCommand().equals("next")){
					showNextProperty();
				} else {
					finish();
				}
			}
		});
		buttons.add(nextButton);
		
		evalFile = new File(file.getParent() + File.separator + file.getName().replace(".txt", "") + "_evaluated.txt");
		
		loadProperties(file);
		
		pack();
		
		setVisible(true);
	}
	
	public <T extends Violation> EvaluationGUI(File file, Map<Property, Set<T>> property2Violations) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(new Dimension(1200, 300));
		setTitle(file.getName());
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		add(panel);
		
		JPanel content = new JPanel();
		panel.add(content, BorderLayout.NORTH);
		
		propertyLabel = new JLabel();
//		content.add(propertyLabel);
		
		content.add(new JLabel("Axiom is correct:"));
		cb = new JCheckBox();
		content.add(cb);
		
		sampleLabel  = new JEditorPane();
		sampleLabel.setContentType("text/html");
		sampleLabel.setEditable(false);
		sampleLabel.addHyperlinkListener(new HyperlinkListener() {
			
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				HyperlinkEvent.EventType type = e.getEventType();
			    final URL url = e.getURL();
				if (type == HyperlinkEvent.EventType.ACTIVATED) {
					try {
						Desktop.getDesktop().browse(URI.create(url.toString().replace("http://dbpedia", "http://live.dbpedia")));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		panel.add(sampleLabel, BorderLayout.CENTER);
		
		JPanel buttons = new JPanel();
		panel.add(buttons, BorderLayout.SOUTH);
		
		backButton = new JButton("Back");
		backButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				showPreviousProperty();
			}
		});
		buttons.add(backButton);
		
		nextButton = new JButton("Next");
		nextButton.setActionCommand("next");
		nextButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(e.getActionCommand().equals("next")){
					showNextProperty();
				} else if(e.getActionCommand().equals("finish")){
					finish();
				}
			}
		});
		buttons.add(nextButton);
		
		evalFile = new File(file.getParent() + File.separator + file.getName().replace(".txt", "") + "_evaluated.txt");
		
		loadProperties(file, property2Violations);
		
		pack();
		
		setVisible(true);
	}
	
	private void write2Disk(){
		try {
			bw = new BufferedWriter(new FileWriter(evalFile));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		for(Entry<Property, Boolean> entry : property2Value.entrySet()){
			try {
				bw.write((entry.getValue() ? "+" : "-") + "," + entry.getKey());
				bw.newLine();
				bw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void finish(){
		Property oldProperty = new ObjectProperty(propertyLabel.getText());
		property2Value.put(oldProperty, cb.isSelected());
		stack.push(oldProperty);
		write2Disk();
		dispose();
	}
	
	private void showNextProperty(){
		Property oldProperty = new ObjectProperty(propertyLabel.getText());
		property2Value.put(oldProperty, cb.isSelected());
		stack.push(oldProperty);
		
		write2Disk();
		
		Property newProperty = properties.remove(0);
		propertyLabel.setText(newProperty.getURI().toString());
		if(property2Value.containsKey(newProperty)){
			cb.setSelected(property2Value.get(newProperty));
		}
		showSample(newProperty);
		
		cb.setSelected(false);
		
		if(properties.isEmpty()){
			nextButton.setText("Finish");
			nextButton.setActionCommand("finish");
		} else {
			nextButton.setText("Next");
			nextButton.setActionCommand("next");
		}
//		nextButton.setEnabled(!properties.isEmpty());
		backButton.setEnabled(true);
	}
	
	private void showPreviousProperty(){
		write2Disk();
		
		Property p = stack.pop();
		propertyLabel.setText(p.getURI().toString());
		showSample(p);
		cb.setSelected(property2Value.get(p));
		properties.add(0, p);
		
		nextButton.setEnabled(true);
		backButton.setEnabled(!stack.isEmpty());
	}
	
	private void showSample(Property p){
		String text = "<html>";
		text += "<p><b>Property:</b>" + p.getURI().toString() + "</p>";
		String desc = getDescription(p);
		if(desc != null){
			text += "<p><b>Description:</b> " + desc + "</p>";
		}
		String domain = getDomain(p);
		if(domain != null){
			text += "<p><b>Domain:</b> " + domain + "</p>";
		}
		String range = getRange(p);
		if(range != null){
			text += "<p><b>Range:</b> " + range + "</p>";
		}
		text += "<p><b>Sample:</b><br/>" + violations.get(p).toString().replace("\n", "<br/>") + "</p>";
		text += "</html>";
		sampleLabel.setText(text);
	}
	
	
	
	private void loadProperties(File file){
		try {
			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				String[] split = strLine.split("###");
				properties.add(new ObjectProperty(split[0].trim()));
				violations.put(new ObjectProperty(split[0].trim()), split[1].replace("***", "\n"));
			}
			in.close();
			//check for already evaluated properties
			File evalFile = new File(file.getParent()  + File.separator + file.getName().replace(".txt", "") + "_evaluated.txt");
			if(evalFile.exists()){
				fstream = new FileInputStream(evalFile);
				in = new DataInputStream(fstream);
				br = new BufferedReader(new InputStreamReader(in));
				while ((strLine = br.readLine()) != null) {
					Property p = new ObjectProperty(strLine.split(",")[1].trim());
					boolean correct = strLine.split(",")[0].trim().equals("+") ? true : false;
					property2Value.put(p, correct);
					stack.push(p);
					properties.remove(p);
				}
				in.close();
			}
			
			Property p = properties.remove(0);
			propertyLabel.setText(p.getURI().toString());
			showSample(p);
			
			nextButton.setEnabled(!properties.isEmpty());
			backButton.setEnabled(!stack.isEmpty());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			bw = new BufferedWriter(new FileWriter(evalFile));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private <T extends Violation> void loadProperties(File file, Map<Property, Set<T>> property2Violations){
		try {
			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				String[] split = strLine.split("###");
				ObjectProperty p = new ObjectProperty(split[0].trim());
				properties.add(p);
				this.violations.put(p, property2Violations.get(p).iterator().next().asHTML());
			}
			in.close();
			//check for already evaluated properties
			LinkedList<Property> evaluatedProperties = new LinkedList<Property>();
			File evalFile = new File(file.getParent()  + File.separator + file.getName().replace(".txt", "") + "_evaluated.txt");
			if(evalFile.exists()){
				fstream = new FileInputStream(evalFile);
				in = new DataInputStream(fstream);
				br = new BufferedReader(new InputStreamReader(in));
				while ((strLine = br.readLine()) != null) {
					Property p = new ObjectProperty(strLine.split(",")[1].trim());
					boolean correct = strLine.split(",")[0].trim().equals("+") ? true : false;
					property2Value.put(p, correct);
					stack.push(p);
					evaluatedProperties.add(p);
				}
				in.close();
			}
			properties.removeAll(evaluatedProperties);
			Property p;
			if(properties.isEmpty()){
				p = evaluatedProperties.getLast();
			} else {
				p = properties.remove(0);
			}
			propertyLabel.setText(p.getURI().toString());
			showSample(p);
			
			
			nextButton.setEnabled(!properties.isEmpty());
			backButton.setEnabled(!stack.isEmpty());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			bw = new BufferedWriter(new FileWriter(evalFile));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private String getDomain(Property p){
		String s = null;
		String q = String.format("SELECT ?domain WHERE {<%s> rdfs:domain ?domain.}", p.getURI().toString());
		ResultSet rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, q));
		if(rs.hasNext()){
			s = rs.next().getResource("domain").getURI();
		}
		return s;
	}
	
	private String getRange(Property p){
		String s = null;
		String q = String.format("SELECT ?range WHERE {<%s> rdfs:range ?range.}", p.getURI().toString());
		ResultSet rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, q));
		if(rs.hasNext()){
			s = rs.next().getResource("range").getURI();
		}
		return s;
	}
	
	private String getDescription(Property p){
		String s = null;
		String q = String.format("SELECT ?desc WHERE {<%s> rdfs:comment ?desc.}", p.getURI().toString());
		ResultSet rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, q));
		if(rs.hasNext()){
			s = rs.next().getLiteral("desc").getLexicalForm();
		}
		return s;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		new EvaluationGUI(new File("dbpedia_evaluation/asymmetric.txt"));
//		new EvaluationGUI(new File("dbpedia_evaluation/inverse_functional.txt"));
		new EvaluationGUI(new File("dbpedia_evaluation/irreflexive.txt"));

	}

}
