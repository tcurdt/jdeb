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

    // ---- toMode tests ----

    @Test
    public void testToModeWithValidOctalString() {
        assertEquals(0644, PermMapper.toMode("644"));
    }

    @Test
    public void testToModeWithNullReturnsMinusOne() {
        assertEquals(-1, PermMapper.toMode(null));
    }

    @Test
    public void testToModeWithEmptyStringReturnsMinusOne() {
        assertEquals(-1, PermMapper.toMode(""));
    }

    @Test
    public void testToModeWithFullPermissionString() {
        assertEquals(0755, PermMapper.toMode("755"));
    }

    // ---- String constructor tests (delegates to toMode) ----

    @Test
    public void testStringConstructorWithValidModes() {
        PermMapper mapper = new PermMapper(1000, 1000, "user", "group", "644", "755", 0, null);
        TarArchiveEntry fileEntry = new TarArchiveEntry("file.txt", false);
        TarArchiveEntry mapped = mapper.map(fileEntry);
        assertEquals(0644, mapped.getMode());
    }

    @Test
    public void testStringConstructorWithNullModes() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, null, null, 0, null);
        TarArchiveEntry fileEntry = new TarArchiveEntry("file.txt", false);
        fileEntry.setMode(0100644);
        TarArchiveEntry mapped = mapper.map(fileEntry);
        assertEquals(0100644, mapped.getMode());
    }

    // ---- Ownership tests (uid, gid, user, group) ----

    @Test
    public void testMapSetsUserId() {
        PermMapper mapper = new PermMapper(1000, -1, null, null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        assertEquals(1000, entry.getUserId());
    }

    @Test
    public void testMapSetsGroupId() {
        PermMapper mapper = new PermMapper(-1, 1000, null, null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        assertEquals(1000, entry.getGroupId());
    }

    @Test
    public void testMapSetsUserName() {
        PermMapper mapper = new PermMapper(-1, -1, "testuser", null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        assertEquals("testuser", entry.getUserName());
    }

    @Test
    public void testMapSetsGroupName() {
        PermMapper mapper = new PermMapper(-1, -1, null, "testgroup", -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        assertEquals("testgroup", entry.getGroupName());
    }

    @Test
    public void testMapSetsAllOwnership() {
        PermMapper mapper = new PermMapper(1000, 1000, "user", "group", -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        assertEquals(1000, entry.getUserId());
        assertEquals(1000, entry.getGroupId());
        assertEquals("user", entry.getUserName());
        assertEquals("group", entry.getGroupName());
    }

    // ---- Permission mode tests (fileMode, dirMode) ----

    @Test
    public void testMapSetsFileMode() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, 0644, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        assertEquals(0644, entry.getMode());
    }

    @Test
    public void testMapSetsDirMode() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, 0755, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("dir/", true);
        mapper.map(entry);
        assertEquals(0755, entry.getMode());
    }

    @Test
    public void testMapDoesNotSetDirModeOnFile() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, 0755, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        entry.setMode(0);
        mapper.map(entry);
        assertEquals(0, entry.getMode());
    }

    @Test
    public void testMapDoesNotSetFileModeOnDir() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, 0644, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("dir/", true);
        entry.setMode(0);
        mapper.map(entry);
        assertEquals(0, entry.getMode());
    }

    // ---- Boundary tests: uid=0 and gid=0 must still apply (uid > -1, gid > -1) ----

    @Test
    public void testMapWithUidZeroSetsUserId() {
        PermMapper mapper = new PermMapper(0, -1, null, null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        assertEquals(0, entry.getUserId());
    }

    @Test
    public void testMapWithGidZeroSetsGroupId() {
        PermMapper mapper = new PermMapper(-1, 0, null, null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        assertEquals(0, entry.getGroupId());
    }

    // ---- Boundary tests: fileMode=0 and dirMode=0 must still apply (mode > -1) ----

    @Test
    public void testMapWithFileModeZeroSetsMode() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, 0, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        assertEquals(0, entry.getMode());
    }

    @Test
    public void testMapWithDirModeZeroSetsMode() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, 0, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("dir/", true);
        mapper.map(entry);
        assertEquals(0, entry.getMode());
    }

    // ---- Prefix tests ----

    @Test
    public void testMapWithPrefix() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, -1, 0, "opt/app");
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        assertEquals("opt/app/file.txt", entry.getName());
    }

    @Test
    public void testMapWithPrefixAndAbsolutePath() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, -1, 0, "opt/app");
        TarArchiveEntry entry = new TarArchiveEntry("/file.txt", false);
        mapper.map(entry);
        assertEquals("opt/app/file.txt", entry.getName());
    }

    // ---- Strip tests ----

    @Test
    public void testMapWithStrip() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, -1, 2, null);
        TarArchiveEntry entry = new TarArchiveEntry("a/b/c/file.txt", false);
        mapper.map(entry);
        assertEquals("c/file.txt", entry.getName());
    }

    @Test
    public void testMapWithStripAndPrefix() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, -1, 1, "opt/app");
        TarArchiveEntry entry = new TarArchiveEntry("a/b/file.txt", false);
        mapper.map(entry);
        assertEquals("opt/app/b/file.txt", entry.getName());
    }

    // ---- Ownership not set when uid/gid are -1 ----

    @Test
    public void testMapDoesNotSetUserIdWhenUidIsMinusOne() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        entry.setUserId(999);
        mapper.map(entry);
        assertEquals(999, entry.getUserId());
    }

    @Test
    public void testMapDoesNotSetGroupIdWhenGidIsMinusOne() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        entry.setGroupId(999);
        mapper.map(entry);
        assertEquals(999, entry.getGroupId());
    }

    // ---- Full integration test ----

    @Test
    public void testMapWithAllParameters() {
        PermMapper mapper = new PermMapper(1000, 1000, "user", "group", 0644, 0755, 1, "opt/app");
        TarArchiveEntry fileEntry = new TarArchiveEntry("a/file.txt", false);
        mapper.map(fileEntry);
        assertEquals("opt/app/file.txt", fileEntry.getName());
        assertEquals(1000, fileEntry.getUserId());
        assertEquals(1000, fileEntry.getGroupId());
        assertEquals("user", fileEntry.getUserName());
        assertEquals("group", fileEntry.getGroupName());
        assertEquals(0644, fileEntry.getMode());

        TarArchiveEntry dirEntry = new TarArchiveEntry("a/dir/", true);
        mapper.map(dirEntry);
        assertEquals("opt/app/dir/", dirEntry.getName());
        assertEquals(0755, dirEntry.getMode());
    }

    // ---- Kill prefix null-check mutant (line 45: prefix == null) ----

    @Test
    public void testMapWithNullPrefixDoesNotPrefixWithNull() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        // prefix is null, so entry name should NOT start with "null"
        assertEquals("file.txt", entry.getName());
    }

    // ---- Kill uid/gid boundary mutants (lines 65, 68: uid > -1, gid > -1) ----
    // When uid=0 and condition is mutated to uid > 0, uid should still be set.
    // We pre-set a different userId so the mutation is detectable.

    @Test
    public void testMapWithUidZeroOverridesExistingUserId() {
        PermMapper mapper = new PermMapper(0, -1, null, null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        entry.setUserId(9999);
        mapper.map(entry);
        assertEquals(0, entry.getUserId());
    }

    @Test
    public void testMapWithGidZeroOverridesExistingGroupId() {
        PermMapper mapper = new PermMapper(-1, 0, null, null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        entry.setGroupId(9999);
        mapper.map(entry);
        assertEquals(0, entry.getGroupId());
    }

    // ---- Kill user/group null-check mutants (lines 71, 74: user != null, group != null) ----
    // When user is null and condition is mutated to always-true, setUserName(null) would be called.
    // We pre-set a userName so the mutation is detectable.

    @Test
    public void testMapWithNullUserPreservesExistingUserName() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        entry.setUserName("existingUser");
        mapper.map(entry);
        assertEquals("existingUser", entry.getUserName());
    }

    @Test
    public void testMapWithNullGroupPreservesExistingGroupName() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, -1, -1, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        entry.setGroupName("existingGroup");
        mapper.map(entry);
        assertEquals("existingGroup", entry.getGroupName());
    }

    // ---- Kill toMode call removal mutant (line 55) ----
    // If toMode call is removed, fileMode would be 0 instead of the parsed value.

    @Test
    public void testStringConstructorParsesFileModeCorrectly() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, "777", null, 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("file.txt", false);
        mapper.map(entry);
        assertEquals(0777, entry.getMode());
    }

    @Test
    public void testStringConstructorParsesDirModeCorrectly() {
        PermMapper mapper = new PermMapper(-1, -1, null, null, null, "700", 0, null);
        TarArchiveEntry entry = new TarArchiveEntry("dir/", true);
        mapper.map(entry);
        assertEquals(0700, entry.getMode());
    }
}
