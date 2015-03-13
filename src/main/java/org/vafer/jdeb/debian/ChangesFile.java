/*
 * Copyright 2015 The jdeb developers.
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

import org.vafer.jdeb.changes.ChangeSet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map.Entry;

/**
 * Reflecting a changes file
 *
 * @see <a href="http://www.debian.org/doc/debian-policy/ch-controlfields.html#s-debianchangesfiles">Debian Policy Manual - Debian changes files</a>
 */
public final class ChangesFile extends ControlFile {

    private static final ControlField[] FIELDS = {
            new ControlField("Format", true),
            new ControlField("Date", true),
            new ControlField("Source", true),
            new ControlField("Binary", true),
            new ControlField("Architecture", true),
            new ControlField("Version", true),
            new ControlField("Distribution", true),
            new ControlField("Urgency", true),
            new ControlField("Maintainer", true),
            new ControlField("Changed-By"),
            new ControlField("Description", true, ControlField.Type.MULTILINE, true),
            new ControlField("Changes", true, ControlField.Type.MULTILINE, true),
            new ControlField("Closes"),
            new ControlField("Checksums-Sha1", true, ControlField.Type.MULTILINE, true),
            new ControlField("Checksums-Sha256", true, ControlField.Type.MULTILINE, true),
            new ControlField("Files", true, ControlField.Type.MULTILINE, true)
    };

    public ChangesFile() {
        set("Format", "1.8");
        set("Urgency", "low");
        set("Distribution", "stable");
    }

    /**
     * Initializes the fields on the changes file with the values of the specified
     * binary package control file.
     *
     * @param packageControlFile
     */
    public void initialize(BinaryPackageControlFile packageControlFile) {
        set("Binary",       packageControlFile.get("Package"));
        set("Source",       packageControlFile.get("Package"));
        set("Architecture", packageControlFile.get("Architecture"));
        set("Version",      packageControlFile.get("Version"));
        set("Maintainer",   packageControlFile.get("Maintainer"));
        set("Changed-By",   packageControlFile.get("Maintainer"));
        set("Distribution", packageControlFile.get("Distribution"));

        for (Entry<String, String> entry : packageControlFile.getUserDefinedFields().entrySet()) {
            set(entry.getKey(), entry.getValue());
        }

        StringBuilder description = new StringBuilder();
        description.append(packageControlFile.get("Package"));
        if (packageControlFile.get("Description") != null) {
            description.append(" - ");
            description.append(packageControlFile.getShortDescription());
        }
        set("Description",  description.toString());
    }

    public void setChanges(ChangeSet[] changeSets) {
        StringBuilder sb = new StringBuilder();

        if (changeSets.length > 0) {
            final ChangeSet mostRecentChangeSet = changeSets[0];
            set("Urgency", mostRecentChangeSet.getUrgency());
            set("Changed-By", mostRecentChangeSet.getChangedBy());

            for (ChangeSet changeSet : changeSets) {
                sb.append(changeSet.toString());
            }
        }

        set("Changes", sb.toString());
    }

    @Override
    protected ControlField[] getFields() {
        return FIELDS;
    }

    @Override
    protected char getUserDefinedFieldLetter() {
        return 'C';
    }

    public static String formatDate(Date date) {
        final DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH); // RFC 2822 format
        return format.format(date);
    }
}
