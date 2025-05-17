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

import java.util.LinkedHashMap;
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

        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        Mockito.when(mockMavenProject.getBasedir()).thenReturn(FileUtils.getTempDirectory());

        // Mock Maven Properties
        LinkedHashMap<String, String> mavenMap = new LinkedHashMap<String, String>();
        mavenMap.put("maven.compiler.target", "1.8");
        mavenMap.put("release.version", "2.0.0");
        mavenMap.put("property.project.version", "${release.version}-1");
        mavenMap.put("property.package.revision", "1");
        mavenMap.put("maven.compiler.source", "1.8");
        mavenMap.put("property.upstream.version", "2.0.0-SNAPSHOT");

        Properties mockMavenProperties = new Properties();
        mockMavenProperties.putAll(mavenMap);

        Mockito.when(mockMavenProject.getProperties()).thenReturn(mockMavenProperties);

        // Mock System Properties
        LinkedHashMap<String, String> systemMap = new LinkedHashMap<String, String>();
        systemMap.put("key", "value");

        Properties mockSystemProperties = new Properties();
        mockSystemProperties.putAll(systemMap);

        // Builder
        VariableResolver resolver = builder
            .withName("test")
            .withVersion("1.0.0")
            .withMavenProject(mockMavenProject)
            .withSystemProperties(mockSystemProperties)
            .withBuildDirectory("aDirectory")
            .build();

        assertEquals("test", resolver.get("name"));
        assertEquals("1.8", resolver.get("maven.compiler.source"));
        assertEquals("2.0.0-1", resolver.get("property.project.version"));

    }
}
