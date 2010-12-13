/*
 * Copyright 2010 The Apache Software Foundation.
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * A ChangeSet basically reflect a release as defined in the changes file.
 *
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public final class ChangeSet {

    private final String packageName;
    private final String version;
    private final Date date;
    private final String distribution;
    private final String urgency;
    private final String changedBy;
    private final String[] changes;

    public ChangeSet( String pPackageName, String pVersion, Date pDate, String pDistribution, String pUrgency, String pChangedBy, final String[] pChanges ) {
        changes = pChanges;
        packageName = pPackageName;
        version = pVersion;
        date = pDate;
        distribution = pDistribution;
        urgency = pUrgency;
        changedBy = pChangedBy;
    }
    /*
     package (version) distribution(s); urgency=urgency
            [optional blank line(s), stripped]
       * change details
         more change details
            [blank line(s), included in output of dpkg-parsechangelog]
       * even more change details
            [optional blank line(s), stripped]
      -- maintainer name <email address>[two spaces]  date
    */

    public static DateFormat createDateForma() {
        return new SimpleDateFormat("HH:mm dd.MM.yyyy");
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

    public String toString() {
        final StringBuffer sb = new StringBuffer();

        sb.append(" ").append(getPackage()).append(" (").append(getVersion()).append(") ");
        sb.append(getDistribution()).append("; urgency=").append(getUrgency());
        for (int i = 0; i < changes.length; i++) {
            sb.append('\n').append(" * ").append(changes[i]);
        }

        return sb.toString();
    }
}
