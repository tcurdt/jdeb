/*
 * Copyright 2014 The jdeb developers.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.vafer.jdeb.mapping.PermMapper;
import org.vafer.jdeb.synology.BinaryPackageInfoFile;

/**
 * A generic class for creating Synology archives.
 *
 * @author Torsten Curdt
 * @author Bryan Sant
 */
public class SpkMaker extends PackageMaker {

    /** The Synology package produced */
    private File spk;

    /** The info file */
    private File info;

    /** The directory containing the script files to build the package */
    private File scripts;

    /** The name of the package. Default value if not specified in the info file */
    private String packageName;

    /** The description of the package. Default value if not specified in the control file */
    private String description;


    public SpkMaker(Console console, Collection<DataProducer> dataProducers) {
    	super(console, dataProducers);
    }

    public void setSpk(File spk) {
        this.spk = spk;
    }

    public void setInfo(File info) {
        this.info = info;
    }

    public void setScripts(File scripts) {
        this.scripts = scripts;
    }

    public void setPackage(String packageName) {
        this.packageName = packageName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Validates the input parameters.
     */
    public void validate() throws PackagingException {
        if (scripts == null || !scripts.isDirectory()) {
            throw new PackagingException("The 'scripts' attribute doesn't point to a directory.");
        }

        if (Compression.toEnum(compression) == null || !Compression.toEnum(compression).equals(Compression.GZIP)) {
            throw new PackagingException("The compression method '" + compression + "' is not supported (expected 'gzip')");
        }

        if (spk == null) {
            throw new PackagingException("You need to specify where the spk file is supposed to be created.");
        }
        
        if (dataProducers.size() == 0) {
            throw new PackagingException("You need to provide at least one reference to a tgz or directory with data.");
        }
    }

    public void makeSpk() throws PackagingException {
        try {
            console.info("Creating synology package: " + spk);

            createSpk(Compression.toEnum(compression));
        } catch (Exception e) {
            throw new PackagingException("Failed to create synology package " + spk, e);
        }
    }
    
    /**
     * Create the synology archive with from the provided script files and data producers.
     *
     * @param compression   the compression method used for the data file
     * @return BinaryPackageInfoFile
     * @throws PackagingException
     */
    public BinaryPackageInfoFile createSpk(Compression compression) throws PackagingException {
        File tempData = null;
        File tempInfo = null;

        try {
            tempData = File.createTempFile("spk", "data");

            console.debug("Building data");
            SpkDataBuilder dataBuilder = new SpkDataBuilder(console);
            StringBuilder md5s = new StringBuilder();
            BigInteger size = dataBuilder.buildData(dataProducers, tempData, md5s, compression);

            MessageDigest md = MessageDigest.getInstance("MD5");
    		String digestMD5 = getDigest(new FileInputStream(tempData), md, 2048);
            
            console.debug("Building info");
            SpkInfoBuilder infoBuilder = new SpkInfoBuilder(console, variableResolver);
            BinaryPackageInfoFile packageInfoFile = infoBuilder.createPackageInfoFile(info, size, digestMD5);
            if (packageInfoFile.get("package") == null) {
                packageInfoFile.set("package", packageName);
            }
            if (packageInfoFile.get("description") == null) {
                packageInfoFile.set("description", description);
            }
            
            if (!packageInfoFile.isValid()) {
                throw new PackagingException("Info file fields are invalid " + packageInfoFile.invalidFields() +
                    ". The following fields are mandatory: " + packageInfoFile.getMandatoryFields() +
                    ". Please check your pom.xml/build.xml and your info file.");
            }

            spk.getParentFile().mkdirs();
            
            
            TarArchiveOutputStream tar = new TarArchiveOutputStream(new FileOutputStream(spk));
            
            addTo(tar, "INFO", packageInfoFile.toString(), "root", 0, "root", 0, PermMapper.toMode("644"));
            addTo(tar, "package.tgz", tempData, "root", 0, "root", 0, PermMapper.toMode("644"));
            SpkScriptsBuilder scriptsBuilder = new SpkScriptsBuilder(console, variableResolver, openReplaceToken, closeReplaceToken);
            scriptsBuilder.addDirectoryTo(tar, "scripts", scripts);

            tar.close();
            
            return packageInfoFile;

        } catch (Exception e) {
            throw new PackagingException("Could not create synology package", e);
        } finally {
            if (tempData != null) {
                if (!tempData.delete()) {
                    console.warn("Could not delete the temporary file " + tempData);
                }
            }
            if (tempInfo != null) {
                if (!tempInfo.delete()) {
                    console.warn("Could not delete the temporary file " + tempInfo);
                }
            }
        }
    }

	private String getDigest(InputStream is, MessageDigest md, int byteArraySize) throws NoSuchAlgorithmException, IOException {
		String checksum = null;
		byte[] buffer = new byte[8192];
		int numOfBytesRead;
		while ((numOfBytesRead = is.read(buffer)) > 0) {
			md.update(buffer, 0, numOfBytesRead);
		}
		byte[] hash = md.digest();
		checksum = new BigInteger(1, hash).toString(16); // don't use this, truncates leading zero

		return checksum;
	}
}
