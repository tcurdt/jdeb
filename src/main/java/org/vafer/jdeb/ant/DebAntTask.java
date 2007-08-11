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
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.Processor;
import org.vafer.jdeb.descriptors.ChangesDescriptor;

public class DebAntTask extends MatchingTask {

    private File deb;
    private File control;
    private File changes;
    private File keyring;
    private String key;
    private String passphrase;
    
    private Collection dataProducers = new ArrayList();
    
    
    public void setDestfile( File deb ) {
    	this.deb = deb;
    }
    
    public void setControl( File control ) {
    	this.control = control;
    }
    
    public void setChanges( File changes ) {
    	this.changes = changes;
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
    
    public void addData( Data data ) {
    	dataProducers.add(data);
    }
    
	public void execute() {
		
		if (control == null || !control.isDirectory()) {
			throw new BuildException("You need to point the 'control' attribute to the control directory.");
		}

		if (changes != null && changes.isDirectory()) {
			throw new BuildException("If you want the changes written out provide the file via 'changes' attribute.");			
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
				log(s);
			}			
		});
		
		try {
						
			final ChangesDescriptor changesDescriptor = processor.createDeb(controlFiles, data, new FileOutputStream(deb));

			log("Created " + deb);

			if (changes != null) {

				changesDescriptor.addFile(new FileInputStream(deb), deb.getName());
				
				processor.createChanges(changesDescriptor, (keyring!=null)?new FileInputStream(keyring):null, key, passphrase, new FileOutputStream(changes));

				log("Created changes file " + changes);
			}			
		} catch (Exception e) {
			log("Failed to create debian package " + e);
			e.printStackTrace();
		}
		
	}
}
