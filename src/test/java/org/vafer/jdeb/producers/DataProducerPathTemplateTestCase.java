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
package org.vafer.jdeb.producers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;

public class DataProducerPathTemplateTestCase extends TestCase {

    private static final String[] INCLUDES = { };
    private static final String[] EXCLUDES = { };

    private CaptureDataConsumer captureDataConsumer = new CaptureDataConsumer();

    private Mapper[] mappers = new Mapper[0];
    private DataProducer dataProducer;

    public void testTypical() throws Exception {

        String[] paths = { "/var/log", "/var/lib" };
        dataProducer = new DataProducerPathTemplate(paths, INCLUDES, EXCLUDES, mappers);
        dataProducer.produce(captureDataConsumer);

        assertEquals(2, captureDataConsumer.invocations.size());

        CaptureDataConsumer.Invocation invocation = captureDataConsumer.invocations.get(0);
        assertEquals(invocation.dirname, "/var/log");
        assertEquals(invocation.gid, 0);
        assertEquals(invocation.group, "root");
        assertEquals(invocation.linkname, "");
        assertEquals(invocation.mode, TarArchiveEntry.DEFAULT_DIR_MODE);
        assertEquals(invocation.size, 0);
        assertEquals(invocation.uid, 0);
        assertEquals(invocation.user, "root");

        invocation = captureDataConsumer.invocations.get(1);
        assertEquals(invocation.dirname, "/var/lib");
        assertEquals(invocation.gid, 0);
        assertEquals(invocation.group, "root");
        assertEquals(invocation.linkname, "");
        assertEquals(invocation.mode, TarArchiveEntry.DEFAULT_DIR_MODE);
        assertEquals(invocation.size, 0);
        assertEquals(invocation.uid, 0);
        assertEquals(invocation.user, "root");
    }

    public class CaptureDataConsumer implements DataConsumer {

        private List<Invocation> invocations;

        public CaptureDataConsumer() {
            invocations = new ArrayList<Invocation>();
        }

        @Override
        public void onEachDir( String dirname, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {
            invocations.add(new Invocation(dirname, linkname, user, uid, group, gid, mode, size));
        }

        @Override
        public void onEachFile( InputStream input, String filename, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {
        }

        @Override
        public void onEachLink(String path, String linkName, boolean symlink, String user, int uid, String group, int gid, int mode) throws IOException {
        }

        private class Invocation {

            private String dirname;
            private String linkname;
            private String user;
            private int uid;
            private String group;
            private int gid;
            private int mode;
            private long size;

            private Invocation( String dirname, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {
                this.dirname = dirname;
                this.linkname = linkname;
                this.user = user;
                this.uid = uid;
                this.group = group;
                this.gid = gid;
                this.mode = mode;
                this.size = size;
            }

        }

    }
}
