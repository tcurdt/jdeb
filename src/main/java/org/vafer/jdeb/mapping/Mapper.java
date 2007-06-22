package org.vafer.jdeb.mapping;

import org.apache.tools.tar.TarEntry;

public interface Mapper {

	public TarEntry map( final TarEntry entry );
}
