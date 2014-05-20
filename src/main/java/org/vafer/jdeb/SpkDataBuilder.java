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

import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;

/**
 * Builds the data archive of the synology package.
 */
class SpkDataBuilder extends DataBuilder {

    private static final String PATH_PREFIX = "";

    SpkDataBuilder(final Console console) {
        this.console = console;
        this.encoding = ZipEncodingHelper.getZipEncoding(null);
    }

    @Override
    protected String fixPath(String path) {
        if (path == null || path.equals(".")) {
            return path;
        }
        
        // If we're receiving directory names from Windows, then we'll convert to use slash
        // This does eliminate the ability to use of a backslash in a directory name on *NIX,
        // but in practice, this is a non-issue
        if (path.contains("\\")) {
            path = path.replace('\\', '/');
        }
        
        // ensure the path is like : foo/bar
        if (path.startsWith("/")) {
        	path = path.substring(1);
        }
        /*
        if (!path.startsWith(PATH_PREFIX)) {
        	path = PATH_PREFIX + path;
        }
        */
        
        if (path.contains("//")) {
            path = path.replace("//", "/");
        }
        
        return path;
    }

    @Override
    protected String getPathPrefix() {
    	return PATH_PREFIX;
    }

}
