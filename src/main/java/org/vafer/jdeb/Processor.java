package org.vafer.jdeb;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.text.ParseException;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.vafer.jdeb.ar.ArArchive;
import org.vafer.jdeb.ar.FileArEntry;
import org.vafer.jdeb.ar.StaticArEntry;
import org.vafer.jdeb.descriptors.ChangesDescriptor;
import org.vafer.jdeb.descriptors.PackageDescriptor;
import org.vafer.jdeb.signing.SigningUtils;

public class Processor {

	private final Console console;
		
	public Processor( Console pConsole ) {
		console = pConsole;
	}
	
	public ChangesDescriptor createDeb( File[] pControlFiles, DataProducer[] pData, OutputStream pDebOuput ) throws PackagingException {
		
		File tempData = null;
		File tempControl = null;
		
		try {
			tempData = File.createTempFile("deb", "data");			
			tempControl = File.createTempFile("deb", "control");			
			
			console.println("Building data");
			final StringBuffer md5s = buildData(pData, tempData);
			
			console.println("Building control");
			final PackageDescriptor packageDescriptor = buildControl(pControlFiles, md5s, tempControl);
						
			final ArArchive ar = new ArArchive(pDebOuput);
			ar.add(new StaticArEntry("debian-binary", 0, 0, 33188, "2.0\n"));
			ar.add(new FileArEntry(tempControl,"control.tar.gz", 0, 0, 33188));
			ar.add(new FileArEntry(tempData, "data.tar.gz", 0, 0, 33188));
			ar.close();
			
			final ChangesDescriptor changesDescriptor = new ChangesDescriptor(packageDescriptor); 
			
			// add deb to changes descriptor
			
			return changesDescriptor;
			
		} catch(Exception e) {
			throw new PackagingException("Could not create deb package", e);
		} finally {
			if (tempData != null) {
				tempData.delete();
			}
			if (tempControl != null) {
				tempControl.delete();
			}
		}
	}

	public void createChanges( ChangesDescriptor changesDescriptor, OutputStream output ) throws IOException {
		createChanges(changesDescriptor, null, null, null, output);
	}

	public void createChanges( ChangesDescriptor changesDescriptor, InputStream ring, String key, String passphrase, OutputStream output ) throws IOException {
		
		final String changes = changesDescriptor.toString();

		console.println(changes);

		final byte[] changesBytes = changes.getBytes("UTF-8");
		
		if (ring == null || key == null || passphrase == null) {			
			output.write(changesBytes);
			output.close();			
			return;
		}
		
		console.println("Signing changes with key " + key);
		
		final InputStream input = new ByteArrayInputStream(changesBytes);
		
		try {
			SigningUtils.clearSign(input, ring, key, passphrase, output);		
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		} catch (PGPException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
		
		output.close();
	}
	
	private PackageDescriptor buildControl( final File[] pControlFiles, final StringBuffer md5s, final File pOutput ) throws FileNotFoundException, IOException, ParseException {
		
		PackageDescriptor packageDescriptor = null;
		
		final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(pOutput)));
		outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

		for (int i = 0; i < pControlFiles.length; i++) {
			final File file = pControlFiles[i];

			if (file.isDirectory()) {
		        break;
		    }

			final TarEntry entry = new TarEntry(file);
			
			final String name = file.getName();
			
			entry.setName(name);
			
			if ("control".equals(name)) {
				packageDescriptor = new PackageDescriptor(new FileInputStream(file));
				continue;
			}			
			
			final InputStream inputStream = new FileInputStream(file);

			outputStream.putNextEntry(entry);

			Utils.copy(inputStream, outputStream);								
			
			outputStream.closeEntry();
			
			inputStream.close();
			
		}

		if (packageDescriptor == null) {
			packageDescriptor = new PackageDescriptor();
		}
		
		addEntry("control", packageDescriptor.toString(), outputStream);
		
		addEntry("md5sums", md5s.toString(), outputStream);
		
		outputStream.close();
		
		return packageDescriptor;
	}

	private void addEntry( String name, String content, TarOutputStream outputStream ) throws IOException {
		final byte[] data = content.getBytes("UTF-8");
		
		final TarEntry entry = new TarEntry(name);
		entry.setSize(data.length);

		outputStream.putNextEntry(entry);
		outputStream.write(data);
		outputStream.closeEntry();		
	}

	
	private static class Total {
		private BigInteger count = BigInteger.valueOf(0);

		public void add(long size) {
			count = count.add(BigInteger.valueOf(size));
		}
		
		public String toString() {
			return "" + count;
		}
		
		public BigInteger toBigInteger() {
			return count;
		}
	}
	
	private StringBuffer buildData( final DataProducer[] pData, final File pOutput ) throws NoSuchAlgorithmException, IOException {
		final StringBuffer md5s = new StringBuffer();
		
		final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(pOutput)));
		outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

		final MessageDigest digest = MessageDigest.getInstance("MD5");

		final Total total = new Total();
		
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
			    
			    total.add(size);
			    
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

		console.println("Total size: " + total);
		
		return md5s;
	}
	


}
