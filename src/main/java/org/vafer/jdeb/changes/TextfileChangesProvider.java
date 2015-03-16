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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.vafer.jdeb.debian.BinaryPackageControlFile;

/**
 * Gets the changes from a changes file. The first entry are the current changes.
 * The release line will be added. Example:
 *
 * release date=22:13 19.08.2007,version=1.5+r90114,urgency=low,by=Torsten Curdt &lt;torsten@vafer.org&gt;
 *   * debian changes support
 * release date=20:13 17.08.2007,version=1.4+r89114,urgency=low,by=Torsten Curdt &lt;torsten@vafer.org&gt;
 *   * debian changes support
 *
 */
public final class TextfileChangesProvider implements ChangesProvider {

    private final ChangeSet[] changeSets;
    
    private DateFormat fmt = new SimpleDateFormat("HH:mm dd.MM.yyyy");

    public TextfileChangesProvider( final InputStream pInput, final BinaryPackageControlFile packageControlFile ) throws IOException, ParseException {

        final BufferedReader reader = new BufferedReader(new InputStreamReader(pInput));
        
        String packageName = packageControlFile.get("Package");
        String version = packageControlFile.get("Version");
        Date date = new Date();
        String distribution = packageControlFile.get("Distribution");
        String urgency = packageControlFile.get("Urgency");
        String changedBy = packageControlFile.get("Maintainer");
        Collection<String> changesColl = new ArrayList<String>();
        Collection<ChangeSet> changeSetColl = new ArrayList<ChangeSet>();


        while (true) {
            final String line = reader.readLine();
            if (line == null) {
                final String[] changes = changesColl.toArray(new String[changesColl.size()]);
                final ChangeSet changeSet = new ChangeSet(packageName, version, date, distribution, urgency, changedBy, changes);
                changeSetColl.add(changeSet);
                break;
            }

            if (line.startsWith("release ")) {

                if (changesColl.size() > 0) {
                    final String[] changes = changesColl.toArray(new String[changesColl.size()]);
                    final ChangeSet changeSet = new ChangeSet(packageName, version, date, distribution, urgency, changedBy, changes);
                    changeSetColl.add(changeSet);
                    changesColl.clear();
                }

                final String[] tokens = line.substring("release ".length()).split(",");
                for (String token : tokens) {
                    final String[] lr = token.trim().split("=");
                    final String key = lr[0];
                    final String value = lr[1];

                    if ("urgency".equals(key)) {
                        urgency = value;
                    } else if ("by".equals(key)) {
                        changedBy = value;
                    } else if ("date".equals(key)) {
                        date = fmt.parse(value);
                    } else if ("version".equals(key)) {
                        version = value;
                    } else if ("distribution".equals(key)) {
                        distribution = value;
                    }
                }
                continue;
            }

            if (line.startsWith(" * ")) {
                changesColl.add(line.substring(" * ".length()));
                continue;
            }

            throw new ParseException("Unknown line syntax [" + line + "]", 0);
        }

        reader.close();

        changeSets = changeSetColl.toArray(new ChangeSet[changeSetColl.size()]);
    }

    public void save(OutputStream pOutput) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(pOutput));

        for (ChangeSet changeSet : changeSets) {
            writer.write("release ");
            writer.write("date=" + fmt.format(changeSet.getDate()) + ",");
            writer.write("version=" + changeSet.getVersion() + ",");
            writer.write("urgency=" + changeSet.getUrgency() + ",");
            writer.write("by=" + changeSet.getChangedBy() + ",");
            writer.write("distribution=" + changeSet.getDistribution());
            writer.write("\n");

            for (String change : changeSet.getChanges()) {
                writer.write(" * ");
                writer.write(change);
                writer.write("\n");
            }
        }

        writer.close();
    }

    @Override
    public ChangeSet[] getChangesSets() {
        return changeSets;
    }
}
