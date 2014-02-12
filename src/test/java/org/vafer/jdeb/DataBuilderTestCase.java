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

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import junit.framework.TestCase;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.vafer.jdeb.producers.DataProducerFile;
import org.vafer.jdeb.producers.DataProducerFileSet;

public class DataBuilderTestCase extends TestCase {
    
    /**
     * Checks if the file paths in the md5sums file use only unix file separators
     * (this test can only fail on Windows)
     */
    public void testBuildDataWithFileSet() throws Exception {
        DataBuilder builder = new DataBuilder(new NullConsole());

        Project project = new Project();
        project.setCoreLoader(getClass().getClassLoader());
        project.init();

        FileSet fileset = new FileSet();
        fileset.setDir(new File(getClass().getResource("deb/data").toURI()));
        fileset.setIncludes("**/*");
        fileset.setProject(project);

        StringBuilder md5s = new StringBuilder();
        builder.buildData(Arrays.asList((DataProducer) new DataProducerFileSet(fileset)), new File("target/data.tar"), md5s, Compression.GZIP);

        assertTrue("empty md5 file", md5s.length() > 0);
        assertFalse("windows path separator found", md5s.indexOf("\\") != -1);
    }
    
    public void testCreateParentDirectories() throws Exception {
        File archive = new File("target/data.tar");
        if (archive.exists()) {
            archive.delete();
        }
        
        DataBuilder builder = new DataBuilder(new NullConsole());
        
        DataProducer producer = new DataProducerFile(new File("pom.xml"), "/usr/share/myapp/pom.xml", null, null, null); 
        
        builder.buildData(Arrays.asList(producer), archive, new StringBuilder(), Compression.NONE);
        
        int count = 0;
        TarArchiveInputStream in = null;
        try {
            in = new TarArchiveInputStream(new FileInputStream(archive));
            while (in.getNextTarEntry() != null) {
                count++;
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        
        assertEquals("entries", 4, count);
    }
}
