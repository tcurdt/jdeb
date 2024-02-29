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

package org.vafer.jdeb.utils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;
import org.junit.Assert;

public final class UtilsTestCase extends Assert {

    @Test
    public void testJoinPath() {
        assertEquals("", "foo/bar", Utils.joinUnixPath(null, "foo", "bar"));
        assertEquals("", "/foo/bar", Utils.joinUnixPath(null, "/foo", "/bar"));
        assertEquals("", "foo/bar", Utils.joinUnixPath(null, "foo/bar"));
    }

    @Test
    public void testStripPath() {
        assertEquals("foo/bar", Utils.stripPath(0, "foo/bar"));

        assertEquals("bar", Utils.stripPath(1, "foo/bar"));

        assertEquals("bar/baz", Utils.stripPath(1, "foo/bar/baz"));
        assertEquals("baz", Utils.stripPath(2, "foo/bar/baz"));

        assertEquals("foo/", Utils.stripPath(0, "foo/"));
        assertEquals("", Utils.stripPath(1, "foo/"));
        assertEquals("foo/", Utils.stripPath(2, "foo/"));
    }

    private String convert(String s) throws Exception {
        byte[] data = Utils.toUnixLineEndings(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
        return new String(data, StandardCharsets.UTF_8);
    }

    @Test
    public void testNewlineConversionLF() throws Exception {
        String expected = "test\ntest\n\ntest\n";
        String actual = convert("test\ntest\n\ntest");
        assertEquals(expected, actual);
    }

    @Test
    public void testNewlineConversionCRLF() throws Exception {
        String expected = "test\ntest\n\ntest\n";
        String actual = convert("test\r\ntest\r\n\r\ntest");
        assertEquals(expected, actual);
    }

    @Test
    public void testNewlineConversionCR() throws Exception {
        String expected = "test\ntest\n\ntest\n";
        String actual = convert("test\rtest\r\rtest");
        assertEquals(expected, actual);
    }

    @Test
    public void testReplaceVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("version", "1.2.3");
        variables.put("name", "jdeb");
        variables.put("url", "https://github.com/tcurdt/jdeb");

        VariableResolver resolver = new MapVariableResolver(variables);

        String input, result;

        // main case
        result = Utils.replaceVariables(resolver, "Version: [[version]]", "[[", "]]");
        assertEquals("Version: 1.2.3", result);

        // multiple variables in the same expression
        result = Utils.replaceVariables(resolver, "[[name]] [[version]]", "[[", "]]");
        assertEquals("jdeb 1.2.3", result);

        // collision with script syntax
        result = Utils.replaceVariables(resolver, "if [[ \"${HOST_TYPE}\" -eq \"admin\" ]] ; then", "[[", "]]");
        assertEquals("if [[ \"${HOST_TYPE}\" -eq \"admin\" ]] ; then", result);

        // collision with python syntax
        input = "c[t]['A']";
        result = Utils.replaceVariables(resolver, input, "[[", "]]");
        assertEquals(input, result);

        // end of line https://github.com/tcurdt/jdeb/issues/154
        input = "if [ -e some_file ]";
        result = Utils.replaceVariables(resolver, input, "[[", "]]");
        assertEquals(input, result);

        // mixed valid and unknown variables
        result = Utils.replaceVariables(resolver, "[[name]] [[test]]", "[[", "]]");
        assertEquals("jdeb [[test]]", result);

        // nested vars
        result = Utils.replaceVariables(new VariableResolver() {
            public String get(String pKey) {
                return "VAR";
            }
        }, "[[var]] [[ [[var]] [[ [[var]] ]] [[var]] ]]", "[[", "]]");

        assertEquals("VAR [[ VAR [[ VAR ]] VAR ]]", result);
    }

    @Test
    public void testReplaceVariablesWithinOpenCloseTokens() {
        Map<String, String> variables = new HashMap<>();
        variables.put("artifactId", "jdeb");

        VariableResolver resolver = new MapVariableResolver(variables);

        String result = Utils.replaceVariables(resolver, "if [[ -z \"$(grep [[artifactId]] /etc/passwd )\" ]] ; then", "[[", "]]");

        assertEquals("", "if [[ -z \"$(grep jdeb /etc/passwd )\" ]] ; then", result);

    }

    @Test
    public void testVersionConversion() {
        Calendar cal = new GregorianCalendar(2013, Calendar.FEBRUARY, 17);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals("should match", "1.0", Utils.convertToDebianVersion("1.0", false, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~SNAPSHOT", Utils.convertToDebianVersion("1.0+SNAPSHOT", false, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~SNAPSHOT", Utils.convertToDebianVersion("1.0-SNAPSHOT", false, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~20130217000000", Utils.convertToDebianVersion("1.0+SNAPSHOT", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~RC2", Utils.convertToDebianVersion("1.0-RC2", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~alpha3", Utils.convertToDebianVersion("1.0-alpha3", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~Beta+4", Utils.convertToDebianVersion("1.0.Beta-4", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~milestone+4", Utils.convertToDebianVersion("1.0-milestone-4", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~a+4", Utils.convertToDebianVersion("1.0-a-4", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~a+4", Utils.convertToDebianVersion("1.0a-4", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~b+4", Utils.convertToDebianVersion("1.0-b-4", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~rc7", Utils.convertToDebianVersion("1.0rc7", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~M1", Utils.convertToDebianVersion("1.0.M1", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~M2", Utils.convertToDebianVersion("1.0-M2", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0~M3", Utils.convertToDebianVersion("1.0M3", true, "SNAPSHOT", null, cal.getTime()));

        assertEquals("should match", "1.0+prj+3", Utils.convertToDebianVersion("1.0-prj_3", false, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~SNAPSHOT", Utils.convertToDebianVersion("1.0-prj_3+SNAPSHOT", false, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~SNAPSHOT", Utils.convertToDebianVersion("1.0-prj_3-SNAPSHOT", false, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~20130217000000", Utils.convertToDebianVersion("1.0-prj_3+SNAPSHOT", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~RC2", Utils.convertToDebianVersion("1.0-prj_3-RC2", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~alpha3", Utils.convertToDebianVersion("1.0-prj_3-alpha3", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~Beta+4", Utils.convertToDebianVersion("1.0-prj_3.Beta-4", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~milestone+4", Utils.convertToDebianVersion("1.0-prj_3-milestone-4", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~a+4", Utils.convertToDebianVersion("1.0-prj_3-a-4", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~b+4", Utils.convertToDebianVersion("1.0-prj_3-b-4", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~rc7", Utils.convertToDebianVersion("1.0-prj_3-rc7", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~M1", Utils.convertToDebianVersion("1.0-prj_3.M1", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3~M2", Utils.convertToDebianVersion("1.0-prj_3-M2", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj~M3", Utils.convertToDebianVersion("1.0-prj_M3", true, "SNAPSHOT", null, cal.getTime()));

        assertEquals("should match", "1.0+prj+~M3", Utils.convertToDebianVersion("1.0-prj__-M3", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+.+~M3", Utils.convertToDebianVersion("1.0-prj_._-M3", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3+~M3", Utils.convertToDebianVersion("1.0-prj_3:M3", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3+c+~20130217000000", Utils.convertToDebianVersion("1.0-prj_3-c++-SNAPSHOT", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+prj+3+c+~20130217000000", Utils.convertToDebianVersion("1.0-prj_3-c+++++++-SNAPSHOT", true, "SNAPSHOT", null, cal.getTime()));

        assertEquals("should match", "1.0+MMM+3", Utils.convertToDebianVersion("1.0-MMM-3", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+aaa+3", Utils.convertToDebianVersion("1.0-aaa-3", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0+bbb+3", Utils.convertToDebianVersion("1.0-bbb-3", true, "SNAPSHOT", null, cal.getTime()));
        assertEquals("should match", "1.0aaa+3", Utils.convertToDebianVersion("1.0aaa-3", true, "SNAPSHOT", null, cal.getTime()));
    }

    public void testVersionConversionWithTemplate() {
        Calendar cal = new GregorianCalendar(2013, Calendar.FEBRUARY, 17);
        assertEquals("should match", "1.0~20130217000000", Utils.convertToDebianVersion("1.0+SNAPSHOT", true, "SNAPSHOT", "[yyyyMMddHHmmss]", cal.getTime()));
        assertEquals("should match", "1.0~130217000000", Utils.convertToDebianVersion("1.0+SNAPSHOT", true, "SNAPSHOT", "[yyMMddHHmmss]", cal.getTime()));
        assertEquals("should match", "1.0~20130217", Utils.convertToDebianVersion("1.0+SNAPSHOT", true, "SNAPSHOT", "[yyyyMMdd]", cal.getTime()));
        assertEquals("should match", "1.0~100.20130217", Utils.convertToDebianVersion("1.0+SNAPSHOT", true, "SNAPSHOT", "100.[yyyyMMdd]", cal.getTime()));
        assertEquals("should match", "1.0~100.20130217.50", Utils.convertToDebianVersion("1.0+SNAPSHOT", true, "SNAPSHOT", "100.[yyyyMMdd].50", cal.getTime()));
        assertEquals("should match", "1.0~100.20130217.foo", Utils.convertToDebianVersion("1.0+SNAPSHOT", true, "SNAPSHOT", "100.[yyyyMMdd].foo", cal.getTime()));
    }

    @Test
    public void testMovePath() {
        assertEquals("/usr/share/file.txt", Utils.movePath("file.txt", "/usr/share"));
        assertEquals("/usr/share/file.txt", Utils.movePath("file.txt", "/usr/share/"));
        assertEquals("/usr/share/noext", Utils.movePath("noext", "/usr/share/"));
        assertEquals("/usr/share/file.txt", Utils.movePath("/home/user/file.txt", "/usr/share"));
        assertEquals("/usr/share/file.txt", Utils.movePath("../relative/file.txt", "/usr/share/"));
    }
}
