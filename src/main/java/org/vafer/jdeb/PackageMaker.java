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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.vafer.jdeb.producers.DataProducerDirectory;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * A abstract class for creating archives.
 *
 * @author Torsten Curdt
 * @author Bryan Sant
 */
public abstract class PackageMaker {

    /** A console to output log message with */
    protected Console console;

    /** The compression method used for the data file (none, gzip, bzip2 or xz) */
    protected String compression = "gzip";

    protected VariableResolver variableResolver;
    protected String openReplaceToken;
    protected String closeReplaceToken;

    protected final Collection<DataProducer> dataProducers = new ArrayList<DataProducer>();

    public PackageMaker(final Console console, final Collection<DataProducer> dataProducers) {
        this.console = console;
        if (dataProducers != null) {
            this.dataProducers.addAll(dataProducers);
        }
    }

    public final void setCompression(final String compression) {
        this.compression = compression;
    }

    public final void setResolver(final VariableResolver variableResolver) {
        this.variableResolver = variableResolver;
    }

    protected final boolean isWritableFile(final File file) {
        return !file.exists() || file.isFile() && file.canWrite();
    }

    protected final void addTo(final ArArchiveOutputStream pOutput, final String pName, final String pContent) throws IOException {
        final byte[] content = pContent.getBytes();
        pOutput.putArchiveEntry(new ArArchiveEntry(pName, content.length));
        pOutput.write(content);
        pOutput.closeArchiveEntry();
    }

    protected final void addTo(final ArArchiveOutputStream pOutput, final String pName, final File pContent) throws IOException {
        pOutput.putArchiveEntry(new ArArchiveEntry(pName, pContent.length()));

        final InputStream input = new FileInputStream(pContent);
        try {
            Utils.copy(input, pOutput);
        } finally {
            input.close();
        }

        pOutput.closeArchiveEntry();
    }

    protected final void addTo(final TarArchiveOutputStream pOutput, final String pName, final String pContent, final String user, final int uid, final String group, final int gid, final int mode) throws IOException {
        final byte[] content = pContent.getBytes();
        addTo(pOutput, pName, content, user, uid, group, gid, mode);
    }

    protected final void addTo(final TarArchiveOutputStream pOutput, final String pName, final byte[] pContent, final String user, final int uid, final String group, final int gid, final int mode) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(pName);
        entry.setSize(pContent.length);
        entry.setUserName(user);
        entry.setUserId(uid);
        entry.setGroupName(group);
        entry.setGroupId(gid);
        entry.setMode(mode);
        
        pOutput.putArchiveEntry(entry);
        pOutput.write(pContent);
        pOutput.closeArchiveEntry();
    }

    protected final void addTo(final TarArchiveOutputStream pOutput, final String pName, final File pContent, final String user, final int uid, final String group, final int gid, final int mode) throws IOException {
    	TarArchiveEntry entry = new TarArchiveEntry(pName);
        entry.setSize(pContent.length());
        entry.setUserName(user);
        entry.setUserId(uid);
        entry.setGroupName(group);
        entry.setGroupId(gid);
        entry.setMode(mode);
        
        pOutput.putArchiveEntry(entry);

        final InputStream input = new FileInputStream(pContent);
        try {
            Utils.copy(input, pOutput);
        } finally {
            input.close();
        }
        pOutput.closeArchiveEntry();
    }

    protected final void addDirectoryTo(final ArArchiveOutputStream pOutput, final String pDirName, final File pDir) throws IOException {
    	if (pOutput == null || pDir == null || pDirName == null) {
    		return;
    	}
    	
    	final String pDirPrefix;
    	if (pDirName.endsWith("/")) {
    		pDirPrefix = pDirName;
    	} else {
    		pDirPrefix = pDirName + "/";
    	}
    	
        DataProducerDirectory dirProducer = new DataProducerDirectory(pDir, null, null, null);
    	DataConsumer dirConsumer = new DataConsumer() {
			@Override
			public void onEachLink(final String path, final String linkName, final boolean symlink, final String user, final int uid, final String group, final int gid, final int mode) throws IOException {
				
			}
			
			@Override
			public void onEachFile(final InputStream input, final String filename, final String linkname, final String user, final int uid, final String group, final int gid, final int mode, final long size) throws IOException {
				pOutput.putArchiveEntry(new ArArchiveEntry(pDirPrefix + filename, size));
		        try {
		            Utils.copy(input, pOutput);
		        } finally {
		            input.close();
		        }
		        pOutput.closeArchiveEntry();
			}
			
			@Override
			public void onEachDir(final String dirname, final String linkname, final String user, final int uid, final String group, final int gid, final int mode, final long size) throws IOException {
				//
			}
		};
		dirProducer.produce(dirConsumer);
    }

    public final void setOpenReplaceToken(final String openReplaceToken) {
        this.openReplaceToken = openReplaceToken;
    }

    public final void setCloseReplaceToken(final String closeReplaceToken) {
        this.closeReplaceToken = closeReplaceToken;
    }
}
