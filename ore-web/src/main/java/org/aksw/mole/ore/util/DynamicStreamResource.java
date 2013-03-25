package org.aksw.mole.ore.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.vaadin.Application;
import com.vaadin.terminal.DownloadStream;
import com.vaadin.terminal.StreamResource;

public class DynamicStreamResource extends StreamResource {

	  private static final long serialVersionUID = -4304057799149311779L;
	  
	  public static final String MIME_TYPE_BINARY_DATA = "application/octet-stream";
	  public static final String MIME_TYPE_PDF = "application/pdf";
	  public static final String MIME_TYPE_RDF_XML = "application/rdf+xml";

	  private final byte[] binaryData;

	  private final String filename;

	  public DynamicStreamResource(final byte[] binaryData, final String filename, 
	        final String mimeType, final Application application) {
	    super(new StreamSource() {

	      @Override
	      public InputStream getStream() {
	        return new ByteArrayInputStream(binaryData);
	      }
	    }, filename, application);
	    
	    this.binaryData = binaryData;
	    this.filename = filename;
	    
	    setMIMEType(mimeType);
	  }
	  
	  @Override
	  public DownloadStream getStream() {
	    final DownloadStream downloadStream = super.getStream();

	    // Set the "attachment" to force save-dialog. Important for IE7 (and probably IE8)
	    downloadStream.setParameter("Content-Disposition", "attachment; filename=\"" + filename + "\"");

	    // Enable deterministic progressbar for download
	    downloadStream.setParameter("Content-Length", Integer.toString(binaryData.length));
	    
	    return downloadStream;
	  }

	}
