/*
 * Copyright 2010 The Apache Software Foundation.
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
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.bzip2.CBZip2OutputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.vafer.jdeb.ar.ArEntry;
import org.vafer.jdeb.ar.ArOutputStream;
import org.vafer.jdeb.changes.ChangeSet;
import org.vafer.jdeb.changes.ChangesProvider;
import org.vafer.jdeb.descriptors.ChangesDescriptor;
import org.vafer.jdeb.descriptors.InvalidDescriptorException;
import org.vafer.jdeb.descriptors.PackageDescriptor;
import org.vafer.jdeb.mapping.PermMapper;
import org.vafer.jdeb.signing.SigningUtils;
import org.vafer.jdeb.utils.InformationOutputStream;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * The processor does the actual work of building the deb related files.
 * It is been used by the ant task and (later) the maven plugin.
 * 
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public class Processor {

    private final Console console;
    private final VariableResolver resolver;

    private static final class Total {
        private BigInteger count = BigInteger.valueOf(0);

        public void add(long size) {
            count = count.add(BigInteger.valueOf(size));
        }

        public String toString() {
            return "" + count;
        }

//        public BigInteger toBigInteger() {
//            return count;
//        }
    }

    public Processor( final Console pConsole, final VariableResolver pResolver ) {
        console = pConsole;
        resolver = pResolver;
    }

    private void addTo( final ArOutputStream pOutput, final String pName, final String pContent ) throws IOException {
        final byte[] content = pContent.getBytes(); 
        pOutput.putNextEntry(new ArEntry(pName, content.length));
        pOutput.write(content);
    }

    private void addTo( final ArOutputStream pOutput, final String pName, final File pContent ) throws IOException {
        pOutput.putNextEntry(new ArEntry(pName, pContent.length()));
        
        final InputStream input = new FileInputStream(pContent);
        try {
            Utils.copy(input, pOutput);
        } finally {
            input.close();
        }
    }
    
    /**
     * Create the debian archive with from the provided control files and data producers.
     * 
     * @param pControlFiles
     * @param pData
     * @param pOutput
     * @param compression the compression method used for the data file (gzip, bzip2 or anything else for no compression)
     * @return PackageDescriptor
     * @throws PackagingException
     */
    public PackageDescriptor createDeb( final File[] pControlFiles, final DataProducer[] pData, final File pOutput, String compression ) throws PackagingException, InvalidDescriptorException {

        File tempData = null;
        File tempControl = null;

        try {
            tempData = File.createTempFile("deb", "data");          
            tempControl = File.createTempFile("deb", "control");            

            console.println("Building data");
            final StringBuffer md5s = new StringBuffer();
            final BigInteger size = buildData(pData, tempData, md5s, compression);

            console.println("Building control");
            final PackageDescriptor packageDescriptor = buildControl(pControlFiles, size, md5s, tempControl);

            if (!packageDescriptor.isValid()) {
                throw new InvalidDescriptorException(packageDescriptor);
            }

            final InformationOutputStream output = new InformationOutputStream(new FileOutputStream(pOutput), MessageDigest.getInstance("MD5"));

            final ArOutputStream ar = new ArOutputStream(output);

            addTo(ar, "debian-binary", "2.0\n");
            addTo(ar, "control.tar.gz", tempControl);
            addTo(ar, "data.tar" + getExtension(compression), tempData);
            
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
                if (!tempData.delete()) {
                    throw new PackagingException("Could not delete " + tempData);                   
                }
            }
            if (tempControl != null) {
                if (!tempControl.delete()) {
                    throw new PackagingException("Could not delete " + tempControl);                    
                }
            }
        }
    }

    /**
     * Return the extension of a file compressed with the specified method.
     *
     * @param pCompression the compression method used
     * @return
     */
    private String getExtension( final String pCompression ) {
        if ("gzip".equals(pCompression)) {
            return ".gz";
        } else if ("bzip2".equals(pCompression)) {
            return ".bz2";
        } else {
            return "";
        }
    }

    /**
     * Create changes file based on the provided PackageDescriptor.
     * If pRing, pKey and pPassphrase are provided the changes file will also be signed.
     * It returns a ChangesDescriptor reflecting the changes  
     * @param pPackageDescriptor
     * @param pChangesProvider
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
    private PackageDescriptor buildControl( final File[] pControlFiles, final BigInteger pDataSize, final StringBuffer pChecksums, final File pOutput ) throws IOException, ParseException {

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

            entry.setName("./" + name);
            entry.setNames("root", "root");
            entry.setMode(PermMapper.toMode("755"));

            if ("control".equals(name)) {
                packageDescriptor = new PackageDescriptor(new FileInputStream(file), resolver);

                if (packageDescriptor.get("Date") == null) {
                    SimpleDateFormat fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH); // Mon, 26 Mar 2007 11:44:04 +0200 (RFC 2822)
                    // FIXME Is this field allowed in package descriptors ?
                    packageDescriptor.set("Date", fmt.format(new Date()));
                }

                if (packageDescriptor.get("Distribution") == null) {
                    packageDescriptor.set("Distribution", "unknown");
                }

                if (packageDescriptor.get("Urgency") == null) {
                    packageDescriptor.set("Urgency", "low");
                }

                final String debFullName = System.getenv("DEBFULLNAME");
                final String debEmail = System.getenv("DEBEMAIL");

                if (debFullName != null && debEmail != null) {
                    packageDescriptor.set("Maintainer", debFullName + " <" + debEmail + ">");
                    console.println("Using maintainer from the environment variables.");
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

        packageDescriptor.set("Installed-Size", pDataSize.divide(BigInteger.valueOf(1024)).toString());

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
     * @param pCompression the compression method used for the data file (gzip, bzip2 or anything else for no compression)
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    BigInteger buildData( final DataProducer[] pData, final File pOutput, final StringBuffer pChecksums, String pCompression ) throws NoSuchAlgorithmException, IOException {

        OutputStream out = new FileOutputStream(pOutput);
        if ("gzip".equals(pCompression)) {
            out = new GZIPOutputStream(out);
        } else if ("bzip2".equals(pCompression)) {
            out.write("BZ".getBytes());
            out = new CBZip2OutputStream(out);
        }
        
        final TarOutputStream outputStream = new TarOutputStream(out);
        outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

        final MessageDigest digest = MessageDigest.getInstance("MD5");

        final Total dataSize = new Total();

        final List addedDirectories = new ArrayList();
        final DataConsumer receiver = new DataConsumer() {
            public void onEachDir( String dirname, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {
                dirname = fixPath(dirname);
                
                createParentDirectories((new File(dirname)).getParent(), user, uid, group, gid);
                
                // The directory passed in explicitly by the caller also gets the passed-in mode.  (Unlike
                // the parent directories for now.  See related comments at "int mode =" in 
                // createParentDirectories, including about a possible bug.)
                createDirectory(dirname, user, uid, group, gid, mode, 0);

                console.println("dir: " + dirname);
            }

            public void onEachFile( InputStream inputStream, String filename, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {
                filename = fixPath(filename);

                createParentDirectories((new File(filename)).getParent(), user, uid, group, gid);

                TarEntry entry = new TarEntry(filename);

                // FIXME: link is in the constructor
                entry.setUserName(user);
                entry.setUserId(uid);
                entry.setGroupName(group);
                entry.setGroupId(gid);
                entry.setMode(mode);
                entry.setSize(size);

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
            
            private String fixPath(String path) {
                // If we're receiving directory names from Windows, then we'll convert to use slash
                // This does eliminate the ability to use of a backslash in a directory name on *NIX, but in practice, this is a non-issue
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
            
            private void createDirectory(String directory, String user, int uid, String group, int gid, int mode, long size) throws IOException {
                // All dirs should end with "/" when created, or the test DebAndTaskTestCase.testTarFileSet() thinks its a file
                // and so thinks it has the wrong permission.
                // This consistency also helps when checking if a directory already exists in addedDirectories.
              
                directory += (directory.endsWith("/") ? "" : "/"); 
                
                if (!addedDirectories.contains(directory)) {
                    TarEntry entry = new TarEntry(directory);
                    // FIXME: link is in the constructor
                    entry.setUserName(user);
                    entry.setUserId(uid);
                    entry.setGroupName(group);
                    entry.setGroupId(gid);
                    entry.setMode(mode);
                    entry.setSize(size);

                    outputStream.putNextEntry(entry);
                    outputStream.closeEntry();
                    addedDirectories.add(directory); // so addedDirectories consistently have "/" for finding duplicates.
                }
            }

            private void createParentDirectories(String dirname, String user, int uid, String group, int gid) throws IOException {
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
                    int mode = TarEntry.DEFAULT_DIR_MODE;

                    createDirectory(parentDir, user, uid, group, gid, mode, 0);
                }
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

        final TarEntry entry = new TarEntry("./" + pName);
        entry.setSize(data.length);
        entry.setNames("root", "root");

        pOutput.putNextEntry(entry);
        pOutput.write(data);
        pOutput.closeEntry();       
    }


}
