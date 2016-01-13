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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.sis.util.Static;
import org.apache.sis.util.logging.Logging;


/**
 * Names of loggers used in SIS other than the "module-wide" loggers. We often use approximatively one logger
 * per module, using the appropriate constant of the {@link Modules} class as the "module-wide" logger name.
 * However we also have a few more specialized loggers, which are listed here.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public final class Loggers extends Static {
    /**
     * The root logger.
     */
    public static final String ROOT = "org.apache.sis";

    /**
     * The logger for Apache SIS internal operations. The name of this logger does not match the package name
     * of the classes using it, because this logger name does not have the {@code "internal"} part in it.
     */
    public static final String SYSTEM = "org.apache.sis.system";

    /**
     * The logger for operations related to JDBC operations.
     */
    public static final String SQL = "org.apache.sis.sql";

    /**
     * The logger for operations related to XML marshalling or unmarshalling.
     */
    public static final String XML = "org.apache.sis.xml";

    /**
     * The logger for operations related to WKT parsing or formatting.
     * Note that WKT formatting often occurs in different packages.
     */
    public static final String WKT = "org.apache.sis.io.wkt";

    /**
     * The logger for operations related to geometries.
     */
    public static final String GEOMETRY = "org.apache.sis.geometry";

    /**
     * The logger for metadata operation related to the ISO 19115 standard.
     * This is a child of the logger for all metadata operations.
     */
    public static final String ISO_19115 = "org.apache.sis.metadata.iso";

    /**
     * The logger name for operation related to the creating of CRS objects.
     * This is a child of the logger for all referencing operations.
     */
    public static final String CRS_FACTORY = "org.apache.sis.referencing.factory";

    /**
     * The logger name for operation related to coordinate operations, in particular math transforms.
     * This is a child of the logger for all referencing operations.
     */
    public static final String COORDINATE_OPERATION = "org.apache.sis.referencing.operation";

    /**
     * The logger name for operation related to localization.
     */
    public static final String LOCALIZATION = "org.apache.sis.util.resources";

    /**
     * The logger name for operation related to application (console, GUI or web).
     */
    public static final String APPLICATION = "org.apache.sis.application";

    /**
     * Do not allow instantiation of this class.
     */
    private Loggers() {
    }

    /**
     * Returns a map of effective logging levels for SIS loggers. The effective logging level take in account the level
     * of parent loggers and the level of handlers. For example if a logger level is set to {@link Level#FINE} but no
     * handler have a level finer than {@link Level#INFO}, then the effective logging level will be {@link Level#INFO}.
     *
     * <p>This method does not report the loggers that have an effective level identical to its parent logger.</p>
     *
     * @return The effective logging levels of SIS loggers.
     */
    public static SortedMap<String,Level> getEffectiveLevels() {
        final SortedMap<String,Level> levels = new TreeMap<String,Level>();
        for (final Field field : Loggers.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) try {
                levels.put((String) field.get(null), null);
            } catch (IllegalAccessException e) {
                /*
                 * Should never happen, unless we added some fields and forgot to update this method.
                 * In such case forget the problematic fields and search the next one. This is okay
                 * since this method is only for information purpose.
                 */
                Logging.unexpectedException(Logging.getLogger(SYSTEM), Loggers.class, "getEffectiveLevels", e);
            }
        }
        /*
         * Process the loggers in alphabetical order. The intend is to process parent loggers before child.
         * The first logger in the map should be the SIS root logger, "org.apache.sis".
         */
        final Iterator<Map.Entry<String,Level>> it = levels.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String,Level> entry = it.next();
            final String name = entry.getKey();
            final Logger logger = Logging.getLogger(name);
            Level level = getEffectiveLevel(logger);
            final Level h = getHandlerLevel(logger);
            if (h.intValue() > level.intValue()) {
                level = h;                              // Take in account the logging level of handlers.
            }
            entry.setValue(level);
            /*
             * Now verify if the level is identical to the effective level of parent logger.
             * If they are identical, then we remove the entry in order to report only the changes.
             */
            Logger parent = logger;
            while ((parent = parent.getParent()) != null) {
                final Level p = levels.get(parent.getName());
                if (p != null) {
                    if (p.equals(level)) {
                        it.remove();
                    }
                    break;
                }
            }
        }
        return levels;
    }

    /**
     * Returns the effective level of the given logger, searching in the parent loggers if needed.
     * This method does not verify if handlers have higher level.
     */
    private static Level getEffectiveLevel(Logger logger) {
        while (logger != null) {
            final Level level = logger.getLevel();
            if (level != null) {
                return level;
            }
            logger = logger.getParent();
        }
        return Level.INFO;      // Default value specified by the java.util.logging framework.
    }

    /**
     * Returns the finest level of registered handlers for the given logger.
     * This method verifies also in the parent handlers if the logger use them.
     */
    private static Level getHandlerLevel(Logger logger) {
        Level level = Level.OFF;
        while (logger != null) {
            for (final Handler handler : logger.getHandlers()) {
                final Level c = handler.getLevel();
                if (c != null && c.intValue() < level.intValue()) {
                    level = c;
                }
            }
            if (!logger.getUseParentHandlers()) {
                break;
            }
            logger = logger.getParent();
        }
        return level;
    }
}
