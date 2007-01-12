package org.vafer.jdeb;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.vafer.jdeb.ar.ArArchive;
import org.vafer.jdeb.ar.ArEntry;
import org.vafer.jdeb.ar.StaticArEntry;

public class DebAntTask extends Task {

    private File deb;
    private File control;
    private File data;
    
    public void setDestfile(File deb) {
    	this.deb = deb;
    }
    
    public void setControl(File control) {
    	this.control = control;
    }
    
    public void setData(File data) {
    	this.data = data;
    }
    
	
	public void execute() {
		
		if (control == null) {
			throw new BuildException("you need to point the 'control' attribute to the control tgz");
		}

		if (data == null) {
			throw new BuildException("you need to point the 'data' attribute to the data tgz");
		}

		if (deb == null) {
			throw new BuildException("you need to point the 'destfile' attribute to where the deb is supposed to be created");
		}
		
		if (!data.isFile() || !control.isFile()) {
			throw new BuildException("both 'data' and 'control' need to point to existing tgz archives");
		}
		
		log("Building " + deb + " for project " + getProject().getName());
		
		try {
			ArArchive ar = new ArArchive(new FileOutputStream(deb));
			ar.add(new StaticArEntry("debian-binary",  0, 0, 33188, "2.0\n"));
			ar.add(new ArEntry(control, 0, 0, 33188));
			ar.add(new ArEntry(data,    0, 0, 33188));
			ar.close();
		} catch(Exception e) {
			throw new BuildException("could not create deb package", e);
		}
	}
}
