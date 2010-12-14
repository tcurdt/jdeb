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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
     * substitutions are [[baseDir]] [[buildDir]] [[artifactId]] [[version]]
     * [[extension]] and [[groupId]].
     * 
     * @parameter default-value="[[buildDir]]/[[artifactId]]_[[version]].[[extension]]"
     */
    private String deb;

    /**
     * Explicitly defines the path to the control directory. At least the
     * control file is mandatory.
     * 
     * @parameter default-value="[[baseDir]]/src/deb/control"
     */
    private String controlDir;

    /**
     * Explicitly define the file to read the changes from.
     * 
     * @parameter default-value="[[baseDir]]/CHANGES.txt"
     */
    private String changesIn;

    /**
     * Explicitly define the file where to write the changes to.
     * 
     * @parameter default-value="[[baseDir]]/CHANGES.txt"
     */
    private String changesOut;

    /**
     * Explicitly define the file where to write the changes of the changes
     * input to.
     * 
     * @parameter default-value="[[baseDir]]/CHANGES.txt"
     */
    private String changesSave;

    /**
     * The compression method used for the data file (none, gzip or bzip2)
     * 
     * @parameter default-value="gzip"
     */
    private String compression;

    /**
     * The location where all package files will be installed. By default, all
     * packages are installed in /opt (see the FHS here:
     * http://www.pathname.com/
     * fhs/pub/fhs-2.3.html#OPTADDONAPPLICATIONSOFTWAREPACKAGES)
     * 
     * @parameter default-value="/opt/[[artifactId]]"
     */
    private String installDir;

    
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
     * with the regex we'll split the version string into three:
     * version.major version.minor version.build
     * 
     * @parameter default-value="[.-]"
     */
    private String splitVersionRegexString;

    /**
     * version mask builder
     *
     * @parameter default-value="[[version.major]].[[version.minor]]+[[version.build]]"
     */
    private String versionMask;

    /**
     * Version replacement, in case version.build have splitter elements will replace it to
     * this char
     * 
     * @parameter default-value="+"
     */
    private String versionReplacementString;

    private String generatedVersion;

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
    
    /* end of parameters */
    
    private String openReplaceToken = "[[";
    private String closeReplaceToken = "]]";
    private Collection dataProducers = new ArrayList();

    public void setOpenReplaceToken(String openReplaceToken) {
        this.openReplaceToken = openReplaceToken;
        AbstractDescriptor.setOpenToken(openReplaceToken);
    }

    public void setCloseReplaceToken(String closeReplaceToken) {
        this.closeReplaceToken = closeReplaceToken;
        AbstractDescriptor.setCloseToken(closeReplaceToken);
    }

    protected void setData(Data[] pData) {
        dataSet = pData;
        dataProducers.clear();
        if (pData != null) {
            dataProducers.addAll(Arrays.asList(pData));
        }
    }

    protected VariableResolver initializeVariableResolver(Map variables) {
        variables.put("name", getProject().getName());
        variables.put("artifactId", getProject().getArtifactId());
        variables.put("groupId", getProject().getGroupId());
        variables.put("version", generatedVersion);
        variables.put("description", getProject().getDescription());
        variables.put("extension", "deb");
        variables.put("baseDir", getProject().getBasedir().getAbsolutePath());
        variables.put("buildDir", buildDirectory.getAbsolutePath());
        return new MapVariableResolver(variables);
    }

    /**
     * Main entry point
     * 
     * @throws MojoExecutionException on error
     */
    public void execute() throws MojoExecutionException {

        setData(dataSet);

        try {
            initGeneratedVesion();
            final VariableResolver resolver = initializeVariableResolver(new HashMap());
            
            final File debFile = new File(Utils.replaceVariables(resolver, deb, openReplaceToken, closeReplaceToken)); 
            final File controlDirFile = new File(Utils.replaceVariables(resolver, controlDir, openReplaceToken, closeReplaceToken)); 
            final File installDirFile = new File(Utils.replaceVariables(resolver, installDir, openReplaceToken, closeReplaceToken));            
            final File changesInFile = new File(Utils.replaceVariables(resolver, changesIn, openReplaceToken, closeReplaceToken));
            final File changesOutFile = new File(Utils.replaceVariables(resolver, changesOut, openReplaceToken, closeReplaceToken));
            final File changesSaveFile = new File(Utils.replaceVariables(resolver, changesSave, openReplaceToken, closeReplaceToken));
            
            // If there are no dataProducers, then we'll add a single producer that
            // processes the
            // maven artifact file (be it a jar, war, etc.)
            if (dataProducers.isEmpty()) {
                final File file = getProject().getArtifact().getFile();
                dataProducers.add(new DataProducer() {
                    public void produce(final DataConsumer receiver) {
                        try {
                            receiver.onEachFile(new FileInputStream(file),
                                    new File(installDirFile, file.getName()).getAbsolutePath(), "",
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

                DebMaker debMaker = new DebMaker(infoConsole, debFile, controlDirFile, dataProducers, resolver);
                
                if (changesInFile.exists() && changesInFile.canRead()) {
                    debMaker.setChangesIn(changesInFile);
                    debMaker.setChangesOut(changesOutFile);
                    debMaker.setChangesSave(changesSaveFile);               
                }
                
                debMaker.setCompression(compression);
                debMaker.makeDeb();
    
                getLog().info("Attaching created debian archive " + debFile);
                projectHelper.attachArtifact(getProject(), type, classifier, debFile);

            } catch (PackagingException e) {
                getLog().error("Failed to create debian package " + debFile, e);
                throw new MojoExecutionException("Failed to create debian package " + debFile, e);
            }

        } catch (ParseException e) {
            throw new MojoExecutionException("Failed parsing pattern", e);
        }
    }

    private static final String VERSION_MAJOR="version.major";
    private static final String VERSION_MINOR="version.minor";
    private static final String VERSION_BUILD="version.build";


    /**
     * Calculate current version according to version mask
     * In case not found will replace field with zero
     */
    private void initGeneratedVesion() {
        if(this.splitVersionRegexString==null || this.splitVersionRegexString.length()==0){
            getLog().error("Cannot init mojo, please provide splitVersionRegexString!");
            throw new IllegalStateException("Cannot init mojo, please provide splitVersionRegexString!");
        }

        if(this.versionMask==null || this.versionMask.length()==0){
            getLog().error("Cannot init mojo, please provide versionMask!");
            throw new IllegalStateException("Cannot init mojo, please provide versionMask!");
        }



        try {
            Pattern pt=Pattern.compile(this.splitVersionRegexString);
            String[] versionArray=pt.split(getProject().getVersion(),3);
            if(getLog().isDebugEnabled()){
                getLog().debug("Version splitted into:"+Arrays.toString(versionArray));
            }
            //Cannot be null we'll get at least single element array
            String versionMajor=versionArray[0];
            if(versionArray.length==0){
                versionMajor="0";
            }

            String versionMinor;
            if(versionArray.length>1){
                versionMinor=versionArray[1];
                if(versionArray.length==0){
                    versionMinor="0";
                }
            } else {
                versionMinor="0";
            }

            String versionBuild;
            if(versionArray.length>2){
                versionBuild=versionArray[2];
                if(versionArray.length==0){
                    versionBuild="0";
                }
            } else {
                versionBuild="0";
            }

            Map<String,String> replacementMap=new HashMap();
            replacementMap.put(VERSION_MAJOR, versionMajor);
            replacementMap.put(VERSION_MINOR, versionMinor);
            replacementMap.put(VERSION_BUILD, pt.matcher(versionBuild).replaceAll(versionReplacementString));
            VariableResolver resolver=new MapVariableResolver(replacementMap);
            this.generatedVersion=Utils.replaceVariables(resolver,
                    this.versionMask, openReplaceToken, closeReplaceToken);

        } catch (ParseException ex) {
            getLog().error("Cannot init mojo, invalid version mask!",ex);
            throw new IllegalStateException("Cannot init mojo, invalid version mask!",ex);
        } catch(PatternSyntaxException e){
            getLog().error("Cannot init mojo, invalid regex pattern in splitVersionRegexString!",e);
            throw new IllegalStateException("Cannot init mojo, invalid pattern in splitVersionRegexString!",e);
        }
    }
}
