/*
 * Copyright 2019 The jdeb developers.
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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vafer.jdeb.debian.BinaryPackageControlFile;
import org.vafer.jdeb.debian.ChangesFile;

public class ChangesFileBuilderTestCase {
    
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
    public void testChangedByNotSet() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));

        final String input =
                "release distribution=production, date=14:00 13.01.2007, version=12324, urgency=low\n" +
                " * change1\n" +
                " * change2\n";

        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();
        packageControlFile.set("Package", "package");
        packageControlFile.set("Version", "version");
        packageControlFile.set("Date", "Mon, 20 Aug 2007 15:25:57 +0200");

        final TextfileChangesProvider provider = new TextfileChangesProvider(new ByteArrayInputStream(input.getBytes(UTF_8)), packageControlFile, StandardCharsets.UTF_8);
        final ChangeSet[] changeSets = provider.getChangesSets();
        
        assertNotNull(changeSets);
        assertEquals(1, changeSets.length);
        assertEquals(1168718400000L, changeSets[0].getDate().getTime());
        
        ChangesFile changesFile = new ChangesFile();
        changesFile.setChanges(provider.getChangesSets());
        changesFile.initialize(packageControlFile);

        assertNotNull(changesFile);
        assertEquals(null, changesFile.get("Changed-By"));
    }
    
    @Test
    public void testChangedByFromControl() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));

        final String input =
                "release distribution=production, date=14:00 13.01.2007, version=12324, urgency=low\n" +
                " * change1\n" +
                " * change2\n";

        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();
        packageControlFile.set("Package", "package");
        packageControlFile.set("Version", "version");
        packageControlFile.set("Maintainer", "tcurdt@joost.com");
        packageControlFile.set("Date", "Mon, 20 Aug 2007 15:25:57 +0200");

        final TextfileChangesProvider provider = new TextfileChangesProvider(new ByteArrayInputStream(input.getBytes(UTF_8)), packageControlFile, StandardCharsets.UTF_8);
        final ChangeSet[] changeSets = provider.getChangesSets();
        
        assertNotNull(changeSets);
        assertEquals(1, changeSets.length);
        assertEquals(1168718400000L, changeSets[0].getDate().getTime());
        
        ChangesFile changesFile = new ChangesFile();
        changesFile.setChanges(provider.getChangesSets());
        changesFile.initialize(packageControlFile);

        assertNotNull(changesFile);
        assertEquals("tcurdt@joost.com", changesFile.get("Changed-By"));
    }
    
    @Test
    public void testChangedByFromChangesProvider() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));

        final String input =
                "release distribution=production, date=14:00 13.01.2007, version=12324, urgency=low, by=mrasko@test.com\n" +
                " * change1\n" +
                " * change2\n";

        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();
        packageControlFile.set("Package", "package");
        packageControlFile.set("Version", "version");
        packageControlFile.set("Maintainer", "tcurdt@joost.com");
        packageControlFile.set("Date", "Mon, 20 Aug 2007 15:25:57 +0200");

        final TextfileChangesProvider provider = new TextfileChangesProvider(new ByteArrayInputStream(input.getBytes(UTF_8)), packageControlFile, StandardCharsets.UTF_8);
        final ChangeSet[] changeSets = provider.getChangesSets();
        
        assertNotNull(changeSets);
        assertEquals(1, changeSets.length);
        assertEquals(1168718400000L, changeSets[0].getDate().getTime());
        
        ChangesFile changesFile = new ChangesFile();
        changesFile.setChanges(provider.getChangesSets());
        changesFile.initialize(packageControlFile);

        assertNotNull(changesFile);
        assertEquals("mrasko@test.com", changesFile.get("Changed-By"));
    }
    
    @Test
    public void testReproducible() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));

        final String input =
                "release distribution=production, date=14:00 13.01.2007, version=12324, urgency=low\n" +
                " * change1\n" +
                " * change2\n";

        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();
        packageControlFile.set("Package", "package");
        packageControlFile.set("Version", "version");
        packageControlFile.set("Date", "Mon, 20 Aug 2007 15:25:57 +0200");

        final TextfileChangesProvider provider = new TextfileChangesProvider(new ByteArrayInputStream(input.getBytes(UTF_8)), packageControlFile, 1175385600000L, StandardCharsets.UTF_8);
        final ChangeSet[] changeSets = provider.getChangesSets();
        
        assertNotNull(changeSets);
        assertEquals(1, changeSets.length);
        assertEquals(1168696800000L, changeSets[0].getDate().getTime());
        
        ChangesFile changesFile = new ChangesFile();
        changesFile.setChanges(provider.getChangesSets());
        changesFile.initialize(packageControlFile);

        assertNotNull(changesFile);
        assertEquals(null, changesFile.get("Changed-By"));
    }
}
