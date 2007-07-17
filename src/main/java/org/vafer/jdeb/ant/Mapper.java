package org.vafer.jdeb.ant;

import java.io.File;
import java.io.FileInputStream;

import org.vafer.jdeb.mapping.LsMapper;
import org.vafer.jdeb.mapping.NullMapper;
import org.vafer.jdeb.mapping.PrefixMapper;

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
