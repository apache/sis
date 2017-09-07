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
import java.io.Reader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.channels.ReadableByteChannel;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.ImageIO;
import java.sql.Connection;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.internal.storage.io.ChannelImageInputStream;
import org.apache.sis.internal.storage.io.InputStreamAdapter;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assume.*;
import static org.opengis.test.Assert.*;


/**
 * Tests {@link StorageConnector}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
@DependsOn(org.apache.sis.internal.storage.io.ChannelImageInputStreamTest.class)
public final strictfp class StorageConnectorTest extends TestCase {
    /**
     * Name of the test file, in the same directory than this {@code StorageConnectorTest} file.
     */
    private static final String FILENAME = "Any.txt";

    /**
     * Beginning of the first sentence in {@value #FILENAME}.
     */
    private static final String FIRST_SENTENCE = "The purpose of this file";

    /**
     * The 4 first characters of {@link #FIRST_SENTENCE}, encoded as an integer.
     */
    private static final int MAGIC_NUMBER = ('T' << 24) | ('h' << 16) | ('e' << 8) | ' ';

    /**
     * Creates the instance to test. This method uses the {@code "test.txt"} ASCII file as
     * the resource to test. The resource can be provided either as a URL or as a stream.
     */
    private static StorageConnector create(final boolean asStream) {
        final Class<?> c = StorageConnectorTest.class;
        final Object storage = asStream ? c.getResourceAsStream(FILENAME) : c.getResource(FILENAME);
        assertNotNull(storage);
        final StorageConnector connector = new StorageConnector(storage);
        connector.setOption(OptionKey.ENCODING, StandardCharsets.US_ASCII);
        connector.setOption(OptionKey.URL_ENCODING, "UTF-8");
        return connector;
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
        final StorageConnector connection = create(asStream);
        final DataInput input = connection.getStorageAs(DataInput.class);
        assertSame("Value shall be cached.", input, connection.getStorageAs(DataInput.class));
        assertInstanceOf("Needs the SIS implementation.", ChannelImageInputStream.class, input);
        assertSame("Instance shall be shared.", input, connection.getStorageAs(ChannelDataInput.class));
        /*
         * Reads a single integer for checking that the stream is at the right position, then close the stream.
         * Since the file is a compiled Java class, the integer that we read shall be the Java magic number.
         */
        final ReadableByteChannel channel = ((ChannelImageInputStream) input).channel;
        assertTrue("channel.isOpen()", channel.isOpen());
        assertEquals("First 4 bytes", MAGIC_NUMBER, input.readInt());
        connection.closeAllExcept(null);
        assertFalse("channel.isOpen()", channel.isOpen());
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link ImageInputStream} type.
     * This is basically a synonymous of {@code getStorageAs(DataInput.class)}.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    @DependsOnMethod("testGetAsDataInputFromURL")
    public void testGetAsImageInputStream() throws DataStoreException, IOException {
        final StorageConnector connection = create(false);
        final ImageInputStream in = connection.getStorageAs(ImageInputStream.class);
        assertSame(connection.getStorageAs(DataInput.class), in);
        connection.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link InputStream} type.
     * The {@code InputStream} was specified directly to the {@link StorageConnector} constructor.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    @DependsOnMethod("testGetAsImageInputStream")
    public void testGetOriginalInputStream() throws DataStoreException, IOException {
        final StorageConnector connection = create(true);
        final InputStream in = connection.getStorageAs(InputStream.class);
        assertSame("The InputStream shall be the one specified to the constructor.", connection.getStorage(), in);
        /*
         * Ask a different type and request a few bytes. We do not test the ImageInputStream type here as this is
         * not the purpose of this method. But we need a different type before to request again the InputStream.
         */
        final ImageInputStream data = connection.getStorageAs(ImageInputStream.class);
        final byte[] sample = new byte[32];
        data.readFully(sample);
        /*
         * Request again the InputStream and read the same amount of bytes than above. The intend of this test
         * is to verify that StorageConnector has reseted the InputStream position before to return it.
         * Note that this test requires InputStream implementations supporting mark/reset operations
         * (which is the case when the resource is an ordinary file, not an entry inside a JAR file),
         * otherwise the call to connection.getStorageAs(…) throws a DataStoreException.
         */
        assumeTrue("Can not use a JAR file entry for this test.", in.markSupported());
        assertSame(in, connection.getStorageAs(InputStream.class));
        final byte[] actual = new byte[sample.length];
        assertEquals("Should read all requested bytes.", actual.length, in.read(actual));
        assertArrayEquals("InputStream shall be reseted to the beginning of the stream.", sample, actual);
        connection.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link InputStream} type.
     * The {@code InputStream} was specified as a URL.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    @DependsOnMethod("testGetAsImageInputStream")
    public void testGetAsInputStream() throws DataStoreException, IOException {
        final StorageConnector connection = create(false);
        final InputStream in = connection.getStorageAs(InputStream.class);
        assertNotSame(connection.getStorage(), in);
        assertSame("Expected cached value.", in, connection.getStorageAs(InputStream.class));
        assertInstanceOf("Expected Channel backend.", InputStreamAdapter.class, in);
        final ImageInputStream input = ((InputStreamAdapter) in).input;
        assertInstanceOf("Expected Channel backend.", ChannelImageInputStream.class, input);
        assertSame(input, connection.getStorageAs(DataInput.class));
        assertSame(input, connection.getStorageAs(ImageInputStream.class));

        final ReadableByteChannel channel = ((ChannelImageInputStream) input).channel;
        assertTrue(channel.isOpen());
        connection.closeAllExcept(null);
        assertFalse(channel.isOpen());
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link Reader} type.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    @DependsOnMethod({"testGetAsInputStream", "testGetAsDataInputFromStream"})
    public void testGetAsReader() throws DataStoreException, IOException {
        final StorageConnector connection = create(true);
        final Reader in = connection.getStorageAs(Reader.class);
        final char[] expected = FIRST_SENTENCE.toCharArray();
        final char[] actual = new char[expected.length];
        in.mark(1000);
        assertEquals("Number of characters read.", expected.length, in.read(actual));
        assertArrayEquals("First sentence.", expected, actual);
        assertSame("Expected cached value.", in, connection.getStorageAs(Reader.class));
        in.reset();
        /*
         * Open as an ImageInputStream and verify that reading starts from the beginning.
         * This operation should force StorageConnector to discard the previous Reader.
         */
        final ImageInputStream im = connection.getStorageAs(ImageInputStream.class);
        assertInstanceOf("Needs the SIS implementation.", ChannelImageInputStream.class, im);
        im.mark();
        assertEquals("First 4 bytes", MAGIC_NUMBER, im.readInt());
        im.reset();
        /*
         * Get a reader again. It should be a new one, in order to read from the beginning again.
         */
        final Reader in2 = connection.getStorageAs(Reader.class);
        assertNotSame("Expected a new Reader instance.", in, in2);
        assertEquals("Number of characters read.", expected.length, in.read(actual));
        assertArrayEquals("First sentence.", expected, actual);
        assertSame("Expected cached value.", in2, connection.getStorageAs(Reader.class));
        connection.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link ChannelDataInput} type.
     * The initial value should not be an instance of {@link ChannelImageInputStream} in order to avoid initializing
     * the Image I/O classes. However after a call to {@code getStorageAt(ChannelImageInputStream.class)}, the type
     * should have been promoted.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testGetAsChannelDataInput() throws DataStoreException, IOException {
        final StorageConnector connection = create(true);
        final ChannelDataInput input = connection.getStorageAs(ChannelDataInput.class);
        assertFalse(input instanceof ChannelImageInputStream);
        assertEquals(MAGIC_NUMBER, input.buffer.getInt());
        /*
         * Get as an image input stream and ensure that the cached value has been replaced.
         */
        final DataInput stream = connection.getStorageAs(DataInput.class);
        assertInstanceOf("Needs the SIS implementation", ChannelImageInputStream.class, stream);
        assertNotSame("Expected a new instance.", input, stream);
        assertSame("Shall share the channel.", input.channel, ((ChannelDataInput) stream).channel);
        assertSame("Shall share the buffer.",  input.buffer,  ((ChannelDataInput) stream).buffer);
        assertSame("Cached valud shall have been replaced.", stream, connection.getStorageAs(ChannelDataInput.class));
        connection.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link ByteBuffer} type.
     * This method uses the same test file than {@link #testGetAsDataInputFromURL()}.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    @DependsOnMethod("testGetAsDataInputFromURL")
    public void testGetAsByteBuffer() throws DataStoreException, IOException {
        final StorageConnector connection = create(false);
        final ByteBuffer buffer = connection.getStorageAs(ByteBuffer.class);
        assertNotNull("getStorageAs(ByteBuffer.class)", buffer);
        assertEquals(StorageConnector.DEFAULT_BUFFER_SIZE, buffer.capacity());
        assertEquals(MAGIC_NUMBER, buffer.getInt());
        connection.closeAllExcept(null);
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
    @DependsOnMethod("testGetAsDataInputFromStream")
    public void testGetAsTemporaryByteBuffer() throws DataStoreException, IOException {
        StorageConnector connection = create(true);
        final DataInput in = ImageIO.createImageInputStream(connection.getStorage());
        assertNotNull("ImageIO.createImageInputStream(InputStream)", in);                   // Sanity check.
        connection = new StorageConnector(in);
        assertSame(in, connection.getStorageAs(DataInput.class));

        final ByteBuffer buffer = connection.getStorageAs(ByteBuffer.class);
        assertNotNull("getStorageAs(ByteBuffer.class)", buffer);
        assertEquals(StorageConnector.MINIMAL_BUFFER_SIZE, buffer.capacity());
        assertEquals(MAGIC_NUMBER, buffer.getInt());
        connection.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#getStorageAs(Class)} method for the {@link Connection} type.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException should never happen since we do not open any file.
     */
    public void testGetAsConnection() throws DataStoreException, IOException {
        final StorageConnector connection = create(false);
        assertNull(connection.getStorageAs(Connection.class));
        connection.closeAllExcept(null);
    }

    /**
     * Verifies that {@link StorageConnector#getStorageAs(Class)} returns {@code null} for unavailable
     * target classes, and throws an exception for illegal target classes.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     */
    @Test
    public void testGetInvalidObject() throws DataStoreException {
        final StorageConnector connection = create(true);
        assertNotNull("getStorageAs(InputStream.class)", connection.getStorageAs(InputStream.class));
        assertNull   ("getStorageAs(URI.class)",         connection.getStorageAs(URI.class));
        assertNull   ("getStorageAs(String.class)",      connection.getStorageAs(String.class));
        try {
            connection.getStorageAs(Float.class);       // Any unconvertible type.
            fail("Should not have accepted Float.class");
        } catch (UnconvertibleObjectException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("Float"));
        }
        connection.closeAllExcept(null);
    }

    /**
     * Tests the {@link StorageConnector#closeAllExcept(Object)} method.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    @DependsOnMethod("testGetAsDataInputFromStream")
    public void testCloseAllExcept() throws DataStoreException, IOException {
        final StorageConnector connection = create(true);
        final DataInput input = connection.getStorageAs(DataInput.class);
        final ReadableByteChannel channel = ((ChannelImageInputStream) input).channel;
        assertTrue("channel.isOpen()", channel.isOpen());
        connection.closeAllExcept(input);
        assertTrue("channel.isOpen()", channel.isOpen());
        channel.close();
    }
}
