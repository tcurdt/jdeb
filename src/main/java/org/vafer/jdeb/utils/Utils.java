/*
 * Copyright 2013 The jdeb developers.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
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
     * Read properties from the active profiles.
     * 
     * Goes through all active profiles (in the order the
     * profiles are defined in settings.xml) and extracts
     * the desired properties (if present). The prefix is
     * used when looking up properties in the profile but
     * not in the returned map.
     * 
     * @param prefix The prefix to use or null if no prefix should be used
     * @param settings The Settings to read from
     * @param properties The properties to read
     * 
     * @return A map containing the values for the properties that were found
     */
    public static Map<String, String> readPropertiesFromActiveProfiles(String prefix, Settings settings, String... properties) {
        Map<String, String> map = new HashMap<String, String>();
        Set<String> activeProfiles = new HashSet<String>(settings.getActiveProfiles());

    	// Iterate over all active profiles in order
		for(Profile profile : settings.getProfiles()) {

			// Check if the profile is active
			if(activeProfiles.contains(profile.getId())) {

				// Check all desired properties
				for(String property : properties) {
					String value = profile.getProperties().getProperty(prefix != null ? prefix + property : property);
					if(value != null)
					{
						map.put(property, value);
					}
				}
			}
		}

        return map;
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
        char[] watch = open;
        int w = 0;
        for (char c : pExpression.toCharArray()) {
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
                    } else {
                        // found close
                        final String variable = pResolver.get(sb.toString());
                        if (variable != null) {
                            out.append(variable);
                        } else {
                            out.append(pOpen);
                            out.append(sb);
                            out.append(pClose);
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
    public static byte[] toUnixLineEndings( InputStream input ) throws IOException {
        String encoding = "ISO-8859-1";
        FixCrLfFilter filter = new FixCrLfFilter(new InputStreamReader(input, encoding));
        filter.setEol(FixCrLfFilter.CrLf.newInstance("unix"));

        ByteArrayOutputStream filteredFile = new ByteArrayOutputStream();
        Utils.copy(new ReaderInputStream(filter, encoding), filteredFile);

        return filteredFile.toByteArray();
    }

    /**
     * convert to debian version format
     */
    public static String convertToDebianVersion( String version, Date timestamp ) {
        version = version.replace('-', '+');
        if (version.endsWith("+SNAPSHOT")) {
            version = version.substring(0, version.length() - "+SNAPSHOT".length());
            version += "~";

            if (timestamp != null) {
                version += new SimpleDateFormat("yyyyMMddHHmmss").format(timestamp);
            } else {
                version += "SNAPSHOT";
            }
        }
        return version;
    }
    
    /**
     * Get the possible locations where the secure keyring can be located.
     * Looks through known locations of the GNU PG secure keyring.
     * 
     * @return The location of the PGP secure keyring if it was found,
     *         null otherwise
     */
    public static Collection<String> getPossiblePGPSecureRingLocations()
    {
        LinkedHashSet<String> locations = new LinkedHashSet<String>();

        // The user's roaming profile on Windows, via environment
        String windowsRoaming = System.getenv("APPDATA");
        if(windowsRoaming != null) {
            locations.add(joinPaths(windowsRoaming, "gnupg", "secring.gpg"));
        }

        // The user's local profile on Windows, via environment
        String windowsLocal = System.getenv("LOCALAPPDATA");
        if(windowsLocal != null) {
            locations.add(joinPaths(windowsLocal, "gnupg", "secring.gpg"));
        }

        // The user's home directory
        String home = System.getProperty("user.home");
        if(home != null) {
            // *nix, including OS X
            locations.add(joinPaths(home, ".gnupg", "secring.gpg"));

            // These are for various flavours of Windows if the environment variables above should fail
            locations.add(joinPaths(home, "AppData", "Roaming", "gnupg", "secring.gpg")); // Roaming profile on Vista and later
            locations.add(joinPaths(home, "AppData", "Local", "gnupg", "secring.gpg")); // Local profile on Vista and later
            locations.add(joinPaths(home, "Application Data", "gnupg", "secring.gpg")); // Roaming profile on 2000 and XP
            locations.add(joinPaths(home, "Local Settings", "Application Data", "gnupg", "secring.gpg")); // Local profile on 2000 and XP
        }

        // The Windows installation directory
        String windir = System.getProperty("WINDIR");
        if(windir != null) {
            // Local Profile on Windows 98 and ME
            locations.add(joinPaths(windir, "Application Data", "gnupg", "secring.gpg"));
        }

        return locations;
    }

    /**
     * Join together path elements with File.separator. Filters out null
     * elements.
     * 
     * @param elements The path elements to join
     * @return elements concatenated together with File.separator
     */
    public static String joinPaths(String... elements) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for(int i = 0; i < elements.length; i++) {
            // Skip null elements
            if(elements[i] == null)
            {
                // This won't change the value of first if we skip elements
                // in the beginning of the array
                continue;
            }
            if(!first) {
                builder.append(File.separatorChar);
            }
            builder.append(elements[i]);
            first = false;
        }
        return builder.toString();
    }
}
