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
package org.apache.sis.internal.system;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.ServiceLoader;
import java.util.ServiceConfigurationError;
import org.apache.sis.util.logging.Logging;


/**
 * Default factories defined in the {@code sis-utility} module.
 * This is a temporary placeholder until we leverage the "dependency injection" pattern.
 * A candidate replacement is JSR-330.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 *
 * @see <a href="https://jcp.org/en/jsr/detail?id=330">JSR-330</a>
 */
public final class DefaultFactories extends SystemListener {
    /**
     * Cache of factories which are found by {@code META-INF/services}.
     */
    private static final Map<Class<?>, Object> FACTORIES = new IdentityHashMap<>(4);
    static {
        SystemListener.add(new DefaultFactories());
    }

    /**
     * For the singleton system listener only.
     */
    private DefaultFactories() {
        super(Modules.UTILITIES);
    }

    /**
     * Discards cached factories when the classpath has changed.
     */
    @Override
    protected void classpathChanged() {
        synchronized (DefaultFactories.class) {
            FACTORIES.clear();
        }
    }

    /**
     * Returns {@code true} if the default factory of the given type is the given instance.
     *
     * @param  <T>  The interface type.
     * @param  type The interface type.
     * @param  factory The factory implementation to test.
     * @return {@code true} if the given factory implementation is the default instance.
     */
    public static synchronized <T> boolean isDefaultInstance(final Class<T> type, final T factory) {
        return FACTORIES.get(type) == factory;
    }

    /**
     * Returns the default factory implementing the given interface.
     * This method gives preference to Apache SIS implementation of factories if present.
     * This is a temporary mechanism while we are waiting for a real dependency injection mechanism.
     *
     * @param  <T>  The interface type.
     * @param  type The interface type.
     * @return A factory implementing the given interface, or {@code null} if none.
     */
    public static synchronized <T> T forClass(final Class<T> type) {
        T factory = type.cast(FACTORIES.get(type));
        if (factory == null && !FACTORIES.containsKey(type)) {
            T fallback = null;
            for (final T candidate : createServiceLoader(type)) {
                if (candidate.getClass().getName().startsWith(Modules.CLASSNAME_PREFIX)) {
                    if (factory != null) {
                        throw new ServiceConfigurationError("Found two implementations of " + type);
                    }
                    factory = candidate;
                } else if (fallback == null) {
                    fallback = candidate;
                }
            }
            if (factory == null) {
                factory = fallback;
            }
            /*
             * Verifies if the factory that we just selected is the same implementation than an existing instance.
             * The main case for this test is org.apache.sis.referencing.factory.GeodeticObjectFactory, where the
             * same class implements 3 factory interfaces.
             */
            if (factory != null) {
                for (final Object existing : FACTORIES.values()) {
                    if (existing != null && factory.getClass().equals(existing.getClass())) {
                        factory = type.cast(existing);
                        break;
                    }
                }
            }
            FACTORIES.put(type, factory);
        }
        return factory;
    }

    /**
     * Returns a factory which is guaranteed to be present. If the factory is not found,
     * this will be considered a configuration error (corrupted JAR files of incorrect classpath).
     *
     * @param  <T>  The interface type.
     * @param  type The interface type.
     * @return A factory implementing the given interface.
     *
     * @since 0.6
     */
    public static <T> T forBuildin(final Class<T> type) {
        final T factory = forClass(type);
        if (factory == null) {
            throw new ServiceConfigurationError("Missing “META-INF/services/" + type.getName() + "” file. "
                    + "The JAR file may be corrupted or the classpath incorrect.");
        }
        return factory;
    }

    /**
     * Returns a factory of the given type, making sure that it is an implementation of the given class.
     * Use this method only when we know that Apache SIS registers only one implementation of a given service.
     *
     * @param  <T>  The interface type.
     * @param  <I>  The requested implementation class.
     * @param  type The interface type.
     * @param  impl The requested implementation class.
     * @return A factory implementing the given interface.
     *
     * @since 0.6
     */
    public static <T, I extends T> I forBuildin(final Class<T> type, final Class<I> impl) {
        final T factory = forBuildin(type);
        if (!impl.isInstance(factory)) {
            throw new ServiceConfigurationError("The “META-INF/services/" + type.getName() + "” file should contains only “"
                + impl.getName() + "” in the Apache SIS namespace, but we found “" + factory.getClass().getName() + "”.");
        }
        return impl.cast(factory);
    }

    /**
     * Returns a service loader for the given type using the default class loader.
     * The default is the current thread {@linkplain Thread#getContextClassLoader() context class loader},
     * provided that it can access at least the Apache SIS stores.
     *
     * @param  <T> The compile-time value of {@code service} argument.
     * @param  service The interface or abstract class representing the service.
     * @return A new service loader for the given service type.
     *
     * @since 0.8
     */
    public static <T> ServiceLoader<T> createServiceLoader(final Class<T> service) {
        try {
            return ServiceLoader.load(service, getContextClassLoader());
        } catch (SecurityException e) {
            /*
             * We were not allowed to invoke Thread.currentThread().getContextClassLoader().
             * But ServiceLoader.load(Class) may be allowed to, since it is part of JDK.
             */
            Logging.recoverableException(Logging.getLogger(Loggers.SYSTEM),
                    DefaultFactories.class, "createServiceLoader", e);
            return ServiceLoader.load(service);
        }
    }

    /**
     * Returns the context class loader, but makes sure that it has Apache SIS on its classpath.
     * First, this method invokes {@link Thread#getContextClassLoader()} for the current thread.
     * Then this method scans over all Apache SIS classes on the stack trace. For each SIS class,
     * its loader is compared to the above-cited context class loader. If the context class loader
     * is equal or is a child of the SIS loader, then it is left unchanged. Otherwise the context
     * class loader is replaced by the SIS one.
     *
     * <p>The intend of this method is to ensure that {@link ServiceLoader#load(Class)} will find the
     * Apache SIS services even in an environment that defined an unsuitable context class loader.</p>
     *
     * @return The context class loader if suitable, or another class loader otherwise.
     * @throws SecurityException if this method is not allowed to get the current thread
     *         context class loader or one of its parent.
     *
     * @since 0.8
     */
    private static ClassLoader getContextClassLoader() throws SecurityException {
        final Thread thread = Thread.currentThread();
        ClassLoader loader = thread.getContextClassLoader();
        final Set<ClassLoader> parents = new HashSet<>();
        for (ClassLoader c = loader; c != null; c = c.getParent()) {
            parents.add(c);
        }
        boolean warnings = false;
        for (final StackTraceElement trace : thread.getStackTrace()) {
            final String element = trace.getClassName();
            if (element.startsWith(Modules.CLASSNAME_PREFIX)) try {
                ClassLoader c = Class.forName(element).getClassLoader();
                if (!parents.contains(c)) {
                    loader = c;
                    parents.clear();
                    while (c != null) {
                        parents.add(c);
                        c = c.getParent();
                    }
                }
            } catch (SecurityException | ClassNotFoundException e) {
                if (!warnings) {
                    warnings = true;
                    Logging.recoverableException(Logging.getLogger(Loggers.SYSTEM),
                            DefaultFactories.class, "getContextClassLoader", e);
                }
            }
        }
        return loader;
    }
}
