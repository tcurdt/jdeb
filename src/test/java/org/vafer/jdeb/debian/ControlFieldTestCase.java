/*
 * Copyright 2015 The jdeb developers.
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

package org.vafer.jdeb.debian;

import junit.framework.TestCase;

public class ControlFieldTestCase extends TestCase {

    public void testFormatSimpleValue() {
        ControlField field = new ControlField("Field-Name");
        
        assertEquals("Field-Name: value\n", field.format("value"));
    }
    
    public void testFormatMultilineValue1() {
        ControlField field = new ControlField("Field-Name", false, ControlField.Type.MULTILINE);
        
        assertEquals("Field-Name: value1\n value2\n .\n value3\n", field.format("value1\nvalue2\n\nvalue3"));
    }
    
    public void testFormatMultilineValue2() {
        ControlField field = new ControlField("Field-Name", false, ControlField.Type.MULTILINE, true);
        
        assertEquals("Field-Name:\n value1\n value2\n .\n value3\n", field.format("value1\nvalue2\n\nvalue3"));
    }
}
