/*
 * Copyright 2025 The jdeb developers.
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
import org.apache.maven.model.Organization;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.vafer.jdeb.utils.MapVariableResolver.MapVariableResolverBuilder;

public final class MapVariableResolverTestCase extends Assert {

    @Test(expected = IllegalStateException.class)
    public void builderThrowsWithMissingMavenProject() throws Exception {
        MapVariableResolverBuilder builder = MapVariableResolver.builder();

        // Mock Maven Project
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        File expectedBaseDir = FileUtils.getTempDirectory();
        Mockito.when(mockMavenProject.getBasedir()).thenReturn(expectedBaseDir);

        Properties mockMavenProperties = new Properties();
        Mockito.when(mockMavenProject.getProperties()).thenReturn(mockMavenProperties);

        // Mock System properties
        Properties mockSystemProperties = new Properties();

        builder
            .withName("Name")
            .withVersion("2.0.0")
            .withSystemProperties(mockSystemProperties)
            .withBuildDirectory("aDirectory")
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void builderThrowsWithMissingSystemProperties() throws Exception {
        MapVariableResolverBuilder builder = MapVariableResolver.builder();

        // Mock Maven Project
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        File expectedBaseDir = FileUtils.getTempDirectory();
        Mockito.when(mockMavenProject.getBasedir()).thenReturn(expectedBaseDir);

        Properties mockMavenProperties = new Properties();
        Mockito.when(mockMavenProject.getProperties()).thenReturn(mockMavenProperties);

        builder
            .withName("Name")
            .withVersion("2.0.0")
            .withMavenProject(mockMavenProject)
            .withBuildDirectory("aDirectory")
            .build();
    }

    @Test
    public void builderWorksWithNoData() throws Exception {
        MapVariableResolverBuilder builder = MapVariableResolver.builder();

        // Mock Maven Project
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        File expectedBaseDir = FileUtils.getTempDirectory();
        Mockito.when(mockMavenProject.getBasedir()).thenReturn(expectedBaseDir);

        Properties mockMavenProperties = new Properties();
        Mockito.when(mockMavenProject.getProperties()).thenReturn(mockMavenProperties);

        // Mock System properties
        Properties mockSystemProperties = new Properties();

        // Builder
        MapVariableResolver resolver = builder
            .withName("test")
            .withVersion("2.0.0")
            .withMavenProject(mockMavenProject)
            .withSystemProperties(mockSystemProperties)
            .withBuildDirectory("aDirectory")
            .build();

        assertEquals(null, resolver.get("artifactId"));
        assertEquals(expectedBaseDir.getAbsolutePath(), resolver.get("baseDir"));
        assertEquals("aDirectory", resolver.get("buildDir"));
        assertEquals(null, resolver.get("description"));
        assertEquals("deb", resolver.get("extension"));
        assertEquals(null, resolver.get("groupId"));
        assertEquals("test", resolver.get("name"));
        assertEquals(null, resolver.get("project.version"));
        assertEquals(null, resolver.get("url"));
        assertEquals("2.0.0", resolver.get("version"));
    }

    @Test
    public void builderWorksWithAllMandatoryData() throws Exception {
        MapVariableResolverBuilder builder = MapVariableResolver.builder();

        // Mock Maven Project
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        File expectedBaseDir = FileUtils.getTempDirectory();
        Mockito.when(mockMavenProject.getBasedir()).thenReturn(expectedBaseDir);

        LinkedHashMap<String, String> mavenMap = new LinkedHashMap<String, String>();
        mavenMap.put("maven.compiler.source", "1.8");
        mavenMap.put("maven.compiler.target", "1.8");
        mavenMap.put("property.one", "one");
        mavenMap.put("meta.property", "${property.one}-${property.two}-${property.three}");
        mavenMap.put("nested.meta.property", "${property.one}+${meta.property}");

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
        systemMap.put("meta.property.system", "${property.three}");
        systemMap.put("property.three", "three");
        systemMap.put("property.two", "two");
        systemMap.put("java.specification.version", "17");

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
        assertEquals("anAwesomeArtifactId", resolver.get("artifactId"));
        assertEquals(expectedBaseDir.getAbsolutePath(), resolver.get("baseDir"));
        assertEquals("aDirectory", resolver.get("buildDir"));
        assertEquals("anAwesomeDescription", resolver.get("description"));
        assertEquals("deb", resolver.get("extension"));
        assertEquals("anAwesomeGroupId", resolver.get("groupId"));
        assertEquals("17", resolver.get("java.specification.version"));
        assertEquals("1.8", resolver.get("maven.compiler.source"));
        assertEquals("1.8", resolver.get("maven.compiler.target"));
        assertEquals("one-two-three", resolver.get("meta.property"));
        assertEquals("test", resolver.get("name"));
        assertEquals("one+one-two-three", resolver.get("nested.meta.property"));
        assertEquals("2.0.0", resolver.get("project.version"));
        assertEquals("one", resolver.get("property.one"));
        assertEquals("three", resolver.get("property.three"));
        assertEquals("two", resolver.get("property.two"));
        assertEquals("three", resolver.get("meta.property.system"));
        assertEquals("https://github.com/tcurdt/jdeb", resolver.get("url"));
        assertEquals("2.0.0", resolver.get("version"));
    }

    @Test
    public void builderWorksWithOptionalData() throws Exception {
        MapVariableResolverBuilder builder = MapVariableResolver.builder();

        // Mock Maven Project
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        File expectedBaseDir = FileUtils.getTempDirectory();
        Mockito.when(mockMavenProject.getBasedir()).thenReturn(expectedBaseDir);

        Properties mockMavenProperties = new Properties();
        Mockito.when(mockMavenProject.getProperties()).thenReturn(mockMavenProperties);

        Mockito.when(mockMavenProject.getArtifactId()).thenReturn("anAwesomeArtifactId");
        Mockito.when(mockMavenProject.getGroupId()).thenReturn("anAwesomeGroupId");
        Mockito.when(mockMavenProject.getDescription()).thenReturn("anAwesomeDescription");
        Mockito.when(mockMavenProject.getVersion()).thenReturn("2.0.0");
        Mockito.when(mockMavenProject.getUrl()).thenReturn("https://github.com/tcurdt/jdeb");

        // Optional info
        Mockito.when(mockMavenProject.getInceptionYear()).thenReturn("1990");

        Organization mockOrganization = new Organization();
        mockOrganization.setName("anAwesomeOrganization");
        mockOrganization.setUrl("https://www.awesome.org");
        Mockito.when(mockMavenProject.getOrganization()).thenReturn(mockOrganization);

        // Mock System Properties
        Properties mockSystemProperties = new Properties();

        // Builder
        MapVariableResolver resolver = builder
            .withName("test")
            .withVersion("2.0.0")
            .withMavenProject(mockMavenProject)
            .withSystemProperties(mockSystemProperties)
            .withBuildDirectory("aDirectory")
            .build();

        // Expected Resolver
        assertEquals("anAwesomeArtifactId", resolver.get("artifactId"));
        assertEquals(expectedBaseDir.getAbsolutePath(), resolver.get("baseDir"));
        assertEquals("aDirectory", resolver.get("buildDir"));
        assertEquals("anAwesomeDescription", resolver.get("description"));
        assertEquals("deb", resolver.get("extension"));
        assertEquals("anAwesomeGroupId", resolver.get("groupId"));
        assertEquals("test", resolver.get("name"));
        assertEquals("2.0.0", resolver.get("project.version"));
        assertEquals("https://github.com/tcurdt/jdeb", resolver.get("url"));
        assertEquals("2.0.0", resolver.get("version"));
        assertEquals("1990", resolver.get("project.inceptionYear"));
        assertEquals("anAwesomeOrganization", resolver.get("project.organization.name"));
        assertEquals("https://www.awesome.org", resolver.get("project.organization.url"));
    }
}
