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
package org.vafer.jdeb.ar;

import java.io.File;
import java.io.FileInputStream;

import junit.framework.TestCase;

public final class ArInputStreamTestCase extends TestCase {

	public void testRead() throws Exception {
		final File archive = new File(getClass().getResource("data.ar").toURI());

		final ArInputStream ar = new ArInputStream(new FileInputStream(archive));
		final ArEntry entry1 = ar.getNextEntry();

		assertEquals("data.tgz", entry1.getName());
		assertEquals(148, entry1.getLength());

		for (int i = 0; i < entry1.getLength(); i++) {
			ar.read();			
		}
		
		final ArEntry entry2 = ar.getNextEntry();
		
		assertNull(entry2);
		
		ar.close();
	}
}
