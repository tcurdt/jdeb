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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tools.tar.TarEntry;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.Processor;
import org.vafer.jdeb.changes.TextfileChangesProvider;
import org.vafer.jdeb.descriptors.PackageDescriptor;

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

    private File deb;
    private File controlDir;
    
	/**
     * Main entry point
     * @throws MojoExecutionException on error
     */
    public void execute()
        throws MojoExecutionException
    {
    	if (deb == null) {
    		deb = new File(buildDirectory, "target.deb");
    	}
    	
    	if (controlDir == null) {
    		controlDir = new File("src/deb/control");
    	}
    	
    	if (!controlDir.exists() || !controlDir.isDirectory()) {
    		throw new MojoExecutionException(controlDir + " needs to be a directory");
    	}
    	
    	final File file = getProject().getArtifact().getFile();
		final File[] controlFiles = controlDir.listFiles();
		final DataProducer[] data = new DataProducer[] { new DataProducer() {
			public void produce( final DataConsumer receiver ) {
				try {
					receiver.onEachFile(new FileInputStream(file), file.getName(), "", "root", 0, "root", 0, TarEntry.DEFAULT_FILE_MODE, file.length());
				} catch (Exception e) {
					getLog().error(e);
				}
			}			
		}};

		final File changesIn = null;
    	final File changesOut = null;
    	final File keyring = null;
    	final String key = null;
    	final String passphrase = null;
		
		final Processor processor = new Processor(new Console()
		{
			public void println( final String s )
			{
				getLog().info(s);
			}			
		});
		
		try
		{

			final PackageDescriptor packageDescriptor = processor.createDeb(controlFiles, data, deb);

			getLog().info("Attaching created debian archive " + deb);
			projectHelper.attachArtifact( getProject(), "deb-archive", deb.getName(), deb );

			if (changesOut != null)
			{
				// for now only support reading the changes form a textfile provider
				final TextfileChangesProvider changesProvider = new TextfileChangesProvider(new FileInputStream(changesIn), packageDescriptor);
				
				processor.createChanges(packageDescriptor, changesProvider, (keyring!=null)?new FileInputStream(keyring):null, key, passphrase, new FileOutputStream(changesOut));

				// write the release information to this file
				changesProvider.save(new FileOutputStream(changesIn));
				
				getLog().info("Attaching created debian changes file " + changesOut);
				projectHelper.attachArtifact( getProject(), "deb-changes", changesOut.getName(), changesOut );
			}			
		}
		catch (Exception e)
		{
			getLog().error("Failed to create debian package" + deb, e);
		}    	
    }    

}
