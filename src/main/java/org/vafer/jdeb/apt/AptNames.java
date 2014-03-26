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
package org.vafer.jdeb.apt;

import java.util.regex.Pattern;

/**
 * A APT name helper
 * 
 * @author Jens Reimann
 * 
 */
final class AptNames {
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9]+");

    public static void validate( String fieldName, String name ) {
        if (name == null || name.isEmpty())
            throw new NullPointerException("'" + fieldName + "' must not be null or empty");
        if (!NAME_PATTERN.matcher(name).matches())
            throw new IllegalArgumentException("'" + fieldName + "' must match pattern: " + NAME_PATTERN.pattern());
    }
}
