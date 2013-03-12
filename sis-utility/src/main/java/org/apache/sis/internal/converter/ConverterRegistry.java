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
package org.apache.sis.internal.converter;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.ThreadSafe;
import org.apache.sis.internal.util.SystemListener;
import org.apache.sis.util.ObjectConverter;


/**
 * A collection of {@link ObjectConverter} instances.
 * A converter from the given <var>source type</var> to the given <var>target type</var> can be
 * obtained by a call to {@link #converter(Class, Class)}. If no converter exists for the given
 * source and target types, then this registry searches for a suitable converter accepting a
 * parent class of the given source type, or returning a sub-class of the given target type.
 *
 * <p>New instances of {@code ConverterRegistry} are initially empty. Custom converters must be
 * explicitly {@linkplain #register registered}. However a system-wide registry initialized
 * with default converters is provided by the {@link #SYSTEM} constant.</p>
 *
 * {@section Note about conversions from interfaces}
 * {@code ConverterRegistry} is primarily designed for handling converters from classes to
 * other classes. Handling of interfaces are not prohibited (and actually sometime supported),
 * but their behavior may be more ambiguous than in the case of classes because of
 * multi-inheritance in interface hierarchy.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.20)
 * @version 0.3
 * @module
 */
@ThreadSafe
public class ConverterRegistry {
    /**
     * The default system-wide instance. This register is initialized with conversions between
     * some basic Java and SIS objects, like conversions between {@link java.util.Date} and
     * {@link java.lang.Long}. Those conversions are defined for the lifetime of the JVM.
     *
     * <p>If a temporary set of converters is desired, a new instance of {@code ConverterRegistry}
     * should be created explicitly instead.</p>
     *
     * {@section Adding system-wide converters}
     * Applications can add system-wide custom providers either by explicit call to the
     * {@link #register(ObjectConverter)} method on the system converter, or by listing
     * the fully qualified classnames of their {@link ObjectConverter} instances in the
     * following file (see {@link ServiceLoader} for more info about services loading):
     *
     * {@preformat text
     *     META-INF/services/org.apache.sis.util.converter.ObjectConverter
     * }
     */
    public static final ConverterRegistry SYSTEM = new ConverterRegistry();
    static {
        SystemListener.add(new SystemListener() {
            @Override protected void classpathChanged() {
                SYSTEM.clear();
            }
        });
    }

    /**
     * The map of converters of any kind. We use a single read/write lock for the whole map
     * because write operations will be rare (so {@code ConcurrentHashMap} may be an overkill).
     */
    private final Map<ClassPair<?,?>, SystemConverter<?,?>> converters;

    /**
     * The locks for the {@link #converters} map.
     */
    private final ReadWriteLock locks;

    /**
     * Creates an initially empty set of object converters.
     */
    public ConverterRegistry() {
        converters = new LinkedHashMap<>();
        locks = new ReentrantReadWriteLock();
    }

    /**
     * Removes all converters from this registry.
     */
    public void clear() {
        final Lock lock = locks.writeLock();
        lock.lock();
        try {
            converters.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns an unique instance of the given converter. If a converter already exists for the
     * same source an target classes, then that converter is returned. Otherwise that converter
     * is cached if {@code cache} is {@code true} and returned.
     *
     * @param  converter The converter to look for a unique instance.
     * @param  cache Whether to cache the given converter if there is no existing instance.
     * @return A previously existing instance if one exists, or the given converter otherwise.
     */
    @SuppressWarnings("unchecked")
    final <S,T> ObjectConverter<S,T> unique(final SystemConverter<S,T> converter, final boolean cache) {
        SystemConverter<S,T> existing;
        Lock lock = locks.readLock();
        lock.lock();
        try {
            existing = (SystemConverter<S,T>) converters.get(converter);
        } finally {
            lock.unlock();
        }
        /*
         * If no instance existed before for the source and target classes, stores this
         * instance in the pool. However we will need to check again during the write
         * operation in case an other thread had the time to add an instance in the pool.
         */
        if (existing == null) {
            if (!cache) {
                return converter;
            }
            lock = locks.writeLock();
            lock.lock();
            try {
                existing = (SystemConverter<S,T>) converters.put(converter, converter);
                if (existing != null) {
                    converters.put(existing, existing);
                } else {
                    existing = converter;
                }
            } finally {
                lock.unlock();
            }
        }
        return existing;
    }
}
