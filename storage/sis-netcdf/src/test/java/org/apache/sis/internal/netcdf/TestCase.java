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
package org.apache.sis.internal.netcdf;

import java.util.Date;
import java.util.Map;
import java.util.EnumMap;
import java.util.Iterator;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.netcdf.ucar.DecoderWrapper;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.event.StoreListeners;
import org.opengis.test.dataset.TestData;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFile;
import org.junit.AfterClass;

import static org.junit.Assert.*;


/**
 * Base class of netCDF tests. The base class uses the UCAR decoder, which is taken as a reference implementation.
 * Subclasses testing Apache SIS implementation needs to override the {@link #createDecoder(TestData)}.
 *
 * <p>This class is <strong>not</strong> thread safe - do not run subclasses in parallel.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.3
 * @module
 */
public abstract strictfp class TestCase extends org.apache.sis.test.TestCase {
    /**
     * The {@code searchPath} argument value to be given to the {@link Decoder#setSearchPath(String[])}
     * method when the decoder shall search only in global attributes.
     */
    private static final String[] GLOBAL = new String[1];

    /**
     * The decoders cached by {@link #selectDataset(TestData)}.
     * The map is non-empty only during the execution of a single test class.
     *
     * @see #closeAllDecoders()
     */
    private static final Map<TestData,Decoder> DECODERS = new EnumMap<>(TestData.class);

    /**
     * The decoder to test, which is set by {@link #selectDataset(TestData)}.
     * This field must be set before any {@code assert} method is invoked.
     */
    private Decoder decoder;

    /**
     * Creates a new test case.
     */
    protected TestCase() {
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
        return NetcdfFile.openInMemory(location, file.content());
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
        return new DecoderWrapper(new NetcdfDataset(createUCAR(file)), GeometryLibrary.JAVA2D, createListeners());
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
                super(null, false);
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
        synchronized (DECODERS) {               // Paranoiac safety, but should not be used in multi-threads environment.
            decoder = DECODERS.get(name);
            if (decoder == null) {
                decoder = createDecoder(name);
                assertNotNull(decoder);
                assertNull(DECODERS.put(name, decoder));
            }
            decoder.setSearchPath(GLOBAL);
            return decoder;                     // Reminder: Decoder instances are not thread-safe.
        }
    }

    /**
     * Returns the decoder being tested.
     */
    final Decoder decoder() {
        return decoder;
    }

    /**
     * Invoked after all tests in a class have been executed.
     * This method closes all netCDF files.
     *
     * @throws IOException if an error occurred while closing a file.
     */
    @AfterClass
    public static void closeAllDecoders() throws IOException {
        Throwable failure = null;
        synchronized (DECODERS) {               // Paranoiac safety.
            final Iterator<Decoder> it = DECODERS.values().iterator();
            while (it.hasNext()) {
                final Decoder decoder = it.next();
                try {
                    decoder.close();
                } catch (Throwable e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
                it.remove();
            }
            assertTrue(DECODERS.isEmpty());
        }
        /*
         * If we failed to close a file, propagates the error
         * only after we have closed all other files.
         */
        if (failure != null) {
            if (failure instanceof IOException) {
                throw (IOException) failure;
            }
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            if (failure instanceof Error) {
                throw (Error) failure;
            }
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
        assertEquals(attributeName, expected, decoder.stringValue(attributeName));
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
        assertEquals(attributeName, expected, decoder.numericValue(attributeName));
    }

    /**
     * Asserts that the temporal value of the named attribute is equal to the expected value.
     * The {@link #selectDataset(TestData)} method must be invoked at least once before this method.
     *
     * @param  expected       the expected attribute value.
     * @param  attributeName  the name of the attribute to test.
     * @throws IOException if an error occurred while reading the netCDF file.
     */
    protected final void assertAttributeEquals(final Date expected, final String attributeName) throws IOException {
        assertEquals(attributeName, expected, decoder.dateValue(attributeName));
    }
}
