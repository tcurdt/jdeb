/*
 * Copyright 2014 The jdeb developers.
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

import java.io.FileInputStream;
import java.text.ParseException;

import junit.framework.TestCase;

public final class PackageControlFileTestCase extends TestCase {

    public void testParse() throws Exception {
        String input =
                "Key1: Value1\n" +
                "Key2: Value2\n" +
                " Value2.1\n" +
                " Value2.2\n" +
                "Key3: Value3\n";

        BinaryPackageControlFile d = new BinaryPackageControlFile(input);
        assertFalse(d.isValid());

        assertEquals("key 1", "Value1", d.get("Key1"));
        assertEquals("key 2", "Value2\nValue2.1\nValue2.2", d.get("Key2"));
        assertEquals("key 3", "Value3", d.get("Key3"));
    }

    public void testToString() throws Exception {
        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();
        packageControlFile.set("Package", "test-package");
        packageControlFile.set("Description", "This is\na description\non several lines");
        packageControlFile.set("Version", "1.0");

        String s = packageControlFile.toString();

        BinaryPackageControlFile packageControlFile2 = new BinaryPackageControlFile(s);
        assertEquals("Package", packageControlFile.get("Package"), packageControlFile2.get("Package"));
        assertEquals("Description", packageControlFile.get("Description"), packageControlFile2.get("Description"));
        assertEquals("Version 3", packageControlFile.get("Version"), packageControlFile2.get("Version"));
    }

    public void testEmptyLines() throws Exception {
        String input =
                "Key1: Value1\n" +
                "Key2: Value2\n" +
                "\n";
        try {
            new BinaryPackageControlFile(input);
            fail("Should throw a ParseException");
        } catch (ParseException e) {
        }
    }

    public void testGetShortDescription() {
        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();

        assertNull(packageControlFile.getShortDescription());

        packageControlFile.set("Description", "This is the short description\nThis is the loooooong description");

        assertEquals("short description", "This is the short description", packageControlFile.getShortDescription());

        packageControlFile.set("Description", "\nThere is no short description");

        assertEquals("short description", "", packageControlFile.getShortDescription());
    }

    public void testGetDescription() throws Exception {
        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();
        packageControlFile.parse(new FileInputStream("target/test-classes/org/vafer/jdeb/deb/control/control"));

        assertEquals("Description", "revision @REVISION@, test package\n" +
                "This is a sample package control file.\n\n" +
                "Use for testing purposes only.",
                packageControlFile.get("Description"));
    }

    public void testGetUserDefinedFields() throws Exception {
        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile();
        packageControlFile.parse(new FileInputStream("target/test-classes/org/vafer/jdeb/deb/control/control"));
        assertEquals("UserDefinedField", "This is a user defined field.",  packageControlFile.get("UserDefinedField"));
    }
}
