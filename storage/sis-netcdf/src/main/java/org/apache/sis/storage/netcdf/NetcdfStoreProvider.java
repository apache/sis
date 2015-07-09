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

import java.io.FileNotFoundException;
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
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.Version;


/**
 * The provider of {@link NetcdfStore} instances. Given a {@link StorageConnector} input,
 * this class tries to instantiate a {@code NetcdfStore} using the embedded NetCDF decoder.
 * If the embedded decoder can not decode the given input and the UCAR library is reachable
 * on the classpath, then this class tries to instantiate a {@code NetcdfStore} backed by
 * the UCAR library.
 *
 * <div class="section">Thread safety</div>
 * The same {@code NetcdfStoreProvider} instance can be safely used by many threads without synchronization on
 * the part of the caller. However the {@link NetcdfStore} instances created by this factory are not thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see NetcdfStore
 */
public class NetcdfStoreProvider extends DataStoreProvider {
    /**
     * The MIME type for NetCDF files.
     */
    static final String MIME_TYPE = "application/x-netcdf";

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
     * Clears the cached constructors if the classpath has changed,
     * because the UCAR library may no longer be on the classpath.
     */
    static {
        SystemListener.add(new SystemListener(Modules.NETCDF) {
            @Override protected void classpathChanged() {
                reset();
            }
        });
    }

    /**
     * Creates a new provider.
     */
    public NetcdfStoreProvider() {
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be supported by {@link NetcdfStore}.
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the
     * {@linkplain StorageConnector#getStorage() storage object} or contents.
     *
     * @param  storage Information about the storage (URL, stream, {@link ucar.nc2.NetcdfFile} instance, <i>etc</i>).
     * @return {@code SUPPORTED} if the given storage seems to be usable by the {@code NetcdfStore} instances.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(StorageConnector storage) throws DataStoreException {
        int     version     = 0;
        boolean hasVersion  = false;
        boolean isSupported = false;
        final ByteBuffer buffer = storage.getStorageAs(ByteBuffer.class);
        if (buffer != null) {
            if (buffer.remaining() < Integer.SIZE / Byte.SIZE) {
                return ProbeResult.INSUFFICIENT_BYTES;
            }
            final int header = buffer.getInt(buffer.position());
            if ((header & 0xFFFFFF00) == ChannelDecoder.MAGIC_NUMBER) {
                hasVersion  = true;
                version     = header & 0xFF;
                isSupported = (version >= 1 && version <= ChannelDecoder.MAX_VERSION);
            }
        }
        /*
         * If we failed to check using the embedded decoder, tries using the UCAR library.
         * The UCAR library is an optional dependency. If that library is present and the
         * input is a String, then the following code may trigs a large amount of classes
         * loading.
         *
         * Note that the UCAR library expects a String argument, not a File, because it
         * has special cases for "file:", "http:", "nodods:" and "slurp:" protocols.
         */
        if (!isSupported) {
            final String path = storage.getStorageAs(String.class);
            if (path != null) {
                ensureInitialized();
                final Method method = canOpenFromPath;
                if (method != null) try {
                    isSupported = (Boolean) method.invoke(null, path);
                } catch (IllegalAccessException e) {
                    // Should never happen, since the method is public.
                    throw (Error) new IncompatibleClassChangeError("canOpen").initCause(e);
                } catch (InvocationTargetException e) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof DataStoreException) throw (DataStoreException) cause;
                    if (cause instanceof RuntimeException)   throw (RuntimeException)   cause;
                    if (cause instanceof Error)              throw (Error)              cause;
                    if (cause instanceof FileNotFoundException) {
                        /*
                         * Happen if the String argument uses any protocol not recognized by the UCAR library,
                         * in which case UCAR tries to open it as a file even if it is not a file. For example
                         * we get this exception for "jar:file:/file.jar!/entry.nc".
                         */
                        Logging.recoverableException(Logging.getLogger(Modules.NETCDF), netcdfFileClass, "canOpen", cause);
                        return ProbeResult.UNSUPPORTED_STORAGE;
                    }
                    throw new DataStoreException(e); // The cause may be IOException.
                }
            } else {
                /*
                 * Check if the given input is itself an instance of the UCAR oject.
                 * We check classnames instead of netcdfFileClass.isInstance(storage)
                 * in order to avoid loading the UCAR library if not needed.
                 */
                for (Class<?> type = storage.getStorage().getClass(); type != null; type = type.getSuperclass()) {
                    if (UCAR_CLASSNAME.equals(type.getName())) {
                        isSupported = true;
                        break;
                    }
                }
            }
        }
        /*
         * At this point, the readability status has been determined. The file version number
         * is unknown if we are able to open the file only through the UCAR library.
         */
        if (hasVersion) {
            return new ProbeResult(isSupported, MIME_TYPE, Version.valueOf(version));
        }
        return isSupported ? new ProbeResult(true, MIME_TYPE, null) : ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Returns a {@link NetcdfStore} implementation associated with this provider.
     *
     * @param  storage Information about the storage (URL, stream, {@link ucar.nc2.NetcdfFile} instance, <i>etc</i>).
     * @return A data store implementation associated with this provider for the given storage.
     * @throws DataStoreException If an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector storage) throws DataStoreException {
        return new NetcdfStore(storage);
    }

    /**
     * Creates a decoder for the given input. This method invokes
     * {@link StorageConnector#closeAllExcept(Object)} after the decoder has been created.
     *
     * @param  listeners Where to send the warnings.
     * @param  storage Information about the input (file, input stream, <i>etc.</i>)
     * @return The decoder for the given input.
     * @throws IOException If an error occurred while opening the NetCDF file.
     * @throws DataStoreException If a logical error (other than I/O) occurred.
     */
    static Decoder decoder(final WarningListeners<?> listeners, final StorageConnector storage)
            throws IOException, DataStoreException
    {
        Decoder decoder;
        Object keepOpen;
        final ChannelDataInput input = storage.getStorageAs(ChannelDataInput.class);
        if (input != null) try {
            decoder = new ChannelDecoder(listeners, input);
            keepOpen = input;
        } catch (DataStoreException e) {
            final String path = storage.getStorageAs(String.class);
            if (path != null) {
                decoder = createByReflection(listeners, path, false);
                keepOpen = path;
            }
            throw e;
        } else {
            keepOpen = storage.getStorage();
            decoder = createByReflection(listeners, keepOpen, true);
        }
        storage.closeAllExcept(keepOpen);
        return decoder;
    }

    /**
     * Creates a new NetCDF decoder as a wrapper around the UCAR library. This decoder is used only when we can
     * not create our embedded NetCDF decoder. This method uses reflection for creating the wrapper, in order
     * to keep the UCAR dependency optional.
     *
     * @param  listeners Where to send the warnings.
     * @param  input  The NetCDF file object of filename string from which to read data.
     * @param  isUCAR {@code true} if {@code input} is an instance of the UCAR {@link ucar.nc2.NetcdfFile} object,
     *                or {@code false} if it is the filename as a {@code String}.
     * @return The {@link DecoderWrapper} instance for the given input.
     * @throws IOException If an error occurred while opening the NetCDF file.
     * @throws DataStoreException If a logical error (other than I/O) occurred.
     */
    private static Decoder createByReflection(final WarningListeners<?> listeners, final Object input, final boolean isUCAR)
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
            return constructor.newInstance(listeners, input);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException)        throw (IOException)        cause;
            if (cause instanceof DataStoreException) throw (DataStoreException) cause;
            if (cause instanceof RuntimeException)   throw (RuntimeException)   cause;
            if (cause instanceof Error)              throw (Error)              cause;
            throw new UndeclaredThrowableException(cause); // Should never happen actually.
        } catch (Exception e) { // (ReflectiveOperationException) on JDK7 branch.
            throw new AssertionError(e); // Should never happen (shall be verified by the JUnit tests).
        }
    }

    /**
     * Gets the {@link java.lang.Class} that represent the {@link ucar.nc2.NetcdfFile type}.
     */
    private static void ensureInitialized() {
        if (netcdfFileClass == null) {
            synchronized (NetcdfStoreProvider.class) {
                /*
                 * No double-check because it is not a big deal if the constructors are fetched twice.
                 * The sychronization is mostly a safety against concurrent execution of 'reset()'.
                 */
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
                    final Class<?>[] parameterTypes = new Class<?>[] {WarningListeners.class, netcdfFileClass};
                    createFromUCAR = wrapper.getConstructor(parameterTypes);
                    parameterTypes[1] = String.class;
                    createFromPath = wrapper.getConstructor(parameterTypes);
                } catch (Exception e) { // (ReflectiveOperationException) on JDK7 branch.
                    throw new AssertionError(e); // Should never happen (shall be verified by the JUnit tests).
                }
            }
        }
    }

    /**
     * Invoked when the classpath changed. Clears the cached class and constructors, since we don't know
     * if the UCAR library is still on the classpath.
     */
    static synchronized void reset() {
        netcdfFileClass = null;
        canOpenFromPath = null;
        createFromUCAR  = null;
        createFromPath  = null;
    }
}
