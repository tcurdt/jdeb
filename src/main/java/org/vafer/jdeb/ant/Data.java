/*
 * Copyright 2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vafer.jdeb.ant;

import java.io.File;

import org.apache.tools.ant.types.PatternSet;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.producers.DataProducerArchive;
import org.vafer.jdeb.producers.DataProducerDirectory;


/**
 * Ant "data" elment acting as a factory for DataProducers.
 * So far Archive and Directory producers are supported.
 * Both support the usual ant pattern set matching.
 * 
 * @author tcurdt
 */
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
		System.err.println("ATTENTION: the prefix attribute is deprecated. Please specify it on a mapper element inside the data element.");
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
		System.err.println("ATTENTION: the strip attribute is deprecated. Please specify it on a mapper element inside the data element.");
		if (mapper == null) {
			mapper = new Mapper();
			mapper.setType("prefix");
		}
		mapper.setStrip(pStrip);
	}


}
