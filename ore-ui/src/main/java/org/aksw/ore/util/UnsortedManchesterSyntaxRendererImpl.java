package org.aksw.ore.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aksw.mole.ore.util.PrefixedShortFromProvider;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.ShortFormProvider;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxObjectRenderer;

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
		
		@Override
		protected List<? extends OWLObject> sort(Collection<? extends OWLObject> objects) {
			List<OWLObject> sorted = new ArrayList<OWLObject>();
			sorted.add(firstEntity);
			for(OWLObject o : objects){
				if(!sorted.contains(o)){
					sorted.add(o);
				}
			}System.out.println(sorted);
			return sorted;
		}
	}
	

}


