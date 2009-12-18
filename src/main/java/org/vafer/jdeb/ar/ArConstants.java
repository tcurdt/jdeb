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
package org.vafer.jdeb.ar;

/**
 * TODO: To be replace by commons compress
 * 
 * @author Emmanuel Bourg
 */
interface ArConstants {

    static final byte[] HEADER = "!<arch>\n".getBytes();

    static final byte[] ENTRY_TERMINATOR = "`\012".getBytes();

    static final int FIELD_SIZE_NAME = 16;
    static final int FIELD_SIZE_LASTMODIFIED = 12;
    static final int FIELD_SIZE_UID = 6;
    static final int FIELD_SIZE_GID = 6;
    static final int FIELD_SIZE_MODE = 8;
    static final int FIELD_SIZE_LENGTH = 10;

    static final int HEADER_SIZE =
            FIELD_SIZE_NAME
            + FIELD_SIZE_LASTMODIFIED
            + FIELD_SIZE_UID
            + FIELD_SIZE_GID
            + FIELD_SIZE_MODE
            + FIELD_SIZE_LENGTH
            + ENTRY_TERMINATOR.length;

}
