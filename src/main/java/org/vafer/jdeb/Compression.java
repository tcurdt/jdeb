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

package org.vafer.jdeb;

import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

/**
 * Compression method used for the data file.
 */
public enum Compression {

    NONE(""),
    GZIP(".gz"),
    BZIP2(".bz2"),
    XZ(".xz");

    private String extension;

    private Compression(String extension) {
        this.extension = extension;
    }

    /**
     * Returns the extension of the compression method
     */
    public String getExtension() {
        return extension;
    }

    public OutputStream toCompressedOutputStream(OutputStream out) throws CompressorException {
        switch (this) {
            case GZIP:
                return new CompressorStreamFactory().createCompressorOutputStream("gz", out);
            case BZIP2:
                return new CompressorStreamFactory().createCompressorOutputStream("bzip2", out);
            case XZ:
                return new CompressorStreamFactory().createCompressorOutputStream("xz", out);
            default:
                return out;
        }
    }

    /**
     * Returns the compression method corresponding to the specified name.
     * The matching is case insensitive.
     * 
     * @param name the name of the compression method
     * @return the compression method, or null if not recognized
     */
    public static Compression toEnum(String name) {
        if ("gzip".equalsIgnoreCase(name) || "gz".equalsIgnoreCase(name)) {
            return GZIP;
        } else if ("bzip2".equalsIgnoreCase(name) || "bz2".equalsIgnoreCase(name)) {
            return BZIP2;
        } else if ("xz".equalsIgnoreCase(name)) {
            return XZ;
        } else if ("none".equalsIgnoreCase(name)) {
            return NONE;
        } else {
            return null;
        }
    }
}
