/*
 * Copyright 2010 The Apache Software Foundation.
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tools.tar.TarEntry;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.PackagingException;
import org.vafer.jdeb.descriptors.AbstractDescriptor;
import org.vafer.jdeb.utils.MapVariableResolver;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * Creates deb archive
 * 
 * @goal jdeb
 */
public class DebMojo extends AbstractPluginMojo {

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Defines the pattern of the name of final artifacts. Possible
     * substitutions are [[artifactId]] [[version]] [[extension]] and
     * [[groupId]].
     * 
     * @parameter expression="${namePattern}"
     *            default-value="[[artifactId]]_[[version]].[[extension]]"
     */
    private String namePattern;

    /**
     * Explicitly defines the final artifact name (without using the pattern)
     * 
     * @parameter expression="${deb}"
     */
    private File deb;
    private String debName;

    /**
     * Explicitly defines the path to the control directory. At least the
     * control file is mandatory.
     * 
     * @parameter expression="${controlDir}"
     */
    private File controlDir;

    /**
     * Explicitly define the file to read the changes from.
     * 
     * @parameter expression="${changesIn}"
     */
    private File changesIn = null;
    private String changesName;

    /**
     * Explicitly define the file where to write the changes to.
     * 
     * @parameter expression="${changesOut}"
     */
    private File changesOut = null;

//  /**
//   * Explicitly define the file where to write the changes of the changes
//   * input to.
//   * 
//   * @parameter expression="${changesSave}"
//   */
//  private File changesSave = null;

//  /**
//   * The keyring file. Usually some/path/secring.gpg
//   * 
//   * @parameter expression="${keyring}"
//   */
//  private File keyring = null;

//  /**
//   * The hex key id to use for signing.
//   * 
//   * @parameter expression="${key}"
//   */
//  private String key = null;

//  /**
//   * The passphrase for the key to sign the changes file.
//   * 
//   * @parameter expression="${passhrase}"
//   */
//  private String passphrase = null;

//  /**
//   * The compression method used for the data file (none, gzip or bzip2)
//   * 
//   * @parameter expression="${compression}" default-value="gzip"
//   */
//  private String compression;

    /**
     * The location where all package files will be installed. By default, all
     * packages are installed in /opt (see the FHS here:
     * http://www.pathname.com/
     * fhs/pub/fhs-2.3.html#OPTADDONAPPLICATIONSOFTWAREPACKAGES)
     * 
     * @parameter expression="${installDir}"
     *            default-value="/opt/${project.artifactId}"
     */
    private String installDir;
    private String openReplaceToken = "[[";
    private String closeReplaceToken = "]]";

    /**
     * The type of attached artifact
     *
     * @parameter default-value="deb"
     */
    private String type;

    /**
     * The classifier of attached artifact
     *
     * @parameter
     */
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
     *         <data>
     *           <src>${project.basedir}/README.txt</src>
     *         </data>
     *         </dataSet>
     *       </configuration>
     *     </plugins>
     *   </build>
     * </pre>
     * 
     * @parameter expression="${dataSet}"
     */
    private Data[] dataSet;
    private Collection dataProducers = new ArrayList();

    public void setData(Data[] pData) {
        dataSet = pData;
        dataProducers.clear();
        if (pData != null) {
            for (int i = 0; i < pData.length; i++) {
                dataProducers.add(pData[i]);
            }
        }
    }

    public void setOpenReplaceToken(String openReplaceToken) {
        this.openReplaceToken = openReplaceToken;
        AbstractDescriptor.setOpenToken(openReplaceToken);
    }

    public void setCloseReplaceToken(String closeReplaceToken) {
        this.closeReplaceToken = closeReplaceToken;
        AbstractDescriptor.setCloseToken(closeReplaceToken);
    }

    protected VariableResolver initializeVariableResolver(Map variables) {
        variables.put("name", getProject().getName());
        variables.put("artifactId", getProject().getArtifactId());
        variables.put("groupId", getProject().getGroupId());
        variables.put("version", getProject().getVersion().replace('-', '+'));
        variables.put("description", getProject().getDescription());
        variables.put("extension", "deb");
        return new MapVariableResolver(variables);
    }

    protected File getDebFile() {
        // if not specified try to the default
        if (deb == null) {
            deb = new File(buildDirectory, debName);
        }
        return deb;
    }

    protected File getControlDir() {
        // if not specified try to the default
        if (controlDir == null) {
            controlDir = new File(getProject().getBasedir(), "src/deb/control");
            getLog().info(
                    "Using default path to control directory " + controlDir);
        }
        return controlDir;
    }

    protected File getControlFile() {
        return new File(controlDir, "control");
    }

    protected String getInstallDir() {
        // if not specified try to the default
        if (installDir == null) {
            installDir = "/opt/" + getProject().getArtifactId();
            getLog().info("Using default path to install directory " + installDir);
        }
        return installDir;
    }

    protected File getChangesInFile() {
        // if not specified try to the default
        if (changesIn == null) {
            final File f = new File(getProject().getBasedir(), "CHANGES.txt");
            if (f.exists() && f.isFile() && f.canRead()) {
                changesIn = f;
            }
        }
        return changesIn;
    }

    protected File getChangesOutFile() {
        // if not specified try to the default
        if (changesOut == null) {
            changesOut = new File(buildDirectory, changesName);
        }
        return changesOut;
    }

    /**
     * Main entry point
     * 
     * @throws MojoExecutionException on error
     */
    public void execute() throws MojoExecutionException {
        Map variables = new HashMap();
        final VariableResolver resolver = initializeVariableResolver(variables);
        try {
            // expand name pattern
            debName = Utils.replaceVariables(resolver, namePattern, openReplaceToken, closeReplaceToken);
            variables.put("extension", "changes");
            changesName = Utils.replaceVariables(resolver, namePattern, openReplaceToken, closeReplaceToken);
        } catch (ParseException e) {
            throw new MojoExecutionException("Failed parsing artifact name pattern", e);
        }

        deb = getDebFile();
        changesIn = getChangesInFile();
        changesOut = getChangesOutFile();
        controlDir = getControlDir();

        setData(dataSet);

        // If there are no dataProducers, then we'll add a single producer that
        // processes the
        // maven artifact file (be it a jar, war, etc.)
        if (dataProducers.isEmpty()) {
            final File file = getProject().getArtifact().getFile();
            dataProducers.add(new DataProducer() {
                public void produce(final DataConsumer receiver) {
                    try {
                        receiver.onEachFile(new FileInputStream(file),
                                new File(new File(getInstallDir()), file.getName()).getAbsolutePath(), "",
                                "root", 0, "root", 0,
                                TarEntry.DEFAULT_FILE_MODE, file.length());
                    } catch (Exception e) {
                        getLog().error(e);
                    }
                }
            });
        }

        Console infoConsole = new Console() {
            public void println(String s) {
                getLog().info(s);
            }
        };
        try {
            DebMaker debMaker = new DebMaker(infoConsole, deb, controlDir, dataProducers, resolver);
            debMaker.makeDeb();
            getLog().info("Attaching created debian archive " + deb);
            projectHelper.attachArtifact(getProject(), type, classifier, deb);
        } catch (PackagingException e) {
            getLog().error("Failed to create debian package " + deb, e);
            throw new MojoExecutionException("Failed to create debian package " + deb, e);
        }
    }
}
