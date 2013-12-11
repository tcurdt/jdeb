/*
 * Copyright 2013 The jdeb developers.
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
package org.vafer.jdeb.producers;

import junit.framework.TestCase;

import java.io.File;

import static org.vafer.jdeb.producers.DataProducerManPage.makeDestination;

/**
 * @author Roman Kashitsyn <roman.kashitsyn@gmail.com>
 */
public class DataProducerManPageTest extends TestCase {

    private static final String[][] TEST_TABLE = {
            /* given destination | file name | expected result */
            {null, "page.1", "/usr/share/man/man1/page.1.gz"},
            {"/some/fixed/location", "page.1", "/some/fixed/location"},
            {"/some/fixed/location", "page.1.gz", "/some/fixed/location"},
            {"", "/some/page.2.gz", "/usr/share/man/man2/page.2.gz"},
            {"", "/some/page.7.bz2", "/usr/share/man/man7/page.7.bz2"},
            {"", "page", "/usr/share/man/man1/page.1.gz"},
            {"", "page.-1", "/usr/share/man/man1/page.-1.1.gz"},
            {"", "page.txt", "/usr/share/man/man1/page.txt.1.gz"}
    };

    public void testMakeDestination() throws Exception {
        for (final String[] tuple : TEST_TABLE) {
            final String result = tuple[2];
            assertEquals(result, makeDestination(tuple[0], new File(tuple[1])));
        }
    }
}
