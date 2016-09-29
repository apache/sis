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
import java.util.Map;
import java.util.Locale;
import java.util.MissingResourceException;
import javax.annotation.Generated;
import org.opengis.util.InternationalString;


/**
 * Locale-dependent resources for error messages.
 *
 * <div class="section">Argument order convention</div>
 * This resource bundle applies the same convention than JUnit: for every {@code format(…)} method,
 * the first arguments provide information about the context in which the error occurred (e.g. the
 * name of a method argument or the range of valid values), while the erroneous values that caused
 * the error are last. Note that being the last programmatic parameter does not means that the value
 * will appears last in the formatted text, since every localized message can reorder the parameters
 * as they want.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.8
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
     * @since   0.3
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
         * Name “{2}” is ambiguous because it can be understood as either “{0}” or “{1}”.
         */
        public static final short AmbiguousName_3 = 0;

        /**
         * No element can be added to this set because properties ‘{0}’ and ‘{1}’ are mutually
         * exclusive.
         */
        public static final short CanNotAddToExclusiveSet_2 = 1;

        /**
         * Can not assign units “{1}” to dimension “{0}”.
         */
        public static final short CanNotAssignUnitToDimension_2 = 2;

        /**
         * Can not assign “{1}” to “{0}”.
         */
        public static final short CanNotAssign_2 = 3;

        /**
         * Can not compute “{0}”.
         */
        public static final short CanNotCompute_1 = 4;

        /**
         * Can not connect to “{0}”.
         */
        public static final short CanNotConnectTo_1 = 5;

        /**
         * Can not convert from type ‘{0}’ to type ‘{1}’.
         */
        public static final short CanNotConvertFromType_2 = 6;

        /**
         * Can not convert value “{0}” to type ‘{1}’.
         */
        public static final short CanNotConvertValue_2 = 7;

        /**
         * Can not create an object “{1}” as an instance of class ‘{0}’.
         */
        public static final short CanNotCreateObjectAsInstanceOf_2 = 8;

        /**
         * Can not instantiate “{0}”.
         */
        public static final short CanNotInstantiate_1 = 9;

        /**
         * Can not open “{0}”.
         */
        public static final short CanNotOpen_1 = 10;

        /**
         * Can not parse “{1}” as a file in the {0} format.
         */
        public static final short CanNotParseFile_2 = 11;

        /**
         * Can not read property “{1}” in file “{0}”.
         */
        public static final short CanNotReadPropertyInFile_2 = 12;

        /**
         * Can not read “{0}”.
         */
        public static final short CanNotRead_1 = 13;

        /**
         * Can not represent “{1}” in a strictly standard-compliant {0} format.
         */
        public static final short CanNotRepresentInFormat_2 = 14;

        /**
         * Can not set a value for parameter “{0}”.
         */
        public static final short CanNotSetParameterValue_1 = 15;

        /**
         * Can not set a value for property “{0}”.
         */
        public static final short CanNotSetPropertyValue_1 = 16;

        /**
         * Can not transform envelope.
         */
        public static final short CanNotTransformEnvelope = 17;

        /**
         * Circular reference.
         */
        public static final short CircularReference = 18;

        /**
         * Class ‘{0}’ is not final.
         */
        public static final short ClassNotFinal_1 = 19;

        /**
         * Can not clone an object of type ‘{0}’.
         */
        public static final short CloneNotSupported_1 = 20;

        /**
         * This {0} reader is closed.
         */
        public static final short ClosedReader_1 = 21;

        /**
         * Database error while creating a ‘{0}’ object for code “{1}”.
         */
        public static final short DatabaseError_2 = 22;

        /**
         * Thread “{0}” is dead.
         */
        public static final short DeadThread_1 = 23;

        /**
         * This instance of ‘{0}’ has been disposed.
         */
        public static final short DisposedInstanceOf_1 = 24;

        /**
         * Element “{0}” is duplicated.
         */
        public static final short DuplicatedElement_1 = 25;

        /**
         * Name or identifier “{0}” is used more than once.
         */
        public static final short DuplicatedIdentifier_1 = 26;

        /**
         * Option “{0}” is duplicated.
         */
        public static final short DuplicatedOption_1 = 27;

        /**
         * Name or alias for parameter “{0}” at index {1} conflict with name “{2}” at index {3}.
         */
        public static final short DuplicatedParameterName_4 = 28;

        /**
         * Element “{0}” is already present.
         */
        public static final short ElementAlreadyPresent_1 = 29;

        /**
         * Element “{0}” has not been found.
         */
        public static final short ElementNotFound_1 = 30;

        /**
         * Argument ‘{0}’ shall not be empty.
         */
        public static final short EmptyArgument_1 = 31;

        /**
         * The dictionary shall contain at least one entry.
         */
        public static final short EmptyDictionary = 32;

        /**
         * Envelope must be at least two-dimensional and non-empty.
         */
        public static final short EmptyEnvelope2D = 33;

        /**
         * Property named “{0}” shall not be empty.
         */
        public static final short EmptyProperty_1 = 34;

        /**
         * An error occurred in file “{0}” at Line {1}.
         */
        public static final short ErrorInFileAtLine_2 = 35;

        /**
         * Error in “{0}”: {1}
         */
        public static final short ErrorIn_2 = 36;

        /**
         * Argument ‘{0}’ shall not contain more than {1} elements. A number of {2} is excessive.
         */
        public static final short ExcessiveArgumentSize_3 = 37;

        /**
         * A size of {1} elements is excessive for the “{0}” list.
         */
        public static final short ExcessiveListSize_2 = 38;

        /**
         * For this algorithm, {0} is an excessive number of dimensions.
         */
        public static final short ExcessiveNumberOfDimensions_1 = 39;

        /**
         * No factory of kind ‘{0}’ found.
         */
        public static final short FactoryNotFound_1 = 40;

        /**
         * File “{0}” has not been found.
         */
        public static final short FileNotFound_1 = 41;

        /**
         * Attribute “{0}” is not allowed for an object of type ‘{1}’.
         */
        public static final short ForbiddenAttribute_2 = 42;

        /**
         * Property “{0}” is not allowed.
         */
        public static final short ForbiddenProperty_1 = 43;

        /**
         * Argument ‘{0}’ can not be an instance of ‘{1}’.
         */
        public static final short IllegalArgumentClass_2 = 44;

        /**
         * Argument ‘{0}’ can not be an instance of ‘{2}’. Expected an instance of ‘{1}’ or derived
         * type.
         */
        public static final short IllegalArgumentClass_3 = 45;

        /**
         * Argument ‘{0}’ can not take the “{1}” value, because the ‘{2}’ field can not take the “{3}”
         * value.
         */
        public static final short IllegalArgumentField_4 = 46;

        /**
         * Argument ‘{0}’ can not take the “{1}” value.
         */
        public static final short IllegalArgumentValue_2 = 47;

        /**
         * Illegal bits pattern: {0}.
         */
        public static final short IllegalBitsPattern_1 = 48;

        /**
         * Coordinate reference system can not be of type ‘{0}’.
         */
        public static final short IllegalCRSType_1 = 49;

        /**
         * The “{2}” character in “{1}” is not permitted by the “{0}” format.
         */
        public static final short IllegalCharacterForFormat_3 = 50;

        /**
         * The “{1}” character can not be used for “{0}”.
         */
        public static final short IllegalCharacter_2 = 51;

        /**
         * Class ‘{1}’ is illegal. It must be ‘{0}’ or a derived class.
         */
        public static final short IllegalClass_2 = 52;

        /**
         * Coordinate system can not be “{0}”.
         */
        public static final short IllegalCoordinateSystem_1 = 53;

        /**
         * The “{1}” pattern can not be applied to formating of objects of type ‘{0}’.
         */
        public static final short IllegalFormatPatternForClass_2 = 54;

        /**
         * “{1}” is not a valid identifier for the “{0}” code space.
         */
        public static final short IllegalIdentifierForCodespace_2 = 55;

        /**
         * The {0} reader does not accept inputs of type ‘{1}’.
         */
        public static final short IllegalInputTypeForReader_2 = 56;

        /**
         * The “{0}” language is not recognized.
         */
        public static final short IllegalLanguageCode_1 = 57;

        /**
         * Member “{0}” can not be associated to type “{1}”.
         */
        public static final short IllegalMemberType_2 = 58;

        /**
         * Option ‘{0}’ can not take the “{1}” value.
         */
        public static final short IllegalOptionValue_2 = 59;

        /**
         * The [{0} … {1}] range of ordinate values is not valid for the “{2}” axis.
         */
        public static final short IllegalOrdinateRange_3 = 60;

        /**
         * Parameter “{0}” can not be of type ‘{1}’.
         */
        public static final short IllegalParameterType_2 = 61;

        /**
         * Parameter “{0}” does not accept values of ‘{2}’ type. Expected an instance of ‘{1}’ or
         * derived type.
         */
        public static final short IllegalParameterValueClass_3 = 62;

        /**
         * Parameter “{0}” can not take the “{1}” value.
         */
        public static final short IllegalParameterValue_2 = 63;

        /**
         * Property “{0}” does not accept instances of ‘{1}’.
         */
        public static final short IllegalPropertyValueClass_2 = 64;

        /**
         * Expected an instance of ‘{1}’ for the “{0}” property, but got an instance of ‘{2}’.
         */
        public static final short IllegalPropertyValueClass_3 = 65;

        /**
         * Range [{0} … {1}] is not valid.
         */
        public static final short IllegalRange_2 = 66;

        /**
         * Value {1} for “{0}” is not a valid Unicode code point.
         */
        public static final short IllegalUnicodeCodePoint_2 = 67;

        /**
         * Can not use the “{1}” format with “{0}”.
         */
        public static final short IncompatibleFormat_2 = 68;

        /**
         * Property “{0}” has an incompatible value.
         */
        public static final short IncompatiblePropertyValue_1 = 69;

        /**
         * Unit “{0}” is incompatible with current value.
         */
        public static final short IncompatibleUnit_1 = 70;

        /**
         * Units “{0}” and “{1}” are incompatible.
         */
        public static final short IncompatibleUnits_2 = 71;

        /**
         * Value “{1}” of attribute ‘{0}’ is inconsistent with other attributes.
         */
        public static final short InconsistentAttribute_2 = 72;

        /**
         * Expected “{0}” namespace for “{1}”.
         */
        public static final short InconsistentNamespace_2 = 73;

        /**
         * Inconsistent table columns.
         */
        public static final short InconsistentTableColumns = 74;

        /**
         * Unit of measurement “{0}” is inconsistent with coordinate system axes.
         */
        public static final short InconsistentUnitsForCS_1 = 75;

        /**
         * Index {0} is out of bounds.
         */
        public static final short IndexOutOfBounds_1 = 76;

        /**
         * Indices ({0}, {1}) are out of bounds.
         */
        public static final short IndicesOutOfBounds_2 = 77;

        /**
         * Argument ‘{0}’ can not take an infinite value.
         */
        public static final short InfiniteArgumentValue_1 = 78;

        /**
         * Argument ‘{0}’ shall contain at least {1} elements. A number of {2} is insufficient.
         */
        public static final short InsufficientArgumentSize_3 = 79;

        /**
         * A different value is already associated to the “{0}” key.
         */
        public static final short KeyCollision_1 = 80;

        /**
         * Attribute “{0}” is mandatory for an object of type ‘{1}’.
         */
        public static final short MandatoryAttribute_2 = 81;

        /**
         * Mismatched array lengths.
         */
        public static final short MismatchedArrayLengths = 82;

        /**
         * The coordinate reference system must be the same for all objects.
         */
        public static final short MismatchedCRS = 83;

        /**
         * The “{0}” coordinate reference system has {1} dimension{1,choice,1#|2#s}, but the given
         * geometry is {2}-dimensional.
         */
        public static final short MismatchedDimensionForCRS_3 = 84;

        /**
         * Mismatched object dimensions: {0}D and {1}D.
         */
        public static final short MismatchedDimension_2 = 85;

        /**
         * Argument ‘{0}’ has {2} dimension{2,choice,1#|2#s}, while {1} was expected.
         */
        public static final short MismatchedDimension_3 = 86;

        /**
         * The grid geometry must be the same for “{0}” and “{1}”.
         */
        public static final short MismatchedGridGeometry_2 = 87;

        /**
         * Mismatched matrix sizes: expected {0}×{1} but got {2}×{3}.
         */
        public static final short MismatchedMatrixSize_4 = 88;

        /**
         * Mismatched descriptor for “{0}” parameter.
         */
        public static final short MismatchedParameterDescriptor_1 = 89;

        /**
         * Missing a ‘{1}’ character in “{0}” element.
         */
        public static final short MissingCharacterInElement_2 = 90;

        /**
         * Missing a “{1}” component in “{0}”.
         */
        public static final short MissingComponentInElement_2 = 91;

        /**
         * This operation requires the “{0}” module.
         */
        public static final short MissingRequiredModule_1 = 92;

        /**
         * Missing value for “{0}” option.
         */
        public static final short MissingValueForOption_1 = 93;

        /**
         * Missing value for “{0}” parameter.
         */
        public static final short MissingValueForParameter_1 = 94;

        /**
         * Missing value for “{0}” property.
         */
        public static final short MissingValueForProperty_1 = 95;

        /**
         * Missing value in the “{0}” column.
         */
        public static final short MissingValueInColumn_1 = 96;

        /**
         * Options “{0}” and “{1}” are mutually exclusive.
         */
        public static final short MutuallyExclusiveOptions_2 = 97;

        /**
         * Argument ‘{0}’ shall not be negative. The given value was {1}.
         */
        public static final short NegativeArgument_2 = 98;

        /**
         * Can not create a “{0}” array of negative length.
         */
        public static final short NegativeArrayLength_1 = 99;

        /**
         * No value is associated to “{0}”.
         */
        public static final short NoSuchValue_1 = 100;

        /**
         * Node “{0}” can not be a child of itself.
         */
        public static final short NodeChildOfItself_1 = 101;

        /**
         * Node “{0}” already has another parent.
         */
        public static final short NodeHasAnotherParent_1 = 102;

        /**
         * Node “{0}” has no parent.
         */
        public static final short NodeHasNoParent_1 = 103;

        /**
         * Node “{0}” is a leaf.
         */
        public static final short NodeIsLeaf_1 = 104;

        /**
         * “{0}” is not an angular unit.
         */
        public static final short NonAngularUnit_1 = 105;

        /**
         * Missing a ‘{1}’ parenthesis in “{0}”.
         */
        public static final short NonEquilibratedParenthesis_2 = 106;

        /**
         * Conversion is not invertible.
         */
        public static final short NonInvertibleConversion = 107;

        /**
         * “{0}” is not a linear unit.
         */
        public static final short NonLinearUnit_1 = 108;

        /**
         * “{0}” is not a scale unit.
         */
        public static final short NonScaleUnit_1 = 109;

        /**
         * “{0}” is not a time unit.
         */
        public static final short NonTemporalUnit_1 = 110;

        /**
         * No element for the “{0}” identifier, or the identifier is a forward reference.
         */
        public static final short NotABackwardReference_1 = 111;

        /**
         * “{0}” is not a key-value pair.
         */
        public static final short NotAKeyValuePair_1 = 112;

        /**
         * Argument ‘{0}’ shall not be NaN (Not-a-Number).
         */
        public static final short NotANumber_1 = 113;

        /**
         * Class ‘{0}’ is not a primitive type wrapper.
         */
        public static final short NotAPrimitiveWrapper_1 = 114;

        /**
         * Text “{0}” is not a Unicode identifier.
         */
        public static final short NotAUnicodeIdentifier_1 = 115;

        /**
         * Argument ‘{0}’ shall not be null.
         */
        public static final short NullArgument_1 = 116;

        /**
         * ‘{0}’ collection does not accept null elements.
         */
        public static final short NullCollectionElement_1 = 117;

        /**
         * Null key is not allowed in this dictionary.
         */
        public static final short NullMapKey = 118;

        /**
         * Null values are not allowed in this dictionary.
         */
        public static final short NullMapValue = 119;

        /**
         * Unexpected null value in record “{2}” for the column “{1}” in table “{0}”.
         */
        public static final short NullValueInTable_3 = 120;

        /**
         * Array length is {0}, while we expected an even length.
         */
        public static final short OddArrayLength_1 = 121;

        /**
         * Coordinate is outside the domain of validity.
         */
        public static final short OutsideDomainOfValidity = 122;

        /**
         * No parameter named “{1}” has been found in “{0}”.
         */
        public static final short ParameterNotFound_2 = 123;

        /**
         * No property named “{1}” has been found in “{0}”.
         */
        public static final short PropertyNotFound_2 = 124;

        /**
         * Record “{1}” is already defined in schema “{0}”.
         */
        public static final short RecordAlreadyDefined_2 = 125;

        /**
         * Recursive call while creating an object of type ‘{0}’ for code “{1}”.
         */
        public static final short RecursiveCreateCallForCode_2 = 126;

        /**
         * Recursive call while creating an object for the “{0}” key.
         */
        public static final short RecursiveCreateCallForKey_1 = 127;

        /**
         * A decimal separator is required.
         */
        public static final short RequireDecimalSeparator = 128;

        /**
         * Thread “{0}” seems stalled.
         */
        public static final short StalledThread_1 = 129;

        /**
         * Table “{0}” has not been found.
         */
        public static final short TableNotFound_1 = 130;

        /**
         * Expected at least {0} argument{0,choice,1#|2#s}, but got {1}.
         */
        public static final short TooFewArguments_2 = 131;

        /**
         * Too few occurrences of “{1}”. Expected at least {0} of them.
         */
        public static final short TooFewOccurrences_2 = 132;

        /**
         * Expected at most {0} argument{0,choice,1#|2#s}, but got {1}.
         */
        public static final short TooManyArguments_2 = 133;

        /**
         * Too many occurrences of “{1}”. The maximum is {0}.
         */
        public static final short TooManyOccurrences_2 = 134;

        /**
         * Tree depth exceeds the maximum.
         */
        public static final short TreeDepthExceedsMaximum = 135;

        /**
         * Ordering between “{0}” and “{1}” elements is undefined.
         */
        public static final short UndefinedOrderingForElements_2 = 136;

        /**
         * Expected an array of length {0}, but got {1}.
         */
        public static final short UnexpectedArrayLength_2 = 137;

        /**
         * Unexpected change in ‘{0}’.
         */
        public static final short UnexpectedChange_1 = 138;

        /**
         * The “{1}” characters after “{0}” was unexpected.
         */
        public static final short UnexpectedCharactersAfter_2 = 139;

        /**
         * Text for ‘{0}’ was expected to {1,choice,0#begin|1#end} with “{2}”, but found “{3}”.
         */
        public static final short UnexpectedCharactersAtBound_4 = 140;

        /**
         * Unexpected end of file while reading “{0}”.
         */
        public static final short UnexpectedEndOfFile_1 = 141;

        /**
         * More characters were expected at the end of “{0}”.
         */
        public static final short UnexpectedEndOfString_1 = 142;

        /**
         * File “{1}” seems to be encoded in an other format than {0}.
         */
        public static final short UnexpectedFileFormat_2 = 143;

        /**
         * Parameter “{0}” was not expected.
         */
        public static final short UnexpectedParameter_1 = 144;

        /**
         * Unexpected scale factor {1} for unit of measurement “{0}”.
         */
        public static final short UnexpectedScaleFactorForUnit_2 = 145;

        /**
         * Expected “{0}” to reference an instance of ‘{1}’, but found an instance of ‘{2}’.
         */
        public static final short UnexpectedTypeForReference_3 = 146;

        /**
         * Unexpected value “{1}” in “{0}” element.
         */
        public static final short UnexpectedValueInElement_2 = 147;

        /**
         * Parameter “{0}” has no unit.
         */
        public static final short UnitlessParameter_1 = 148;

        /**
         * Command “{0}” is not recognized.
         */
        public static final short UnknownCommand_1 = 149;

        /**
         * “{1}” is not a known or supported value for the ‘{0}’ enumeration.
         */
        public static final short UnknownEnumValue_2 = 150;

        /**
         * Keyword “{0}” is unknown.
         */
        public static final short UnknownKeyword_1 = 151;

        /**
         * Option “{0}” is not recognized.
         */
        public static final short UnknownOption_1 = 152;

        /**
         * Type ‘{0}’ is unknown in this context.
         */
        public static final short UnknownType_1 = 153;

        /**
         * Unit “{0}” is not recognized.
         */
        public static final short UnknownUnit_1 = 154;

        /**
         * The cell at column “{1}” of row “{0}” is unmodifiable.
         */
        public static final short UnmodifiableCellValue_2 = 155;

        /**
         * This metadata is not modifiable.
         */
        public static final short UnmodifiableMetadata = 156;

        /**
         * This instance of ‘{0}’ is not modifiable.
         */
        public static final short UnmodifiableObject_1 = 157;

        /**
         * Text “{1}” can not be parsed as an object of type ‘{0}’.
         */
        public static final short UnparsableStringForClass_2 = 158;

        /**
         * Text “{1}” can not be parsed as an object of type ‘{0}’, because of the “{2}” characters.
         */
        public static final short UnparsableStringForClass_3 = 159;

        /**
         * Can not parse “{1}” in element “{0}”.
         */
        public static final short UnparsableStringInElement_2 = 160;

        /**
         * Coordinate reference system has not been specified.
         */
        public static final short UnspecifiedCRS = 161;

        /**
         * No format is specified for objects of class ‘{0}’.
         */
        public static final short UnspecifiedFormatForClass_1 = 162;

        /**
         * Parameter values have not been specified.
         */
        public static final short UnspecifiedParameterValues = 163;

        /**
         * Version {1} of {0} format is not supported.
         */
        public static final short UnsupportedFormatVersion_2 = 164;

        /**
         * Can not handle this instance of ‘{0}’ because arbitrary implementations are not yet
         * supported.
         */
        public static final short UnsupportedImplementation_1 = 165;

        /**
         * The “{0}” interpolation is unsupported.
         */
        public static final short UnsupportedInterpolation_1 = 166;

        /**
         * The ‘{0}’ operation is unsupported.
         */
        public static final short UnsupportedOperation_1 = 167;

        /**
         * The ‘{0}’ type is unsupported.
         */
        public static final short UnsupportedType_1 = 168;

        /**
         * A value is already defined for “{0}”.
         */
        public static final short ValueAlreadyDefined_1 = 169;

        /**
         * Value ‘{0}’ = {1} is invalid. Expected a number greater than 0.
         */
        public static final short ValueNotGreaterThanZero_2 = 170;

        /**
         * Value ‘{0}’ = {3} is invalid. Expected a value in the [{1} … {2}] range.
         */
        public static final short ValueOutOfRange_4 = 171;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources  the path of the binary file containing resources, or {@code null} if
     *        there is no resources. The resources may be a file or an entry in a JAR file.
     */
    Errors(final URL resources) {
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
    public static Errors getResources(final Locale locale) throws MissingResourceException {
        return getBundle(Errors.class, locale);
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
    public static Errors getResources(final Map<?,?> properties) throws MissingResourceException {
        return getResources(getLocale(properties));
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key  the key for the desired string.
     * @return the string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short key) throws MissingResourceException {
        return getResources((Locale) null).getString(key);
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
        return getResources((Locale) null).getString(key, arg0);
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
        return getResources((Locale) null).getString(key, arg0, arg1);
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
        return getResources((Locale) null).getString(key, arg0, arg1, arg2);
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
        return getResources((Locale) null).getString(key, arg0, arg1, arg2, arg3);
    }

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = -5355796215044405012L;

        International(short key)                           {super(key);}
        International(short key, Object args)              {super(key, args);}
        @Override protected KeyConstants getKeyConstants() {return Keys.INSTANCE;}
        @Override protected IndexedResourceBundle getBundle(final Locale locale) {
            return getResources(locale);
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
