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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.apache.tools.tar.TarEntry;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.DebMaker;
import org.vafer.jdeb.PackagingException;
import org.vafer.jdeb.utils.FilteredFile;
import org.vafer.jdeb.utils.MapVariableResolver;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

import static org.vafer.jdeb.utils.Utils.lookupIfEmpty;

/**
 * Creates Debian package
 */
@SuppressWarnings("unused")
@Mojo(name = "jdeb", defaultPhase = LifecyclePhase.PACKAGE)
public class DebMojo extends AbstractPluginMojo {

    @Component
    private MavenProjectHelper projectHelper;

    @Component(hint = "jdeb-sec")
    private SecDispatcher secDispatcher;

    /**
     * Defines the name of deb package.
     */
    @Parameter
    private String name;

    /**
     * Defines the pattern of the name of final artifacts. Possible
     * substitutions are [[baseDir]] [[buildDir]] [[artifactId]] [[version]]
     * [[extension]] and [[groupId]].
     */
    @Parameter(defaultValue = "[[buildDir]]/[[artifactId]]_[[version]]_all.[[extension]]")
    private String deb;

    /**
     * Explicitly defines the path to the control directory. At least the
     * control file is mandatory.
     */
    @Parameter(defaultValue = "[[baseDir]]/src/deb/control")
    private String controlDir;

    /**
     * Explicitly define the file to read the changes from.
     */
    @Parameter(defaultValue = "[[baseDir]]/CHANGES.txt")
    private String changesIn;

    /**
     * Explicitly define the file where to write the changes to.
     */
    @Parameter(defaultValue = "[[buildDir]]/[[artifactId]]_[[version]]_all.changes")
    private String changesOut;

    /**
     * Explicitly define the file where to write the changes of the changes input to.
     */
    @Parameter(defaultValue = "[[baseDir]]/CHANGES.txt")
    private String changesSave;

    /**
     * The compression method used for the data file (none, gzip, bzip2 or xz)
     */
    @Parameter(defaultValue = "gzip")
    private String compression;

    /**
     * Boolean option whether to attach the artifact to the project
     */
    @Parameter(defaultValue = "true")
    private String attach;

    /**
     * The location where all package files will be installed. By default, all
     * packages are installed in /opt (see the FHS here:
     * http://www.pathname.com/
     * fhs/pub/fhs-2.3.html#OPTADDONAPPLICATIONSOFTWAREPACKAGES)
     */
    @Parameter(defaultValue = "/opt/[[artifactId]]")
    private String installDir;

    /**
     * The type of attached artifact
     */
    @Parameter(defaultValue = "deb")
    private String type;

    /**
     * The project base directory
     */
    @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
    private File baseDir;

    /**
     * Run the plugin on all sub-modules.
     * If set to false, the plugin will be run in the same folder where the
     * mvn command was invoked
     */
    @Parameter(defaultValue = "true")
    private boolean submodules;

    /**
     * The Maven Session Object
     */
    @Component
    private MavenSession session;

    /**
     * The classifier of attached artifact
     */
    @Parameter
    private String classifier;

    /**
     * "data" entries used to determine which files should be added to this deb.
     * The "data" entries may specify a tarball (tar.gz, tar.bz2, tgz), a
     * directory, or a normal file. An entry would look something like this in
     * your pom.xml:
     *
     * <pre>
     *   <build>
     *     <plugins>
     *       <plugin>
     *       <artifactId>jdeb</artifactId>
     *       <groupId>org.vafer</groupId>
     *       ...
     *       <configuration>
     *         ...
     *         <dataSet>
     *           <data>
     *             <src>${project.basedir}/target/my_archive.tar.gz</src>
     *             <include>...</include>
     *             <exclude>...</exclude>
     *             <mapper>
     *               <type>perm</type>
     *               <strip>1</strip>
     *               <prefix>/somewhere/else</prefix>
     *               <user>santbj</user>
     *               <group>santbj</group>
     *               <mode>600</mode>
     *             </mapper>
     *           </data>
     *           <data>
     *             <src>${project.build.directory}/data</src>
     *             <include></include>
     *             <exclude>**&#47;.svn</exclude>
     *             <mapper>
     *               <type>ls</type>
     *               <src>mapping.txt</src>
     *             </mapper>
     *           </data>
     *           <data>
     *             <type>link</type>
     *             <linkName>/a/path/on/the/target/fs</linkName>
     *             <linkTarget>/a/sym/link/to/the/scr/file</linkTarget>
     *             <symlink>true</symlink>
     *           </data>
     *           <data>
     *             <src>${project.basedir}/README.txt</src>
     *           </data>
     *         </dataSet>
     *       </configuration>
     *     </plugins>
     *   </build>
     * </pre>
     */
    @Parameter
    private Data[] dataSet;

    /**
     * When SNAPSHOT version replace <code>SNAPSHOT</code> with current date
     * and time to make sure each build is unique.
     */
    @Parameter(defaultValue = "false")
    private boolean timestamped;

    /**
     * If verbose is true more build messages are logged.
     */
    @Parameter(defaultValue = "false")
    private boolean verbose;

    /**
     * Indicates if the execution should be disabled. If <code>true</code>, nothing will occur during execution.
     * 
     * @since 1.1
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * If signPackage is true then a origin signature will be placed
     * in the generated package.
     */
    @Parameter(defaultValue = "false")
    private boolean signPackage;

    /**
     * The keyring to use for signing operations.
     */
    @Parameter
    private String keyring;

    /**
     * The key to use for signing operations.
     */
    @Parameter
    private String key;

    /**
     * The passphrase to use for signing operations.
     */
    @Parameter
    private String passphrase; 

    /**
     * The prefix to use when reading signing variables
     * from settings.
     */
    @Parameter(defaultValue = "jdeb.")
    private String signCfgPrefix;

    /**
     * The settings.
     */
    @Parameter(defaultValue = "${settings}")
    private Settings settings;

    /* end of parameters */


    private static final String KEY = "key";
    private static final String KEYRING = "keyring";
    private static final String PASSPHRASE = "passphrase";

    private String openReplaceToken = "[[";
    private String closeReplaceToken = "]]";
    private Console console;
    private Collection<DataProducer> dataProducers = new ArrayList<DataProducer>();
    private Collection<DataProducer> conffileProducers = new ArrayList<DataProducer>();

    public void setOpenReplaceToken( String openReplaceToken ) {
        this.openReplaceToken = openReplaceToken;
        // FIXME yuck!
        FilteredFile.setOpenToken(openReplaceToken);
    }

    public void setCloseReplaceToken( String closeReplaceToken ) {
        this.closeReplaceToken = closeReplaceToken;
        // FIXME yuck!
        FilteredFile.setCloseToken(closeReplaceToken);
    }

    protected void setData( Data[] dataSet ) {
        this.dataSet = dataSet;
        dataProducers.clear();
        conffileProducers.clear();
        if (dataSet != null) {
            Collections.addAll(dataProducers, dataSet);
            
            for (Data item : dataSet) {
                if (item.getConffile()) {
                    conffileProducers.add(item);
                }
            }
        }
    }

    protected VariableResolver initializeVariableResolver( Map<String, String> variables ) {
        @SuppressWarnings("unchecked")
        final Map<String, String> projectProperties = Map.class.cast(getProject().getProperties());
        @SuppressWarnings("unchecked")
        final Map<String, String> systemProperties = Map.class.cast(System.getProperties());

        variables.putAll(projectProperties);
        variables.putAll(systemProperties);
        variables.put("name", name != null ? name : getProject().getName());
        variables.put("artifactId", getProject().getArtifactId());
        variables.put("groupId", getProject().getGroupId());
        variables.put("version", getProjectVersion());
        variables.put("description", getProject().getDescription());
        variables.put("extension", "deb");
        variables.put("baseDir", getProject().getBasedir().getAbsolutePath());
        variables.put("buildDir", buildDirectory.getAbsolutePath());
        variables.put("project.version", getProject().getVersion());
        variables.put("url", getProject().getUrl());

        return new MapVariableResolver(variables);
    }

    /**
     * Doc some cleanup and conversion on the Maven project version.
     * <ul>
     * <li>any "-" is replaced by "+"</li>
     * <li>"SNAPSHOT" is replaced with the current time and date, prepended by "~"</li>
     * </ul>
     *
     * @return the Maven project version
     */
    private String getProjectVersion() {
        return Utils.convertToDebianVersion(getProject().getVersion(), this.timestamped ? session.getStartTime() : null);
    }

    /**
     * @return whether or not Maven is currently operating in the execution root
     */
    private boolean isSubmodule() {
        // FIXME there must be a better way
        return !session.getExecutionRootDirectory().equalsIgnoreCase(baseDir.toString());
    }

    /**
     * @return whether or not the main artifact was created
     */
    private boolean hasMainArtifact() {
        final MavenProject project = getProject();
        final Artifact artifact = project.getArtifact();
        return artifact.getFile() != null && artifact.getFile().isFile();
    }

    /**
     * Main entry point
     *
     * @throws MojoExecutionException on error
     */
    @Override
    public void execute() throws MojoExecutionException {

        final MavenProject project = getProject();

        if (skip) {
            getLog().info("skipping execution");
            return;
        }

        if (isSubmodule() && !submodules) {
            getLog().info("skipping sub module: jdeb executing at top-level only");
            return;
        }

        setData(dataSet);

        console = new MojoConsole(getLog(), verbose);

        initializeSignProperties();

        final VariableResolver resolver = initializeVariableResolver(new HashMap<String, String>());

        final File debFile = new File(Utils.replaceVariables(resolver, deb, openReplaceToken, closeReplaceToken));
        final File controlDirFile = new File(Utils.replaceVariables(resolver, controlDir, openReplaceToken, closeReplaceToken));
        final File installDirFile = new File(Utils.replaceVariables(resolver, installDir, openReplaceToken, closeReplaceToken));
        final File changesInFile = new File(Utils.replaceVariables(resolver, changesIn, openReplaceToken, closeReplaceToken));
        final File changesOutFile = new File(Utils.replaceVariables(resolver, changesOut, openReplaceToken, closeReplaceToken));
        final File changesSaveFile = new File(Utils.replaceVariables(resolver, changesSave, openReplaceToken, closeReplaceToken));
        final File keyringFile = keyring == null ? null : new File(Utils.replaceVariables(resolver, keyring, openReplaceToken, closeReplaceToken));

        // if there are no producers defined we try to use the artifacts
        if (dataProducers.isEmpty()) {

            if (!hasMainArtifact()) {

                final String packaging = project.getPackaging();
                if ("pom".equalsIgnoreCase(packaging)) {
                    getLog().warn("Creating empty debian package.");
                } else {
                    throw new MojoExecutionException(
                        "Nothing to include into the debian package. " +
                            "Did you maybe forget to add a <data> tag or call the plugin directly?");
                }

            } else {

                Set<Artifact> artifacts = new HashSet<Artifact>();

                artifacts.add(project.getArtifact());

                @SuppressWarnings("unchecked")
                final Set<Artifact> projectArtifacts = project.getArtifacts();

                for (Artifact artifact : projectArtifacts) {
                    artifacts.add(artifact);
                }

                @SuppressWarnings("unchecked")
                final List<Artifact> attachedArtifacts = project.getAttachedArtifacts();

                for (Artifact artifact : attachedArtifacts) {
                    artifacts.add(artifact);
                }

                for (Artifact artifact : artifacts) {
                    final File file = artifact.getFile();
                    if (file != null) {
                        dataProducers.add(new DataProducer() {
                            @Override
                            public void produce( final DataConsumer receiver ) {
                                try {
                                    receiver.onEachFile(
                                        new FileInputStream(file),
                                        new File(installDirFile, file.getName()).getAbsolutePath(),
                                        "",
                                        "root", 0, "root", 0,
                                        TarEntry.DEFAULT_FILE_MODE,
                                        file.length());
                                } catch (Exception e) {
                                    getLog().error(e);
                                }
                            }
                        });
                    } else {
                        getLog().error("No file for artifact " + artifact);
                    }
                }
            }
        }

        try {
            DebMaker debMaker = new DebMaker(console, dataProducers, conffileProducers);
            debMaker.setDeb(debFile);
            debMaker.setControl(controlDirFile);
            debMaker.setPackage(getProject().getArtifactId());
            debMaker.setDescription(getProject().getDescription());
            debMaker.setHomepage(getProject().getUrl());
            debMaker.setChangesIn(changesInFile);
            debMaker.setChangesOut(changesOutFile);
            debMaker.setChangesSave(changesSaveFile);
            debMaker.setCompression(compression);
            debMaker.setKeyring(keyringFile);
            debMaker.setKey(key);
            debMaker.setPassphrase(passphrase);
            debMaker.setSignPackage(signPackage);
            debMaker.setResolver(resolver);
            debMaker.validate();
            debMaker.makeDeb();

            // Always attach unless explicitly set to false
            if ("true".equalsIgnoreCase(attach)) {
                console.info("Attaching created debian package " + debFile);
                projectHelper.attachArtifact(project, type, classifier, debFile);
            }

        } catch (PackagingException e) {
            getLog().error("Failed to create debian package " + debFile, e);
            throw new MojoExecutionException("Failed to create debian package " + debFile, e);
        }
    }

    /**
     * Initializes unspecified sign properties using available defaults
     * and global settings.
     */
    private void initializeSignProperties() {
        if (!signPackage) {
            return;
        }

        if (key != null && keyring != null && passphrase != null) {
            return;
        }

        Map<String, String> properties =
                readPropertiesFromActiveProfiles(signCfgPrefix, KEY, KEYRING, PASSPHRASE);

        key = lookupIfEmpty(key, properties, KEY);
        keyring = lookupIfEmpty(keyring, properties, KEYRING);
        passphrase = decrypt(lookupIfEmpty(passphrase, properties, PASSPHRASE));

        if (keyring == null) {
            try {
                keyring = Utils.guessKeyRingFile().getAbsolutePath();
                console.info("Located keyring at " + keyring);
            } catch (FileNotFoundException e) {
                console.warn(e.getMessage());
            }
        }
    }

    /**
     * Decrypts given passphrase if needed using maven security dispatcher.
     * See http://maven.apache.org/guides/mini/guide-encryption.html for details.
     *
     * @param maybeEncryptedPassphrase possibly encrypted passphrase
     * @return decrypted passphrase
     */
    private String decrypt( final String maybeEncryptedPassphrase ) {
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
    public Map<String, String> readPropertiesFromActiveProfiles( final String prefix,
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
