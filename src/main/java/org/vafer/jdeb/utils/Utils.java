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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;

import org.apache.tools.ant.filters.FixCrLfFilter;
import org.apache.tools.ant.util.ReaderInputStream;

/**
 * Simple utils functions.
 *
 * ATTENTION: don't use outside of jdeb
 *
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public final class Utils {

    public static int copy( final InputStream pInput, final OutputStream pOutput ) throws IOException {
        final byte[] buffer = new byte[2048];
        int count = 0;
        int n = 0;
        while (-1 != (n = pInput.read(buffer))) {
                pOutput.write(buffer, 0, n);
                count += n;
        }
        return count;
     }

    public static String toHex( final byte[] pBytes ) {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < pBytes.length; ++i) {
            sb.append(Integer.toHexString((pBytes[i]>>4) & 0x0f));
            sb.append(Integer.toHexString(pBytes[i] & 0x0f));
        }

        return sb.toString();
    }

    public static String stripPath( final int p, final String s ) {

        if (p <= 0) {
            return s;
        }

        int x = 0;
        for (int i=0 ; i<p; i++) {
            x = s.indexOf('/', x+1);
            if (x < 0) {
                return s;
            }
        }

        return s.substring(x+1);
    }

    public static String stripLeadingSlash( final String s ) {
        if (s == null) {
            return s;
        }
        if (s.length() == 0) {
            return s;
        }
        if (s.charAt(0) == '/') {
            return s.substring(1);
        }
        return s;
    }


    /**
     * Substitute the variables in the given expression with the
     * values from the resolver
     *
     * @param pVariables
     * @param pExpression
     * @return
     */
    public static String replaceVariables(final VariableResolver pResolver, final String pExpression, final String pOpen, final String pClose) throws ParseException {

        final char[] s = pExpression.toCharArray();

        final char[] open = pOpen.toCharArray();
        final char[] close = pClose.toCharArray();

        final StringBuilder out = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        char[] watch = open;
        int w = 0;
        for (int i = 0; i < s.length; i++) {
            char c = s[i];

            if (c == watch[w]) {
                w++;
                if (watch.length == w) {
                    // found the full token to watch for

                    if (watch == open) {
                        // found open
                        out.append(sb);
                        sb = new StringBuilder();
                        // search for close
                        watch = close;
                    } else if (watch == close) {
                        // found close
                        final String variable = (String) pResolver.get(sb.toString());
                        if (variable != null) {
                            out.append(variable);
                        } else {
                            throw new ParseException("Failed to resolve variable '" + sb + "'", i);
                        }
                        sb = new StringBuilder();
                        // search for open
                        watch = open;
                    }
                    w = 0;
                }
            } else {

                if (w > 0) {
                    sb.append(watch, 0, w);
                }

                sb.append(c);

                w = 0;
            }
        }

        if (watch == close) {
            out.append(open);
        }
        out.append(sb);

        return out.toString();
    }

    /**
     * Replaces new line delimiters in the input stream with the Unix line feed.
     * 
     * @param input
     */
    public static byte[] toUnixLineEndings(InputStream input) throws IOException {
        String encoding = "ISO-8859-1";
        FixCrLfFilter filter = new FixCrLfFilter(new InputStreamReader(input, encoding));
        filter.setEol(FixCrLfFilter.CrLf.newInstance("unix"));
        
        ByteArrayOutputStream filteredFile = new ByteArrayOutputStream();
        Utils.copy(new ReaderInputStream(filter, encoding), filteredFile);
        
        return filteredFile.toByteArray();
    }
}
