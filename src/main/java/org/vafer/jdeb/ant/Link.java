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

package org.vafer.jdeb.ant;

import org.apache.commons.compress.archivers.zip.UnixStat;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.PermMapper;
import org.vafer.jdeb.producers.DataProducerLink;

/**
 * Defines a symbolic or hard link.
 */
public final class Link {

    private String name;
    private String target;
    private boolean symbolic = true;
    private String username = "root";
    private String group = "root";
    private int uid = 0;
    private int gid = 0;
    private int mode = UnixStat.LINK_FLAG | UnixStat.DEFAULT_LINK_PERM;

    DataProducer toDataProducer() {
        org.vafer.jdeb.mapping.Mapper mapper = new PermMapper(uid, gid, username, group, mode, mode, 0, null);
        return new DataProducerLink(name, target, symbolic, null, null, new org.vafer.jdeb.mapping.Mapper[]{mapper});
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean isSymbolic() {
        return symbolic;
    }

    public void setSymbolic(boolean symbolic) {
        this.symbolic = symbolic;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = UnixStat.LINK_FLAG | Integer.parseInt(mode, 8);
    }
}
