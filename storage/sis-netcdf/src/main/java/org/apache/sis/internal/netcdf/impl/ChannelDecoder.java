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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.lang.reflect.Array;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.GridGeometry;
import org.apache.sis.internal.netcdf.WarningProducer;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;


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
     * The encoding of dimension, variable and attribute names. This is fixed to {@value} by the
     * NetCDF specification. Note however that the encoding of attribute values may be different.
     *
     * @see #encoding
     * @see #readName()
     */
    private static final String NAME_ENCODING = "UTF-8";

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
     * A file identifier used only for formatting error message.
     */
    private final String filename;

    /**
     * The channel from where data are read.
     * This is supplied at construction time.
     */
    private final ReadableByteChannel channel;

    /**
     * The buffer to use for transferring data from the channel to memory, mostly at header reading time.
     * Must be backed by a Java {@code byte[]} array, because we will occasionally reference that array.
     */
    private final ByteBuffer buffer;

    /**
     * {@code false} if the file is the classic format, or
     * {@code true} if it is the 64-bits offset format.
     */
    private final boolean is64bits;

    /**
     * Number of records as an unsigned integer, or {@value #STREAMING} if unlimited.
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
     * The attributes found in the NetCDF file.
     */
    private final Map<String,Attribute> attributeMap;

    /**
     * Creates a new decoder for the given file.
     * This constructor parses immediately the header.
     *
     * @param  parent   Where to send the warnings, or {@code null} if none.
     * @param  filename A file identifier used only for formatting error message.
     * @param  channel  The channel from where data are read.
     * @throws IOException If an error occurred while reading the channel.
     * @throws DataStoreException If the content of the given channel is not a NetCDF file.
     */
    public ChannelDecoder(final WarningProducer parent, final String filename, final ReadableByteChannel channel)
            throws IOException, DataStoreException
    {
        super(parent);
        this.filename = filename;
        this.channel  = channel;
        this.buffer   = ByteBuffer.allocate(1024);
        channel.read(buffer);
        buffer.flip();
        /*
         * Check the magic number, which is expected to be exactly 3 bytes forming the "CDF" string.
         * The 4th byte is the version number, which we opportunistically use after the magic number check.
         */
        int version = readInt();
        if ((version & 0xFFFFFF00) != (('C' << 24) | ('D' << 16) | ('F' <<  8))) {
            throw new DataStoreException(Errors.format(Errors.Keys.UnexpectedFileFormat_2, "NetCDF", filename));
        }
        /*
         * Check the version number.
         */
        version &= 0xFF;
        switch (version) {
            case 1:  is64bits = false; break;
            case 2:  is64bits = true;  break;
            default: throw new DataStoreException(Errors.format(Errors.Keys.UnsupportedVersion_1, version));
        }
        numrecs = readInt();
        /*
         * Read the dimension, attribute and variable declarations. We expect exactly 3 lists,
         * where any of them can be flagged as absent by a long (64 bits) 0.
         */
        Dimension[]    dimensions = null;
        VariableInfo[] variables  = null;
        Attribute[]    attributes = null;
        for (int i=0; i<3; i++) {
            long nelems = readLong(); // Not yet the actual value (the 'tag' part is separated later).
            if (nelems != 0) {
                final int tag = (int) (nelems >>> Integer.SIZE);
                nelems &= 0xFFFFFFFFL;
                if (nelems > Integer.MAX_VALUE) {
                    throw new DataStoreException(Errors.format(Errors.Keys.ExcessiveListSize_2,
                            filename + DefaultNameSpace.DEFAULT_SEPARATOR + tagName(tag), nelems));
                }
                switch (tag) {
                    case DIMENSION: dimensions = readDimensions((int) nelems); break;
                    case VARIABLE:  variables  = readVariables ((int) nelems, dimensions); break;
                    case ATTRIBUTE: attributes = readAttributes((int) nelems); break;
                    default:        throw malformedHeader();
                }
            }
        }
        attributeMap = Attribute.toMap(attributes);
    }

    /**
     * Return the localized name of the given tag.
     *
     * @param  tag One of {@link #DIMENSION}, {@link #VARIABLE} or {@link #ATTRIBUTE} constants.
     * @return The localized name of the given tag, or its hexadecimal number if the given value is not
     *         one of the expected constants.
     */
    private static String tagName(final int tag) {
        final int key;
        switch (tag) {
            case DIMENSION: key = Vocabulary.Keys.Dimensions; break;
            case VARIABLE:  key = Vocabulary.Keys.Variables;  break;
            case ATTRIBUTE: key = Vocabulary.Keys.Attributes; break;
            default:        return Integer.toHexString(tag);
        }
        return Vocabulary.format(key);
    }

    /**
     * Returns an exception for a malformed header. This is used only after we have determined
     * that the file should be a NetCDF one, but we found some inconsistency or unknown tags.
     */
    private DataStoreException malformedHeader() {
        return new DataStoreException(Errors.format(Errors.Keys.CanNotParseFile_2, "NetCDF", filename));
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
    private int require(final int n, final int dataSize, final String name) throws IOException, DataStoreException {
        // (n+3) & ~3  is a trick for rounding 'n' to the next multiple of 4.
        final int size = (n * dataSize + 3) & ~3;
        if (size > buffer.capacity()) {
            throw new DataStoreException(Errors.format(Errors.Keys.ExcessiveListSize_2,
                    filename + DefaultNameSpace.DEFAULT_SEPARATOR + name, n));
        }
        require(size);
        return size;
    }

    /**
     * Makes sure that the buffer contains at least <var>n</var> remaining bytes.
     * It is caller's responsibility to ensure that the given number of bytes is
     * not greater than the buffer capacity.
     *
     * @param  n The minimal number of bytes needed in the {@linkplain #buffer}.
     * @throws EOFException If the channel has reached the end of stream.
     * @throws IOException If an other kind of error occurred while reading.
     */
    private void require(int n) throws IOException {
        assert n <= buffer.capacity() : n;
        n -= buffer.remaining();
        if (n > 0) {
            buffer.compact();
            do {
                final int c = channel.read(buffer);
                if (c < 0) {
                    throw new EOFException(Errors.format(Errors.Keys.UnexpectedEndOfFile_1, filename));
                }
                n -= c;
            } while (n > 0);
            buffer.flip();
        }
    }

    /**
     * Reads the next integer value from the buffer.
     */
    private int readInt() throws IOException {
        require(Integer.SIZE / Byte.SIZE);
        return buffer.getInt();
    }

    /**
     * Reads the next long value from the buffer.
     */
    private long readLong() throws IOException {
        require(Long.SIZE / Byte.SIZE);
        return buffer.getLong();
    }

    /**
     * Reads the next offset, which may be encoded on 32 or 64 bits depending on the file format.
     */
    private long readOffset() throws IOException {
        return is64bits ? readLong() : (readInt() & 0xFFFFFFFFL);
    }

    /**
     * Reads a string from the buffer in the {@value #NAME_ENCODING}. This is suitable for the dimension,
     * variable and attribute names in the header. Note that attribute value may have a different encoding.
     */
    private String readName() throws IOException, DataStoreException {
        final int length = readInt();
        if (length < 0) {
            throw malformedHeader();
        }
        final int size = require(length, 1, "<name>");
        final int position = buffer.position(); // Must be after 'require'
        final String text = new String(buffer.array(), position, length, NAME_ENCODING);
        buffer.position(position + size);
        return text;
    }

    /**
     * Returns the values of the given type. In the given type is {@code CHAR}, then this method returns the values
     * as a {@link String}. Otherwise this method returns the value as an array of the corresponding primitive type
     * and the given length.
     */
    private Object readValues(final String name, final int type, final int length) throws IOException, DataStoreException {
        final int size = require(length, VariableInfo.sizeOf(type), name);
        final int position = buffer.position(); // Must be after 'require'
        final Object result;
        switch (type) {
            case VariableInfo.CHAR: {
                final String text = new String(buffer.array(), position, length, encoding).trim();
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
     */
    private Dimension[] readDimensions(final int nelems) throws IOException, DataStoreException {
        final Dimension[] dimensions = new Dimension[nelems];
        for (int i=0; i<nelems; i++) {
            dimensions[i] = new Dimension(readName(), readInt());
        }
        return dimensions;
    }

    /**
     * Reads attribute values from the NetCDF file header. The record structure is:
     *
     * <ul>
     *   <li>The attribute name                             (use {@link #readName()})</li>
     *   <li>The attribute type (BYTE, SHORT, …)            (use {@link #readInt()})</li>
     *   <li>The number of values of the above type         (use {@link #readInt()})</li>
     *   <li>The actual values as a variable length list    (use {@link #readValues(String,int,int)})</li>
     * </ul>
     *
     * @param nelems The number of attributes to read.
     */
    private Attribute[] readAttributes(final int nelems) throws IOException, DataStoreException {
        final Attribute[] attributes = new Attribute[nelems];
        for (int i=0; i<nelems; i++) {
            final String name = readName();
            attributes[i] = new Attribute(name, readValues(name, readInt(), readInt()));
        }
        return attributes;
    }

    /**
     * Reads information (not data) about all variables from the NetCDF file header.
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
            final int n = readInt();
            final Dimension[] varDims = new Dimension[n];
            try {
                for (int i=0; i<n; i++) {
                    varDims[i] = dimensions[readInt()];
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
            long na = readLong();
            if (na != 0) {
                final int tag = (int) (na >>> Integer.SIZE);
                na &= 0xFFFFFFFFL;
                if (na > Integer.MAX_VALUE) {
                    throw new DataStoreException(Errors.format(Errors.Keys.ExcessiveListSize_2,
                            filename + DefaultNameSpace.DEFAULT_SEPARATOR + tagName(tag), na));
                }
                switch (tag) {
                    // More cases may be added later if it appears to exist.
                    case ATTRIBUTE: attributes = readAttributes((int) na); break;
                    default:        throw malformedHeader();
                }
            }
            variables[j] = new VariableInfo(name, varDims, attributes, readInt(), readInt(), readOffset());
        }
        return variables;
    }



    // --------------------------------------------------------------------------------------------
    //  Decoder API begins below this point. Above code was specific to parsing of NetCDF header.
    // --------------------------------------------------------------------------------------------

    /**
     * Defines the groups where to search for named attributes, in preference order.
     * The {@code null} group name stands for the global attributes.
     */
    @Override
    public void setSearchPath(final String... groupNames) throws IOException {
        if (groupNames.length != 1 || groupNames[0] != null) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns the path which is currently set. The array returned by this method may be only
     * a subset of the array given to {@link #setSearchPath(String[])} since only the name of
     * groups which have been found in the NetCDF file are returned by this method.
     */
    @Override
    public String[] getSearchPath() throws IOException {
        return new String[1];
    }

    /**
     * Returns the NetCDF attribute of the given name, or {@code null} if none.
     * The {@code name} argument is typically (but is not restricted too) one of
     * the constants defined in the {@link AttributeNames} class.
     *
     * @param  name The name of the attribute to search, or {@code null}.
     * @return The attribute, or {@code null} if none.
     */
    private Attribute findAttribute(final String name) {
        return attributeMap.get(name);
    }

    /**
     * Returns the value for the attribute of the given name, or {@code null} if none.
     *
     * @param  name The name of the attribute to search, or {@code null}.
     * @return The attribute value, or {@code null} if none or empty or if the given name was null.
     */
    @Override
    public String stringValue(final String name) throws IOException {
        final Attribute attribute = findAttribute(name);
        if (attribute != null && attribute.value != null) {
            return attribute.value.toString();
        }
        return null;
    }

    /**
     * Returns the value of the attribute of the given name as a number, or {@code null} if none.
     * If there is more than one numeric value, only the first one is returned.
     */
    @Override
    public Number numericValue(final String name) throws IOException {
        final Attribute attribute = findAttribute(name);
        if (attribute != null && attribute.value != null) {
            if (attribute.value instanceof String) {
                return parseNumber((String) attribute.value);
            }
            if (Array.getLength(attribute.value) != 0) {
                return (Number) Array.get(attribute.value, 0);
            }
        }
        return null;
    }

    @Override
    public Date dateValue(String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date[] numberToDate(String symbol, Number... values) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Variable> getVariables() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<GridGeometry> getGridGeometries() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Closes the channel.
     *
     * @throws IOException If an error occurred while closing the channel.
     */
    @Override
    public void close() throws IOException {
        channel.close();
    }
}
