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

import java.io.InputStream;
import java.util.Map;
import java.util.Locale;
import java.util.MissingResourceException;
import org.opengis.util.InternationalString;


/**
 * Locale-dependent resources for error messages.
 *
 * <h2>Argument order convention</h2>
 * This resource bundle applies the same convention as JUnit: for every {@code format(…)} method,
 * the first arguments provide information about the context in which the error occurred (e.g. the
 * name of a method argument or the range of valid values), while the erroneous values that caused
 * the error are last. Note that being the last programmatic parameter does not means that the value
 * will appears last in the formatted text, since every localized message can reorder the parameters
 * as they want.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public class Errors extends IndexedResourceBundle {
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
         * ‘{0}’ is already initialized.
         */
        public static final short AlreadyInitialized_1 = 1;

        /**
         * Name “{2}” is ambiguous because it can be understood as either “{0}” or “{1}”.
         */
        public static final short AmbiguousName_3 = 2;

        /**
         * Computation in background failed.
         */
        public static final short BackgroundComputationFailed = 3;

        /**
         * This object can iterate only once.
         */
        public static final short CanIterateOnlyOnce = 4;

        /**
         * No element can be added to this set because properties ‘{0}’ and ‘{1}’ are mutually
         * exclusive.
         */
        public static final short CanNotAddToExclusiveSet_2 = 5;

        /**
         * Cannot assign units “{1}” to dimension “{0}”.
         */
        public static final short CanNotAssignUnitToDimension_2 = 6;

        /**
         * Cannot assign units “{1}” to variable “{0}”.
         */
        public static final short CanNotAssignUnitToVariable_2 = 7;

        /**
         * Cannot assign “{1}” to “{0}”.
         */
        public static final short CanNotAssign_2 = 8;

        /**
         * Cannot compute “{0}”.
         */
        public static final short CanNotCompute_1 = 9;

        /**
         * Cannot connect to “{0}”.
         */
        public static final short CanNotConnectTo_1 = 10;

        /**
         * Cannot convert from type ‘{0}’ to type ‘{1}’.
         */
        public static final short CanNotConvertFromType_2 = 11;

        /**
         * Cannot convert value “{0}” to type ‘{1}’.
         */
        public static final short CanNotConvertValue_2 = 12;

        /**
         * Cannot copy “{0}”.
         */
        public static final short CanNotCopy_1 = 13;

        /**
         * Cannot open “{0}”.
         */
        public static final short CanNotOpen_1 = 14;

        /**
         * Cannot parse the Coordinate Reference System of “{0}”.
         */
        public static final short CanNotParseCRS_1 = 155;

        /**
         * Cannot parse “{0}”.
         */
        public static final short CanNotParse_1 = 15;

        /**
         * Cannot process property “{1}” located at path “{0}”. The reason is: {2}
         */
        public static final short CanNotProcessPropertyAtPath_3 = 16;

        /**
         * Cannot process property “{0}”. The reason is: {1}
         */
        public static final short CanNotProcessProperty_2 = 17;

        /**
         * Cannot read property “{1}” in file “{0}”.
         */
        public static final short CanNotReadPropertyInFile_2 = 18;

        /**
         * Cannot read “{0}”.
         */
        public static final short CanNotRead_1 = 19;

        /**
         * Cannot read “{0}” at line {1}, column {2}.
         */
        public static final short CanNotRead_3 = 20;

        /**
         * Cannot represent “{1}” in a strictly standard-compliant {0} format.
         */
        public static final short CanNotRepresentInFormat_2 = 21;

        /**
         * Cannot resolve “{0}” as an absolute path.
         */
        public static final short CanNotResolveAsAbsolutePath_1 = 22;

        /**
         * Cannot set a value for parameter “{0}”.
         */
        public static final short CanNotSetParameterValue_1 = 23;

        /**
         * Cannot set a value for property “{0}”.
         */
        public static final short CanNotSetPropertyValue_1 = 24;

        /**
         * Cannot store the {0} value in this vector.
         */
        public static final short CanNotStoreInVector_1 = 25;

        /**
         * Cannot transform envelope.
         */
        public static final short CanNotTransformEnvelope = 26;

        /**
         * Cannot write “{1}” as a file in the {0} format.
         */
        public static final short CanNotWriteFile_2 = 27;

        /**
         * Cannot compare instance of ‘{0}’ with ‘{1}’.
         */
        public static final short CannotCompareInstanceOf_2 = 205;

        /**
         * Circular reference.
         */
        public static final short CircularReference = 28;

        /**
         * Class ‘{0}’ is not final.
         */
        public static final short ClassNotFinal_1 = 29;

        /**
         * Cannot clone an object of type ‘{0}’.
         */
        public static final short CloneNotSupported_1 = 30;

        /**
         * Connection is closed.
         */
        public static final short ConnectionClosed = 31;

        /**
         * Cross references are not supported.
         */
        public static final short CrossReferencesNotSupported = 32;

        /**
         * Database error while creating a ‘{0}’ object for the “{1}” identifier.
         */
        public static final short DatabaseError_2 = 33;

        /**
         * Failed to {0,choice,0#insert|1#update} record “{2}” in database table “{1}”.
         */
        public static final short DatabaseUpdateFailure_3 = 34;

        /**
         * Thread “{0}” is dead.
         */
        public static final short DeadThread_1 = 35;

        /**
         * This instance of ‘{0}’ has been disposed.
         */
        public static final short DisposedInstanceOf_1 = 36;

        /**
         * Element “{0}” is duplicated.
         */
        public static final short DuplicatedElement_1 = 37;

        /**
         * File “{0}” is referenced more than once.
         */
        public static final short DuplicatedFileReference_1 = 38;

        /**
         * Name or identifier “{0}” is used more than once.
         */
        public static final short DuplicatedIdentifier_1 = 39;

        /**
         * Value {0,number} is used more than once.
         */
        public static final short DuplicatedNumber_1 = 40;

        /**
         * Option “{0}” is duplicated.
         */
        public static final short DuplicatedOption_1 = 41;

        /**
         * Element “{0}” is already present.
         */
        public static final short ElementAlreadyPresent_1 = 42;

        /**
         * Element “{0}” has not been found.
         */
        public static final short ElementNotFound_1 = 43;

        /**
         * Argument ‘{0}’ shall not be empty.
         */
        public static final short EmptyArgument_1 = 44;

        /**
         * The dictionary shall contain at least one entry.
         */
        public static final short EmptyDictionary = 45;

        /**
         * Envelope must be at least two-dimensional and non-empty.
         */
        public static final short EmptyEnvelope2D = 46;

        /**
         * Property named “{0}” shall not be empty.
         */
        public static final short EmptyProperty_1 = 47;

        /**
         * An error occurred in file “{0}” at line {1}.
         */
        public static final short ErrorInFileAtLine_2 = 48;

        /**
         * A size of {1} elements is excessive for the “{0}” list.
         */
        public static final short ExcessiveListSize_2 = 49;

        /**
         * For this algorithm, {0} is an excessive number of dimensions.
         */
        public static final short ExcessiveNumberOfDimensions_1 = 50;

        /**
         * No factory of kind ‘{0}’ found.
         */
        public static final short FactoryNotFound_1 = 51;

        /**
         * File “{0}” has not been found.
         */
        public static final short FileNotFound_1 = 52;

        /**
         * Attribute “{0}” is not allowed for an object of type ‘{1}’.
         */
        public static final short ForbiddenAttribute_2 = 53;

        /**
         * Property “{0}” is not allowed.
         */
        public static final short ForbiddenProperty_1 = 54;

        /**
         * “{0}” uses two or more different units of measurement.
         */
        public static final short HeterogynousUnitsIn_1 = 55;

        /**
         * Identifier “{1}” is not in “{0}” namespace.
         */
        public static final short IdentifierNotInNamespace_2 = 56;

        /**
         * Argument ‘{0}’ cannot be an instance of ‘{1}’.
         */
        public static final short IllegalArgumentClass_2 = 57;

        /**
         * Argument ‘{0}’ cannot be an instance of ‘{2}’. Expected an instance of ‘{1}’ or derived
         * type.
         */
        public static final short IllegalArgumentClass_3 = 58;

        /**
         * Argument ‘{0}’ cannot take the “{1}” value.
         */
        public static final short IllegalArgumentValue_2 = 59;

        /**
         * Illegal bits pattern: {0}.
         */
        public static final short IllegalBitsPattern_1 = 60;

        /**
         * Coordinate reference system cannot be of type ‘{0}’.
         */
        public static final short IllegalCRSType_1 = 61;

        /**
         * The “{2}” character in “{1}” is not permitted by the “{0}” format.
         */
        public static final short IllegalCharacterForFormat_3 = 62;

        /**
         * The “{1}” character cannot be used for “{0}”.
         */
        public static final short IllegalCharacter_2 = 63;

        /**
         * Class ‘{1}’ is illegal. It must be ‘{0}’ or a derived class.
         */
        public static final short IllegalClass_2 = 64;

        /**
         * The [{0} … {1}] range of coordinate values is not valid for the “{2}” axis.
         */
        public static final short IllegalCoordinateRange_3 = 65;

        /**
         * Coordinate system cannot be “{0}”.
         */
        public static final short IllegalCoordinateSystem_1 = 66;

        /**
         * The “{1}” pattern cannot be applied to formatting of objects of type ‘{0}’.
         */
        public static final short IllegalFormatPatternForClass_2 = 67;

        /**
         * “{1}” is not a valid identifier for the “{0}” code space.
         */
        public static final short IllegalIdentifierForCodespace_2 = 68;

        /**
         * The “{0}” language is not recognized.
         */
        public static final short IllegalLanguageCode_1 = 69;

        /**
         * Illegal mapping: {0} → {1}.
         */
        public static final short IllegalMapping_2 = 70;

        /**
         * Member “{0}” cannot be associated to type “{1}”.
         */
        public static final short IllegalMemberType_2 = 71;

        /**
         * Option ‘{0}’ cannot take the “{1}” value.
         */
        public static final short IllegalOptionValue_2 = 72;

        /**
         * Property “{0}” does not accept instances of ‘{1}’.
         */
        public static final short IllegalPropertyValueClass_2 = 73;

        /**
         * Expected an instance of ‘{1}’ for the “{0}” property, but got an instance of ‘{2}’.
         */
        public static final short IllegalPropertyValueClass_3 = 74;

        /**
         * Property “{0}” cannot take the “{1}” value.
         */
        public static final short IllegalPropertyValue_2 = 75;

        /**
         * Range [{0} … {1}] is not valid.
         */
        public static final short IllegalRange_2 = 76;

        /**
         * Sexagesimal angle {0,number} is illegal because the {1,choice,0#minutes|1#seconds} field
         * cannot take the {2,number} value.
         */
        public static final short IllegalSexagesimalField_3 = 77;

        /**
         * Value {1} for “{0}” is not a valid Unicode code point.
         */
        public static final short IllegalUnicodeCodePoint_2 = 78;

        /**
         * Cannot use the {1} format with “{0}”.
         */
        public static final short IncompatibleFormat_2 = 80;

        /**
         * Property “{0}” has an incompatible value.
         */
        public static final short IncompatiblePropertyValue_1 = 81;

        /**
         * The “{0}” unit of measurement has dimension of ‘{1}’ ({2}). It is incompatible with
         * dimension of ‘{3}’ ({4}).
         */
        public static final short IncompatibleUnitDimension_5 = 82;

        /**
         * Unit “{0}” is incompatible with current value.
         */
        public static final short IncompatibleUnit_1 = 83;

        /**
         * Units “{0}” and “{1}” are incompatible.
         */
        public static final short IncompatibleUnits_2 = 84;

        /**
         * Value “{1}” of attribute ‘{0}’ is inconsistent with other attributes.
         */
        public static final short InconsistentAttribute_2 = 85;

        /**
         * Inconsistent table columns.
         */
        public static final short InconsistentTableColumns = 86;

        /**
         * Unit of measurement “{0}” is inconsistent with coordinate system axes.
         */
        public static final short InconsistentUnitsForCS_1 = 87;

        /**
         * The position is indeterminate.
         */
        public static final short IndeterminatePosition = 206;

        /**
         * Index {0} is out of bounds.
         */
        public static final short IndexOutOfBounds_1 = 88;

        /**
         * Indices ({0}, {1}) are out of bounds.
         */
        public static final short IndicesOutOfBounds_2 = 89;

        /**
         * Argument ‘{0}’ cannot take an infinite value.
         */
        public static final short InfiniteArgumentValue_1 = 90;

        /**
         * Integer overflow during {0} bits arithmetic operation.
         */
        public static final short IntegerOverflow_1 = 91;

        /**
         * Interrupted while waiting result.
         */
        public static final short InterruptedWhileWaitingResult = 92;

        /**
         * “{0}” is an invalid version identifier.
         */
        public static final short InvalidVersionIdentifier_1 = 93;

        /**
         * Key “{0}” is associated twice to different values.
         */
        public static final short KeyCollision_1 = 94;

        /**
         * Attribute “{0}” is mandatory for an object of type ‘{1}’.
         */
        public static final short MandatoryAttribute_2 = 95;

        /**
         * Mismatched array lengths.
         */
        public static final short MismatchedArrayLengths = 96;

        /**
         * Mismatched axes “{1}” and “{2}” at dimension {0}.
         */
        public static final short MismatchedAxes_3 = 97;

        /**
         * The coordinate reference system must be the same for all objects.
         */
        public static final short MismatchedCRS = 98;

        /**
         * The “{0}” coordinate reference system has {1} dimension{1,choice,1#|2#s}, but the given
         * geometry is {2}-dimensional.
         */
        public static final short MismatchedDimensionForCRS_3 = 99;

        /**
         * Mismatched object dimensions: {0}D and {1}D.
         */
        public static final short MismatchedDimension_2 = 100;

        /**
         * Argument ‘{0}’ has {2} dimension{2,choice,1#|2#s}, while {1} was expected.
         */
        public static final short MismatchedDimension_3 = 101;

        /**
         * The grid geometry must be the same for “{0}” and “{1}”.
         */
        public static final short MismatchedGridGeometry_2 = 102;

        /**
         * Mismatched matrix sizes: expected {0}×{1} but got {2}×{3}.
         */
        public static final short MismatchedMatrixSize_4 = 103;

        /**
         * The “{0}” transform has {3} {1,choice,0#source|1#target} dimension{3,choice,1#|2#s}, while
         * {2} was expected.
         */
        public static final short MismatchedTransformDimension_4 = 104;

        /**
         * Missing a ‘{1}’ character in “{0}” element.
         */
        public static final short MissingCharacterInElement_2 = 105;

        /**
         * Missing a “{1}” component in “{0}”.
         */
        public static final short MissingComponentInElement_2 = 106;

        /**
         * JAXB context has not been specified.
         */
        public static final short MissingJAXBContext = 107;

        /**
         * Missing or empty ‘{1}’ attribute in “{0}”.
         */
        public static final short MissingOrEmptyAttribute_2 = 108;

        /**
         * This operation requires the “{0}” module.
         */
        public static final short MissingRequiredModule_1 = 109;

        /**
         * Missing value for the “{0}” option.
         */
        public static final short MissingValueForOption_1 = 110;

        /**
         * Missing value for the “{0}” property.
         */
        public static final short MissingValueForProperty_1 = 111;

        /**
         * Missing value for the “{1}” property in “{0}”.
         */
        public static final short MissingValueForProperty_2 = 112;

        /**
         * Missing value in the “{0}” column.
         */
        public static final short MissingValueInColumn_1 = 113;

        /**
         * Cannot return a single value for “{0}” because there is at least two occurrences, at indices
         * {1} and {2}.
         */
        public static final short MultiOccurenceValueAtIndices_3 = 114;

        /**
         * Options “{0}” and “{1}” are mutually exclusive.
         */
        public static final short MutuallyExclusiveOptions_2 = 115;

        /**
         * Native interfaces “{1}” not available for the {0} platform.
         */
        public static final short NativeInterfacesNotFound_2 = 116;

        /**
         * Argument ‘{0}’ shall not be negative. The given value was {1}.
         */
        public static final short NegativeArgument_2 = 117;

        /**
         * Cannot create a “{0}” array of negative length.
         */
        public static final short NegativeArrayLength_1 = 118;

        /**
         * Nested “{0}” elements are not allowed.
         */
        public static final short NestedElementNotAllowed_1 = 119;

        /**
         * The object is nil for the following reason: {0}.
         */
        public static final short NilObject_1 = 120;

        /**
         * No value is associated to “{0}”.
         */
        public static final short NoSuchValue_1 = 121;

        /**
         * Node “{0}” cannot be a child of itself.
         */
        public static final short NodeChildOfItself_1 = 122;

        /**
         * Node “{0}” already has another parent.
         */
        public static final short NodeHasAnotherParent_1 = 123;

        /**
         * Node “{0}” has no parent.
         */
        public static final short NodeHasNoParent_1 = 124;

        /**
         * Node “{0}” is a leaf.
         */
        public static final short NodeIsLeaf_1 = 125;

        /**
         * “{0}” is not an angular unit.
         */
        public static final short NonAngularUnit_1 = 126;

        /**
         * Missing a ‘{1}’ parenthesis in “{0}”.
         */
        public static final short NonEquilibratedParenthesis_2 = 127;

        /**
         * No horizontal component found in the “{0}” coordinate reference system.
         */
        public static final short NonHorizontalCRS_1 = 128;

        /**
         * Conversion is not invertible.
         */
        public static final short NonInvertibleConversion = 129;

        /**
         * “{0}” is not a linear unit.
         */
        public static final short NonLinearUnit_1 = 130;

        /**
         * The scale of measurement for “{0}” unit is not a ratio scale.
         */
        public static final short NonRatioUnit_1 = 131;

        /**
         * “{0}” is not a scale unit.
         */
        public static final short NonScaleUnit_1 = 132;

        /**
         * “{0}” is not a time unit.
         */
        public static final short NonTemporalUnit_1 = 133;

        /**
         * Expected the “{0}” value for all members, but found a member with the “{1}” value.
         */
        public static final short NonUniformValue_2 = 79;

        /**
         * No element for the “{0}” identifier, or the identifier is a forward reference.
         */
        public static final short NotABackwardReference_1 = 134;

        /**
         * Value of ‘{0}’ shall be a {1,choice,0#divisor|1#multiple} of {2} but the given value is {3}.
         */
        public static final short NotADivisorOrMultiple_4 = 135;

        /**
         * “{0}” is not a key-value pair.
         */
        public static final short NotAKeyValuePair_1 = 136;

        /**
         * Argument ‘{0}’ shall not be NaN (Not-a-Number).
         */
        public static final short NotANumber_1 = 137;

        /**
         * Class ‘{0}’ is not a primitive type wrapper.
         */
        public static final short NotAPrimitiveWrapper_1 = 138;

        /**
         * Text “{0}” is not a Unicode identifier.
         */
        public static final short NotAUnicodeIdentifier_1 = 139;

        /**
         * {0} is not an integer value.
         */
        public static final short NotAnInteger_1 = 140;

        /**
         * Argument ‘{0}’ shall not be null.
         */
        public static final short NullArgument_1 = 141;

        /**
         * ‘{0}’ collection does not accept null elements.
         */
        public static final short NullCollectionElement_1 = 142;

        /**
         * Null key is not allowed in this dictionary.
         */
        public static final short NullMapKey = 143;

        /**
         * Null values are not allowed in this dictionary.
         */
        public static final short NullMapValue = 144;

        /**
         * Unexpected null value in record “{2}” for the column “{1}” in table “{0}”.
         */
        public static final short NullValueInTable_3 = 145;

        /**
         * Array length is {0}, while we expected an even length.
         */
        public static final short OddArrayLength_1 = 146;

        /**
         * “{1}” is opened in {0,choice,0#read|1#write}-only mode.
         */
        public static final short OpenedReadOrWriteOnly_2 = 147;

        /**
         * Coordinate is outside the domain of validity.
         */
        public static final short OutsideDomainOfValidity = 148;

        /**
         * No property named “{1}” has been found in “{0}”.
         */
        public static final short PropertyNotFound_2 = 149;

        /**
         * Record “{1}” is already defined in schema “{0}”.
         */
        public static final short RecordAlreadyDefined_2 = 150;

        /**
         * No record found in “{0}” table for “{1}” key.
         */
        public static final short RecordNotFound_2 = 151;

        /**
         * Recursive call while creating an object for the “{0}” key.
         */
        public static final short RecursiveCreateCallForKey_1 = 152;

        /**
         * A decimal separator is required.
         */
        public static final short RequireDecimalSeparator = 153;

        /**
         * Thread “{0}” seems stalled.
         */
        public static final short StalledThread_1 = 154;

        /**
         * Expected at least {0,number} argument{0,choice,1#|2#s}, but got {1,number}.
         */
        public static final short TooFewArguments_2 = 156;

        /**
         * Collection “{0}” contains only {2,number} element{2,choice,1#|2#s} while at least {1,number}
         * elements were expected.
         */
        public static final short TooFewCollectionElements_3 = 157;

        /**
         * Too few occurrences of “{1}”. Expected at least {0,number} of them.
         */
        public static final short TooFewOccurrences_2 = 158;

        /**
         * Expected at most {0,number} argument{0,choice,1#|2#s}, but got {1,number}.
         */
        public static final short TooManyArguments_2 = 159;

        /**
         * Collection “{0}” contains {2,number} elements while at most {1,number} element{1,choice,1#
         * was|2#s were} expected.
         */
        public static final short TooManyCollectionElements_3 = 160;

        /**
         * Too many occurrences of “{1}”. The maximum is {0,number}.
         */
        public static final short TooManyOccurrences_2 = 161;

        /**
         * Tree depth exceeds the maximum.
         */
        public static final short TreeDepthExceedsMaximum = 162;

        /**
         * Ordering between “{0}” and “{1}” elements is undefined.
         */
        public static final short UndefinedOrderingForElements_2 = 163;

        /**
         * Expected an array of length {0,number}, but got {1,number}.
         */
        public static final short UnexpectedArrayLength_2 = 164;

        /**
         * Unexpected change in ‘{0}’.
         */
        public static final short UnexpectedChange_1 = 165;

        /**
         * The “{1}” characters after “{0}” were unexpected.
         */
        public static final short UnexpectedCharactersAfter_2 = 166;

        /**
         * Text for ‘{0}’ was expected to {1,choice,0#begin|1#end} with “{2}”, but found “{3}”.
         */
        public static final short UnexpectedCharactersAtBound_4 = 167;

        /**
         * Unexpected end of file while reading “{0}”.
         */
        public static final short UnexpectedEndOfFile_1 = 168;

        /**
         * More characters were expected at the end of “{0}”.
         */
        public static final short UnexpectedEndOfString_1 = 169;

        /**
         * File “{1}” seems to be encoded in another format than {0}.
         */
        public static final short UnexpectedFileFormat_2 = 170;

        /**
         * The “{1}” name is not valid in this context, because the “{0}” namespace was expected.
         */
        public static final short UnexpectedNamespace_2 = 171;

        /**
         * Parameter “{0}” was not expected.
         */
        public static final short UnexpectedParameter_1 = 172;

        /**
         * Property “{1}” was not expected in “{0}”.
         */
        public static final short UnexpectedProperty_2 = 173;

        /**
         * Unexpected scale factor {1,number,#,##0.######} for unit of measurement “{0}”.
         */
        public static final short UnexpectedScaleFactorForUnit_2 = 174;

        /**
         * Expected “{0}” to reference an instance of ‘{1}’, but found an instance of ‘{2}’.
         */
        public static final short UnexpectedTypeForReference_3 = 175;

        /**
         * Unexpected value “{1}” in the “{0}” element.
         */
        public static final short UnexpectedValueInElement_2 = 176;

        /**
         * ‘{0}’ has not been initialized.
         */
        public static final short Uninitialized_1 = 177;

        /**
         * Command “{0}” is not recognized.
         */
        public static final short UnknownCommand_1 = 178;

        /**
         * “{1}” is not a known or supported value for the ‘{0}’ enumeration.
         */
        public static final short UnknownEnumValue_2 = 179;

        /**
         * Keyword “{0}” is unknown.
         */
        public static final short UnknownKeyword_1 = 180;

        /**
         * Option “{0}” is not recognized.
         */
        public static final short UnknownOption_1 = 181;

        /**
         * Type ‘{0}’ is unknown in this context.
         */
        public static final short UnknownType_1 = 182;

        /**
         * Unit “{0}” is not recognized.
         */
        public static final short UnknownUnit_1 = 183;

        /**
         * The cell at column “{1}” of row “{0}” is unmodifiable.
         */
        public static final short UnmodifiableCellValue_2 = 184;

        /**
         * This instance of ‘{0}’ is not modifiable.
         */
        public static final short UnmodifiableObject_1 = 185;

        /**
         * Text “{1}” cannot be parsed as an object of type ‘{0}’.
         */
        public static final short UnparsableStringForClass_2 = 186;

        /**
         * Text “{1}” cannot be parsed as an object of type ‘{0}’, because of the “{2}” characters.
         */
        public static final short UnparsableStringForClass_3 = 187;

        /**
         * Cannot parse “{1}” in element “{0}”.
         */
        public static final short UnparsableStringInElement_2 = 188;

        /**
         * Coordinate reference system has not been specified.
         */
        public static final short UnspecifiedCRS = 189;

        /**
         * No format is specified for objects of class ‘{0}’.
         */
        public static final short UnspecifiedFormatForClass_1 = 190;

        /**
         * The “{0}” argument value is unsupported.
         */
        public static final short UnsupportedArgumentValue_1 = 191;

        /**
         * Axes with “{0}” direction are not supported by this operation.
         */
        public static final short UnsupportedAxisDirection_1 = 192;

        /**
         * The “{0}” coordinate system is not supported by this operation.
         */
        public static final short UnsupportedCoordinateSystem_1 = 193;

        /**
         * The “{0}” datum is not supported by this operation.
         */
        public static final short UnsupportedDatum_1 = 194;

        /**
         * Version {1} of {0} format is not supported.
         */
        public static final short UnsupportedFormatVersion_2 = 195;

        /**
         * Format “{0}” is unsupported.
         */
        public static final short UnsupportedFormat_1 = 196;

        /**
         * Cannot handle this instance of ‘{0}’ because arbitrary implementations are not yet
         * supported.
         */
        public static final short UnsupportedImplementation_1 = 197;

        /**
         * The “{0}” interpolation is unsupported.
         */
        public static final short UnsupportedInterpolation_1 = 198;

        /**
         * The ‘{0}’ operation is unsupported.
         */
        public static final short UnsupportedOperation_1 = 199;

        /**
         * The ‘{0}’ type is not supported in this context.
         */
        public static final short UnsupportedType_1 = 200;

        /**
         * XPath “{0}” is not recognized. The current implementation supports only simple paths.
         */
        public static final short UnsupportedXPath_1 = 201;

        /**
         * A value is already defined for “{0}”.
         */
        public static final short ValueAlreadyDefined_1 = 202;

        /**
         * Value ‘{0}’ = {1,number} is invalid. Expected a number greater than 0.
         */
        public static final short ValueNotGreaterThanZero_2 = 203;

        /**
         * Value ‘{0}’ = {3} is invalid. Expected a value in the [{1} … {2}] range.
         */
        public static final short ValueOutOfRange_4 = 204;
    }

    /**
     * Constructs a new resource bundle loading data from
     * the resource file of the same name as this class.
     */
    public Errors() {
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
    public static Errors forLocale(final Locale locale) {
        /*
         * We cannot factorize this method into the parent class, because we need to call
         * `ResourceBundle.getBundle(String)` from the module that provides the resources.
         * We do not cache the result because `ResourceBundle` already provides a cache.
         */
        return (Errors) getBundle(Errors.class.getName(), nonNull(locale));
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
    public static Errors forProperties(final Map<?,?> properties) throws MissingResourceException {
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
        private static final long serialVersionUID = -5355796215044405012L;

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
