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
package org.apache.sis.geometry.wrapper;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.feature.internal.Resources;


/**
 * The list of factories available in the current environment.
 * This list depends on which dependencies (JTS, ERSI, <i>etc.</i>) are available at runtime.
 * This list needs to be created in another class than {@link Geometries} for avoiding class
 * initialization order problem when a {@code Geometries} subclass starts its initialization
 * before {@code Geometries} (in such case, an {@code Factory.INSTANCE} field may be null).
 *
 * <p>Note: we can bring this code back into {@link Geometries} if JEP 8209964 is implemented.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GeometryFactories {
    /**
     * The default geometry implementation to use. The default implementation
     * is JTS if present, or otherwise ESRI if present, or otherwise Java2D.
     */
    static final Geometries<?> DEFAULT = link(link(link(null, "j2d"), "esri"), "jts");
    static {
        setStandard(new StandardGeometries<>(DEFAULT));
    }

    /**
     * Do not allow instantiation of this class.
     */
    private GeometryFactories() {
    }

    /**
     * Gets the library implementation of the given package (JTS or ESRI) if present.
     * The given name shall be the sub-package name of a {@code Factory} class.
     * The last registered library will be the default implementation.
     */
    private static Geometries<?> link(final Geometries<?> previous, final String name) {
        final String classname = GeometryFactories.class.getPackageName() + '.' + name + ".Factory";
        final Geometries<?> factory;
        try {
            factory = (Geometries<?>) Class.forName(classname).getField("INSTANCE").get(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            LogRecord record = Resources.forLocale(null).createLogRecord(Level.CONFIG,
                    Resources.Keys.OptionalLibraryNotFound_2, name, e.toString());
            Logging.completeAndLog(Geometries.LOGGER, Geometries.class, "register", record);
            return previous;
        }
        factory.fallback = previous;
        return factory;
    }

    /**
     * Sets the implementation to associate to {@link GeometryLibrary#GEOAPI}.
     * This method is used for experimenting GeoAPI implementations outside Apache SIS.
     * This method may be removed in a future version if a stable GeoAPI geometry implementation
     * become available.
     *
     * @param  standard  the implementation to associate to {@link GeometryLibrary#GEOAPI}.
     */
    public static void setStandard(final Geometries<?> standard) {
        Geometries<?> factory = DEFAULT;        // Should never be null because at least "j2d" should be present.
        Geometries<?> last;
        do {
            last = factory;
            factory = factory.fallback;
        } while (factory != null && factory.library != GeometryLibrary.GEOAPI);
        last.fallback = standard;
    }
}
