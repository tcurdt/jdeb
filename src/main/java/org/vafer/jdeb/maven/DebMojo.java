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
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.Processor;
import org.vafer.jdeb.changes.TextfileChangesProvider;
import org.vafer.jdeb.descriptors.PackageDescriptor;

public final class DebMojo extends AbstractPluginMojo {

	/**
     * @component
     */
    private MavenProjectHelper projectHelper;

	/**
     * Main entry point
     * @throws MojoExecutionException on error
     */
    public void execute()
        throws MojoExecutionException
    {

    	final File changesIn = null;
    	final File changesOut = null;
    	final File keyring = null;
    	final String key = null;
    	final String passphrase = null;
		final File deb = null;
		final File[] controlFiles = null;		
		final DataProducer[] data = null;
		
		final Processor processor = new Processor(new Console()
		{
			public void println(String s)
			{
				getLog().debug(s);
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
			getLog().error("Failed to create debian package " + e);
			e.printStackTrace();
		}    	
    }    

}
