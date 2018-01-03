package org.vafer.jdeb.producers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.vafer.jdeb.DataConsumer;

/**
 * Tests for {@link org.vafer.jdeb.producers.DataProducerFiles}.
 */
public final class DataProducerFilesTestCase extends Assert {

    File file1;
    File file2;

    @Before
    public void setUp() throws Exception {
        file1 = File.createTempFile(getClass().getSimpleName() + ".1", "txt");
        file2 = File.createTempFile(getClass().getSimpleName() + ".2", "txt");
    }

    @After
    public void tearDown() throws Exception {
        file1.delete();
        file2.delete();
    }

    @Test
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
                        public boolean matches(final Object o) {
                            if (!(o instanceof TarArchiveEntry)) {
                                return false;
                            }
                            final TarArchiveEntry e = (TarArchiveEntry) o;
                            return e.getSize() == f.length()
                                && e.getLongGroupId() == 0
                                && e.getLongUserId() == 0
                                && "root".equals(e.getUserName())
                                && "root".equals(e.getGroupName())
                                && ("/usr/include/" + f.getName()).equals(e.getName())
                                   ;
                        }

                        public void describeTo(final Description description) {
                        }
                    })
            );
        }
    }

    @Test
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
                        public boolean matches(final Object o) {
                            if (!(o instanceof TarArchiveEntry)) {
                                return false;
                            }
                            final TarArchiveEntry e = (TarArchiveEntry) o;

                            // Turns out compress is stripping the drive letter
                            // https://github.com/apache/commons-compress/blob/master/src/main/java/org/apache/commons/compress/archivers/tar/TarArchiveEntry.java#L1337
                            // Which is bad - but needs to be fixed there.
                            // We have to work around in the test for now.
                            final String s = f.getAbsolutePath().replace(File.separator, "/").replace("C:/", "/");

                            return e.getSize() == f.length()
                                    && e.getLongGroupId() == 0
                                    && e.getLongUserId() == 0
                                    && "root".equals(e.getUserName())
                                    && "root".equals(e.getGroupName())
                                    && s.equals(e.getName())
                                    ;
                        }

                        public void describeTo(final Description description) {
                        }
                    })
            );
        }
    }
}
