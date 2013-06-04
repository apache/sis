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
package org.apache.sis.storage.netcdf;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.impl.ChannelDecoder;
import org.apache.sis.internal.netcdf.ucar.DecoderWrapper;
import org.apache.sis.internal.storage.ChannelDataInput;
import org.apache.sis.internal.storage.WarningProducer;
import org.apache.sis.storage.DataStoreConnection;
import org.apache.sis.storage.DataStoreException;


/**
 * The provider of {@link NetcdfStore} instances. Given an {@link DataStoreConnection} input,
 * this class tries to instantiate a {@code NetcdfStore} using the embedded NetCDF decoder.
 * If the embedded decoder can not decode the given input and the UCAR library is reachable
 * on the classpath, then this class tries to instantiate a {@code NetcdfStore} backed by
 * the UCAR library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class NetcdfStoreProvider {
    /**
     * The {@link ucar.nc2.NetcdfFile} class, or {@code null} if not found. An attempt to load this class
     * will be performed when first needed since the UCAR library is optional. If not found, then this field
     * will be assigned the {@link Void#TYPE} sentinel value, meaning "No UCAR library on the classpath".
     */
    private static Class<?> netcdfFileClass;

    /**
     * If the {@link #netcdfFileClass} has been found, then the {@link DecoderWrapper} constructor receiving
     * in argument a UCAR {@code NetcdfFile} object. Otherwise {@code null}.
     */
    private static volatile Constructor<? extends Decoder> fromUCAR;

    /**
     * If the {@link #netcdfFileClass} has been found, then the {@link DecoderWrapper} constructor receiving
     * in argument the name of the NetCDF file as a {@link String} object. Otherwise {@code null}.
     */
    private static volatile Constructor<? extends Decoder> fromFilename;

    /**
     * Creates a new provider.
     */
    public NetcdfStoreProvider() {
    }

    /**
     * Creates a decoder for the given input.
     *
     * @param  sink       Where to send the warnings, or {@code null} if none.
     * @param  connection Information about the input (file, input stream, <i>etc.</i>)
     * @return The decoder for the given input.
     * @throws IOException If an error occurred while opening the NetCDF file.
     * @throws DataStoreException If a logical error (other than I/O) occurred.
     */
    static Decoder decoder(final WarningProducer sink, final DataStoreConnection connection)
            throws IOException, DataStoreException
    {
        final ChannelDataInput input = connection.getStorageAs(ChannelDataInput.class);
        if (input != null) try {
            return new ChannelDecoder(sink, input);
        } catch (DataStoreException e) {
            final String path = connection.getStorageAs(String.class);
            if (path != null) try {
                return createByReflection(sink, path, false);
            } catch (IOException | DataStoreException s) {
                e.addSuppressed(s);
            }
            throw e;
        }
        return createByReflection(sink, connection, true);
    }

    /**
     * Creates a new NetCDF decoder as a wrapper around the UCAR library. This decoder is used only when we can
     * not create our embedded NetCDF decoder. This method uses reflection for creating the wrapper, in order
     * to keep the UCAR dependency optional.
     *
     * @param  sink   Where to send the warnings, or {@code null} if none.
     * @param  input  The NetCDF file object of filename string from which to read data.
     * @param  isUCAR {@code true} if {@code input} is an instance of the UCAR {@link ucar.nc2.NetcdfFile} object,
     *                or {@code false} if it is the filename as a {@code String}.
     * @return The {@link DecoderWrapper} instance for the given input.
     * @throws IOException If an error occurred while opening the NetCDF file.
     * @throws DataStoreException If a logical error (other than I/O) occurred.
     */
    private static Decoder createByReflection(final WarningProducer sink, final Object input, final boolean isUCAR)
            throws IOException, DataStoreException
    {
        /*
         * Get the java.lang.Class that represent the ucar.nc2.NetcdfFile type. We do not synchronize since it
         * is not a big deal if Class.forName(…) is invoked twice. The Class.forName(…) method performs itself
         * the required synchronization for returning the same singleton Class instance.
         */
        if (netcdfFileClass == null) {
            try {
                netcdfFileClass = Class.forName("ucar.nc2.NetcdfFile");
            } catch (ClassNotFoundException e) {
                netcdfFileClass = Void.TYPE;
                return null;
            }
            try {
                final Class<? extends Decoder> wrapper =
                        Class.forName("org.apache.sis.internal.netcdf.ucar.DecoderWrapper").asSubclass(Decoder.class);
                final Class<?>[] parameterTypes = new Class<?>[] {WarningProducer.class, netcdfFileClass};
                fromUCAR = wrapper.getConstructor(parameterTypes);
                parameterTypes[1] = String.class;
                fromFilename = wrapper.getConstructor(parameterTypes);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e); // Should never happen (shall be verified by the JUnit tests).
            }
        }
        /*
         * Get the appropriate constructor for the isUCAR argument. This constructor will be null
         * if the above code failed to load the UCAR library. Otherwise, instantiate the wrapper.
         */
        final Constructor<? extends Decoder> constructor;
        final Class<?> expectedType;
        if (isUCAR) {
            constructor  = fromUCAR;
            expectedType = netcdfFileClass;
        } else {
            constructor  = fromFilename;
            expectedType = String.class;
        }
        if (constructor == null || !expectedType.isInstance(input)) {
            return null;
        }
        try {
            return constructor.newInstance(sink, input);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException)        throw (IOException)        cause;
            if (cause instanceof DataStoreException) throw (DataStoreException) cause;
            if (cause instanceof RuntimeException)   throw (RuntimeException)   cause;
            if (cause instanceof Error)              throw (Error)              cause;
            throw new UndeclaredThrowableException(cause); // Should never happen actually.
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e); // Should never happen (shall be verified by the JUnit tests).
        }
    }
}
