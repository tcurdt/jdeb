/*
 * Copyright 2015 The jdeb developers.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.compressors.CompressorException;
import org.vafer.jdeb.utils.Utils;

/**
 * Builds the data archive of the Debian package.
 */
class DataBuilder {

    private Console console;
    
    private ZipEncoding encoding;
    
    private static final class Total {
        private BigInteger count = BigInteger.valueOf(0);

        public void add( long size ) {
            count = count.add(BigInteger.valueOf(size));
        }

        @Override
        public String toString() {
            return "" + count;
        }
    }

    DataBuilder(Console console) {
        this.console = console;
        this.encoding = ZipEncodingHelper.getZipEncoding(null);
    }

    private void checkField(String name, int length) throws IOException {
        if (name != null) {
            ByteBuffer b = encoding.encode(name);
            if (b.limit() > length) {
                throw new IllegalArgumentException("Field '" + name + "' too long, maximum is " + length);
            }
        }
    }

    /**
     * Build the data archive of the deb from the provided DataProducers
     *
     * @param producers
     * @param output
     * @param checksums
     * @param compression the compression method used for the data file
     * @return
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.IOException
     * @throws org.apache.commons.compress.compressors.CompressorException
     */
    BigInteger buildData(Collection<DataProducer> producers, File output, final StringBuilder checksums, Compression compression) throws NoSuchAlgorithmException, IOException, CompressorException {

        final File dir = output.getParentFile();
        if (dir != null && (!dir.exists() || !dir.isDirectory())) {
            throw new IOException("Cannot write data file at '" + output.getAbsolutePath() + "'");
        }

        final TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(compression.toCompressedOutputStream(new FileOutputStream(output)));
        tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

        final MessageDigest digest = MessageDigest.getInstance("MD5");

        final Total dataSize = new Total();

        final List<String> addedDirectories = new ArrayList<String>();
        final DataConsumer receiver = new DataConsumer() {
            @Override
            public void onEachDir( String dirname, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {
                // Check link name
                checkField(linkname, TarConstants.NAMELEN);
                // Check user name
                checkField(user, TarConstants.UNAMELEN);
                // Check group name
                checkField(group, TarConstants.GNAMELEN);

                dirname = fixPath(dirname);

                createParentDirectories(dirname, user, uid, group, gid);

                // The directory passed in explicitly by the caller also gets the passed-in mode.  (Unlike
                // the parent directories for now.  See related comments at "int mode =" in
                // createParentDirectories, including about a possible bug.)
                createDirectory(dirname, user, uid, group, gid, mode, 0);

                console.debug("dir: " + dirname);
            }

            @Override
            public void onEachFile(InputStream input, TarArchiveEntry entry) throws IOException {
                // Check link name
                checkField(entry.getLinkName(), TarConstants.NAMELEN);
                // Check user name
                checkField(entry.getUserName(), TarConstants.UNAMELEN);
                // Check group name
                checkField(entry.getGroupName(), TarConstants.GNAMELEN);

                entry.setName(fixPath(entry.getName()));

                createParentDirectories(entry.getName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId());

                tarOutputStream.putArchiveEntry(entry);

                dataSize.add(entry.getSize());
                digest.reset();

                Utils.copy(input, new DigestOutputStream(tarOutputStream, digest));

                final String md5 = Utils.toHex(digest.digest());

                tarOutputStream.closeArchiveEntry();

                console.debug(
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

                // append to file md5 list, two spaces to be compatible with GNU coreutils md5sum
                checksums.append(md5).append("  ").append(entry.getName()).append('\n');
            }

            @Override
            public void onEachLink(TarArchiveEntry entry) throws IOException {
                // Check link name
                checkField(entry.getLinkName(), TarConstants.NAMELEN);
                // Check user name
                checkField(entry.getUserName(), TarConstants.UNAMELEN);
                // Check group name
                checkField(entry.getGroupName(), TarConstants.GNAMELEN);

                entry.setName(fixPath(entry.getName()));

                createParentDirectories(entry.getName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId());

                tarOutputStream.putArchiveEntry(entry);
                tarOutputStream.closeArchiveEntry();

                console.debug(
                    "link:" + entry.getName() +
                    " mode:" + entry.getMode() +
                    " linkname:" + entry.getLinkName() +
                    " username:" + entry.getUserName() +
                    " userid:" + entry.getUserId() +
                    " groupname:" + entry.getGroupName() +
                    " groupid:" + entry.getGroupId()
                 );
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
        
            private void createParentDirectories( String filename, String user, int uid, String group, int gid ) throws IOException {
                String dirname = fixPath(new File(filename).getParent());
                
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
                String[] pathParts = dirname.split("/");
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
            for (DataProducer data : producers) {
                data.produce(receiver);
            }
        } finally {
            tarOutputStream.close();
        }

        console.debug("Total size: " + dataSize);

        return dataSize.count;
    }

    private String fixPath( String path ) {
        if (path == null || path.equals(".")) {
            return path;
        }
        
        // If we're receiving directory names from Windows, then we'll convert to use slash
        // This does eliminate the ability to use of a backslash in a directory name on *NIX,
        // but in practice, this is a non-issue
        if (path.contains("\\")) {
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

}
