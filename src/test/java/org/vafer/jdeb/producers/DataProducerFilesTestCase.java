package org.vafer.jdeb.producers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.vafer.jdeb.DataConsumer;

/**
 * Tests for {@link org.vafer.jdeb.producers.DataProducerFiles}.
 *
 * @author Roman Kashitsyn
 */
public class DataProducerFilesTestCase extends TestCase {
    File file1;
    File file2;

    public void setUp() throws Exception {
        file1 = File.createTempFile(getClass().getSimpleName() + ".1", "txt");
        file2 = File.createTempFile(getClass().getSimpleName() + ".2", "txt");
    }

    public void tearDown() throws Exception {
        file1.delete();
        file2.delete();
    }

    public void testProducesMultiplePaths() throws IOException {
        DataConsumer consumer = mock(DataConsumer.class);
        new DataProducerFiles(
                new String[]{
                        file1.getAbsolutePath(),
                        file2.getAbsolutePath()
                },
                "/usr/include",
                null
        ).produce(consumer);

        for (File f : Arrays.asList(file1, file2)) {
            verify(consumer).onEachFile(
                    any(FileInputStream.class),
                    eq("/usr/include/" + f.getName()),
                    any(String.class),
                    eq("root"),
                    eq(0),
                    eq("root"),
                    eq(0),
                    anyInt(),
                    eq(f.length())
            );
        }
    }

    public void testProducesMultiplePathsNoDestination() throws IOException {
        DataConsumer consumer = mock(DataConsumer.class);
        new DataProducerFiles(
                new String[]{
                        file1.getAbsolutePath(),
                        file2.getAbsolutePath()
                },
                null,
                null
        ).produce(consumer);

        for (File f : Arrays.asList(file1, file2)) {
            verify(consumer).onEachFile(
                    any(FileInputStream.class),
                    eq(new TarArchiveEntry(f.getAbsolutePath(), true).getName()),
                    any(String.class),
                    eq("root"),
                    eq(0),
                    eq("root"),
                    eq(0),
                    anyInt(),
                    eq(f.length())
            );
        }
    }
}
