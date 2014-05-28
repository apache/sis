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
import org.opengis.util.NameFactory;
import org.apache.sis.util.iso.DefaultNameFactory;


/**
 * Default factories defined in the {@code sis-utility} module.
 * This is a temporary placeholder until we leverage the "dependency injection" pattern.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
public final class DefaultFactories extends SystemListener {
    /**
     * A name factory which is guaranteed to be an instance of SIS {@link DefaultNameFactory}.
     * We use this factory when we need to ensure that the created names are instances of the
     * SIS {@link org.apache.sis.util.iso.AbstractName} implementation.
     *
     * <p>Note that this need to be the exact SIS class, not a user-provided subclass,
     * otherwise we could not guarantee the above-cited requirement.</p>
     */
    public static final DefaultNameFactory SIS_NAMES = new DefaultNameFactory();

    /**
     * The factory to use for creating names, not necessarily SIS instances.
     * This is fixed to {@link #SIS_NAMES} for now, but will probably be fetched in a more
     * dynamic way later.
     */
    public static final NameFactory NAMES = SIS_NAMES;

    /**
     * Cache of factories which are found by {@code META-INF/services}.
     */
    private static final Map<Class<?>, Object> FACTORIES = new IdentityHashMap<Class<?>, Object>(4);
    static {
        FACTORIES.put(NameFactory.class, NAMES);
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
            FACTORIES.put(NameFactory.class, NAMES);
        }
    }

    /**
     * Return the default factory implementing the given interface.
     * This method will give preference to Apache SIS factories if any.
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
                    factory = candidate;
                    break;
                }
                /*
                 * Select the first provider found in the iteration. If more than one provider is found,
                 * select the most specialized type. This is okay only for relatively simple configurations,
                 * while we are waiting for a real dependency injection mechanism.
                 */
                if (factory == null || factory.getClass().isAssignableFrom(ct)) {
                    factory = candidate;
                }
            }
            FACTORIES.put(type, factory);
        }
        return factory;
    }
}
