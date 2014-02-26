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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

/**
 * Binary package control file.
 *
 * @see <a href="http://www.debian.org/doc/debian-policy/ch-controlfields.html#s-binarycontrolfiles">Debian Policy Manual - Binary package control files</a>
 * @author Torsten Curdt
 */
public final class BinaryPackageControlFile extends ControlFile {

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

    public BinaryPackageControlFile() {
        set("Architecture", "all");
        set("Priority", "optional");
    }

    public BinaryPackageControlFile(String input) throws IOException, ParseException {
        parse(input);
    }

    public BinaryPackageControlFile(InputStream input) throws IOException, ParseException {
        parse(input);
    }

    @Override
    public void set(final String field, final String value) {
        super.set(field, value);
    }

    @Override
    protected ControlField[] getFields() {
        return FIELDS;
    }

    /**
     * Returns the short description of the package. The short description
     * consists in the first line of the Description field.
     *
     * @return
     */
    public String getShortDescription() {
        if (get("Description") == null) {
            return null;
        }

        return get("Description").split("\n")[0];
    }

    @Override
    protected char getUserDefinedFieldLetter() {
        return 'B';
    }
}
