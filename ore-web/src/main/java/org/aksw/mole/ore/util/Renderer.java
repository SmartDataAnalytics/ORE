package org.aksw.mole.ore.util;

import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.cache.OWLEntityShortFormProvider;
import org.aksw.mole.ore.rendering.KeywordColorMap;
import org.dllearner.core.owl.Axiom;
import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.Individual;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.dllearner.utilities.owl.OWLAPIDescriptionConvertVisitor;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.ShortFormProvider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

public class Renderer {
	
	public enum Syntax{
		MANCHESTER, DL
	}
	
	private LoadingCache<OWLObject, String> manchesterCache = CacheBuilder.newBuilder()
		       .maximumSize(1000)
		       .expireAfterWrite(10, TimeUnit.MINUTES)
		       .build(
		           new CacheLoader<OWLObject, String>() {
		             public String load(OWLObject object){
		               return renderManchesterSyntax(object);
		             }
		           });
	
	private LoadingCache<OWLObject, String> manchesterLongFormCache = CacheBuilder.newBuilder()
		       .maximumSize(1000)
		       .expireAfterWrite(10, TimeUnit.MINUTES)
		       .build(
		           new CacheLoader<OWLObject, String>() {
		             public String load(OWLObject object){
		               return renderManchesterSyntaxLongForm(object);
		             }
		           });
	
	private LoadingCache<OWLObject, String> dlCache = CacheBuilder.newBuilder()
		       .maximumSize(1000)
		       .expireAfterWrite(10, TimeUnit.MINUTES)
		       .build(
		           new CacheLoader<OWLObject, String>() {
		             public String load(OWLObject object){
		               return renderDLSyntax(object);
		             }
		           });
	
	
	private OWLObjectRenderer manchesterSyntaxRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
	private OWLObjectRenderer manchesterSyntaxRendererPrefixed = new ManchesterOWLSyntaxOWLObjectRendererImpl();
	private OWLObjectRenderer dlSyntaxRenderer = new DLSyntaxObjectRenderer(); 
	
	private KeywordColorMap colorMap = new KeywordColorMap();
	
	public Renderer() {
		OWLEntityShortFormProvider sfp = new OWLEntityShortFormProvider();
		manchesterSyntaxRenderer.setShortFormProvider(sfp);
		dlSyntaxRenderer.setShortFormProvider(sfp);
		manchesterSyntaxRendererPrefixed.setShortFormProvider(new ShortFormProvider() {
			@Override
			public String getShortForm(OWLEntity entity) {
				return entity.toStringID();
			}
			@Override
			public void dispose() {
			}
		});
	}
	
	public String render(Description desc, Syntax syntax){
		OWLClassExpression expr = OWLAPIDescriptionConvertVisitor.getOWLClassExpression(desc);
		return renderManchesterSyntax(expr);
	}
	
	public String render(Description desc, Syntax syntax, boolean html){
		OWLClassExpression expr = OWLAPIDescriptionConvertVisitor.getOWLClassExpression(desc);
		if(html){
			return renderManchesterSyntax(expr);
		} else {
			return manchesterSyntaxRenderer.render(expr);
		}
	}
	
	public String render(Individual ind, Syntax syntax){
		OWLIndividual i = OWLAPIConverter.getOWLAPIIndividual(ind);
		return renderManchesterSyntax(i);
	}
	
	public String render(Individual ind, Syntax syntax, boolean html){
		OWLIndividual i = OWLAPIConverter.getOWLAPIIndividual(ind);
		if(html){
			return renderManchesterSyntax(i);
		} else {
			return manchesterSyntaxRenderer.render(i);
		}
		
	}
	
	public String render(Axiom axiom, Syntax syntax){
		return render(axiom ,syntax, false);
	}
	
	public String render(Axiom axiom, Syntax syntax, boolean longForm){
		OWLAxiom ax = OWLAPIConverter.getOWLAPIAxiom(axiom);
		return render(ax, syntax, longForm);
	}
	
	public String render(OWLObject object, Syntax syntax){
		return render(object, syntax, false);
	}
	
	public String render(OWLObject object, Syntax syntax, boolean longForm){
		try {
			if(syntax == Syntax.DL){
				return dlCache.get(object);
			} else if(syntax == Syntax.MANCHESTER){
				if(longForm){
					return manchesterLongFormCache.get(object);
				} else {
					return manchesterCache.get(object);
				}
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String renderManchesterSyntaxLongForm(OWLObject object){
		return renderManchesterSyntaxHTML(manchesterSyntaxRendererPrefixed.render(object));
	}
	
	private String renderManchesterSyntax(OWLObject object){
		return renderManchesterSyntaxHTML(manchesterSyntaxRenderer.render(object));
	}
	
	private String renderDLSyntax(OWLObject object){
		return dlSyntaxRenderer.render(object);
	}
	
	private String renderManchesterSyntaxHTML(String renderedString){
		StringTokenizer st = new StringTokenizer(renderedString);
		StringBuffer bf = new StringBuffer();
		
		bf.append("<html>");
		
		String token;
		while(st.hasMoreTokens()){
			token = st.nextToken();
			String color = "black";
			
			boolean isReserved = false;
			String c = colorMap.get(token);
			if(c != null){
				isReserved = true;
				color = c;
			}
			if(isReserved){
				bf.append("<b><font color=" + color + ">" + token + " </font></b>");
			} else {
				bf.append(" " + token + " ");
			}
		}
		bf.append("</html>");
		renderedString = bf.toString();

		return renderedString;
	}

}
