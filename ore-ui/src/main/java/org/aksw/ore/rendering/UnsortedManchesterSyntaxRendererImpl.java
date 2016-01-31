package org.aksw.ore.rendering;

import org.aksw.mole.ore.util.PrefixedShortFromProvider;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.CollectionFactory;
import org.semanticweb.owlapi.util.ShortFormProvider;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Extended OWL API class, but omit sorting.
 * @author Lorenz Bühmann
 *
 */
public class UnsortedManchesterSyntaxRendererImpl implements OWLObjectRenderer{
	
	private UnsortedManchesterSyntaxRenderer ren;

    private WriterDelegate writerDelegate;
    
    private OWLEntity firstEntity;

    public UnsortedManchesterSyntaxRendererImpl() {
        writerDelegate = new WriterDelegate();
        ren = new UnsortedManchesterSyntaxRenderer(writerDelegate, new PrefixedShortFromProvider());
    }


    public synchronized String render(OWLObject object) {
        writerDelegate.reset();
        object.accept(ren);
        return writerDelegate.toString();
    }
    
    public synchronized String render(OWLObject object, OWLEntity firstEntity) {
    	this.firstEntity = firstEntity;
        writerDelegate.reset();
        object.accept(ren);
        return writerDelegate.toString();
    }


    public synchronized void setShortFormProvider(ShortFormProvider shortFormProvider) {
        ren = new UnsortedManchesterSyntaxRenderer(writerDelegate, shortFormProvider);
    }
	
	
	private static class WriterDelegate extends Writer {

        private StringWriter delegate;

        public WriterDelegate() {
			// TODO Auto-generated constructor stub
		}


		private void reset() {
            delegate = new StringWriter();
        }


        @Override
		public String toString() {
            return delegate.getBuffer().toString();
        }


        @Override
		public void close() throws IOException {
            delegate.close();
        }


        @Override
		public void flush() throws IOException {
            delegate.flush();
        }


        @Override
		public void write(char cbuf[], int off, int len) throws IOException {
            delegate.write(cbuf, off, len);
        }
    }
	
	class UnsortedManchesterSyntaxRenderer extends ManchesterOWLSyntaxObjectRenderer{
		public UnsortedManchesterSyntaxRenderer(Writer writer, ShortFormProvider entityShortFormProvider) {
			super(writer, entityShortFormProvider);
		}

//		@Override
//		protected static List<T extends OWLObject> sort(Collection<T> objects) {
//			List<OWLObject> sorted = new ArrayList<OWLObject>();
//			sorted.add(firstEntity);
//			for(OWLObject o : objects){
//				if(!sorted.contains(o)){
//					sorted.add(o);
//				}
//			}
//			return sorted;
//		}
	}
	

}


