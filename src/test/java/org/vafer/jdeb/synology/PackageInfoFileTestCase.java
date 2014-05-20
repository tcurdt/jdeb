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

package org.vafer.jdeb.synology;

import java.io.FileInputStream;
import java.text.ParseException;

import org.vafer.jdeb.debian.BinaryPackageControlFile;
import org.vafer.jdeb.synology.BinaryPackageInfoFile;

import junit.framework.TestCase;

public final class PackageInfoFileTestCase extends TestCase {

    public void testParse() throws Exception {
        String input =
                "Key1=\"Value1\"\n" +
                "Key2=\"Value2\"\n" +
                " Value2.1\"\n" +
                " Value2.2\"\n" +
                "Key3=\"Value3\"\n";

        BinaryPackageInfoFile d = new BinaryPackageInfoFile(input);
        assertFalse(d.isValid());

        assertEquals("key 1", "Value1", d.get("Key1"));
        assertEquals("key 2", "Value2\nValue2.1\nValue2.2", d.get("Key2"));
        assertEquals("key 3", "Value3", d.get("Key3"));
    }

    public void testToString() throws Exception {
    	BinaryPackageInfoFile packageInfoFile = new BinaryPackageInfoFile();
        packageInfoFile.set("package", "test-package");
        packageInfoFile.set("description", "This is\na description\non several lines");
        packageInfoFile.set("version", "1.0");

        String s = packageInfoFile.toString();

        BinaryPackageInfoFile packageInfoFile2 = new BinaryPackageInfoFile(s);
        assertEquals("package", packageInfoFile.get("package"), packageInfoFile2.get("package"));
        assertEquals("description", packageInfoFile.get("description"), packageInfoFile2.get("description"));
        assertEquals("version", packageInfoFile.get("version"), packageInfoFile2.get("version"));
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
    	BinaryPackageInfoFile packageInfoFile = new BinaryPackageInfoFile();

        assertNull(packageInfoFile.getShortDescription());

        packageInfoFile.set("description", "This is the short description\nThis is the loooooong description");

        assertEquals("short description", "This is the short description", packageInfoFile.getShortDescription());

        packageInfoFile.set("description", "\nThere is no short description");

        assertEquals("short description", "", packageInfoFile.getShortDescription());
    }

    public void testGetDescription() throws Exception {
    	BinaryPackageInfoFile packageInfoFile = new BinaryPackageInfoFile();
        packageInfoFile.parse(new FileInputStream("target/test-classes/org/vafer/jdeb/spk/info"));

        assertEquals("description", "revision @REVISION@, test package\n" +
                "This is a sample package control file.\n\n" +
                "Use for testing purposes only.",
                packageInfoFile.get("description"));
    }

    public void testGetUserDefinedFields() throws Exception {
    	BinaryPackageInfoFile packageInfoFile = new BinaryPackageInfoFile();
        packageInfoFile.parse(new FileInputStream("target/test-classes/org/vafer/jdeb/spk/info"));
        assertEquals("userdefinedfield", "This is a user defined field.",  packageInfoFile.get("userdefinedfield"));
    }
}
