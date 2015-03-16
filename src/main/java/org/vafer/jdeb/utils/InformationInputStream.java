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

package org.vafer.jdeb.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class InformationInputStream extends FilterInputStream {

    private long i;
    private long ascii;
    private long nonascii;
    private long cr;
    private long lf;
    private long zero;

    private final Map<BOM, Integer> bomPositions = new HashMap<BOM, Integer>();
    private final Map<Shell, Integer> shellPositions = new HashMap<Shell, Integer>();

    /**
     * Byte Order Marks
     */
    private enum BOM {
        NONE(null),
        UTF8("UTF-8", 0xEF, 0xBB, 0xBF),
        UTF16LE("UTF-16LE", 0xFF, 0xFE),
        UTF16BE("UTF-16BE", 0xFE, 0xFF);

        int[] sequence;
        String encoding;

        private BOM( String encoding, int... sequence ) {
            this.encoding = encoding;
            this.sequence = sequence;
        }
    }

    /**
     * Shebang for shell scripts in various encodings.
     */
    private enum Shell {
        NONE,
        ASCII(0x23, 0x21),
        UTF16BE(0x00, 0x23, 0x00, 0x21),
        UTF16LE(0x23, 0x00, 0x21, 0x00);

        int[] header;

        private Shell( int... header ) {
            this.header = header;
        }
    }

    private BOM bom = BOM.NONE;
    private Shell shell = Shell.NONE;

    public InformationInputStream( InputStream in ) {
        super(in);
    }

    public boolean hasBom() {
        return bom != BOM.NONE;
    }

    public boolean isShell() {
        return shell != Shell.NONE;
    }

    public boolean hasUnixLineEndings() {
        return cr == 0;
    }

    public String getEncoding() {
        String encoding = bom.encoding;

        if (encoding == null) {
            // guess the encoding from the shebang
            if (shell == Shell.UTF16BE) {
                encoding = BOM.UTF16BE.encoding;
            } else if (shell == Shell.UTF16LE) {
                encoding = BOM.UTF16LE.encoding;
            }
        }

        return encoding;
    }

    private void add( int c ) {
        if (i < 10) {
            if (shell == Shell.NONE) {
                for (Shell shell : Shell.values()) {
                    int position = shellPositions.containsKey(shell) ? shellPositions.get(shell) : 0;
                    if (position < shell.header.length) {
                        if (c == shell.header[position]) {
                            shellPositions.put(shell, position + 1);
                        } else {
                            shellPositions.put(shell, 0);
                        }
                    } else {
                        this.shell = shell;
                    }
                }
            }

            if (bom == BOM.NONE) {
                for (BOM bom : BOM.values()) {
                    int position = bomPositions.containsKey(bom) ? bomPositions.get(bom) : 0;
                    if (position < bom.sequence.length) {
                        if (c == bom.sequence[position] && position == i) {
                            bomPositions.put(bom, position + 1);
                        } else {
                            bomPositions.put(bom, 0);
                        }
                    } else {
                        this.bom = bom;
                    }
                }
            }
        }

        i++;

        if (c == '\n') {
            lf++;
            return;
        }
        if (c == '\r') {
            cr++;
            return;
        }
        if (c >= ' ' && c <= '~') {
            ascii++;
            return;
        }
        if (c == 0) {
            zero++;
            return;
        }
        nonascii++;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            add(b & 0xFF);
        }
        return b;
    }

    @Override
    public int read( byte[] b, int off, int len ) throws IOException {
        int length = super.read(b, off, len);
        for (int i = 0; i < length; i++) {
            add(b[off + i] & 0xFF);
        }
        return length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("total=").append(i);
        sb.append(",noascii=").append(nonascii);
        sb.append(",ascii=").append(ascii);
        sb.append(",cr=").append(cr);
        sb.append(",lf=").append(lf);
        sb.append(",zero=").append(zero);
        sb.append("}");
        return sb.toString();
    }
}
