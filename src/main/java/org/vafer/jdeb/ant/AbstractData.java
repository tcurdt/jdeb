package org.vafer.jdeb.ant;

import java.io.File;

import org.vafer.jdeb.DataProducer;

public abstract class AbstractData {

	private File data;
//	private String prefix = "";
//	private int strip = 0;
//
//	
//	public void setStrip( int strip ) {
//		this.strip = strip;
//	}
//
//	public int getStrip() {
//		return strip;
//	}
//
//	
//	public void setPrefix( String prefix ) {
//		if (!prefix.endsWith("/")) {
//    		this.prefix = prefix + "/";
//    		return;
//		}
//		
//		this.prefix = prefix;
//	}
//
//	public String getPrefix() {
//		return prefix;
//	}
//
//	
//	public void setSrc( File data ) {
//		this.data = data;
//	}
//	
//	public File getSrc() {
//		return data;
//	}
		
	public abstract DataProducer getDataProducer();	
		
	public String toString() {
		return data.toString();
	}


}
