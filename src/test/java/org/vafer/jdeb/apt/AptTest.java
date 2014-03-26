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

import org.apache.commons.io.FileUtils;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.NullConsole;

import junit.framework.TestCase;

/**
 * 
 * @author Jens Reimann
 * 
 */
public class AptTest extends TestCase {
    public void test1() throws Exception {

        AptDistribution dist = new AptDistribution();
        dist.setName("stable");
        dist.setLabel("Label");
        dist.setOrigin("Origin");

        AptComponent comp = new AptComponent();
        dist.setName("main");
        dist.addComponent(comp);

        AptConfiguration configuration = new AptConfiguration();
        configuration.addDistribution(dist);
        File sourceFolder = new File("/tmp/apt.test/input");
        File targetFolder = new File("/tmp/apt.test/output");

        FileUtils.deleteDirectory(targetFolder);

        configuration.setTargetFolder(targetFolder);
        configuration.setSourceFolder(sourceFolder);

        Console console = new NullConsole();
        AptWriter writer = new AptWriter(configuration, console);
        writer.build();
    }
}
