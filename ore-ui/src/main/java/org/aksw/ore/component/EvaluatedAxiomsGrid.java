package org.aksw.ore.component;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.aksw.ore.ORESession;
import org.aksw.ore.rendering.Renderer;
import org.aksw.ore.util.AxiomScoreExplanationGenerator;
import org.dllearner.core.EvaluatedAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.google.common.collect.Sets;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.themes.ValoTheme;

import static org.aksw.ore.component.EvaluatedAxiomsTable.Columns.*;

public class EvaluatedAxiomsGrid extends Grid{
	
	enum Columns{
		SELECTED, ACCURACY, AXIOM
	}
	
	private static final DecimalFormat df = new DecimalFormat("0.00%");
	
	private Set<Object> selectedObjects = new HashSet<>();
	
	private AxiomType<? extends OWLAxiom> axiomType;
	private Collection<EvaluatedAxiom<OWLAxiom>> axioms;
	
	private Renderer renderer = ORESession.getRenderer();
	
	public EvaluatedAxiomsGrid(final AxiomType<? extends OWLAxiom> axiomType, Collection<EvaluatedAxiom<OWLAxiom>> axioms) {
		this.axiomType = axiomType;
		this.axioms = axioms;
		
		addStyleName("enrichment-axioms-table");
		addStyleName(ValoTheme.TABLE_BORDERLESS);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);
        
        setHeaderVisible(false);
		
		
		setWidth("100%");
		setHeightUndefined();
//		setPageLength(0);
        setSelectionMode(SelectionMode.MULTI);
        setImmediate(true);
        setColumnReorderingAllowed(false);
		
		IndexedContainer container = new IndexedContainer();
		container.addContainerProperty(Columns.AXIOM, OWLAxiom.class, null);
		container.addContainerProperty(Columns.ACCURACY, Double.class, null);
		setContainerDataSource(container);
		
		Set<String> renderedAxioms = Sets.newHashSetWithExpectedSize(axioms.size());
		final Set<EvaluatedAxiom<OWLAxiom>> axiomsToBePrefixed = Sets.newHashSetWithExpectedSize(axioms.size());
		for (EvaluatedAxiom<OWLAxiom> axiom : axioms) {
			String s = renderer.render(axiom.getAxiom());
			if(!renderedAxioms.add(s)){
				axiomsToBePrefixed.add(axiom);
			}
		}
		
		getColumn(Columns.AXIOM).setExpandRatio(1);
		
		Item item;
		for(EvaluatedAxiom<OWLAxiom> ax : axioms){
			item = container.addItem(ax);
			item.getItemProperty(Columns.ACCURACY).setValue(ax.getScore().getAccuracy());
			item.getItemProperty(Columns.AXIOM).setValue(ax.getAxiom());
		}
		
		
		getColumn(Columns.AXIOM).setRenderer(new HtmlRenderer(), new Converter<String, OWLAxiom>() {

			@Override
			public OWLAxiom convertToModel(String value, Class<? extends OWLAxiom> targetType, Locale locale)
					throws com.vaadin.data.util.converter.Converter.ConversionException {
				return null;
			}

			@Override
			public String convertToPresentation(OWLAxiom value, Class<? extends String> targetType, Locale locale)
					throws com.vaadin.data.util.converter.Converter.ConversionException {
				String s = renderer.renderHTML(value);
				return s;
			}

			@Override
			public Class<OWLAxiom> getModelType() {
				return OWLAxiom.class;
			}

			@Override
			public Class<String> getPresentationType() {
				return String.class;
			}
		});
		
	}
	
	public Set<Object> getSelectedObjects() {
		return selectedObjects;
	}
	
	public static void main(String[] args) {
		System.out.println(Pattern.compile("(\\s)([x,y,z])([\\s|,|.])").matcher(" ,").replaceAll("$1<i>$2</i>$3"));
	}
	
	class EvaluatedAxiomRenderer extends AbstractRenderer<EvaluatedAxiom> {

		/**
		 * @param presentationType
		 * @param nullRepresentation
		 */
		protected EvaluatedAxiomRenderer() {
			super(EvaluatedAxiom.class, null);
			
		}
		
	}

}
