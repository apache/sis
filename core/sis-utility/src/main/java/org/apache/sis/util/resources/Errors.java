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
package org.apache.sis.util.resources;

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import org.opengis.util.InternationalString;


/**
 * Locale-dependent resources for error messages.
 *
 * {@section Argument order convention}
 * This resource bundle applies the same convention than JUnit: for every {@code format(…)} method,
 * the first arguments provide information about the context in which the error occurred (e.g. the
 * name of a method argument or the range of valid values), while the erroneous values that caused
 * the error are last. Note that being the last programmatic parameter does not means that the value
 * will appears last in the formatted text, since every localized message can reorder the parameters
 * as they want.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.2)
 * @version 0.3
 * @module
 */
public final class Errors extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.3 (derived from geotk-2.2)
     * @version 0.3
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
         * No element can be added to this set because properties ‘{0}’ and ‘{1}’ are mutually
         * exclusive.
         */
        public static final int CanNotAddToExclusiveSet_2 = 87;

        /**
         * Can not compute the derivative.
         */
        public static final int CanNotComputeDerivative = 44;

        /**
         * Can not connect to “{0}”.
         */
        public static final int CanNotConnectTo_1 = 114;

        /**
         * Can not convert from type ‘{0}’ to type ‘{1}’.
         */
        public static final int CanNotConvertFromType_2 = 72;

        /**
         * Can not convert value “{0}” to type ‘{1}’.
         */
        public static final int CanNotConvertValue_2 = 74;

        /**
         * Can not instantiate an object of type ‘{0}’.
         */
        public static final int CanNotInstantiate_1 = 81;

        /**
         * Can not map an axis from “{0}” to direction “{1}”.
         */
        public static final int CanNotMapAxisToDirection_2 = 123;

        /**
         * Can not open “{0}”.
         */
        public static final int CanNotOpen_1 = 97;

        /**
         * Can not parse “{1}” as a file in the {0} format.
         */
        public static final int CanNotParseFile_2 = 79;

        /**
         * Can not read “{0}”.
         */
        public static final int CanNotRead_1 = 108;

        /**
         * Can not represent “{1}” in the {0} format.
         */
        public static final int CanNotRepresentInFormat_2 = 110;

        /**
         * Can not set a value for property “{0}”.
         */
        public static final int CanNotSetPropertyValue_1 = 75;

        /**
         * Class ‘{0}’ is not final.
         */
        public static final int ClassNotFinal_1 = 71;

        /**
         * Can not clone an object of type ‘{0}’.
         */
        public static final int CloneNotSupported_1 = 42;

        /**
         * Axis directions {0} and {1} are colinear.
         */
        public static final int ColinearAxisDirections_2 = 122;

        /**
         * Thread “{0}” is dead.
         */
        public static final int DeadThread_1 = 43;

        /**
         * Element “{0}” is duplicated.
         */
        public static final int DuplicatedElement_1 = 116;

        /**
         * Identifier “{0}” is duplicated.
         */
        public static final int DuplicatedIdentifier_1 = 38;

        /**
         * Option “{0}” is duplicated.
         */
        public static final int DuplicatedOption_1 = 100;

        /**
         * Element “{0}” is already present.
         */
        public static final int ElementAlreadyPresent_1 = 36;

        /**
         * Argument ‘{0}’ shall not be empty.
         */
        public static final int EmptyArgument_1 = 1;

        /**
         * The dictionary shall contain at least one entry.
         */
        public static final int EmptyDictionary = 54;

        /**
         * Envelope must be at least two-dimensional and non-empty.
         */
        public static final int EmptyEnvelope2D = 88;

        /**
         * Property named “{0}” shall not be empty.
         */
        public static final int EmptyProperty_1 = 55;

        /**
         * Argument ‘{0}’ shall not contain more than {1} elements. A number of {2} is excessive.
         */
        public static final int ExcessiveArgumentSize_3 = 52;

        /**
         * A size of {1} elements is excessive for the “{0}” list.
         */
        public static final int ExcessiveListSize_2 = 94;

        /**
         * Attribute “{0}” is not allowed for an object of type ‘{1}’.
         */
        public static final int ForbiddenAttribute_2 = 21;

        /**
         * Identifier “{0}” is already associated to another object.
         */
        public static final int IdentifierAlreadyBound_1 = 50;

        /**
         * Argument ‘{0}’ can not be an instance of ‘{1}’.
         */
        public static final int IllegalArgumentClass_2 = 17;

        /**
         * Argument ‘{0}’ can not be an instance of ‘{2}’. Expected an instance of ‘{1}’ or derived
         * type.
         */
        public static final int IllegalArgumentClass_3 = 2;

        /**
         * Argument ‘{0}’ can not take the “{1}” value, because the ‘{2}’ field can not take the “{3}”
         * value.
         */
        public static final int IllegalArgumentField_4 = 15;

        /**
         * Argument ‘{0}’ can not take the “{1}” value.
         */
        public static final int IllegalArgumentValue_2 = 14;

        /**
         * Coordinate system of class ‘{0}’ can not have axis in the {1} direction.
         */
        public static final int IllegalAxisDirection_2 = 128;

        /**
         * Illegal bits pattern: {0}.
         */
        public static final int IllegalBitsPattern_1 = 16;

        /**
         * Class ‘{1}’ is illegal. It must be ‘{0}’ or a derived class.
         */
        public static final int IllegalClass_2 = 3;

        /**
         * The “{1}” pattern can not be applied to formating of objects of type ‘{0}’.
         */
        public static final int IllegalFormatPatternForClass_2 = 29;

        /**
         * The “{0}” language is not recognized.
         */
        public static final int IllegalLanguageCode_1 = 12;

        /**
         * Member “{0}” can not be associated to type “{1}”.
         */
        public static final int IllegalMemberType_2 = 106;

        /**
         * Option ‘{0}’ can not take the “{1}” value.
         */
        public static final int IllegalOptionValue_2 = 101;

        /**
         * The [{0} … {1}] range of ordinate values is not valid for the “{2}” axis.
         */
        public static final int IllegalOrdinateRange_3 = 5;

        /**
         * Property ‘{0}’ does not accept instances of ‘{1}’.
         */
        public static final int IllegalPropertyClass_2 = 62;

        /**
         * Range [{0} … {1}] is not valid.
         */
        public static final int IllegalRange_2 = 11;

        /**
         * Value {1} for “{0}” is not a valid Unicode code point.
         */
        public static final int IllegalUnicodeCodePoint_2 = 112;

        /**
         * Unit of measurement “{1}” is not valid for “{0}” values.
         */
        public static final int IllegalUnitFor_2 = 129;

        /**
         * Incompatible coordinate system types.
         */
        public static final int IncompatibleCoordinateSystemTypes = 130;

        /**
         * Property “{0}” has an incompatible value.
         */
        public static final int IncompatiblePropertyValue_1 = 86;

        /**
         * Units “{0}” and “{1}” are incompatible.
         */
        public static final int IncompatibleUnits_2 = 67;

        /**
         * Value “{1}” of attribute ‘{0}’ is inconsistent with other attributes.
         */
        public static final int InconsistentAttribute_2 = 27;

        /**
         * Inconsistent table columns.
         */
        public static final int InconsistentTableColumns = 40;

        /**
         * Index {0} is out of bounds.
         */
        public static final int IndexOutOfBounds_1 = 4;

        /**
         * Indices ({0}, {1}) are out of bounds.
         */
        public static final int IndicesOutOfBounds_2 = 120;

        /**
         * Argument ‘{0}’ can not take an infinite value.
         */
        public static final int InfiniteArgumentValue_1 = 45;

        /**
         * Infinite recursivity.
         */
        public static final int InfiniteRecursivity = 51;

        /**
         * Argument ‘{0}’ shall contain at least {1} elements. A number of {2} is insufficient.
         */
        public static final int InsufficientArgumentSize_3 = 53;

        /**
         * A different value is already associated to the “{0}” key.
         */
        public static final int KeyCollision_1 = 19;

        /**
         * Attribute “{0}” is mandatory for an object of type ‘{1}’.
         */
        public static final int MandatoryAttribute_2 = 22;

        /**
         * Mismatched array lengths.
         */
        public static final int MismatchedArrayLengths = 111;

        /**
         * The coordinate reference system must be the same for all objects.
         */
        public static final int MismatchedCRS = 57;

        /**
         * Mismatched object dimensions: {0}D and {1}D.
         */
        public static final int MismatchedDimension_2 = 60;

        /**
         * Argument ‘{0}’ has {2} dimension{2,choice,1#|2#s}, while {1} was expected.
         */
        public static final int MismatchedDimension_3 = 58;

        /**
         * Mismatched matrix sizes: expected {0}×{1} but got {2}×{3}.
         */
        public static final int MismatchedMatrixSize_4 = 118;

        /**
         * This operation requires the “{0}” module.
         */
        public static final int MissingRequiredModule_1 = 84;

        /**
         * Missing scheme in URI.
         */
        public static final int MissingSchemeInURI = 109;

        /**
         * Missing value for option “{0}”.
         */
        public static final int MissingValueForOption_1 = 99;

        /**
         * Missing value for property “{0}”.
         */
        public static final int MissingValueForProperty_1 = 85;

        /**
         * Missing value in the “{0}” column.
         */
        public static final int MissingValueInColumn_1 = 77;

        /**
         * Options “{0}” and “{1}” are mutually exclusive.
         */
        public static final int MutuallyExclusiveOptions_2 = 103;

        /**
         * Argument ‘{0}’ shall not be negative. The given value was {1}.
         */
        public static final int NegativeArgument_2 = 8;

        /**
         * Can not create a “{0}” array of negative length.
         */
        public static final int NegativeArrayLength_1 = 78;

        /**
         * No convergence for points {0} and {1}.
         */
        public static final int NoConvergenceForPoints_2 = 117;

        /**
         * Element “{0}” has not been found.
         */
        public static final int NoSuchElement_1 = 96;

        /**
         * No property named “{0}” has been found in “{1}”.
         */
        public static final int NoSuchProperty_2 = 73;

        /**
         * No unit of measurement has been specified.
         */
        public static final int NoUnit = 68;

        /**
         * Node “{0}” can not be a child of itself.
         */
        public static final int NodeChildOfItself_1 = 37;

        /**
         * Node “{0}” already has another parent.
         */
        public static final int NodeHasAnotherParent_1 = 35;

        /**
         * Node “{0}” has no parent.
         */
        public static final int NodeHasNoParent_1 = 34;

        /**
         * Node “{0}” is a leaf.
         */
        public static final int NodeIsLeaf_1 = 90;

        /**
         * No “{0}” node found.
         */
        public static final int NodeNotFound_1 = 39;

        /**
         * “{0}” is not an angular unit.
         */
        public static final int NonAngularUnit_1 = 46;

        /**
         * Missing a ‘{1}’ parenthesis in “{0}”.
         */
        public static final int NonEquilibratedParenthesis_2 = 59;

        /**
         * Conversion is not invertible.
         */
        public static final int NonInvertibleConversion = 82;

        /**
         * Non invertible {0}×{1} matrix.
         */
        public static final int NonInvertibleMatrix_2 = 124;

        /**
         * Transform is not invertible.
         */
        public static final int NonInvertibleTransform = 83;

        /**
         * Unit conversion from “{0}” to “{1}” is non-linear.
         */
        public static final int NonLinearUnitConversion_2 = 131;

        /**
         * “{0}” is not a linear unit.
         */
        public static final int NonLinearUnit_1 = 47;

        /**
         * “{0}” is not a scale unit.
         */
        public static final int NonScaleUnit_1 = 48;

        /**
         * “{0}” is not a time unit.
         */
        public static final int NonTemporalUnit_1 = 49;

        /**
         * Scale is not uniform.
         */
        public static final int NonUniformScale = 126;

        /**
         * Argument ‘{0}’ shall not be NaN (Not-a-Number).
         */
        public static final int NotANumber_1 = 9;

        /**
         * Class ‘{0}’ is not a primitive type wrapper.
         */
        public static final int NotAPrimitiveWrapper_1 = 10;

        /**
         * Matrix is not skew-symmetric.
         */
        public static final int NotASkewSymmetricMatrix = 127;

        /**
         * Text “{0}” is not a Unicode identifier.
         */
        public static final int NotAUnicodeIdentifier_1 = 113;

        /**
         * Transform is not affine.
         */
        public static final int NotAnAffineTransform = 121;

        /**
         * Class ‘{0}’ is not a comparable.
         */
        public static final int NotComparableClass_1 = 66;

        /**
         * Argument ‘{0}’ shall not be null.
         */
        public static final int NullArgument_1 = 0;

        /**
         * Null key is not allowed in this dictionary.
         */
        public static final int NullMapKey = 64;

        /**
         * Null values are not allowed in this dictionary.
         */
        public static final int NullMapValue = 65;

        /**
         * Array length is {0}, while we expected an even length.
         */
        public static final int OddArrayLength_1 = 61;

        /**
         * Recursive call while creating an object for the “{0}” key.
         */
        public static final int RecursiveCreateCallForKey_1 = 18;

        /**
         * A decimal separator is required.
         */
        public static final int RequireDecimalSeparator = 33;

        /**
         * Matrix is singular.
         */
        public static final int SingularMatrix = 125;

        /**
         * Thread “{0}” seems stalled.
         */
        public static final int StalledThread_1 = 63;

        /**
         * Can not move backward in the “{0}” stream.
         */
        public static final int StreamIsForwardOnly_1 = 95;

        /**
         * Expected at least {0} argument{0,choice,1#|2#s}, but got {1}.
         */
        public static final int TooFewArguments_2 = 104;

        /**
         * Expected at most {0} argument{0,choice,1#|2#s}, but got {1}.
         */
        public static final int TooManyArguments_2 = 105;

        /**
         * Ordering between “{0}” and “{1}” elements is undefined.
         */
        public static final int UndefinedOrderingForElements_2 = 70;

        /**
         * Expected an array of length {0}, but got {1}.
         */
        public static final int UnexpectedArrayLength_2 = 119;

        /**
         * Unexpected change in ‘{0}’.
         */
        public static final int UnexpectedChange_1 = 56;

        /**
         * Unexpected end of file while reading “{0}”.
         */
        public static final int UnexpectedEndOfFile_1 = 91;

        /**
         * More characters were expected at the end of “{0}”.
         */
        public static final int UnexpectedEndOfString_1 = 30;

        /**
         * File “{1}” seems to be encoded in an other format than {0}.
         */
        public static final int UnexpectedFileFormat_2 = 92;

        /**
         * Command “{0}” is not recognized.
         */
        public static final int UnknownCommand_1 = 102;

        /**
         * Unknown enumeration value: {0}.
         */
        public static final int UnknownEnumValue_1 = 115;

        /**
         * Format of “{0}” is not recognized.
         */
        public static final int UnknownFormatFor_1 = 107;

        /**
         * Option “{0}” is not recognized.
         */
        public static final int UnknownOption_1 = 98;

        /**
         * Type of the “{0}” property is unknown.
         */
        public static final int UnknownTypeForProperty_1 = 80;

        /**
         * Type ‘{0}’ is unknown in this context.
         */
        public static final int UnknownType_1 = 76;

        /**
         * This affine transform is unmodifiable.
         */
        public static final int UnmodifiableAffineTransform = 23;

        /**
         * The cell at column “{1}” of row “{0}” is unmodifiable.
         */
        public static final int UnmodifiableCellValue_2 = 89;

        /**
         * This geometry is unmodifiable.
         */
        public static final int UnmodifiableGeometry = 24;

        /**
         * This metadata is unmodifiable.
         */
        public static final int UnmodifiableMetadata = 25;

        /**
         * Object ‘{0}’ is unmodifiable.
         */
        public static final int UnmodifiableObject_1 = 26;

        /**
         * Text “{1}” can not be parsed as an object of type ‘{0}’.
         */
        public static final int UnparsableStringForClass_2 = 31;

        /**
         * Text “{1}” can not be parsed as an object of type ‘{0}’, because of the “{2}” characters.
         */
        public static final int UnparsableStringForClass_3 = 32;

        /**
         * No format is specified for objects of class ‘{0}’.
         */
        public static final int UnspecifiedFormatForClass_1 = 41;

        /**
         * Can not handle instances of ‘{0}’ because arbitrary implementations are not yet supported.
         */
        public static final int UnsupportedImplementation_1 = 28;

        /**
         * The ‘{0}’ operation is unsupported.
         */
        public static final int UnsupportedOperation_1 = 20;

        /**
         * The ‘{0}’ type is unsupported.
         */
        public static final int UnsupportedType_1 = 69;

        /**
         * Version {0} is not supported.
         */
        public static final int UnsupportedVersion_1 = 93;

        /**
         * A value is already defined for “{0}”.
         */
        public static final int ValueAlreadyDefined_1 = 13;

        /**
         * Value ‘{0}’={1} is invalid. Expected a number greater than 0.
         */
        public static final int ValueNotGreaterThanZero_2 = 7;

        /**
         * Value ‘{0}’={3} is invalid. Expected a value in the [{1} … {2}] range.
         */
        public static final int ValueOutOfRange_4 = 6;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources The path of the binary file containing resources, or {@code null} if
     *        there is no resources. The resources may be a file or an entry in a JAR file.
     */
    Errors(final URL resources) {
        super(resources);
    }

    /**
     * Returns the handle for the {@code Keys} constants.
     */
    @Override
    final KeyConstants getKeyConstants() {
        return Keys.INSTANCE;
    }

    /**
     * Returns resources in the given locale.
     *
     * @param  locale The locale, or {@code null} for the default locale.
     * @return Resources in the given locale.
     * @throws MissingResourceException if resources can't be found.
     */
    public static Errors getResources(final Locale locale) throws MissingResourceException {
        return getBundle(Errors.class, locale);
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final int key) throws MissingResourceException {
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
    public static String format(final int    key,
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
    public static String format(final int    key,
                                final Object arg0,
                                final Object arg1) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0, arg1);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute to "{0}".
     * @param  arg1 Value to substitute to "{1}".
     * @param  arg2 Value to substitute to "{2}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final int    key,
                                final Object arg0,
                                final Object arg1,
                                final Object arg2) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0, arg1, arg2);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute to "{0}".
     * @param  arg1 Value to substitute to "{1}".
     * @param  arg2 Value to substitute to "{2}".
     * @param  arg3 Value to substitute to "{3}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final int    key,
                                final Object arg0,
                                final Object arg1,
                                final Object arg2,
                                final Object arg3) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0, arg1, arg2, arg3);
    }

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = -5355796215044405012L;

        International(int key)                   {super(key);}
        International(int key, Object args)      {super(key, args);}
        @Override KeyConstants getKeyConstants() {return Keys.INSTANCE;}
        @Override IndexedResourceBundle getBundle(final Locale locale) {
            return getResources(locale);
        }
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * @param  key The key for the desired string.
     * @return An international string for the given key.
     */
    public static InternationalString formatInternational(final int key) {
        return new International(key);
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * {@note This method is redundant with the one expecting <code>Object...</code>, but avoid
     *        the creation of a temporary array. There is no risk of confusion since the two
     *        methods delegate their work to the same <code>format</code> method anyway.}
     *
     * @param  key The key for the desired string.
     * @param  arg Values to substitute to "{0}".
     * @return An international string for the given key.
     */
    public static InternationalString formatInternational(final int key, final Object arg) {
        return new International(key, arg);
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * @param  key  The key for the desired string.
     * @param  args Values to substitute to "{0}", "{1}", <i>etc</i>.
     * @return An international string for the given key.
     */
    public static InternationalString formatInternational(final int key, final Object... args) {
        return new International(key, args);
    }
}
