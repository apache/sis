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
package org.apache.sis.storage.panama;

import java.lang.ref.WeakReference;
import org.apache.sis.util.Disposable;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.system.ReferenceQueueConsumer;
import org.apache.sis.system.Shutdown;


/**
 * Weak reference to a service which depends on a native library.
 * Each native library is identified by the unique identifier of the file which has been loaded.
 * Exactly one service ({@link DataStoreProvider}) is associated to each native library.
 *
 * <h2>Restriction</h2>
 * The current implementation does not allow two services to share the same native library.
 * This is for avoiding conflict if a {@link NativeFunctions#destroy()} is executed when a
 * service is garbage-collected while another service is still using the same native library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class LibraryReference extends WeakReference<DataStoreProvider> implements Disposable {
    /**
     * Most recently loaded library, or {@code null} if none.
     * This is the starting point of a linked list ordered from
     * most recently loaded library to less recently loaded library.
     */
    private static LibraryReference LAST;

    /**
     * Whether a shutdown hook has already been registered.
     */
    private static boolean hasShutdownHook;

    /**
     * Nodes which was created before this node, or {@code null} if none.
     * This is used for managing the linked list.
     *
     * <p>Implementation note: while a linked list does not scale well compared to a hash map,
     * we expect very few elements in the list, typically only 1 or 2 elements.</p>
     */
    private LibraryReference previous;

    /**
     * The native functions used by the service.
     * Its {@link NativeFunctions#destroy()} method will be invoked when the service is garbage-collected.
     */
    private final NativeFunctions functions;

    /**
     * An object that uniquely identifies the native library which has been loaded.
     * This is usually a unique identifier of the file from which the library was loaded.
     */
    private final Object libraryKey;

    /**
     * Creates a new reference to a native library wrapped by the given service.
     *
     * @param service     the service which uses a native library.
     * @param functions   the native functions used by the service.
     * @param libraryKey  an object that uniquely identifies the native library which has been loaded.
     */
    private LibraryReference(final DataStoreProvider service, final NativeFunctions functions, final Object libraryKey) {
        super(service, ReferenceQueueConsumer.QUEUE);
        this.functions  = functions;
        this.libraryKey = libraryKey;
        previous = LAST;
    }

    /**
     * Returns a service for the native library identified by the given key.
     * If a service already exists for the given key, it must be an instance
     * of the exact class (not a subclass) specified by {@code serviceType}.
     *
     * <p>If {@code global} is {@code true}, then the native functions will be released at <abbr>JVM</abbr>
     * shutdown time instead of when the service is garbage-collected. Callers should use this mode only when
     * a strong reference to the service will be kept for the <abbr>JVM</abbr> lifetime.</p>
     *
     * <h4>Error handling</h4>
     * If an exception is thrown, then the caller is responsible for closing {@link LibraryLoader#libraryArena}
     * if this is a shared arena. The caller will do nothing (not close the arena) if this is the global arena.
     *
     * @param  <F>         type of the classes providing native functions.
     * @param  <S>         compile-time value of the {@code serviceType} argument.
     * @param  libraryKey  an object that uniquely identifies the native library which has been loaded.
     * @param  loader      information about the library to load.
     * @param  global      whether the native library uses the global arena instead of a shared arena.
     * @return an existing or newly created service for the native library identified by the given key.
     * @throws IllegalStateException if the native library identified by the given key is already in use.
     */
    static synchronized <F extends NativeFunctions, S extends DataStoreProvider>
            S getOrCreateService(final Object libraryKey, final LibraryLoader<F,S> loader, final boolean global)
    {
        for (LibraryReference library = LAST; library != null; library = library.previous) {
            if (libraryKey.equals(library.libraryKey)) {
                @SuppressWarnings("unchecked")
                final S service = (S) library.get();
                if (service != null) {
                    if (service.getClass() != loader.serviceType) {
                        throw new IllegalStateException("Native library already in use.");
                    }
                    loader.libraryArena.close();    // Because we will use the arena of an existing instance.
                    return service;
                }
                break;
            }
        }
        final F functions = loader.createNativeFunctions();
        final S service;
        try {
            service = loader.createService(functions);
            /*
             * Note: registering a shutdown hook cause a reference to be kept for the JVM lifetime.
             * But such reference should exist anyway because this `global(String)` method should
             * be invoked for initialization of a static variable. Furthermore, subclass may need
             * to do a last native method class for flushing some cache.
             *
             * TODO: unregister if the library had a fatal error and should not be used anymore.
             */
            if (global) {
                Shutdown.register(functions);
            } else {
                if (!hasShutdownHook) {
                    Shutdown.register(LibraryReference::disposeAll);
                    hasShutdownHook = true;
                }
                LAST = new LibraryReference(service, functions, libraryKey);
            }
        } catch (Throwable e) {
            try {
                functions.destroy();
            } catch (Throwable s) {
                e.addSuppressed(s);
            }
            // `functions.libraryArena` shall be closed (if not global) by the caller.
            throw e;
        }
        return service;
    }

    /**
     * Invoked when the service is garbage collected. This method invokes {@link NativeFunctions#destroy()}
     * for releasing the native resources. If an exception occurs, it is logged.
     */
    @Override
    public void dispose() {
        synchronized (LibraryReference.class) {
            LibraryReference library = LAST;
            if (library == this) {
                LAST = previous;
            }
            while (library != null) {
                if (library == this) {
                    try (functions.libraryArena) {
                        functions.call();    // Inside the synchronized block for blocking a reload to happen in same time.
                    }
                    break;
                }
                if (library.previous == this) {
                    library.previous = previous;
                    previous = null;
                }
                library = library.previous;
            }
            /*
             * If the loop completes, we did not found this node in the linked list.
             * This is normal if `disposeAll()` has been executed before `dispose()`.
             * We shall not invoke `destroy()` a second time.
             */
        }
    }

    /**
     * Disposes all nodes in reverse order (most recently added library is disposed first).
     * This method should be invoked only in the shutdown hook, when all non-daemon threads
     * are terminated. No thread should be using the native library at this time.
     *
     * @return ignored (declared only for matching the method signature expected by lambda function).
     */
    private static synchronized Object disposeAll() {
        LibraryReference library = LAST;
        LAST = null;
        while (library != null) {
            try (library.functions.libraryArena) {
                library.functions.call();
            }
            library = library.previous;
        }
        return null;
    }
}
