/*
 * Copyright 2012 The Apache Software Foundation.
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
package org.vafer.jdeb.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.PackagingException;
import org.vafer.jdeb.Processor;
import org.vafer.jdeb.changes.TextfileChangesProvider;
import org.vafer.jdeb.descriptors.PackageDescriptor;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * TODO generalize with DebAntTask
 *
 * A generic class for creating debian archives. Even supports signed changes
 * files.
 *
 * This class is largely based on the DebAntTask class, and provides the same
 * featues to maven developers that the ant task provides to ant developers.
 *
 * @see org.vafer.jdeb.ant.DebAntTask
 * @author Bryan Sant <bryan.sant@gmail.com>
 */
public class DebMaker {

    /** A console to output log message with */
    private Console console;

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

    /** The compression method used for the data file (none, gzip or bzip2) */
    private String compression = "gzip";

    private final VariableResolver variableResolver;


    private final Collection dataProducers;

    public DebMaker(Console console, VariableResolver variableResolver) {
        this(console, null, null, null, variableResolver);
    }

    public DebMaker(Console console, File deb, File controlDir, VariableResolver variableResolver) {
        this(console, deb, controlDir, null, variableResolver);
    }

    public DebMaker(Console console, File deb, File controlDir, Collection dataProducers, VariableResolver variableResolver) {
        this.console = console;
        this.deb = deb;
        this.control = controlDir;
        this.variableResolver = variableResolver;
        if (dataProducers != null) {
            this.dataProducers = dataProducers;
        } else {
            this.dataProducers = new ArrayList();
        }
    }

    public void setDeb(File deb) {
        this.deb = deb;
    }

    public void setControl(File control) {
        this.control = control;
    }

    public void setChangesIn(File changes) {
        this.changesIn = changes;
    }

    public void setChangesOut(File changes) {
        this.changesOut = changes;
    }

    public void setChangesSave(File changes) {
        this.changesSave = changes;
    }

    public void setKeyring(File keyring) {
        this.keyring = keyring;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    /**
     * Adds a new data source to the deb. The new data source may point to a
     * single file, a directory, or a tarball.
     *
     * @param file
     *            The data source to add
     */
    public void addData(File file) {
        final Data data = new Data();
        data.setSrc(file);
        dataProducers.add(data);
    }

    public void addData(Data data) {
        dataProducers.add(data);
    }

    private boolean isPossibleOutput(File file) {

        if (file.exists()) {
            return file.isFile() && file.canWrite();
        }

        return true;
    }

    public void makeDeb() throws PackagingException {

        if (control == null || !control.isDirectory()) {
            throw new PackagingException(
                "\"" + control + "\" is not a valid 'control' directory)");
        }

        if (changesIn != null) {

            if (!changesIn.isFile() || !changesIn.canRead()) {
                throw new PackagingException(
                        "The 'changesIn' setting needs to point to a readable file. "
                                + changesIn + " was not found/readable.");
            }

            if (changesOut == null) {
                throw new PackagingException(
                        "A 'changesIn' without a 'changesOut' does not make much sense.");
            }

            if (!isPossibleOutput(changesOut)) {
                throw new PackagingException(
                        "Cannot write the output for 'changesOut' to "
                                + changesOut);
            }

            if (changesSave != null && !isPossibleOutput(changesSave)) {
                throw new PackagingException(
                        "Cannot write the output for 'changesSave' to "
                                + changesSave);
            }

        } else {
            if (changesOut != null || changesSave != null) {
                throw new PackagingException(
                        "The 'changesOut' or 'changesSave' settings may only be used when there is a 'changesIn' specified.");
            }
        }

        if (!"gzip".equals(compression) && !"bzip2".equals(compression)
                && !"none".equals(compression)) {
            throw new PackagingException("The compression method '"
                    + compression + "' is not supported");
        }

        if (dataProducers.size() == 0) {
            throw new PackagingException(
                    "You need to provide at least one reference to a tgz or directory with data.");
        }

        if (deb == null) {
            throw new PackagingException(
                    "You need to specify where the deb file is supposed to be created.");
        }

        final File[] controlFiles = control.listFiles();

      console.println("control dir:" + control);
        for(File file : controlFiles) {
          console.println("-" + file);
        }
        
        final DataProducer[] data = new DataProducer[dataProducers.size()];
        dataProducers.toArray(data);

        final Processor processor = new Processor(console, variableResolver);

        final PackageDescriptor packageDescriptor;
        try {

            console.println("Creating debian package: " + deb);

            packageDescriptor = processor.createDeb(controlFiles, data, deb, compression);

        } catch (Exception e) {
            throw new PackagingException("Failed to create debian package " + deb, e);
        }

        final TextfileChangesProvider changesProvider;

        try {
            if (changesOut == null) {
                return;
            }

            console.println("Creating changes file: " + changesOut);

            // for now only support reading the changes form a textfile provider
            changesProvider = new TextfileChangesProvider(new FileInputStream(
                    changesIn), packageDescriptor);

            processor.createChanges(packageDescriptor, changesProvider,
                    (keyring != null) ? new FileInputStream(keyring) : null,
                    key, passphrase, new FileOutputStream(changesOut));

        } catch (Exception e) {
            throw new PackagingException(
                    "Failed to create debian changes file " + changesOut, e);
        }

        try {
            if (changesSave == null) {
                return;
            }

            console.println("Saving changes to file: " + changesSave);

            changesProvider.save(new FileOutputStream(changesSave));

        } catch (Exception e) {
            throw new PackagingException("Failed to save debian changes file " + changesSave, e);
        }

    }
}
