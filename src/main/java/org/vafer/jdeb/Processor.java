package org.vafer.jdeb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.vafer.jdeb.ar.ArArchive;
import org.vafer.jdeb.ar.FileArEntry;
import org.vafer.jdeb.ar.StaticArEntry;

public class Processor {

	private final Console console;
	
	public Processor( Console pConsole ) {
		console = pConsole;
	}
	
	public void createDeb( File[] pControlFiles, DataProducer[] pData, OutputStream pDebOuput ) throws PackagingException {

		File tempData = null;
		File tempControl = null;
		
		try {
			tempData = File.createTempFile("deb", "data");			
			tempControl = File.createTempFile("deb", "control");			
			
			console.println("building data");
			final StringBuffer md5s = buildData(pData, tempData);
			
			console.println("building control");
			buildControl(pControlFiles, md5s, tempControl);
						
			final ArArchive ar = new ArArchive(pDebOuput);
			ar.add(new StaticArEntry("debian-binary", 0, 0, 33188, "2.0\n"));
			ar.add(new FileArEntry(tempControl,"control.tar.gz", 0, 0, 33188));
			ar.add(new FileArEntry(tempData, "data.tar.gz", 0, 0, 33188));
			ar.close();
			
		} catch(Exception e) {
			throw new PackagingException("could not create deb package", e);
		} finally {
			if (tempData != null) {
				tempData.delete();
			}
			if (tempControl != null) {
				tempControl.delete();
			}
		}
	}

	private void buildControl( final File[] pControlFiles, final StringBuffer md5s, final File pOutput ) throws FileNotFoundException, IOException {
		final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(pOutput)));
		outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

		for (int i = 0; i < pControlFiles.length; i++) {
			final File file = pControlFiles[i];

			if (file.isDirectory()) {
		        return;
		    }

			final TarEntry entry = new TarEntry(file);
			
			entry.setName(file.getName());
			
			final InputStream inputStream = new FileInputStream(file);

			outputStream.putNextEntry(entry);

			Utils.copy(inputStream, outputStream);								
			
			outputStream.closeEntry();
			
			inputStream.close();
			
		}

		final byte[] data = md5s.toString().getBytes("UTF-8");
		
		final TarEntry entry = new TarEntry("md5sums");
		entry.setSize(data.length);

		outputStream.putNextEntry(entry);
		outputStream.write(data);
		outputStream.closeEntry();
		
		outputStream.close();
		
	}
	
	private StringBuffer buildData( final DataProducer[] pData, final File pOutput ) throws NoSuchAlgorithmException, IOException {
		final StringBuffer md5s = new StringBuffer();
		
		final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(pOutput)));
		outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

		final MessageDigest digest = MessageDigest.getInstance("MD5");

		final DataConsumer receiver = new DataConsumer() {
			public void onEachFile( InputStream inputStream, String filename, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {

				final TarEntry entry = new TarEntry(filename);
				
				// link?
				entry.setUserName(user);
				entry.setUserId(uid);
				entry.setUserName(group);
				entry.setUserId(gid);
				entry.setMode(mode);
				entry.setSize(inputStream == null?0:size);

				outputStream.putNextEntry(entry);
				
			    if (inputStream == null) {
					console.println("dir: " + filename);

				    outputStream.closeEntry();
				    return;
				}
			    
			    digest.reset();
			    
				Utils.copy(inputStream, new DigestOutputStream(outputStream, digest));
				
				final String md5 = Utils.toHex(digest.digest());
				
				outputStream.closeEntry();

				console.println(
						"file:" + entry.getName() +
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
				
				md5s.append(md5).append(" ").append(entry.getName()).append('\n');

			}					
		};

		for (int i = 0; i < pData.length; i++) {
			final DataProducer data = pData[i];
			data.produce(receiver);
		}

		outputStream.close();

		return md5s;
	}
	


}
