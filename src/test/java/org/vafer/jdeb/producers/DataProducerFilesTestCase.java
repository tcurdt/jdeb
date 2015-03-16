package org.vafer.jdeb.producers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.vafer.jdeb.DataConsumer;

/**
 * Tests for {@link org.vafer.jdeb.producers.DataProducerFiles}.
 */
public class DataProducerFilesTestCase extends TestCase {
    File file1;
    File file2;

    @Override
    public void setUp() throws Exception {
        file1 = File.createTempFile(getClass().getSimpleName() + ".1", "txt");
        file2 = File.createTempFile(getClass().getSimpleName() + ".2", "txt");
    }

    @Override
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

        for (final File f : Arrays.asList(file1, file2)) {
            verify(consumer).onEachFile(
                    any(FileInputStream.class),
                    argThat(new BaseMatcher<TarArchiveEntry>() {
                        @Override
                        public boolean matches(final Object o) {
                            if (!(o instanceof TarArchiveEntry)) {
                                return false;
                            }
                            final TarArchiveEntry e = (TarArchiveEntry) o;
                            return e.getSize() == f.length()
                                && e.getGroupId() == 0
                                && e.getUserId() == 0
                                && "root".equals(e.getUserName())
                                && "root".equals(e.getGroupName())
                                && ("/usr/include/" + f.getName()).equals(e.getName())
                                   ;
                        }

                        @Override
                        public void describeTo(final Description description) {
                        }
                    })
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

        for (final File f : Arrays.asList(file1, file2)) {
            verify(consumer).onEachFile(
                    any(FileInputStream.class),
                    argThat(new BaseMatcher<TarArchiveEntry>() {
                        @Override
                        public boolean matches(final Object o) {
                            if (!(o instanceof TarArchiveEntry)) {
                                return false;
                            }
                            final TarArchiveEntry e = (TarArchiveEntry) o;
                            return e.getSize() == f.length()
                                    && e.getGroupId() == 0
                                    && e.getUserId() == 0
                                    && "root".equals(e.getUserName())
                                    && "root".equals(e.getGroupName())
                                    && (f.getAbsolutePath()).equals(e.getName())
                                    ;
                        }

                        @Override
                        public void describeTo(final Description description) {
                        }
                    })
            );
        }
    }
}
