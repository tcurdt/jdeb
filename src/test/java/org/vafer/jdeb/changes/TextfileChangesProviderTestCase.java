/*
 * Copyright 2007-2024 The jdeb developers.
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
package org.vafer.jdeb.changes;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import org.vafer.jdeb.debian.BinaryPackageControlFile;

import static java.nio.charset.StandardCharsets.*;

public final class TextfileChangesProviderTestCase extends Assert {

    private TimeZone defaultTimeZone;

    @Before
    public void before() {
        defaultTimeZone = TimeZone.getDefault();
    }

    @After
    public void after() {
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void testParsing() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));

        final String input =
            " * change1\n" +
                " * change2\n" +
                "release date=14:00 13.01.2007, version=12324, urgency=low, by=tcurdt@joost.com\n" +
                " * change1\n" +
                " * change2\n" +
                "release date=12:00 10.01.2007, version=10324, urgency=low, by=tcurdt@joost.com\n" +
                " * change1\n" +
                " * change2\n";

        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();
        packageControlFile.set("Package", "package");
        packageControlFile.set("Version", "version");
        packageControlFile.set("Distribution", "distribution");
        packageControlFile.set("Date", "Mon, 20 Aug 2007 15:25:57 +0200");

        final TextfileChangesProvider provider = new TextfileChangesProvider(new ByteArrayInputStream(input.getBytes(UTF_8)), packageControlFile, StandardCharsets.UTF_8);
        final ChangeSet[] changeSets = provider.getChangesSets();

        assertNotNull(changeSets);
        assertEquals(3, changeSets.length);
        assertEquals(1168718400000L, changeSets[1].getDate().getTime());
        assertEquals(1168452000000L, changeSets[2].getDate().getTime());
    }

    @Test
    public void testDistributionFromChangesProvider() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));

        final String input =
            "release distribution=production\n" +
                " * change1\n" +
                " * change2\n" +
                "release distribution=staging, date=14:00 13.01.2007, version=12324, urgency=low, by=tcurdt@joost.com\n" +
                " * change1\n" +
                " * change2\n" +
                "release distribution=development, date=12:00 10.01.2007, version=10324, urgency=low, by=tcurdt@joost.com\n" +
                " * change1\n" +
                " * change2\n";

        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();
        packageControlFile.set("Package", "package");
        packageControlFile.set("Version", "version");
        packageControlFile.set("Date", "Mon, 20 Aug 2007 15:25:57 +0200");

        final TextfileChangesProvider provider = new TextfileChangesProvider(new ByteArrayInputStream(input.getBytes(UTF_8)), packageControlFile, StandardCharsets.UTF_8);
        final ChangeSet[] changeSets = provider.getChangesSets();

        assertNotNull(changeSets);
        assertEquals(3, changeSets.length);

        assertEquals("production", changeSets[0].getDistribution());
        assertEquals("staging", changeSets[1].getDistribution());
        assertEquals(1168718400000L, changeSets[1].getDate().getTime());
        assertEquals("development", changeSets[2].getDistribution());
        assertEquals(1168452000000L, changeSets[2].getDate().getTime());
    }

    @Test
    public void testReproducible() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));

        final String input =
            " * change1\n" +
                " * change2\n" +
                "release date=14:00 13.01.2007, version=12324, urgency=low, by=tcurdt@joost.com\n" +
                " * change1\n" +
                " * change2\n" +
                "release date=12:00 10.01.2007, version=10324, urgency=low, by=tcurdt@joost.com\n" +
                " * change1\n" +
                " * change2\n";

        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();
        packageControlFile.set("Package", "package");
        packageControlFile.set("Version", "version");
        packageControlFile.set("Distribution", "distribution");
        packageControlFile.set("Date", "Mon, 20 Aug 2007 15:25:57 +0200");

        final TextfileChangesProvider provider = new TextfileChangesProvider(new ByteArrayInputStream(input.getBytes(UTF_8)), packageControlFile, 1175385600000L, StandardCharsets.UTF_8);
        final ChangeSet[] changeSets = provider.getChangesSets();

        assertNotNull(changeSets);
        assertEquals(3, changeSets.length);
        assertEquals(1175385600000L, changeSets[0].getDate().getTime());
        assertEquals(1168696800000L, changeSets[1].getDate().getTime());
        assertEquals(1168430400000L, changeSets[2].getDate().getTime());
    }

}
