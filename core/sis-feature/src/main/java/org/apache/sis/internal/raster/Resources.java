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
package org.apache.sis.internal.raster;

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import javax.annotation.Generated;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * Warning and error messages that are specific to the {@code sis-raster} module.
 * Resources in this file should not be used by any other module. For resources shared by
 * all modules in the Apache SIS project, see {@link org.apache.sis.util.resources} package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class Resources extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   1.0
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
         * Can not enumerate values in the {0} range.
         */
        public static final short CanNotEnumerateValuesInRange_1 = 18;

        /**
         * Some envelope dimensions can not be mapped to grid dimensions.
         */
        public static final short CanNotMapToGridDimensions = 12;

        /**
         * Can not set this derived grid property after a call to “{0}” method.
         */
        public static final short CanNotSetDerivedGridProperty_1 = 32;

        /**
         * Can not simplify transfer function of sample dimension “{0}”.
         */
        public static final short CanNotSimplifyTransferFunction_1 = 19;

        /**
         * The two categories “{0}” and “{2}” have overlapping ranges: {1} and {3} respectively.
         */
        public static final short CategoryRangeOverlap_4 = 13;

        /**
         * Indices ({3}) are outside grid coverage. The value in dimension {0} shall be between
         * {1,number} and {2,number} inclusive.
         */
        public static final short GridCoordinateOutsideCoverage_4 = 21;

        /**
         * The grid envelope must have at least {0} dimensions.
         */
        public static final short GridEnvelopeMustBeNDimensional_1 = 25;

        /**
         * Envelope is outside grid coverage. Indices [{3,number} … {4,number}] in dimension {0} do not
         * intersect the [{1,number} … {2,number}] grid extent.
         */
        public static final short GridEnvelopeOutsideCoverage_5 = 22;

        /**
         * Sample value range {1} for “{0}” category is illegal.
         */
        public static final short IllegalCategoryRange_2 = 15;

        /**
         * Illegal grid envelope [{1,number} … {2,number}] for dimension {0}.
         */
        public static final short IllegalGridEnvelope_3 = 8;

        /**
         * Can not create a grid geometry with the given “{0}” component.
         */
        public static final short IllegalGridGeometryComponent_1 = 23;

        /**
         * Illegal transfer function for “{0}” category.
         */
        public static final short IllegalTransferFunction_1 = 16;

        /**
         * The ({0}, {1}) tile has an unexpected size, number of bands or sample layout.
         */
        public static final short IncompatibleTile_2 = 2;

        /**
         * Iteration is finished.
         */
        public static final short IterationIsFinished = 3;

        /**
         * Iteration did not started.
         */
        public static final short IterationNotStarted = 4;

        /**
         * The bands have different number of sample values.
         */
        public static final short MismatchedBandSize = 28;

        /**
         * The bands store sample values using different data types.
         */
        public static final short MismatchedDataType = 30;

        /**
         * The two images have different size or pixel coordinates.
         */
        public static final short MismatchedImageLocation = 5;

        /**
         * The two images use different sample models.
         */
        public static final short MismatchedSampleModel = 6;

        /**
         * The two images have different tile grid.
         */
        public static final short MismatchedTileGrid = 7;

        /**
         * No category for value {0}.
         */
        public static final short NoCategoryForValue_1 = 14;

        /**
         * Can not infer a {0}-dimensional slice from the grid envelope. Dimension {1} has {2,number}
         * cells.
         */
        public static final short NoNDimensionalSlice_3 = 26;

        /**
         * non-linear in {0} dimension{0,choice,1#|2#s}:
         */
        public static final short NonLinearInDimensions_1 = 20;

        /**
         * The specified dimensions are not in strictly ascending order.
         */
        public static final short NotStrictlyOrderedDimensions = 24;

        /**
         * The ({0,number}, {1,number}) pixel coordinate is outside iterator domain.
         */
        public static final short OutOfIteratorDomain_2 = 1;

        public static final short PointOutsideCoverageDomain_1 = 33;

        /**
         * Too many qualitative categories.
         */
        public static final short TooManyQualitatives = 17;

        /**
         * Expected {0} bands but got {1}.
         */
        public static final short UnexpectedNumberOfBands_2 = 27;

        /**
         * Raster data type ‘{0}’ is unknown or unsupported.
         */
        public static final short UnknownDataType_1 = 29;

        /**
         * Coordinate reference system is unspecified.
         */
        public static final short UnspecifiedCRS = 9;

        /**
         * Grid extent is unspecified.
         */
        public static final short UnspecifiedGridExtent = 10;

        /**
         * Raster data are unspecified.
         */
        public static final short UnspecifiedRasterData = 31;

        /**
         * Coordinates transform is unspecified.
         */
        public static final short UnspecifiedTransform = 11;
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
     * Gets a string for the given key and replace all occurrence of "{0}"
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
     * Gets a string for the given key and replace all occurrence of "{0}",
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
     * Gets a string for the given key and replace all occurrence of "{0}",
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
     * Gets a string for the given key and replace all occurrence of "{0}",
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
