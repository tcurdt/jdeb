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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.tools.ant.DirectoryScanner;
import org.vafer.jdeb.debian.BinaryPackageControlFile;
import org.vafer.jdeb.mapping.PermMapper;
import org.vafer.jdeb.utils.FilteredFile;
import org.vafer.jdeb.utils.InformationInputStream;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * Builds the control archive of the Debian package.
 */
class ControlBuilder {
    
    /** The name of the package maintainer scripts */
    private static final Set<String> MAINTAINER_SCRIPTS = new HashSet<String>(Arrays.asList("preinst", "postinst", "prerm", "postrm", "config"));

    /** The name of the other control files subject to token substitution */
    private static final Set<String> CONFIGURATION_FILENAMES = new HashSet<String>(Arrays.asList("conffiles", "templates", "triggers"));

    private Console console;
    private VariableResolver resolver;

    ControlBuilder(Console console, VariableResolver resolver) {
        this.console = console;
        this.resolver = resolver;
    }

    /**
     * Build control archive of the deb
     *
     * @param packageControlFile the package control file
     * @param controlFiles the other control information files (maintainer scripts, etc)
     * @param dataSize  the size of the installed package
     * @param checksums the md5 checksums of the files in the data archive
     * @param output
     * @return
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     * @throws java.text.ParseException
     */
    void buildControl(BinaryPackageControlFile packageControlFile, File[] controlFiles, List<String> conffiles, StringBuilder checksums, File output) throws IOException, ParseException {
        final File dir = output.getParentFile();
        if (dir != null && (!dir.exists() || !dir.isDirectory())) {
            throw new IOException("Cannot write control file at '" + output.getAbsolutePath() + "'");
        }

        final TarArchiveOutputStream outputStream = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(output)));
        outputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        
        boolean foundConffiles = false;
        
        // create the final package control file out of the "control" file, copy all other files, ignore the directories
        for (File file : controlFiles) {
            if (file.isDirectory()) {
                // warn about the misplaced directory, except for directories ignored by default (.svn, cvs, etc)
                if (!isDefaultExcludes(file)) {
                    console.warn("Found directory '" + file + "' in the control directory. Maybe you are pointing to wrong dir?");
                }
                continue;
            }

            if ("conffiles".equals(file.getName())) {
                foundConffiles = true;
            }
            
            if (CONFIGURATION_FILENAMES.contains(file.getName()) || MAINTAINER_SCRIPTS.contains(file.getName())) {
                FilteredFile configurationFile = new FilteredFile(new FileInputStream(file), resolver);
                addControlEntry(file.getName(), configurationFile.toString(), outputStream);

            } else if (!"control".equals(file.getName())) {
                // initialize the information stream to guess the type of the file
                InformationInputStream infoStream = new InformationInputStream(new FileInputStream(file));
                Utils.copy(infoStream, NullOutputStream.NULL_OUTPUT_STREAM);
                infoStream.close();

                // fix line endings for shell scripts
                InputStream in = new FileInputStream(file);
                if (infoStream.isShell() && !infoStream.hasUnixLineEndings()) {
                    byte[] buf = Utils.toUnixLineEndings(in);
                    in = new ByteArrayInputStream(buf);
                }
                
                addControlEntry(file.getName(), IOUtils.toString(in), outputStream);
                
                in.close();
            }
        }
        
        if ((conffiles != null) && (conffiles.size() > 0)) {
            if (foundConffiles) {
                console.info("Found file 'conffiles' in the control directory. Skipping conffiles generation.");
            } else {
                addControlEntry("conffiles", createPackageConffilesFile(conffiles), outputStream);
            }
        } else if ((conffiles != null) && (conffiles.size() == 0)) {
            console.info("Skipping 'conffiles' generation. No entries provided.");
        }

        if (packageControlFile == null) {
            throw new FileNotFoundException("No 'control' file found in " + controlFiles.toString());
        }
        
        addControlEntry("control", packageControlFile.toString(), outputStream);
        addControlEntry("md5sums", checksums.toString(), outputStream);

        outputStream.close();
    }
    
    private String createPackageConffilesFile(final List<String> conffiles) {
        StringBuilder content = new StringBuilder();
        
        if (conffiles != null && !conffiles.isEmpty()) {
            for (String nextFileName : conffiles) {
                content.append(nextFileName).append("\n");
            }
        }

        return content.toString();
    }
    
    
    /**
     * Creates a package control file from the specified file and adds the
     * <tt>Date</tt>, <tt>Distribution</tt> and <tt>Urgency</tt> fields if missing.
     * The <tt>Installed-Size</tt> field is also initialized to the actual size of
     * the package. The <tt>Maintainer</tt> field is overridden by the <tt>DEBEMAIL</tt>
     * and <tt>DEBFULLNAME</tt> environment variables if defined.
     * 
     * @param file       the control file
     * @param pDataSize  the size of the installed package
     */
    public BinaryPackageControlFile createPackageControlFile(File file, BigInteger pDataSize) throws IOException, ParseException {
        FilteredFile controlFile = new FilteredFile(new FileInputStream(file), resolver);
        BinaryPackageControlFile packageControlFile = new BinaryPackageControlFile(controlFile.toString());
        
        if (packageControlFile.get("Distribution") == null) {
            packageControlFile.set("Distribution", "unknown");
        }

        if (packageControlFile.get("Urgency") == null) {
            packageControlFile.set("Urgency", "low");
        }

        packageControlFile.set("Installed-Size", pDataSize.divide(BigInteger.valueOf(1024)).toString());

        // override the Version if the DEBVERSION environment variable is defined
        final String debVersion = System.getenv("DEBVERSION");
        if (debVersion != null) {
            packageControlFile.set("Version", debVersion);
            console.debug("Using version'" + debVersion + "' from the environment variables.");
        }


        // override the Maintainer field if the DEBFULLNAME and DEBEMAIL environment variables are defined
        final String debFullName = System.getenv("DEBFULLNAME");
        final String debEmail = System.getenv("DEBEMAIL");

        if (debFullName != null && debEmail != null) {
            final String maintainer = debFullName + " <" + debEmail + ">";
            packageControlFile.set("Maintainer", maintainer);
            console.debug("Using maintainer '" + maintainer + "' from the environment variables.");
        }
        
        return packageControlFile;
    }


    private static void addControlEntry(final String pName, final String pContent, final TarArchiveOutputStream pOutput) throws IOException {
        final byte[] data = pContent.getBytes("UTF-8");

        final TarArchiveEntry entry = new TarArchiveEntry("./" + pName, true);
        entry.setSize(data.length);
        entry.setNames("root", "root");
        
        if (MAINTAINER_SCRIPTS.contains(pName)) {
            entry.setMode(PermMapper.toMode("755"));
        } else {
            entry.setMode(PermMapper.toMode("644"));
        }
        
        pOutput.putArchiveEntry(entry);
        pOutput.write(data);
        pOutput.closeArchiveEntry();
    }
    
    /**
     * Tells if the specified directory is ignored by default (.svn, cvs, etc)
     * 
     * @param directory
     */
    private boolean isDefaultExcludes(File directory) {
        for (String pattern : DirectoryScanner.getDefaultExcludes()) {
            if (DirectoryScanner.match(pattern, directory.getAbsolutePath().replace("\\", "/"))) {
                return true;
            }
        }
        
        return false;
    }
}
