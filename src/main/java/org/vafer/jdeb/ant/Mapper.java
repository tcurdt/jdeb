/*
 * Copyright 2010 The Apache Software Foundation.
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

import org.vafer.jdeb.mapping.LsMapper;
import org.vafer.jdeb.mapping.NullMapper;
import org.vafer.jdeb.mapping.PermMapper;
import org.vafer.jdeb.mapping.PrefixMapper;

/**
 * Ant "mapper" element acting as factory for the entry mapper.
 * Supported types: ls, prefix, perm
 *
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public final class Mapper {

    private String mapperType;
    private File src;

    private String prefix;
    private int strip;
    private int uid = -1;
    private int gid = -1;
    private String user;
    private String group;
    private int fileMode = -1;
    private int dirMode = -1;

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

    public void setFileMode( final int pFileMode ) {
        fileMode = pFileMode;
    }

    public void setDirMode(int pDirMode) {
        dirMode = pDirMode;
    }

    public org.vafer.jdeb.mapping.Mapper createMapper() {

        if ("perm".equalsIgnoreCase(mapperType)) {
            return new PermMapper(uid, gid, user, group, fileMode, dirMode, strip, prefix);
        }

        if ("prefix".equalsIgnoreCase(mapperType)) {
            return new PrefixMapper(strip, prefix);
        }

        if ("ls".equalsIgnoreCase(mapperType)) {
            try {
                return new LsMapper(new FileInputStream(src));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new NullMapper();
    }

}
