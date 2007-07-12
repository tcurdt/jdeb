package org.vafer.jdeb.ant;

import java.io.File;

import org.apache.tools.ant.types.PatternSet;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;

public final class Data extends PatternSet implements DataProducer {

	private File src;
	
	public void setSrc( final File pSrc ) {
		src = pSrc;
	}
	
	public void produce( final DataConsumer receiver ) {
		if (src.isFile()) {
			new DataProducerArchive(src).produce(receiver);
		} else {
			new DataProducerDirectory(src, this).produce(receiver);			
		}
	}

	/**
	 * @deprecated
	 * @param prefix
	 */
	public void setPrefix( final String prefix ) {		
	}
}
