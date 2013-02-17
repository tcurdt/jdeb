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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * A descriptor holds the usual key value pairs.
 *
 * @author Torsten Curdt
 * @see <a href="http://www.debian.org/doc/debian-policy/ch-controlfields.html">Debian Policy Manual - Control files and their fields</a>
 */
public abstract class AbstractDescriptor {

    protected final Map<String, String> values = new LinkedHashMap<String, String>();
    protected final VariableResolver resolver;

    private static String openToken = "[[";
    private static String closeToken = "]]";

    public AbstractDescriptor( final VariableResolver pResolver ) {
        resolver = pResolver;
    }

    public AbstractDescriptor( final AbstractDescriptor pDescriptor ) {
        this(pDescriptor.resolver);
        values.putAll(pDescriptor.values);
    }

    public static void setOpenToken( final String pToken ) {
        openToken = pToken;
    }

    public static void setCloseToken( final String pToken ) {
        closeToken = pToken;
    }

    protected void parse( final InputStream pInput ) throws IOException, ParseException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(pInput));
        StringBuilder buffer = new StringBuilder();
        String key = null;
        int linenr = 0;
        while (true) {
            final String line = br.readLine();

            if (line == null) {
                if (buffer.length() > 0) {
                    // flush value of previous key
                    set(key, buffer.toString());
                    buffer = null;
                }
                break;
            }

            linenr++;

            if (line.length() == 0) {
                throw new ParseException("Empty line", linenr);
            }

            final char first = line.charAt(0);
            if (Character.isLetter(first)) {

                // new key

                if (buffer.length() > 0) {
                    // flush value of previous key
                    set(key, buffer.toString());
                    buffer = new StringBuilder();
                }


                final int i = line.indexOf(':');

                if (i < 0) {
                    throw new ParseException("Line misses ':' delimiter", linenr);
                }

                key = line.substring(0, i);
                buffer.append(line.substring(i + 1).trim());

                continue;
            }

            // continuing old value
            buffer.append('\n').append(line.substring(1));
        }
        br.close();

    }

    public void set(final String pKey, final String pValue) {
        String value = Utils.replaceVariables(resolver, pValue, openToken, closeToken);
        if ("".equals(value)) {
            value = null;
        }
        values.put(pKey, value);
    }

    public String get( String key ) {
        return values.get(key);
    }

    public abstract String[] getMandatoryKeys();

    public boolean isValid() {
        return invalidKeys().size() == 0;
    }

    public Set<String> invalidKeys() {
        final Set<String> invalid = new HashSet<String>();
        
        for (String aMk : getMandatoryKeys()) {
            if (get(aMk) == null) {
                invalid.add(aMk);
            }
        }

        return invalid;
    }

    public String toString( final String[] keys ) {
        StringBuilder s = new StringBuilder();
        for (String key : keys) {
            String value = values.get(key);
            if (value != null) {
                s.append(key).append(":");

                try {
                    BufferedReader reader = new BufferedReader(new StringReader(value));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.length() != 0 && !Character.isWhitespace(line.charAt(0))) {
                            s.append(' ');
                        }

                        s.append(line).append('\n');
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return s.toString();
    }
}
