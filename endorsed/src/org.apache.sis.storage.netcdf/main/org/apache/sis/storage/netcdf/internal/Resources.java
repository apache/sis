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
package org.apache.sis.storage.netcdf.internal;

import java.io.InputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.util.resources.ResourceInternationalString;


/**
 * Warning and error messages that are specific to the {@code org.apache.sis.storage.netcdf} module.
 * Resources in this file should not be used by any other module. For resources shared by
 * all modules in the Apache SIS project, see {@code org.apache.sis.util.resources} package.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
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
         * NetCDF file “{0}” provides an ambiguous axis direction for variable “{1}”. It could be
         * {2} or {3}.
         */
        public static final short AmbiguousAxisDirection_4 = 9;

        /**
         * Cannot compute data location for “{1}” variable in the “{0}” netCDF file.
         */
        public static final short CanNotComputeVariablePosition_2 = 6;

        /**
         * Cannot create the Coordinate Reference System “{1}” in the “{0}” netCDF file. The reason is:
         * {2}
         */
        public static final short CanNotCreateCRS_3 = 11;

        /**
         * Cannot create the grid geometry “{1}” in the “{0}” netCDF file. The reason is: {2}
         */
        public static final short CanNotCreateGridGeometry_3 = 12;

        /**
         * Cannot inject component “{0}” in the reference system.
         */
        public static final short CanNotInjectComponent_1 = 26;

        /**
         * Cannot relate dimension “{2}” of variable “{1}” to a coordinate system dimension in netCDF
         * file “{0}”.
         */
        public static final short CanNotRelateVariableDimension_3 = 15;

        /**
         * Cannot render an image for “{0}”. The reason is: {1}
         */
        public static final short CanNotRender_2 = 14;

        /**
         * Cannot set map projection parameter “{1}​:{2}” = {3} in the “{0}” netCDF file. The reason
         * is: {4}
         */
        public static final short CanNotSetProjectionParameter_5 = 20;

        /**
         * Cannot use axis “{0}” in a grid geometry.
         */
        public static final short CanNotUseAxis_1 = 18;

        /**
         * Cannot use UCAR library for netCDF format. Fallback on Apache SIS implementation.
         */
        public static final short CanNotUseUCAR = 4;

        /**
         * Computed localization grid for “{0}” in {1} seconds.
         */
        public static final short ComputeLocalizationGrid_2 = 22;

        /**
         * Dimension “{2}” declared by attribute “{1}” is not found in the “{0}” file.
         */
        public static final short DimensionNotFound_3 = 1;

        /**
         * Axes “{2}” and “{3}” have the same type “{1}” in netCDF file “{0}”.
         */
        public static final short DuplicatedAxisType_4 = 3;

        /**
         * Duplicated axis “{1}” in a grid of netCDF file “{0}”.
         */
        public static final short DuplicatedAxis_2 = 7;

        /**
         * The grid spans {0}° of longitude, which may be too wide for the “{1}” domain.
         */
        public static final short GridLongitudeSpanTooWide_2 = 27;

        /**
         * Illegal value “{2}” for attribute “{1}” in netCDF file “{0}”.
         */
        public static final short IllegalAttributeValue_3 = 21;

        /**
         * Illegal value range {2,number} … {3,number} for variable “{1}” in netCDF file “{0}”.
         */
        public static final short IllegalValueRange_4 = 16;

        /**
         * The CRS declared by WKT is inconsistent with the attributes of “{1}” in the “{0}” netCDF
         * file.
         */
        public static final short InconsistentCRS_2 = 29;

        /**
         * The “{2}” attribute does not match the transform inferred from the axes of “{1}” in the
         * “{0}” netCDF file.
         */
        public static final short InconsistentTransform_3 = 30;

        /**
         * Attributes “{1}” and “{2}” on variable “{0}” have different lengths: {3} and {4}
         * respectively.
         */
        public static final short MismatchedAttributeLength_5 = 24;

        /**
         * The declared size of variable “{1}” in netCDF file “{0}” is {2,number} bytes greater than
         * expected.
         */
        public static final short MismatchedVariableSize_3 = 8;

        /**
         * Variables “{1}” and “{2}” in netCDF file “{0}” does not have the same type.
         */
        public static final short MismatchedVariableType_3 = 13;

        /**
         * Missing attribute “{2}” on the “{1}” variable of netCDF file “{0}”.
         */
        public static final short MissingVariableAttribute_3 = 23;

        /**
         * Variable “{1}” or netCDF file “{0}” has a different size than its coordinate system, but no
         * resampling interval is specified.
         */
        public static final short ResamplingIntervalNotFound_2 = 17;

        /**
         * Reference system of type ‘{1}’ cannot have {2} axes. The axes found in the “{0}” netCDF file
         * are: {3}.
         */
        public static final short UnexpectedAxisCount_4 = 10;

        /**
         * Variable “{1}” in file “{0}” has a dimension “{3}” while we expected “{2}”.
         */
        public static final short UnexpectedDimensionForVariable_4 = 2;

        /**
         * The attributes of “{1}” in the “{0}” netCDF file contains unknown projection parameters:
         * {2}.
         */
        public static final short UnknownProjectionParameters_3 = 25;

        /**
         * Variable “{1}” in file “{0}” has {2,number} dimensions but only {3,number} can be associated
         * to a coordinate reference system.
         */
        public static final short UnmappedDimensions_4 = 19;

        /**
         * NetCDF file “{0}” uses unsupported data type {2} for variable “{1}”.
         */
        public static final short UnsupportedDataType_3 = 5;

        /**
         * Value “{2}” of enumeration “{0}” cannot be converted to the ‘{1}’ type.
         */
        public static final short UnsupportedEnumerationValue_3 = 28;
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

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = -4837501927964400035L;

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
     * @param  key   the key for the desired string.
     * @param  args  values to substitute to "{0}", "{1}", <i>etc</i>.
     * @return an international string for the given key.
     */
    public static InternationalString formatInternational(final short key, final Object... args) {
        return new International(key, args);
    }
}
