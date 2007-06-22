package org.vafer.jdeb.descriptors;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

public final class ChangesDescriptor extends AbstractDescriptor {

	/*
	Format: 1.7
	Date: Mon, 26 Mar 2007 11:44:04 +0200
	Binary: tvp-conflicts tvp-standard tvp-minimal
	Architecture: all
	Version: 0.1.23
	Distribution: tvp
	Urgency: low
	Maintainer: Thom May <thom@theveniceproject.com>
	Changed-By: Thom May <thom@joost.com>
	Description: 
	 tvp-standard - Minimal core of TVP services
	Changes: 
	 tvp-standard (0.1.23) tvp; urgency=low
	 .
	   * BLA
	   * BLUB
	Files: 
	 b5bcdd0bb123bb16808e8d4deb381d80 3856 base optional tvp-standard_0.1.23_i386.deb
	 
	 */

	private final static String[] keys = {
		"Format",
		"Date",
		"Binary",
		"Architecture",
		"Version",
		"Dsitribution",
		"Urgency",
		"Maintainer",
		"Changed-By",
		"Descrition",
		"Changes",
		"Files"
	};

	public ChangesDescriptor() {		
	}

	public ChangesDescriptor( final AbstractDescriptor pDescriptor ) {
		super(pDescriptor);
	}
	
	public ChangesDescriptor( final InputStream pInput ) throws IOException, ParseException {		
		parse(pInput);
	}
	
	public void addFile( final InputStream pInput, final String pName ) {
		// TODO
	}
	
	public String toString() {
		return toString(keys);
	}
}
