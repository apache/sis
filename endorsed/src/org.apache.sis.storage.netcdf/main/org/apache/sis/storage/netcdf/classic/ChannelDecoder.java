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
package org.apache.sis.storage.netcdf.classic;

import java.util.Set;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.AbstractMap;
import java.util.AbstractList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.time.DateTimeException;
import java.time.Instant;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.channels.ReadableByteChannel;
import java.time.temporal.Temporal;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import javax.measure.format.MeasurementParseException;
import org.opengis.parameter.InvalidParameterCardinalityException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.netcdf.base.DataType;
import org.apache.sis.storage.netcdf.base.Decoder;
import org.apache.sis.storage.netcdf.base.Node;
import org.apache.sis.storage.netcdf.base.Grid;
import org.apache.sis.storage.netcdf.base.Variable;
import org.apache.sis.storage.netcdf.base.Dimension;
import org.apache.sis.storage.netcdf.base.Convention;
import org.apache.sis.storage.netcdf.base.NamedElement;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.temporal.TemporalDate;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.measure.Units;
import org.apache.sis.math.Vector;
import org.apache.sis.pending.jdk.JDK19;


/**
 * Provides netCDF decoding services as a standalone library.
 * The javadoc in this class uses the "file" word for the source of data, but
 * this implementation actually works with arbitrary {@link ReadableByteChannel}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="http://portal.opengeospatial.org/files/?artifact_id=43734">NetCDF Classic and 64-bit Offset Format (1.0)</a>
 */
public final class ChannelDecoder extends Decoder {
    /**
     * The netCDF magic number expected in the first integer of the stream.
     * The comparison shall ignore the 8 lowest bits, as in the following example:
     *
     * {@snippet lang="java" :
     *     int header = ...;     // The first integer in the stream.
     *     boolean isNetCDF = (header & 0xFFFFFF00) == MAGIC_NUMBER;
     *     }
     */
    public static final int MAGIC_NUMBER = ('C' << 24) | ('D' << 16) | ('F' <<  8);

    /**
     * The maximal version number supported by this implementation.
     */
    public static final int MAX_VERSION = 2;

    /**
     * The encoding of dimension, variable and attribute names. This is fixed to UTF-8 by the netCDF specification.
     * Note however that the encoding of attribute values may be different.
     *
     * @see #encoding
     * @see #readName()
     */
    private static final Charset NAME_ENCODING = StandardCharsets.UTF_8;

    /*
     * NOTE: the names of the static constants below this point match the names used in the Backus-Naur Form (BNF)
     *       definitions in the netCDF Classic and 64-bit Offset Format (1.0) specification (link in class javdoc),
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
     * dimension, variable and attribute names, which are fixed to UTF-8 as of netCDF specification.
     *
     * The specification said: "Although the characters used in netCDF names must be encoded as UTF-8,
     * character data may use other encodings. The variable attribute “_Encoding” is reserved for this
     * purpose in future implementations."
     *
     * In current Apache SIS implementation, the "_Encoding" attribute value takes effect only on the
     * attributes declared after the "_Encoding" attribute. If the attribute is a variable attribute,
     * its effect is local to that variable.
     *
     * @see #NAME_ENCODING
     * @see #getEncoding()
     * @see #readValues(DataType, int)
     */
    private Charset encoding;

    /**
     * The variables found in the netCDF file.
     *
     * @see #getVariables()
     */
    final VariableInfo[] variables;

    /**
     * Contains all {@link #variables}, but as a map for faster lookup by name. The same {@link VariableInfo}
     * instance may be repeated in two entries if the original variable name contains upper case letters.
     * In such case, the value is repeated and associated to a key in all lower case key letters.
     *
     * @see #findVariable(String)
     */
    private final Map<String,VariableInfo> variableMap;

    /**
     * The attributes found in the netCDF file.
     * Values in this map give directly the attribute value (there is no {@code Attribute} object).
     * Values are {@link String}, wrappers such as {@link Double}, or {@link Vector} objects.
     *
     * @see #findAttribute(String)
     */
    private final Map<String,Object> attributeMap;

    /**
     * Names of attributes. This is {@code attributeMap.keySet()} unless some attributes have a name
     * containing upper case letters. In such case a separated set is created for avoiding duplicated
     * names (the name with upper case letters + the name in all lower case letters).
     *
     * @see #getAttributeNames()
     */
    private final Set<String> attributeNames;

    /**
     * All dimensions in the netCDF files.
     *
     * @see #readDimensions(int)
     * @see #findDimension(String)
     */
    private Map<String,DimensionInfo> dimensionMap;

    /**
     * The grid geometries, created when first needed.
     *
     * @see #getGridCandidates()
     */
    private transient Grid[] gridGeometries;

    /**
     * Creates a new decoder for the given file.
     * This constructor parses immediately the header, which shall have the following structure:
     *
     * <ul>
     *   <li>Magic number: 'C','D','F'</li>
     *   <li>Version number: 1 or 2</li>
     *   <li>Number of records | {@value #STREAMING}</li>
     *   <li>List of netCDF dimensions  (see {@link #readDimensions(int)})</li>
     *   <li>List of global attributes  (see {@link #readAttributes(int)})</li>
     *   <li>List of variables          (see {@link #readVariables(int, DimensionInfo[])})</li>
     * </ul>
     *
     * @param  input      the channel and the buffer from where data are read.
     * @param  encoding   the encoding of attribute value, or {@code null} for the default value.
     * @param  geomlib    the library for geometric objects, or {@code null} for the default.
     * @param  listeners  where to send the warnings.
     * @throws IOException if an error occurred while reading the channel.
     * @throws DataStoreException if the content of the given channel is not a netCDF file.
     * @throws ArithmeticException if a variable is too large.
     */
    public ChannelDecoder(final ChannelDataInput input, final Charset encoding, final GeometryLibrary geomlib,
            final StoreListeners listeners) throws IOException, DataStoreException
    {
        super(geomlib, listeners);
        this.input = input;
        this.encoding = (encoding != null) ? encoding : StandardCharsets.UTF_8;
        /*
         * Check the magic number, which is expected to be exactly 3 bytes forming the "CDF" string.
         * The 4th byte is the version number, which we opportunistically use after the magic number check.
         */
        int version = input.readInt();
        if ((version & 0xFFFFFF00) != MAGIC_NUMBER) {
            throw new DataStoreContentException(errors().getString(Errors.Keys.UnexpectedFileFormat_2, FORMAT_NAME, getFilename()));
        }
        /*
         * Check the version number.
         */
        version &= 0xFF;
        switch (version) {
            case 1:  is64bits = false; break;
            case 2:  is64bits = true;  break;
            default: throw new DataStoreContentException(errors().getString(Errors.Keys.UnsupportedFormatVersion_2, FORMAT_NAME, version));
            // If more cases are added, remember to increment the MAX_VERSION constant.
        }
        numrecs = input.readInt();
        /*
         * Read the dimension, attribute and variable declarations. We expect exactly 3 lists,
         * where any of them can be flagged as absent by a long (64 bits) 0.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        VariableInfo[]  variables  = null;
        DimensionInfo[] dimensions = null;
        List<Map.Entry<String,Object>> attributes = List.of();
        for (int i=0; i<3; i++) {
            final long tn = input.readLong();                   // Combination of tag and nelems
            if (tn != 0) {
                final int tag = (int) (tn >>> Integer.SIZE);
                final int nelems = (int) tn;
                ensureNonNegative(nelems, tag);
                try {
                    switch (tag) {
                        case DIMENSION: dimensions = readDimensions(nelems); break;
                        case VARIABLE:  variables  = readVariables (nelems, dimensions); break;
                        case ATTRIBUTE: attributes = readAttributes(nelems); break;
                        default:        throw malformedHeader();
                    }
                } catch (InvalidParameterCardinalityException e) {
                    throw malformedHeader().initCause(e);
                }
            }
        }
        attributeMap = CollectionsExt.toCaseInsensitiveNameMap(attributes, Decoder.DATA_LOCALE);
        attributeNames = attributeNames(attributes, attributeMap);
        if (variables != null) {
            this.variables   = variables;
            this.variableMap = toCaseInsensitiveNameMap(variables);
        } else {
            this.variables   = new VariableInfo[0];
            this.variableMap = Map.of();
        }
        initialize();
    }

    /**
     * Creates a (<var>name</var>, <var>element</var>) mapping for the given array of elements.
     * If the name of an element is not all lower cases, then this method also adds an entry for the
     * lower cases version of that name in order to allow case-insensitive searches.
     *
     * <p>Code searching in the returned map shall ask for the original (non lower-case) name
     * <strong>before</strong> to ask for the lower-cases version of that name.</p>
     *
     * @param  <E>       the type of elements.
     * @param  elements  the elements to store in the map, or {@code null} if none.
     * @return a (<var>name</var>, <var>element</var>) mapping with lower cases entries where possible.
     * @throws InvalidParameterCardinalityException if the same name is used for more than one element.
     */
    private static <E extends NamedElement> Map<String,E> toCaseInsensitiveNameMap(final E[] elements) {
        return CollectionsExt.toCaseInsensitiveNameMap(new AbstractList<Map.Entry<String,E>>() {
            @Override
            public int size() {
                return elements.length;
            }

            @Override
            public Map.Entry<String,E> get(final int index) {
                final E e = elements[index];
                return new AbstractMap.SimpleImmutableEntry<>(e.getName(), e);
            }
        }, Decoder.DATA_LOCALE);
    }

    /**
     * Return the localized name of the given tag.
     *
     * @param  tag  one of {@link #DIMENSION}, {@link #VARIABLE} or {@link #ATTRIBUTE} constants.
     * @return the localized name of the given tag, or its hexadecimal number if the given value
     *         is not one of the expected constants.
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
     * Returns the localized error resource bundle for the locale given by {@link StoreListeners#getLocale()}.
     *
     * @return the localized error resource bundle.
     */
    final Errors errors() {
        return Errors.forLocale(getLocale());
    }

    /**
     * Returns an exception for a malformed header. This is used only after we have determined
     * that the file should be a netCDF one, but we found some inconsistency or unknown tags.
     */
    private DataStoreContentException malformedHeader() {
        return new DataStoreContentException(getLocale(), FORMAT_NAME, getFilename(), null);
    }

    /**
     * Ensures that {@code nelems} is not a negative value.
     */
    private void ensureNonNegative(final int nelems, final int tag) throws DataStoreContentException {
        if (nelems < 0) {
            throw new DataStoreContentException(errors().getString(Errors.Keys.NegativeArrayLength_1, tagPath(tagName(tag))));
        }
    }

    /**
     * Returns the name of a tag to show in error message. The returned name include the filename.
     */
    private String tagPath(final String name) {
        return getFilename() + Constants.DEFAULT_SEPARATOR + name;
    }

    /**
     * Aligns position in the stream after reading the given number of bytes.
     * This method should be invoked only for {@link DataType#BYTE} and {@link DataType#CHAR}.
     *
     * <p>The netCDF format adds padding after bytes, characters and short integers in order to align the data
     * on multiple of 4 bytes. This method is used for adding such padding to the number of bytes to read.</p>
     *
     * @param  length   number of byte reads.
     * @throws IOException if an error occurred while skipping bytes.
     */
    private void align(int length) throws IOException {
        length &= 3;
        if (length != 0) {
            length = 4 - length;
            input.ensureBufferContains(length);
            input.buffer.position(input.buffer.position() + length);
        }
    }

    /**
     * Reads the next offset, which may be encoded on 32 or 64 bits depending on the file format.
     */
    private long readOffset() throws IOException {
        return is64bits ? input.readLong() : input.readUnsignedInt();
    }

    /**
     * Reads a string from the channel in the {@link #NAME_ENCODING}. This is suitable for the dimension,
     * variable and attribute names in the header. Note that attribute value may have a different encoding.
     */
    private String readName() throws IOException, DataStoreContentException {
        final int length = input.readInt();
        if (length < 0) {
            throw malformedHeader();
        }
        final String text = input.readString(length, NAME_ENCODING);
        align(length);
        return text;
    }

    /**
     * Returns the values of the given type. In the given type is {@code CHAR}, then this method returns the values
     * as a {@link String}. Otherwise if the length is 1, then this method returns the primitive value in its wrapper.
     * Otherwise this method returns the value as a {@link Vector} of the corresponding primitive type and given length.
     *
     * <p>If the value is a {@code String}, then leading and trailing spaces and control characters have been trimmed
     * by {@link String#trim()}.</p>
     *
     * @return the value, or {@code null} if it was an empty string or an empty array.
     */
    private Object readValues(final DataType type, final int length) throws IOException, DataStoreContentException {
        if (length <= 0) {
            if (length == 0) {
                return null;
            }
            throw malformedHeader();
        }
        if (length == 1) {
            switch (type) {
                case BYTE:   {final byte  v =         input.readByte();          align(1); return v;}
                case UBYTE:  {final short v = (short) input.readUnsignedByte();  align(1); return v;}
                case SHORT:  {final short v =         input.readShort();         align(2); return v;}
                case USHORT: {final int   v =         input.readUnsignedShort(); align(2); return v;}
                case INT:    return input.readInt();
                case INT64:  return input.readLong();
                case UINT:   return input.readUnsignedInt();
                case FLOAT:  return input.readFloat();
                case DOUBLE: return input.readDouble();
            }
        }
        final Object data;
        switch (type) {
            case CHAR: {
                final String text = input.readString(length, encoding);
                align(length);
                return text.isEmpty() ? null : text;
            }
            case BYTE:
            case UBYTE: {
                final byte[] array = new byte[length];
                input.readFully(array);
                align(length);
                data = array;
                break;
            }
            case SHORT:
            case USHORT: {
                final short[] array = new short[length];
                input.readFully(array, 0, length);
                align(length << 1);
                data = array;
                break;
            }
            case INT:
            case UINT: {
                final int[] array = new int[length];
                input.readFully(array, 0, length);
                data =  array;
                break;
            }
            case INT64:
            case UINT64: {
                final long[] array = new long[length];
                input.readFully(array, 0, length);
                data = array;
                break;
            }
            case FLOAT: {
                final float[] array = new float[length];
                input.readFully(array, 0, length);
                return Vector.createForDecimal(array);
            }
            case DOUBLE: {
                final double[] array = new double[length];
                input.readFully(array, 0, length);
                final float[] asFloats = ArraysExt.copyAsFloatsIfLossless(array);
                if (asFloats != null) return Vector.createForDecimal(asFloats);
                data = array;
                break;
            }
            default: {
                throw malformedHeader();
            }
        }
        return Vector.create(data, type.isUnsigned);
    }

    /**
     * Reads dimensions from the netCDF file header. The record structure is:
     *
     * <ul>
     *   <li>The dimension name     (use {@link #readName()})</li>
     *   <li>The dimension length   (use {@link ChannelDataInput#readInt()})</li>
     * </ul>
     *
     * @param  nelems  the number of dimensions to read.
     * @return the dimensions in the order they are declared in the netCDF file.
     */
    private DimensionInfo[] readDimensions(final int nelems) throws IOException, DataStoreContentException {
        final DimensionInfo[] dimensions = new DimensionInfo[nelems];
        for (int i=0; i<nelems; i++) {
            final String name = readName();
            int length = input.readInt();
            boolean isUnlimited = (length == 0);
            if (isUnlimited) {
                length = numrecs;
                if (length == STREAMING) {
                    throw new DataStoreContentException(errors().getString(Errors.Keys.MissingValueForProperty_1, tagPath("numrecs")));
                }
            }
            dimensions[i] = new DimensionInfo(name, length, isUnlimited);
        }
        dimensionMap = toCaseInsensitiveNameMap(dimensions);
        return dimensions;
    }

    /**
     * Reads attribute values from the netCDF file header. Current implementation has no restriction on
     * the location in the header where the netCDF attribute can be declared. The record structure is:
     *
     * <ul>
     *   <li>The attribute name                             (use {@link #readName()})</li>
     *   <li>The attribute type (BYTE, SHORT, …)            (use {@link ChannelDataInput#readInt()})</li>
     *   <li>The number of values of the above type         (use {@link ChannelDataInput#readInt()})</li>
     *   <li>The actual values as a variable length list    (use {@link #readValues(DataType,int)})</li>
     * </ul>
     *
     * If the value is a {@code String}, then leading and trailing spaces and control characters have been
     * trimmed by {@link String#trim()}. If the value has more than one element, then the values are stored
     * in a {@link Vector}.
     *
     * @param  nelems  the number of attributes to read.
     */
    private List<Map.Entry<String,Object>> readAttributes(int nelems) throws IOException, DataStoreException {
        final List<Map.Entry<String,Object>> attributes = new ArrayList<>(nelems);
        while (--nelems >= 0) {
            final String name = readName();
            final Object value = readValues(DataType.valueOf(input.readInt()), input.readInt());
            if (value != null) {
                attributes.add(new AbstractMap.SimpleEntry<>(name, value));
                if (name.equals("_Encoding")) try {
                    encoding = Charset.forName(name);
                } catch (IllegalArgumentException e) {
                    listeners.warning(Errors.format(Errors.Keys.CanNotReadPropertyInFile_2, getFilename(), "_Encoding"), e);
                }
            }
        }
        return attributes;
    }

    /**
     * Reads information (not data) about all variables from the netCDF file header.
     * The current implementation requires the dimensions to be read before the variables.
     * The record structure is:
     *
     * <ul>
     *   <li>The variable name          (use {@link #readName()})</li>
     *   <li>The number of dimensions   (use {@link ChannelDataInput#readInt()})</li>
     *   <li>Index of all dimensions    (use {@link ChannelDataInput#readInt()} <var>n</var> time)</li>
     *   <li>The {@link #ATTRIBUTE} tag (use {@link ChannelDataInput#readInt()} - actually combined as a long with next item)</li>
     *   <li>Number of attributes       (use {@link ChannelDataInput#readInt()} - actually combined as a long with above item)</li>
     *   <li>The attribute values       (use {@link #readAttributes(int)})</li>
     *   <li>The data type (BYTE, …)    (use {@link ChannelDataInput#readInt()})</li>
     *   <li>The variable size          (use {@link ChannelDataInput#readInt()})</li>
     *   <li>Offset where data begins   (use {@link #readOffset()})</li>
     * </ul>
     *
     * @param  nelems         the number of variables to read.
     * @param  allDimensions  the dimensions previously read by {@link #readDimensions(int)}.
     * @throws DataStoreContentException if a logical error is detected.
     * @throws ArithmeticException if a variable is too large.
     */
    private VariableInfo[] readVariables(final int nelems, final DimensionInfo[] allDimensions)
            throws IOException, DataStoreException
    {
        if (allDimensions == null) {
            throw malformedHeader();        // May happen if readDimensions(…) has not been invoked.
        }
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final VariableInfo[] variables = new VariableInfo[nelems];
        for (int j=0; j<nelems; j++) {
            final String name = readName();
            final int n = input.readInt();
            final DimensionInfo[] varDims = new DimensionInfo[n];
            try {
                for (int i=0; i<n; i++) {
                    varDims[i] = allDimensions[input.readInt()];
                }
            } catch (IndexOutOfBoundsException cause) {
                throw malformedHeader().initCause(cause);
            }
            /*
             * Following block is almost a copy-and-paste of similar block in the contructor,
             * but with less cases in the "switch" statements.
             */
            List<Map.Entry<String,Object>> attributes = List.of();
            final long tn = input.readLong();
            if (tn != 0) {
                final int tag = (int) (tn >>> Integer.SIZE);
                final int na  = (int) tn;
                ensureNonNegative(na, tag);
                switch (tag) {
                    // More cases may be added later if they appear to exist.
                    case ATTRIBUTE: {
                        final Charset globalEncoding = encoding;
                        attributes = readAttributes(na);            // May change the encoding.
                        encoding = globalEncoding;
                        break;
                    }
                    default: throw malformedHeader();
                }
            }
            final Map<String,Object> map = CollectionsExt.toCaseInsensitiveNameMap(attributes, Decoder.DATA_LOCALE);
            variables[j] = new VariableInfo(this, input, name, varDims, map, attributeNames(attributes, map),
                    DataType.valueOf(input.readInt()), input.readInt(), readOffset());
        }
        /*
         * The VariableInfo constructor determined if the variables are "unlimited" or not.
         * The number of unlimited variable determines to padding to apply, because of an
         * historical particularity in netCDF format. Those final adjustment can be done
         * only after we finished creating all variables.
         */
        VariableInfo.complete(variables);
        return variables;
    }

    /**
     * Returns the keys of {@code attributeMap} without the duplicated values caused by the change of name case.
     * For example if an attribute {@code "Foo"} exists and a {@code "foo"} key has been generated for enabling
     * case-insensitive search, only the {@code "Foo"} name is added in the returned set.
     *
     * @param  attributes    the attributes returned by {@link #readAttributes(int)}.
     * @param  attributeMap  the map created by {@link CollectionsExt#toCaseInsensitiveNameMap(Collection, Locale)}.
     * @return {@code attributes.keySet()} without duplicated keys.
     */
    private static Set<String> attributeNames(final List<Map.Entry<String,Object>> attributes, final Map<String,?> attributeMap) {
        if (attributes.size() >= attributeMap.size()) {
            return Collections.unmodifiableSet(attributeMap.keySet());
        }
        final Set<String> attributeNames = JDK19.newLinkedHashSet(attributes.size());
        attributes.forEach((e) -> attributeNames.add(e.getKey()));
        return attributeNames;
    }



    // --------------------------------------------------------------------------------------------
    //  Decoder API begins below this point. Above code was specific to parsing of netCDF header.
    // --------------------------------------------------------------------------------------------

    /**
     * Returns a filename for formatting error message and for information purpose.
     * The filename does not contain path, but may contain file extension.
     *
     * @return a filename to include in warnings or error messages.
     */
    @Override
    public final String getFilename() {
        return input.filename;
    }

    /**
     * Sets an identification of the file format. This method uses a reference to a database entry
     * known to {@link org.apache.sis.metadata.sql.MetadataSource#lookup(Class, String)}.
     */
    @Override
    public void addFormatDescription(MetadataBuilder builder) {
        builder.setPredefinedFormat(Constants.NETCDF, null, true);
        builder.addFormatReaderSIS(Constants.NETCDF);
    }

    /**
     * Defines the groups where to search for named attributes, in preference order.
     * The {@code null} group name stands for the global attributes.
     *
     * <p>Current implementation does nothing, since the netCDF binary files that {@code ChannelDecoder}
     * can read do not have groups anyway. Future SIS implementations may honor the given group names if
     * groups support is added.</p>
     */
    @Override
    public void setSearchPath(final String... groupNames) {
    }

    /**
     * Returns the path which is currently set. The array returned by this method may be only
     * a subset of the array given to {@link #setSearchPath(String[])} since only the name of
     * groups which have been found in the netCDF file are returned by this method.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String[] getSearchPath() {
        return new String[1];
    }

    /**
     * Returns the dimension of the given name (eventually ignoring case), or {@code null} if none.
     * This method searches in all dimensions found in the netCDF file, regardless of variables.
     * The search will ignore case only if no exact match is found for the given name.
     *
     * @param  dimName  the name of the dimension to search.
     * @return dimension of the given name, or {@code null} if none.
     */
    @Override
    @SuppressWarnings("StringEquality")
    protected Dimension findDimension(final String dimName) {
        DimensionInfo dim = dimensionMap.get(dimName);          // Give precedence to exact match before to ignore case.
        if (dim == null) {
            final String lower = dimName.toLowerCase(Decoder.DATA_LOCALE);
            if (lower != dimName) {                             // Identity comparison is okay here.
                dim = dimensionMap.get(lower);
            }
        }
        return dim;
    }

    /**
     * Returns the netCDF variable of the given name, or {@code null} if none.
     *
     * @param  name  the name of the variable to search, or {@code null}.
     * @return the variable of the given name, or {@code null} if none.
     */
    @SuppressWarnings("StringEquality")
    private VariableInfo findVariableInfo(final String name) {
        VariableInfo v = variableMap.get(name);
        if (v == null && name != null) {
            final String lower = name.toLowerCase(Decoder.DATA_LOCALE);
            // Identity comparison is ok since following check is only an optimization for a common case.
            if (lower != name) {
                v = variableMap.get(lower);
            }
        }
        return v;
    }

    /**
     * Returns the netCDF variable of the given name, or {@code null} if none.
     *
     * @param  name  the name of the variable to search, or {@code null}.
     * @return the variable of the given name, or {@code null} if none.
     */
    @Override
    protected Variable findVariable(final String name) {
        return findVariableInfo(name);
    }

    /**
     * Returns the variable of the given name. Note that groups do not exist in netCDF 3.
     *
     * @param  name  the name of the variable to search, or {@code null}.
     * @return the variable of the given name, or {@code null} if none.
     */
    @Override
    protected Node findNode(final String name) {
        return findVariableInfo(name);
    }

    /**
     * Returns the netCDF attribute of the given name, or {@code null} if none. This method is invoked
     * for every global attributes to be read by this class (but not {@linkplain VariableInfo variable}
     * attributes), thus providing a single point where we can filter the attributes to be read.
     * The {@code name} argument is typically (but is not restricted to) one of the constants
     * defined in the {@link org.apache.sis.storage.netcdf.AttributeNames} class.
     *
     * @param  name  the name of the attribute to search, or {@code null}.
     * @return the attribute value, or {@code null} if none.
     *
     * @see #getAttributeNames()
     */
    @SuppressWarnings("StringEquality")
    private Object findAttribute(final String name) {
        if (name == null) {
            return null;
        }
        int index = 0;
        String mappedName;
        final Convention convention = convention();
        while ((mappedName = convention.mapAttributeName(name, index++)) != null) {
            Object value = attributeMap.get(mappedName);
            if (value != null) return value;
            /*
             * If no value were found for the given name, tries the following alternatives:
             *
             *   - Same name but in lower cases.
             *   - Alternative name specific to the non-standard convention used by current file.
             *   - Same alternative name but in lower cases.
             *
             * Identity comparisons performed between String instances below are okay since they
             * are only optimizations for skipping calls to Map.get(Object) in common cases.
             */
            final String lowerCase = mappedName.toLowerCase(DATA_LOCALE);
            if (lowerCase != mappedName) {
                value = attributeMap.get(lowerCase);
                if (value != null) return value;
            }
        }
        return null;
    }

    /**
     * Returns the names of all global attributes found in the file.
     * The returned set is unmodifiable.
     *
     * @return names of all global attributes in the file.
     */
    @Override
    public Collection<String> getAttributeNames() {
        return Collections.unmodifiableSet(attributeNames);
    }

    /**
     * Returns the value for the attribute of the given name, or {@code null} if none.
     *
     * @param  name  the name of the attribute to search, or {@code null}.
     * @return the attribute value, or {@code null} if none or empty or if the given name was null.
     */
    @Override
    public String stringValue(final String name) {
        final Object value = findAttribute(name);
        return (value != null) ? value.toString() : null;
    }

    /**
     * Returns the value of the attribute of the given name as a number, or {@code null} if none.
     * If there is more than one numeric value, only the first one is returned.
     *
     * @return {@inheritDoc}
     */
    @Override
    public Number numericValue(final String name) {
        final Object value = findAttribute(name);
        if (value instanceof Number) {
            return (Number) value;
        } else if (value instanceof String) {
            return parseNumber(name, (String) value);
        } else if (value instanceof Vector) {
            return ((Vector) value).get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns the value of the attribute of the given name as a date, or {@code null} if none.
     * If there is more than one numeric value, only the first one is returned.
     *
     * @return {@inheritDoc}
     */
    @Override
    public Temporal dateValue(final String name) {
        final Object value = findAttribute(name);
        if (value instanceof CharSequence) try {
            return LenientDateFormat.parseBest((CharSequence) value);
        } catch (RuntimeException e) {
            listeners.warning(e);
        }
        return null;
    }

    /**
     * Converts the given numerical values to date, using the information provided in the given unit symbol.
     * The unit symbol is typically a string like <q>days since 1970-01-01T00:00:00Z</q>.
     *
     * @param  values  the values to convert. May contain {@code null} elements.
     * @return the converted values. May contain {@code null} elements.
     */
    @Override
    public Temporal[] numberToDate(final String symbol, final Number... values) {
        final var dates = new Instant[values.length];
        final Matcher parts = Variable.TIME_UNIT_PATTERN.matcher(symbol);
        if (parts.matches()) try {
            final UnitConverter converter = Units.valueOf(parts.group(1)).getConverterToAny(Units.SECOND);
            final Instant epoch = LenientDateFormat.parseInstantUTC(parts.group(2));
            for (int i=0; i<values.length; i++) {
                final Number value = values[i];
                if (value != null) {
                    dates[i] = TemporalDate.addSeconds(epoch, converter.convert(value.doubleValue()));
                }
            }
        } catch (IncommensurableException | MeasurementParseException | DateTimeException | ArithmeticException e) {
            listeners.warning(e);
        }
        return dates;
    }

    /**
     * Returns the encoding for attribute or variable data.
     * This is <strong>not</strong> the encoding of netCDF names.
     *
     * @return encoding of data (not the encoding of netCDF names).
     */
    final Charset getEncoding() {
        return encoding;
    }

    /**
     * Returns all variables found in the netCDF file.
     * This method returns a direct reference to an internal array - do not modify.
     *
     * @return {@inheritDoc}
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Variable[] getVariables() {
        return variables;
    }

    /**
     * Adds to the given set all variables of the given names. This operation is performed when the set of axes is
     * specified by a {@code "coordinates"} attribute associated to a data variable, or by customized conventions
     * specified by {@link org.apache.sis.storage.netcdf.base.Convention#namesOfAxisVariables(Variable)}.
     *
     * @param  names       names of variables containing axis data, or {@code null} if none.
     * @param  axes        where to add named variables.
     * @param  dimensions  where to report all dimensions used by added axes.
     * @return whether {@code names} was non-null and non-empty.
     */
    private boolean listAxes(final CharSequence[] names, final Set<VariableInfo> axes, final Set<DimensionInfo> dimensions) {
        if (names == null || names.length == 0) {
            return false;
        }
        for (final CharSequence name : names) {
            final VariableInfo axis = findVariableInfo(name.toString());
            if (axis == null) {
                dimensions.clear();
                axes.clear();
                break;
            }
            axes.add(axis);
            Collections.addAll(dimensions, axis.dimensions);
        }
        return true;
    }

    /**
     * Returns all grid geometries found in the netCDF file.
     * This method returns a direct reference to an internal array - do not modify.
     *
     * @return {@inheritDoc}
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Grid[] getGridCandidates() {
        if (gridGeometries == null) {
            /*
             * First, find all variables which are used as coordinate system axis. The keys in the map are
             * the grid dimensions which are the domain of the variable (i.e. the sources of the conversion
             * from grid coordinates to CRS coordinates). For each key there is usually only one value, but
             * more complicated netCDF files (e.g. using two-dimensional localisation grids) also exist.
             */
            final var dimToAxes = new IdentityHashMap<DimensionInfo, Set<VariableInfo>>();
            for (final VariableInfo variable : variables) {
                switch (variable.getRole()) {
                    case COVERAGE:
                    case DISCRETE_COVERAGE: {
                        // If Convention.roleOf(…) overwrote the value computed by VariableInfo,
                        // remember the new value for avoiding to ask again in next loops.
                        variable.isCoordinateSystemAxis = false;
                        break;
                    }
                    case AXIS: {
                        variable.isCoordinateSystemAxis = true;
                        for (final DimensionInfo dimension : variable.dimensions) {
                            CollectionsExt.addToMultiValuesMap(dimToAxes, dimension, variable);
                        }
                    }
                }
            }
            /*
             * For each variable, get its list of axes. More than one variable may have the same list of axes,
             * so we remember the previously created instances in order to share the grid geometry instances.
             */
            final var axes = new LinkedHashSet<VariableInfo>(8);
            final var usedDimensions = new HashSet<DimensionInfo>(8);
            final var shared = new LinkedHashMap<GridInfo,GridInfo>();
nextVar:    for (final VariableInfo variable : variables) {
                if (variable.isCoordinateSystemAxis || variable.dimensions.length == 0) {
                    continue;
                }
                /*
                 * The axes can be inferred in two ways: if the variable contains a "coordinates" attribute,
                 * that attribute lists explicitly the variables to use as axes. Otherwise we have to infer
                 * the axes from the variable dimensions, using the `dimToAxes` map computed at the beginning
                 * of this method. If and only if we can find all axes, we create the GridGeometryInfo.
                 * This is a "all or nothing" operation.
                 */
                axes.clear();
                usedDimensions.clear();
                if (!listAxes(variable.getCoordinateVariables(), axes, usedDimensions)) {
                    listAxes(convention().namesOfAxisVariables(variable), axes, usedDimensions);
                }
                /*
                 * In theory the "coordinates" attribute would enumerate all axes needed for covering all dimensions,
                 * and we would not need to check for variables having dimension names. However, in practice there is
                 * incomplete attributes, so we check for other dimensions even if the above loop did some work.
                 */
                for (int i=variable.dimensions.length; --i >= 0;) {                     // Reverse of netCDF order.
                    final DimensionInfo dimension = variable.dimensions[i];
                    if (usedDimensions.add(dimension)) {
                        final Set<VariableInfo> axis = dimToAxes.get(dimension);       // Should have only 1 element.
                        if (axis == null) {
                            continue nextVar;
                        }
                        axes.addAll(axis);
                    }
                }
                /*
                 * Creates the grid geometry using the given domain and range, reusing existing instance if one exists.
                 * We usually try to preserve axis order as declared in the netCDF file. But if we mixed axes inferred
                 * from the "coordinates" attribute and axes inferred from variable names matching dimension names, we
                 * use axes from "coordinates" attribute first followed by other axes.
                 */
                GridInfo grid = new GridInfo(variable.dimensions, axes.toArray(VariableInfo[]::new));
                GridInfo existing = shared.putIfAbsent(grid, grid);
                if (existing != null) {
                    grid = existing;
                }
                variable.grid = grid;
            }
            gridGeometries = shared.values().toArray(Grid[]::new);
        }
        return gridGeometries;
    }

    /**
     * Closes the channel.
     * This method can be invoked asynchronously for interrupting a long reading process.
     *
     * @param  lock  ignored because this method can be run asynchronously.
     * @throws IOException if an error occurred while closing the channel.
     */
    @Override
    public void close(final DataStore lock) throws IOException {
        input.channel.close();
    }

    /**
     * Adds netCDF attributes to the given node, including variables attributes.
     * Variables attributes are shown first, and global attributes are last.
     *
     * @param  root  the node where to add netCDF attributes.
     */
    @Override
    public void addAttributesTo(final TreeTable.Node root) {
        for (final VariableInfo variable : variables) {
            final TreeTable.Node node = root.newChild();
            node.setValue(TableColumn.NAME, variable.getName());
            variable.addAttributesTo(node);
        }
        VariableInfo.addAttributesTo(root, attributeNames, attributeMap);
    }

    /**
     * Returns a string representation to be inserted in {@link org.apache.sis.storage.netcdf.NetcdfStore#toString()}
     * result. This is for debugging purpose only any may change in any future SIS version.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        final var buffer = new StringBuilder();
        buffer.append("SIS driver: “").append(getFilename()).append('”');
        if (!input.channel.isOpen()) {
            buffer.append(" (closed)");
        }
        return buffer.toString();
    }
}
