package org.vafer.jdeb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class ChecksumAntTask extends Task {
	
	private File root;
	private File report;
	
	public void setDestfile(File report) {
		this.report = report;
	}
	
	public void setBasedir(File root) {
		this.root = root;
	}
	
	
	public void execute() {

		if (root == null || !root.isDirectory()) {
			throw new BuildException("you need to point the 'basedir' attribute to the root of your data directory");
		}
		
		if (report == null) {
			throw new BuildException("you need to point the 'destfile' attribute to where you want the md5 report to be saved");
		}

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(report);
			
			checksum(root, writer, root.getAbsolutePath().length() + 1);
			
		} catch (FileNotFoundException e) {
			throw new BuildException(e);
		} finally {
			if (writer != null) {
				writer.close();		
			}
		}
	}
	
	private byte[] getMessageDigest( File file, String impl ) throws Exception {
	    MessageDigest md = MessageDigest.getInstance(impl);
	    byte[] buffer = new byte[8192];
	    InputStream in = new FileInputStream(file);
	    for(int i = 0; (i = in.read(buffer)) > -1;) {
	      md.update(buffer, 0, i);
	    }
	    return md.digest();
	  }
	 
	private String md5( File file ) throws Exception {
	    byte[] digest = getMessageDigest(file, "MD5");
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < digest.length; i++) {
			sb.append(Integer.toHexString( digest[i] & 0xFF));
		}
		return sb.toString();
	}
	
	
	private void checksum(File dir, PrintWriter writer, int strip) {
		if (dir.isFile()) {
			
			try {
				writer.print(md5(dir));
				writer.print(' ');
				writer.print(dir.getAbsolutePath().substring(strip));
				writer.print('\n');
			} catch (Exception e) {
				log("Could not generate checksum for " + dir + " " + e);
			}
			
			return;
		}
		
		File[] childs = dir.listFiles();
		for (int i = 0; i < childs.length; i++) {
			checksum(childs[i], writer, strip);
		}		
	}
}
