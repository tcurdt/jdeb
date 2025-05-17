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
package org.vafer.jdeb.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

/**
 * Resolve variables based on a Map.
 *
 * ATTENTION: don't use outside of jdeb
 */
public final class MapVariableResolver implements VariableResolver {

    private final Map<String, String> map;

    public MapVariableResolver( Map<String, String> map ) {
        this.map = map;
    }

    public String get( String key ) {
        return map.get(key);
    }

    public static MapVariableResolverBuilder builder() {
        return new MapVariableResolverBuilder();
    }

    public static class MapVariableResolverBuilder {
        private String name;
        private MavenProject mavenProject;
        private Properties systemProperties;
        private String buildDirectory;
        private String version;

        public MapVariableResolverBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public MapVariableResolverBuilder withMavenProject(MavenProject project) {
            this.mavenProject = project;
            return this;
        }

        public MapVariableResolverBuilder withSystemProperties(Properties properties) {
            this.systemProperties = properties;
            return this;
        }

        public MapVariableResolverBuilder withBuildDirectory(String directory) {
            this.buildDirectory = directory;
            return this;
        }

        public MapVariableResolverBuilder withVersion(String version) {
            this.version = version;
            return this;
        }

        public MapVariableResolver build() {
            Map<String, String> variables = new HashMap<String, String>() ;

            Map<String, String> combinedProperties = new HashMap<>();
            combinedProperties.putAll((Map) this.mavenProject.getProperties());
            combinedProperties.putAll((Map) this.systemProperties);

            // Expand (interpolate) values using RegexBasedInterpolator
            RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
            for (Map.Entry<String, String> entry : combinedProperties.entrySet()) {
                interpolator.addValueSource(new org.codehaus.plexus.interpolation.MapBasedValueSource(combinedProperties));
                try {
                    String expandedValue = interpolator.interpolate(entry.getValue(), "");
                    variables.put(entry.getKey(), expandedValue);
                } catch (InterpolationException e) {
                    // Fallback to original value if interpolation fails
                    variables.put(entry.getKey(), entry.getValue());
                }
            }

            variables.put("name", this.name != null ? this.name : this.mavenProject.getName());
            variables.put("artifactId", this.mavenProject.getArtifactId());
            variables.put("groupId", this.mavenProject.getGroupId());
            variables.put("version", this.version);
            variables.put("description", this.mavenProject.getDescription());
            variables.put("extension", "deb");
            variables.put("baseDir", this.mavenProject.getBasedir().getAbsolutePath());
            variables.put("buildDir", this.buildDirectory);
            variables.put("project.version", this.mavenProject.getVersion());

            if (this.mavenProject.getInceptionYear() != null) {
                variables.put("project.inceptionYear", this.mavenProject.getInceptionYear());
            }
            if (this.mavenProject.getOrganization() != null) {
                if (this.mavenProject.getOrganization().getName() != null) {
                    variables.put("project.organization.name", this.mavenProject.getOrganization().getName());
                }
                if (this.mavenProject.getOrganization().getUrl() != null) {
                    variables.put("project.organization.url", this.mavenProject.getOrganization().getUrl());
                }
            }

            variables.put("url", this.mavenProject.getUrl());

            return new MapVariableResolver(variables);
        }
    }

}
