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

import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DataStoreProvider}.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
@DependsOn(StorageConnectorTest.class)
public final strictfp class DataStoreProviderTest extends TestCase {
    /**
     * A dummy provider instance to test. Only the
     * {@link DataStoreProvider#probeContent(StorageConnector, Class, Prober)} method is useful on this instance.
     */
    private final DataStoreProvider provider;

    /**
     * Creates a new test case.
     */
    public DataStoreProviderTest() {
        provider = new DataStoreProvider() {
            @Override public String getShortName() {return "Provider mock";}
            @Override public ParameterDescriptorGroup getOpenParameters() {throw new AssertionError();}
            @Override public ProbeResult probeContent(StorageConnector connector) {throw new AssertionError();}
            @Override public DataStore open(StorageConnector connector) {throw new AssertionError();}
        };
    }

    /**
     * Asserts that probing with {@link InputStream} input gives the expected result.
     */
    private void verifyProbeWithInputStream(final StorageConnector connector) throws DataStoreException {
        assertEquals(ProbeResult.SUPPORTED, provider.probeContent(connector, InputStream.class, stream -> {
            StorageConnectorTest.assertExpectedBytes(stream);
            return ProbeResult.SUPPORTED;
        }));
    }

    /**
     * Asserts that probing with {@link Reader} input gives the expected result.
     */
    private void verifyProbeWithReader(final StorageConnector connector) throws DataStoreException {
        assertEquals(ProbeResult.SUPPORTED, provider.probeContent(connector, Reader.class, stream -> {
            StorageConnectorTest.assertExpectedChars(stream);
            return ProbeResult.SUPPORTED;
        }));
    }

    /**
     * Verifies that the {@link ByteBuffer} given to the {@code Prober} always have the default
     * {@link ByteOrder#BIG_ENDIAN}. Some data store implementations rely on this default value.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     */
    @Test
    public void verifyByteOrder() throws DataStoreException {
        /*
         * Creates a byte buffer with an arbitrary position and byte order.
         */
        final ByteBuffer original = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        original.position(3);
        final StorageConnector connector = new StorageConnector(original);
        /*
         * Verify that the byte buffer given to the prober always have the big endian order,
         * regardless the byte order of the original buffer. This is part of method contract.
         */
        assertEquals(ProbeResult.UNDETERMINED, provider.probeContent(connector, ByteBuffer.class, buffer -> {
            assertEquals(ByteOrder.BIG_ENDIAN, buffer.order());
            assertEquals(3, buffer.position());
            assertEquals(8, buffer.limit());
            buffer.position(5).mark();
            return ProbeResult.UNDETERMINED;
        }));
        /*
         * Verifies that the origial buffer has its byte order and position unchanged.
         */
        assertEquals(ByteOrder.LITTLE_ENDIAN, original.order());
        assertEquals(3, original.position());
    }

    /**
     * Verifies that {@link DataStoreProvider#probeContent(StorageConnector, Class, Prober)}
     * with a {@link ByteBuffer} leaves the position of the original buffer unchanged.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     */
    @Test
    public void testProbeWithByteBuffer() throws DataStoreException {
        /*
         * Change the buffer position for simulating a read operation
         * without resetting the buffer position.
         */
        final StorageConnector connector = StorageConnectorTest.create(false);
        assertEquals(ProbeResult.UNDETERMINED, provider.probeContent(connector, ByteBuffer.class, buffer -> {
            assertEquals(0, buffer.position());
            buffer.position(15).mark();
            return ProbeResult.UNDETERMINED;
        }));
        /*
         * Read again. The buffer position should be the original position
         * (i.e. above call to `position(15)` shall have no effect below).
         */
        assertEquals(ProbeResult.SUPPORTED, provider.probeContent(connector, ByteBuffer.class, buffer -> {
            assertEquals(0, buffer.position());
            final byte[] expected = StorageConnectorTest.getFirstExpectedBytes();
            final byte[] actual = new byte[expected.length];
            buffer.get(actual);
            assertArrayEquals(expected, actual);
            return ProbeResult.SUPPORTED;
        }));
    }

    /**
     * Tests {@link DataStoreProvider#probeContent(StorageConnector, Class, Prober)} with an {@link java.net.URL}.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     */
    @Test
    public void testProbeWithURL() throws DataStoreException {
        testProbeWithInputStream(false);
    }

    /**
     * Verifies that {@link DataStoreProvider#probeContent(StorageConnector, Class, Prober)}
     * with an {@link InputStream} leaves the position of the original stream unchanged.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     */
    @Test
    public void testProbeWithInputStream() throws DataStoreException {
        testProbeWithInputStream(true);
    }

    /**
     * Implementation of {@link #testProbeWithURL()} and {@link #testProbeWithInputStream()}.
     */
    private void testProbeWithInputStream(final boolean asStream) throws DataStoreException {
        /*
         * Read a few bytes and verify that user can not overwrite the mark.
         */
        final StorageConnector connector = StorageConnectorTest.create(asStream);
        assertEquals(ProbeResult.SUPPORTED, provider.probeContent(connector, InputStream.class, stream -> {
            assertEquals(!asStream, stream.markSupported());
            stream.skip(5);
            stream.mark(10);
            stream.skip(4);
            if (asStream) try {
                stream.reset();
                fail("Mark/reset should not be supported.");
            } catch (IOException e) {
                assertTrue(e.getMessage().contains("reset"));
            } else {
                stream.reset();         // Should be supported if opened from URL.
            }
            return ProbeResult.SUPPORTED;
        }));
        /*
         * Read the first bytes and verify that they are really the
         * beginning of the file despite above reading of some bytes.
         */
        verifyProbeWithInputStream(connector);
    }

    /**
     * Verifies that {@link DataStoreProvider#probeContent(StorageConnector, Class, Prober)}
     * with a {@link Reader} leaves the position of the original stream unchanged.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     */
    @Test
    public void testProbeWithReader() throws DataStoreException {
        final StorageConnector connector = testProbeWithReader(false);
        /*
         * Attempt to read with `InputStream` should not be possible because
         * the connector does not know the original stream (we wrapped it).
         */
        try {
            verifyProbeWithInputStream(connector);
            fail("Operation should not be allowed.");
        } catch (ForwardOnlyStorageException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("InputStreamReader"));
        }
    }

    /**
     * Verifies that {@link DataStoreProvider#probeContent(StorageConnector, Class, Prober)}
     * with a {@link BufferedReader} leaves the position of the original stream unchanged.
     *
     * @throws DataStoreException if an error occurred while using the storage connector.
     */
    @Test
    public void testProbeWithBufferedReader() throws DataStoreException {
        final StorageConnector connector = testProbeWithReader(true);
        /*
         * Get the input in another form (InputStream) and verifies that we still
         * have the same first characters. Then try again reading with a `Reader`.
         * The intent is to verify that it is not corrupted by `InputStream` use.
         */
        verifyProbeWithInputStream(connector);
        verifyProbeWithReader(connector);
    }

    /**
     * Implementation of {@link #testProbeWithReader()} and {@link #testProbeWithBufferedReader()}.
     * If {@code buffered} is {@code true}, this method will creates itself a non-buffered reader
     * (because reader created by {@link StorageConnector} are buffered by default).
     *
     * @param  buffered  whether to use a buffered reader.
     * @return the storage connector created by this method, for allowing caller to do more tests.
     */
    private StorageConnector testProbeWithReader(final boolean buffered) throws DataStoreException {
        StorageConnector connector = StorageConnectorTest.create(true);
        if (!buffered) {
            final InputStream stream = (InputStream) connector.getStorage();
            connector = new StorageConnector(new InputStreamReader(stream, StandardCharsets.US_ASCII));
        }
        /*
         * Read a few bytes and verify that user can not overwrite the mark.
         */
        assertEquals(ProbeResult.SUPPORTED, provider.probeContent(connector, Reader.class, stream -> {
            assertEquals(buffered, stream instanceof BufferedReader);
            assertFalse(stream.markSupported());
            stream.skip(5);
            try {
                stream.mark(10);
                fail("Mark/reset should not be supported.");
            } catch (IOException e) {
                assertTrue(e.getMessage().contains("mark"));
            }
            return ProbeResult.SUPPORTED;
        }));
        /*
         * Read the first bytes and verify that they are really the
         * beginning of the file despite above reading of some bytes.
         */
        verifyProbeWithReader(connector);
        return connector;
    }
}
