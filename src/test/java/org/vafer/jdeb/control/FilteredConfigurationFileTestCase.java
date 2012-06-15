package org.vafer.jdeb.control;

import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.tools.ant.util.ReaderInputStream;
import org.vafer.jdeb.utils.MapVariableResolver;
import org.vafer.jdeb.utils.VariableResolver;

public class FilteredConfigurationFileTestCase extends TestCase {

    private VariableResolver variableResolver;

    private FilteredConfigurationFile placeHolder;

    @Override
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

}
