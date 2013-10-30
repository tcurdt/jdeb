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
import java.util.Arrays;
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
    protected final Map<String, String> userDefinedFields = new LinkedHashMap<String, String>();
    protected final Set<ControlField> userDefinedFieldNames = new HashSet<ControlField>();

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
                // flush value of the previous field
                set(field, buffer.toString());
                break;
            }

            linenr++;

            if (line.length() == 0) {
                throw new ParseException("Empty line", linenr);
            }

            final char first = line.charAt(0);
            if (Character.isLetter(first)) {

                // new field

                // flush value of the previous field
                set(field, buffer.toString());
                buffer = new StringBuilder();


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

    public void set(String field, final String value) {
        if (field != null && isUserDefinedField(field)) {
            userDefinedFields.put(field, value);
            String fieldName = getUserDefinedFieldName(field);

            if (fieldName != null) {
                userDefinedFieldNames.add(new ControlField(fieldName));
            }

            field = fieldName;
        }

        if (field != null && !"".equals(field)) {
            values.put(field, value);
        }
    }

    public String get(String field) {
        return values.get(field);
    }

    protected abstract ControlField[] getFields();

    protected Map<String, String> getUserDefinedFields() {
        return userDefinedFields;
    }

    protected Set<ControlField> getUserDefinedFieldNames() {
        return userDefinedFieldNames;
    }

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
        List<ControlField> fields = new ArrayList<ControlField>();
        fields.addAll(Arrays.asList(getFields()));
        fields.addAll(getUserDefinedFieldNames());
        return toString(fields.toArray(new ControlField[fields.size()]));
    }

    /**
     * Returns the letter expected in the prefix of a user defined field
     * in order to include the field in this control file.
     *
     * @return The letter returned is:
     * <ul>
     *   <li>B: for a binary package</li>
     *   <li>S: for a source package</li>
     *   <li>C: for a changes file</li>
     * </ul>
     * 
     * @since 1.1
     * @see <a href="http://www.debian.org/doc/debian-policy/ch-controlfields.html#s5.7">Debian Policy - User-defined fields</a>
     */
    protected abstract char getUserDefinedFieldLetter();

    /**
     * Tells if the specified field name is a user defined field.
     * User-defined fields must begin with an 'X', followed by one or more
     * letters that specify the output file and a hyphen.
     * 
     * @param field the name of the field
     *
     * @since 1.1
     * @see <a href="http://www.debian.org/doc/debian-policy/ch-controlfields.html#s5.7">Debian Policy - User-defined fields</a>
     */
    protected boolean isUserDefinedField(String field) {
        return field.startsWith("X") && field.indexOf("-") > 0;
    }

    /**
     * Returns the user defined field without its prefix.
     * 
     * @param field the name of the user defined field
     * @return the user defined field without the prefix, or null if the fields
     *         doesn't apply to this control file.
     * @since 1.1
     */
    protected String getUserDefinedFieldName(String field) {
        int index = field.indexOf('-');
        char letter = getUserDefinedFieldLetter();

        for (int i = 0; i < index; ++i) {
            if (field.charAt(i) == letter) {
                return field.substring(index + 1);
            }
        }

        return null;
    }
}
