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
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.ResourceInternationalString;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * Warning and error messages that are specific to the {@code sis-feature} module.
 * Resources in this file should not be used by any other module. For resources shared by
 * all modules in the Apache SIS project, see {@link org.apache.sis.util.resources} package.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
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
        public static final short CanNotBuildGridCoverage = 3;

        /**
         * Can not compute tile ({0}, {1}).
         */
        public static final short CanNotComputeTile_2 = 4;

        /**
         * Can not create a two-dimensional reference system from the “{0}” system.
         */
        public static final short CanNotCreateTwoDimensionalCRS_1 = 5;

        /**
         * Can not enumerate values in the {0} range.
         */
        public static final short CanNotEnumerateValuesInRange_1 = 6;

        /**
         * Property “{0}” is not a type that can be instantiated.
         */
        public static final short CanNotInstantiateProperty_1 = 7;

        /**
         * Some envelope dimensions can not be mapped to grid dimensions.
         */
        public static final short CanNotMapToGridDimensions = 8;

        /**
         * Can not process tile ({0}, {1}).
         */
        public static final short CanNotProcessTile_2 = 9;

        /**
         * Can not set a value of type ‘{1}’ to characteristic “{0}”.
         */
        public static final short CanNotSetCharacteristics_2 = 10;

        /**
         * Can not set this derived grid property after a call to “{0}” method.
         */
        public static final short CanNotSetDerivedGridProperty_1 = 11;

        /**
         * Type of the “{0}” property does not allow to set a value.
         */
        public static final short CanNotSetPropertyValue_1 = 12;

        /**
         * Can not simplify transfer function of sample dimension “{0}”.
         */
        public static final short CanNotSimplifyTransferFunction_1 = 13;

        /**
         * Can not update tile ({0}, {1}).
         */
        public static final short CanNotUpdateTile_2 = 14;

        /**
         * Can not visit a “{1}” {0,choice,0#filter|1#expression}.
         */
        public static final short CanNotVisit_2 = 77;

        /**
         * The two categories “{0}” and “{2}” have overlapping ranges: {1} and {3} respectively.
         */
        public static final short CategoryRangeOverlap_4 = 15;

        /**
         * Characteristics “{1}” already exists in attribute “{0}”.
         */
        public static final short CharacteristicsAlreadyExists_2 = 16;

        /**
         * No characteristics named “{1}” has been found in “{0}” attribute.
         */
        public static final short CharacteristicsNotFound_2 = 17;

        /**
         * Conversions from coverage CRS to grid cell indices.
         */
        public static final short CrsToGridConversion = 79;

        /**
         * Operation “{0}” requires a “{1}” property, but no such property has been found in “{2}”.
         */
        public static final short DependencyNotFound_3 = 18;

        /**
         * Image has zero pixel.
         */
        public static final short EmptyImage = 19;

        /**
         * Empty tile or image region.
         */
        public static final short EmptyTileOrImageRegion = 20;

        /**
         * Indices ({3}) are outside grid coverage. The value in dimension {0} shall be between
         * {1,number} and {2,number} inclusive.
         */
        public static final short GridCoordinateOutsideCoverage_4 = 21;

        /**
         * The grid envelope must have at least {0} dimensions.
         */
        public static final short GridEnvelopeMustBeNDimensional_1 = 22;

        /**
         * Envelope is outside grid coverage. Indices [{3,number} … {4,number}] in dimension {0} do not
         * intersect the [{1,number} … {2,number}] grid extent.
         */
        public static final short GridEnvelopeOutsideCoverage_5 = 23;

        /**
         * Sample value range {1} for “{0}” category is illegal.
         */
        public static final short IllegalCategoryRange_2 = 24;

        /**
         * Expected an instance of ‘{1}’ for the “{0}” characteristics, but got an instance of ‘{2}’.
         */
        public static final short IllegalCharacteristicsType_3 = 25;

        /**
         * Association “{0}” does not accept features of type ‘{2}’. Expected an instance of ‘{1}’ or
         * derived type.
         */
        public static final short IllegalFeatureType_3 = 26;

        /**
         * Illegal grid envelope [{1,number} … {2,number}] for dimension {0}.
         */
        public static final short IllegalGridEnvelope_3 = 27;

        /**
         * Can not create a grid geometry with the given “{0}” component.
         */
        public static final short IllegalGridGeometryComponent_1 = 28;

        /**
         * Type or result of “{0}” property can not be ‘{1}’ for this operation.
         */
        public static final short IllegalPropertyType_2 = 29;

        /**
         * Property “{0}” does not accept values of type ‘{2}’. Expected an instance of ‘{1}’ or
         * derived type.
         */
        public static final short IllegalPropertyValueClass_3 = 30;

        /**
         * Illegal transfer function for “{0}” category.
         */
        public static final short IllegalTransferFunction_1 = 31;

        /**
         * Image allows transparency.
         */
        public static final short ImageAllowsTransparency = 32;

        /**
         * Image has alpha channel.
         */
        public static final short ImageHasAlphaChannel = 33;

        /**
         * Image is opaque.
         */
        public static final short ImageIsOpaque = 34;

        /**
         * The ({0}, {1}) tile has an unexpected size, number of bands or sample layout.
         */
        public static final short IncompatibleTile_2 = 35;

        /**
         * Data buffer capacity is insufficient for a grid of {0} cells × {1} bands. Missing {2}
         * elements.
         */
        public static final short InsufficientBufferCapacity_3 = 36;

        /**
         * Iteration is finished.
         */
        public static final short IterationIsFinished = 38;

        /**
         * Iteration did not started.
         */
        public static final short IterationNotStarted = 39;

        /**
         * Image number of bands {0,number} does not match the number of sample dimensions
         * ({1,number}).
         */
        public static final short MismatchedBandCount_2 = 40;

        /**
         * The bands have different number of sample values.
         */
        public static final short MismatchedBandSize = 41;

        /**
         * The bands store sample values using different data types.
         */
        public static final short MismatchedDataType = 42;

        /**
         * The two images have different size or pixel coordinates.
         */
        public static final short MismatchedImageLocation = 43;

        /**
         * Image {0,choice,0#width|1#height} ({1,number} pixels) does not match the grid extent size
         * ({2,number} cells).
         */
        public static final short MismatchedImageSize_3 = 44;

        /**
         * Mismatched type for “{0}” property.
         */
        public static final short MismatchedPropertyType_1 = 45;

        /**
         * The two images use different sample models.
         */
        public static final short MismatchedSampleModel = 46;

        /**
         * The two images have different tile grid.
         */
        public static final short MismatchedTileGrid = 47;

        /**
         * An attribute for ‘{1}’ values where expected, but the “{0}” attribute specifies values of
         * type ‘{2}’.
         */
        public static final short MismatchedValueClass_3 = 48;

        /**
         * No category for value {0}.
         */
        public static final short NoCategoryForValue_1 = 49;

        /**
         * Can not infer a {0}-dimensional slice from the grid envelope. Dimension {1} has {2,number}
         * cells.
         */
        public static final short NoNDimensionalSlice_3 = 50;

        /**
         * non-linear in {0} dimension{0,choice,1#|2#s}:
         */
        public static final short NonLinearInDimensions_1 = 51;

        /**
         * Value provided by first expression is not a geometry.
         */
        public static final short NotAGeometryAtFirstExpression = 52;

        /**
         * Property “{0}” contains more than one value.
         */
        public static final short NotASingleton_1 = 53;

        /**
         * The specified dimensions are not in strictly ascending order.
         */
        public static final short NotStrictlyOrderedDimensions = 54;

        /**
         * This operation requires an image with only one band.
         */
        public static final short OperationRequiresSingleBand = 55;

        /**
         * The {0} optional library is not available. Geometric operations will ignore that library.
         * Cause is {1}.
         */
        public static final short OptionalLibraryNotFound_2 = 56;

        /**
         * The ({0,number}, {1,number}) pixel coordinate is outside iterator domain.
         */
        public static final short OutOfIteratorDomain_2 = 57;

        /**
         * Point ({0}) is outside the coverage domain.
         */
        public static final short PointOutsideCoverageDomain_1 = 37;

        /**
         * Property “{1}” already exists in feature “{0}”.
         */
        public static final short PropertyAlreadyExists_2 = 58;

        /**
         * No property named “{1}” has been found in “{0}” feature.
         */
        public static final short PropertyNotFound_2 = 59;

        /**
         * Tile ({0}, {1}) is unavailable because of error in a previous calculation attempt.
         */
        public static final short TileErrorFlagSet_2 = 60;

        /**
         * Tile ({0}, {1}) is not writable.
         */
        public static final short TileNotWritable_2 = 61;

        /**
         * Too many qualitative categories.
         */
        public static final short TooManyQualitatives = 62;

        /**
         * Coordinate operation depends on grid dimension {0}.
         */
        public static final short TransformDependsOnDimension_1 = 63;

        /**
         * The {0} geometry library is not available in current runtime environment.
         */
        public static final short UnavailableGeometryLibrary_1 = 64;

        /**
         * Can not convert grid coordinate {1} to type ‘{0}’.
         */
        public static final short UnconvertibleGridCoordinate_2 = 65;

        /**
         * Can not convert sample values.
         */
        public static final short UnconvertibleSampleValues = 66;

        /**
         * Expected {0} bands but got {1}.
         */
        public static final short UnexpectedNumberOfBands_2 = 67;

        /**
         * The “{1}” value given to “{0}” property should be separable in {2} components, but we got
         * {3}.
         */
        public static final short UnexpectedNumberOfComponents_4 = 68;

        /**
         * Raster data type ‘{0}’ is unknown or unsupported.
         */
        public static final short UnknownDataType_1 = 69;

        /**
         * Function “{0}” is unknown or unsupported.
         */
        public static final short UnknownFunction_1 = 70;

        /**
         * Feature named “{0}” has not yet been resolved.
         */
        public static final short UnresolvedFeatureName_1 = 71;

        /**
         * Coordinate reference system is unspecified.
         */
        public static final short UnspecifiedCRS = 72;

        /**
         * Grid extent is unspecified.
         */
        public static final short UnspecifiedGridExtent = 73;

        /**
         * Raster data are unspecified.
         */
        public static final short UnspecifiedRasterData = 74;

        /**
         * Coordinates transform is unspecified.
         */
        public static final short UnspecifiedTransform = 75;

        /**
         * Unsupported geometry {0}D object.
         */
        public static final short UnsupportedGeometryObject_1 = 76;

        /**
         * Sample type with a size of {0} bits can not have ‘{1}’ = “{2}” characteristic.
         */
        public static final short UnsupportedSampleType_3 = 78;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources  the path of the binary file containing resources, or {@code null} if
     *        there are no resources. The resources may be a file or an entry in a JAR file.
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

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = -667435900917846518L;

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
     * @param  key   the key for the desired string.
     * @param  args  values to substitute to "{0}", "{1}", <i>etc</i>.
     * @return an international string for the given key.
     */
    public static InternationalString formatInternational(final short key, final Object... args) {
        return new International(key, args);
    }
}
