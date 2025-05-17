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

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.vafer.jdeb.utils.MapVariableResolver.MapVariableResolverBuilder;

public final class MapVariableResolverTestCase extends Assert {

    @Test
    public void test() throws Exception {
        MapVariableResolverBuilder builder = MapVariableResolver.builder();

        // Mock Maven Project
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        File expectedBaseDir = FileUtils.getTempDirectory();
        Mockito.when(mockMavenProject.getBasedir()).thenReturn(expectedBaseDir);

        LinkedHashMap<String, String> mavenMap = new LinkedHashMap<String, String>();
        mavenMap.put("maven.compiler.source", "1.8");
        mavenMap.put("maven.compiler.target", "1.8");
        mavenMap.put("property.package.revision", "1");
        mavenMap.put("property.project.version", "${release.version}-1");
        mavenMap.put("property.upstream.version", "2.0.0-SNAPSHOT");
        mavenMap.put("release.version", "2.0.0");

        Properties mockMavenProperties = new Properties();
        mockMavenProperties.putAll(mavenMap);
        Mockito.when(mockMavenProject.getProperties()).thenReturn(mockMavenProperties);

        Mockito.when(mockMavenProject.getArtifactId()).thenReturn("anAwesomeArtifactId");
        Mockito.when(mockMavenProject.getGroupId()).thenReturn("anAwesomeGroupId");
        Mockito.when(mockMavenProject.getDescription()).thenReturn("anAwesomeDescription");
        Mockito.when(mockMavenProject.getVersion()).thenReturn("2.0.0");
        Mockito.when(mockMavenProject.getUrl()).thenReturn("https://github.com/tcurdt/jdeb");

        // Mock System Properties
        LinkedHashMap<String, String> systemMap = new LinkedHashMap<String, String>();
        // TODO

        Properties mockSystemProperties = new Properties();
        mockSystemProperties.putAll(systemMap);

        // Builder
        MapVariableResolver resolver = builder
            .withName("test")
            .withVersion("2.0.0")
            .withMavenProject(mockMavenProject)
            .withSystemProperties(mockSystemProperties)
            .withBuildDirectory("aDirectory")
            .build();

        // Expected Resolver
        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("artifactId", "anAwesomeArtifactId");
        expectedMap.put("baseDir", expectedBaseDir.getAbsolutePath());
        expectedMap.put("buildDir", "aDirectory");
        expectedMap.put("description", "anAwesomeDescription");
        expectedMap.put("extension", "deb");
        expectedMap.put("groupId", "anAwesomeGroupId");
        expectedMap.put("maven.compiler.source", "1.8");
        expectedMap.put("maven.compiler.target", "1.8");
        expectedMap.put("name", "test");
        expectedMap.put("project.version", "2.0.0");
        expectedMap.put("property.package.revision", "1");
        expectedMap.put("property.project.version", "2.0.0-1");
        expectedMap.put("property.upstream.version", "2.0.0-SNAPSHOT");
        expectedMap.put("release.version", "2.0.0");
        expectedMap.put("url", "https://github.com/tcurdt/jdeb");
        expectedMap.put("version", "2.0.0");

        assertTrue(String.format("Expected:\n%s\nFound:\n%s", expectedMap, resolver.getMap()), expectedMap.equals(resolver.getMap()));
    }
}
