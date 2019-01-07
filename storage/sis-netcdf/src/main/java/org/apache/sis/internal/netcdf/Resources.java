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
package org.apache.sis.internal.netcdf;

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import javax.annotation.Generated;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * Warning and error messages that are specific to the {@code sis-netcdf} module.
 * Resources in this file should not be used by any other module. For resources shared by
 * all modules in the Apache SIS project, see {@link org.apache.sis.util.resources} package.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class Resources extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.8
     * @module
     */
    @Generated("org.apache.sis.util.resources.IndexedResourceCompiler")
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
         * NetCDF file “{0}” provides an ambiguous axis direction for variable “{1}”. It could be
         * {2} or {3}.
         */
        public static final short AmbiguousAxisDirection_4 = 9;

        /**
         * Can not compute data location for “{1}” variable in the “{0}” netCDF file.
         */
        public static final short CanNotComputeVariablePosition_2 = 6;

        /**
         * Can not create the Coordinate Reference System for grid geometry “{1}” in the “{0}” netCDF
         * file. The reason is: {2}
         */
        public static final short CanNotCreateCRS_3 = 11;

        /**
         * Can not create the grid geometry “{1}” in the “{0}” netCDF file. The reason is: {2}
         */
        public static final short CanNotCreateGridGeometry_3 = 12;

        /**
         * Can not render an image for “{0}”. The reason is: {1}
         */
        public static final short CanNotRender_2 = 14;

        /**
         * Can not use UCAR library for netCDF format. Fallback on Apache SIS implementation.
         */
        public static final short CanNotUseUCAR = 4;

        /**
         * Dimension “{2}” declared by attribute “{1}” is not found in the “{0}” file.
         */
        public static final short DimensionNotFound_3 = 1;

        /**
         * Duplicated reference to “{1}” in netCDF file “{0}”.
         */
        public static final short DuplicatedReference_2 = 7;

        /**
         * The declared size of variable “{1}” in netCDF file “{0}” is {2} bytes greater than expected.
         */
        public static final short MismatchedVariableSize_3 = 8;

        /**
         * Variables “{1}” and “{2}” in netCDF file “{0}” does not have the same type.
         */
        public static final short MismatchedVariableType_3 = 13;

        /**
         * Reference system of type ‘{1}’ can not have {2} axes. The axes found in the “{0}” netCDF
         * file are: {3}.
         */
        public static final short UnexpectedAxisCount_4 = 10;

        /**
         * Variable “{1}” in file “{0}” has a dimension “{3}” while we expected “{2}”.
         */
        public static final short UnexpectedDimensionForVariable_4 = 2;

        /**
         * NetCDF file “{0}” uses unsupported data type {2} for variable “{1}”.
         */
        public static final short UnsupportedDataType_3 = 5;

        /**
         * Variable “{1}” is not found in the “{0}” file.
         */
        public static final short VariableNotFound_2 = 3;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources  the path of the binary file containing resources, or {@code null} if
     *        there is no resources. The resources may be a file or an entry in a JAR file.
     */
    public Resources(final URL resources) {
        super(resources);
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
     * @throws MissingResourceException if resources can not be found.
     */
    public static Resources forLocale(final Locale locale) throws MissingResourceException {
        return getBundle(Resources.class, locale);
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
     * Gets a string for the given key and replaces all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @param  arg1  value to substitute to "{1}".
     * @param  arg2  value to substitute to "{2}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0,
                                final Object arg1,
                                final Object arg2) throws MissingResourceException
    {
        return forLocale(null).getString(key, arg0, arg1, arg2);
    }

    /**
     * Gets a string for the given key and replaces all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @param  arg1  value to substitute to "{1}".
     * @param  arg2  value to substitute to "{2}".
     * @param  arg3  value to substitute to "{3}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0,
                                final Object arg1,
                                final Object arg2,
                                final Object arg3) throws MissingResourceException
    {
        return forLocale(null).getString(key, arg0, arg1, arg2, arg3);
    }
}
