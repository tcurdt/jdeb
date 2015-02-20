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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.vafer.jdeb.mapping.LsMapper;
import org.vafer.jdeb.mapping.PermMapper;

/**
 * Ant "mapper" element acting as factory for the entry mapper.
 * Supported types: ls, perm
 */
public final class Mapper {

    private String mapperType = "perm";
    private File src;

    private String prefix;
    private int strip;
    private int uid = -1;
    private int gid = -1;
    private String user;
    private String group;
    private String fileMode;
    private String dirMode;

    public void setType( final String pType ) {
        mapperType = pType;
    }

    public void setSrc( final File pSrc ) {
        src = pSrc;
    }


    public void setPrefix( final String pPrefix ) {
        prefix = pPrefix;
    }

    public void setStrip( final int pStrip ) {
        strip = pStrip;
    }


    public void setUid( final int pUid ) {
        uid = pUid;
    }

    public void setGid( final int pGid ) {
        gid = pGid;
    }

    public void setUser( final String pUser ) {
        user = pUser;
    }

    public void setGroup( final String pGroup ) {
        group = pGroup;
    }

    public void setFileMode( final String pFileMode ) {
        fileMode = pFileMode;
    }

    public void setDirMode( final String pDirMode ) {
        dirMode = pDirMode;
    }

    public org.vafer.jdeb.mapping.Mapper createMapper() throws IOException {

        if ("perm".equalsIgnoreCase(mapperType)) {
            return new PermMapper(uid, gid, user, group, fileMode, dirMode, strip, prefix);
        }

        if ("ls".equalsIgnoreCase(mapperType)) {
            try {
                return new LsMapper(new FileInputStream(src));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        throw new IOException("Unknown mapper type '" + mapperType + "'");
    }

}
