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
package org.vafer.jdeb.apt;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * An APT repository configuration
 * 
 * @author Jens Reimann
 * 
 */
public class AptConfiguration {
    private File sourceFolder;

    private File targetFolder;

    private Set<AptDistribution> distributions = new HashSet<AptDistribution>();

    private Set<String> architectures = new HashSet<String>(Arrays.asList("i386", "amd64"));

    public AptConfiguration() {
    }

    public AptConfiguration( AptConfiguration other ) {
        this.sourceFolder = other.sourceFolder;
        this.targetFolder = other.targetFolder;
        for (AptDistribution dist : other.distributions) {
            this.distributions.add(new AptDistribution(dist));
        }
        this.architectures = new HashSet<String>(other.architectures);
    }

    public void validate() throws IllegalStateException {
        if (architectures == null || architectures.isEmpty())
            throw new IllegalStateException("Architectures must be set");

        for (String arch : architectures) {
            AptNames.validate("architecture", arch);
        }
    }

    @Override
    protected AptConfiguration clone() {
        return new AptConfiguration(this);
    }

    public File getSourceFolder() {
        return sourceFolder;
    }

    public void setSourceFolder( File sourceFolder ) {
        this.sourceFolder = sourceFolder;
    }

    public File getTargetFolder() {
        return targetFolder;
    }

    public void setTargetFolder( File targetFolder ) {
        this.targetFolder = targetFolder;
    }

    public void setArchitectures( Set<String> architectures ) {
        this.architectures = architectures;
    }

    public Set<String> getArchitectures() {
        return architectures;
    }

    public Set<AptDistribution> getDistributions() {
        return distributions;
    }

    /**
     * Add a new component to the distribution. <br/>
     * The component is copied and cannot be altered after adding
     * 
     * @param dist distribution to add
     */
    public void addDistribution( AptDistribution dist ) {
        this.distributions.add(new AptDistribution(dist));
    }
}
