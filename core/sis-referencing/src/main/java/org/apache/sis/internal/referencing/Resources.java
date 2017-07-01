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
package org.apache.sis.internal.referencing;

import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import javax.annotation.Generated;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.util.resources.ResourceInternationalString;


/**
 * Warning and error messages that are specific to the {@code sis-referencing} module.
 * Resources in this file should not be used by any other module. For resources shared
 * by all modules in the Apache SIS project, see {@link org.apache.sis.util.resources} package.
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
         * Ambiguity between inverse flattening and semi minor axis length for “{0}”. Using inverse
         * flattening.
         */
        public static final short AmbiguousEllipsoid_1 = 1;

        /**
         * Can not compute the coordinate operation derivative.
         */
        public static final short CanNotComputeDerivative = 2;

        /**
         * Can not concatenate transforms “{0}” and “{1}”.
         */
        public static final short CanNotConcatenateTransforms_2 = 3;

        /**
         * Can not create an object of group “{1}” as an instance of class ‘{0}’.
         */
        public static final short CanNotCreateObjectAsInstanceOf_2 = 4;

        /**
         * Can not infer a grid size from the given values in {0} range.
         */
        public static final short CanNotInferGridSizeFromValues_1 = 75;

        /**
         * Can not instantiate geodetic object for “{0}”.
         */
        public static final short CanNotInstantiateGeodeticObject_1 = 5;

        /**
         * Can not map an axis from the specified coordinate system to the “{0}” direction.
         */
        public static final short CanNotMapAxisToDirection_1 = 6;

        /**
         * Target dimension {0} depends on excluded source dimensions.
         */
        public static final short CanNotSeparateTargetDimension_1 = 7;

        /**
         * Can not transform envelope to a geodetic reference system.
         */
        public static final short CanNotTransformEnvelopeToGeodetic = 8;

        /**
         * Can not use the {0} geodetic parameters: {1}
         */
        public static final short CanNotUseGeodeticParameters_2 = 9;

        /**
         * Axis directions {0} and {1} are colinear.
         */
        public static final short ColinearAxisDirections_2 = 10;

        /**
         * This result indicates if a datum shift method has been applied.
         */
        public static final short ConformanceMeansDatumShift = 11;

        /**
         * This parameter is shown for completeness, but should never have a value different than {0}
         * for this projection.
         */
        public static final short ConstantProjParameterValue_1 = 12;

        /**
         * Coordinate conversion of transformation from system “{0}” to “{1}” has not been found.
         */
        public static final short CoordinateOperationNotFound_2 = 13;

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
         * There is no factory for version {1} of “{0}” authority. Fallback on default version for
         * objects creation.
         */
        public static final short FallbackDefaultFactoryVersion_2 = 17;

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
         * Coordinate system of class ‘{0}’ can not have axis in the {1} direction.
         */
        public static final short IllegalAxisDirection_2 = 20;

        /**
         * Dimensions of “{0}” operation can not be ({1} → {2}).
         */
        public static final short IllegalOperationDimension_3 = 21;

        /**
         * This operation can not be applied to values of class ‘{0}’.
         */
        public static final short IllegalOperationForValueClass_1 = 22;

        /**
         * Parameter “{0}” can not be of type ‘{1}’.
         */
        public static final short IllegalParameterType_2 = 23;

        /**
         * Parameter “{0}” does not accept values of ‘{2}’ type. Expected an instance of ‘{1}’ or
         * derived type.
         */
        public static final short IllegalParameterValueClass_3 = 24;

        /**
         * Parameter “{0}” can not take the “{1}” value.
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
         * The transform has {2} {0,choice,0#source|1#target} dimension{2,choice,1#|2#s}, while {1} was
         * expected.
         */
        public static final short MismatchedTransformDimension_3 = 37;

        /**
         * Despite its name, this parameter is effectively “{0}”.
         */
        public static final short MisnamedParameter_1 = 38;

        /**
         * No authority was specified for code “{0}”. The expected syntax is “AUTHORITY:CODE”.
         */
        public static final short MissingAuthority_1 = 39;

        /**
         * No horizontal dimension found in “{0}”.
         */
        public static final short MissingHorizontalDimension_1 = 40;

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
         * No spatial or temporal dimension found in “{0}”
         */
        public static final short MissingSpatioTemporalDimension_1 = 42;

        /**
         * No temporal dimension found in “{0}”
         */
        public static final short MissingTemporalDimension_1 = 43;

        /**
         * Missing value for “{0}” parameter.
         */
        public static final short MissingValueForParameter_1 = 44;

        /**
         * No vertical dimension found in “{0}”
         */
        public static final short MissingVerticalDimension_1 = 45;

        /**
         * No convergence.
         */
        public static final short NoConvergence = 46;

        /**
         * No convergence for points {0} and {1}.
         */
        public static final short NoConvergenceForPoints_2 = 47;

        /**
         * No ‘{1}’ object found for code “{2}”. However only a subset of the {0} geodetic dataset has
         * been queried. See {3} for instruction about how to install the full {0} database.
         */
        public static final short NoSuchAuthorityCodeInSubset_4 = 48;

        /**
         * No ‘{1}’ object found for code “{2}” in the “{0}” geodetic dataset.
         */
        public static final short NoSuchAuthorityCode_3 = 49;

        /**
         * No operation method found for name or identifier “{0}”.
         */
        public static final short NoSuchOperationMethod_1 = 50;

        /**
         * The coordinate system axes in the given “{0}” description do not conform to the expected
         * axes according “{1}” authoritative description.
         */
        public static final short NonConformAxes_2 = 72;

        /**
         * The given “{0}” description does not conform to the “{1}” authoritative description.
         * Differences are found in {2,choice,0#conversion method|1#conversion description|2#coordinate
         * system|3#datum|4#prime meridian|5#CRS}.
         */
        public static final short NonConformCRS_3 = 73;

        /**
         * No horizontal component found in the “{0}” coordinate reference system.
         */
        public static final short NonHorizontalCRS_1 = 71;

        /**
         * Non invertible {0}×{1} matrix.
         */
        public static final short NonInvertibleMatrix_2 = 51;

        /**
         * Can not invert the “{0}” operation.
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
         * No parameter named “{1}” has been found in “{0}”.
         */
        public static final short ParameterNotFound_2 = 61;

        /**
         * Recursive call while creating an object of type ‘{0}’ for code “{1}”.
         */
        public static final short RecursiveCreateCallForCode_2 = 62;

        /**
         * Matrix is singular.
         */
        public static final short SingularMatrix = 63;

        /**
         * Unexpected dimension for a coordinate system of type ‘{0}’.
         */
        public static final short UnexpectedDimensionForCS_1 = 64;

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
     * @throws MissingResourceException if resources can't be found.
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
     * Gets a string for the given key are replace all occurrence of "{0}"
     * with values of {@code arg0}.
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
     * Gets a string for the given key are replace all occurrence of "{0}",
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
     * Gets a string for the given key are replace all occurrence of "{0}",
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
     * Gets a string for the given key are replace all occurrence of "{0}",
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
     * <div class="note"><b>API note:</b>
     * This method is redundant with the one expecting {@code Object...}, but avoid the creation
     * of a temporary array. There is no risk of confusion since the two methods delegate their
     * work to the same {@code format} method anyway.</div>
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
