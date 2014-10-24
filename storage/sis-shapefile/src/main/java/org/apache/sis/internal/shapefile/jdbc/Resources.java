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
package org.apache.sis.internal.shapefile.jdbc;

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * Locale-dependent resources for messages.
 *
 * @author  Marc Le Bihan
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class Resources extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     *
     * @author  Marc Le Bihan
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.5
     * @version 0.5
     * @module
     */
    public static final class Keys {
        /**
         * For {@link #INSTANCE} creation only.
         */
        private Keys() {
        }

        /**
         * The auto-commit ‘{0}’ value is currently ignored (not implemented): auto-commit is always
         * true.
         */
        public static final short AutoCommitIgnored_1 = 0;

        /**
         * The connection is closed.
         */
        public static final short ClosedConnection = 1;

        /**
         * The result set is closed.
         */
        public static final short ClosedResultSet = 3;

        /**
         * The statement is closed.
         */
        public static final short ClosedStatement = 2;

        /**
         * Commit and rollback has no effect (currently not implemented): auto-commit is always true.
         */
        public static final short CommitRollbackIgnored = 4;

        /**
         * The “{0}” DBF file seems to have an invalid format, as its descriptor could not be read: {1}
         */
        public static final short InvalidDBFFormatDescriptor_2 = 5;

        /**
         * The result set has no more results.
         */
        public static final short NoMoreResults = 6;

        /**
         * There is no “{0}” column in this query.
         */
        public static final short NoSuchColumn_1 = 8;

        /**
         * Internal JDBC driver currently does not support the {0}.{1} ability/calls.
         */
        public static final short UnsupportedDriverFeature_2 = 7;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources The path of the binary file containing resources, or {@code null} if
     *        there is no resources. The resources may be a file or an entry in a JAR file.
     */
    public Resources(final URL resources) {
        super(resources);
    }

    /**
     * Returns resources in the given locale.
     *
     * @param  locale The locale, or {@code null} for the default locale.
     * @return Resources in the given locale.
     * @throws MissingResourceException if resources can't be found.
     */
    public static Resources getResources(final Locale locale) throws MissingResourceException {
        return getBundle(Resources.class, locale);
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final short key) throws MissingResourceException {
        return getResources(null).getString(key);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}"
     * with values of {@code arg0}.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute to "{0}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute to "{0}".
     * @param  arg1 Value to substitute to "{1}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0,
                                final Object arg1) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0, arg1);
    }
}
