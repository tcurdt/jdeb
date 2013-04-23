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

package org.vafer.jdeb.debian;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A control file as specified by the <a href="http://www.debian.org/doc/debian-policy/ch-controlfields.html">Debian policy</a>.
 *
 * @author Torsten Curdt
 */
public abstract class ControlFile {

    protected final Map<String, String> values = new LinkedHashMap<String, String>();

    public void parse(String input) throws IOException, ParseException {
        parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
    }

    public void parse(InputStream input) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        StringBuilder buffer = new StringBuilder();
        String field = null;
        int linenr = 0;
        while (true) {
            final String line = reader.readLine();

            if (line == null) {
                if (buffer.length() > 0) {
                    // flush value of the previous field
                    set(field, buffer.toString());
                }
                break;
            }

            linenr++;

            if (line.length() == 0) {
                throw new ParseException("Empty line", linenr);
            }

            final char first = line.charAt(0);
            if (Character.isLetter(first)) {

                // new field

                if (buffer.length() > 0) {
                    // flush value of the previous field
                    set(field, buffer.toString());
                    buffer = new StringBuilder();
                }


                final int i = line.indexOf(':');

                if (i < 0) {
                    throw new ParseException("Line misses ':' delimiter", linenr);
                }

                field = line.substring(0, i);
                buffer.append(line.substring(i + 1).trim());

                continue;
            }

            // continuing old value, lines with only a dot are ignored
            buffer.append('\n');
            if (!".".equals(line.substring(1).trim())) {
                buffer.append(line.substring(1));
            }
        }
        reader.close();

    }

    public void set(final String field, final String value) {
        if (!"".equals(value)) {
            values.put(field, value);
        }
    }

    public String get(String field) {
        return values.get(field);
    }

    protected abstract ControlField[] getFields();

    public List<String> getMandatoryFields() {
        List<String> fields = new ArrayList<String>();

        for (ControlField field : getFields()) {
            if (field.isMandatory()) {
                fields.add(field.getName());
            }
        }
        
        return fields;
    }

    public boolean isValid() {
        return invalidFields().size() == 0;
    }

    public Set<String> invalidFields() {
        Set<String> invalid = new HashSet<String>();
        
        for (ControlField field : getFields()) {
            if (field.isMandatory() && get(field.getName()) == null) {
                invalid.add(field.getName());
            }
        }

        return invalid;
    }

    public String toString(ControlField... fields) {
        StringBuilder s = new StringBuilder();
        for (ControlField field : fields) {
            String value = values.get(field.getName());
            s.append(field.format(value));
        }
        return s.toString();
    }

    public String toString() {
        return toString(getFields());
    }
}
