package org.vafer.jdeb.descriptors;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

public final class PackageDescriptor extends AbstractDescriptor {

	/*
Package: @NAME@
Version: @VERSION@+r@REVISION@
Section: misc
Priority: optional
Architecture: @ARCH@
Installed-Size: @SIZE@
Depends: tvp-jrockit-jdk (>= 1.5)
Maintainer: Torsten Curdt <torsten@joost.com>
Description: revision @REVISION@, jetty java servlet container
	 */
	
	private final static String[] keys = {
		"Package",
		"Version",
		"Section",
		"Priority",
		"Architecture",
		"Installed-Size",
		"Depends",
		"Maintainer",
		"Description"
	};
	
	public PackageDescriptor() {
	}

	public PackageDescriptor( InputStream is )  throws IOException, ParseException {		
		parse(is);
	}

	public String toString() {
		return toString(keys);
	}
}
