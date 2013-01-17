/*
 * Copyright 2013 The Apache Software Foundation.
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
 * @author Torsten Curdt
 */
public final class ChangesDescriptor extends AbstractDescriptor {

    private static final String[] KEYS = {
        "Format",
        "Date",
        "Source",
        "Binary",
        "Architecture",
        "Version",
        "Distribution",
        "Urgency",
        "Maintainer",
        "Changed-By",
        "Description",
        "Changes",
        "Closes",
        "Checksums-Sha1",
        "Checksums-Sha256",
        "Files"
    };

    public static final String[] MANDATORY_KEYS = {
        "Format",
        "Date",
        "Source",
        "Binary",
        "Architecture",
        "Version",
        "Distribution",
        "Urgency",
        "Maintainer",
        "Description",
        "Changes",
        "Checksums-Sha1",
        "Checksums-Sha256",
        "Files"
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

    public String[] getMandatoryKeys() {
        return MANDATORY_KEYS;
    }

    public String toString() {
        return toString(KEYS);
    }
}
