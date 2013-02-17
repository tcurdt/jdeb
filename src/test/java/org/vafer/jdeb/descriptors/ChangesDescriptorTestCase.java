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
package org.vafer.jdeb.descriptors;

import junit.framework.TestCase;
import org.vafer.jdeb.changes.ChangeSet;

public final class ChangesDescriptorTestCase extends TestCase {

    public void testToString() throws Exception {
        final PackageDescriptor descriptor = new PackageDescriptor();
        descriptor.set("Package", "test-package");
        descriptor.set("Description", "This is\na description\non several lines");
        descriptor.set("Version", "1.0");

        final ChangesDescriptor changes = new ChangesDescriptor(descriptor, new ChangeSet[0]);

        assertEquals("1.0", changes.get("Version"));
    }

}
