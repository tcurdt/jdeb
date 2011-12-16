/*
 * Copyright 2012 The Apache Software Foundation.
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

package org.vafer.jdeb.utils;


import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

public class UtilsTestCase extends TestCase {

    public void testStripPath() throws Exception {
        assertEquals("foo/bar", Utils.stripPath(0,"foo/bar"));

        assertEquals("bar", Utils.stripPath(1,"foo/bar"));

        assertEquals("bar/baz", Utils.stripPath(1,"foo/bar/baz"));
        assertEquals("baz", Utils.stripPath(2,"foo/bar/baz"));

        assertEquals("foo/", Utils.stripPath(0,"foo/"));
        assertEquals("", Utils.stripPath(1,"foo/"));
        assertEquals("foo/", Utils.stripPath(2,"foo/"));
    }

    private String convert(String s) throws Exception {
        byte[] data = Utils.toUnixLineEndings(new ByteArrayInputStream(s.getBytes("UTF-8")));
        return new String(data, "UTF-8");
    }
    
    public void testNewlineConversionLF() throws Exception {
        String expected = "test\ntest\n\ntest\n";
        String actual = convert("test\ntest\n\ntest");
        assertEquals(expected, actual);
    }

    public void testNewlineConversionCRLF() throws Exception {
        String expected = "test\ntest\n\ntest\n";
        String actual = convert("test\r\ntest\r\n\r\ntest");
        assertEquals(expected, actual);
    }

    public void testNewlineConversionCR() throws Exception {
        String expected = "test\ntest\n\ntest\n";
        String actual = convert("test\rtest\r\rtest");
        assertEquals(expected, actual);
    }
    
}
