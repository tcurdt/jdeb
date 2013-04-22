/*
 * Copyright 2013 The jdeb developers.
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

import java.text.ParseException;

import junit.framework.TestCase;

public final class PackageDescriptorTestCase extends TestCase {

    public void testParse() throws Exception {
        String input =
                "Key1: Value1\n" +
                "Key2: Value2\n" +
                " Value2.1\n" +
                " Value2.2\n" +
                "Key3: Value3\n";
        
        PackageDescriptor d = new PackageDescriptor(input);
        assertFalse(d.isValid());

        assertEquals("key 1", "Value1", d.get("Key1"));
        assertEquals("key 2", "Value2\nValue2.1\nValue2.2", d.get("Key2"));
        assertEquals("key 3", "Value3", d.get("Key3"));
    }

    public void testToString() throws Exception {
        PackageDescriptor descriptor = new PackageDescriptor();
        descriptor.set("Package", "test-package");
        descriptor.set("Description", "This is\na description\non several lines");
        descriptor.set("Version", "1.0");

        String s = descriptor.toString();
        
        PackageDescriptor descriptor2 = new PackageDescriptor(s);
        assertEquals("Package", descriptor.get("Package"), descriptor2.get("Package"));
        assertEquals("Description", descriptor.get("Description"), descriptor2.get("Description"));
        assertEquals("Version 3", descriptor.get("Version"), descriptor2.get("Version"));
    }

    public void testEmptyLines() throws Exception {
        String input =
                "Key1: Value1\n" +
                "Key2: Value2\n" +
                "\n";
        try {
            new PackageDescriptor(input);
            fail("Should throw a ParseException");
        } catch (ParseException e) {
        }
    }
}
