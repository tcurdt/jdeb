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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.filters.FixCrLfFilter;
import org.apache.tools.ant.util.ReaderInputStream;

/**
 * Simple utils functions.
 *
 * ATTENTION: don't use outside of jdeb
 */
public final class Utils {
    private static final Pattern BETA_PATTERN = Pattern.compile("^(?:(?:(.*?)([.\\-_]))|(.*[^a-z]))(alpha|a|beta|b|milestone|m|cr|rc)([^a-z].*)?$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SNAPSHOT_PATTERN = Pattern.compile("(.*)[\\-+]SNAPSHOT");

    public static int copy( final InputStream pInput, final OutputStream pOutput ) throws IOException {
        final byte[] buffer = new byte[2048];
        int count = 0;
        int n;
        while (-1 != (n = pInput.read(buffer))) {
            pOutput.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static String toHex( final byte[] bytes ) {
        final StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(Integer.toHexString((b >> 4) & 0x0f));
            sb.append(Integer.toHexString(b & 0x0f));
        }

        return sb.toString();
    }

    public static String stripPath( final int p, final String s ) {

        if (p <= 0) {
            return s;
        }

        int x = 0;
        for (int i = 0; i < p; i++) {
            x = s.indexOf('/', x + 1);
            if (x < 0) {
                return s;
            }
        }

        return s.substring(x + 1);
    }

    private static String joinPath(char sep, String ...paths) {
        final StringBuilder sb = new StringBuilder();
        for (String p : paths) {
            if (p == null) continue;
            if (!p.startsWith("/") && sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(p);
        }
        return sb.toString();
    }

    public static String joinUnixPath(String ...paths) {
        return joinPath('/', paths);
    }

    public static String joinLocalPath(String ...paths) {
        return joinPath(File.separatorChar, paths);
    }

    public static String stripLeadingSlash( final String s ) {
        if (s == null) {
            return s;
        }
        if (s.length() == 0) {
            return s;
        }
        if (s.charAt(0) == '/' || s.charAt(0) == '\\') {
            return s.substring(1);
        }
        return s;
    }

    /**
     * Substitute the variables in the given expression with the
     * values from the resolver
     *
     * @param pResolver
     * @param pExpression
     */
    public static String replaceVariables( final VariableResolver pResolver, final String pExpression, final String pOpen, final String pClose ) {
        final char[] open = pOpen.toCharArray();
        final char[] close = pClose.toCharArray();

        final StringBuilder out = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        char[] last = null;
        int wo = 0;
        int wc = 0;
        int level = 0;
        for (char c : pExpression.toCharArray()) {
            if (c == open[wo]) {
                if (wc > 0) {
                    sb.append(close, 0, wc);
                }
                wc = 0;
                wo++;
                if (open.length == wo) {
                    // found open
                    if (last == open) {
                        out.append(open);
                    }
                    level++;
                    out.append(sb);
                    sb = new StringBuilder();
                    wo = 0;
                    last = open;
                }
            } else if (c == close[wc]) {
                if (wo > 0) {
                    sb.append(open, 0, wo);
                }
                wo = 0;
                wc++;
                if (close.length == wc) {
                    // found close
                    if (last == open) {
                        final String variable = pResolver.get(sb.toString());
                        if (variable != null) {
                            out.append(variable);
                        } else {
                            out.append(open);
                            out.append(sb);
                            out.append(close);
                        }
                    } else {
                        out.append(sb);
                        out.append(close);
                    }
                    sb = new StringBuilder();
                    level--;
                    wc = 0;
                    last = close;
                }
            } else {

                if (wo > 0) {
                    sb.append(open, 0, wo);
                }

                if (wc > 0) {
                    sb.append(close, 0, wc);
                }

                sb.append(c);

                wo = wc = 0;
            }
        }

        if (wo > 0) {
            sb.append(open, 0, wo);
        }

        if (wc > 0) {
            sb.append(close, 0, wc);
        }

        if (level > 0) {
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
    public static byte[] toUnixLineEndings( InputStream input ) throws IOException {
        String encoding = "ISO-8859-1";
        FixCrLfFilter filter = new FixCrLfFilter(new InputStreamReader(input, encoding));
        filter.setEol(FixCrLfFilter.CrLf.newInstance("unix"));

        ByteArrayOutputStream filteredFile = new ByteArrayOutputStream();
        Utils.copy(new ReaderInputStream(filter, encoding), filteredFile);

        return filteredFile.toByteArray();
    }

    private static String formatSnapshotTemplate( String template, Date timestamp ) {
        int startBracket = template.indexOf('[');
        int endBracket = template.indexOf(']');
        if(startBracket == -1 || endBracket == -1) {
            return template;
        } else {
            // prefix[yyMMdd]suffix
            final String date = new SimpleDateFormat(template.substring(startBracket + 1, endBracket)).format(timestamp);
            String datePrefix = startBracket == 0 ? "" : template.substring(0, startBracket);
            String dateSuffix = endBracket == template.length() ? "" : template.substring(endBracket + 1);
            return datePrefix + date + dateSuffix;
        }
    }

    /**
     * Convert the project version to a version suitable for a Debian package.
     * -SNAPSHOT suffixes are replaced with a timestamp (~yyyyMMddHHmmss).
     * The separator before a rc, alpha or beta version is replaced with '~'
     * such that the version is always ordered before the final or GA release.
     *
     * @param version the project version to convert to a Debian package version
     * @param template the template used to replace -SNAPSHOT, the timestamp format is in brackets,
     *        the rest of the string is preserved (prefix[yyMMdd]suffix -> prefix151230suffix)
     * @param timestamp the UTC date used as the timestamp to replace the SNAPSHOT suffix.
     */
    public static String convertToDebianVersion( String version, boolean apply, String envName, String template, Date timestamp ) {
        Matcher matcher = SNAPSHOT_PATTERN.matcher(version);
        if (matcher.matches()) {
            version = matcher.group(1) + "~";

            if (apply) {
                final String envValue = System.getenv(envName);
                if(template != null && template.length() > 0) {
                    version += formatSnapshotTemplate(template, timestamp);
                } else if (envValue != null && envValue.length() > 0) {
                    version += envValue;
                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    version += dateFormat.format(timestamp);
                }
            } else {
                version += "SNAPSHOT";
            }
        } else {
            matcher = BETA_PATTERN.matcher(version);
            if (matcher.matches()) {
                if (matcher.group(1) != null) {
                    version = matcher.group(1) + "~" + matcher.group(4) + matcher.group(5);
                } else {
                    version = matcher.group(3) + "~" + matcher.group(4) + matcher.group(5);
                }
            }
        }

        // safest upstream_version should only contain full stop, plus, tilde, and alphanumerics
        // https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Version
        version = version.replaceAll("[^.+~A-Za-z0-9]", "+").replaceAll("\\++", "+");

        return version;
    }

    /**
     * Construct new path by replacing file directory part. No
     * files are actually modified.
     * @param file path to move
     * @param target new path directory
     */
    public static String movePath( final String file,
                                   final String target ) {
        final String name = new File(file).getName();
        return target.endsWith("/") ? target + name : target + '/' + name;
    }

    /**
     * Extracts value from map if given value is null.
     * @param value current value
     * @param props properties to extract value from
     * @param key property name to extract
     * @return initial value or value extracted from map
     */
    public static String lookupIfEmpty( final String value,
                                        final Map<String, String> props,
                                        final String key ) {
        return value != null ? value : props.get(key);
    }

    /**
    * Get the known locations where the secure keyring can be located.
    * Looks through known locations of the GNU PG secure keyring.
    *
    * @return The location of the PGP secure keyring if it was found,
    *         null otherwise
    */
    public static Collection<String> getKnownPGPSecureRingLocations() {
        final LinkedHashSet<String> locations = new LinkedHashSet<>();

        final String os = System.getProperty("os.name");
        final boolean runOnWindows = os == null || os.toLowerCase().contains("win");

        if (runOnWindows) {
            // The user's roaming profile on Windows, via environment
            final String windowsRoaming = System.getenv("APPDATA");
            if (windowsRoaming != null) {
                locations.add(joinLocalPath(windowsRoaming, "gnupg", "secring.gpg"));
            }

            // The user's local profile on Windows, via environment
            final String windowsLocal = System.getenv("LOCALAPPDATA");
            if (windowsLocal != null) {
                locations.add(joinLocalPath(windowsLocal, "gnupg", "secring.gpg"));
            }

            // The Windows installation directory
            final String windir = System.getProperty("WINDIR");
            if (windir != null) {
                // Local Profile on Windows 98 and ME
                locations.add(joinLocalPath(windir, "Application Data", "gnupg", "secring.gpg"));
            }
        }

        final String home = System.getProperty("user.home");

        if (home != null && runOnWindows) {
            // These are for various flavours of Windows
            // if the environment variables above have failed

            // Roaming profile on Vista and later
            locations.add(joinLocalPath(home, "AppData", "Roaming", "gnupg", "secring.gpg"));
            // Local profile on Vista and later
            locations.add(joinLocalPath(home, "AppData", "Local", "gnupg", "secring.gpg"));
            // Roaming profile on 2000 and XP
            locations.add(joinLocalPath(home, "Application Data", "gnupg", "secring.gpg"));
            // Local profile on 2000 and XP
            locations.add(joinLocalPath(home, "Local Settings", "Application Data", "gnupg", "secring.gpg"));
        }

        // *nix, including OS X
        if (home != null) {
            locations.add(joinLocalPath(home, ".gnupg", "secring.gpg"));
        }

        return locations;
    }

    /**
     * Tries to guess location of the user secure keyring using various
     * heuristics.
     *
     * @return path to the keyring file
     * @throws FileNotFoundException if no keyring file found
     */
    public static File guessKeyRingFile() throws FileNotFoundException {
        final Collection<String> possibleLocations = getKnownPGPSecureRingLocations();
        for (final String location : possibleLocations) {
            final File candidate = new File(location);
            if (candidate.exists()) {
                return candidate;
            }
        }
        final StringBuilder message = new StringBuilder("Could not locate secure keyring, locations tried: ");
        final Iterator<String> it = possibleLocations.iterator();
        while (it.hasNext()) {
            message.append(it.next());
            if (it.hasNext()) {
                message.append(", ");
            }
        }
        throw new FileNotFoundException(message.toString());
    }

    /**
     * Returns true if string is null or empty.
     */
    public static boolean isNullOrEmpty(final String str) {
        return str == null || str.length() == 0;
    }

    /**
    * Return fallback if first string is null or empty
    */
    public static String defaultString(final String str, final String fallback) {
        return isNullOrEmpty(str) ? fallback : str;
    }


    /**
     * Check if a CharSequence is whitespace, empty ("") or null.
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     */
    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
