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
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;

import org.vafer.jdeb.synology.BinaryPackageInfoFile;
import org.vafer.jdeb.utils.FilteredFile;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * Builds the info file of the Synology package.
 */
class SpkInfoBuilder {

    private Console console;
    private VariableResolver resolver;

    SpkInfoBuilder(Console console, VariableResolver resolver) {
        this.console = console;
        this.resolver = resolver;
    }
    
    /**
	 * Creates a package info file from the specified file. The
	 * <tt>Maintainer</tt> field is overridden by the <tt>SPKEMAIL</tt> and
	 * <tt>SPKFULLNAME</tt> environment variables if defined.
	 * 
	 * @param file
	 *            the control file
	 * @param pDataSize
	 *            the size of the installed package
	 */
    public BinaryPackageInfoFile createPackageInfoFile(final File file, final BigInteger pDataSize, final String pChecksum) throws IOException, ParseException {
    	final BinaryPackageInfoFile packageInfoFile;
    	if (file != null) {
    		FilteredFile infoFile = new FilteredFile(new FileInputStream(file), resolver);
        	packageInfoFile = new BinaryPackageInfoFile(infoFile.toString());
    	} else {
    		packageInfoFile = new BinaryPackageInfoFile("");
    	}
        
        packageInfoFile.set("extractsize", pDataSize.toString());
        
        packageInfoFile.set("checksum", pChecksum);
        
        // override the Version if the SPKVERSION environment variable is defined
        final String spkVersion = System.getenv("SPKVERSION");
        if (spkVersion != null) {
            packageInfoFile.set("version", spkVersion);
            console.debug("Using version'" + spkVersion + "' from the environment variables.");
        }

        // override the Maintainer field if the SPKFULLNAME and SPKEMAIL environment variables are defined
        final String spkFullName = System.getenv("SPKFULLNAME");
        final String spkEmail = System.getenv("SPKEMAIL");

        if (spkFullName != null && spkEmail != null) {
            final String maintainer = spkFullName + " <" + spkEmail + ">";
            packageInfoFile.set("maintainer", maintainer);
            console.debug("Using maintainer '" + maintainer + "' from the environment variables.");
        }
        
        return packageInfoFile;
    }
}
