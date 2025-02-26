/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage;

import java.net.URI;
import java.io.DataInput;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.Reader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.channels.ReadableByteChannel;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.sql.Connection;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelImageInputStream;
import org.apache.sis.storage.internal.InputStreamAdapter;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;


/**
 * Tests {@link StorageConnector}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class StorageConnectorTest extends TestCase {
    /**
     * Name of the test file, in the same directory as this {@code StorageConnectorTest} file.
     */
    private static final String FILENAME = "Any.txt";

    /**
     * Beginning of the first sentence in {@value #FILENAME}.
     *
     * @see #getFirstExpectedBytes()
     */
    private static final String FIRST_SENTENCE = "The purpose of this file";

    /**
     * The 4 first characters of {@link #FIRST_SENTENCE}, encoded as an integer.
     */
    private static final int MAGIC_NUMBER = ('T' << 24) | ('h' << 16) | ('e' << 8) | ' ';

    /**
     * Creates a new test case.
     */
    public StorageConnectorTest() {
    }

    /**
     * Ensures that the given input stream supports marks. This check is needed because
     * {@link Class#getResourceAsStream(String)} may return different classes of stream
     * depending on the context. For example on Java 20 with observed that the resource
     * from unnamed module was returned as {@link BufferedInputStream}, while the same
     * resource was returned as {@code sun.nio.ch.ChannelInputStream} after the module
     * became a named one.
     *
     * @param  in  the input stream to check.
     * @return the input stream with mark support.
     */
    private static InputStream withMarkSupport(InputStream in) {
        if (in != null && !in.markSupported()) {
            in = new BufferedInputStream(in);
        }
        return in;
    }

    /**
     * Creates the instance to test. This method uses the {@code "Any.txt"} ASCII file as
     * the resource to test. The resource can be provided either as a URL or as a stream.
     */
    static StorageConnector create(final boolean asStream) {
        final Class<?> c = StorageConnectorTest.class;
        final Object storage = asStream ? withMarkSupport(c.getResourceAsStream(FILENAME)) : c.getResource(FILENAME);
        assertNotNull(storage);
        final StorageConnector connector = new StorageConnector(storage);
        connector.setOption(OptionKey.ENCODING, StandardCharsets.US_ASCII);
        connector.setOption(OptionKey.URL_ENCODING, "UTF-8");
        return connector;
    }

    /**
     * Returns the first bytes expected to be found in the {@value #FILENAME} file.
     */
    static byte[] getFirstExpectedBytes() {
        return FIRST_SENTENCE.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Reads the first bytes from the given input stream and verifies
     * that they are equal to the expected content.
     */
    static void assertExpectedBytes(final InputStream stream) throws IOException {
        final byte[] expected = getFirstExpectedBytes();
        final byte[] content = new byte[expected.length];
        assertEquals(content.length, stream.read(content));
        assertArrayEquals(expected, content);
    }

    /**
     * Reads the first characters from the given reader and verifies
     * that they are equal to the expected content.
     */
    static void assertExpectedChars(final Reader stream) throws IOException {
        final char[] expected = FIRST_SENTENCE.toCharArray();
        final char[] content = new char[expected.length];
        assertEquals(content.length, stream.read(content));
        assertArrayEquals(expected, content);
    }

    /**
     * Tests the {@link StorageConnector#getStorageName()} method.
     */
    @Test
    public void testGetStorageName() {
        final StorageConnector c = create(false);
        assertEquals(FILENAME, c.getStorageName());
    }

    /**
     * Tests the {@link StorageConnector#getFileExtension()} method.
     */
    @Test
    public void testGetExtension() {
        final StorageConnector c = create(false);
        assertEquals("txt", c.getFileExtension());
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link String} type.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException should never happen since we do not open any file.
     */
    @Test
    public void testGetAsString() throws DataStoreException, IOException {
        final StorageConnector c = create(false);
        assertTrue(c.getStorageAs(String.class).endsWith("org/apache/sis/storage/" + FILENAME));
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the I/O types.
     * The initial storage object is a {@link java.net.URL}.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testGetAsDataInputFromURL() throws DataStoreException, IOException {
        testGetAsDataInput(false);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the I/O types.
     * The initial storage object is an {@link java.io.InputStream}.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testGetAsDataInputFromStream() throws DataStoreException, IOException {
        testGetAsDataInput(true);
    }

    /**
     * Implementation of {@link #testGetAsDataInputFromURL()} and {@link #testGetAsDataInputFromStream()}.
     */
    private void testGetAsDataInput(final boolean asStream) throws DataStoreException, IOException {
        final StorageConnector connector = create(asStream);
        assertEquals(asStream, connector.getStorageAs(URI.class)  == null);
        assertEquals(asStream, connector.getStorageAs(Path.class) == null);
        final ChannelDataInput input = assertInstanceOf(ChannelDataInput.class,
                connector.getStorageAs(DataInput.class), "Needs the SIS implementation.");
        assertSame(input, connector.getStorageAs(DataInput.class), "Value shall be cached.");
        assertSame(input, connector.getStorageAs(ChannelDataInput.class), "Instance shall be shared.");
        /*
         * Reads a single integer for checking that the stream is at the right position, then close the stream.
         * Since the file is a compiled Java class, the integer that we read shall be the Java magic number.
         */
        assertTrue(input.channel.isOpen(), "channel.isOpen()");
        assertEquals(MAGIC_NUMBER, input.readInt(), "First 4 bytes");
        connector.closeAllExcept(null);
        assertFalse(input.channel.isOpen(), "channel.isOpen()");
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link ImageInputStream} type.
     * This is basically a synonymous of {@code getStorageAs(DataInput.class)}.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testGetAsImageInputStream() throws DataStoreException, IOException {
        final StorageConnector connector = create(false);
        final ImageInputStream in = connector.getStorageAs(ImageInputStream.class);
        assertSame(connector.getStorageAs(ChannelImageInputStream.class), in);
        assertSame(connector.getStorageAs(DataInput.class), in);
        connector.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link ChannelImageInputStream} type.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testGetAsChannelImageInputStream() throws DataStoreException, IOException {
        final StorageConnector connector = create(false);
        final ChannelImageInputStream in = connector.getStorageAs(ChannelImageInputStream.class);
        assertSame(connector.getStorageAs(ImageInputStream.class), in);
        assertSame(connector.getStorageAs(DataInput.class), in);
        connector.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link InputStream} type.
     * The {@code InputStream} was specified directly to the {@link StorageConnector} constructor.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testGetOriginalInputStream() throws DataStoreException, IOException {
        final StorageConnector connector = create(true);
        final InputStream in = connector.getStorageAs(InputStream.class);
        assertSame(connector.getStorage(), in, "The InputStream shall be the one specified to the constructor.");
        /*
         * Ask a different type and request a few bytes. We do not test the ImageInputStream type here as this is
         * not the purpose of this method. But we need a different type before to request again the InputStream.
         */
        final ImageInputStream data = connector.getStorageAs(ImageInputStream.class);
        final byte[] sample = new byte[32];
        data.readFully(sample);
        /*
         * Request again the InputStream and read the same number of bytes than above. The intent of this test
         * is to verify that StorageConnector has reset the InputStream position before to return it.
         * Note that this test requires InputStream implementations supporting mark/reset operations
         * (which is the case when the resource is an ordinary file, not an entry inside a JAR file),
         * otherwise the call to connector.getStorageAs(…) throws a DataStoreException.
         */
        assertTrue(in.markSupported(), "Cannot use a JAR file entry for this test.");
        assertSame(in, connector.getStorageAs(InputStream.class));
        final byte[] actual = new byte[sample.length];
        assertEquals(actual.length, in.read(actual), "Should read all requested bytes.");
        assertArrayEquals(sample, actual, "InputStream shall be reset to the beginning of the stream.");
        connector.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link InputStream} type.
     * The {@code InputStream} was specified as a URL.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testGetAsInputStream() throws DataStoreException, IOException {
        final StorageConnector connector = create(false);
        final InputStream in = connector.getStorageAs(InputStream.class);
        assertNotSame(connector.getStorage(), in);
        assertSame(in, connector.getStorageAs(InputStream.class), "Expected cached value.");

        final ImageInputStream input = assertInstanceOf(InputStreamAdapter.class, in).input;
        assertSame(input, connector.getStorageAs(DataInput.class));
        assertSame(input, connector.getStorageAs(ImageInputStream.class));

        final ReadableByteChannel channel = assertInstanceOf(ChannelImageInputStream.class, input).channel;
        assertTrue(channel.isOpen());
        connector.closeAllExcept(null);
        assertFalse(channel.isOpen());
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link Reader} type.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testGetAsReader() throws DataStoreException, IOException {
        final StorageConnector connector = create(true);
        final Reader in = connector.getStorageAs(Reader.class);
        in.mark(1000);
        assertExpectedChars(in);
        assertSame(in, connector.getStorageAs(Reader.class), "Expected cached value.");
        in.reset();
        /*
         * Open as an ImageInputStream and verify that reading starts from the beginning.
         * This operation should force StorageConnector to discard the previous Reader.
         */
        final ImageInputStream im = connector.getStorageAs(ImageInputStream.class);
        assertInstanceOf(ChannelImageInputStream.class, im, "Needs the SIS implementation.");
        im.mark();
        assertEquals(MAGIC_NUMBER, im.readInt(), "First 4 bytes");
        im.reset();
        /*
         * Get a reader again. It should be a new one, in order to read from the beginning again.
         */
        final Reader in2 = connector.getStorageAs(Reader.class);
        assertNotSame(in, in2, "Expected a new Reader instance.");
        assertExpectedChars(in2);
        assertSame(in2, connector.getStorageAs(Reader.class), "Expected cached value.");
        connector.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link ChannelDataInput} type.
     * The initial value should not be an instance of {@link ChannelImageInputStream} in order to avoid initializing
     * the Image I/O classes. However, after a call to {@code getStorageAs(ChannelImageInputStream.class)}, the type
     * should have been promoted.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testGetAsChannelDataInput() throws DataStoreException, IOException {
        final StorageConnector connector = create(true);
        final ChannelDataInput input = connector.getStorageAs(ChannelDataInput.class);
        assertFalse(input instanceof ChannelImageInputStream);
        assertEquals(MAGIC_NUMBER, input.buffer.getInt());
        /*
         * Get as an image input stream and ensure that the cached value has been replaced.
         */
        final ChannelImageInputStream stream = assertInstanceOf(ChannelImageInputStream.class,
                connector.getStorageAs(ImageInputStream.class), "Needs the SIS implementation");
        assertNotSame(input, stream, "Expected a new instance.");
        assertSame(input.channel, stream.channel, "Shall share the channel.");
        assertSame(input.buffer,  stream.buffer,  "Shall share the buffer.");
        assertSame(stream, connector.getStorageAs(ChannelDataInput.class), "Cached valud shall have been replaced.");
        connector.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link ByteBuffer} type.
     * This method uses the same test file as {@link #testGetAsDataInputFromURL()}.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testGetAsByteBuffer() throws DataStoreException, IOException {
        final StorageConnector connector = create(false);
        final ByteBuffer buffer = connector.getStorageAs(ByteBuffer.class);
        assertNotNull(buffer, "getStorageAs(ByteBuffer.class)");
        assertEquals(StorageConnector.DEFAULT_BUFFER_SIZE, buffer.capacity());
        assertEquals(MAGIC_NUMBER, buffer.getInt());
        connector.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link ByteBuffer} type when
     * the buffer is only temporary. The difference between this test and {@link #testGetAsByteBuffer()} is
     * that the buffer created in this test will not be used for the "real" reading process in the data store.
     * Consequently, it should be a smaller, only temporary, buffer.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testGetAsTemporaryByteBuffer() throws DataStoreException, IOException {
        StorageConnector connector = create(true);
        final DataInput in = ImageIO.createImageInputStream(connector.getStorage());
        assertNotNull(in, "ImageIO.createImageInputStream(InputStream)");                   // Sanity check.
        connector = new StorageConnector(in);
        assertSame(in, connector.getStorageAs(DataInput.class));

        final ByteBuffer buffer = connector.getStorageAs(ByteBuffer.class);
        assertNotNull(buffer, "getStorageAs(ByteBuffer.class)");
        assertEquals(StorageConnector.MINIMAL_BUFFER_SIZE, buffer.capacity());
        assertEquals(MAGIC_NUMBER, buffer.getInt());
        connector.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link Connection} type.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException should never happen since we do not open any file.
     */
    public void testGetAsConnection() throws DataStoreException, IOException {
        final StorageConnector connector = create(false);
        assertNull(connector.getStorageAs(Connection.class));
        connector.closeAllExcept(null);
    }

    /**
     * Verifies that {@link StorageConnector#getStorageAs(Class)} returns {@code null} for unavailable
     * target classes, and throws an exception for illegal target classes.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     */
    @Test
    public void testGetInvalidObject() throws DataStoreException {
        final StorageConnector connector = create(true);
        assertNotNull(connector.getStorageAs(InputStream.class));
        assertNull   (connector.getStorageAs(URI.class));
        assertNull   (connector.getStorageAs(String.class));

        var exception = assertThrows(UnconvertibleObjectException.class,
                () -> connector.getStorageAs(Float.class), "Should not accept Float.class");
        assertMessageContains(exception, "Float");
        connector.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#closeAllExcept(Object)} method.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testCloseAllExcept() throws DataStoreException, IOException {
        final StorageConnector connector = create(true);
        final ChannelDataInput input = connector.getStorageAs(ChannelDataInput.class);
        assertTrue(input.channel.isOpen());
        connector.closeAllExcept(input);
        assertTrue(input.channel.isOpen());
        input.channel.close();      // No "try-with-resource" for easier debugging if needed.
    }

    /**
     * Tests the {@link StorageConnector#commit(Class, String)} method.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testCommit() throws DataStoreException, IOException {
        final StorageConnector connector = create(false);
        final InputStream stream = connector.commit(InputStream.class, "Test");

        var exception = assertThrows(IllegalStateException.class,
                () -> connector.getStorageAs(ByteBuffer.class), "Connector should be closed.");
        assertMessageContains(exception);
        assertExpectedBytes(stream);
        stream.close();                 // No "try-with-resource" for easier debugging if needed.
    }

    /**
     * Verifies that the {@link StorageConnector#closeAllExcept(Object)} method is idempotent
     * (i.e. calls after the first call have no effect).
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while closing the test file.
     */
    @Test
    public void testCloseIsIdempotent() throws DataStoreException, IOException {
        final StorageConnector connector = StorageConnectorTest.create(true);
        final InputStream stream = connector.commit(InputStream.class, "Test");
        connector.closeAllExcept(null);
        assertExpectedBytes(stream);
        stream.close();                 // No "try-with-resource" for easier debugging if needed.
    }
}
