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

import java.net.URL;
import java.util.Map;
import java.util.Locale;
import java.util.MissingResourceException;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * Warning and error messages that are specific to the {@code sis-feature} module.
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
     * @since   1.1
     * @module
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
         * Feature type ‘{0}’ is abstract.
         */
        public static final short AbstractFeatureType_1 = 1;

        /**
         * Can not assign characteristics to the “{0}” property.
         */
        public static final short CanNotAssignCharacteristics_1 = 2;

        /**
         * Can not build the grid coverage.
         */
        public static final short CanNotBuildGridCoverage = 72;

        /**
         * Can not compute tile ({0}, {1}).
         */
        public static final short CanNotComputeTile_2 = 66;

        /**
         * Can not create a two-dimensional reference system from the “{0}” system.
         */
        public static final short CanNotCreateTwoDimensionalCRS_1 = 60;

        /**
         * Can not enumerate values in the {0} range.
         */
        public static final short CanNotEnumerateValuesInRange_1 = 23;

        /**
         * Property “{0}” is not a type that can be instantiated.
         */
        public static final short CanNotInstantiateProperty_1 = 3;

        /**
         * Some envelope dimensions can not be mapped to grid dimensions.
         */
        public static final short CanNotMapToGridDimensions = 24;

        /**
         * Can not process tile ({0}, {1}).
         */
        public static final short CanNotProcessTile_2 = 70;

        /**
         * Can not set a value of type ‘{1}’ to characteristic “{0}”.
         */
        public static final short CanNotSetCharacteristics_2 = 4;

        /**
         * Can not set this derived grid property after a call to “{0}” method.
         */
        public static final short CanNotSetDerivedGridProperty_1 = 25;

        /**
         * Type of the “{0}” property does not allow to set a value.
         */
        public static final short CanNotSetPropertyValue_1 = 5;

        /**
         * Can not simplify transfer function of sample dimension “{0}”.
         */
        public static final short CanNotSimplifyTransferFunction_1 = 26;

        /**
         * Can not update tile ({0}, {1}).
         */
        public static final short CanNotUpdateTile_2 = 69;

        /**
         * The two categories “{0}” and “{2}” have overlapping ranges: {1} and {3} respectively.
         */
        public static final short CategoryRangeOverlap_4 = 27;

        /**
         * Characteristics “{1}” already exists in attribute “{0}”.
         */
        public static final short CharacteristicsAlreadyExists_2 = 6;

        /**
         * No characteristics named “{1}” has been found in “{0}” attribute.
         */
        public static final short CharacteristicsNotFound_2 = 7;

        /**
         * Operation “{0}” requires a “{1}” property, but no such property has been found in “{2}”.
         */
        public static final short DependencyNotFound_3 = 8;

        /**
         * Image has zero pixel.
         */
        public static final short EmptyImage = 73;

        /**
         * Empty tile or image region.
         */
        public static final short EmptyTileOrImageRegion = 67;

        /**
         * Indices ({3}) are outside grid coverage. The value in dimension {0} shall be between
         * {1,number} and {2,number} inclusive.
         */
        public static final short GridCoordinateOutsideCoverage_4 = 28;

        /**
         * The grid envelope must have at least {0} dimensions.
         */
        public static final short GridEnvelopeMustBeNDimensional_1 = 29;

        /**
         * Envelope is outside grid coverage. Indices [{3,number} … {4,number}] in dimension {0} do not
         * intersect the [{1,number} … {2,number}] grid extent.
         */
        public static final short GridEnvelopeOutsideCoverage_5 = 30;

        /**
         * Sample value range {1} for “{0}” category is illegal.
         */
        public static final short IllegalCategoryRange_2 = 31;

        /**
         * Association “{0}” does not accept features of type ‘{2}’. Expected an instance of ‘{1}’ or
         * derived type.
         */
        public static final short IllegalFeatureType_3 = 9;

        /**
         * Illegal grid envelope [{1,number} … {2,number}] for dimension {0}.
         */
        public static final short IllegalGridEnvelope_3 = 32;

        /**
         * Can not create a grid geometry with the given “{0}” component.
         */
        public static final short IllegalGridGeometryComponent_1 = 33;

        /**
         * Type or result of “{0}” property can not be ‘{1}’ for this operation.
         */
        public static final short IllegalPropertyType_2 = 10;

        /**
         * Property “{0}” does not accept values of type ‘{2}’. Expected an instance of ‘{1}’ or
         * derived type.
         */
        public static final short IllegalPropertyValueClass_3 = 11;

        /**
         * Illegal transfer function for “{0}” category.
         */
        public static final short IllegalTransferFunction_1 = 34;

        /**
         * Image allows transparency.
         */
        public static final short ImageAllowsTransparency = 63;

        /**
         * Image has alpha channel.
         */
        public static final short ImageHasAlphaChannel = 64;

        /**
         * Image is opaque.
         */
        public static final short ImageIsOpaque = 65;

        /**
         * The ({0}, {1}) tile has an unexpected size, number of bands or sample layout.
         */
        public static final short IncompatibleTile_2 = 35;

        /**
         * Data buffer capacity is insufficient for a grid of {0} cells × {1} bands. Missing {2}
         * elements.
         */
        public static final short InsufficientBufferCapacity_3 = 71;

        /**
         * Invalid or unsupported “{1}” expression at index {0}.
         */
        public static final short InvalidExpression_2 = 56;

        /**
         * Iteration is finished.
         */
        public static final short IterationIsFinished = 36;

        /**
         * Iteration did not started.
         */
        public static final short IterationNotStarted = 37;

        /**
         * Image number of bands {0,number} does not match the number of sample dimensions
         * ({1,number}).
         */
        public static final short MismatchedBandCount_2 = 61;

        /**
         * The bands have different number of sample values.
         */
        public static final short MismatchedBandSize = 38;

        /**
         * The bands store sample values using different data types.
         */
        public static final short MismatchedDataType = 39;

        /**
         * The two images have different size or pixel coordinates.
         */
        public static final short MismatchedImageLocation = 40;

        /**
         * Image {0,choice,0#width|1#height} ({1,number} pixels) does not match the grid extent size
         * ({2,number} cells).
         */
        public static final short MismatchedImageSize_3 = 62;

        /**
         * Mismatched type for “{0}” property.
         */
        public static final short MismatchedPropertyType_1 = 12;

        /**
         * The two images use different sample models.
         */
        public static final short MismatchedSampleModel = 41;

        /**
         * The two images have different tile grid.
         */
        public static final short MismatchedTileGrid = 42;

        /**
         * An attribute for ‘{1}’ values where expected, but the “{0}” attribute specifies values of
         * type ‘{2}’.
         */
        public static final short MismatchedValueClass_3 = 13;

        /**
         * No category for value {0}.
         */
        public static final short NoCategoryForValue_1 = 43;

        /**
         * Can not infer a {0}-dimensional slice from the grid envelope. Dimension {1} has {2,number}
         * cells.
         */
        public static final short NoNDimensionalSlice_3 = 44;

        /**
         * non-linear in {0} dimension{0,choice,1#|2#s}:
         */
        public static final short NonLinearInDimensions_1 = 45;

        /**
         * Value provided by first expression is not a geometry.
         */
        public static final short NotAGeometryAtFirstExpression = 57;

        /**
         * Property “{0}” contains more than one value.
         */
        public static final short NotASingleton_1 = 14;

        /**
         * The specified dimensions are not in strictly ascending order.
         */
        public static final short NotStrictlyOrderedDimensions = 46;

        /**
         * The {0} optional library is not available. Geometric operations will ignore that library.
         * Cause is {1}.
         */
        public static final short OptionalLibraryNotFound_2 = 19;

        /**
         * The ({0,number}, {1,number}) pixel coordinate is outside iterator domain.
         */
        public static final short OutOfIteratorDomain_2 = 47;

        public static final short PointOutsideCoverageDomain_1 = 55;

        /**
         * Property “{1}” already exists in feature “{0}”.
         */
        public static final short PropertyAlreadyExists_2 = 15;

        /**
         * No property named “{1}” has been found in “{0}” feature.
         */
        public static final short PropertyNotFound_2 = 16;

        /**
         * Tile ({0}, {1}) has the error flag set.
         */
        public static final short TileErrorFlagSet_2 = 68;

        /**
         * Too many qualitative categories.
         */
        public static final short TooManyQualitatives = 48;

        /**
         * Coordinate operation depends on grid dimension {0}.
         */
        public static final short TransformDependsOnDimension_1 = 74;

        /**
         * The {0} geometry library is not available in current runtime environment.
         */
        public static final short UnavailableGeometryLibrary_1 = 21;

        /**
         * Can not convert grid coordinate {1} to type ‘{0}’.
         */
        public static final short UnconvertibleGridCoordinate_2 = 59;

        /**
         * Expected {0} bands but got {1}.
         */
        public static final short UnexpectedNumberOfBands_2 = 49;

        /**
         * The “{1}” value given to “{0}” property should be separable in {2} components, but we got
         * {3}.
         */
        public static final short UnexpectedNumberOfComponents_4 = 17;

        /**
         * The “{0}” feature at {1} has a {3} coordinate values, while we expected a multiple of {2}.
         */
        public static final short UnexpectedNumberOfCoordinates_4 = 22;

        /**
         * Raster data type ‘{0}’ is unknown or unsupported.
         */
        public static final short UnknownDataType_1 = 50;

        /**
         * Function “{0}” is unknown or unsupported.
         */
        public static final short UnknownFunction_1 = 58;

        /**
         * Feature named “{0}” has not yet been resolved.
         */
        public static final short UnresolvedFeatureName_1 = 18;

        /**
         * Coordinate reference system is unspecified.
         */
        public static final short UnspecifiedCRS = 51;

        /**
         * Grid extent is unspecified.
         */
        public static final short UnspecifiedGridExtent = 52;

        /**
         * Raster data are unspecified.
         */
        public static final short UnspecifiedRasterData = 53;

        /**
         * Coordinates transform is unspecified.
         */
        public static final short UnspecifiedTransform = 54;

        /**
         * Unsupported geometry {0}D object.
         */
        public static final short UnsupportedGeometryObject_1 = 20;
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
     * Returns resources in the locale specified in the given property map. This convenience method looks
     * for the {@link #LOCALE_KEY} entry. If the given map is null, or contains no entry for the locale key,
     * or the value is not an instance of {@link Locale}, then this method fallback on the default locale.
     *
     * @param  properties  the map of properties, or {@code null} if none.
     * @return resources in the given locale.
     * @throws MissingResourceException if resources can't be found.
     *
     * @since 0.4
     */
    public static Resources forProperties(final Map<?,?> properties) throws MissingResourceException {
        return forLocale(getLocale(properties));
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
