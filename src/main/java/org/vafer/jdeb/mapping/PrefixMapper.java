package org.vafer.jdeb.mapping;

import org.apache.tools.tar.TarEntry;
import org.vafer.jdeb.Utils;

public final class PrefixMapper implements Mapper {

	private final int strip;
	private final String prefix;
	
	public PrefixMapper( final int pStrip, final String pPrefix ) {
		strip = pStrip;
		prefix = pPrefix;
	}
		
	public TarEntry map( final TarEntry pEntry ) {
		
		final String name = pEntry.getName();

		final TarEntry newEntry = new TarEntry(prefix + '/' + Utils.stripPath(strip, name));		
		
		newEntry.setUserId(pEntry.getUserId());
		newEntry.setGroupId(pEntry.getGroupId());
		newEntry.setUserName(pEntry.getUserName());
		newEntry.setGroupName(pEntry.getGroupName());
		newEntry.setMode(pEntry.getMode());
		newEntry.setSize(pEntry.getSize());

		return newEntry;
	}

}
