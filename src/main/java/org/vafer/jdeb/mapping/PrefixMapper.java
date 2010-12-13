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
package org.vafer.jdeb.mapping;

import org.apache.tools.tar.TarEntry;
import org.vafer.jdeb.utils.Utils;

/**
 * Just adds a prefix to the entry coming in
 *
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public class PrefixMapper implements Mapper {

    protected final int strip;
    protected final String prefix;

    public PrefixMapper( final int pStrip, final String pPrefix ) {
        strip = pStrip;
        prefix = (pPrefix == null) ? "" : pPrefix;
    }

    public TarEntry map( final TarEntry pEntry ) {

        final String name = pEntry.getName();

        final TarEntry newEntry = new TarEntry(prefix + '/' + Utils.stripPath(strip, name));

        newEntry.setUserId(pEntry.getUserId());
        newEntry.setGroupId(pEntry.getGroupId());
        newEntry.setUserName(pEntry.getUserName());
        newEntry.setGroupName(pEntry.getGroupName());
        newEntry.setMode(pEntry.getMode());
        newEntry.setSize(pEntry.getSize());

        return newEntry;
    }

}
