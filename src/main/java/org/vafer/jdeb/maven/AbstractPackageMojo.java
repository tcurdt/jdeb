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

package org.vafer.jdeb.maven;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.vafer.jdeb.Console;

public abstract class AbstractPackageMojo extends AbstractPluginMojo {

    @Component
    protected MavenProjectHelper projectHelper;

    @Component(hint = "jdeb-sec")
    private SecDispatcher secDispatcher;

    /**
     * Boolean option whether to attach the artifact to the project
     */
    @Parameter(defaultValue = "true")
    protected String attach;

    /**
     * The project base directory
     */
    @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
    protected File baseDir;

    /**
     * Run the plugin on all sub-modules.
     * If set to false, the plugin will be run in the same folder where the
     * mvn command was invoked
     */
    @Parameter(defaultValue = "true")
    protected boolean submodules;

    /**
     * The Maven Session Object
     */
    @Component
    protected MavenSession session;

    /**
     * The classifier of attached artifact
     */
    @Parameter
    protected String classifier;

    /**
     * If verbose is true more build messages are logged.
     */
    @Parameter(defaultValue = "false")
    protected boolean verbose;

    /**
     * Indicates if the execution should be disabled. If <code>true</code>, nothing will occur during execution.
     * 
     * @since 1.1
     */
    @Parameter(defaultValue = "false")
    protected boolean skip;

    /**
     * The settings.
     */
    @Parameter(defaultValue = "${settings}")
    protected Settings settings;

    /* end of parameters */


    protected String openReplaceToken = "[[";
    protected String closeReplaceToken = "]]";
    protected Console console;

    public void setOpenReplaceToken( String openReplaceToken ) {
        this.openReplaceToken = openReplaceToken;
    }

    public void setCloseReplaceToken( String closeReplaceToken ) {
        this.closeReplaceToken = closeReplaceToken;
    }

    /**
     * @return whether the artifact is a POM or not
     */
    protected final boolean isPOM() {
        String type = getProject().getArtifact().getType();
        return "pom".equalsIgnoreCase(type);
    }

    /**
     * @return whether or not Maven is currently operating in the execution root
     */
    protected final boolean isSubmodule() {
        // FIXME there must be a better way
        return !session.getExecutionRootDirectory().equalsIgnoreCase(baseDir.toString());
    }

    /**
     * @return whether or not the main artifact was created
     */
    protected final boolean hasMainArtifact() {
        final MavenProject project = getProject();
        final Artifact artifact = project.getArtifact();
        return artifact.getFile() != null && artifact.getFile().isFile();
    }

    /**
     * Decrypts given passphrase if needed using maven security dispatcher.
     * See http://maven.apache.org/guides/mini/guide-encryption.html for details.
     *
     * @param maybeEncryptedPassphrase possibly encrypted passphrase
     * @return decrypted passphrase
     */
    protected final String decrypt( final String maybeEncryptedPassphrase ) {
        if (maybeEncryptedPassphrase == null) {
            return null;
        }

        try {
            final String decrypted = secDispatcher.decrypt(maybeEncryptedPassphrase);
            if (maybeEncryptedPassphrase.equals(decrypted)) {
                console.info("Passphrase was not encrypted");
            } else {
                console.info("Passphrase was successfully decrypted");
            }
            return decrypted;
        } catch (SecDispatcherException e) {
            console.warn("Unable to decrypt passphrase: " + e.getMessage());
        }

        return maybeEncryptedPassphrase;
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
     * @param properties The properties to read
     *
     * @return A map containing the values for the properties that were found
     */
    public final Map<String, String> readPropertiesFromActiveProfiles( final String prefix,
                                                                 final String... properties ) {
        if (settings == null) {
            console.debug("No maven setting injected");
            return Collections.emptyMap();
        }

        final List<String> activeProfilesList = settings.getActiveProfiles();
        if (activeProfilesList.isEmpty()) {
            console.debug("No active profiles found");
            return Collections.emptyMap();
        }

        final Map<String, String> map = new HashMap<String, String>();
        final Set<String> activeProfiles = new HashSet<String>(activeProfilesList);

        // Iterate over all active profiles in order
        for (final Profile profile : settings.getProfiles()) {
            // Check if the profile is active
            final String profileId = profile.getId();
            if (activeProfiles.contains(profileId)) {
                console.debug("Trying active profile " + profileId);
                for (final String property : properties) {
                    final String propKey = prefix != null ? prefix + property : property;
                    final String value = profile.getProperties().getProperty(propKey);
                    if (value != null) {
                        console.debug("Found property " + property + " in profile " + profileId);
                        map.put(property, value);
                    }
                }
            }
        }

        return map;
    }

}
