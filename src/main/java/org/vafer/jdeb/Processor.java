/*
 * Copyright 2013 The jdeb developers.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.tools.ant.DirectoryScanner;
import org.vafer.jdeb.changes.ChangeSet;
import org.vafer.jdeb.changes.ChangesProvider;
import org.vafer.jdeb.control.FilteredConfigurationFile;
import org.vafer.jdeb.descriptors.ChangesDescriptor;
import org.vafer.jdeb.descriptors.PackageDescriptor;
import org.vafer.jdeb.mapping.PermMapper;
import org.vafer.jdeb.signing.SigningUtils;
import org.vafer.jdeb.utils.InformationInputStream;
import org.vafer.jdeb.utils.InformationOutputStream;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * The processor does the actual work of building the deb related files.
 * It is been used by the ant task and (later) the maven plugin.
 *
 * @author Torsten Curdt
 */
public class Processor {

    /** The name of the package maintainer scripts */
    private static final Set<String> MAINTAINER_SCRIPTS = new HashSet<String>(Arrays.asList("preinst", "postinst", "prerm", "postrm", "config"));

    /** The name of the other control files subject to token substitution */
    private static final Set<String> CONFIGURATION_FILENAMES = new HashSet<String>(Arrays.asList("conffiles", "templates", "triggers"));

    private final Console console;
    private final VariableResolver resolver;

    private static final class Total {
        private BigInteger count = BigInteger.valueOf(0);

        public void add( long size ) {
            count = count.add(BigInteger.valueOf(size));
        }

        public String toString() {
            return "" + count;
        }
    }

    public Processor( final Console pConsole, final VariableResolver pResolver ) {
        console = pConsole;
        resolver = pResolver;
    }

    private void addTo( final ArArchiveOutputStream pOutput, final String pName, final String pContent ) throws IOException {
        final byte[] content = pContent.getBytes();
        pOutput.putArchiveEntry(new ArArchiveEntry(pName, content.length));
        pOutput.write(content);
        pOutput.closeArchiveEntry();
    }

    private void addTo( final ArArchiveOutputStream pOutput, final String pName, final File pContent ) throws IOException {
        pOutput.putArchiveEntry(new ArArchiveEntry(pName, pContent.length()));

        final InputStream input = new FileInputStream(pContent);
        try {
            Utils.copy(input, pOutput);
        } finally {
            input.close();
        }

        pOutput.closeArchiveEntry();
    }

    /**
     * Create the debian archive with from the provided control files and data producers.
     *
     * @param pControlFiles
     * @param pData
     * @param pOutput
     * @param compression   the compression method used for the data file
     * @return PackageDescriptor
     * @throws PackagingException
     */
    public PackageDescriptor createDeb( final File[] pControlFiles, final DataProducer[] pData, final File pOutput, Compression compression ) throws PackagingException {

        File tempData = null;
        File tempControl = null;

        try {
            tempData = File.createTempFile("deb", "data");
            tempControl = File.createTempFile("deb", "control");

            console.info("Building data");
            final StringBuilder md5s = new StringBuilder();
            final BigInteger size = buildData(pData, tempData, md5s, compression);

            console.info("Building control");
            final PackageDescriptor packageDescriptor = buildControl(pControlFiles, size, md5s, tempControl);

            if (!packageDescriptor.isValid()) {
                throw new PackagingException("Control file fields are invalid " + packageDescriptor.invalidFields() +
                    ". The following fields are mandatory: " + packageDescriptor.getMandatoryFields() +
                    ". Please check your pom.xml/build.xml and your control file.");
            }

            pOutput.getParentFile().mkdirs();

            // pass through stream chain to calculate all the different digests
            final InformationOutputStream md5output = new InformationOutputStream(new FileOutputStream(pOutput), MessageDigest.getInstance("MD5"));
            final InformationOutputStream sha1output = new InformationOutputStream(md5output, MessageDigest.getInstance("SHA1"));
            final InformationOutputStream sha256output = new InformationOutputStream(sha1output, MessageDigest.getInstance("SHA-256"));
            final ArArchiveOutputStream ar = new ArArchiveOutputStream(sha256output);

            addTo(ar, "debian-binary", "2.0\n");
            addTo(ar, "control.tar.gz", tempControl);
            addTo(ar, "data.tar" + compression.getExtension(), tempData);

            ar.close();

            // intermediate values
            packageDescriptor.set("MD5", md5output.getHexDigest());
            packageDescriptor.set("SHA1", sha1output.getHexDigest());
            packageDescriptor.set("SHA256", sha256output.getHexDigest());
            packageDescriptor.set("Size", "" + md5output.getSize());
            packageDescriptor.set("File", pOutput.getName());

            return packageDescriptor;

        } catch (Exception e) {
            throw new PackagingException("Could not create deb package", e);
        } finally {
            if (tempData != null) {
                if (!tempData.delete()) {
                    console.warn("Could not delete the temporary file " + tempData);
                }
            }
            if (tempControl != null) {
                if (!tempControl.delete()) {
                    console.warn("Could not delete the temporary file " + tempControl);
                }
            }
        }
    }

    /**
     * Create changes file based on the provided PackageDescriptor.
     * If pRing, pKey and pPassphrase are provided the changes file will also be signed.
     * It returns a ChangesDescriptor reflecting the changes
     *
     * @param pPackageDescriptor
     * @param pChangesProvider
     * @param pRing
     * @param pKey
     * @param pPassphrase
     * @param pOutput
     * @return ChangesDescriptor
     * @throws IOException
     * @throws PackagingException
     */
    public ChangesDescriptor createChanges( final PackageDescriptor pPackageDescriptor, final ChangesProvider pChangesProvider, final InputStream pRing, final String pKey, final String pPassphrase, final OutputStream pOutput ) throws IOException, PackagingException {

        final ChangeSet[] changeSets = pChangesProvider.getChangesSets();
        final ChangesDescriptor changesDescriptor = new ChangesDescriptor(pPackageDescriptor, changeSets);

        changesDescriptor.set("Format", "1.8");

        if (changesDescriptor.get("Binary") == null) {
            changesDescriptor.set("Binary", changesDescriptor.get("Package"));
        }

        if (changesDescriptor.get("Source") == null) {
            changesDescriptor.set("Source", changesDescriptor.get("Package"));
        }

        final StringBuilder checksumsSha1 = new StringBuilder("\n");
        // Checksums-Sha1:
        // 56ef4c6249dc3567fd2967f809c42d1f9b61adf7 45964 jdeb.deb
        checksumsSha1.append(' ').append(changesDescriptor.get("SHA1"));
        checksumsSha1.append(' ').append(changesDescriptor.get("Size"));
        checksumsSha1.append(' ').append(changesDescriptor.get("File"));
        changesDescriptor.set("Checksums-Sha1", checksumsSha1.toString());

        final StringBuilder checksumsSha256 = new StringBuilder("\n");
        // Checksums-Sha256:
        // 38c6fa274eb9299a69b739bcbdbd05c7ffd1d8d6472f4245ed732a25c0e5d616 45964 jdeb.deb
        checksumsSha256.append(' ').append(changesDescriptor.get("SHA256"));
        checksumsSha256.append(' ').append(changesDescriptor.get("Size"));
        checksumsSha256.append(' ').append(changesDescriptor.get("File"));
        changesDescriptor.set("Checksums-Sha256", checksumsSha256.toString());


        final StringBuilder files = new StringBuilder("\n");
        files.append(' ').append(changesDescriptor.get("MD5"));
        files.append(' ').append(changesDescriptor.get("Size"));
        files.append(' ').append(changesDescriptor.get("Section"));
        files.append(' ').append(changesDescriptor.get("Priority"));
        files.append(' ').append(changesDescriptor.get("File"));
        changesDescriptor.set("Files", files.toString());

        if (!changesDescriptor.isValid()) {
            throw new PackagingException("Changes file fields are invalid " + changesDescriptor.invalidFields() +
                ". The following fields are mandatory: " + changesDescriptor.getMandatoryFields() +
                ". Please check your pom.xml/build.xml and your control file.");
        }

        final String changes = changesDescriptor.toString();
        final byte[] changesBytes = changes.getBytes("UTF-8");

        if (pRing == null || pKey == null || pPassphrase == null) {
            pOutput.write(changesBytes);
            pOutput.close();
            return changesDescriptor;
        }

        console.info("Signing changes with key " + pKey);

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
     *
     * @param pControlFiles
     * @param pDataSize
     * @param pChecksums
     * @param pOutput
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ParseException
     */
    private PackageDescriptor buildControl( final File[] pControlFiles, final BigInteger pDataSize, final StringBuilder pChecksums, final File pOutput ) throws IOException, ParseException {

        final File dir = pOutput.getParentFile();
        if (dir != null && (!dir.exists() || !dir.isDirectory())) {
            throw new IOException("Cannot write control file at '" + pOutput.getAbsolutePath() + "'");
        }

        final TarArchiveOutputStream outputStream = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(pOutput)));
        outputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        
        List<FilteredConfigurationFile> configurationFiles = new ArrayList<FilteredConfigurationFile>();
        
        // create a descriptor out of the "control" file, copy all other files, ignore directories
        PackageDescriptor packageDescriptor = null;
        for (File file : pControlFiles) {
            if (file.isDirectory()) {
                // warn about the misplaced directory, except for directories ignored by default (.svn, cvs, etc)
                if (!isDefaultExcludes(file)) {
                    console.info("Found directory '" + file + "' in the control directory. Maybe you are pointing to wrong dir?");
                }
                continue;
            }

            if (CONFIGURATION_FILENAMES.contains(file.getName()) || MAINTAINER_SCRIPTS.contains(file.getName())) {
                FilteredConfigurationFile configurationFile = new FilteredConfigurationFile(file.getName(), new FileInputStream(file), resolver);
                configurationFiles.add(configurationFile);

            } else if ("control".equals(file.getName())) {
                packageDescriptor = createPackageDescriptor(file, pDataSize);

            } else {
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

        if (packageDescriptor == null) {
            throw new FileNotFoundException("No 'control' file found in " + Arrays.toString(pControlFiles));
        }

        for (FilteredConfigurationFile configurationFile : configurationFiles) {
            addControlEntry(configurationFile.getName(), configurationFile.toString(), outputStream);
        }
        addControlEntry("control", packageDescriptor.toString(), outputStream);
        addControlEntry("md5sums", pChecksums.toString(), outputStream);

        outputStream.close();

        return packageDescriptor;
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
    
    /**
     * Creates a package descriptor from the specified control file and adds the
     * <tt>Date</tt>, <tt>Distribution</tt> and <tt>Urgency</tt> fields if missing.
     * The <tt>Installed-Size</tt> field is also initialized to the actual size of
     * the package. The <tt>Maintainer</tt> field is overridden by the <tt>DEBEMAIL</tt>
     * and <tt>DEBFULLNAME</tt> environment variables if defined.
     * 
     * @param file       the control file
     * @param pDataSize  the size of the installed package
     */
    private PackageDescriptor createPackageDescriptor(File file, BigInteger pDataSize) throws IOException, ParseException {
        PackageDescriptor packageDescriptor = new PackageDescriptor(new FileInputStream(file), resolver);

        if (packageDescriptor.get("Date") == null) {
            // Mon, 26 Mar 2007 11:44:04 +0200 (RFC 2822)
            SimpleDateFormat fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            packageDescriptor.set("Date", fmt.format(new Date()));
        }

        if (packageDescriptor.get("Distribution") == null) {
            packageDescriptor.set("Distribution", "unknown");
        }

        if (packageDescriptor.get("Urgency") == null) {
            packageDescriptor.set("Urgency", "low");
        }

        packageDescriptor.set("Installed-Size", pDataSize.divide(BigInteger.valueOf(1024)).toString());

        // override the Version if the DEBVERSION environment variable is defined
        final String debVersion = System.getenv("DEBVERSION");
        if (debVersion != null) {
            packageDescriptor.set("Version", debVersion);
            console.info("Using version'" + debVersion + "' from the environment variables.");
        }


        // override the Maintainer field if the DEBFULLNAME and DEBEMAIL environment variables are defined
        final String debFullName = System.getenv("DEBFULLNAME");
        final String debEmail = System.getenv("DEBEMAIL");

        if (debFullName != null && debEmail != null) {
            final String maintainer = debFullName + " <" + debEmail + ">";
            packageDescriptor.set("Maintainer", maintainer);
            console.info("Using maintainer '" + maintainer + "' from the environment variables.");
        }
        
        return packageDescriptor;
    }

    /**
     * Build the data archive of the deb from the provided DataProducers
     *
     * @param pData
     * @param pOutput
     * @param pChecksums
     * @param compression the compression method used for the data file
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws CompressorException
     */
    BigInteger buildData( final DataProducer[] pData, final File pOutput, final StringBuilder pChecksums, Compression compression ) throws NoSuchAlgorithmException, IOException, CompressorException {

        final File dir = pOutput.getParentFile();
        if (dir != null && (!dir.exists() || !dir.isDirectory())) {
            throw new IOException("Cannot write data file at '" + pOutput.getAbsolutePath() + "'");
        }

        final TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(compression.toCompressedOutputStream(new FileOutputStream(pOutput)));
        tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

        final MessageDigest digest = MessageDigest.getInstance("MD5");

        final Total dataSize = new Total();

        final List<String> addedDirectories = new ArrayList<String>();
        final DataConsumer receiver = new DataConsumer() {
            public void onEachDir( String dirname, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {
                dirname = fixPath(dirname);

                createParentDirectories((new File(dirname)).getParent(), user, uid, group, gid);

                // The directory passed in explicitly by the caller also gets the passed-in mode.  (Unlike
                // the parent directories for now.  See related comments at "int mode =" in
                // createParentDirectories, including about a possible bug.)
                createDirectory(dirname, user, uid, group, gid, mode, 0);

                console.info("dir: " + dirname);
            }

            public void onEachFile( InputStream inputStream, String filename, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {
                filename = fixPath(filename);

                createParentDirectories((new File(filename)).getParent(), user, uid, group, gid);

                final TarArchiveEntry entry = new TarArchiveEntry(filename, true);

                entry.setUserName(user);
                entry.setUserId(uid);
                entry.setGroupName(group);
                entry.setGroupId(gid);
                entry.setMode(mode);
                entry.setSize(size);

                tarOutputStream.putArchiveEntry(entry);

                dataSize.add(size);
                digest.reset();

                Utils.copy(inputStream, new DigestOutputStream(tarOutputStream, digest));

                final String md5 = Utils.toHex(digest.digest());

                tarOutputStream.closeArchiveEntry();

                console.info(
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

                // append to file md5 list
                pChecksums.append(md5).append(" ").append(entry.getName()).append('\n');
            }

            public void onEachLink(String path, String linkName, boolean symlink, String user, int uid, String group, int gid, int mode) throws IOException {
                path = fixPath(path);

                createParentDirectories((new File(path)).getParent(), user, uid, group, gid);

                final TarArchiveEntry entry = new TarArchiveEntry(path, symlink ? TarArchiveEntry.LF_SYMLINK : TarArchiveEntry.LF_LINK);
                entry.setLinkName(linkName);

                entry.setUserName(user);
                entry.setUserId(uid);
                entry.setGroupName(group);
                entry.setGroupId(gid);
                entry.setMode(mode);

                tarOutputStream.putArchiveEntry(entry);
                tarOutputStream.closeArchiveEntry();

                console.info(
                    "link:" + entry.getName() +
                    " mode:" + entry.getMode() +
                    " linkname:" + entry.getLinkName() +
                    " username:" + entry.getUserName() +
                    " userid:" + entry.getUserId() +
                    " groupname:" + entry.getGroupName() +
                    " groupid:" + entry.getGroupId()
                 );
            }

            private String fixPath( String path ) {
                // If we're receiving directory names from Windows, then we'll convert to use slash
                // This does eliminate the ability to use of a backslash in a directory name on *NIX,
                // but in practice, this is a non-issue
                if (path.indexOf('\\') > -1) {
                    path = path.replace('\\', '/');
                }
                // ensure the path is like : ./foo/bar
                if (path.startsWith("/")) {
                    path = "." + path;
                } else if (!path.startsWith("./")) {
                    path = "./" + path;
                }
                return path;
            }

            private void createDirectory( String directory, String user, int uid, String group, int gid, int mode, long size ) throws IOException {
                // All dirs should end with "/" when created, or the test DebAndTaskTestCase.testTarFileSet() thinks its a file
                // and so thinks it has the wrong permission.
                // This consistency also helps when checking if a directory already exists in addedDirectories.

                if (!directory.endsWith("/")) {
                    directory += "/";
                }

                if (!addedDirectories.contains(directory)) {
                    TarArchiveEntry entry = new TarArchiveEntry(directory, true);
                    entry.setUserName(user);
                    entry.setUserId(uid);
                    entry.setGroupName(group);
                    entry.setGroupId(gid);
                    entry.setMode(mode);
                    entry.setSize(size);

                    tarOutputStream.putArchiveEntry(entry);
                    tarOutputStream.closeArchiveEntry();
                    addedDirectories.add(directory); // so addedDirectories consistently have "/" for finding duplicates.
                }
            }

            private void createParentDirectories( String dirname, String user, int uid, String group, int gid ) throws IOException {
                // Debian packages must have parent directories created
                // before sub-directories or files can be installed.
                // For example, if an entry of ./usr/lib/foo/bar existed
                // in a .deb package, but the ./usr/lib/foo directory didn't
                // exist, the package installation would fail.  The .deb must
                // then have an entry for ./usr/lib/foo and then ./usr/lib/foo/bar

                if (dirname == null) {
                    return;
                }

                // The loop below will create entries for all parent directories
                // to ensure that .deb packages will install correctly.
                String[] pathParts = dirname.split("\\/");
                String parentDir = "./";
                for (int i = 1; i < pathParts.length; i++) {
                    parentDir += pathParts[i] + "/";
                    // Make it so the dirs can be traversed by users.
                    // We could instead try something more granular, like setting the directory
                    // permission to 'rx' for each of the 3 user/group/other read permissions
                    // found on the file being added (ie, only if "other" has read
                    // permission on the main node, then add o+rx permission on all the containing
                    // directories, same w/ user & group), and then also we'd have to
                    // check the parentDirs collection of those already added to
                    // see if those permissions need to be similarly updated.  (Note, it hasn't
                    // been demonstrated, but there might be a bug if a user specifically
                    // requests a directory with certain permissions,
                    // that has already been auto-created because it was a parent, and if so, go set
                    // the user-requested mode on that directory instead of this automatic one.)
                    // But for now, keeping it simple by making every dir a+rx.   Examples are:
                    // drw-r----- fs/fs   # what you get with setMode(mode)
                    // drwxr-xr-x fs/fs   # Usable. Too loose?
                    int mode = TarArchiveEntry.DEFAULT_DIR_MODE;

                    createDirectory(parentDir, user, uid, group, gid, mode, 0);
                }
            }
        };

        try {
            for (DataProducer data : pData) {
                data.produce(receiver);
            }
        } finally {
            tarOutputStream.close();
        }

        console.info("Total size: " + dataSize);

        return dataSize.count;
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
}
