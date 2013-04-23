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

package org.vafer.jdeb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.vafer.jdeb.changes.TextfileChangesProvider;
import org.vafer.jdeb.debian.BinaryPackageControlFile;
import org.vafer.jdeb.debian.ChangesFile;
import org.vafer.jdeb.signing.PGPSigner;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * A generic class for creating Debian archives. Even supports signed changes
 * files.
 *
 * @author Bryan Sant
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

    private VariableResolver variableResolver;

    private final Collection<DataProducer> dataProducers = new ArrayList<DataProducer>();


    public DebMaker(Console console, Collection<DataProducer> dataProducers) {
        this.console = console;
        this.dataProducers.addAll(dataProducers);
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

    public void setResolver(VariableResolver variableResolver) {
        this.variableResolver = variableResolver;
    }

    private boolean isWritableFile(File file) {
        return file.isFile() && file.canWrite();
    }

    /**
     * Validates the input parameters.
     */
    public void validate() throws PackagingException {
        if (control == null || !control.isDirectory()) {
            throw new PackagingException("The 'control' attribute doesn't point to a directory.");
        }

        if (changesIn != null) {

            if (!changesIn.isFile() || !changesIn.canRead()) {
                throw new PackagingException("The 'changesIn' setting needs to point to a readable file. " + changesIn + " was not found/readable.");
            }

            if (changesOut == null) {
                throw new PackagingException("A 'changesIn' without a 'changesOut' does not make much sense.");
            }

            if (!isWritableFile(changesOut)) {
                throw new PackagingException("Cannot write the output for 'changesOut' to " + changesOut);
            }

            if (changesSave != null && !isWritableFile(changesSave)) {
                throw new PackagingException("Cannot write the output for 'changesSave' to " + changesSave);
            }

        } else {
            if (changesOut != null || changesSave != null) {
                throw new PackagingException("The 'changesOut' or 'changesSave' settings may only be used when there is a 'changesIn' specified.");
            }
        }

        if (Compression.toEnum(compression) == null) {
            throw new PackagingException("The compression method '" + compression + "' is not supported (expected 'none', 'gzip' or 'bzip2')");
        }

        if (deb == null) {
            throw new PackagingException("You need to specify where the deb file is supposed to be created.");
        }
        
        if (dataProducers.size() == 0) {
            throw new PackagingException("You need to provide at least one reference to a tgz or directory with data.");
        }
    }

    public void makeDeb() throws PackagingException {
        final File[] controlFiles = control.listFiles();

        final DataProducer[] data = new DataProducer[dataProducers.size()];
        dataProducers.toArray(data);

        final Processor processor = new Processor(console, variableResolver);

        final BinaryPackageControlFile packageControlFile;
        try {

            console.info("Creating debian package: " + deb);

            packageControlFile = processor.createDeb(controlFiles, data, deb, Compression.toEnum(compression));

        } catch (Exception e) {
            throw new PackagingException("Failed to create debian package " + deb, e);
        }
        
        makeChangesFiles(processor, packageControlFile);
    }

    private void makeChangesFiles(Processor processor, BinaryPackageControlFile packageControlFile) throws PackagingException {
        if (changesOut == null) {
            return;
        }
        
        TextfileChangesProvider changesProvider;
        FileOutputStream out = null;
        
        try {
            console.info("Creating changes file: " + changesOut);
            
            out = new FileOutputStream(changesOut);

            // for now only support reading the changes form a textfile provider
            changesProvider = new TextfileChangesProvider(new FileInputStream(changesIn), packageControlFile);
            
            ChangesFile changesFile = processor.createChanges(packageControlFile, deb, changesProvider);
            
            if (keyring != null && key != null && passphrase != null) {
                console.info("Signing the changes file with the key " + key);
                PGPSigner signer = new PGPSigner(new FileInputStream(keyring), key, passphrase);
                signer.clearSign(changesFile.toString(), out);
            } else {
                out.write(changesFile.toString().getBytes("UTF-8"));
            }
            out.flush();

        } catch (Exception e) {
            throw new PackagingException("Failed to create the Debian changes file " + changesOut, e);
        } finally {
            IOUtils.closeQuietly(out);
        }
        
        if (changesSave == null) {
            return;
        }
        
        try {
            console.info("Saving changes to file: " + changesSave);

            changesProvider.save(new FileOutputStream(changesSave));

        } catch (Exception e) {
            throw new PackagingException("Failed to save debian changes file " + changesSave, e);
        }
    }
}
