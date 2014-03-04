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
package org.vafer.jdeb.utils;

import java.util.Map;

/**
 * Resolve variables based on a Map.
 *
 * ATTENTION: don't use outside of jdeb
 *
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public final class MapVariableResolver implements VariableResolver {

    private static final String MVN_VAR_PREFIX = "${";
    private static final String MVN_VAR_SUFFIX = "}";
    
    private final Map<String, String> map;

    public MapVariableResolver( Map<String, String> map ) {
        this.map = map;
    }

    public String get( String key ) {
        String value = map.get(key);
        try {
            return resolveVariables(value);
        } catch (Exception e) {
            return value;
        }
    }
    
    private String resolveVariables(String value ) {
        String transformingValue = value;
        int searchVariableFromIndex = 0;
        while (!isEmptyOfVariable(transformingValue)) {
            int indexOfPrefix = value.indexOf(MVN_VAR_PREFIX, searchVariableFromIndex);
            int indexOfSuffix = value.indexOf(MVN_VAR_SUFFIX, searchVariableFromIndex) +1;
            searchVariableFromIndex = indexOfSuffix;

            String variable = value.substring(indexOfPrefix, indexOfSuffix);
            transformingValue = substituteVariable(transformingValue, variable);
        }
        return transformingValue;
    }

    private String substituteVariable(String transformingValue, String variable) {
        return transformingValue.replace(variable, get(stripVariable(variable)));
    }

    private String stripVariable(String variable) {
        return variable.substring(MVN_VAR_PREFIX.length(), variable.length() -1);
    }

    private boolean isEmptyOfVariable(String value) {
        int indexOfPrefix = value.indexOf(MVN_VAR_PREFIX);
        int indexOfSuffix = value.indexOf(MVN_VAR_SUFFIX);
        return value == null || indexOfSuffix <= indexOfPrefix;
    }

}
