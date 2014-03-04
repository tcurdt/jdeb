/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version, provided you comply 
 * with the Additional Terms applicable for OBM connector by Linagora 
 * pursuant to Section 7 of the GNU Affero General Public License, 
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain 
 * the “Message sent thanks to OBM, Free Communication by Linagora” 
 * signature notice appended to any and all outbound messages 
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between 
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain 
 * from infringing Linagora intellectual property rights over its trademarks 
 * and commercial brands. Other Additional Terms apply, 
 * see <http://www.linagora.com/licenses/> for more details. 
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details. 
 *
 * You should have received a copy of the GNU Affero General Public License 
 * and its applicable Additional Terms for OBM along with this program. If not, 
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3 
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to 
 * OBM connectors. 
 * 
 * ***** END LICENSE BLOCK ***** */
package org.vafer.jdeb.utils;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class MapVariableResolverTest extends TestCase {

	public void testRead() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "b");
		MapVariableResolver testee = new MapVariableResolver(map);

		assertEquals("b", testee.get("a"));
	}

	public void testWhenValueContainsNotWellFormatedVariable() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "}b${");
		MapVariableResolver testee = new MapVariableResolver(map);

		assertEquals("}b${", testee.get("a"));
	}

	public void testWhenValueContainsEmptyVariable() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "b${}");
		MapVariableResolver testee = new MapVariableResolver(map);

		assertEquals("b${}", testee.get("a"));
	}

	public void testWhenValueContainsUnknownVariable() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "${b}");
		MapVariableResolver testee = new MapVariableResolver(map);

		assertEquals("${b}", testee.get("a"));
	}

	public void testWhenValueContainsKnownVariable() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "${b}");
		map.put("b", "c");
		MapVariableResolver testee = new MapVariableResolver(map);

		assertEquals("c", testee.get("a"));
	}

	public void testWhenValueContainsKnownVariableAndMore() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "abc${b}d${e}f");
		map.put("b", "${c}");
		map.put("c", "Z");
		map.put("e", "X");
		MapVariableResolver testee = new MapVariableResolver(map);

		assertEquals("abcZdXf", testee.get("a"));
	}
	
	public void testWhenValueBeginsKnownVariableAndMore() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "${b}${e}abcdf");
		map.put("b", "${c}");
		map.put("c", "Z");
		map.put("e", "X");
		MapVariableResolver testee = new MapVariableResolver(map);
		
		assertEquals("ZXabcdf", testee.get("a"));
	}
	
	public void testWhenValueEndsKnownVariableAndMore() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "abcdf${b}${e}");
		map.put("b", "${c}");
		map.put("c", "Z");
		map.put("e", "X");
		MapVariableResolver testee = new MapVariableResolver(map);
		
		assertEquals("abcdfZX", testee.get("a"));
	}
}
