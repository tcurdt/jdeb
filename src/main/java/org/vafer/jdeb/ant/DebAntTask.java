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

/**
 * AntTask for creating debian archives.
 *
 * @author Torsten Curdt
 */
public class DebAntTask extends MatchingTask {

    /** The Debian package produced */
    private File deb;

    /** The directory containing the control files to build the package */
    private File control;

    /** The file containing the PGP keys */
    private File keyring;

    /** The key to use in the keyring */
    private String key;

    /** The passphrase for the key to sign the changes file */
    private String passphrase;

    /** The file to read the changes from */
    private File changesIn;

    /** The file where to write the changes to */
    private File changesOut;

    /** The file where to write the changes of the changes input to */
    private File changesSave;

    /** The compression method used for the data file (none, gzip, bzip2 or xz) */
    private String compression = "gzip";

    /** Trigger the verbose mode detailing all operations */
    private boolean verbose;

    private Collection<Link> links = new ArrayList<Link>();

    private Collection<DataProducer> dataProducers = new ArrayList<DataProducer>();
    private Collection<DataProducer> conffilesProducers = new ArrayList<DataProducer>();


    public void setDestfile( File deb ) {
        this.deb = deb;
    }

    public void setControl( File control ) {
        this.control = control;
    }

    public void setChangesIn( File changes ) {
        this.changesIn = changes;
    }

    public void setChangesOut( File changes ) {
        this.changesOut = changes;
    }

    public void setChangesSave( File changes ) {
        this.changesSave = changes;
    }

    public void setKeyring( File keyring ) {
        this.keyring = keyring;
    }

    public void setKey( String key ) {
        this.key = key;
    }

    public void setPassphrase( String passphrase ) {
        this.passphrase = passphrase;
    }

    public void setCompression( String compression ) {
        this.compression = compression;
    }

    public void setVerbose( boolean verbose ) {
        this.verbose = verbose;
    }

    public void addFileSet( FileSet fileset ) {
        dataProducers.add(new DataProducerFileSet(fileset));
    }

    public void addTarFileSet( Tar.TarFileSet fileset ) {
        dataProducers.add(new DataProducerFileSet(fileset));
    }

    public void addData( Data data ) {
        dataProducers.add(data);
    }

    public void addLink( Link link ) {
        links.add(link);
    }

    public void execute() {
        // add the data producers for the links
        for (Link link : links) {
            dataProducers.add(link.toDataProducer());
        }
        
        // validate the type of the <data> elements
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
        
        try {
            debMaker.validate();
            debMaker.makeDeb();
            
        } catch (PackagingException e) {
            log("Failed to create the Debian package " + deb, e, Project.MSG_ERR);
            throw new BuildException("Failed to create the Debian package " + deb, e);
        }
    }
}
