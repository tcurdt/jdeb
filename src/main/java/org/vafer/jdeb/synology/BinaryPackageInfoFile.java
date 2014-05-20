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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.vafer.jdeb.utils.ControlField;

/**
 * Binary package info file.
 *
 * @author Torsten Curdt
 */
public final class BinaryPackageInfoFile extends InfoFile {

    private static final ControlField[] FIELDS = {
            new ControlField("package", true),
            new ControlField("version", true),
            new ControlField("maintainer", true),
            new ControlField("description", true, ControlField.Type.MULTILINE),
            new ControlField("arch", true),
            new ControlField("adminport"),
            new ControlField("adminurl"),
            new ControlField("firmware"),
            new ControlField("reloadui"),
            new ControlField("package_icon"),
            new ControlField("extractsize"),
            new ControlField("checksum")
    };

    public BinaryPackageInfoFile() {
        set("arch", "noarch");
    }

    public BinaryPackageInfoFile(final String input) throws IOException, ParseException {
        parse(input);
    }

    public BinaryPackageInfoFile(final InputStream input) throws IOException, ParseException {
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
        if (get("description") == null) {
            return null;
        }

        return get("description").split("\n")[0];
    }

	protected String getDelimiter() {
		return "=";
	}

	protected String getValuePrefix() {
		return "";
	}

	protected String getValueWrapper() {
		return "\"";
	}
}
