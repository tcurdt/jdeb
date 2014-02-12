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

import java.io.IOException;
import java.io.InputStream;

/**
 * A DataConsumer consumes Data produced from a producer.
 *
 * @author Torsten Curdt
 */
public interface DataConsumer {

    void onEachDir( String dirname, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException;

    void onEachFile( InputStream input, String filename, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException;
    
    void onEachLink( String path, String linkName, boolean symlink, String user, int uid, String group, int gid, int mode) throws IOException;

}
