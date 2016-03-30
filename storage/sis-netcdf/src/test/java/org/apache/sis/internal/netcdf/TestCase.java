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
import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.EmptyWarningListeners;
import org.apache.sis.internal.netcdf.ucar.DecoderWrapper;
import org.apache.sis.internal.system.Modules;
import ucar.nc2.dataset.NetcdfDataset;
import org.junit.AfterClass;

import static org.junit.Assert.*;


/**
 * Base class of NetCDF tests. Subclasses shall override the {@link #createDecoder(String)}.
 *
 * <p>This class is <strong>not</strong> thread safe - do not run subclasses in parallel.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract strictfp class TestCase extends IOTestCase {
    /**
     * A dummy list of listeners which can be given to the {@link Decoder} constructor.
     */
    public static EmptyWarningListeners<Decoder> LISTENERS = new EmptyWarningListeners<Decoder>(null, Modules.NETCDF);

    /**
     * The {@code searchPath} argument value to be given to the {@link Decoder#setSearchPath(String[])}
     * method when the decoder shall search only in global attributes.
     */
    private static final String[] GLOBAL = new String[1];

    /**
     * The decoders cached by {@link #selectDataset(String)}.
     */
    private static final Map<String,Decoder> DECODERS = new HashMap<String,Decoder>();

    /**
     * The decoder to test, which is set by {@link #selectDataset(String)}.
     * This field must be set before any {@code assert} method is invoked.
     */
    private Decoder decoder;

    /**
     * Creates a new test case.
     */
    protected TestCase() {
    }

    /**
     * Returns {@code true} if the given supplemental formats (THREDDS, HDF5) is supported.
     * The default implementation returns {@code true} since the UCAR library supports all
     * supplemental formats tested in this suite. Subclasses working only with the NetCDF
     * classic or 64-bits format can unconditionally returns {@code false}.
     *
     * @param  format Either {@code "THREDDS"} or {@code "HDF5"}.
     * @return {@code true} if the given supplemental format is supported.
     */
    protected boolean isSupplementalFormatSupported(final String format) {
        return true;
    }

    /**
     * Invoked when a new {@link Decoder} instance needs to be created for dataset of the given name.
     * The {@code name} parameter can be one of the following values:
     *
     * <ul>
     *   <li>{@link #THREDDS} for a NcML file.</li>
     *   <li>{@link #NCEP}    for a NetCDF binary file.</li>
     *   <li>{@link #CIP}     for a NetCDF binary file.</li>
     *   <li>{@link #LANDSAT} for a NetCDF binary file.</li>
     * </ul>
     *
     * The default implementation first delegates to {@link #open(String)}, then wraps the result
     * in a {@link DecoderWrapper}. We proceeded that way because the UCAR library is used as the
     * reference implementation. However subclasses can override if they want to test a different
     * library.
     *
     * @param  name The file name as one of the above-cited constants.
     * @return The decoder for the given name.
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    protected Decoder createDecoder(final String name) throws IOException, DataStoreException {
        return new DecoderWrapper(LISTENERS, new NetcdfDataset(open(name)));
    }

    /**
     * Selects the dataset to use for the tests. If a decoder for the given name has already been
     * opened, then this method returns that decoder. Otherwise a new decoder is created by a call
     * to {@link #createDecoder(String)}, then cached.
     *
     * <p>The {@linkplain Decoder#setSearchPath(String[]) search path} of the returned decoder
     * is initialized to the global attributes only.</p>
     *
     * @param  name The file name as one of the constants enumerated in the {@link #createDecoder(String)} method.
     * @return The decoder for the given name.
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    protected final Decoder selectDataset(final String name) throws IOException, DataStoreException {
        synchronized (DECODERS) { // Paranoiac safety, but should not be used in multi-threads environment.
            decoder = DECODERS.get(name);
            if (decoder == null) {
                decoder = createDecoder(name);
                assertNotNull(decoder);
                assertNull(DECODERS.put(name, decoder));
            }
            decoder.setSearchPath(GLOBAL);
            return decoder; // Reminder: Decoder instances are not thread-safe.
        }
    }

    /**
     * Invoked after all tests in a class have been executed.
     * This method closes all NetCDF files.
     *
     * @throws IOException if an error occurred while closing a file.
     */
    @AfterClass
    public static void closeAllDecoders() throws IOException {
        Throwable failure = null;
        synchronized (DECODERS) { // Paranoiac safety.
            final Iterator<Decoder> it = DECODERS.values().iterator();
            while (it.hasNext()) {
                final Decoder decoder = it.next();
                try {
                    decoder.close();
                } catch (Throwable e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        // On JDK7 branch: failure.addSuppressed(e);
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
     * Asserts that the textual value of the named attribute is equals to the expected value.
     * The {@link #selectDataset(String)} method must be invoked at least once before this method.
     *
     * @param  expected      The expected attribute value.
     * @param  attributeName The name of the attribute to test.
     * @throws IOException   if an error occurred while reading the NetCDF file.
     */
    protected final void assertAttributeEquals(final String expected, final String attributeName) throws IOException {
        assertEquals(attributeName, expected, decoder.stringValue(attributeName));
    }

    /**
     * Asserts that the numeric value of the named attribute is equals to the expected value.
     * The {@link #selectDataset(String)} method must be invoked at least once before this method.
     *
     * @param  expected      The expected attribute value.
     * @param  attributeName The name of the attribute to test.
     * @throws IOException   If an error occurred while reading the NetCDF file.
     */
    protected final void assertAttributeEquals(final Number expected, final String attributeName) throws IOException {
        assertEquals(attributeName, expected, decoder.numericValue(attributeName));
    }

    /**
     * Asserts that the temporal value of the named attribute is equals to the expected value.
     * The {@link #selectDataset(String)} method must be invoked at least once before this method.
     *
     * @param  expected      The expected attribute value.
     * @param  attributeName The name of the attribute to test.
     * @throws IOException   If an error occurred while reading the NetCDF file.
     */
    protected final void assertAttributeEquals(final Date expected, final String attributeName) throws IOException {
        assertEquals(attributeName, expected, decoder.dateValue(attributeName));
    }
}
