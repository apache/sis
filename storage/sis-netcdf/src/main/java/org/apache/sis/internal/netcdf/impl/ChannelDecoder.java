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
package org.apache.sis.internal.netcdf.impl;

import java.util.Set;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.lang.reflect.Array;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.ConversionException;
import org.opengis.parameter.InvalidParameterCardinalityException;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.GridGeometry;
import org.apache.sis.internal.storage.ChannelDataInput;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Debug;
import org.apache.sis.measure.Units;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;
import org.apache.sis.internal.jdk8.Function;


/**
 * Provides NetCDF decoding services as a standalone library.
 * The javadoc in this class uses the "file" word for the source of data, but
 * this implementation actually works with arbitrary {@link ReadableByteChannel}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see <a href="http://portal.opengeospatial.org/files/?artifact_id=43734">NetCDF Classic and 64-bit Offset Format (1.0)</a>
 */
public final class ChannelDecoder extends Decoder {
    /**
     * The NetCDF magic number expected in the first integer of the stream.
     * The comparison shall ignore the 8 lowest bits, as in the following example:
     *
     * {@preformat java
     *     int header = ...; // The first integer in the stream.
     *     boolean isNetCDF = (header & 0xFFFFFF00) == MAGIC_NUMBER;
     * }
     */
    public static final int MAGIC_NUMBER = ('C' << 24) | ('D' << 16) | ('F' <<  8);

    /**
     * The maximal version number supported by this implementation.
     */
    public static final int MAX_VERSION = 2;

    /**
     * The encoding of dimension, variable and attribute names. This is fixed to {@value} by the
     * NetCDF specification. Note however that the encoding of attribute values may be different.
     *
     * @see #encoding
     * @see #readName()
     */
    private static final String NAME_ENCODING = "UTF-8";

    /**
     * The locale of dimension, variable and attribute names. This is used for the conversion to
     * lower-cases before case-insensitive searches.
     *
     * @see #findAttribute(String)
     */
    private static final Locale NAME_LOCALE = Locale.US;

    /**
     * The pattern to use for separating the component of a time unit.
     * An example of time unit is <cite>"days since 1970-01-01T00:00:00Z"</cite>.
     *
     * @see #numberToDate(String, Number[])
     */
    private static final Pattern TIME_UNIT_PATTERN = Pattern.compile("(?i)\\bsince\\b");

    /*
     * NOTE: the names of the static constants below this point match the names used in the Backus-Naur Form (BNF)
     *       definitions in the NetCDF Classic and 64-bit Offset Format (1.0) specification (link in class javdoc),
     *       with NC_ prefix omitted. The types of those constants match the expected type in the file.
     */

    /**
     * A {@link #numrecs} value indicating indeterminate record count.
     * This value allows streaming data.
     *
     * @see #numrecs
     */
    private static final int STREAMING = -1;

    /**
     * Tag for lists of dimensions, variables or attributes, as 32 bits value to be followed
     * by a 32 bits integer given the number of elements in the list ({@code nelems}).
     * A 64-bits zero means that there is no list (identified as {@code ABSENT} in the BNF).
     *
     * @see #tagName(int)
     */
    private static final int DIMENSION = 0x0A, VARIABLE = 0x0B, ATTRIBUTE = 0x0C;

    /**
     * The {@link ReadableByteChannel} together with a {@link ByteBuffer} for reading the data.
     */
    private final ChannelDataInput input;

    /**
     * {@code false} if the file is the classic format, or
     * {@code true} if it is the 64-bits offset format.
     */
    private final boolean is64bits;

    /**
     * Number of records as an unsigned integer, or {@value #STREAMING} if undetermined.
     */
    private final int numrecs;

    /**
     * The character encoding of attribute values. This encoding does <strong>not</strong> apply to
     * dimension, variable and attribute names, which are fixed to UTF-8 as of NetCDF specification.
     *
     * The specification said: "Although the characters used in netCDF names must be encoded as UTF-8,
     * character data may use other encodings. The variable attribute “_Encoding” is reserved for this
     * purpose in future implementations."
     *
     * @todo Fixed to ISO-LATIN-1 for now, needs to be determined in a better way.
     *
     * @see #NAME_ENCODING
     * @see #readString(String)
     */
    private final String encoding = "ISO-8859-1";

    /**
     * The variables found in the NetCDF file.
     *
     * @see #getVariables()
     */
    private final VariableInfo[] variables;

    /**
     * The attributes found in the NetCDF file.
     *
     * @see #findAttribute(String)
     */
    private final Map<String,Attribute> attributeMap;

    /**
     * The grid geometries, created when first needed.
     *
     * @see #getGridGeometries()
     */
    private transient GridGeometry[] gridGeometries;

    /**
     * Creates a new decoder for the given file.
     * This constructor parses immediately the header.
     *
     * @param  listeners Where to send the warnings.
     * @param  input     The channel and the buffer from where data are read.
     * @throws IOException If an error occurred while reading the channel.
     * @throws DataStoreException If the content of the given channel is not a NetCDF file.
     */
    public ChannelDecoder(final WarningListeners<?> listeners, final ChannelDataInput input)
            throws IOException, DataStoreException
    {
        super(listeners);
        this.input = input;
        /*
         * Check the magic number, which is expected to be exactly 3 bytes forming the "CDF" string.
         * The 4th byte is the version number, which we opportunistically use after the magic number check.
         */
        int version = input.readInt();
        if ((version & 0xFFFFFF00) != MAGIC_NUMBER) {
            throw new DataStoreException(errors().getString(Errors.Keys.UnexpectedFileFormat_2, "NetCDF", input.filename));
        }
        /*
         * Check the version number.
         */
        version &= 0xFF;
        switch (version) {
            case 1:  is64bits = false; break;
            case 2:  is64bits = true;  break;
            default: throw new DataStoreException(errors().getString(Errors.Keys.UnsupportedVersion_1, version));
            // If more cases are added, remember to increment the MAX_VERSION constant.
        }
        numrecs = input.readInt();
        /*
         * Read the dimension, attribute and variable declarations. We expect exactly 3 lists,
         * where any of them can be flagged as absent by a long (64 bits) 0.
         */
        Dimension[]    dimensions = null;
        VariableInfo[] variables  = null;
        Attribute[]    attributes = null;
        for (int i=0; i<3; i++) {
            final long tn = input.readLong(); // Combination of tag and nelems
            if (tn != 0) {
                final int tag = (int) (tn >>> Integer.SIZE);
                final int nelems = (int) tn;
                ensureNonNegative(nelems, tag);
                switch (tag) {
                    case DIMENSION: dimensions = readDimensions(nelems); break;
                    case VARIABLE:  variables  = readVariables (nelems, dimensions); break;
                    case ATTRIBUTE: attributes = readAttributes(nelems); break;
                    default:        throw malformedHeader();
                }
            }
        }
        attributeMap = toMap(attributes, Attribute.NAME_FUNCTION);
        this.variables = variables;
    }

    /**
     * Return the localized name of the given tag.
     *
     * @param  tag One of {@link #DIMENSION}, {@link #VARIABLE} or {@link #ATTRIBUTE} constants.
     * @return The localized name of the given tag, or its hexadecimal number if the given value is not
     *         one of the expected constants.
     */
    private static String tagName(final int tag) {
        final short key;
        switch (tag) {
            case DIMENSION: key = Vocabulary.Keys.Dimensions; break;
            case VARIABLE:  key = Vocabulary.Keys.Variables;  break;
            case ATTRIBUTE: key = Vocabulary.Keys.Attributes; break;
            default:        return Integer.toHexString(tag);
        }
        return Vocabulary.format(key);
    }

    /**
     * Returns the localized error resource bundle for the locale given by {@link #getLocale()}.
     *
     * @return The localized error resource bundle.
     */
    private Errors errors() {
        return Errors.getResources(listeners.getLocale());
    }

    /**
     * Returns an exception for a malformed header. This is used only after we have determined
     * that the file should be a NetCDF one, but we found some inconsistency or unknown tags.
     */
    private DataStoreException malformedHeader() {
        return new DataStoreException(errors().getString(Errors.Keys.CanNotParseFile_2, "NetCDF", input.filename));
    }

    /**
     * Ensures that {@code nelems} is not a negative value.
     */
    private void ensureNonNegative(final int nelems, final int tag) throws DataStoreException {
        if (nelems < 0) {
            throw new DataStoreException(errors().getString(Errors.Keys.NegativeArrayLength_1,
                    input.filename + DefaultNameSpace.DEFAULT_SEPARATOR + tagName(tag)));
        }
    }

    /**
     * Makes sure that the buffer contains at least <var>n</var> remaining elements of the given size.
     * If the buffer does not have enough bytes available, more bytes will be read from the channel.
     * If the buffer capacity is not sufficient for reading the given amount of data, an exception
     * is thrown.
     *
     * <p>The NetCDF format add padding after bytes, characters and short integers in order to align
     * the data on multiple of 4 bytes. This method adds such padding to the number of bytes to read.</p>
     *
     * @param  n        The number of elements to read.
     * @param  dataSize The size of each element, in bytes.
     * @param  name     The name of the element to read, used only in case of error for formatting the message.
     * @return The number of bytes to read, rounded to the next multiple of 4.
     */
    private int ensureBufferContains(final int n, final int dataSize, String name) throws IOException, DataStoreException {
        // (n+3) & ~3  is a trick for rounding 'n' to the next multiple of 4.
        final long size = ((n & 0xFFFFFFFFL) * dataSize + 3) & ~3;
        if (size > input.buffer.capacity()) {
            name = input.filename + DefaultNameSpace.DEFAULT_SEPARATOR + name;
            final Errors errors = errors();
            throw new DataStoreException(n < 0 ?
                    errors.getString(Errors.Keys.NegativeArrayLength_1, name) :
                    errors.getString(Errors.Keys.ExcessiveListSize_2, name, n));
        }
        input.ensureBufferContains((int) size);
        return (int) size;
    }

    /**
     * Reads the next offset, which may be encoded on 32 or 64 bits depending on the file format.
     */
    private long readOffset() throws IOException {
        return is64bits ? input.readLong() : input.readUnsignedInt();
    }

    /**
     * Reads a string from the buffer in the {@value #NAME_ENCODING}. This is suitable for the dimension,
     * variable and attribute names in the header. Note that attribute value may have a different encoding.
     */
    private String readName() throws IOException, DataStoreException {
        final int length = input.readInt();
        if (length < 0) {
            throw malformedHeader();
        }
        final int size = ensureBufferContains(length, 1, "<name>");
        final String text = input.readString(length, NAME_ENCODING);
        input.buffer.position(input.buffer.position() + (size - length));
        return text;
    }

    /**
     * Returns the values of the given type. In the given type is {@code CHAR}, then this method returns the values
     * as a {@link String}. Otherwise this method returns the value as an array of the corresponding primitive type
     * and the given length.
     *
     * <p>If the value is a {@code String}, then leading and trailing spaces and control characters have been trimmed
     * by {@link String#trim()}.</p>
     *
     * @return The value, or {@code null} if it was an empty string or an empty array.
     */
    private Object readValues(final String name, final int type, final int length) throws IOException, DataStoreException {
        if (length == 0) {
            return null;
        }
        final ByteBuffer buffer = input.buffer;
        final int size = ensureBufferContains(length, VariableInfo.sizeOf(type), name);
        final int position = buffer.position(); // Must be after 'ensureBufferContains'
        final Object result;
        switch (type) {
            case VariableInfo.CHAR: {
                final String text = input.readString(length, encoding).trim();
                result = text.isEmpty() ? null : text;
                break;
            }
            case VariableInfo.BYTE: {
                final byte[] array = new byte[length];
                buffer.get(array);
                result = array;
                break;
            }
            case VariableInfo.SHORT: {
                final short[] array = new short[length];
                buffer.asShortBuffer().get(array);
                result = array;
                break;
            }
            case VariableInfo.INT: {
                final int[] array = new int[length];
                buffer.asIntBuffer().get(array);
                result = array;
                break;
            }
            case VariableInfo.FLOAT: {
                final float[] array = new float[length];
                buffer.asFloatBuffer().get(array);
                result = array;
                break;
            }
            case VariableInfo.DOUBLE: {
                final double[] array = new double[length];
                buffer.asDoubleBuffer().get(array);
                result = array;
                break;
            }
            default: {
                throw malformedHeader();
            }
        }
        buffer.position(position + size);
        return result;
    }

    /**
     * Reads dimensions from the NetCDF file header. The record structure is:
     *
     * <ul>
     *   <li>The dimension name     (use {@link #readName()})</li>
     *   <li>The dimension length   (use {@link #readInt()})</li>
     * </ul>
     *
     * @param nelems The number of dimensions to read.
     * @return The dimensions in the order they are declared in the NetCDF file.
     */
    private Dimension[] readDimensions(final int nelems) throws IOException, DataStoreException {
        final Dimension[] dimensions = new Dimension[nelems];
        for (int i=0; i<nelems; i++) {
            final String name = readName();
            int length = input.readInt();
            if (length == 0) {
                length = numrecs;
                if (length == STREAMING) {
                    throw new DataStoreException(errors().getString(Errors.Keys.MissingValueForProperty_1, "numrecs"));
                }
            }
            dimensions[i] = new Dimension(name, length);
        }
        return dimensions;
    }

    /**
     * Reads attribute values from the NetCDF file header. Current implementation has no restriction on
     * the location in the header where the NetCDF attribute can be declared. The record structure is:
     *
     * <ul>
     *   <li>The attribute name                             (use {@link #readName()})</li>
     *   <li>The attribute type (BYTE, SHORT, …)            (use {@link #readInt()})</li>
     *   <li>The number of values of the above type         (use {@link #readInt()})</li>
     *   <li>The actual values as a variable length list    (use {@link #readValues(String,int,int)})</li>
     * </ul>
     *
     * If the value is a {@code String}, then leading and trailing spaces and control characters
     * have been trimmed by {@link String#trim()}.
     *
     * @param nelems The number of attributes to read.
     */
    private Attribute[] readAttributes(final int nelems) throws IOException, DataStoreException {
        final Attribute[] attributes = new Attribute[nelems];
        int count = 0;
        for (int i=0; i<nelems; i++) {
            final String name = readName();
            final Object value = readValues(name, input.readInt(), input.readInt());
            if (value != null) {
                attributes[count++] = new Attribute(name, value);
            }
        }
        return ArraysExt.resize(attributes, count);
    }

    /**
     * Reads information (not data) about all variables from the NetCDF file header.
     * The current implementation requires the dimensions to be read before the variables.
     * The record structure is:
     *
     * <ul>
     *   <li>The variable name          (use {@link #readName()})</li>
     *   <li>The number of dimensions   (use {@link #readInt()})</li>
     *   <li>Index of all dimensions    (use {@link #readInt()} <var>n</var> time)</li>
     *   <li>The {@link #ATTRIBUTE} tag (use {@link #readInt()} - actually combined as a long with next item)</li>
     *   <li>Number of attributes       (use {@link #readInt()} - actually combined as a long with above item)</li>
     *   <li>The attribute values       (use {@link #readAttributes(int)})</li>
     *   <li>The data type (BYTE, …)    (use {@link #readInt()})</li>
     *   <li>The variable size          (use {@link #readInt()})</li>
     *   <li>Offset where data begins   (use {@link #readOffset()})</li>
     * </ul>
     *
     * @param nelems     The number of variables to read.
     * @param dimensions The dimensions previously read by {@link #readDimensions(int)}.
     */
    private VariableInfo[] readVariables(final int nelems, final Dimension[] dimensions)
            throws IOException, DataStoreException
    {
        if (dimensions == null) {
            throw malformedHeader();
        }
        final VariableInfo[] variables = new VariableInfo[nelems];
        for (int j=0; j<nelems; j++) {
            final String name = readName();
            final int n = input.readInt();
            final Dimension[] varDims = new Dimension[n];
            try {
                for (int i=0; i<n; i++) {
                    varDims[i] = dimensions[input.readInt()];
                }
            } catch (IndexOutOfBoundsException cause) {
                final DataStoreException e = malformedHeader();
                e.initCause(cause);
                throw e;
            }
            /*
             * Following block is almost a copy-and-paste of similar block in the contructor,
             * but with less cases in the "switch" statements.
             */
            Attribute[] attributes = null;
            final long tn = input.readLong();
            if (tn != 0) {
                final int tag = (int) (tn >>> Integer.SIZE);
                final int na  = (int) tn;
                ensureNonNegative(na, tag);
                switch (tag) {
                    // More cases may be added later if they appear to exist.
                    case ATTRIBUTE: attributes = readAttributes(na); break;
                    default:        throw malformedHeader();
                }
            }
            variables[j] = new VariableInfo(input, name, varDims, dimensions,
                    toMap(attributes, Attribute.NAME_FUNCTION), input.readInt(), input.readInt(), readOffset());
        }
        return variables;
    }

    /**
     * Creates a (<cite>name</cite>, <cite>element</cite>) mapping for the given array of elements.
     * If the name of an element is not all lower cases, then this method also adds an entry for the
     * lower cases version of that name in order to allow case-insensitive searches.
     *
     * <p>Code searching in the returned map shall ask for the original (non lower-case) name
     * <strong>before</strong> to ask for the lower-cases version of that name.</p>
     *
     * @param  <E>          The type of elements.
     * @param  elements     The elements to store in the map, or {@code null} if none.
     * @param  nameFunction The function for computing a name from an element.
     * @return A (<cite>name</cite>, <cite>element</cite>) mapping with lower cases entries where possible.
     * @throws DataStoreException If the same name is used for more than one element.
     *
     * @see #findAttribute(String)
     */
    private <E> Map<String,E> toMap(final E[] elements, final Function<E,String> nameFunction) throws DataStoreException {
        try {
            return CollectionsExt.toCaseInsensitiveNameMap(Arrays.asList(elements), nameFunction, NAME_LOCALE);
        } catch (InvalidParameterCardinalityException e) {
            throw new DataStoreException(errors().getString(Errors.Keys.ValueAlreadyDefined_1, e.getParameterName()));
        }
    }



    // --------------------------------------------------------------------------------------------
    //  Decoder API begins below this point. Above code was specific to parsing of NetCDF header.
    // --------------------------------------------------------------------------------------------

    /**
     * Defines the groups where to search for named attributes, in preference order.
     * The {@code null} group name stands for the global attributes.
     *
     * <p>Current implementation does nothing, since the NetCDF binary files that {@code ChannelDecoder}
     * can read do not have groups anyway. Future SIS implementations may honor the given group names if
     * groups support is added.</p>
     *
     * @throws IOException {@inheritDoc}
     */
    @Override
    public void setSearchPath(final String... groupNames) throws IOException {
    }

    /**
     * Returns the path which is currently set. The array returned by this method may be only
     * a subset of the array given to {@link #setSearchPath(String[])} since only the name of
     * groups which have been found in the NetCDF file are returned by this method.
     *
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public String[] getSearchPath() throws IOException {
        return new String[1];
    }

    /**
     * Returns the NetCDF attribute of the given name, or {@code null} if none.
     * The {@code name} argument is typically (but is not restricted too) one of
     * the constants defined in the {@link AttributeNames} class.
     *
     * @param  name The name of the attribute to search, or {@code null}.
     * @return The attribute, or {@code null} if none.
     */
    private Attribute findAttribute(final String name) {
        Attribute attribute = attributeMap.get(name);
        if (attribute == null && name != null) {
            final String lower = name.toLowerCase(NAME_LOCALE);
            if (lower != name) { // Identity comparison is ok since this check is only an optimization for a common case.
                attribute = attributeMap.get(lower);
            }
        }
        return attribute;
    }

    /**
     * Returns the value for the attribute of the given name, or {@code null} if none.
     *
     * @param  name The name of the attribute to search, or {@code null}.
     * @return The attribute value, or {@code null} if none or empty or if the given name was null.
     * @throws IOException {@inheritDoc}
     */
    @Override
    public String stringValue(final String name) throws IOException {
        final Attribute attribute = findAttribute(name);
        if (attribute != null) {
            return attribute.value.toString();
        }
        return null;
    }

    /**
     * Returns the value of the attribute of the given name as a number, or {@code null} if none.
     * If there is more than one numeric value, only the first one is returned.
     *
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public Number numericValue(final String name) throws IOException {
        final Attribute attribute = findAttribute(name);
        if (attribute != null && attribute.value != null) {
            if (attribute.value instanceof String) {
                return parseNumber((String) attribute.value);
            }
            return (Number) Array.get(attribute.value, 0);
        }
        return null;
    }

    /**
     * Returns the value of the attribute of the given name as a date, or {@code null} if none.
     * If there is more than one numeric value, only the first one is returned.
     *
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public Date dateValue(final String name) throws IOException {
        final Attribute attribute = findAttribute(name);
        if (attribute != null) {
            if (attribute.value instanceof String) try {
                return JDK8.parseDateTime(Attribute.dateToISO((String) attribute.value));
            } catch (IllegalArgumentException e) {
                listeners.warning(null, e);
            }
        }
        return null;
    }

    /**
     * Converts the given numerical values to date, using the information provided in the given unit symbol.
     * The unit symbol is typically a string like <cite>"days since 1970-01-01T00:00:00Z"</cite>.
     *
     * @param  values The values to convert. May contains {@code null} elements.
     * @return The converted values. May contains {@code null} elements.
     * @throws IOException {@inheritDoc}
     */
    @Override
    public Date[] numberToDate(final String symbol, final Number... values) throws IOException {
        final Date[] dates = new Date[values.length];
        final String[] parts = TIME_UNIT_PATTERN.split(symbol);
        if (parts.length == 2) try {
            final UnitConverter converter = Units.valueOf(parts[0]).getConverterToAny(Units.MILLISECOND);
            final long epoch = JDK8.parseDateTime(Attribute.dateToISO(parts[1])).getTime();
            for (int i=0; i<values.length; i++) {
                final Number value = values[i];
                if (value != null) {
                    dates[i] = new Date(epoch + Math.round(converter.convert(value.doubleValue())));
                }
            }
        } catch (ConversionException e) {
            listeners.warning(null, e);
        } catch (IllegalArgumentException e) {
            listeners.warning(null, e);
        }
        return dates;
    }

    /**
     * Returns all variables found in the NetCDF file.
     * This method returns a direct reference to an internal array - do not modify.
     *
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Variable[] getVariables() throws IOException {
        return variables;
    }

    /**
     * Returns all grid geometries found in the NetCDF file.
     * This method returns a direct reference to an internal array - do not modify.
     *
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public GridGeometry[] getGridGeometries() throws IOException {
        if (gridGeometries == null) {
            /*
             * First, find all variables which are used as coordinate system axis. The keys are the
             * grid dimensions which are the domain of the variable (i.e. the sources of the conversion
             * from grid coordinates to CRS coordinates).
             */
            final Map<Dimension, List<VariableInfo>> dimToAxes = new IdentityHashMap<Dimension, List<VariableInfo>>();
            for (final VariableInfo variable : variables) {
                if (variable.isCoordinateSystemAxis()) {
                    for (final Dimension dimension : variable.dimensions) {
                        CollectionsExt.addToMultiValuesMap(dimToAxes, dimension, variable);
                    }
                }
            }
            /*
             * For each variables, gets the list of all axes associated to their dimensions. The association
             * is given by the above 'dimToVars' map. More than one variable may have the same dimensions,
             * and consequently the same axes, so we will remember the previously created instances in order
             * to share them.
             */
            final Set<VariableInfo> axes = new LinkedHashSet<VariableInfo>(4);
            final Map<List<Dimension>, GridGeometryInfo> dimsToGG = new LinkedHashMap<List<Dimension>, GridGeometryInfo>();
nextVar:    for (final VariableInfo variable : variables) {
                if (variable.isCoordinateSystemAxis()) {
                    continue;
                }
                final List<Dimension> dimensions = Arrays.asList(variable.dimensions);
                GridGeometryInfo gridGeometry = dimsToGG.get(dimensions);
                if (gridGeometry == null) {
                    /*
                     * Found a new list of dimensions for which no axes have been created yet.
                     * If and only if we can find all axes, then create the GridGeometryInfo.
                     * This is a "all or nothing" operation.
                     */
                    for (final Dimension dimension : variable.dimensions) {
                        final List<VariableInfo> axis = dimToAxes.get(dimension);
                        if (axis == null) {
                            axes.clear();
                            continue nextVar;
                        }
                        axes.addAll(axis);
                    }
                    gridGeometry = new GridGeometryInfo(variable.dimensions, axes.toArray(new VariableInfo[axes.size()]));
                    dimsToGG.put(dimensions, gridGeometry);
                    axes.clear();
                }
                variable.gridGeometry = gridGeometry;
            }
            gridGeometries = dimsToGG.values().toArray(new GridGeometry[dimsToGG.size()]);
        }
        return gridGeometries;
    }

    /**
     * Closes the channel.
     *
     * @throws IOException If an error occurred while closing the channel.
     */
    @Override
    public void close() throws IOException {
        input.channel.close();
    }

    /**
     * Returns a string representation to be inserted in {@link org.apache.sis.storage.netcdf.NetcdfStore#toString()}
     * result. This is for debugging purpose only any may change in any future SIS version.
     *
     * @return {@inheritDoc}
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("SIS driver: “").append(input.filename).append('”');
        if (!input.channel.isOpen()) {
            buffer.append(" (closed)");
        }
        return buffer.toString();
    }
}
