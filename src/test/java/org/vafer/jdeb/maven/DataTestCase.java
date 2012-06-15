package org.vafer.jdeb.maven;

import java.io.IOException;

import junit.framework.TestCase;

/*
 * Admittedly not the nicest way to assert that failOnMissingSrc functions. However, the best that can be done without
 * refactoring, mocking, or extending the scope of the test beyond this unit.
 */
public class DataTestCase extends TestCase {

    private Data data;

    @Override
    protected void setUp() throws Exception {
        data = new Data();
    }

    public void testSrcAndPathsNotSetFails() throws IOException {
        RuntimeException expectedException = null;
        try {
            data.setPaths(null);
            data.setSrc(null);
            data.produce(null);
            fail();
        } catch(RuntimeException expected) {
            expectedException = expected;
        }
        assertEquals("src or paths not set", expectedException.getMessage());
    }

    public void testPathsMaySubstituteSrc()throws IOException {
        IOException expectedException = null;
        try {
            data.setPaths(new String[] { "/var/log", "/var/lib" });
            data.setSrc(null);
            data.produce(null);
            fail();
        } catch(IOException expected) {
            expectedException = expected;
        }
        assertTrue(expectedException.getMessage().startsWith("Unknown type "));
    }

}
