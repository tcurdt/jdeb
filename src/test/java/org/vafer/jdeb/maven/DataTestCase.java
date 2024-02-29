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
package org.vafer.jdeb.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

/*
 * Admittedly not the nicest way to assert that failOnMissingSrc functions. However, the best that can be done without
 * refactoring, mocking, or extending the scope of the test beyond this unit.
 */
public final class DataTestCase extends Assert {

    private Data data;
    private File missingFile;
    private File file;

    @Before
    public void setUp() throws Exception {
        data = new Data();
        missingFile = new File("this-file-does-not-exist");
        file = File.createTempFile(getClass().getSimpleName(), "dat");
    }

    @After
    public void tearDown() throws Exception {
        if (file != null) {
            file.delete();
        }
    }

    @Test
    public void testFailOnUnknownValue() throws IOException {
        try {
            data.setSrc(missingFile);
            data.setMissingSrc("not a value value");
            data.produce(null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testFailOnMissingSrcDefaultFileMissing() throws IOException {
        try {
            data.setSrc(missingFile);
            data.produce(null);
            fail();
        } catch (FileNotFoundException expected) {
        }
    }

    @Test
    public void testFailOnMissingSrcIgnoreFileMissing() throws IOException {
        data.setSrc(missingFile);
        data.setMissingSrc("ignore");
        data.produce(null);
    }

    @Test
    public void testFailOnMissingSrcIgnoreFileMissingVaryInput() throws IOException {
        data.setSrc(missingFile);
        data.setMissingSrc(" IGNORE ");
        data.produce(null);
    }

    @Test
    public void testFailOnMissingSrcFailFileMissing() throws IOException {
        try {
            data.setSrc(missingFile);
            data.setMissingSrc("fail");
            data.produce(null);
            fail();
        } catch (FileNotFoundException expected) {
        }
    }

    @Test
    public void testFailOnMissingSrcDefaultFileExists() throws IOException {
        IOException unknownTypeException = null;
        try {
            data.setSrc(file);
            data.produce(null);
        } catch (IOException expected) {
            unknownTypeException = expected;
        }
        assertTrue(unknownTypeException.getMessage().startsWith("Unknown type"));
    }

    @Test
    public void testFailOnMissingSrcIgnoreFileExists() throws IOException {
        IOException unknownTypeException = null;
        try {
            data.setSrc(file);
            data.setMissingSrc("ignore");
            data.produce(null);
        } catch (IOException expected) {
            unknownTypeException = expected;
        }
        assertTrue(unknownTypeException.getMessage().startsWith("Unknown type"));
    }

    @Test
    public void testFailOnMissingSrcFailFileExists() throws IOException {
        IOException unknownTypeException = null;
        try {
            data.setSrc(file);
            data.setMissingSrc("fail");
            data.produce(null);
        } catch (IOException expected) {
            unknownTypeException = expected;
        }
        assertTrue(unknownTypeException.getMessage().startsWith("Unknown type"));
    }

}
