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

package org.vafer.jdeb.debian;

import java.util.Date;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import org.vafer.jdeb.changes.ChangeSet;

public final class ChangesFileTestCase extends Assert {

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
    public void testToString() throws Exception {
        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();
        packageControlFile.set("Package", "test-package");
        packageControlFile.set("Description", "This is\na description\non several lines");
        packageControlFile.set("Version", "1.0");
        packageControlFile.set("XC-UserDefinedField", "This is a user defined field.");

        ChangesFile changes = new ChangesFile();
        changes.setChanges(new ChangeSet[0]);
        changes.initialize(packageControlFile);

        assertEquals("1.0", changes.get("Version"));
        assertEquals("This is a user defined field.", changes.get("UserDefinedField"));
    }

    @Test
    public void testFormatDate() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));

        assertEquals("Mon, 26 Aug 2024 09:00:00 -0500", ChangesFile.formatDate(new Date(1724680800000L)));
    }

    @Test
    public void testFormatDateUTC() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));

        assertEquals("Mon, 26 Aug 2024 14:00:00 +0000", ChangesFile.formatDateUTC(new Date(1724680800000L)));
    }
}
