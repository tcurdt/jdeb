/*
 * Copyright 2014 The jdeb developers.
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
import java.util.ArrayList;
import java.util.List;

public class FilteredFile {

    private static String openToken = "[[";
    private static String closeToken = "]]";
    private List<String> lines = new ArrayList<String>();

    public FilteredFile(InputStream in, VariableResolver resolver) throws IOException {
        parse(in, resolver);
    }

    public static void setOpenToken(String token) {
        openToken = token;
    }

    public static void setCloseToken(String token) {
        closeToken = token;
    }

    private void parse(InputStream in, VariableResolver resolver) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                if (resolver != null) {
                    lines.add(Utils.replaceVariables(resolver, line, openToken, closeToken));
                } else {
                    lines.add(line);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
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
