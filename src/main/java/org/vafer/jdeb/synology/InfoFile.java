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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vafer.jdeb.utils.ControlField;

/**
 * A info file for synology packages.
 *
 * @author Torsten Curdt
 */
public abstract class InfoFile {

    protected final Map<String, String> values = new LinkedHashMap<String, String>();
    
    public void parse(final String input) throws IOException, ParseException {
        parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
    }

    public void parse(final InputStream input) throws IOException, ParseException {
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


                final int i = line.indexOf(getDelimiter());

                if (i < 0) {
                    throw new ParseException("Line misses '" + getDelimiter() + "' delimiter", linenr);
                }

                field = line.substring(0, i).trim();
                
                buffer.append(stripPrefixAndWrapper(line.substring(i + 1)).trim());

                continue;
            }

            // continuing old value, lines with only a dot are ignored
            buffer.append('\n');
            if (!".".equals(line.substring(1).trim())) {
                buffer.append(stripPrefixAndWrapper(line.substring(1)).trim());
            }
        }
        reader.close();

    }
    
    private String stripPrefixAndWrapper(final String value) {
    	String tempValue = value;
        if (tempValue.startsWith(getValuePrefix())) {
        	tempValue = tempValue.substring(getValuePrefix().length());
        }
        if (tempValue.startsWith(getValueWrapper())) {
        	tempValue = tempValue.substring(getValueWrapper().length());
        }
        if (tempValue.endsWith(getValueWrapper())) {
        	tempValue = tempValue.substring(0, tempValue.length() - getValueWrapper().length());
        }
        return tempValue;
    }

    public void set(final String field, final String value) {
        if (field != null && !field.isEmpty()) {
            values.put(field, value);
        }
    }

    public String get(final String field) {
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

    /**
     * Returns the field with the specified value properly formatted. Multiline
     * values are automatically indented, and dots are added on the empty lines.
     * 
     * <pre>
     * Field-Name: value
     * </pre>
     */
    private String formatField(final String value, final ControlField field) {
        StringBuilder s = new StringBuilder();
        
        if (value != null && value.trim().length() > 0) {
            boolean continuationLine = false;
            
            s.append(field.getName()).append(getDelimiter());
            if (field.isFirstLineEmpty()) {
                s.append("\n");
                continuationLine = true;
            }
            
            try {
                BufferedReader reader = new BufferedReader(new StringReader(getValueWrapper() + value + getValueWrapper()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (continuationLine && line.trim().length() == 0) {
                        // put a dot on the empty continuation lines
                        s.append(" .\n");
                    } else if (continuationLine) {
                    	s.append(" ").append(line).append("\n");
                    } else {
                        s.append(line).append("\n");
                    }

                    continuationLine = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return s.toString();
    }

    public String toString(final ControlField... fields) {
    	final StringBuilder s = new StringBuilder();
        for (ControlField field : fields) {
            String value = values.get(field.getName());
            s.append(formatField(value, field));
        }
        return s.toString();
    }

    public String toString() {
        final List<ControlField> fields = new ArrayList<ControlField>();
        for (String field : values.keySet()) {
        	ControlField controlField = getField(field);
        	if (controlField != null) {
        		fields.add(controlField);
        	} else {
        		fields.add(new ControlField(field));
        	}
        }
        return toString(fields.toArray(new ControlField[fields.size()]));
    }
    
    private ControlField getField(final String name) {
    	for (ControlField field : getFields()) {
        	if (field != null && field.getName() != null && field.getName().equals(name)) {
        		return field;
        	}
        }
    	return null;
    }

    protected abstract String getDelimiter();

    protected abstract String getValuePrefix();

    protected abstract String getValueWrapper();
}
