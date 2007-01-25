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
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;
import org.vafer.jdeb.ar.ArArchive;
import org.vafer.jdeb.ar.FileArEntry;
import org.vafer.jdeb.ar.StaticArEntry;

public class DebAntTask extends Task {

    private File deb;
    private File control;
    private int strip = 0;
    private String prefix = "";
    private Collection dataFiles = new ArrayList();
    
    
    public void setDestfile(File deb) {
    	this.deb = deb;
    }
    
    public void setControl(File control) {
    	this.control = control;
    }
    
    public void setData(File data) {
    	dataFiles.add(new FileResource(data));
    }

    public void setStrip(int strip) {
    	this.strip = strip;
    }
    
    public void setPrefix(String prefix) {
    	if (prefix.endsWith("/")) {
        	this.prefix = prefix;
        	return;
    	}
    	this.prefix = prefix + '/';
    }
	
    public void add(ResourceCollection res) {
    	dataFiles.add(res);
    }

    
	public void execute() {
		
		if (control == null || !control.isDirectory()) {
			throw new BuildException("you need to point the 'control' attribute to the control directory");
		}
				
		if (dataFiles.size() == 0) {
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
			
			final StringBuffer md5sum = new StringBuffer();
			for (Iterator it = dataFiles.iterator(); it.hasNext();) {
				final ResourceCollection rc = (ResourceCollection) it.next();
				
				for (Iterator rit = rc.iterator(); rit.hasNext();) {
					final Resource resource = (Resource) rit.next();
					
					if (!(resource instanceof FileResource)) {
						throw new BuildException("only file resources are supported " + resource);
					}
					
					final File data = ((FileResource)resource).getFile();

					buildData(data, tempData, md5sum);					
				}
				
			}
			buildControl(control, md5sum.toString(), tempControl);
						
			ArArchive ar = new ArArchive(new FileOutputStream(deb));
			ar.add(new StaticArEntry("debian-binary",  0, 0, 33188, "2.0\n"));
			ar.add(new FileArEntry(tempControl, "control.tar.gz", 0, 0, 33188));
			ar.add(new FileArEntry(tempData, "data.tar.gz", 0, 0, 33188));
			ar.close();
			
		} catch(Exception e) {
			
			if (tempData != null) {
				tempData.delete();
			}

			if (tempControl != null) {
				tempControl.delete();
			}
			
			e.printStackTrace();
			
			throw new BuildException("could not create deb package", e);
		}
	}
	
	private static interface FileVisitor {
		void visit( File file );
	}
	
	
	private void iterate( File dir, FileVisitor visitor) {
		if (dir.isDirectory()) {
			File[] childs = dir.listFiles();
			for (int i = 0; i < childs.length; i++) {
				iterate(childs[i], visitor);
			}
			return;
		}
		
		visitor.visit(dir);
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
	
	private void buildData( final File src, final File dst, final StringBuffer md5sum ) throws Exception {
		// FIXME: merge both cases via visitor
		if (src.isFile()) {
			final TarInputStream inputStream = new TarInputStream(new GZIPInputStream(new FileInputStream(src)));
			final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(dst)));
			outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);
			
			final MessageDigest digest = MessageDigest.getInstance("MD5");
	
			while(true) {
				final TarEntry entry = inputStream.getNextEntry();
				
				if (entry == null) {
					break;
				}

				entry.setName(prefix + stripPath(strip, entry.getName()));
				
				outputStream.putNextEntry(entry);
	
				digest.reset();

				copy(inputStream, new DigestOutputStream(outputStream, digest));
								
				log("adding data file name:" + entry.getName() +
						" size:" + entry.getSize() +
						" mode:" + entry.getMode() +
						" linkname:" + entry.getLinkName() +
						" username:" + entry.getUserName() +
						" userid:" + entry.getUserId() +
						" groupname:" + entry.getGroupName() +
						" groupid:" + entry.getGroupId() +
						" modtime:" + entry.getModTime() +
						" md5: " + toHex(digest.digest())
				);
				
				outputStream.closeEntry();
				
				md5sum.append(entry.getName()).append(" ").append(toHex(digest.digest())).append('\n');
			}
			
			inputStream.close();
			outputStream.close();
		} else {
			final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(dst)));
			outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

			final MessageDigest digest = MessageDigest.getInstance("MD5");
	
			iterate(src, new FileVisitor() {
				public void visit( File file ) {
					try {
						TarEntry entry = new TarEntry(file);
						
						entry.setName(prefix + stripPath(strip, file.getAbsolutePath().substring(src.getAbsolutePath().length())));
						
						InputStream inputStream = new FileInputStream(file);
						
						log("adding data file name:" + entry.getName() +
								" size:" + entry.getSize() +
								" mode:" + entry.getMode() +
								" linkname:" + entry.getLinkName() +
								" username:" + entry.getUserName() +
								" userid:" + entry.getUserId() +
								" groupname:" + entry.getGroupName() +
								" groupid:" + entry.getGroupId() +
								" modtime:" + entry.getModTime() +
								" md5: " + toHex(digest.digest())
						);

						outputStream.putNextEntry(entry);
			
						digest.reset();

						copy(inputStream, new DigestOutputStream(outputStream, digest));
																
						outputStream.closeEntry();
						
						md5sum.append(entry.getName()).append(" ").append(toHex(digest.digest())).append('\n');

						inputStream.close();

					} catch (Exception e) {
						e.printStackTrace();
					}
				}				
			});
						
			outputStream.close();
			
		}		
	}
	
	
	private void buildControl( final File src, final String digests, final File dst ) throws Exception {
		final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(dst)));
		outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

		iterate(src, new FileVisitor() {
			public void visit( File file ) {
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
