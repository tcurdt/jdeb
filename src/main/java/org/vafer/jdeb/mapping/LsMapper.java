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
package org.vafer.jdeb.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

/**
 * Reads permissions and ownerships from a "ls -laR > mapping.txt" dump and
 * maps entries accordingly.
 *
 * @author Torsten Curdt
 */
public final class LsMapper implements Mapper {

    private final Map<String, TarArchiveEntry> mapping;


    public final static class ParseError extends Exception {

        private static final long serialVersionUID = 1L;

        public ParseError( String message ) {
            super(message);
        }
    }


    public LsMapper( final InputStream pInput ) throws IOException, ParseError {
        mapping = parse(pInput);
    }

    /*
./trunk/target/test-classes/org/vafer/dependency:
total 176
drwxr-xr-x   23 tcurdt  tcurdt   782 Jun 25 03:48 .
drwxr-xr-x    3 tcurdt  tcurdt   102 Jun 25 03:48 ..
-rw-r--r--    1 tcurdt  tcurdt  2934 Jun 25 03:48 DependenciesTestCase.class
-rw-r--r--    1 tcurdt  tcurdt   786 Jun 25 03:48 JarCombiningTestCase$1.class
-rw-r--r--    1 tcurdt  tcurdt  2176 Jun 25 03:48 WarTestCase.class
drwxr-xr-x    4 tcurdt  tcurdt   136 Jun 25 03:48 classes

./trunk/target/test-classes/org/vafer/dependency/classes:
     */

    final private Pattern basePattern = Pattern.compile("^\\./(.*):$");
    final private Pattern totalPattern = Pattern.compile("^total ([0-9]+)$");
    final private Pattern dirPattern = Pattern.compile("^d([rwx-]{9})\\s+([0-9]+)\\s+(\\S*)\\s+(\\S*)\\s+([0-9]+)\\s+(.*)\\s+[\\.]{1,2}$");
    final private Pattern filePattern = Pattern.compile("^([d-])([rwx-]{9})\\s+([0-9]+)\\s+(\\S*)\\s+(\\S*)\\s+([0-9]+)\\s+(.*)\\s+(.*)$");
    final private Pattern newlinePattern = Pattern.compile("$");

    private String readBase( final BufferedReader reader ) throws IOException, ParseError {
        final String line = reader.readLine();
        if (line == null) {
            return null;
        }
        final Matcher matcher = basePattern.matcher(line);
        if (!matcher.matches()) {
            throw new ParseError("expected base line but got \"" + line + "\"");
        }
        return matcher.group(1);
    }

    private String readTotal( final BufferedReader reader ) throws IOException, ParseError {
        final String line = reader.readLine();
        final Matcher matcher = totalPattern.matcher(line);
        if (!matcher.matches()) {
            throw new ParseError("expected total line but got \"" + line + "\"");
        }
        return matcher.group(1);
    }

    private TarArchiveEntry readDir( final BufferedReader reader, final String base ) throws IOException, ParseError {
        final String current = reader.readLine();
        final Matcher currentMatcher = dirPattern.matcher(current);
        if (!currentMatcher.matches()) {
            throw new ParseError("expected dirline but got \"" + current + "\"");
        }

        final String parent = reader.readLine();
        final Matcher parentMatcher = dirPattern.matcher(parent);
        if (!parentMatcher.matches()) {
            throw new ParseError("expected dirline but got \"" + parent + "\"");
        }

        final TarArchiveEntry entry = new TarArchiveEntry(base, true);

        entry.setMode(convertModeFromString(currentMatcher.group(1)));
        entry.setUserName(currentMatcher.group(3));
        entry.setGroupName(currentMatcher.group(4));

        return entry;
    }


    private int convertModeFromString( final String mode ) {

        final char[] m = mode.toCharArray();
        /*
           -rwxrwxrwx

           4000    set-user-ID-on-execution bit
           2000    set-user-ID-on-execution bit
           1000    sticky bit
           0400    allow read by owner.
           0200    allow write by owner.
           0100    execute / search
           0040    allow read by group members.
           0020    allow write by group members.
           0010    execute / search
           0004    allow read by others.
           0002    allow write by others.
           0001    execute / search
         */
        // TODO: simplified - needs fixing
        int sum = 0;
        int bit = 1;
        for (int i = m.length - 1; i >= 0; i--) {
            if (m[i] != '-') {
                sum += bit;
            }
            bit += bit;
        }
        return sum;
    }

    private TarArchiveEntry readFile( final BufferedReader reader, final String base ) throws IOException, ParseError {

        while (true) {
            final String line = reader.readLine();

            if (line == null) {
                return null;
            }

            final Matcher currentMatcher = filePattern.matcher(line);
            if (!currentMatcher.matches()) {
                final Matcher newlineMatcher = newlinePattern.matcher(line);
                if (newlineMatcher.matches()) {
                    return null;
                }
                throw new ParseError("expected file line but got \"" + line + "\"");
            }

            final String type = currentMatcher.group(1);
            if (type.startsWith("-")) {
                final TarArchiveEntry entry = new TarArchiveEntry(base + "/" + currentMatcher.group(8), true);

                entry.setMode(convertModeFromString(currentMatcher.group(2)));
                entry.setUserName(currentMatcher.group(4));
                entry.setGroupName(currentMatcher.group(5));

                return entry;
            }
        }

    }

    private Map<String, TarArchiveEntry> parse( final InputStream pInput ) throws IOException, ParseError {
        final Map<String, TarArchiveEntry> mapping = new HashMap<String, TarArchiveEntry>();

        final BufferedReader reader = new BufferedReader(new InputStreamReader(pInput));

        boolean first = true;
        while (true) {

            final String base;
            if (first) {
                base = "";
                first = false;
            } else {
                base = readBase(reader);
                if (base == null) {
                    break;
                }
            }

            readTotal(reader);
            final TarArchiveEntry dir = readDir(reader, base);
            mapping.put(dir.getName(), dir);

            while (true) {
                final TarArchiveEntry file = readFile(reader, base);

                if (file == null) {
                    break;
                }

                mapping.put(file.getName(), file);
            }
        }

        return mapping;
    }

    public TarArchiveEntry map( final TarArchiveEntry pEntry ) {

        final TarArchiveEntry entry = mapping.get(pEntry.getName());

        if (entry != null) {

            final TarArchiveEntry newEntry = new TarArchiveEntry(entry.getName(), true);
            newEntry.setUserId(entry.getUserId());
            newEntry.setGroupId(entry.getGroupId());
            newEntry.setUserName(entry.getUserName());
            newEntry.setGroupName(entry.getGroupName());
            newEntry.setMode(entry.getMode());
            newEntry.setSize(entry.getSize());

            return newEntry;
        }

        return pEntry;
    }

}
