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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
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
import org.vafer.jdeb.utils.MapVariableResolver;
import org.vafer.jdeb.utils.OutputTimestampResolver;
import org.vafer.jdeb.utils.SymlinkUtils;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

import static org.vafer.jdeb.utils.Utils.isBlank;
import static org.vafer.jdeb.utils.Utils.lookupIfEmpty;

/**
 * Creates Debian package
 */
@Mojo(name = "jdeb", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class DebMojo extends AbstractMojo {

    @Component
    private MavenProjectHelper projectHelper;

    @Component(hint = "jdeb-sec")
    private SecDispatcher secDispatcher;

    @Parameter
    private String name;

    @Parameter(defaultValue = "[[buildDir]]/[[artifactId]]_[[version]]_all.[[extension]]")
    private String deb;

    @Parameter(defaultValue = "[[baseDir]]/src/deb/control")
    private String controlDir;

    @Parameter(defaultValue = "[[baseDir]]/CHANGES.txt")
    private String changesIn;

    @Parameter(defaultValue = "[[buildDir]]/[[artifactId]]_[[version]]_all.changes")
    private String changesOut;

    @Parameter(defaultValue = "[[baseDir]]/CHANGES.txt")
    private String changesSave;

    @Parameter(defaultValue = "gzip")
    private String compression;

    @Parameter(defaultValue = "true")
    private String attach;

    @Parameter(defaultValue = "/opt/[[artifactId]]")
    private String installDir;

    @Parameter(defaultValue = "deb")
    private String type;

    @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
    private File baseDir;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "project.build.directory", required = true, readonly = true)
    private File buildDirectory;

    @Parameter
    private String classifier;

    @Parameter(defaultValue = "SHA256")
    private String digest;

    @Parameter
    private Data[] dataSet;

    @Parameter(defaultValue = "false")
    private boolean snapshotExpand;

    @Parameter(defaultValue = "SNAPSHOT")
    private String snapshotEnv;

    @Parameter
    private String snapshotTemplate;

    @Parameter(defaultValue = "false")
    private boolean verbose;

    @Parameter(property = "jdeb.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "jdeb.skipPOMs", defaultValue = "true")
    private boolean skipPOMs;

    @Parameter(property = "jdeb.skipSubmodules", defaultValue = "false")
    private boolean skipSubmodules;

    @Parameter(defaultValue = "true")
    private boolean submodules;

    @Parameter(defaultValue = "false")
    private boolean signPackage;

    @Parameter(defaultValue = "false")
    private boolean signChanges;

    @Parameter(defaultValue = "debsig-verify")
    private String signMethod;

    @Parameter(defaultValue = "origin")
    private String signRole;

    @Parameter
    private String keyring;

    @Parameter
    private String key;

    @Parameter
    private String passphrase;

    @Parameter(defaultValue = "jdeb.")
    private String signCfgPrefix;

    @Parameter(defaultValue = "${settings}")
    private Settings settings;

    @Parameter(defaultValue = "")
    private String propertyPrefix;

    @Parameter(defaultValue = "gnu")
    private String tarLongFileMode;

    @Parameter(defaultValue = "gnu")
    private String tarBigNumberMode;

    @Parameter(defaultValue = "${project.build.outputTimestamp}")
    private String outputTimestamp;

    private static final String KEY = "key";
    private static final String KEYRING = "keyring";
    private static final String PASSPHRASE = "passphrase";

    private String openReplaceToken = "[[";
    private String closeReplaceToken = "]]";
    private Console console;
    private Collection<DataProducer> dataProducers = new ArrayList<>();
    private Collection<DataProducer> conffileProducers = new ArrayList<>();

    public void setOpenReplaceToken(String openReplaceToken) {
        this.openReplaceToken = openReplaceToken;
    }

    public void setCloseReplaceToken(String closeReplaceToken) {
        this.closeReplaceToken = closeReplaceToken;
    }

    protected void setData(Data[] dataSet) {
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected VariableResolver initializeVariableResolver(Map<String, String> variables) {
        variables.putAll((Map) project.getProperties());
        variables.putAll((Map) System.getProperties());
        variables.put("name", name != null ? name : project.getName());
        variables.put("artifactId", project.getArtifactId());
        variables.put("groupId", project.getGroupId());
        variables.put("version", getProjectVersion());
        variables.put("description", project.getDescription());
        variables.put("extension", "deb");
        variables.put("baseDir", project.getBasedir().getAbsolutePath());
        variables.put("buildDir", buildDirectory.getAbsolutePath());
        variables.put("project.version", project.getVersion());

        if (project.getInceptionYear() != null) {
            variables.put("project.inceptionYear", project.getInceptionYear());
        }
        if (project.getOrganization() != null) {
            if (project.getOrganization().getName() != null)
        variables.put("organization.name", project.getOrganization().getName());
            }
            if (project.getOrganization().getUrl() != null) {
                variables.put("organization.url", project.getOrganization().getUrl());
            }
        }

        // Resolve variables from settings.xml
        if (settings != null) {
            List<Profile> profiles = settings.getProfiles();
            for (Profile profile : profiles) {
                if (profile.getProperties() != null) {
                    variables.putAll((Map) profile.getProperties());
                }
            }
        }

        return new MapVariableResolver(variables);
    }

    protected String getProjectVersion() {
        String projectVersion = project.getVersion();
        if (projectVersion.endsWith("-SNAPSHOT") && snapshotExpand) {
            String snapshotSuffix = projectVersion.substring(projectVersion.lastIndexOf("-SNAPSHOT") + 9);
            String snapshotPrefix = projectVersion.substring(0, projectVersion.lastIndexOf("-SNAPSHOT"));
            return lookupIfEmpty(session.getSystemProperties(), snapshotEnv, snapshotTemplate, snapshotPrefix) + snapshotSuffix;
        }
        return projectVersion;
    }

    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping package creation");
            return;
        }
        if ("pom".equalsIgnoreCase(project.getPackaging()) && skipPOMs) {
            getLog().info("Skipping POM packaging");
            return;
        }
        if (skipSubmodules && "pom".equalsIgnoreCase(project.getPackaging()) && project.getModules() != null && !project.getModules().isEmpty()) {
            getLog().info("Skipping submodules");
            return;
        }

        console = new Console(getLog(), verbose);
        console.debug("Start");

        // Apply system properties
        if (StringUtils.isNotBlank(propertyPrefix)) {
            for (Map.Entry<String, String> entry : System.getProperties().entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(propertyPrefix)) {
                    String name = key.substring(propertyPrefix.length());
                    System.setProperty(name, entry.getValue());
                }
            }
        }

        // Initialize variables
        Map<String, String> variables = new HashMap<>();
        VariableResolver resolver = initializeVariableResolver(variables);

        File debFile = new File(Utils.replace(deb, resolver, openReplaceToken, closeReplaceToken));
        File changesFile = new File(Utils.replace(changesOut, resolver, openReplaceToken, closeReplaceToken));
        File changesSaveFile = new File(Utils.replace(changesSave, resolver, openReplaceToken, closeReplaceToken));

        // Expand deb filename with project properties
        try {
            if (StringUtils.isEmpty(classifier)) {
                classifier = null;
            }
            final Artifact artifact = project.getArtifact();
            if (classifier != null) {
                artifact.setClassifier(classifier);
            }
            if ("pom".equalsIgnoreCase(project.getPackaging())) {
                artifact.setType(type);
            }

            final File control = new File(Utils.replace(controlDir, resolver, openReplaceToken, closeReplaceToken));
            final File changes = new File(Utils.replace(changesIn, resolver, openReplaceToken, closeReplaceToken));

            if (changes.exists()) {
                Utils.copyFile(changes, changesSaveFile);
            }

            final DebMaker maker = new DebMaker(console, debFile, artifact, control, resolver);

            maker.setCompression(compression);
            maker.setChangesFile(changesFile);
            maker.setChangelogFile(changesSaveFile);
            maker.setInstallDir(installDir);
            maker.setDataConsumer(new DataConsumer() {
                public void onData(TarEntry entry, File file) throws PackagingException {
                    if (entry != null && !entry.isDirectory() && entry.getLinkName() != null) {
                        if (entry.getLinkName().startsWith(installDir + "/")) {
                            entry.setLinkName(entry.getLinkName().substring(installDir.length() + 1));
                        } else {
                            entry.setLinkName(SymlinkUtils.getRelativeSymlink(file.getParentFile(), new File(entry.getLinkName())));
                        }
                    }
                }
            });

            // Add checksums
            maker.addChecksum(digest);

            // Add sign parameters
            if (signPackage) {
                if (signMethod != null) {
                    maker.setSignMethod(signMethod);
                }
                if (signRole != null) {
                    maker.setSignRole(signRole);
                }
                if (signCfgPrefix != null) {
                    maker.setSignCfgPrefix(signCfgPrefix);
                }

                // passphrase property takes precedence over password
                String passphrase = session.getUserProperties().getProperty(PASSPHRASE);
                if (passphrase == null) {
                    passphrase = this.passphrase;
                }

                if (passphrase != null) {
                    maker.setPassphrase(passphrase.toCharArray());
                } else {
                    String password = session.getUserProperties().getProperty(KEYRING);
                    if (password == null) {
                        password = session.getUserProperties().getProperty(KEY);
                    }
                    if (password != null) {
                        maker.setKeyring(password.toCharArray());
                    }
                }
            }

            // Handle tar configurations
            maker.setTarLongFileMode(tarLongFileMode);
            maker.setTarBigNumberMode(tarBigNumberMode);
            maker.setConsole(console);
            maker.setVerbose(verbose);

            // Use local data set if provided
            if (dataSet != null) {
                for (Data item : dataSet) {
                    if (item instanceof DataFile) {
                        maker.add((DataFile) item);
                    }
                }
            }

            // Set the timestamp resolver
            maker.setTimestampResolver(new OutputTimestampResolver(new File(session.getExecutionRootDirectory())));

            // Expand any macros in the dataset
            List<String> files = maker.expandDataSet();

            // Add custom datasets
            if (dataSet != null) {
                for (Data item : dataSet) {
                    if (!(item instanceof DataFile)) {
                        item.setVariables(variables);
                        item.setDataConsumer(maker);
                        item.setDataProducer(maker);
                    }
                }
            }

            // Add default POM
            if (submodules) {
                maker.addPom();
            }

            // Add project's files
            if (!skipPOMs) {
                maker.addProject();
            }

            // Sign the package if necessary
            if (signPackage || signChanges) {
                try {
                    maker.sign();
                } catch (Exception e) {
                    console.error("Sign error", e);
                    throw new MojoExecutionException("Sign error", e);
                }
            }

            // Attach the deb package to the project
            if (attach != null && !StringUtils.equalsIgnoreCase(attach, "false")) {
                if (attach != null && Boolean.valueOf(attach)) {
                    projectHelper.attachArtifact(project, "deb", classifier, debFile);
                } else {
                    projectHelper.attachArtifact(project, "deb", classifier, debFile);
                }
            }

            // Attach the changes file to the project
            if (changesOut != null) {
                projectHelper.attachArtifact(project, "changes", classifier, changesFile);
            }

        } catch (Exception e) {
            console.error("Execution error", e);
            throw new MojoExecutionException("Execution error", e);
        }

        console.debug("End");
    }
}
