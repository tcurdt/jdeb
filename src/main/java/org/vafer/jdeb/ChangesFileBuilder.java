/*
 * Copyright 2007-2024 The jdeb developers.
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
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.vafer.jdeb.changes.ChangesProvider;
import org.vafer.jdeb.debian.BinaryPackageControlFile;
import org.vafer.jdeb.debian.ChangesFile;
import org.vafer.jdeb.utils.InformationOutputStream;

/**
 * Builds the Debian changes file.
 */
class ChangesFileBuilder {

    public ChangesFile createChanges(BinaryPackageControlFile packageControlFile, File binaryPackage, ChangesProvider changesProvider) throws IOException, PackagingException {

        ChangesFile changesFile = new ChangesFile();
        changesFile.setChanges(changesProvider.getChangesSets());
        changesFile.initialize(packageControlFile);

        changesFile.set("Date", ChangesFile.formatDate(new Date()));

        try {
            // compute the checksums of the binary package
            InformationOutputStream md5output = new InformationOutputStream(NullOutputStream.INSTANCE, MessageDigest.getInstance("MD5"));
            InformationOutputStream sha1output = new InformationOutputStream(md5output, MessageDigest.getInstance("SHA1"));
            InformationOutputStream sha256output = new InformationOutputStream(sha1output, MessageDigest.getInstance("SHA-256"));

            FileUtils.copyFile(binaryPackage, sha256output);

            // Checksums-Sha1:
            //  56ef4c6249dc3567fd2967f809c42d1f9b61adf7 45964 jdeb.deb
            changesFile.set("Checksums-Sha1", sha1output.getHexDigest() + " " + binaryPackage.length() + " " + binaryPackage.getName());

            // Checksums-Sha256:
            //  38c6fa274eb9299a69b739bcbdbd05c7ffd1d8d6472f4245ed732a25c0e5d616 45964 jdeb.deb
            changesFile.set("Checksums-Sha256", sha256output.getHexDigest() + " " + binaryPackage.length() + " " + binaryPackage.getName());

            StringBuilder files = new StringBuilder(md5output.getHexDigest());
            files.append(' ').append(binaryPackage.length());
            files.append(' ').append(packageControlFile.get("Section"));
            files.append(' ').append(packageControlFile.get("Priority"));
            files.append(' ').append(binaryPackage.getName());
            changesFile.set("Files", files.toString());

        } catch (NoSuchAlgorithmException e) {
            throw new PackagingException("Unable to compute the checksums for " + binaryPackage, e);
        }

        if (!changesFile.isValid()) {
            throw new PackagingException("Changes file fields are invalid " + changesFile.invalidFields() +
                ". The following fields are mandatory: " + changesFile.getMandatoryFields() +
                ". Please check your pom.xml/build.xml and your control file.");
        }

        return changesFile;
    }
}
