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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.vafer.jdeb.ar.ArArchive;
import org.vafer.jdeb.ar.FileArEntry;
import org.vafer.jdeb.ar.StaticArEntry;
import org.vafer.jdeb.changes.ChangeSet;
import org.vafer.jdeb.changes.ChangesProvider;
import org.vafer.jdeb.descriptors.ChangesDescriptor;
import org.vafer.jdeb.descriptors.InvalidDescriptorException;
import org.vafer.jdeb.descriptors.PackageDescriptor;
import org.vafer.jdeb.mapping.Mapper;
import org.vafer.jdeb.signing.SigningUtils;
import org.vafer.jdeb.utils.InformationOutputStream;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * The processor does the actual work of building the deb related files.
 * It is been used by the ant task and (later) the maven plugin.
 * 
 * @author tcurdt
 */
public class Processor {

	private final Console console;
	private final Mapper mapper;
	private final VariableResolver resolver;

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

	public Processor( final Console pConsole, final VariableResolver pResolver ) {
		this(pConsole, new Mapper() {
			public TarEntry map( final TarEntry pEntry ) {
				return pEntry;
			}

		}, pResolver);

	}

	public Processor( final Console pConsole, final Mapper pMapper, final VariableResolver pResolver ) {
		console = pConsole;
		mapper = pMapper;
		resolver = pResolver;
	}

	/**
	 * Create the debian archive with from the provided control files and data producers.
	 * 
	 * @param pControlFiles
	 * @param pData
	 * @param pOutput
	 * @return PackageDescriptor
	 * @throws PackagingException
	 */
	public PackageDescriptor createDeb( final File[] pControlFiles, final DataProducer[] pData, final File pOutput ) throws PackagingException, InvalidDescriptorException {

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

			if (!packageDescriptor.isValid()) {
				throw new InvalidDescriptorException(packageDescriptor);
			}

			final InformationOutputStream output = new InformationOutputStream(new FileOutputStream(pOutput), MessageDigest.getInstance("MD5"));

			final ArArchive ar = new ArArchive(output);
			ar.add(new StaticArEntry("debian-binary", 0, 0, 33188, "2.0\n"));
			ar.add(new FileArEntry(tempControl,"control.tar.gz", 0, 0, 33188));
			ar.add(new FileArEntry(tempData, "data.tar.gz", 0, 0, 33188));
			ar.close();

			// intermediate values
			packageDescriptor.set("MD5", output.getMd5());
			packageDescriptor.set("Size", "" + output.getSize());
			packageDescriptor.set("File", pOutput.getName());

			return packageDescriptor;

		} catch(InvalidDescriptorException e) {
			throw e;
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

	/**
	 * Create changes file based on the provided PackageDescriptor.
	 * If pRing, pKey and pPassphrase are provided the changes file will also be signed.
	 * It returns a ChangesDescriptor reflecting the changes  
	 * @param pPackageDescriptor
	 * @param pChangeSets
	 * @param pRing
	 * @param pKey
	 * @param pPassphrase
	 * @param pOutput
	 * @return ChangesDescriptor
	 * @throws IOException
	 */
	public ChangesDescriptor createChanges( final PackageDescriptor pPackageDescriptor, final ChangesProvider pChangesProvider, final InputStream pRing, final String pKey, final String pPassphrase, final OutputStream pOutput ) throws IOException, InvalidDescriptorException {

		final ChangeSet[] changeSets = pChangesProvider.getChangesSets();
		final ChangesDescriptor changesDescriptor = new ChangesDescriptor(pPackageDescriptor, changeSets);

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
		files.append(' ').append(changesDescriptor.get("File"));			
		changesDescriptor.set("Files", files.toString());

		if (!changesDescriptor.isValid()) {
			throw new InvalidDescriptorException(changesDescriptor);
		}
		
		final String changes = changesDescriptor.toString();
		//console.println(changes);

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
		} catch (Exception e) {
			e.printStackTrace();
		}

		pOutput.close();

		return changesDescriptor;
	}

	/**
	 * Build control archive of the deb
	 * @param pControlFiles
	 * @param pDataSize
	 * @param pChecksums
	 * @param pOutput
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 */
	private PackageDescriptor buildControl( final File[] pControlFiles, final BigInteger pDataSize, final StringBuffer pChecksums, final File pOutput ) throws FileNotFoundException, IOException, ParseException {

		PackageDescriptor packageDescriptor = null;

		final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(pOutput)));
		outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

		for (int i = 0; i < pControlFiles.length; i++) {
			final File file = pControlFiles[i];

			if (file.isDirectory()) {
				continue;
			}

			final TarEntry entry = new TarEntry(file);

			final String name = file.getName();

			entry.setName(name);

			if ("control".equals(name)) {
				packageDescriptor = new PackageDescriptor(new FileInputStream(file), resolver);

				if (packageDescriptor.get("Date") == null) {
					packageDescriptor.set("Date", new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date())); // Mon, 26 Mar 2007 11:44:04 +0200
				}

				if (packageDescriptor.get("Distribution") == null) {
					packageDescriptor.set("Distribution", "unknown");
				}

				if (packageDescriptor.get("Urgency") == null) {
					packageDescriptor.set("Urgency", "low");
				}

				if (packageDescriptor.get("Maintainer") == null) {
					
					final String debFullName = System.getenv("DEBFULLNAME");
					final String debEmail = System.getenv("DEBEMAIL");
					if (debFullName != null && debEmail != null) {
						packageDescriptor.set("Maintainer", debFullName + " <" + debEmail + ">");
					}
				}				
				
				continue;
			}			

			final InputStream inputStream = new FileInputStream(file);

			outputStream.putNextEntry(entry);

			Utils.copy(inputStream, outputStream);								

			outputStream.closeEntry();

			inputStream.close();

		}

		if (packageDescriptor == null) {
			throw new FileNotFoundException("No control file in " + Arrays.toString(pControlFiles));
		}

		packageDescriptor.set("Installed-Size", pDataSize.toString());

		addEntry("control", packageDescriptor.toString(), outputStream);

		addEntry("md5sums", pChecksums.toString(), outputStream);

		outputStream.close();

		return packageDescriptor;
	}

	/**
	 * Build the data archive of the deb from the provided DataProducers
	 * @param pData
	 * @param pOutput
	 * @param pChecksums
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
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
