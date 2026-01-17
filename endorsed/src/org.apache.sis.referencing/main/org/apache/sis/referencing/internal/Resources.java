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
package org.apache.sis.referencing.internal;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.util.resources.ResourceInternationalString;


/**
 * Warning and error messages that are specific to the {@code org.apache.sis.referencing} module.
 * Resources in this file should not be used by any other module. For resources shared
 * by all modules in the Apache SIS project, see {@code org.apache.sis.util.resources} package.
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
         * Accuracy declared in a geodetic dataset.
         */
        public static final short AccuracyFromGeodeticDatase = 105;

        /**
         * Ambiguity between inverse flattening and semi minor axis length for “{0}”. Using inverse
         * flattening.
         */
        public static final short AmbiguousEllipsoid_1 = 1;

        /**
         * The azimuth and distance have not been specified.
         */
        public static final short AzimuthAndDistanceNotSet = 87;

        /**
         * Cannot associate “{0}” transform to the given coordinate systems because of mismatched
         * dimensions: {1}
         */
        public static final short CanNotAssociateToCS_2 = 95;

        /**
         * Cannot create objects of type ‘{0}’ from combined URI.
         */
        public static final short CanNotCombineUriAsType_1 = 79;

        /**
         * Cannot compute the coordinate operation derivative.
         */
        public static final short CanNotComputeDerivative = 2;

        /**
         * Cannot concatenate transforms “{0}” and “{1}”.
         */
        public static final short CanNotConcatenateTransforms_2 = 3;

        /**
         * Cannot convert a point {0,choice,0#from|1#to} coordinate reference system “{1}”.
         */
        public static final short CanNotConvertCoordinates_2 = 89;

        /**
         * Cannot create an object of type “{1}” as an instance of class ‘{0}’.
         */
        public static final short CanNotCreateObjectAsInstanceOf_2 = 4;

        /**
         * Cannot find a coordinate reference system common to all given envelopes.
         */
        public static final short CanNotFindCommonCRS = 82;

        /**
         * Cannot infer a grid size from the given values in {0} range.
         */
        public static final short CanNotInferGridSizeFromValues_1 = 75;

        /**
         * Cannot instantiate geodetic object for “{0}”.
         */
        public static final short CanNotInstantiateGeodeticObject_1 = 5;

        /**
         * Cannot linearize the localization grid.
         */
        public static final short CanNotLinearizeLocalizationGrid = 45;

        /**
         * Cannot map an axis from the specified coordinate system to the “{0}” direction.
         */
        public static final short CanNotMapAxisToDirection_1 = 6;

        /**
         * Cannot parse component {1} in the combined {0,choice,0#URN|1#URL}.
         */
        public static final short CanNotParseCombinedReference_2 = 78;

        /**
         * Cannot read Well-Known Text at line {0}. Caused by: {1}
         */
        public static final short CanNotParseWKT_2 = 96;

        /**
         * Cannot separate the “{0}” coordinate reference system into sub-components.
         */
        public static final short CanNotSeparateCRS_1 = 84;

        /**
         * Target dimension {0} depends on excluded source dimensions.
         */
        public static final short CanNotSeparateTargetDimension_1 = 7;

        /**
         * Cannot separate the transform because result would have {2} {0,choice,0#source|1#target}
         * dimension{2,choice,1#|2#s} instead of {1}.
         */
        public static final short CanNotSeparateTransform_3 = 83;

        /**
         * Cannot transform the ({0,number}, {1,number}) coordinates.
         */
        public static final short CanNotTransformCoordinates_2 = 85;

        /**
         * Cannot transform envelope to a geodetic reference system.
         */
        public static final short CanNotTransformEnvelopeToGeodetic = 8;

        /**
         * Cannot transform the given geometry.
         */
        public static final short CanNotTransformGeometry = 86;

        /**
         * Cannot use the {0} geodetic parameters. Caused by: {1}
         */
        public static final short CanNotUseGeodeticParameters_2 = 9;

        /**
         * Cannot parse the “{0}” element:
         */
        public static final short CannotParseElement_1 = 101;

        /**
         * Axis directions {0} and {1} are colinear.
         */
        public static final short ColinearAxisDirections_2 = 10;

        /**
         * This result indicates if a datum shift method has been applied.
         */
        public static final short ConformanceMeansDatumShift = 11;

        /**
         * A constant value is required for coordinate axis “{0}”.
         */
        public static final short ConstantCoordinateValueRequired_1 = 109;

        /**
         * This parameter is shown for completeness, but should never have a value different than {0}
         * for this projection.
         */
        public static final short ConstantProjParameterValue_1 = 12;

        /**
         * Coordinate operation from system “{0}” to “{1}” has not been found.
         */
        public static final short CoordinateOperationNotFound_2 = 13;

        /**
         * No information about how to change from datum “{0}” to “{1}”.
         */
        public static final short DatumChangeNotFound_2 = 106;

        /**
         * Datum shift files are searched in the “{0}” directory.
         */
        public static final short DatumChangesDirectory_1 = 92;

        /**
         * Origin of temporal datum shall be a date.
         */
        public static final short DatumOriginShallBeDate = 14;

        /**
         * Code “{0}” is deprecated and replaced by code {1}. Reason is: {2}
         */
        public static final short DeprecatedCode_3 = 15;

        /**
         * Name or alias for parameter “{0}” at index {1} conflict with name “{2}” at index {3}.
         */
        public static final short DuplicatedParameterName_4 = 16;

        /**
         * Compound coordinate reference systems cannot contain two {0,choice,1#horizontal|2#vertical}
         * components.
         */
        public static final short DuplicatedSpatialComponents_1 = 76;

        /**
         * … {0} elements omitted …
         */
        public static final short ElementsOmitted_1 = 99;

        /**
         * Compound coordinate reference systems should not contain ellipsoidal height. Use a
         * three-dimensional {0,choice,0#geographic|1#projected} system instead.
         */
        public static final short EllipsoidalHeightNotAllowed_1 = 77;

        /**
         * Definitions from public sources. When a definition corresponds to an EPSG object (ignoring
         * metadata), the EPSG code is provided as a reference where to find the complete definition.
         */
        public static final short FallbackAuthorityNotice = 42;

        /**
         * There is no local registry for version {1} of “{0}” authority. Fallback on default version
         * for objects creation.
         */
        public static final short FallbackDefaultFactoryVersion_2 = 17;

        /**
         * Cannot find {0} file named “{1}”.
         */
        public static final short FileNotFound_2 = 90;

        /**
         * Cannot parse “{1}” as a file in the {0} format.
         */
        public static final short FileNotReadable_2 = 91;

        /**
         * {0} geodetic dataset version {1} on “{2}” version {3}.
         */
        public static final short GeodeticDataBase_4 = 18;

        /**
         * More than one service provider of type ‘{0}’ are declared for “{1}”. Only the first provider
         * (an instance of ‘{2}’) will be used.
         */
        public static final short IgnoredServiceProvider_3 = 19;

        /**
         * Coordinate system of class ‘{0}’ cannot have axis in the {1} direction.
         */
        public static final short IllegalAxisDirection_2 = 20;

        /**
         * This operation cannot be applied to values of class ‘{0}’.
         */
        public static final short IllegalOperationForValueClass_1 = 22;

        /**
         * Parameter “{0}” cannot be of type ‘{1}’.
         */
        public static final short IllegalParameterType_2 = 23;

        /**
         * Parameter “{0}” does not accept values of ‘{2}’ type. Expected an instance of ‘{1}’ or
         * derived type.
         */
        public static final short IllegalParameterValueClass_3 = 24;

        /**
         * Parameter “{0}” cannot take the “{1}” value.
         */
        public static final short IllegalParameterValue_2 = 25;

        /**
         * Unit of measurement “{1}” is not valid for “{0}” values.
         */
        public static final short IllegalUnitFor_2 = 26;

        /**
         * Incompatible coordinate system types.
         */
        public static final short IncompatibleCoordinateSystemTypes = 27;

        /**
         * Datum of “{1}” shall be “{0}”.
         */
        public static final short IncompatibleDatum_2 = 28;

        /**
         * All dynamic components should have the same epoch, but found “{0}” and “{1}”.
         */
        public static final short InconsistentEpochs_2 = 21;

        /**
         * Inverse operation uses this parameter value with opposite sign.
         */
        public static final short InverseOperationUsesOppositeSign = 29;

        /**
         * Inverse operation uses the same parameter value.
         */
        public static final short InverseOperationUsesSameSign = 30;

        /**
         * Latitudes {0} and {1} are opposite.
         */
        public static final short LatitudesAreOpposite_2 = 31;

        /**
         * Loading datum shift file “{0}”.
         */
        public static final short LoadingDatumShiftFile_1 = 32;

        /**
         * Misaligned datum shift grid in “{0}”.
         */
        public static final short MisalignedDatumShiftGrid_1 = 94;

        /**
         * The “{1}” parameter could have been omitted. But it has been given a value of {2} which does
         * not match the definition of the “{0}” ellipsoid.
         */
        public static final short MismatchedEllipsoidAxisLength_3 = 33;

        /**
         * No coordinate operation from “{0}” to “{1}” because of mismatched factories.
         */
        public static final short MismatchedOperationFactories_2 = 34;

        /**
         * Mismatched descriptor for “{0}” parameter.
         */
        public static final short MismatchedParameterDescriptor_1 = 35;

        /**
         * Expected the “{0}” prime meridian but found “{1}”.
         */
        public static final short MismatchedPrimeMeridian_2 = 36;

        /**
         * Invalid coordinate operation step {0}, because the reference system “{1}” cannot be followed
         * by “{2}”.
         */
        public static final short MismatchedSourceTargetCRS_3 = 100;

        /**
         * Despite its name, this parameter is effectively “{0}”.
         */
        public static final short MisnamedParameter_1 = 38;

        /**
         * Missing or empty “ID[…]” element for “{0}”.
         */
        public static final short MissingAuthorityCode_1 = 37;

        /**
         * No authority was specified for code “{0}”. The expected syntax is “AUTHORITY:CODE”.
         */
        public static final short MissingAuthority_1 = 39;

        /**
         * Not enough dimension in ‘MathTransform’ input or output coordinates for the interpolation
         * points.
         */
        public static final short MissingInterpolationOrdinates = 41;

        /**
         * Missing parameter values for “{0}” coordinate operation.
         */
        public static final short MissingParameterValues_1 = 74;

        /**
         * A coordinate epoch is mandatory because the “{0}” reference system is dynamic.
         */
        public static final short MissingReferenceFrameEpoch_1 = 102;

        /**
         * Missing value for the “{0}” parameter.
         */
        public static final short MissingValueForParameter_1 = 44;

        /**
         * The localization grid still have some undefined source or target coordinates.
         */
        public static final short MissingValuesInLocalizationGrid = 81;

        /**
         * No convergence.
         */
        public static final short NoConvergence = 46;

        /**
         * No convergence for points {0} and {1}.
         */
        public static final short NoConvergenceForPoints_2 = 47;

        /**
         * No ‘{1}’ object found for code “{2}”. However, only a subset of the {0} geodetic dataset has
         * been queried. See {3} for instruction about how to install the full {0} database.
         */
        public static final short NoSuchAuthorityCodeInSubset_4 = 48;

        /**
         * No ‘{1}’ object found for code “{2}” in the “{0}” geodetic dataset.
         */
        public static final short NoSuchAuthorityCode_3 = 49;

        /**
         * No operation method found for name or identifier “{0}”. Only methods associated to Java code
         * are supported. See {1} for the list of available methods.
         */
        public static final short NoSuchOperationMethod_2 = 50;

        /**
         * The coordinate system axes in the given “{0}” description do not conform to the expected
         * axes according “{1}” authoritative description.
         */
        public static final short NonConformAxes_2 = 72;

        /**
         * The given “{0}” description does not conform to the “{1}” authoritative description.
         * Differences are found in {2,choice,0#conversion method|1#conversion description|2#coordinate
         * system|3#ellipsoid|4#prime meridian|5#datum|6#CRS}.
         */
        public static final short NonConformCRS_3 = 73;

        /**
         * This interpolated transform is not invertible because the values are not ordered.
         */
        public static final short NonInvertibleBecauseUnordered = 108;

        /**
         * Non invertible {0}×{1} matrix.
         */
        public static final short NonInvertibleMatrix_2 = 51;

        /**
         * Cannot invert the “{0}” operation.
         */
        public static final short NonInvertibleOperation_1 = 52;

        /**
         * Transform is not invertible.
         */
        public static final short NonInvertibleTransform = 53;

        /**
         * Unit conversion from “{0}” to “{1}” is non-linear.
         */
        public static final short NonLinearUnitConversion_2 = 54;

        /**
         * The “{0}” sequence is not monotonic.
         */
        public static final short NonMonotonicSequence_1 = 55;

        /**
         * Axis directions {0} and {1} are not perpendicular.
         */
        public static final short NonPerpendicularDirections_2 = 56;

        /**
         * Scale is not uniform.
         */
        public static final short NonUniformScale = 57;

        /**
         * Datum “{1}” is not a member of the “{0}” datum ensemble.
         */
        public static final short NotAMemberOfDatumEnsemble_2 = 104;

        /**
         * Matrix is not skew-symmetric.
         */
        public static final short NotASkewSymmetricMatrix = 58;

        /**
         * Transform is not affine.
         */
        public static final short NotAnAffineTransform = 59;

        /**
         * This parameter borrowed from the “{0}” projection is not formally a parameter of this
         * projection.
         */
        public static final short NotFormalProjectionParameter_1 = 60;

        /**
         * Operation “{1}” of class ‘{0}’ has no mathematical transform.
         */
        public static final short OperationHasNoTransform_2 = 43;

        /**
         * No parameter named “{1}” has been found in “{0}”.
         */
        public static final short ParameterNotFound_2 = 61;

        /**
         * Points are not on a regular grid.
         */
        public static final short PointsAreNotOnRegularGrid = 40;

        /**
         * Recursive call while creating an object of type ‘{0}’ for code “{1}”.
         */
        public static final short RecursiveCreateCallForCode_2 = 62;

        /**
         * The only valid entries are ±90° or equivalent in alternative angle units.
         */
        public static final short RestrictedToPoleLatitudes = 71;

        /**
         * All members of a datum ensemble shall have the same conventional reference system.
         */
        public static final short ShallHaveSameConventionalRS = 103;

        /**
         * Matrix is singular.
         */
        public static final short SingularMatrix = 63;

        /**
         * The {0,choice,0#start|1#end} point has not been specified.
         */
        public static final short StartOrEndPointNotSet_1 = 88;

        /**
         * Syntax error for Well-Known Text alias at line {0}.
         */
        public static final short SyntaxErrorForAlias_1 = 97;

        /**
         * Cannot use the {0} geodetic dataset because the database at “{1}” does not contain a “{2}”
         * table and no script for automatic installation was found.
         */
        public static final short TableNotFound_3 = 107;

        /**
         * Combined URI contains unexpected components.
         */
        public static final short UnexpectedComponentInURI = 80;

        /**
         * Unexpected dimension for a coordinate system of type ‘{0}’.
         */
        public static final short UnexpectedDimensionForCS_1 = 64;

        /**
         * Unexpected text “{1}” at line {0}. WKT for new object should start with a non-indented line.
         */
        public static final short UnexpectedTextAtLine_2 = 98;

        /**
         * Parameter “{0}” does not expect unit.
         */
        public static final short UnitlessParameter_1 = 65;

        /**
         * Authority “{0}” is unknown.
         */
        public static final short UnknownAuthority_1 = 66;

        /**
         * Axis direction “{0}” is unknown.
         */
        public static final short UnknownAxisDirection_1 = 67;

        /**
         * This affine transform is unmodifiable.
         */
        public static final short UnmodifiableAffineTransform = 68;

        /**
         * Dimensions have not been specified.
         */
        public static final short UnspecifiedDimensions = 69;

        /**
         * Parameter values have not been specified.
         */
        public static final short UnspecifiedParameterValues = 70;

        /**
         * Using datum shift grid from “{0}” to “{1}” created on {2} (updated on {3}).
         */
        public static final short UsingDatumShiftGrid_4 = 93;
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
     * Returns resources in the locale specified in the given property map. This convenience method looks
     * for the {@link #LOCALE_KEY} entry. If the given map is null, or contains no entry for the locale key,
     * or the value is not an instance of {@link Locale}, then this method fallback on the default locale.
     *
     * @param  properties  the map of properties, or {@code null} if none.
     * @return resources in the given locale.
     * @throws MissingResourceException if resources cannot be found.
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
        private static final long serialVersionUID = 4553487496835099424L;

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
     * <h4>API note</h4>
     * This method is redundant with the one expecting {@code Object...}, but avoid the creation
     * of a temporary array. There is no risk of confusion since the two methods delegate their
     * work to the same {@code format} method anyway.
     *
     * @param  key  the key for the desired string.
     * @param  arg  values to substitute to "{0}".
     * @return an international string for the given key.
     */
    public static InternationalString formatInternational(final short key, final Object arg) {
        return new International(key, arg);
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
