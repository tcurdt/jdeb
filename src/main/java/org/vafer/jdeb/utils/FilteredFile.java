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

package org.vafer.jdeb.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class FilteredFile {

    public static final String DEFAULT_OPEN_TOKEN = "[[";
    public static final String DEFAULT_CLOSE_TOKEN = "]]";

    private String openToken;
    private String closeToken;
    private List<String> lines = new ArrayList<>();

    @Deprecated
    public FilteredFile(InputStream in, VariableResolver resolver) throws IOException {
        this(in, resolver, Charset.defaultCharset());
    }

    public FilteredFile(InputStream in, VariableResolver resolver, Charset encoding) throws IOException {
        this(in, resolver, encoding, DEFAULT_OPEN_TOKEN, DEFAULT_CLOSE_TOKEN);
    }

    public FilteredFile(InputStream in, VariableResolver resolver, Charset encoding, String openToken, String closeToken) throws IOException {
        this.openToken = openToken;
        this.closeToken = closeToken;
        parse(in, resolver, encoding);
    }

    @Deprecated
    public void setOpenToken(String token) {
        openToken = token;
    }

    @Deprecated
    public void setCloseToken(String token) {
        closeToken = token;
    }

    private void parse(InputStream in, VariableResolver resolver, Charset encoding) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (resolver != null) {
                    lines.add(Utils.replaceVariables(resolver, line, openToken, closeToken));
                } else {
                    lines.add(line);
                }
            }
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append('\n');
        }
        return builder.toString();
    }
}
