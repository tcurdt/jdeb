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

package org.vafer.jdeb.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.maven.plugins.annotations.Parameter;
import org.vafer.jdeb.mapping.LsMapper;
import org.vafer.jdeb.mapping.NullMapper;
import org.vafer.jdeb.mapping.PermMapper;

/**
 * Maven "mapper" element acting as factory for the entry mapper.
 * Supported types: ls, perm
 *
 * @author Bryan Sant
 */
public final class Mapper {

    @Parameter(required = true)
    private String type;

    @Parameter
    private int uid = -1;

    @Parameter
    private int gid = -1;

    @Parameter
    private String user;

    @Parameter
    private String group;

    @Parameter
    private String filemode;

    @Parameter
    private String dirmode;

    @Parameter
    private String prefix;

    @Parameter
    private int strip;

    @Parameter
    private File src;


    public org.vafer.jdeb.mapping.Mapper createMapper() throws IOException {

        if ("ls".equalsIgnoreCase(type)) {
            try {
                return new LsMapper(new FileInputStream(src));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if ("perm".equalsIgnoreCase(type)) {
            return new PermMapper(uid, gid, user, group, filemode, dirmode, strip, prefix);
        }

        /* NullMapper required for DataProducerPathTemplate */
        return NullMapper.INSTANCE;
    }

}
