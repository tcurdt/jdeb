package org.vafer.jdeb.ant;

import java.io.File;

import org.apache.tools.ant.types.PatternSet;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.producers.DataProducerArchive;
import org.vafer.jdeb.producers.DataProducerDirectory;

public final class Data extends PatternSet implements DataProducer {

	private File src;
	private Mapper mapper;
	
	public void setSrc( final File pSrc ) {
		src = pSrc;
	}

	public void setMapper( final Mapper pMapper ) {
		mapper = pMapper;
	}
	
	public void produce( final DataConsumer pReceiver ) {
		
		if (!src.exists()) {
			System.err.println("ATTENTION: \"" + src + " \" is not existing. Ignoring unexisting data providers is deprecated. This will fail your build in later releases.");
			return;
		}
		
		if (src.isFile()) {
			new DataProducerArchive(
				src,
				getIncludePatterns(getProject()),
				getExcludePatterns(getProject()),
				(mapper != null) ? mapper.createMapper() : null
				).produce(pReceiver);
		} else {
			new DataProducerDirectory(
				src,
				getIncludePatterns(getProject()),
				getExcludePatterns(getProject()),
				(mapper != null) ? mapper.createMapper() : null
				).produce(pReceiver);			
		}
	}

	
	/**
	 * @deprecated
	 */
	public void setPrefix( final String pPrefix ) {
		System.err.println("ATTENTION: the prefix attribue is deprecated.");
		if (mapper == null) {
			mapper = new Mapper();
			mapper.setType("prefix");
		}
		mapper.setPrefix(pPrefix);
	}
	
	/**
	 * @deprecated
	 */
	public void setStrip( final int pStrip ) {
		System.err.println("ATTENTION: the prefix attribue is deprecated.");
		if (mapper == null) {
			mapper = new Mapper();
			mapper.setType("prefix");
		}
		mapper.setStrip(pStrip);
	}


}
