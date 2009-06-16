/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.vafer.jdeb.mapping.LsMapper;
import org.vafer.jdeb.mapping.NullMapper;
import org.vafer.jdeb.mapping.PermMapper;
import org.vafer.jdeb.mapping.PrefixMapper;

/**
 * Maven "mapper" element acting as factory for the entry mapper.
 * So far type "ls" and "prefix" are supported.
 * 
 * @author Bryan Sant <bryan.sant@gmail.com>
 */
public final class Mapper {

    /**
     * @parameter
     * @required
     */
    private String type;
    
    private int uid;
    
    private int gid;

    private String user;

    private String group;

    private String mode;

    /**
     * @parameter
     */
    private String prefix;

    /**
     * @parameter
     */
    private int strip;

    /**
     * @parameter
     */
    private File src;


    public org.vafer.jdeb.mapping.Mapper createMapper() {
        if ("perm".equalsIgnoreCase(type)) {
            return new PermMapper(uid, gid, user, group, mode, strip, prefix);
        } else if ("prefix".equalsIgnoreCase(type)) {
            return new PrefixMapper(strip, prefix);
        } else if ("ls".equalsIgnoreCase(type)) {
            try {
                return new LsMapper(new FileInputStream(src));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new NullMapper();
    }
    
}
