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
package org.vafer.jdeb.mapping;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.vafer.jdeb.utils.Utils;

/**
 * Applies a uniform set of permissions and ownership to all entries.
 */
public final class PermMapper implements Mapper {

    private final int strip;
    private final String prefix;
    private int uid = -1;
    private int gid = -1;
    private String user;
    private String group;
    private int fileMode = -1;
    private int dirMode = -1;

    public static int toMode( String modeString ) {
        int mode = -1;
        if (modeString != null && modeString.length() > 0) {
            mode = Integer.parseInt(modeString, 8);
        }
        return mode;
    }

    public PermMapper( int uid, int gid, String user, String group, int fileMode, int dirMode, int strip, String prefix ) {
        this.strip = strip;
        this.prefix = (prefix == null) ? "" : prefix;
        this.uid = uid;
        this.gid = gid;
        this.user = user;
        this.group = group;
        this.fileMode = fileMode;
        this.dirMode = dirMode;
    }

    public PermMapper( int uid, int gid, String user, String group, String fileMode, String dirMode, int strip, String prefix ) {
        this(uid, gid, user, group, toMode(fileMode), toMode(dirMode), strip, prefix);
    }

    @Override
    public TarArchiveEntry map( final TarArchiveEntry entry ) {
        final String name = entry.getName();
        entry.setName(prefix + '/' + Utils.stripPath(strip, name));

        // Set ownership
        if (uid > -1) {
            entry.setUserId(uid);
        }
        if (gid > -1) {
            entry.setGroupId(gid);
        }
        if (user != null) {
            entry.setUserName(user);
        }
        if (group != null) {
            entry.setGroupName(group);
        }

        // Set permissions
        if (entry.isDirectory()) {
            if (dirMode > -1) {
                entry.setMode(dirMode);
            }
        } else {
            if (fileMode > -1) {
                entry.setMode(fileMode);
            }
        }

        return entry;
    }
}
