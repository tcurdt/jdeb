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

package org.vafer.jdeb.ant;

import junit.framework.TestCase;
import org.apache.tools.ant.types.selectors.SelectorUtils;

public final class AntSelectorTestCase extends TestCase {

    private boolean isExcluded( String name, String[] excludes ) {
        for (String exclude : excludes) {
            if (SelectorUtils.matchPath(exclude, name)) {
                return true;
            }
        }
        return false;
    }

    public void testExclusion() throws Exception {
      assertTrue("should be excluded", isExcluded("/some/bin/stuff", new String[]{ "**/bin/**" }));
      assertTrue("should not be excluded", !isExcluded("/some/stuff", new String[]{ "**/bin/**" }));
    }
}
