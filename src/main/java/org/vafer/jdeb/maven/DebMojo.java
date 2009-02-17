/*
 * Copyright 2005 The Apache Software Foundation.
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
import java.io.FileOutputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tools.tar.TarEntry;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.Processor;
import org.vafer.jdeb.changes.TextfileChangesProvider;
import org.vafer.jdeb.descriptors.PackageDescriptor;
import org.vafer.jdeb.utils.MapVariableResolver;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * Creates deb archive
 *
 * @goal deb
 * @requiresDependencyResolution compile
 * @execute phase="package"
 */
public final class DebMojo extends AbstractPluginMojo {

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Defines the pattern of the name of final artifacts.
     * Possible substitutions are [[artifactId]] [[version]] [[extension]] and [[groupId]].
     * 
     * @parameter expression="${namePattern}" default-value="[[artifactId]]_[[version]].[[extension]]"
     */
    private String namePattern;

    /**
     * Explicitly defines the final artifact name (without using the pattern) 
     * 
     * @parameter expression="${deb}"
     */    
    private File deb;
    
    /**
     * Explicitly defines the path to the control directory. At least the control file is mandatory. 
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

    /**
     * Explicitly define the file where to write the changes to. 
     * 
     * @parameter expression="${changesIn}"
     */    
    private File changesOut = null;

    /**
     * Explicitly define the file where to write the changes of the changes input to. 
     * 
     * @parameter expression="${changesSave}"
     */    
    private File changesSave = null;
    
    /**
     * The keyring file. Usually some/path/secring.gpg
     * 
     * @parameter expression="${keyring}"
     */     
    private File keyring = null;

    /**
     * The hex key id to use for signing. 
     * 
     * @parameter expression="${key}"
     */     
    private String key = null;

    /**
     * The passphrase for the key to sign the changes file. 
     * 
     * @parameter expression="${passhrase}"
     */     
    private String passphrase = null;
    
    /**
     * If not defaultPath is specified this  
     * 
     * @parameter expression="${defaultPath}" default-value="/srv/jetty/www"
     */     
    private String defaultPath = "/srv/jetty/www";

    /**
     * TODO: make configurable
     */
    private DataProducer[] dataProducers = null;
    
    
    /**
     * Main entry point
     * @throws MojoExecutionException on error
     */
    public void execute()
        throws MojoExecutionException
    {
        // expand name pattern
        final String debName;
        final String changesName;       

        final Map variables = new HashMap();
        variables.put("name", getProject().getName());
        variables.put("artifactId", getProject().getArtifactId());
        variables.put("groupId", getProject().getGroupId());
        variables.put("version", getProject().getVersion().replace('-', '+'));
        variables.put("description", getProject().getDescription());
        variables.put("extension", "deb");      
        final VariableResolver resolver = new MapVariableResolver(variables);

        try
        {
            debName = Utils.replaceVariables(resolver, namePattern, "[[", "]]"); 
            
            variables.put("extension", "changes");      
            changesName = Utils.replaceVariables(resolver, namePattern, "[[", "]]"); 
        }
        catch (ParseException e)
        {
            throw new MojoExecutionException("Failed parsing artifact name pattern", e);
        }

        // if not specified try to the default
        if (deb == null)
        {
            deb = new File(buildDirectory, debName);
        }

        // if not specified try to the default
        if (changesIn == null)
        {
            final File f = new File(getProject().getBasedir(), "CHANGES.txt");
            if (f.exists() && f.isFile() && f.canRead())
            {
                changesIn = f;
            }
        }
        
        // if not specified try to the default
        if (changesOut == null)
        {
            changesOut = new File(buildDirectory, changesName);
        }
        
        // if not specified try to the default
        if (controlDir == null)
        {
            controlDir = new File(getProject().getBasedir(), "src/deb/control");
            getLog().info("Using default path to control directory " + controlDir);
        }
        
        // make sure we have at least the mandatory control directory
        if (!controlDir.exists() || !controlDir.isDirectory())
        {
            throw new MojoExecutionException(controlDir + " needs to be a directory");
        }
        
        // make sure we have at least the mandatory control file
        final File controlFile = new File(controlDir, "control");
        if (!controlFile.exists() || !controlFile.isFile() || !controlFile.canRead())
        {
            throw new MojoExecutionException(controlFile + " is mandatory");
        }
        
        final File file = getProject().getArtifact().getFile();
        final File[] controlFiles = controlDir.listFiles();
        
        
        if (dataProducers == null)
        {
            dataProducers = new DataProducer[] { new DataProducer() {
            public void produce( final DataConsumer receiver ) {
                try {
                    receiver.onEachFile(new FileInputStream(file), new File(new File(defaultPath), file.getName()).getAbsolutePath(), "", "root", 0, "root", 0, TarEntry.DEFAULT_FILE_MODE, file.length());
                } catch (Exception e) {
                    getLog().error(e);
                }
            }}};
        }

        
        final Processor processor = new Processor(
                new Console()
                {
                    public void println( final String s )
                    {
                        getLog().info(s);
                    }           
                },
                resolver
        );
        
        final PackageDescriptor packageDescriptor;
        try
        {

            packageDescriptor = processor.createDeb(controlFiles, dataProducers, deb, "gzip");

            getLog().info("Attaching created debian archive " + deb);
            projectHelper.attachArtifact( getProject(), "deb-archive", deb.getName(), deb );
        }
        catch (Exception e)
        {
            getLog().error("Failed to create debian package " + deb, e);
            throw new MojoExecutionException("Failed to create debian package " + deb, e);
        }       

        if (changesIn == null)
        {
            return;
        }
        
        
        final TextfileChangesProvider changesProvider;
        
        try
        {

            // for now only support reading the changes form a text file provider
            changesProvider = new TextfileChangesProvider(new FileInputStream(changesIn), packageDescriptor);
            
            processor.createChanges(packageDescriptor, changesProvider, (keyring!=null)?new FileInputStream(keyring):null, key, passphrase, new FileOutputStream(changesOut));

            getLog().info("Attaching created debian changes file " + changesOut);
            projectHelper.attachArtifact( getProject(), "deb-changes", changesOut.getName(), changesOut );
        }
        catch (Exception e)
        {
            getLog().error("Failed to create debian changes file " + changesOut, e);
            throw new MojoExecutionException("Failed to create debian changes file " + changesOut, e);
        }       

    
        try {
            if (changesSave == null) {
                return;
            }

            changesProvider.save(new FileOutputStream(changesSave));

            getLog().info("Saved release information to file " + changesSave);
            
        } catch (Exception e) {
            getLog().error("Failed to save release information to file " + changesSave);
            throw new MojoExecutionException("Failed to save release information to file " + changesSave, e);
        }    
    }    

}
