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

/**
 * A field of a control file.
 *
 * @author Emmanuel Bourg
 */
public class ControlField {

    public enum Type {SIMPLE, FOLDED, MULTILINE}
    
    private String name;
    private boolean mandatory;
    private Type type = Type.SIMPLE;

    public ControlField(String name) {
        this(name, false);
    }

    public ControlField(String name, boolean mandatory) {
        this(name, mandatory, Type.SIMPLE);
    }

    public ControlField(String name, boolean mandatory, Type type) {
        this.name = name;
        this.mandatory = mandatory;
        this.type = type;
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
}
