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

public class DebAntTask extends MatchingTask {

    private File deb;
    private File control;
    private Collection dataProducers = new ArrayList();
    
    
    public void setDestfile( File deb ) {
    	this.deb = deb;
    }
    
    public void setControl( File control ) {
    	this.control = control;
    }
    
	
    public void addDataFiles( DataFiles data ) {
    	dataProducers.add(data);
    }
    
    public void addDataArchive( DataArchive data ) {
    	dataProducers.add(data);
    }
    
	public void execute() {
		
		if (control == null || !control.isDirectory()) {
			throw new BuildException("you need to point the 'control' attribute to the control directory");
		}
				
		if (dataProducers.size() == 0) {
			throw new BuildException("you need to provide at least one pointer to a tgz or directory with the data");
		}

		if (deb == null) {
			throw new BuildException("you need to point the 'destfile' attribute to where the deb is supposed to be created");
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
			processor.createDeb(controlFiles, data, new FileOutputStream(deb));
		} catch (Exception e) {
			log("failed to create debian package " + e);
			e.printStackTrace();
		}
		
		log("created " + deb);
	}
}
