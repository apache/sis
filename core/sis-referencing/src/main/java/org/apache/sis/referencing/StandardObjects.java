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
package org.apache.sis.referencing;

import java.lang.reflect.Method;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.util.logging.Logging;


/**
 * Utility methods for the {@link GeodeticObjects} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
final class StandardObjects extends SystemListener {
    /**
     * The enumeration class.
     */
    private final Class<?> type;

    /**
     * Creates and register a listener for clearing the cached objects.
     */
    StandardObjects(final Class<?> type) {
        super(Modules.REFERENCING);
        this.type = type;
        add(this);
    }

    /**
     * Invoked when the classpath changed.
     * The listener will invoke the {@code clear()} method of all enumeration values.
     */
    @Override
    protected void classpathChanged() {
        try {
            final Method clear = type.getDeclaredMethod("clear", (Class[]) null);
            for (final Enum<?> value : (Enum[]) type.getMethod("values", (Class[]) null).invoke(null, (Object[]) null)) {
                clear.invoke(value, (Object[]) null);
            }
        } catch (Exception e) { // ReflectiveOperationException on the JDK7 branch.
            throw new AssertionError(e); // Should never happen.
        }
    }

    /**
     * Returns the EPSG factory to use for creating datum, ellipsoids and prime meridians, or {@code null} if none.
     * If this method returns {@code null}, then the caller will silently fallback on hard-coded values.
     */
    static DatumAuthorityFactory datumFactory() {
        return null; // TODO
    }

    /**
     * Invoked when a factory failed to create an object.
     * After invoking this method, then the caller will fallback on hard-coded values.
     */
    static void failure(final Object caller, final String method, final FactoryException e) {
        Logging.unexpectedException(caller.getClass(), method, e);
    }
}
