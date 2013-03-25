import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.core.owl.Property;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;

import com.hp.hpl.jena.query.ResultSet;


public class EvaluationGUI2 extends JFrame{
	
	private List<Property> properties = new ArrayList<Property>();
	
	private JLabel propertyLabel;
	private JEditorPane sampleLabel;;
	private JCheckBox cb;
	private JButton nextButton;
	private JButton backButton;
	
	Stack<Property> stack = new Stack<Property>();
	
	private ExtractionDBCache cache = new ExtractionDBCache("cache");
	private SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();
	
	private DBManager man = new DBManager();
	
	private String name;

	public EvaluationGUI2(String name) {
		this.name = name;
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(new Dimension(1200, 300));
		setTitle(name);
		
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
		
		loadProperties();
		
		Property p = properties.remove(0);
		showProperty(p);
		
		pack();
		
		setVisible(true);
	}
	
	private void finish(){
		Property oldProperty = new ObjectProperty(propertyLabel.getText());
		man.setCorrect(oldProperty, name, cb.isSelected());
		stack.push(oldProperty);
		dispose();
	}
	
	private void showNextProperty(){
		Property oldProperty = new ObjectProperty(propertyLabel.getText());
		man.setCorrect(oldProperty, name, cb.isSelected());
		stack.push(oldProperty);
		
		Property newProperty = properties.remove(0);
		showProperty(newProperty);
		
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
		Property oldProperty = new ObjectProperty(propertyLabel.getText());
		man.setCorrect(oldProperty, name, cb.isSelected());
		
		Property newProperty = stack.pop();
		propertyLabel.setText(newProperty.getURI().toString());
		showSample(newProperty);
		cb.setSelected(man.isCorrect(newProperty, name));
		properties.add(0, newProperty);
		
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
		text += "<p><b>Sample:</b><br/>" + man.getSampleViolationHTML(p, name).replace("\n", "<br/>") + "</p>";
		text += "</html>";
		sampleLabel.setText(text);
	}
	
	
	
	private void loadProperties(){
		properties = man.loadProperties(name);
	}
	
	private void showProperty(Property p){
		propertyLabel.setText(p.getURI().toString());
		cb.setSelected(man.isCorrect(p, name));
		showSample(p);
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
		new EvaluationGUI2("inverse_functional");

	}

}
