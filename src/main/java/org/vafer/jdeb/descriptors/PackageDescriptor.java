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

/**
 * Reflecting the package control file
 *
 * @see <a href="http://www.debian.org/doc/debian-policy/ch-controlfields.html#s-binarycontrolfiles">Debian Policy Manual - Binary package control files</a>
 * @author Torsten Curdt
 */
public final class PackageDescriptor extends AbstractDescriptor {

    private static final ControlField[] FIELDS = {
            new ControlField("Package", true),
            new ControlField("Source"),
            new ControlField("Version", true),
            new ControlField("Section", true),
            new ControlField("Priority", true),
            new ControlField("Architecture", true),
            new ControlField("Essential"),
            new ControlField("Depends"),
            new ControlField("Pre-Depends"),
            new ControlField("Recommends"),
            new ControlField("Suggests"),
            new ControlField("Breaks"),
            new ControlField("Enhances"),
            new ControlField("Conflicts"),
            new ControlField("Provides"),
            new ControlField("Replaces"),
            new ControlField("Installed-Size"),
            new ControlField("Maintainer", true),
            new ControlField("Description", true, ControlField.Type.MULTILINE),
            new ControlField("Homepage")
    };

    public PackageDescriptor() {
    }

    public PackageDescriptor(String input) throws IOException, ParseException {
        parse(input);
    }

    public PackageDescriptor(InputStream input) throws IOException, ParseException {
        parse(input);
    }

    @Override
    protected ControlField[] getFields() {
        return FIELDS;
    }
}
