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
package org.vafer.jdeb.changes;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

import org.vafer.jdeb.descriptors.PackageDescriptor;

public final class TextfileChangesProviderTestCase extends TestCase {

    public void testParsing() throws Exception {

        final String input =
            " * change1\n" +
            " * change2\n" +
            "release date=14:00 13.01.2007, version=12324, urgency=low, by=tcurdt@joost.com\n" +
            " * change1\n" +
            " * change2\n" +
            "release date=12:00 10.01.2007, version=10324, urgency=low, by=tcurdt@joost.com\n" +
            " * change1\n" +
            " * change2\n";
        
        final PackageDescriptor descriptor = new PackageDescriptor();
        descriptor.set("Package", "package");
        descriptor.set("Version", "version");
        descriptor.set("Distribution", "distribution");
        descriptor.set("Date", "Mon, 20 Aug 2007 15:25:57 +0200");
        
        final TextfileChangesProvider provider = new TextfileChangesProvider(new ByteArrayInputStream(input.getBytes("UTF-8")), descriptor);
        final ChangeSet[] changeSets = provider.getChangesSets();
        
        assertNotNull(changeSets);
        assertEquals(3, changeSets.length);     
    }

    public void testDistributionFromChangesProvider() throws Exception {

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
        
        final PackageDescriptor descriptor = new PackageDescriptor();
        descriptor.set("Package", "package");
        descriptor.set("Version", "version");
        descriptor.set("Date", "Mon, 20 Aug 2007 15:25:57 +0200");
        
        final TextfileChangesProvider provider = new TextfileChangesProvider(new ByteArrayInputStream(input.getBytes("UTF-8")), descriptor);
        final ChangeSet[] changeSets = provider.getChangesSets();
        
        assertNotNull(changeSets);
        assertEquals(3, changeSets.length);
        
        assertEquals("production", changeSets[0].getDistribution());
        assertEquals("staging", changeSets[1].getDistribution());
        assertEquals("development", changeSets[2].getDistribution());
        
    }

}
