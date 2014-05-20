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
import org.vafer.jdeb.PackagingException;
import org.vafer.jdeb.SpkMaker;
import org.vafer.jdeb.producers.DataProducerFileSet;

/**
 * AntTask for creating synology archives.
 *
 * @author Torsten Curdt
 */
public class SpkAntTask extends MatchingTask {

    /** The Synology package produced */
    private File spk;

    /** The directory containing the scripts to build the package */
    private File scriptsDir;

    /** The path to the info file */
    private File infoFile;

    /** The compression method used for the data file (gzip) */
    private String compression = "gzip";

    /** Trigger the verbose mode detailing all operations */
    private boolean verbose;

    private Collection<Link> links = new ArrayList<Link>();

    private Collection<DataProducer> dataProducers = new ArrayList<DataProducer>();


    public void setDestfile( File spk ) {
        this.spk = spk;
    }

    public void setScripts( File scripts ) {
        this.scriptsDir = scripts;
    }

    public void setInfo( File infoFile ) {
        this.infoFile = infoFile;
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
            }
        }
        
        Console console = new TaskConsole(this, verbose);
        
        SpkMaker spkMaker = new SpkMaker(console, dataProducers);
        spkMaker.setSpk(spk);
        spkMaker.setScripts(scriptsDir);
        spkMaker.setInfo(infoFile);
        spkMaker.setCompression(compression);
        
        try {
            spkMaker.validate();
            spkMaker.makeSpk();
            
        } catch (PackagingException e) {
            log("Failed to create the Synology package " + spk, e, Project.MSG_ERR);
            throw new BuildException("Failed to create the Synology package " + spk, e);
        }
    }
}
