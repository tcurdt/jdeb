/*
 * Copyright 2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vafer.jdeb.descriptors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.vafer.jdeb.utils.MapVariableResolver;

public final class PackageDescriptorTestCase extends TestCase {

	public void testParse() throws Exception {
		
		final InputStream is = new ByteArrayInputStream(
				("Key1: Value1\n" +
				 "Key2: Value2\n" +
				 " Value2.1\n" +
				 " Value2.2\n" +
				 "Key3: Value3\n").getBytes());
		
		final PackageDescriptor d = new PackageDescriptor(is, null);
		assertFalse(d.isValid());
	}
	
	public void testVariableSubstitution() {
		
		final Map map = new HashMap();
		map.put("VERSION", "1.2");
		map.put("MAINTAINER", "Torsten Curdt <tcurdt@vafer.org>");

		final PackageDescriptor d = new PackageDescriptor(new MapVariableResolver(map));
		d.set("Version", "[[VERSION]]");
		d.set("Maintainer", "[[MAINTAINER]]");
		d.set("NoResolve1", "test[[test");
		d.set("NoResolve2", "[[test]]");
		
		assertEquals("1.2", d.get("Version"));
		assertEquals("Torsten Curdt <tcurdt@vafer.org>", d.get("Maintainer"));
		assertEquals("test[[test", d.get("NoResolve1"));
		assertEquals("[[test]]", d.get("NoResolve2"));
	}
}
