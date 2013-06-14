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
 * @version 0.3
 * @module
 */
public final class DefaultFactories extends SystemListener {
    /**
     * The factory to use for creating names.
     */
    public static final NameFactory NAMES = new DefaultNameFactory();

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
                if (candidate.getClass().getName().startsWith("org.apache.sis.")) {
                    factory = candidate;
                    break;
                }
                if (factory == null) {
                    factory = candidate;
                }
            }
            FACTORIES.put(type, factory);
        }
        return factory;
    }
}
