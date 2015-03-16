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

package org.vafer.jdeb.changes;

import java.util.Date;

/**
 * A ChangeSet basically reflect a release as defined in the changes file.
 *
 * <pre>
 * package (version) distribution(s); urgency=urgency
 *        [optional blank line(s), stripped]
 *   * change details
 *     more change details
 *        [blank line(s), included in output of dpkg-parsechangelog]
 *   * even more change details
 *        [optional blank line(s), stripped]
 *  -- maintainer name &lt;email address&gt;[two spaces]  date
 * </pre>
 * 
 * @see <a href="http://www.debian.org/doc/debian-policy/ch-source.html#s-dpkgchangelog">Debian Policy Manual - Debian changelog</a>
 */
public final class ChangeSet {

    private final String packageName;
    private final String version;
    private final Date date;
    private final String distribution;
    private final String urgency;
    private final String changedBy;
    private final String[] changes;

    public ChangeSet(String packageName, String version, Date date, String distribution, String urgency, String changedBy, String[] changes) {
        this.packageName = packageName;
        this.version = version;
        this.date = date;
        this.distribution = distribution;
        this.urgency = urgency;
        this.changedBy = changedBy;
        this.changes = changes;
    }

    public String getPackage() {
        return packageName;
    }

    public String getVersion() {
        return version;
    }

    public Date getDate() {
        return date;
    }

    public String getDistribution() {
        return distribution;
    }

    public String getUrgency() {
        return urgency;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String[] getChanges() {
        return changes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(getTitle()).append('\n');
        
        if (changes.length > 0) {
            sb.append("\n");
        }
        
        for (String change : changes) {
            sb.append("  * ").append(change).append('\n');
        }

        return sb.toString();
    }

    private String getTitle() {
        return getPackage() + " (" + getVersion() + ") " + getDistribution() + "; urgency=" + getUrgency();
    }
}
