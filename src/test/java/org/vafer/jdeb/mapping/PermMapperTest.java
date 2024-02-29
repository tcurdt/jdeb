/*
 * Copyright 2007-2024 The jdeb developers.
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
import org.junit.Assert;
import org.junit.Test;

public final class PermMapperTest extends Assert {

    @Test
    public void testEntryNameWithRelativePath() {
        PermMapper mapper = createMapper();
        TarArchiveEntry mappedEntry = mapper.map(new TarArchiveEntry("foo/bar", true));
        assertEquals("", "foo/bar", mappedEntry.getName());
    }

    @Test
    public void testEntryNameWithAbsolutePath() {
        PermMapper mapper = createMapper();
        TarArchiveEntry mappedEntry = mapper.map(new TarArchiveEntry("/foo/bar", true));
        assertEquals("", "foo/bar", mappedEntry.getName());
    }

    PermMapper createMapper() {
        return new PermMapper(-1, -1, null, null, null, null, -1, null);
    }
}
