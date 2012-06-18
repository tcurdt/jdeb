package org.vafer.jdeb.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.vafer.jdeb.Console;

/*
 * Admittedly not the nicest way to assert that failOnMissingSrc functions. However, the best that can be done without
 * refactoring, mocking, or extending the scope of the test beyond this unit.
 */
public class DataTestCase extends TestCase {

    private Data data;
    private File missingFile;
    private File file;
    private CaptureConsole console;

    @Override
    protected void setUp() throws Exception {
        data = new Data();
        missingFile = new File("this-file-does-not-exist");
        file = File.createTempFile(getClass().getSimpleName(), "dat");
        console = new CaptureConsole();
    }

    @Override
    protected void tearDown() throws Exception {
        if (file != null) {
            file.delete();
        }
    }

    public void testFailOnUnknownValue() throws IOException {
        try {
            data.setSrc(missingFile);
            data.setMissingSrc("not a value value");
            data.produce(null, console);
            fail();
        } catch(IllegalArgumentException expected) {
        }
        assertEquals(0, console.warn.size());
    }
    
    public void testFailOnMissingSrcDefaultFileMissing() throws IOException {
        try {
            data.setSrc(missingFile);
            data.produce(null, console);
            fail();
        } catch(FileNotFoundException expected) {
        }
        assertEquals(0, console.warn.size());
    }

    public void testFailOnMissingSrcIgnoreFileMissing() throws IOException {
        data.setSrc(missingFile);
        data.setMissingSrc("ignore");
        data.produce(null, console);
        assertEquals(0, console.warn.size());
    }

    public void testFailOnMissingSrcIgnoreFileMissingVaryInput() throws IOException {
        data.setSrc(missingFile);
        data.setMissingSrc(" IGNORE ");
        data.produce(null, console);
        assertEquals(0, console.warn.size());
    }
    
    public void testFailOnMissingSrcFailFileMissing() throws IOException {
        try {
            data.setSrc(missingFile);
            data.setMissingSrc("fail");
            data.produce(null, console);
            fail();
        } catch(FileNotFoundException expected) {
        }
        assertEquals(0, console.warn.size());
    }

    public void testFailOnMissingSrcDefaultFileExists() throws IOException {
        IOException unknownTypeException = null;
        try {
            data.setSrc(file);
            data.produce(null, console);
        } catch(IOException expected) {
            unknownTypeException = expected;
        }
        assertTrue(unknownTypeException.getMessage().startsWith("Unknown type"));
        assertEquals(0, console.warn.size());
    }

    public void testFailOnMissingSrcIgnoreFileExists() throws IOException {
        IOException unknownTypeException = null;
        try {
            data.setSrc(file);
            data.setMissingSrc("ignore");
            data.produce(null, console);
        } catch(IOException expected) {
            unknownTypeException = expected;
        }
        assertTrue(unknownTypeException.getMessage().startsWith("Unknown type"));
        assertEquals(0, console.warn.size());
    }

    public void testFailOnMissingSrcFailFileExists() throws IOException {
        IOException unknownTypeException = null;
        try {
            data.setSrc(file);
            data.setMissingSrc("fail");
            data.produce(null, console);
        } catch(IOException expected) {
            unknownTypeException = expected;
        }
        assertTrue(unknownTypeException.getMessage().startsWith("Unknown type"));
        assertEquals(0, console.warn.size());
    }

    public void testFailOnMissingSrcWarnFileMissing() throws IOException {
        data.setSrc(missingFile);
        data.setMissingSrc("warn");
        data.produce(null, console);
        assertEquals(1, console.warn.size());
    }

    public void testFailOnMissingSrcWarnFileMissingVaryInput() throws IOException {
        data.setSrc(missingFile);
        data.setMissingSrc(" WARN ");
        data.produce(null, console);
        assertEquals(1, console.warn.size());
    }
    
    private class CaptureConsole implements Console {
        
        private List<String> warn = new ArrayList<String>();

        @Override
        public void info( String message ) {
            throw new RuntimeException("info should never be called");
        }

        @Override
        public void warn( String message ) {
            warn.add(message);
        }
    }

}
