package org.vafer.jdeb.utils;

import java.util.Map;

/**
 * Resolve variables based on a Map.
 *  
 * @author Torsten Curdt <tcurdt@vafer.org>
 */

public final class MapVariableResolver implements VariableResolver {

	private final Map map;
	
	public MapVariableResolver( final Map pMap ) {
		map = pMap;
	}
	
	public String get( final String pKey ) {
		return (String) map.get(pKey);
	}

}
