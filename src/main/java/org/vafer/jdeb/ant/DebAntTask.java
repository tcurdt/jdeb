/*
 * Copyright 2007-2024 The jdeb developers.
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

package org.vafer.jdeb.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.types.FileSet;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.DebMaker;
import org.vafer.jdeb.PackagingException;
import org.vafer.jdeb.producers.DataProducerFileSet;
import org.vafer.jdeb.utils.OutputTimestampResolver;

/**
 * AntTask for creating debian archives.
 */
public class DebAntTask extends MatchingTask {

    private File deb;
    private File control;
    private File keyring;
    private String key;
    private String passphrase;
    private File changesIn;
    private File changesOut;
    private File changesSave;
    private String compression = "gzip";
    private String digest = "SHA256";
    private boolean verbose;

    private Collection<Link> links = new ArrayList<>();
    private Collection<DataProducer> dataProducers = new ArrayList<>();
    private Collection<DataProducer> conffilesProducers = new ArrayList<>();

    // getters and setters

    public void execute() {
        dataProducers.addAll(links.stream().map(Link::toDataProducer).collect(Collectors.toList()));

        for (DataProducer dataProducer : dataProducers) {
            if (dataProducer instanceof Data) {
                Data data = (Data) dataProducer;
                if (data.getType() == null) {
                    throw new BuildException("The type of the data element wasn't specified (expected 'file', 'directory' or 'archive')");
                } else if (!Arrays.asList("file", "directory", "archive").contains(data.getType().toLowerCase())) {
                    throw new BuildException("The type '" + data.getType() + "' of the data element is unknown (expected 'file', 'directory' or 'archive')");
                }
                if (data.getConffile() != null && data.getConffile()) {
                    conffilesProducers.add(dataProducer);
                }
            }
        }

        Console console = new TaskConsole(this, verbose);

        DebMaker debMaker = new DebMaker(console, dataProducers, conffilesProducers);
        debMaker.setDeb(deb);
        debMaker.setControl(control);
        debMaker.setChangesIn(changesIn);
        debMaker.setChangesOut(changesOut);
        debMaker.setChangesSave(changesSave);
        debMaker.setKeyring(keyring);
        debMaker.setKey(key);
        debMaker.setPassphrase(passphrase);
        debMaker.setCompression(compression);
        debMaker.setDigest(digest);
        Long outputTimestampMs = new OutputTimestampResolver(console).resolveOutputTimestamp(null);
        debMaker.setOutputTimestampMs(outputTimestampMs);

        try {
            debMaker.validate();
            debMaker.makeDeb();

        } catch (PackagingException e) {
            log("Failed to create the Debian package " + deb, e, Project.MSG_ERR);
            throw new BuildException("Failed to create the Debian package " + deb, e);
        }
    }
}
