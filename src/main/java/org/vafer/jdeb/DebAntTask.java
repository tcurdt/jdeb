package org.vafer.jdeb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;
import org.vafer.jdeb.ar.ArArchive;
import org.vafer.jdeb.ar.FileArEntry;
import org.vafer.jdeb.ar.StaticArEntry;

public class DebAntTask extends Task {

    private File deb;
    private File control;

    private Collection dataCollection = new ArrayList();
    
    
    public void setDestfile(File deb) {
    	this.deb = deb;
    }
    
    public void setControl(File control) {
    	this.control = control;
    }
    
	
    public static class Data {
    	
    	private String prefix = "";
    	private int strip = 0;
    	private File data;
    	
    	public void setStrip(int strip) {
    		this.strip = strip;
    	}
    	
    	public void setPrefix(String prefix) {
    		if (!prefix.endsWith("/")) {
        		this.prefix = prefix + "/";
        		return;
    		}
    		
    		this.prefix = prefix;
    	}
    	
    	public void setSrc(File data) {
    		this.data = data;
    	}
    	
    	public File getFile() {
    		return data;
    	}
    	
    	public int getStrip() {
    		return strip;
    	}
    	
    	public String getPrefix() {
    		return prefix;
    	}
    	
    	public String toString() {
    		return data.toString();
    	}
    }
    
    
    public void addData(Data data) {
    	dataCollection.add(data);
    }
    
    
	public void execute() {
		
		if (control == null || !control.isDirectory()) {
			throw new BuildException("you need to point the 'control' attribute to the control directory");
		}
				
		if (dataCollection.size() == 0) {
			throw new BuildException("you need to provide at least one pointer to a tgz or directory with the data");
		}

		if (deb == null) {
			throw new BuildException("you need to point the 'destfile' attribute to where the deb is supposed to be created");
		}

		File tempData = null;
		File tempControl = null;
		
		try {
			tempData = File.createTempFile("deb", "data");
			tempControl = File.createTempFile("deb", "control");
			
			tempData.deleteOnExit();
			tempControl.deleteOnExit();

			
			final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(tempData)));
			outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);
			
			final StringBuffer md5sum = new StringBuffer();


			for (Iterator it = dataCollection.iterator(); it.hasNext();) {
				final Data data = (Data) it.next();

				log("*** adding data from " + data);
				
				buildData(data, outputStream, md5sum);					
			}
			
			outputStream.close();
			
			buildControl(control, md5sum.toString(), tempControl);
						
			ArArchive ar = new ArArchive(new FileOutputStream(deb));
			ar.add(new StaticArEntry("debian-binary",  0, 0, 33188, "2.0\n"));
			ar.add(new FileArEntry(tempControl, "control.tar.gz", 0, 0, 33188));
			ar.add(new FileArEntry(tempData, "data.tar.gz", 0, 0, 33188));
			ar.close();
			
		} catch(Exception e) {
			
			e.printStackTrace();
			
			throw new BuildException("could not create deb package", e);
		}
		
		log("created " + deb);
	}
	
	private static interface FileVisitor {
		void visit( File file );
	}
	
	
	private void iterate( File dir, FileVisitor visitor) {

		// FIXME: make configurable
		if (".svn".equals(dir.getName())) {				
			return;
		}		
		
	    visitor.visit(dir);

		if (dir.isDirectory()) {
			
			File[] childs = dir.listFiles();
			for (int i = 0; i < childs.length; i++) {
				iterate(childs[i], visitor);
			}
		}
	}
	
	private String stripPath( final int p, final String s ) {
		
		if (p<=0) {
			return s;
		}
		
		int x = 0;
		for (int i=0 ; i<p; i++) {
			x = s.indexOf('/', x);
			if (x < 0) {
				return s;
			}
		}
		
		return s.substring(x+1);
	}
	
	private String stripLeadingSlash( String s ) {
		if (s == null) {
			return s;
		}
		if (s.length() == 0) {
			return s;
		}
		if (s.charAt(0) == '/') {
			return s.substring(1);
		}
		return s;
	}
	
	private void buildData( final Data srcData, final TarOutputStream outputStream, final StringBuffer md5sum ) throws Exception {
		final File src = srcData.getFile();
		
		if (!src.exists()) {
			return;
		}
		
		// FIXME: merge both cases via visitor
		if (src.isFile()) {
			final TarInputStream inputStream = new TarInputStream(new GZIPInputStream(new FileInputStream(src)));
			
			final MessageDigest digest = MessageDigest.getInstance("MD5");
	
			while(true) {
				final TarEntry entry = inputStream.getNextEntry();
				
				if (entry == null) {
					break;
				}

				entry.setName(stripLeadingSlash(srcData.getPrefix() + stripPath(srcData.getStrip(), entry.getName())));

				outputStream.putNextEntry(entry);
					
				if (entry.isDirectory()) {
					//copy(inputStream, outputStream);
					outputStream.closeEntry();
				} else {
				
					digest.reset();
	
					copy(inputStream, new DigestOutputStream(outputStream, digest));

					String md5 = toHex(digest.digest());
					
					log("adding data file name:" + entry.getName() +
							" size:" + entry.getSize() +
							" mode:" + entry.getMode() +
							" linkname:" + entry.getLinkName() +
							" username:" + entry.getUserName() +
							" userid:" + entry.getUserId() +
							" groupname:" + entry.getGroupName() +
							" groupid:" + entry.getGroupId() +
							" modtime:" + entry.getModTime() +
							" md5: " + md5 
					);
					
					outputStream.closeEntry();
					
					md5sum.append(md5).append(" ").append(entry.getName()).append('\n');
				}
			}
			
			inputStream.close();
		} else {

			final MessageDigest digest = MessageDigest.getInstance("MD5");
	
			iterate(src, new FileVisitor() {
				public void visit( File file ) {
					try {
						TarEntry entry = new TarEntry(file);
						
						String localName = file.getAbsolutePath().substring(src.getAbsolutePath().length());
						
						if ("".equals(localName)) {
						    return;
						}
						
						entry.setName(stripLeadingSlash(srcData.getPrefix() + stripPath(srcData.getStrip(), localName.substring(1))));
						
					    outputStream.putNextEntry(entry);

					    if (file.isDirectory()) {
						    outputStream.closeEntry();
						    return;
						}
						
						InputStream inputStream = new FileInputStream(file);
						
						digest.reset();

						copy(inputStream, new DigestOutputStream(outputStream, digest));
						
						String md5 = toHex(digest.digest());
						
						outputStream.closeEntry();

						log("adding data file name:" + entry.getName() +
								" size:" + entry.getSize() +
								" mode:" + entry.getMode() +
								" linkname:" + entry.getLinkName() +
								" username:" + entry.getUserName() +
								" userid:" + entry.getUserId() +
								" groupname:" + entry.getGroupName() +
								" groupid:" + entry.getGroupId() +
								" modtime:" + entry.getModTime() +
								" md5: " + md5
						);
						
						md5sum.append(md5).append(" ").append(entry.getName()).append('\n');

						inputStream.close();

					} catch (Exception e) {
						e.printStackTrace();
					}
				}				
			});						
		}		
	}
	
	
	private void buildControl( final File src, final String digests, final File dst ) throws Exception {
		final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(dst)));
		outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

		iterate(src, new FileVisitor() {
			public void visit( File file ) {
			    
			    if (file.isDirectory()) {
			        return;
			    }
			    
				try {
					TarEntry entry = new TarEntry(file);
					
					entry.setName(file.getName());
					
					InputStream inputStream = new FileInputStream(file);

					log("adding control file " + entry.getName());

					outputStream.putNextEntry(entry);
		
					copy(inputStream, outputStream);								
					
					outputStream.closeEntry();
					
					inputStream.close();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}				
		});

		byte[] data = digests.getBytes("UTF-8");
		
		TarEntry entry = new TarEntry("md5sums");
		entry.setSize(data.length);

		log("adding control file " + entry.getName());

		outputStream.putNextEntry(entry);
		outputStream.write(data);
		outputStream.closeEntry();
		
		outputStream.close();
		
	}
	
    private static String toHex(byte[] b) {
    	final StringBuffer sb = new StringBuffer();

    	for (int i = 0; i < b.length; ++i) {
    		sb.append(Integer.toHexString((b[i]>>4) & 0x0f));
    		sb.append(Integer.toHexString(b[i] & 0x0f));
    	}

    	return sb.toString();
    }
    
    private static int copy(InputStream input, OutputStream output) throws IOException {
    	byte[] buffer = new byte[2048];
    	int count = 0;
    	int n = 0;
    	while (-1 != (n = input.read(buffer))) {
    		output.write(buffer, 0, n);
    		count += n;
    	}
    	return count;
    }
}
