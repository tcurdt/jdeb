/*
 * Copyright 2012 The Apache Software Foundation.
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

import java.io.IOException;
import java.io.InputStream;

public final class InformationInputStream extends InputStream {

    private final InputStream inputStream;

    private final char[] shell_utf8 = { 0x23, 0x21 };
    private final char[] shell_utf16be = { 0x00, 0x23, 0x00, 0x21 };
    private final char[] shell_utf16le = { 0x23, 0x00, 0x21, 0x00 };
    private final char[] bom1 = { 0xEF, 0xBB, 0xBF };
    private final char[] bom2 = { 0xFF, 0xFE };
    private final char[] bom3 = { 0xFE, 0xFF };

    private int shell_utf8_i;
    private int shell_utf16be_i;
    private int shell_utf16le_i;
    private int bom1_i;
    private int bom2_i;
    private int bom3_i;

    private long i;
    private long ascii;
    private long nonascii;
    private long cr;
    private long lf;
    private long zero;

    private enum BOM { NONE, BOM1, BOM2, BOM3 };
    private enum SHELL { NONE, UTF8, UTF16BE, UTF16LE };

    private BOM bom = BOM.NONE;
    private SHELL shell = SHELL.NONE;

    public InformationInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public boolean hasBom() {
        return bom != BOM.NONE;
    }

    public boolean isShell() {
        return shell != SHELL.NONE;
    }
    public boolean hasUnixLineEndings() {
        return cr == 0;
    }

    private void add(int c) {
        // System.out.println(i + ": " + Integer.toHexString(c));
        if (i < 10) {
            // FIXME refactor to make this DRY
            if (shell == SHELL.NONE) {
                if (shell_utf8_i < shell_utf8.length) {
                    if (c == shell_utf8[shell_utf8_i]) {
                        shell_utf8_i++;
                    } else {
                        shell_utf8_i = 0;
                    }
                } else {
                    // System.out.println("utf8 shell at " + (i-shell_utf8.length));
                    shell = SHELL.UTF8;
                }

                if (shell_utf16be_i < shell_utf16be.length) {
                    if (c == shell_utf16be[shell_utf16be_i]) {
                        shell_utf16be_i++;
                    } else {
                        shell_utf16be_i = 0;
                    }
                } else {
                    // System.out.println("utf16be shell at " + (i-shell_utf16be.length));
                    shell = SHELL.UTF16BE;
                }

                if (shell_utf16le_i < shell_utf16le.length) {
                    if (c == shell_utf16le[shell_utf16le_i]) {
                        shell_utf16le_i++;
                    } else {
                        shell_utf16le_i = 0;
                    }
                } else {
                    // System.out.println("utf16le shell at " + (i-shell_utf16le.length));
                    shell = SHELL.UTF16LE;
                }
            }
            if (bom == BOM.NONE) {
                if (bom1_i < bom1.length) {
                    if (c == bom1[bom1_i] && bom1_i == i) {
                        bom1_i++;
                    } else {
                        bom1_i = 0;
                    }
                } else {
                    // System.out.println("bom1 at " + (i-bom1.length));
                    bom = BOM.BOM1;
                }

                if (bom2_i < bom2.length) {
                    if (c == bom2[bom2_i] && bom2_i == i) {
                        bom2_i++;
                    } else {
                        bom2_i = 0;
                    }
                } else {
                    // System.out.println("bom2 at " + (i-bom2.length));
                    bom = BOM.BOM2;
                }

                if (bom3_i < bom3.length) {
                    if (c == bom3[bom3_i] && bom3_i == i) {
                        bom3_i++;
                    } else {
                        bom3_i = 0;
                    }
                } else {
                    // System.out.println("bom3 at " + (i-bom3.length));
                    bom = BOM.BOM3;
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

    public int available() throws IOException {
        return inputStream.available();
    }

    public void close() throws IOException {
        inputStream.close();
    }

    public void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    public boolean markSupported() {
        return inputStream.markSupported();
    }

    public int read() throws IOException {
        int ret = inputStream.read();
        if (ret != -1) {
            add(ret & 0xFF);
        }
        return ret;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int ret = inputStream.read(b, off, len);
        for(int i = 0; i<ret; i++) {
            add(b[off+i] & 0xFF);
        }
        return ret;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public void reset() throws IOException {
        inputStream.reset();
    }

    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

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
