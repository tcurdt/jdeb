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

package org.vafer.jdeb.debian;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * A field of a control file. This class is immutable.
 *
 * @author Emmanuel Bourg
 */
public class ControlField {

    /**
     * The format of a field.
     */
    public enum Type {
        /** Value on a single line */
        SIMPLE,
        /** Value on multiple lines, space characters are ignored */
        FOLDED,
        /** Value on multiple lines, space characters are preserved */
        MULTILINE
    }

    /** The name of the field */
    private String name;

    /** Tells if the field is mandatory */
    private boolean mandatory;

    /** The type of the field */
    private Type type = Type.SIMPLE;

    /** Tells is the first line of the field must be empty (for MULTILINE values only) */
    private boolean firstLineEmpty;


    public ControlField(String name) {
        this(name, false);
    }

    public ControlField(String name, boolean mandatory) {
        this(name, mandatory, Type.SIMPLE);
    }

    public ControlField(String name, boolean mandatory, Type type) {
        this(name, mandatory, type, false);
    }

    public ControlField(String name, boolean mandatory, Type type, boolean firstLineEmpty) {
        this.name = name;
        this.mandatory = mandatory;
        this.type = type;
        this.firstLineEmpty = firstLineEmpty;
    }

    public String getName() {
        return name;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public Type getType() {
        return type;
    }

    public boolean isFirstLineEmpty() {
        return firstLineEmpty;
    }

    /**
     * Returns the field with the specified value properly formatted. Multiline
     * values are automatically indented, and dots are added on the empty lines.
     * 
     * <pre>
     * Field-Name: value
     * </pre>
     */
    public String format(String value) {
        StringBuilder s = new StringBuilder();
        
        if (value != null && value.trim().length() > 0) {
            boolean continuationLine = false;
            
            s.append(getName()).append(":");
            if (isFirstLineEmpty()) {
                s.append("\n");
                continuationLine = true;
            }
            
            try {
                BufferedReader reader = new BufferedReader(new StringReader(value));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (continuationLine && line.trim().length() == 0) {
                        // put a dot on the empty continuation lines
                        s.append(" .\n");
                    } else {
                        s.append(" ").append(line).append("\n");
                    }

                    continuationLine = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return s.toString();
    }
}
