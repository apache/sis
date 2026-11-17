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
package org.apache.sis.map.service;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Painters indexed by the style class they support.
 * A style whose class has no painter uses the painter of its nearest parent class or interface.
 * The search order is:
 * <ul>
 *   <li>the style class itself,</li>
 *   <li>its superclasses, nearest first,</li>
 *   <li>the interfaces of all those classes, in breadth-first order.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
final class PainterRegistry {

    /**
     * Painters registered for the exact class returned by {@link StylePainter#getStyleClass()}.
     */
    private final Map<Class<?>, StylePainter> painters;

    /**
     * Result of the searches done so far, including the classes which have no painter.
     */
    private final Map<Class<?>, Optional<StylePainter>> cache;

    /**
     * Creates a registry for the given painters.
     * If two painters support the same style class, the last one wins.
     *
     * @param  candidates  the painters to register.
     */
    PainterRegistry(final Iterable<? extends StylePainter> candidates) {
        painters = new HashMap<>();
        for (final StylePainter painter : candidates) {
            painters.put(painter.getStyleClass(), painter);
        }
        cache = new ConcurrentHashMap<>();
    }

    /**
     * Returns the painter for the given style class, or {@code null} if none.
     *
     * @param  styleClass  class of the style to paint.
     * @return painter for the style class or one of its parents, or {@code null}.
     */
    StylePainter get(final Class<?> styleClass) {
        return cache.computeIfAbsent(styleClass, this::search).orElse(null);
    }

    /**
     * Searches the painter of the given class or of its nearest parent.
     */
    private Optional<StylePainter> search(final Class<?> styleClass) {
        final Queue<Class<?>> interfaces = new ArrayDeque<>();
        for (Class<?> c = styleClass; c != null; c = c.getSuperclass()) {
            final StylePainter painter = painters.get(c);
            if (painter != null) {
                return Optional.of(painter);
            }
            interfaces.addAll(List.of(c.getInterfaces()));
        }
        final Set<Class<?>> visited = new HashSet<>();
        Class<?> c;
        while ((c = interfaces.poll()) != null) {
            if (visited.add(c)) {
                final StylePainter painter = painters.get(c);
                if (painter != null) {
                    return Optional.of(painter);
                }
                interfaces.addAll(List.of(c.getInterfaces()));
            }
        }
        return Optional.empty();
    }
}
