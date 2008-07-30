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
import java.io.FileInputStream;

import org.vafer.jdeb.mapping.LsMapper;
import org.vafer.jdeb.mapping.NullMapper;
import org.vafer.jdeb.mapping.PrefixMapper;

/**
 * Ant "mapper" element acting as factory for the entry mapper.
 * So far type "ls" and "prefix" are supported.
 * 
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public final class Mapper {

	private String mtype;
	private String prefix;
	private int strip;
	private File src;
	
	public void setType( final String pType ) {
		mtype = pType;
	}
	
	public void setPrefix( final String pPrefix ) {
		prefix = pPrefix;
	}

	public void setStrip( final int pStrip ) {
		strip = pStrip;
	}
		
	public void setSrc( final File pSrc ) {
		src = pSrc;
	}

	public org.vafer.jdeb.mapping.Mapper createMapper() {
		if ("prefix".equalsIgnoreCase(mtype)) {
			return new PrefixMapper(strip, prefix);
		} else if ("ls".equalsIgnoreCase(mtype)) {
			try {
				return new LsMapper(new FileInputStream(src));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new NullMapper();
	}
	
}
