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
package org.apache.sis.storage.sql.feature;

import java.io.InputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.util.resources.ResourceInternationalString;


/**
 * Warning and error messages that are specific to the {@code org.apache.sis.storage.sql} module.
 * Resources in this file should not be used by any other module. For resources shared by
 * all modules in the Apache SIS project, see {@code org.apache.sis.util.resources} package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class Resources extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     */
    public static final class Keys extends KeyConstants {
        /**
         * The unique instance of key constants handler.
         */
        static final Keys INSTANCE = new Keys();

        /**
         * For {@link #INSTANCE} creation only.
         */
        private Keys() {
        }

        /**
         * Assume database byte/tinyint unsigned, due to a lack of metadata.
         */
        public static final short AssumeUnsigned = 16;

        /**
         * Cannot analyze fully the database schema because of incomplete metadata.
         */
        public static final short CanNotAnalyzeFully = 17;

        /**
         * Cannot fetch a Coordinate Reference System (CRS) for SRID code {0}.
         */
        public static final short CanNotFetchCRS_1 = 8;

        /**
         * Cannot find an identifier in the database for the reference system “{0}”.
         */
        public static final short CanNotFindSRID_1 = 15;

        /**
         * Provider of connections to the database.
         */
        public static final short DataSource = 1;

        /**
         * Unexpected duplication of column named “{0}”.
         */
        public static final short DuplicatedColumn_1 = 5;

        /**
         * Spatial Reference Identifier (SRID) {1} has more than one entry in “{0}” table.
         */
        public static final short DuplicatedSRID_2 = 9;

        /**
         * “{0}” is not a valid qualified name for a table.
         */
        public static final short IllegalQualifiedName_1 = 3;

        /**
         * The literal of function “{0}” is not compatible with the reference system of property “{1}”.
         */
        public static final short IncompatibleLiteralCRS_2 = 18;

        /**
         * Unexpected error while analyzing the database schema.
         */
        public static final short InternalError = 6;

        /**
         * Unexpected column “{1}” in the “{0}” foreigner key.
         */
        public static final short MalformedForeignerKey_2 = 7;

        /**
         * Resource names mapped to SQL queries.
         */
        public static final short MappedSQLQueries = 14;

        /**
         * Name “{0}” is already used by another table, view or query.
         */
        public static final short NameAlreadyUsed_1 = 13;

        /**
         * Table names, optionally with their schemas and catalogs.
         */
        public static final short QualifiedTableNames = 2;

        /**
         * The {0} spatial extension is not found.
         */
        public static final short SpatialExtensionNotFound_1 = 12;

        /**
         * SRID {1} does not define a Coordinate Reference System. The object is a `{0}` instead.
         */
        public static final short UnexpectedTypeForSRID_2 = 11;

        /**
         * No entry found in table “{0}” for Spatial Reference Identifier (SRID) {1}.
         */
        public static final short UnknownSRID_2 = 10;

        /**
         * No mapping from SQL type “{0}” to a Java class.
         */
        public static final short UnknownType_1 = 4;
    }

    /**
     * Constructs a new resource bundle loading data from
     * the resource file of the same name as this class.
     */
    public Resources() {
    }

    /**
     * Opens the binary file containing the localized resources to load.
     * This method delegates to {@link Class#getResourceAsStream(String)},
     * but this delegation must be done from the same module as the one
     * that provides the binary file.
     */
    @Override
    protected InputStream getResourceAsStream(final String name) {
        return getClass().getResourceAsStream(name);
    }

    /**
     * Returns the handle for the {@code Keys} constants.
     *
     * @return a handler for the constants declared in the inner {@code Keys} class.
     */
    @Override
    protected KeyConstants getKeyConstants() {
        return Keys.INSTANCE;
    }

    /**
     * Returns resources in the given locale.
     *
     * @param  locale  the locale, or {@code null} for the default locale.
     * @return resources in the given locale.
     * @throws MissingResourceException if resources cannot be found.
     */
    public static Resources forLocale(final Locale locale) {
        /*
         * We cannot factorize this method into the parent class, because we need to call
         * `ResourceBundle.getBundle(String)` from the module that provides the resources.
         * We do not cache the result because `ResourceBundle` already provides a cache.
         */
        return (Resources) getBundle(Resources.class.getName(), nonNull(locale));
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key  the key for the desired string.
     * @return the string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short key) throws MissingResourceException {
        return forLocale(null).getString(key);
    }

    /**
     * Gets a string for the given key and replaces all occurrence of "{0}"
     * with value of {@code arg0}.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0) throws MissingResourceException
    {
        return forLocale(null).getString(key, arg0);
    }

    /**
     * Gets a string for the given key and replaces all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @param  arg1  value to substitute to "{1}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0,
                                final Object arg1) throws MissingResourceException
    {
        return forLocale(null).getString(key, arg0, arg1);
    }

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = 7325356372249131588L;

        International(short key)                           {super(key);}
        International(short key, Object args)              {super(key, args);}
        @Override protected KeyConstants getKeyConstants() {return Keys.INSTANCE;}
        @Override protected IndexedResourceBundle getBundle(final Locale locale) {
            return forLocale(locale);
        }
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * @param  key  the key for the desired string.
     * @return an international string for the given key.
     */
    public static InternationalString formatInternational(final short key) {
        return new International(key);
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * @param  key   the key for the desired string.
     * @param  args  values to substitute to "{0}", "{1}", <i>etc</i>.
     * @return an international string for the given key.
     */
    public static ResourceInternationalString formatInternational(final short key, final Object... args) {
        return new International(key, args);
    }
}
