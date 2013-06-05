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
import java.nio.ByteBuffer;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.impl.ChannelDecoder;
import org.apache.sis.internal.netcdf.ucar.DecoderWrapper;
import org.apache.sis.internal.storage.ChannelDataInput;
import org.apache.sis.internal.storage.WarningProducer;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreConnection;
import org.apache.sis.storage.DataStoreException;


/**
 * The provider of {@link NetcdfStore} instances. Given a {@link DataStoreConnection} input,
 * this class tries to instantiate a {@code NetcdfStore} using the embedded NetCDF decoder.
 * If the embedded decoder can not decode the given input and the UCAR library is reachable
 * on the classpath, then this class tries to instantiate a {@code NetcdfStore} backed by
 * the UCAR library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see NetcdfStore
 */
public class NetcdfStoreProvider extends DataStoreProvider {
    /**
     * The name of the {@link ucar.nc2.NetcdfFile} class, which is {@value}.
     */
    private static final String UCAR_CLASSNAME = "ucar.nc2.NetcdfFile";

    /**
     * The {@link ucar.nc2.NetcdfFile} class, or {@code null} if not found. An attempt to load this class
     * will be performed when first needed since the UCAR library is optional. If not found, then this field
     * will be assigned the {@link Void#TYPE} sentinel value, meaning "No UCAR library on the classpath".
     */
    private static Class<?> netcdfFileClass;

    /**
     * If the {@link #netcdfFileClass} has been found, then the {@link ucar.nc2.NetcdfFile#canOpen(String)}
     * static method.
     */
    private static volatile Method canOpenFromPath;

    /**
     * If the {@link #netcdfFileClass} has been found, then the {@link DecoderWrapper} constructor receiving
     * in argument the name of the NetCDF file as a {@link String} object. Otherwise {@code null}.
     */
    private static volatile Constructor<? extends Decoder> createFromPath;

    /**
     * If the {@link #netcdfFileClass} has been found, then the {@link DecoderWrapper} constructor receiving
     * in argument a UCAR {@code NetcdfFile} object. Otherwise {@code null}.
     */
    private static volatile Constructor<? extends Decoder> createFromUCAR;

    /**
     * Creates a new provider.
     */
    public NetcdfStoreProvider() {
    }

    /**
     * Returns {@code TRUE} if the given storage appears to be supported by {@link NetcdfStore}.
     * Returning {@code TRUE} from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the
     * {@linkplain DataStoreConnection#getStorage() storage object} or contents.
     *
     * @param  storage Information about the storage (URL, stream, {@link ucar.nc2.NetcdfFile} instance, <i>etc</i>).
     * @return {@link Boolean#TRUE} if the given storage seems to be usable by the {@code NetcdfStore} instances,
     *         {@link Boolean#FALSE} if {@code NetcdfStore} will not be able to use the given storage,
     *         or {@code null} if this method does not have enough information.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public Boolean canOpen(DataStoreConnection storage) throws DataStoreException {
        final ByteBuffer buffer = storage.getStorageAs(ByteBuffer.class);
        if (buffer != null) {
            if (buffer.remaining() < Integer.SIZE / Byte.SIZE) {
                return null;
            }
            final int header = buffer.getInt(buffer.position());
            if ((header & 0xFFFFFF00) == ChannelDecoder.MAGIC_NUMBER) {
                return Boolean.TRUE;
            }
        }
        /*
         * If we failed to check using the embedded decoder, tries using the UCAR library.
         */
        final String path = storage.getStorageAs(String.class);
        if (path != null) {
            ensureInitialized();
            final Method method = canOpenFromPath;
            if (method != null) try {
                return (Boolean) method.invoke(null, path);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e); // Should never happen, since the method is public.
            } catch (InvocationTargetException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof DataStoreException) throw (DataStoreException) cause;
                if (cause instanceof RuntimeException)   throw (RuntimeException)   cause;
                if (cause instanceof Error)              throw (Error)              cause;
                throw new DataStoreException(e); // The cause may be IOException.
            }
        }
        /*
         * Check if the given input is itself an instance of the UCAR oject.
         * We check classnames instead of netcdfFileClass.isInstance(storage)
         * in order to avoid loading the UCAR library if not needed.
         */
        for (Class<?> type = storage.getStorage().getClass(); type != null; type = type.getSuperclass()) {
            if (UCAR_CLASSNAME.equals(type.getName())) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * Returns a {@link NetcdfStore} implementation associated with this provider. This method invokes
     * {@link DataStoreConnection#closeAllExcept(Object)} after data store creation, keeping open only
     * the needed resource.
     *
     * @param storage Information about the storage (URL, stream, {@link ucar.nc2.NetcdfFile} instance, <i>etc</i>).
     */
    @Override
    public DataStore open(final DataStoreConnection storage) throws DataStoreException {
        return new NetcdfStore(storage);
    }

    /**
     * Creates a decoder for the given input. This method invokes
     * {@link DataStoreConnection#closeAllExcept(Object)} after the decoder has been created.
     *
     * @param  sink    Where to send the warnings, or {@code null} if none.
     * @param  storage Information about the input (file, input stream, <i>etc.</i>)
     * @return The decoder for the given input.
     * @throws IOException If an error occurred while opening the NetCDF file.
     * @throws DataStoreException If a logical error (other than I/O) occurred.
     */
    static Decoder decoder(final WarningProducer sink, final DataStoreConnection storage)
            throws IOException, DataStoreException
    {
        Decoder decoder;
        Object keepOpen;
        final ChannelDataInput input = storage.getStorageAs(ChannelDataInput.class);
        if (input != null) try {
            decoder = new ChannelDecoder(sink, input);
            keepOpen = input;
        } catch (DataStoreException e) {
            final String path = storage.getStorageAs(String.class);
            if (path != null) try {
                decoder = createByReflection(sink, path, false);
                keepOpen = path;
            } catch (IOException | DataStoreException s) {
                e.addSuppressed(s);
            }
            throw e;
        } else {
            keepOpen = storage.getStorage();
            decoder = createByReflection(sink, keepOpen, true);
        }
        storage.closeAllExcept(keepOpen);
        return decoder;
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
        ensureInitialized();
        /*
         * Get the appropriate constructor for the isUCAR argument. This constructor will be null
         * if the above code failed to load the UCAR library. Otherwise, instantiate the wrapper.
         */
        final Constructor<? extends Decoder> constructor;
        final Class<?> expectedType;
        if (isUCAR) {
            constructor  = createFromUCAR;
            expectedType = netcdfFileClass;
        } else {
            constructor  = createFromPath;
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

    /**
     * Get the {@link java.lang.Class} that represent the {@link ucar.nc2.NetcdfFile type}.
     * We do not synchronize this method since it is not a big deal if {@code Class.forName(…)} is invoked twice.
     * The {@code Class.forName(…)} method performs itself the required synchronization for returning the same
     * singleton {@code Class} instance.
     */
    private static void ensureInitialized() {
        if (netcdfFileClass == null) {
            try {
                netcdfFileClass = Class.forName(UCAR_CLASSNAME);
            } catch (ClassNotFoundException e) {
                netcdfFileClass = Void.TYPE;
                return;
            }
            try {
                /*
                 * UCAR API.
                 */
                canOpenFromPath = netcdfFileClass.getMethod("canOpen", String.class);
                assert canOpenFromPath.getReturnType() == Boolean.TYPE;
                /*
                 * SIS Wrapper API.
                 */
                final Class<? extends Decoder> wrapper =
                        Class.forName("org.apache.sis.internal.netcdf.ucar.DecoderWrapper").asSubclass(Decoder.class);
                final Class<?>[] parameterTypes = new Class<?>[] {WarningProducer.class, netcdfFileClass};
                createFromUCAR = wrapper.getConstructor(parameterTypes);
                parameterTypes[1] = String.class;
                createFromPath = wrapper.getConstructor(parameterTypes);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e); // Should never happen (shall be verified by the JUnit tests).
            }
        }
    }
}
