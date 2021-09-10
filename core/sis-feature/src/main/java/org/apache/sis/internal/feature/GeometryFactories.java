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
package org.apache.sis.internal.feature;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.internal.jdk9.JDK9;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.logging.Logging;


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
 * @version 1.1
 * @since   1.1
 * @module
 */
final class GeometryFactories {
    /**
     * The default geometry implementation to use. The default implementation
     * is JTS if present, or otherwise ESRI if present, or otherwise Java2D.
     */
    static final Geometries<?> implementation = link(link(link(null, "j2d"), "esri"), "jts");

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
        final String classname = JDK9.getPackageName(GeometryFactories.class) + '.' + name + ".Factory";
        final Geometries<?> factory;
        try {
            factory = (Geometries<?>) Class.forName(classname).getField("INSTANCE").get(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            LogRecord record = Resources.forLocale(null).getLogRecord(Level.CONFIG,
                    Resources.Keys.OptionalLibraryNotFound_2, name, e.toString());
            record.setLoggerName(Loggers.GEOMETRY);
            Logging.log(Geometries.class, "register", record);
            return previous;
        }
        factory.fallback = previous;
        return factory;
    }
}
