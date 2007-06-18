package org.vafer.jdeb.ant;

import java.io.File;
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
	
    public void addDataFiles( DataFiles data ) {
    	dataProducers.add(data);
    }
    
    public void addDataArchive( DataArchive data ) {
    	dataProducers.add(data);
    }
    
	public void execute() {
		
		if (control == null || !control.isDirectory()) {
			throw new BuildException("You need to point the 'control' attribute to the control directory.");
		}

		if (changes != null && !changes.isFile()) {
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

			if (changes != null) {
				processor.createChanges(changesDescriptor, new FileOutputStream(changes));
			}			
		} catch (Exception e) {
			log("failed to create debian package " + e);
			e.printStackTrace();
		}
		
		log("created " + deb);
	}
}
