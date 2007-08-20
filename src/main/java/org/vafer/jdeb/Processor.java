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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.vafer.jdeb.ar.ArArchive;
import org.vafer.jdeb.ar.FileArEntry;
import org.vafer.jdeb.ar.StaticArEntry;
import org.vafer.jdeb.changes.ChangeSet;
import org.vafer.jdeb.descriptors.ChangesDescriptor;
import org.vafer.jdeb.descriptors.PackageDescriptor;
import org.vafer.jdeb.mapping.Mapper;
import org.vafer.jdeb.signing.SigningUtils;
import org.vafer.jdeb.utils.InformationOutputStream;
import org.vafer.jdeb.utils.Utils;

public class Processor {

	private final Console console;
	private final Mapper mapper;

	private static final class Total {
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

	public Processor( final Console pConsole ) {
		this(pConsole, new Mapper() {
			public TarEntry map( final TarEntry pEntry ) {
				return pEntry;
			}

		});

	}

	public Processor( final Console pConsole, final Mapper pMapper ) {
		console = pConsole;
		mapper = pMapper;
	}

	public PackageDescriptor createDeb( final File[] pControlFiles, final DataProducer[] pData, final OutputStream pOutput ) throws PackagingException {

		File tempData = null;
		File tempControl = null;

		try {
			tempData = File.createTempFile("deb", "data");			
			tempControl = File.createTempFile("deb", "control");			

			console.println("Building data");
			final StringBuffer md5s = new StringBuffer();
			final BigInteger size = buildData(pData, tempData, md5s);

			console.println("Building control");
			final PackageDescriptor packageDescriptor = buildControl(pControlFiles, size, md5s, tempControl);

			final InformationOutputStream output = new InformationOutputStream(pOutput, MessageDigest.getInstance("MD5"));

			final ArArchive ar = new ArArchive(output);
			ar.add(new StaticArEntry("debian-binary", 0, 0, 33188, "2.0\n"));
			ar.add(new FileArEntry(tempControl,"control.tar.gz", 0, 0, 33188));
			ar.add(new FileArEntry(tempData, "data.tar.gz", 0, 0, 33188));
			ar.close();

			packageDescriptor.set("Date", new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date())); // Mon, 26 Mar 2007 11:44:04 +0200

			if (packageDescriptor.get("Distribution") == null) {
				packageDescriptor.set("Distribution", "unknown");
			}

			if (packageDescriptor.get("Urgency") == null) {
				packageDescriptor.set("Urgency", "low");
			}

			// intermediate values
			packageDescriptor.set("MD5", output.getMd5());
			packageDescriptor.set("Size", "" + output.getSize());

			return packageDescriptor;

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

	public ChangesDescriptor createChanges( final PackageDescriptor pPackageDescriptor, final ChangeSet[] pChangeSets, final InputStream pRing, final String pKey, final String pPassphrase, final OutputStream pOutput ) throws IOException {

		final ChangesDescriptor changesDescriptor = new ChangesDescriptor(pPackageDescriptor, pChangeSets);

		changesDescriptor.set("Format", "1.7");

		if (changesDescriptor.get("Binary") == null) {
			changesDescriptor.set("Binary", changesDescriptor.get("Package"));
		}

		if (changesDescriptor.get("Source") == null) {
			changesDescriptor.set("Source", changesDescriptor.get("Package"));
		}

		if (changesDescriptor.get("Description") == null) {
			changesDescriptor.set("Description", "update to " + changesDescriptor.get("Version"));
		}

		final StringBuffer files = new StringBuffer("\n");
		files.append(' ').append(changesDescriptor.get("MD5"));
		files.append(' ').append(changesDescriptor.get("Size"));
		files.append(' ').append(changesDescriptor.get("Section"));
		files.append(' ').append(changesDescriptor.get("Priority"));
		files.append(' ').append(changesDescriptor.get("Package")).append('_').append(changesDescriptor.get("Version")).append('_').append(changesDescriptor.get("Architecture")).append(".deb");			
		changesDescriptor.set("Files", files.toString());

		if (!changesDescriptor.isValid()) {
			console.println("Could not write changes descriptor. It's invalid!");
			return changesDescriptor;
		}
		
		final String changes = changesDescriptor.toString();

		console.println(changes);

		final byte[] changesBytes = changes.getBytes("UTF-8");

		if (pRing == null || pKey == null || pPassphrase == null) {			
			pOutput.write(changesBytes);
			pOutput.close();			
			return changesDescriptor;
		}

		console.println("Signing changes with key " + pKey);

		final InputStream input = new ByteArrayInputStream(changesBytes);

		try {
			SigningUtils.clearSign(input, pRing, pKey, pPassphrase, pOutput);		
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		} catch (PGPException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}

		pOutput.close();

		return changesDescriptor;
	}

	private PackageDescriptor buildControl( final File[] pControlFiles, final BigInteger pDataSize, final StringBuffer pChecksums, final File pOutput ) throws FileNotFoundException, IOException, ParseException {

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

		packageDescriptor.set("Installed-Size", pDataSize.toString());

		addEntry("control", packageDescriptor.toString(), outputStream);

		addEntry("md5sums", pChecksums.toString(), outputStream);

		outputStream.close();

		return packageDescriptor;
	}

	private BigInteger buildData( final DataProducer[] pData, final File pOutput, final StringBuffer pChecksums ) throws NoSuchAlgorithmException, IOException {

		final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(pOutput)));
		outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

		final MessageDigest digest = MessageDigest.getInstance("MD5");

		final Total dataSize = new Total();

		final DataConsumer receiver = new DataConsumer() {
			public void onEachDir( String dirname, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {

				if (!dirname.endsWith("/")) {
					dirname = dirname + "/";
				}

				if (!dirname.startsWith("/")) {
					dirname = "/" + dirname;
				}

				TarEntry entry = new TarEntry(dirname);

				// FIXME: link is in the constructor
				entry.setUserName(user);
				entry.setUserId(uid);
				entry.setUserName(group);
				entry.setGroupId(gid);
				entry.setMode(mode);
				entry.setSize(0);

				entry = mapper.map(entry);

				outputStream.putNextEntry(entry);

				console.println("dir: " + dirname);

				outputStream.closeEntry();
			}

			public void onEachFile( InputStream inputStream, String filename, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {

				if (!filename.startsWith("/")) {
					filename = "/" + filename;
				}

				TarEntry entry = new TarEntry(filename);

				// FIXME: link is in the constructor
				entry.setUserName(user);
				entry.setUserId(uid);
				entry.setUserName(group);
				entry.setGroupId(gid);
				entry.setMode(mode);
				entry.setSize(size);

				entry = mapper.map(entry);

				outputStream.putNextEntry(entry);

				dataSize.add(size);

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

				pChecksums.append(md5).append(" ").append(entry.getName()).append('\n');

			}					
		};

		for (int i = 0; i < pData.length; i++) {
			final DataProducer data = pData[i];
			data.produce(receiver);
		}

		outputStream.close();

		console.println("Total size: " + dataSize);

		return dataSize.count;
	}

	private static void addEntry( final String pName, final String pContent, final TarOutputStream pOutput ) throws IOException {
		final byte[] data = pContent.getBytes("UTF-8");

		final TarEntry entry = new TarEntry(pName);
		entry.setSize(data.length);

		pOutput.putNextEntry(entry);
		pOutput.write(data);
		pOutput.closeEntry();		
	}


}
