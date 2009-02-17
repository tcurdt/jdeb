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
package org.vafer.jdeb.ar;

/**
 * To be replace by commons compress once released
 * 
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public final class ArEntry {

    private final String name;
    private int userId;
    private int groupId;
    private int mode;
    private long lastModified;
    private long length;

    public ArEntry(String name, long length) {
        this(name, length, 0, 0, 33188, System.currentTimeMillis());
    }

    public ArEntry(String name, long length, int userId, int groupId, int mode, long lastModified) {
        this.name = name;
        this.length = length;
        this.userId = userId;
        this.groupId = groupId;
        this.mode = mode;
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public int getUserId() {
        return userId;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getMode() {
        return mode;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getLength() {
        return length;
    }
}
