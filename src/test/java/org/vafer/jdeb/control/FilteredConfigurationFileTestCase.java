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
package org.vafer.jdeb.control;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.tools.ant.util.ReaderInputStream;
import org.vafer.jdeb.descriptors.PackageDescriptor;
import org.vafer.jdeb.utils.MapVariableResolver;
import org.vafer.jdeb.utils.VariableResolver;

public class FilteredConfigurationFileTestCase extends TestCase {

    private VariableResolver variableResolver;

    private FilteredConfigurationFile placeHolder;

    protected void setUp() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("artifactId", "jdeb");
        map.put("myProperty1", "custom1");
        map.put("myProperty2", "custom2");
        variableResolver = new MapVariableResolver(map);
    }

    public void testTokenSubstitution() throws Exception {
        InputStream inputStream = new ReaderInputStream(new StringReader("#!/bin/sh\ncat [[artifactId]][[myProperty1]] \necho '[[myProperty2]]'\n"));

        placeHolder = new FilteredConfigurationFile("", inputStream, variableResolver);

        String actual = placeHolder.toString();
        assertEquals("#!/bin/sh\ncat jdebcustom1 \necho 'custom2'\n", actual);
    }

    public void testName() throws Exception {
        InputStream inputStream = new ReaderInputStream(new StringReader(""));
        placeHolder = new FilteredConfigurationFile("myName", inputStream, variableResolver);
        assertEquals("myName", placeHolder.getName());
    }

    public void testVariableSubstitution() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("VERSION", "1.2");
        map.put("MAINTAINER", "Torsten Curdt <tcurdt@vafer.org>");
        
        String controlFile = 
                "Version: [[VERSION]]\n"
                + "Maintainer: [[MAINTAINER]]\n"
                + "NoResolve1: test[[test\n"
                + "NoResolve2: [[test]]\n";

        FilteredConfigurationFile filteredFile = new FilteredConfigurationFile("control", new ByteArrayInputStream(controlFile.getBytes()), new MapVariableResolver(map));
        
        PackageDescriptor d = new PackageDescriptor(new ByteArrayInputStream(filteredFile.toString().getBytes()));
        
        assertEquals("1.2", d.get("Version"));
        assertEquals("Torsten Curdt <tcurdt@vafer.org>", d.get("Maintainer"));
        assertEquals("test[[test", d.get("NoResolve1"));
        assertEquals("[[test]]", d.get("NoResolve2"));
    }
}
