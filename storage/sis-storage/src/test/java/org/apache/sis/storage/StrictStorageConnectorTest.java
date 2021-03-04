package org.apache.sis.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.StrictStorageConnector.StorageControlException;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@DependsOn(org.apache.sis.storage.StorageConnectorTest.class)
public class StrictStorageConnectorTest {
    /**
     * Name of the test file, in the same directory than this {@code StorageConnectorTest} file.
     */
    private static final String FILENAME = "Any.txt";

    /**
     * Creates the instance to test. This method uses the {@code "test.txt"} ASCII file as
     * the resource to test. The resource can be provided either as a URL or as a stream.
     */
    private static StrictStorageConnector create(final boolean asStream) {
        final Class<?> c = StorageConnectorTest.class;
        final Object storage = asStream ? c.getResourceAsStream(FILENAME) : c.getResource(FILENAME);
        assertNotNull(storage);
        final StorageConnector unsafeConnector = new StorageConnector(storage);
        unsafeConnector.setOption(OptionKey.ENCODING, StandardCharsets.US_ASCII);
        unsafeConnector.setOption(OptionKey.URL_ENCODING, "UTF-8");
        final StrictStorageConnector connector = new StrictStorageConnector(unsafeConnector);
        return connector;
    }

    private static byte[] getFileBytes() throws URISyntaxException, IOException {
        final Path filePath = Paths.get(StorageConnector.class.getResource(FILENAME).toURI());
        return Files.readAllBytes(filePath);
    }

    @Test
    public void acquiring_path_works() {
        final StrictStorageConnector connector = create(false);
        assertTrue(connector.getPath().isPresent());
        assertTrue(connector.getURI().isPresent());
        assertTrue(connector.getPathAsString().isPresent());
        assertFalse(connector.getSQLDatasource().isPresent());
    }

    @Test
    public void stream_based_connector_return_empty_path() {
        final StrictStorageConnector connector = create(true);
        assertFalse(connector.getPath().isPresent());
        assertFalse(connector.getURI().isPresent());
        assertFalse(connector.getPathAsString().isPresent());
        assertFalse(connector.getSQLDatasource().isPresent());
    }

    @Test
    public void byte_buffer_is_rewind_after_use() throws Exception {
        final byte[] ctrl = getFileBytes();
        try (final StrictStorageConnector connector = create(false)) {
            // Mess with internal buffer
            connector.useAsBuffer(buffer -> {
                // mess with it
                return buffer.get(new byte[10]);
            });
            // ensure it has been properly rewind
            connector.useAsBuffer(buffer -> {
                assertEquals(0, buffer.position());
                byte[] readValue = new byte[buffer.remaining()];
                buffer.get(readValue);
                assertArrayEquals(ctrl, readValue);
                return null;
            });
        }
    }

    @Test
    public void fail_fast_when_user_corrupts_stream_mark() throws IOException, DataStoreException {
        try (final StrictStorageConnector c = create(false)) {
            try {
                c.useAsImageInputStream(stream -> {
                    stream.skipBytes(1);
                    stream.mark();
                    return 0;
                });
                fail("We should have detected something has gone wrong");
            } catch (StorageControlException e) {
                // Expected behavior: connector has detected that rewind did not work properly.
            }
        }
    }

    @Test
    public void no_concurrency_allowed() throws Exception {
        try (final StrictStorageConnector c = create(false)) {
            synchronized (c) {
            new Thread(() -> {
                try {
                    c.useAsBuffer(buffer -> {
                        synchronized (c) {
                            try {
                                c.notifyAll();
                                c.wait(1000);
                            } catch (InterruptedException e) {
                                // Do not matter here.
                            }
                        }
                        return null;
                    });

                } catch (IOException | DataStoreException e) {
                    // Do not matter here.
                }
            }).start();

            // Ensure above operation is
                c.wait(100);
            }

            try {
                c.useAsBuffer(buffer -> null);
                fail("Concurrency error should have been raised.");
            } catch (ConcurrentReadException e) {
                // Expected behavior: fail-fast to prevent concurrency.
            }
            synchronized (c) {
                c.notifyAll();
            }
        }
    }

    @Test
    public void commit_close_all_resources_but_chosen() throws Exception {
        final InputStream is;
        try (final StrictStorageConnector c = create(false)) {

            is = c.commit(InputStream.class);

            try {
                c.useAsBuffer(buffer -> null);
                fail("connector should be closed");
            } catch (IllegalStateException e) {
                // Expected behavior
                try {
                    is.read();
                } catch (IOException bis) {
                    fail("We queried for the input stream to stay open.");
                }
            }
        }

        try ( final InputStream close = is ) {
            is.read();
        } catch (IOException e) {
            fail("Committed storage view should still be opened.");
        }
    }

    @Test
    public void closing_multiple_times_causes_no_error() throws Exception {
        try ( StrictStorageConnector c = create(true) ) {

            c.commit(InputStream.class);
            c.closeAllExcept(null);
        }
    }
}
