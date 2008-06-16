/*
 * Copyright 2005 The Apache Software Foundation.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.types.FileSet;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.Processor;
import org.vafer.jdeb.changes.TextfileChangesProvider;
import org.vafer.jdeb.descriptors.PackageDescriptor;

/**
 * AntTask for creating debian archives.
 * Even supports signed changes files.
 * 
 * @author tcurdt
 */
		
public class DebAntTask extends MatchingTask {

	/** The Debian package produced */
	private File deb;

	/** The directory containing the control files to build the package */
	private File control;

	private File keyring;
	private File changesIn;
	private File changesOut;
	private File changesSave;
	private String key;
	private String passphrase;
	private boolean verbose;

	private Collection dataProducers = new ArrayList();


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

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void addFileSet(FileSet fileset) {
		dataProducers.add(new FileSetDataProducer(fileset));
	}

	public void addTarFileSet(Tar.TarFileSet fileset) {
		dataProducers.add(new FileSetDataProducer(fileset));
	}

	public void addData( Data data ) {
    	dataProducers.add(data);
    }
    
    private boolean isPossibleOutput( File file ) {
    	
    	if (file.exists()) {    		
    		return file.isFile() && file.canWrite();
    	}
    	
    	return true;
    }
    
	public void execute() {
		
		if (control == null || !control.isDirectory()) {
			throw new BuildException("You need to point the 'control' attribute to the control directory.");
		}

		if (changesIn != null) {
			
			if (!changesIn.isFile() || !changesIn.canRead()) {
				throw new BuildException("The 'changesIn' attribute needs to point to a readable file. " + changesIn + " was not found/readable.");				
			}

			if (changesOut == null) {
				throw new BuildException("A 'changesIn' without a 'changesOut' does not make much sense.");
			}
			
			if (!isPossibleOutput(changesOut)) {
				throw new BuildException("Cannot write the output for 'changesOut' to " + changesOut);				
			}

			if (changesSave != null && !isPossibleOutput(changesSave)) {
				throw new BuildException("Cannot write the output for 'changesSave' to " + changesSave);				
			}
			
		} else {
			if (changesOut != null || changesSave != null) {
				throw new BuildException("The 'changesOut' or 'changesSave' attributes may only be used when there is a 'changesIn' specified.");							
			}
		}
				
		if (dataProducers.size() == 0) {
			throw new BuildException("You need to provide at least one reference to a tgz or directory with data.");
		}

		if (deb == null) {
			throw new BuildException("You need to point the 'destfile' attribute to where the deb is supposed to be created.");
		}
		
		final File[] controlFiles = control.listFiles();
		
		final DataProducer[] data = new DataProducer[dataProducers.size()];
		dataProducers.toArray(data);
		
		final Processor processor = new Processor(new Console() {
			public void println(String s) {
				if (verbose) {
					log(s);
				}
			}
		}, null);
		
		final PackageDescriptor packageDescriptor;
		try {

			packageDescriptor = processor.createDeb(controlFiles, data, deb);

			log("Created " + deb);

		} catch (Exception e) {
			throw new BuildException("Failed to create debian package " + deb, e);
		}

		final TextfileChangesProvider changesProvider;
		
		try {
			if (changesOut == null) {
				return;
			}

			// for now only support reading the changes form a textfile provider
			changesProvider = new TextfileChangesProvider(new FileInputStream(changesIn), packageDescriptor);
			
			processor.createChanges(packageDescriptor, changesProvider, (keyring!=null)?new FileInputStream(keyring):null, key, passphrase, new FileOutputStream(changesOut));

			log("Created changes file " + changesOut);
						
		} catch (Exception e) {
			throw new BuildException("Failed to create debian changes file " + changesOut, e);
		}

		try {
			if (changesSave == null) {
				return;
			}

			changesProvider.save(new FileOutputStream(changesSave));

			log("Saved changes to file " + changesSave);
			
		} catch (Exception e) {
			throw new BuildException("Failed to save debian changes file " + changesSave, e);
		}
				
	}
}
