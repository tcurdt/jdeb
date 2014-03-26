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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.vafer.jdeb.apt.AptComponent;
import org.vafer.jdeb.apt.AptConfiguration;
import org.vafer.jdeb.apt.AptDistribution;
import org.vafer.jdeb.apt.AptWriter;

/**
 * Create an APT repository structure
 * 
 * @author Jens Reimann
 * 
 */
@Mojo(name = "apt", requiresProject = false, threadSafe = false)
public class RepositoryMojo extends AbstractMojo {

    /**
     * The source directory
     */
    @Parameter(required = true)
    private File sourceDirectory;

    /**
     * The output directory
     */
    @Parameter(required = true, defaultValue = "${project.build.directory}/apt")
    private File outputDirectory;

    /**
     * The supported architectures
     * <p>
     * File that are <q>all</q> will be registered with all these architectures.
     * </p>
     */
    @Parameter(required = false)
    private Set<String> architectures = new HashSet<String>(Arrays.asList("i386", "amd64"));

    /**
     * The name of the distribution
     */
    @Parameter(required = true, defaultValue = "devel")
    private String distributionName;

    @Parameter
    private String distributionLabel;

    /**
     * The name of the component
     */
    @Parameter(required = true, defaultValue = "main")
    private String componentName;

    @Parameter
    private String componentLabel;

    /**
     * The origin of the repository
     */
    @Parameter
    private String origin;
    
    /**
     * The description of the repository
     */
    @Parameter
    private String description;
    
    /**
     * If verbose is true more build messages are logged.
     */
    @Parameter(defaultValue = "false")
    private boolean verbose;

    public void setArchitectures( Set<String> architectures ) {
        this.architectures = architectures;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        AptConfiguration configuration = new AptConfiguration();

        configuration.setSourceFolder(sourceDirectory);
        configuration.setTargetFolder(outputDirectory);
        configuration.setArchitectures(architectures);

        AptDistribution dist = new AptDistribution();
        dist.setName(distributionName);
        dist.setOrigin(origin);
        dist.setLabel(distributionLabel);
        dist.setDescription(description);

        AptComponent comp = new AptComponent();
        comp.setName(componentName);
        comp.setLabel(componentLabel);
        dist.addComponent(comp);

        configuration.addDistribution(dist);
        
        

        AptWriter writer = new AptWriter(configuration, new MojoConsole(getLog(), verbose));
        try {
            writer.build();
        } catch(Exception e) {
            throw new MojoExecutionException("Failed to create APT repository", e);
        }
    }

}
