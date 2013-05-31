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

import java.io.DataInput;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import org.apache.sis.internal.storage.ChannelImageInputStream;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link DataStoreConnection}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class DataStoreConnectionTest extends TestCase {
    /**
     * The magic number of Java class files, used for verifying the content of our test file.
     */
    private static final int MAGIC_NUMBER = 0xCAFEBABE;

    /**
     * Creates the instance to test. This method uses the {@code DataStoreConnectionTest} compiled
     * class file as the resource to test. The resource can be provided either as a URL or as a stream.
     */
    private DataStoreConnection create(final boolean asStream) {
        final Class<?> c = DataStoreConnectionTest.class;
        final String name = c.getSimpleName() + ".class";
        final Object storage = asStream ? c.getResourceAsStream(name) : c.getResource(name);
        assertNotNull(storage);
        return new DataStoreConnection(storage);
    }

    /**
     * Tests the {@link DataStoreConnection#getStorageName()} method.
     */
    @Test
    public void testGetStorageName() {
        final DataStoreConnection c = create(false);
        assertEquals("DataStoreConnectionTest.class", c.getStorageName());
    }

    /**
     * Tests the {@link DataStoreConnection#getFileExtension()} method.
     */
    @Test
    public void testGetExtension() {
        final DataStoreConnection c = create(false);
        assertEquals("class", c.getFileExtension());
    }

    /**
     * Tests the {@link DataStoreConnection#openAs(Class)} method for the I/O types.
     * The initial storage object is a {@link java.net.URL}.
     *
     * @throws DataStoreException Should never happen.
     * @throws IOException If an error occurred while reading the test file.
     */
    @Test
    public void testOpenFromURL() throws DataStoreException, IOException {
        testOpenAsDataInput(false);
    }

    /**
     * Tests the {@link DataStoreConnection#openAs(Class)} method for the I/O types.
     * The initial storage object is an {@link java.io.InputStream}.
     *
     * @throws DataStoreException Should never happen.
     * @throws IOException If an error occurred while reading the test file.
     */
    @Test
    public void testOpenFromStream() throws DataStoreException, IOException {
        testOpenAsDataInput(true);
    }

    /**
     * Implementation of {@link #testOpenAsStream()}.
     */
    private void testOpenAsDataInput(final boolean asStream) throws DataStoreException, IOException {
        final DataStoreConnection connection = create(asStream);
        final DataInput input = connection.openAs(DataInput.class);
        assertSame("Value shall be cached.", input, connection.openAs(DataInput.class));
        assertInstanceOf("Needs the SIS implementation", ChannelImageInputStream.class, input);
        final ReadableByteChannel channel = ((ChannelImageInputStream) input).channel;
        /*
         * Reads a single integer for checking that the stream is at the right position,
         * then close the stream.
         */
        assertTrue("channel.isOpen()", channel.isOpen());
        assertEquals(MAGIC_NUMBER, input.readInt());
        connection.closeAllExcept(null);
        assertFalse("channel.isOpen()", channel.isOpen());
    }
}
