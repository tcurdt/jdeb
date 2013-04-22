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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.vafer.jdeb.utils.VariableResolver;

/**
 * Reflecting the package control file
 *
 * @see <a href="http://www.debian.org/doc/debian-policy/ch-controlfields.html#s-binarycontrolfiles">Debian Policy Manual - Binary package control files</a>
 * @author Torsten Curdt
 */
public final class PackageDescriptor extends AbstractDescriptor {

    private static final String[] FIELDS = {
        "Package",
        "Source",
        "Version",
        "Section",
        "Priority",
        "Architecture",
        "Essential",
        "Depends",
        "Pre-Depends",
        "Recommends",
        "Suggests",
        "Breaks",
        "Enhances",
        "Conflicts",
        "Provides",
        "Replaces",
        "Installed-Size",
        "Maintainer",
        "Description",
        "Homepage",
    };

    public static final String[] MANDATORY_FIELDS = {
        "Package",
        "Version",
        "Section",
        "Priority",
        "Architecture",
        "Maintainer",
        "Description"
    };

    public PackageDescriptor() {
        this(null);
    }

    public PackageDescriptor( final VariableResolver pResolver ) {
        super(pResolver);
    }

    public PackageDescriptor( final InputStream pInput, final VariableResolver pResolver ) throws IOException, ParseException {
        this(pResolver);
        parse(pInput);
    }

    public String[] getMandatoryFields() {
        return MANDATORY_FIELDS;
    }

    public String toString() {
        return toString(FIELDS);
    }

}
