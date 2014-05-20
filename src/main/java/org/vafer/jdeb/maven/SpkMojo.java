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
import org.vafer.jdeb.SpkMaker;
import org.vafer.jdeb.utils.MapVariableResolver;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

import static org.vafer.jdeb.utils.Utils.lookupIfEmpty;

/**
 * Creates Synology package
 */
@SuppressWarnings("unused")
@Mojo(name = "spk", defaultPhase = LifecyclePhase.PACKAGE)
public class SpkMojo extends AbstractPackageMojo {

    /**
     * Defines the name of spk package.
     */
    @Parameter
    private String name;

    /**
     * Defines the pattern of the name of final artifacts. Possible
     * substitutions are [[baseDir]] [[buildDir]] [[artifactId]] [[version]]
     * [[extension]] and [[groupId]].
     */
    @Parameter(defaultValue = "[[buildDir]]/[[artifactId]]_[[version]].[[extension]]")
    private String spk;

    /**
     * Explicitly defines the path to the info file.
     */
    @Parameter(defaultValue = "[[baseDir]]/src/spk/info")
    private String infoFile;

    /**
     * Explicitly defines the path to the scripts directory.
     */
    @Parameter(defaultValue = "[[baseDir]]/src/spk/scripts")
    private String scriptsDir;

    /**
     * The compression method used for the data file (gzip)
     */
    @Parameter(defaultValue = "gzip")
    private String compression;

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
    @Parameter(defaultValue = "spk")
    protected String type;

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
     * When enabled SNAPSHOT inside the version gets replaced with current timestamp or
     * if set a value of a environment variable.
     */
    @Parameter(defaultValue = "false")
    private boolean snapshotExpand;

    /**
     * Which environment variable to check for the SNAPSHOT value.
     * If the variable is not set/empty it will default to use the timestamp.
     */
    @Parameter(defaultValue = "SNAPSHOT")
    private String snapshotEnv;

    /* end of parameters */

    private Collection<DataProducer> dataProducers = new ArrayList<DataProducer>();

    protected void setData( Data[] dataSet ) {
        this.dataSet = dataSet;
        dataProducers.clear();
        if (dataSet != null) {
            Collections.addAll(dataProducers, dataSet);
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
        variables.put("extension", "spk");
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
        return Utils.convertToDebianVersion(getProject().getVersion(), this.snapshotExpand, this.snapshotEnv, session.getStartTime());
    }

    /**
     * Main entry point
     *
     * @throws MojoExecutionException on error
     */
    public void execute() throws MojoExecutionException {

        final MavenProject project = getProject();

        if (skip) {
            getLog().info("skipping execution as configured");
            return;
        }

        if (isPOM()) {
            getLog().info("skipping execution because artifact is a pom");
            return;
        }

        if (isSubmodule() && !submodules) {
            getLog().info("skipping sub module: jdeb executing at top-level only");
            return;
        }


        setData(dataSet);

        console = new MojoConsole(getLog(), verbose);

        final VariableResolver resolver = initializeVariableResolver(new HashMap<String, String>());

        final File spkFile = new File(Utils.replaceVariables(resolver, spk, openReplaceToken, closeReplaceToken));
        final File spkInfoFile = new File(Utils.replaceVariables(resolver, infoFile, openReplaceToken, closeReplaceToken));
        final File scriptsDirFile = new File(Utils.replaceVariables(resolver, scriptsDir, openReplaceToken, closeReplaceToken));
        final File installDirFile = new File(Utils.replaceVariables(resolver, installDir, openReplaceToken, closeReplaceToken));

        // if there are no producers defined we try to use the artifacts
        if (dataProducers.isEmpty()) {

            if (!hasMainArtifact()) {

                final String packaging = project.getPackaging();
                if ("pom".equalsIgnoreCase(packaging)) {
                    getLog().warn("Creating empty synology package.");
                } else {
                    throw new MojoExecutionException(
                        "Nothing to include into the synology package. " +
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
            SpkMaker spkMaker = new SpkMaker(console, dataProducers);
            spkMaker.setSpk(spkFile);
            spkMaker.setInfo(spkInfoFile);
            spkMaker.setScripts(scriptsDirFile);
            spkMaker.setPackage(getProject().getArtifactId());
            spkMaker.setDescription(getProject().getDescription());
            spkMaker.setCompression(compression);
            spkMaker.setResolver(resolver);
            spkMaker.setOpenReplaceToken(openReplaceToken);
            spkMaker.setCloseReplaceToken(closeReplaceToken);
            spkMaker.validate();
            spkMaker.makeSpk();

            // Always attach unless explicitly set to false
            if ("true".equalsIgnoreCase(attach)) {
                console.info("Attaching created synology package " + spkFile);
                projectHelper.attachArtifact(project, type, classifier, spkFile);
            }

        } catch (PackagingException e) {
            getLog().error("Failed to create synology package " + spkFile, e);
            throw new MojoExecutionException("Failed to create synology package " + spkFile, e);
        }
    }

}
