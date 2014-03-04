/* ***** BEGIN LICENSE BLOCK *****
*
* Copyright 2014 The jdeb developers.
* Copyright (C) 2014 Linagora
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
