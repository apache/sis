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
package org.apache.sis.storage.netcdf.base;

import java.util.EnumMap;
import java.util.Iterator;
import java.time.Instant;
import java.time.ZoneOffset;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.NetcdfDataset;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.netcdf.ucar.DecoderWrapper;
import org.apache.sis.temporal.TemporalDate;
import org.apache.sis.setup.GeometryLibrary;

// Test dependencies
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.apache.sis.storage.DataStoreMock;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.dataset.TestData;


/**
 * Base class of netCDF tests. The base class uses the UCAR decoder, which is taken as a reference implementation.
 * Subclasses testing Apache SIS implementation needs to override the {@link #createDecoder(TestData)}.
 *
 * <p>Subclasses must ensure that they do not hold any state, or that they clear the state after each test.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TestCase extends org.apache.sis.test.TestCase {
    /**
     * The {@code searchPath} argument value to be given to the {@link Decoder#setSearchPath(String[])}
     * method when the decoder shall search only in global attributes.
     */
    private static final String[] GLOBAL = new String[1];

    /**
     * The decoders cached by {@link #selectDataset(TestData)}.
     *
     * @see #closeAllDecoders()
     */
    private final EnumMap<TestData,Decoder> decoders;

    /**
     * The decoder to test, which is set by {@link #selectDataset(TestData)}.
     * This field must be set before any {@code assert} method is invoked.
     *
     * @see #decoder()
     * @see #reset()
     */
    private Decoder decoder;

    /**
     * Creates a new test case.
     */
    protected TestCase() {
        decoders = new EnumMap<>(TestData.class);
    }

    /**
     * Creates a netCDF reader from the UCAR library for the specified data set.
     * We use the UCAR library as a reference implementation for the tests.
     *
     * @param  file  the dataset as one of the {@code NETCDF_*} constants.
     * @return the decoder for the specified dataset.
     * @throws IOException if an I/O error occurred while opening the file.
     */
    protected static NetcdfFile createUCAR(final TestData file) throws IOException {
        /*
         * Binary netCDF files need to be read either from a file, or from a byte array in memory.
         * Reading from a file is not possible if the test file is in geoapi-conformance JAR file.
         * But since those test files are less than 15 kilobytes, loading them in memory is okay.
         */
        String location = file.location().toString();
        location = location.substring(location.lastIndexOf('/') + 1);
        return NetcdfFiles.openInMemory(location, file.content());
    }

    /**
     * Invoked when a new {@link Decoder} instance needs to be created for the specified dataset.
     * The {@code file} parameter can be one of the following values:
     *
     * <ul>
     *   <li>{@link TestData#NETCDF_2D_GEOGRAPHIC} — uses a geographic CRS for global data over the world.</li>
     *   <li>{@link TestData#NETCDF_4D_PROJECTED}  — uses a projected CRS with elevation and time.</li>
     * </ul>
     *
     * Default implementation opens the file with UCAR netCDF library and wraps the UCAR object in {@link DecoderWrapper}.
     * We proceeded that way because we use UCAR library as the reference implementation.
     * Subclasses override this method for testing with Apache SIS implementation.
     *
     * @param  file  the dataset as one of the above-cited constants.
     * @return the decoder for the specified dataset.
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    protected Decoder createDecoder(final TestData file) throws IOException, DataStoreException {
        final NetcdfDataset ds = NetcdfDatasets.enhance(createUCAR(file), NetcdfDataset.getDefaultEnhanceMode(), null);
        return new DecoderWrapper(ds, GeometryLibrary.JAVA2D, createListeners());
    }

    /**
     * Creates a dummy set of store listeners.
     * Used only for constructors that require a non-null {@link StoreListeners} instance.
     *
     * @return a dummy set of listeners.
     */
    protected static StoreListeners createListeners() {
        final class DummyResource extends AbstractResource {
            /** Creates a dummy resource without parent. */
            DummyResource() {
                super(null);
            }

            /** Makes listeners accessible to this package. */
            StoreListeners listeners() {
                return listeners;
            }
        }
        return new DummyResource().listeners();
    }

    /**
     * Selects the dataset to use for the tests. If a decoder for the given name has already been
     * opened, then this method returns that decoder. Otherwise a new decoder is created by a call
     * to {@link #createDecoder(TestData)}, then cached.
     *
     * <p>The {@linkplain Decoder#setSearchPath(String[]) search path} of the returned decoder
     * is initialized to the global attributes only.</p>
     *
     * @param  name  the file as one of the constants enumerated in the {@link #createDecoder(TestData)} method.
     * @return the decoder for the given name.
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    protected final Decoder selectDataset(final TestData name) throws IOException, DataStoreException {
        synchronized (decoders) {               // Paranoiac safety, but should not be used in multi-threads environment.
            decoder = decoders.get(name);
            if (decoder == null) {
                decoder = createDecoder(name);
                assertNotNull(decoder);
                assertNull(decoders.put(name, decoder));
            }
            decoder.setSearchPath(GLOBAL);
            return decoder;                     // Reminder: Decoder instances are not thread-safe.
        }
    }

    /**
     * Returns the decoder being tested.
     */
    final Decoder decoder() {
        assertNotNull(decoder);
        return decoder;
    }

    /**
     * Forgets (but do not close) the decoder used by the current test.
     * This method makes {@code this} instance ready for another test
     * method reusing the decoders that are already opened.
     */
    @AfterEach
    public void reset() {
        decoder = null;
    }

    /**
     * Invoked after all tests in a class have been executed.
     * This method closes all netCDF files.
     *
     * @throws IOException if an error occurred while closing a file.
     */
    @AfterAll
    public void closeAllDecoders() throws IOException {
        final var ds = new DataStoreMock("lock");
        Throwable failure = null;
        synchronized (decoders) {               // Paranoiac safety.
            final Iterator<Decoder> it = decoders.values().iterator();
            while (it.hasNext()) {
                final Decoder d = it.next();
                try {
                    d.close(ds);
                } catch (Throwable e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
                it.remove();
            }
            assertTrue(decoders.isEmpty());
        }
        /*
         * If we failed to close a file, propagates the error
         * only after we have closed all other files.
         */
        if (failure != null) {
            if (failure instanceof IOException e) throw e;
            if (failure instanceof RuntimeException e) throw e;
            if (failure instanceof Error e) throw e;
            throw new UndeclaredThrowableException(failure);
        }
    }

    /**
     * Asserts that the textual value of the named attribute is equal to the expected value.
     * The {@link #selectDataset(TestData)} method must be invoked at least once before this method.
     *
     * @param  expected       the expected attribute value.
     * @param  attributeName  the name of the attribute to test.
     * @throws IOException if an error occurred while reading the netCDF file.
     */
    protected final void assertAttributeEquals(final String expected, final String attributeName) throws IOException {
        assertEquals(expected, decoder.stringValue(attributeName), attributeName);
    }

    /**
     * Asserts that the numeric value of the named attribute is equal to the expected value.
     * The {@link #selectDataset(TestData)} method must be invoked at least once before this method.
     *
     * @param  expected       the expected attribute value.
     * @param  attributeName  the name of the attribute to test.
     * @throws IOException if an error occurred while reading the netCDF file.
     */
    protected final void assertAttributeEquals(final Number expected, final String attributeName) throws IOException {
        assertEquals(expected, decoder.numericValue(attributeName), attributeName);
    }

    /**
     * Asserts that the temporal value of the named attribute is equal to the expected value.
     * The {@link #selectDataset(TestData)} method must be invoked at least once before this method.
     *
     * @param  expected       the expected attribute value.
     * @param  attributeName  the name of the attribute to test.
     * @throws IOException if an error occurred while reading the netCDF file.
     */
    protected final void assertAttributeEquals(final Instant expected, final String attributeName) throws IOException {
        assertEquals(expected, TemporalDate.toInstant(decoder.dateValue(attributeName), ZoneOffset.UTC), attributeName);
    }
}
