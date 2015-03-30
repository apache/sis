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

import java.util.Map;
import java.util.IdentityHashMap;
import java.util.ServiceLoader;


/**
 * Default factories defined in the {@code sis-utility} module.
 * This is a temporary placeholder until we leverage the "dependency injection" pattern.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
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
     * Return the default factory implementing the given interface.
     * This method returns only Apache SIS implementation of factories, and ignore all other.
     * This is a temporary mechanism while we are waiting for a real dependency injection mechanism.
     *
     * @param  <T>  The interface type.
     * @param  type The interface type.
     * @return A factory implementing the given interface, or {@code null} if none.
     */
    public static synchronized <T> T forClass(final Class<T> type) {
        T factory = type.cast(FACTORIES.get(type));
        if (factory == null && !FACTORIES.containsKey(type)) {
            for (final T candidate : ServiceLoader.load(type)) {
                final Class<?> ct = candidate.getClass();
                if (ct.getName().startsWith("org.apache.sis.")) {
                    if (factory != null) {
                        throw new IllegalStateException("Found two implementations of " + type);
                    }
                    factory = candidate;
                    break;
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
            throw new AssertionError("Missing “META-INF/services/" + type.getName() + "” file. "
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
            throw new AssertionError("The “META-INF/services/" + type.getName() + "” file should contains only “"
                + impl.getName() + "” in the Apache SIS namespace, but we found “" + factory.getClass().getName() + "”.");
        }
        return impl.cast(factory);
    }
}
