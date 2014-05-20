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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.tools.ant.DirectoryScanner;
import org.vafer.jdeb.mapping.PermMapper;
import org.vafer.jdeb.producers.DataProducerDirectory;
import org.vafer.jdeb.utils.FilteredFile;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * Builds the scripts directory of the Synology package.
 */
class SpkScriptsBuilder {
    
    /** The name of the package maintainer scripts */
    private static final Set<String> MAINTAINER_SCRIPTS = new HashSet<String>(Arrays.asList("postinst", "postuninst", "postupgrade", "preinst", "preuninst", "preupgrade", "start-stop-status"));
    
    private Console console;
    private VariableResolver resolver;
    private final String openReplaceToken;
    private final String closeReplaceToken;

    SpkScriptsBuilder(Console console, VariableResolver resolver, String openReplaceToken, String closeReplaceToken) {
        this.console = console;
        this.resolver = resolver;
        this.openReplaceToken = openReplaceToken;
        this.closeReplaceToken = closeReplaceToken;
    }
    
    protected final void addDirectoryTo(final TarArchiveOutputStream pOutput, final String pName, final File pDir) throws IOException {
    	if (pOutput == null || pDir == null || pName == null) {
    		return;
    	}
    	
    	final String pDirName;
    	if (pName.endsWith("/")) {
    		pDirName = pName;
    	} else {
    		pDirName = pName + "/";
    	}
    	
    	pOutput.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
    	
    	final TarArchiveEntry entry = new TarArchiveEntry(pDirName, true);
        entry.setNames("root", "root");
        entry.setMode(PermMapper.toMode("644"));
        
        pOutput.putArchiveEntry(entry);
        pOutput.closeArchiveEntry();
    	
        DataProducerDirectory dirProducer = new DataProducerDirectory(pDir, null, null, null);
    	DataConsumer dirConsumer = new DataConsumer() {
			@Override
			public void onEachLink(final String path, final String linkName, final boolean symlink, final String user, final int uid, final String group, final int gid, final int mode) throws IOException {
				//
			}
			
			@Override
			public void onEachFile(final InputStream input, final String filename, final String linkname, final String user, final int uid, final String group, final int gid, final int mode, final long size) throws IOException {
				FilteredFile configurationFile = new FilteredFile(input, resolver);
                configurationFile.setOpenToken(openReplaceToken);
                configurationFile.setCloseToken(closeReplaceToken);
				
                addScriptsEntry(pOutput, pName, filename, configurationFile.toString().getBytes("UTF-8"), user, uid, group, gid, mode);
			}
			
			@Override
			public void onEachDir(final String dirname, final String linkname, final String user, final int uid, final String group, final int gid, final int mode, final long size) throws IOException {
		        //
			}
		};
		dirProducer.produce(dirConsumer);
    }
    
    private static void addScriptsEntry(final TarArchiveOutputStream pOutput, final String pDir, final String pName, final String pContent, final String user, final int uid, final String group, final int gid, final int mode) throws IOException {
        final byte[] content = pContent.getBytes("UTF-8");
        addScriptsEntry(pOutput, pDir, pName, content, user, uid, group, gid, mode);
    }

    private static void addScriptsEntry(final TarArchiveOutputStream pOutput, final String pDir, final String pName, final byte[] pContent, final String user, final int uid, final String group, final int gid, final int mode) throws IOException {
    	final String pDirName;
    	if (pDir.endsWith("/")) {
    		pDirName = pDir;
    	} else {
    		pDirName = pDir + "/";
    	}
    	
        final TarArchiveEntry entry = new TarArchiveEntry(pDirName + pName, true);
        entry.setSize(pContent.length);
        entry.setNames("root", "root");
        
        if (MAINTAINER_SCRIPTS.contains(pName)) {
            entry.setMode(PermMapper.toMode("755"));
        } else {
            entry.setMode(PermMapper.toMode("644"));
        }
        
        pOutput.putArchiveEntry(entry);
        pOutput.write(pContent);
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
