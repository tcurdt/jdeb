package org.vafer.jdeb.mapping;

import org.apache.tools.tar.TarEntry;

public final class NullMapper implements Mapper {

	public TarEntry map( final TarEntry pEntry ) {
		return pEntry;
	}

}
