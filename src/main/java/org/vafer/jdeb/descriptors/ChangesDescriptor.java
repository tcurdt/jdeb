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

import org.vafer.jdeb.changes.ChangeSet;

/**
 * Reflecting a changes file
 *
 * @see <a href="http://www.debian.org/doc/debian-policy/ch-controlfields.html#s-debianchangesfiles">Debian Policy Manual - Debian changes files</a>
 * @author Torsten Curdt
 */
public final class ChangesDescriptor extends AbstractDescriptor {

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
            new ControlField("Description", true, ControlField.Type.MULTILINE),
            new ControlField("Changes", true, ControlField.Type.MULTILINE),
            new ControlField("Closes"),
            new ControlField("Checksums-Sha1", true, ControlField.Type.MULTILINE),
            new ControlField("Checksums-Sha256", true, ControlField.Type.MULTILINE),
            new ControlField("Files", true, ControlField.Type.MULTILINE)
    };

    public ChangesDescriptor( final AbstractDescriptor pDescriptor, final ChangeSet[] changeSets ) {
        super(pDescriptor);

        final StringBuilder sb = new StringBuilder();

        if (changeSets.length > 0) {
            final ChangeSet latestChangeSet = changeSets[0];
            set("Urgency", latestChangeSet.getUrgency());
            set("Changed-By", latestChangeSet.getChangedBy());

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
}
