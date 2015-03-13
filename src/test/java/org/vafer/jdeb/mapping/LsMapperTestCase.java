/*
 * Copyright 2015 The jdeb developers.
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
package org.vafer.jdeb.mapping;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.vafer.jdeb.mapping.LsMapper.ParseError;

public final class LsMapperTestCase extends TestCase {

    private final static String output =
        "total 0\n" +
            "drwxr-xr-x   23 tcurdt  tcurdt   782 Jun 25 03:48 .\n" +
            "drwxr-xr-x    3 tcurdt  tcurdt   102 Jun 25 03:48 ..\n" +
            "\n" +
            "./trunk/target/test-classes/org/vafer/dependency:\n" +
            "total 176\n" +
            "drwxr-xr-x   23 tcurdt  tcurdt   782 Jun 25 03:48 .\n" +
            "drwxr-xr-x    3 tcurdt  tcurdt   102 Jun 25 03:48 ..\n" +
            "-rw-r--r--    1 tcurdt  tcurdt  2934 Jun 25 03:48 DependenciesTestCase.class\n" +
            "-rw-r--r--    1 tcurdt  tcurdt   786 Jun 25 03:48 JarCombiningTestCase$1.class\n" +
            "drwxr-xr-x    4 tcurdt  tcurdt   136 Jun 25 03:48 classes\n" +
            "\n" +
            "./trunk/src/test-classes/org/vafer/dependency:\n" +
            "total 76\n" +
            "drwxr-xr-x   23 tcurdt  tcurdt   782 Jun 25 03:48 .\n" +
            "drwxr-xr-x    3 tcurdt  tcurdt   102 Jun 25 03:48 ..\n" +
            "-rw-r--r--    1 tcurdt  tcurdt  2934 Jun 25 03:48 DependenciesTestCase.class\n" +
            "-rw-r--r--    1 tcurdt  tcurdt   786 Jun 25 03:48 JarCombiningTestCase$1.class\n" +
            "drwxr-xr-x    4 tcurdt  tcurdt   136 Jun 25 03:48 classes\n" +
            "\n";

    public void testModes() throws Exception {
        final ByteArrayInputStream is = new ByteArrayInputStream(output.getBytes("UTF-8"));

        final Mapper mapper = new LsMapper(is);

        final TarArchiveEntry entry1 = mapper.map(new TarArchiveEntry("trunk/target/test-classes/org/vafer/dependency", true));

        assertEquals(493, entry1.getMode());
        assertEquals("tcurdt", entry1.getUserName());
        assertEquals("tcurdt", entry1.getGroupName());

        final TarArchiveEntry entry2 = mapper.map(new TarArchiveEntry("trunk/target/test-classes/org/vafer/dependency/DependenciesTestCase.class", true));

        assertEquals(420, entry2.getMode());
        assertEquals("tcurdt", entry2.getUserName());
        assertEquals("tcurdt", entry2.getGroupName());
    }

    public void testSuccessfulParsing() throws Exception {
        final ByteArrayInputStream is = new ByteArrayInputStream(output.getBytes("UTF-8"));

        final Mapper mapper = new LsMapper(is);

        final TarArchiveEntry unknown = new TarArchiveEntry("xyz", true);
        assertSame(unknown, mapper.map(unknown));

        final TarArchiveEntry known = new TarArchiveEntry("trunk/target/test-classes/org/vafer/dependency", true);
        final TarArchiveEntry knownMapped = mapper.map(known);

        assertNotSame(known, knownMapped);

    }

    public void testPrematureEOF() throws Exception {
        final ByteArrayInputStream is = new ByteArrayInputStream(output.substring(0, 200).getBytes("UTF-8"));

        try {
            new LsMapper(is);
            fail("should fail to parse");
        } catch (ParseError e) {
        }
    }

    public void testWrongFormat() throws Exception {
        final ByteArrayInputStream is = new ByteArrayInputStream("asas\n".getBytes("UTF-8"));

        try {
            new LsMapper(is);
            fail("should fail to parse");
        } catch (ParseError e) {
        }
    }
}
