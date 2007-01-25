package org.vafer.jdeb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    private File data;
    private int strip = 0;
    private String prefix = "";
    
    public void setDestfile(File deb) {
    	this.deb = deb;
    }
    
    public void setControl(File control) {
    	this.control = control;
    }
    
    public void setData(File data) {
    	this.data = data;
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
	
	public void execute() {
		
		if (control == null || !control.isDirectory()) {
			throw new BuildException("you need to point the 'control' attribute to the control directory");
		}
				
		if (data == null) {
			throw new BuildException("you need to point the 'data' attribute either to the tgz or the directory with the data");
		}

		if (deb == null) {
			throw new BuildException("you need to point the 'destfile' attribute to where the deb is supposed to be created");
		}
		

		File tempData = null;
		File tempControl = null;
		
		try {
			tempData = File.createTempFile("deb", "data");
			tempControl = File.createTempFile("deb", "control");
			
			Map digests = buildData(data, tempData);
			buildControl(control, digests, tempControl);
						
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
	
	private Map buildData( final File src, final File dst ) throws Exception {
		final Map map = new HashMap();
		
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
								
				log("name:" + entry.getName() +
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
				
				map.put(entry.getName(), toHex(digest.digest()));
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

						outputStream.putNextEntry(entry);
			
						digest.reset();

						copy(inputStream, new DigestOutputStream(outputStream, digest));
										
						log("name:" + entry.getName() +
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
						
						map.put(entry.getName(), toHex(digest.digest()));

						inputStream.close();

					} catch (Exception e) {
						e.printStackTrace();
					}
				}				
			});
						
			outputStream.close();
			
		}
		
		return map;

	}
	
	
	private void buildControl( final File src, final Map digests, final File dst ) throws Exception {
		final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(dst)));
		outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

		iterate(src, new FileVisitor() {
			public void visit( File file ) {
				try {
					TarEntry entry = new TarEntry(file);
					
					entry.setName(file.getName());
					
					InputStream inputStream = new FileInputStream(file);

					outputStream.putNextEntry(entry);
		
					copy(inputStream, outputStream);
									
					log("name:" + entry.getName() +
							" size:" + entry.getSize() +
							" mode:" + entry.getMode() +
							" linkname:" + entry.getLinkName() +
							" username:" + entry.getUserName() +
							" userid:" + entry.getUserId() +
							" groupname:" + entry.getGroupName() +
							" groupid:" + entry.getGroupId() +
							" modtime:" + entry.getModTime()
					);
					
					outputStream.closeEntry();
					
					inputStream.close();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}				
		});

		// FIXME: no real point for a map ...just creat the stringbuffer while we create the data.tgz
		StringBuffer sb = new StringBuffer();
		for (Iterator it = digests.entrySet().iterator(); it.hasNext();) {
			final Map.Entry entry = (Map.Entry) it.next();
			final String path = (String) entry.getKey();
			final String md5 = (String) entry.getValue();
			sb.append(md5).append(" ").append(path).append('\n');
		}
		
		byte[] data = sb.toString().getBytes("UTF-8");
		
		TarEntry entry = new TarEntry("md5sums");
		entry.setSize(data.length);

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
