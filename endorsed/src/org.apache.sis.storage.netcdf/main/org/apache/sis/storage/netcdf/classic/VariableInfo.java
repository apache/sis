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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.format.DateTimeParseException;
import ucar.nc2.constants.CF;       // String constants are copied by the compiler with no UCAR reference left.
import ucar.nc2.constants.CDM;      // idem
import ucar.nc2.constants._Coordinate;
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.netcdf.AttributeNames;
import org.apache.sis.storage.netcdf.base.Decoder;
import org.apache.sis.storage.netcdf.base.DataType;
import org.apache.sis.storage.netcdf.base.Dimension;
import org.apache.sis.storage.netcdf.base.Grid;
import org.apache.sis.storage.netcdf.base.Variable;
import org.apache.sis.storage.netcdf.base.GridAdjustment;
import org.apache.sis.storage.netcdf.internal.Resources;
import org.apache.sis.storage.base.StoreUtilities;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.HyperRectangleReader;
import org.apache.sis.io.stream.Region;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Classes;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.measure.Units;
import org.apache.sis.math.Vector;


/**
 * Description of a variable found in a netCDF file.
 * The natural ordering of {@code VariableInfo} is the order in which the variables appear in the stream of bytes
 * that makes the netCDF file. Reading variables in natural order reduces the number of channel seek operations.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class VariableInfo extends Variable implements Comparable<VariableInfo> {
    /**
     * The names of attributes where to look for the description to be returned by {@link #getDescription()}.
     * We use the same attributes as the one documented in the {@link ucar.nc2.Variable#getDescription()}
     * javadoc, in same order.
     */
    private static final String[] DESCRIPTION_ATTRIBUTES = {
        CDM.LONG_NAME,
        CDM.DESCRIPTION,
        CDM.TITLE,
        CF.STANDARD_NAME
    };

    /**
     * Helper class for reading a sub-area with a subsampling,
     * or {@code null} if {@code dataType} is not a supported type.
     */
    private final HyperRectangleReader reader;

    /**
     * The variable name.
     *
     * @see #getName()
     */
    private final String name;

    /**
     * The dimensions of this variable, in the order they appear in netCDF file. When iterating over the values stored in
     * this variable (a flattened one-dimensional sequence of values), index in the domain of {@code dimensions[length-1]}
     * varies faster, followed by index in the domain of {@code dimensions[length-2]}, <i>etc.</i>
     *
     * @see #getGridDimensions()
     * @see GridInfo#domain
     */
    final DimensionInfo[] dimensions;

    /**
     * The offset (in bytes) to apply for moving to the next record in variable data, or -1 if unknown.
     * A record is a <var>n-1</var> dimensional cube spanning all dimensions except the unlimited one.
     * This concept is relevant only for {@linkplain #isUnlimited() unlimited variables}.
     * For other kind of variables, this field is ignored and its value can be arbitrary.
     *
     * <p>On construction, this field is initialized to the number of bytes in this variable, omitting
     * the unlimited dimension (if any), or 0 if unknown. After {@link #complete(VariableInfo[])} has
     * been invoked, this field is modified as below:</p>
     *
     * <ul>
     *   <li>For {@linkplain #isUnlimited() unlimited variables}, this is the number of bytes to
     *       skip <strong>after</strong> the data has been read for reading the next record.</li>
     *   <li>For other variables, this field is ignored. In current implementation it is left to
     *       the size of this variable, and kept for information purpose only.</li>
     * </ul>
     */
    private long offsetToNextRecord;

    /**
     * The attributes associates to the variable, or an empty map if none.
     * Values can be:
     *
     * <ul>
     *   <li>{@link String} if the attribute contains a single textual value.</li>
     *   <li>{@link Number} if the attribute contains a single numerical value.</li>
     *   <li>{@link Vector} if the attribute contains many numerical values.</li>
     *   <li>{@code String[]} if the attribute is one of predefined attributes
     *       for which many text values are expected (e.g. an enumeration).</li>
     * </ul>
     *
     * If the value is a {@code String}, then leading and trailing spaces and control characters
     * should be trimmed by {@link String#trim()}.
     */
    private final Map<String,Object> attributes;

    /**
     * Names of attributes. This is {@code attributeMap.keySet()} unless some attributes have a name
     * containing upper case letters. In such case a separated set is used for avoiding duplicated
     * names (the name with upper case letters + the name in all lower case letters).
     *
     * @see #getAttributeNames()
     */
    private final Set<String> attributeNames;

    /**
     * The netCDF type of data, or {@code null} if unknown.
     *
     * @see #getDataType()
     */
    private final DataType dataType;

    /**
     * The grid geometry associated to this variable, computed by {@link ChannelDecoder#getGridCandidates()} when first needed.
     * May stay {@code null} if the variable is not a data cube. We do not need disambiguation between the case where
     * the grid has not yet been computed and the case where the computation has been done with {@code null} result,
     * because {@link #findGrid(GridAdjustment)} should be invoked only once per variable.
     *
     * @see #findGrid(GridAdjustment)
     */
    GridInfo grid;

    /**
     * {@code true} if this variable seems to be a coordinate system axis, as determined by comparing its name
     * with the name of all dimensions in the netCDF file. This information is computed at construction time
     * because requested more than once.
     */
    boolean isCoordinateSystemAxis;

    /**
     * Creates a new variable.
     *
     * @param  decoder     the netCDF file where this variable is stored.
     * @param  input       the channel together with a buffer for reading the variable data.
     * @param  name        the variable name.
     * @param  dimensions  the dimensions of this variable.
     * @param  attributes  the attributes associates to the variable, or an empty map if none.
     * @param  dataType    the netCDF type of data, or {@code null} if unknown.
     * @param  size        the variable size. May be inaccurate and ignored.
     * @param  offset      the offset where the variable data begins in the netCDF file.
     * @throws ArithmeticException if the variable size exceeds {@link Long#MAX_VALUE}.
     * @throws DataStoreContentException if a logical error is detected.
     */
    VariableInfo(final Decoder            decoder,
                 final ChannelDataInput   input,
                 final String             name,
                 final DimensionInfo[]    dimensions,
                 final Map<String,Object> attributes,
                 final Set<String>        attributeNames,
                       DataType           dataType,
                 final int                size,
                 final long               offset) throws DataStoreContentException
    {
        super(decoder);
        this.name           = name;
        this.dimensions     = dimensions;
        this.attributes     = attributes;
        this.attributeNames = attributeNames;
        final Object isUnsigned = getAttributeValue(CDM.UNSIGNED, "_unsigned");
        if (isUnsigned instanceof String) {
            dataType = dataType.unsigned(Boolean.parseBoolean((String) isUnsigned));
        }
        this.dataType = dataType;
        /*
         * The `size` value is provided in the netCDF files, but does not need to be stored because it can
         * be computed from dimension lengths and its type is not wide enough for large variables anyway.
         * Instead, we compute the length ourselves, excluding the unlimited dimension.
         */
        if (dataType != null && (offsetToNextRecord = dataType.size()) != 0) {
            for (int i=0; i<dimensions.length; i++) {
                final DimensionInfo dim = dimensions[i];
                if (!dim.isUnlimited) {
                    offsetToNextRecord = Math.multiplyExact(offsetToNextRecord, dim.length());
                } else if (i != 0) {
                    // Unlimited dimension, if any, must be first in a netCDF 3 classic format.
                    throw new DataStoreContentException(decoder.getLocale(), Decoder.FORMAT_NAME, input.filename, null);
                }
            }
            reader = new HyperRectangleReader(dataType.number, input);
            reader.setOrigin(offset);
        } else {
            reader = null;
        }
        /*
         * If the value that we computed ourselves does not match the value declared in the netCDF file,
         * maybe for some reason the writer used a different layout. For example, maybe it inserted some
         * additional padding.
         */
        if (size != -1) {                           // Maximal unsigned value, means possible overflow.
            final long expected = paddedSize();
            final long actual = Integer.toUnsignedLong(size);
            if (actual != expected) {
                if (expected != 0) {
                    warning(ChannelDecoder.class, "readVariables", null,      // Caller of this constructor.
                            Resources.Keys.MismatchedVariableSize_3, getFilename(), name, actual - expected);
                }
                if (actual > offsetToNextRecord) {
                    offsetToNextRecord = actual;
                }
            }
        }
        /*
         * According CF conventions, a variable is considered a coordinate system axis if it has the same name
         * as its dimension. But the "_CoordinateAxisType" attribute is often used for making explicit that a
         * variable is an axis. We check that case before to check variable name.
         */
        if (dimensions.length == 1 || dimensions.length == 2) {
            isCoordinateSystemAxis = (getAxisType() != null);
            if (!isCoordinateSystemAxis && (dimensions.length == 1 || dataType == DataType.CHAR)) {
                /*
                 * If the "_CoordinateAliasForDimension" attribute is defined, then its value will be used
                 * instead of the variable name when determining if the variable is a coordinate system axis.
                 * "_CoordinateVariableAlias" seems to be a legacy attribute name for the same purpose.
                 */
                Object value = getAttributeValue(_Coordinate.AliasForDimension, "_coordinatealiasfordimension");
                if (value == null) {
                    value = getAttributeValue("_CoordinateVariableAlias", "_coordinatevariablealias");
                    if (value == null) {
                        value = name;
                    }
                }
                isCoordinateSystemAxis = dimensions[0].name.equals(value);
            }
        }
        /*
         * Rewrite the enumeration names as an array for avoiding to parse the string if this information
         * is asked twice (e.g. in `setEnumeration(…)` and in `MetadataReader`). Note that there is no need
         * to perform similar operation for vectors of numbers.
         */
        split(AttributeNames.FLAG_NAMES);
        split(AttributeNames.FLAG_MEANINGS);
        setEnumeration(null);
    }

    /**
     * Splits a space-separated attribute value into an array of strings.
     * If the attribute is a list of numbers, it will be left unchanged.
     * (should not happen, but we are paranoiac)
     */
    private void split(final String attributeName) {
        final CharSequence[] values = getAttributeAsStrings(attributeName, ' ');
        if (values != null) {
            final Object previous = attributes.put(attributeName, values);
            if (previous instanceof Vector) {
                attributes.put(attributeName, previous);
            }
        }
    }

    /**
     * Returns the value of {@link #offsetToNextRecord} with padding applied. This value is true
     * only if this method is invoked <strong>before</strong> {@link #complete(VariableInfo[])}.
     */
    private long paddedSize() {
        return Math.addExact(offsetToNextRecord, Integer.BYTES - 1) & ~(Integer.BYTES - 1);
    }

    /**
     * Performs the final adjustment of the {@link #offsetToNextRecord} field of all the given variables.
     * This method applies padding except for the special case documented in netCDF specification:
     * <q>In the special case when there is only one {@linkplain #isUnlimited() record variable}
     * and it is of type character, byte, or short, no padding is used between record slabs,
     * so records after the first record do not necessarily start on four-byte boundaries</q>.
     *
     * <p>After padding has been applied, this method set the {@link #offsetToNextRecord} of all unlimited
     * variables to the number of bytes to skip before reading the next record.</p>
     *
     * @throws ArithmeticException if the stride between two records exceeds {@link Long#MAX_VALUE}.
     */
    static void complete(final VariableInfo[] variables) {
        final Set<CharSequence> referencedAsAxis = new HashSet<>();
        final VariableInfo[] unlimited = new VariableInfo[variables.length];
        int     count        = 0;               // Number of valid elements in the `unlimited` array.
        long    recordStride = 0;               // Sum of the size of all variables having a unlimited dimension.
        boolean isUnknown    = false;           // True if `total` is actually unknown.
        for (final VariableInfo variable : variables) {
            // Opportunistically store names of all axes listed in "coordinates" attributes of all variables.
            Collections.addAll(referencedAsAxis, variable.getCoordinateVariables());
            if (variable.isUnlimited()) {
                final long paddedSize = variable.paddedSize();
                unlimited[count++] = variable;
                isUnknown |= (paddedSize == 0);
                recordStride = Math.addExact(recordStride, paddedSize);
            }
        }
        if (isUnknown) {
            // If at least one unlimited variable has unknown size, the whole record stride is unknown.
            for (int i=0; i<count; i++) {
                unlimited[i].offsetToNextRecord = -1;
            }
        } else if (count == 1) {
            unlimited[0].offsetToNextRecord = 0;        // Special case cited in method javadoc.
        } else for (int i=0; i<count; i++) {
            unlimited[i].offsetToNextRecord = Math.subtractExact(recordStride, unlimited[i].offsetToNextRecord);
        }
        /*
         * If some variables have a "coordinates" attribute listing names of variables used as axes,
         * mark those variables as axes. We perform this check as a complement for the check done in
         * the constructor in order to detect two-dimensional axes. Example:
         *
         *    dimensions:
         *        Y = 471 ;
         *        X = 720 ;
         *    variables:
         *        float lon(Y, X) ;                       // Not detected as coordinate axis by the constructor.
         *            lon:long_name = "longitude" ;
         *            lon:units = "degree_east" ;
         *        float lat(Y, X) ;                       // Not detected as coordinate axis by the constructor.
         *            lat:long_name = "latitude" ;
         *            lat:units = "degree_north" ;
         *        short temperature(Y, X) ;
         *            temperature:long_name = "Potential Temperature" ;
         *            temperature:coordinates = "lon lat" ;
         */
        if (!referencedAsAxis.isEmpty()) {
            for (final VariableInfo variable : variables) {
                if (referencedAsAxis.remove(variable.name)) {
                    variable.isCoordinateSystemAxis = true;
                    if (referencedAsAxis.isEmpty()) break;
                }
            }
        }
    }

    /**
     * Returns the name of the netCDF file containing this variable, or {@code null} if unknown.
     */
    @Override
    public String getFilename() {
        if (reader != null) {
            final String filename = reader.filename();
            if (filename != null) return filename;
        }
        return super.getFilename();
    }

    /**
     * Returns the name of this variable.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the description of this variable, or {@code null} if none.
     * This method searches for the first attribute named {@code "long_name"},
     * {@code "description"}, {@code "title"} or {@code "standard_name"}.
     */
    @Override
    public String getDescription() {
        for (final String attributeName : DESCRIPTION_ATTRIBUTES) {
            final String value = getAttributeAsString(attributeName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Returns the unit of measurement as a string, or {@code null} if none.
     */
    @Override
    protected String getUnitsString() {
        return getAttributeAsString(CDM.UNITS);
    }

    /**
     * Parses the given unit symbol and set the {@link #epoch} if the parsed unit is a temporal unit.
     * This method is called by {@link #getUnit()}.
     *
     * @throws MeasurementParseException if the given symbol cannot be parsed.
     */
    @Override
    protected Unit<?> parseUnit(String symbols) {
        final Matcher parts = TIME_UNIT_PATTERN.matcher(symbols);
        DateTimeParseException dateError = null;
        if (parts.matches()) {
            /*
             * If we enter in this block, the unit is of the form "days since 1970-01-01 00:00:00".
             * The TIME_PATTERN splits the string in two parts, "days" and "1970-01-01 00:00:00".
             * The parse method will replace the space between date and time by 'T' letter.
             */
            try {
                epoch = LenientDateFormat.parseInstantUTC(parts.group(2));
            } catch (DateTimeParseException e) {
                dateError = e;
            }
            symbols = parts.group(1);
        }
        /*
         * Parse the unit symbol after removing the "since 1970-01-01 00:00:00" part of the text,
         * even if we failed to parse the date. We need to be tolerant regarding the date because
         * sometimes the text looks like "hours since analysis".
         */
        final Unit<?> unit;
        try {
            unit = Units.valueOf(symbols);
        } catch (MeasurementParseException e) {
            if (dateError != null) {
                e.addSuppressed(dateError);
            }
            throw e;
        }
        /*
         * Log the warning about date format only if the rest of this method succeeded.
         * We report `getUnit()` as the source method because it is the public caller.
         */
        if (dateError != null) {
            error(Variable.class, "getUnit", dateError, Errors.Keys.CanNotAssignUnitToVariable_2, getName(), symbols);
        }
        return unit;
    }

    /**
     * Returns the type of data, or {@code UNKNOWN} if the data type is unknown to this method.
     * If this variable has a {@code "_Unsigned = true"} attribute, then the returned data type
     * will be a unsigned variant.
     */
    @Override
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Returns whether this variable can grow. A variable is unlimited if at least one of its dimension is unlimited.
     * In netCDF 3 classic format, only the first dimension can be unlimited.
     */
    @Override
    protected boolean isUnlimited() {
        // The constructor verified that only the first dimension is unlimited.
        return (dimensions.length != 0) && dimensions[0].isUnlimited;
    }

    /**
     * Returns {@code true} if this variable seems to be a coordinate system axis,
     * determined by comparing its name with the name of all dimensions in the netCDF file.
     * Also determined by inspection of {@code "coordinates"} attribute on other variables.
     */
    @Override
    protected boolean isCoordinateSystemAxis() {
        return isCoordinateSystemAxis;
    }

    /**
     * Returns the value of the {@code "_CoordinateAxisType"} or {@code "axis"} attribute, or {@code null} if none.
     * Note that a {@code null} value does not mean that this variable is not an axis.
     */
    @Override
    protected String getAxisType() {
        Object value = getAttributeValue(_Coordinate.AxisType, "_coordinateaxistype");
        if (value == null) {
            value = getAttributeValue(CF.AXIS);
        }
        return (value != null) ? value.toString() : null;
    }

    /**
     * Returns the names of variables to use as axes for this variable, or an empty array if none.
     */
    final CharSequence[] getCoordinateVariables() {
        return CharSequences.split(getAttributeAsString(CF.COORDINATES), ' ');
    }

    /**
     * Returns a builder for the grid geometry of this variable, or {@code null} if this variable is not a data cube.
     * The grid geometry builders are opportunistically cached in {@code VariableInfo} instances after they have been
     * computed by {@link ChannelDecoder#getGridCandidates()}. This method delegates to the super-class method only
     * if the grid requires more analysis than the one performed by {@link ChannelDecoder}.
     *
     * @see ChannelDecoder#getGridCandidates()
     */
    @Override
    protected Grid findGrid(final GridAdjustment adjustment) throws IOException, DataStoreException {
        if (grid == null) {
            decoder.getGridCandidates();                      // Force calculation of grid geometries if not already done.
            if (grid == null) {                               // May have been computed as a side-effect of getGridCandidates().
                grid = (GridInfo) super.findGrid(adjustment); // Non-null if grid dimensions are different than this variable.
            }
        }
        return grid;
    }

    /**
     * Returns the number of grid dimensions. This is the size of the {@link #getGridDimensions()} list.
     *
     * @return number of grid dimensions.
     */
    @Override
    public int getNumDimensions() {
        return dimensions.length;
    }

    /**
     * Returns the dimensions of this variable in the order they are declared in the netCDF file.
     * The dimensions are those of the grid, not the dimensions (or axes) of the coordinate system.
     * In ISO 19123 terminology, the {@linkplain Dimension#length() dimension lengths} give the upper
     * corner of the grid envelope plus one. The lower corner is always (0, 0, …, 0).
     *
     * @see #getNumDimensions()
     */
    @Override
    public List<Dimension> getGridDimensions() {
        return UnmodifiableArrayList.wrap(dimensions);
    }

    /**
     * Returns the names of all attributes associated to this variable.
     * The returned set is unmodifiable.
     *
     * @return names of all attributes associated to this variable.
     */
    @Override
    public Collection<String> getAttributeNames() {
        return Collections.unmodifiableSet(attributeNames);
    }

    /**
     * Returns the type of the attribute of the given name, or {@code null} if the given attribute is not found.
     * If the attribute contains more than one value, then this method returns a {@code Vector.class} subtype.
     *
     * @param  attributeName  the name of the attribute for which to get the type, preferably in lowercase.
     * @return type of the given attribute, or {@code null} if the attribute does not exist.
     */
    @Override
    public Class<?> getAttributeType(final String attributeName) {
        return Classes.getClass(getAttributeValue(attributeName));
    }

    /**
     * Returns the value of the given attribute, or {@code null} if none.
     * This method should be invoked only for hard-coded names that mix lower-case and upper-case letters.
     *
     * @param  attributeName  name of attribute to search, in the expected case.
     * @param  lowerCase      the all lower-case variant of {@code attributeName}.
     * @return variable attribute value of the given name, or {@code null} if none.
     */
    private Object getAttributeValue(final String attributeName, final String lowerCase) {
        Object value = getAttributeValue(attributeName);
        if (value == null) {
            value = attributes.get(lowerCase);
        }
        return value;
    }

    /**
     * Returns the value of the given attribute, or {@code null} if none.
     * This method does not search the lower-case variant of the given name because the argument given to this method
     * is usually a hard-coded value from {@link CF} or {@link CDM} conventions, which are already in lower-cases.
     *
     * <p>All other {@code getAttributeFoo(…)} methods in this class or parent class ultimately invoke this method.
     * This provides a single point to override if the functionality needs to be extended.</p>
     *
     * @param  attributeName  name of attribute to search, in the expected case.
     * @return variable attribute value of the given name, or {@code null} if none.
     */
    @Override
    protected Object getAttributeValue(final String attributeName) {
        return attributes.get(attributeName);
    }

    /**
     * Adds the attributes of this variable to the given node. This is used for building the tree returned by
     * {@link org.apache.sis.storage.netcdf.NetcdfStore#getNativeMetadata()}. For information purpose only.
     *
     * @param  branch  where to add new nodes for the attributes of this variable.
     */
    final void addAttributesTo(final TreeTable.Node branch) {
        addAttributesTo(branch, attributeNames, attributes);
    }

    /**
     * Adds the given attributes to the given node. This is used for building the tree
     * returned by {@link org.apache.sis.storage.netcdf.NetcdfStore#getNativeMetadata()}.
     * This tree is for information purpose only.
     *
     * @param  branch          where to add new nodes for the given attributes.
     * @param  attributeNames  name of attribute to add to the specified branch.
     * @param  attributes      the attributes to add to the specified branch.
     */
    static void addAttributesTo(final TreeTable.Node branch,
            final Set<String> attributeNames, final Map<String,Object> attributes)
    {
        for (final String name : attributeNames) {
            final TreeTable.Node node = branch.newChild();
            node.setValue(TableColumn.NAME, name);
            Object value = attributes.get(name);
            if (value != null) {
                if (value instanceof Vector) {
                    value = ((Vector) value).toArray();
                }
                node.setValue(TableColumn.VALUE, value);
            }
        }
    }

    /**
     * Reads all the data for this variable and returns them as an array of a Java primitive type.
     * Multi-dimensional variables are flattened as a one-dimensional array (wrapped in a vector).
     * Fill values/missing values are replaced by NaN if {@link #hasRealValues()} is {@code true}.
     *
     * @throws ArithmeticException if the size of the variable exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     *
     * @see #read()
     */
    @Override
    protected Object readFully() throws IOException, DataStoreException {
        return readArray(null, null);
    }

    /**
     * Reads a subsampled sub-area of the variable.
     * Multi-dimensional variables are flattened as a one-dimensional array (wrapped in a vector).
     * Array elements are in "natural" order (inverse of netCDF order).
     *
     * @param  area         indices of cell values to read along each dimension, in "natural" order.
     * @param  subsampling  subsampling along each dimension. 1 means no subsampling.
     * @return the data as an array of a Java primitive type.
     * @throws ArithmeticException if the size of the region to read exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    @Override
    public Vector read(final GridExtent area, final long[] subsampling) throws IOException, DataStoreException {
        return Vector.create(readArray(area, subsampling), dataType.isUnsigned);
    }

    /**
     * Reads a subsampled sub-area of the variable and returns them as a list of any object.
     * Elements in the returned list may be {@link Number} or {@link String} instances.
     *
     * @param  area         indices of cell values to read along each dimension, in "natural" order.
     * @param  subsampling  subsampling along each dimension, or {@code null} if none.
     * @return the data as a list of {@link Number} or {@link String} instances.
     */
    @Override
    public List<?> readAnyType(final GridExtent area, final long[] subsampling) throws IOException, DataStoreException {
        final Object array = readArray(area, subsampling);
        if (dataType == DataType.CHAR && dimensions.length >= STRING_DIMENSION) {
            return createStringList(array, area);
        }
        return Vector.create(array, dataType.isUnsigned);
    }

    /**
     * Reads the data from this variable and returns them as an array of a Java primitive type.
     * Multi-dimensional variables are flattened as a one-dimensional array (wrapped in a vector).
     * Fill values/missing values are replaced by NaN if {@link #hasRealValues()} is {@code true}.
     * Array elements are in "natural" order (inverse of netCDF order).
     *
     * @param  area         indices (in "natural" order) of cell values to read, or {@code null} for whole variable.
     * @param  subsampling  subsampling along each dimension, or {@code null} if none. Ignored if {@code area} is null.
     * @return the data as an array of a Java primitive type.
     * @throws ArithmeticException if the size of the variable exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     *
     * @see #read()
     * @see #read(GridExtent, long[])
     */
    private Object readArray(final GridExtent area, long[] subsampling) throws IOException, DataStoreException {
        if (reader == null) {
            throw new DataStoreContentException(unknownType());
        }
        final int dimension = dimensions.length;
        final long[] lower  = new long[dimension];
        final long[] upper  = new long[dimension];
        final long[] size   = (area != null) ? new long[dimension] : upper;
        /*
         * NetCDF sorts datas in reverse dimension order. Example:
         *
         * DIMENSIONS:
         *   time: 3
         *   lat : 2
         *   lon : 4
         *
         * VARIABLES:
         *   temperature (time,lat,lon)
         *
         * DATA INDICES:
         *   (0,0,0) (0,0,1) (0,0,2) (0,0,3)
         *   (0,1,0) (0,1,1) (0,1,2) (0,1,3)
         *   (1,0,0) (1,0,1) (1,0,2) (1,0,3)
         *   (1,1,0) (1,1,1) (1,1,2) (1,1,3)
         *   (2,0,0) (2,0,1) (2,0,2) (2,0,3)
         *   (2,1,0) (2,1,1) (2,1,2) (2,1,3)
         */
        for (int i=0; i<dimension; i++) {
            size[i] = dimensions[(dimension - 1) - i].length();
            if (area != null) {
                lower[i] = area.getLow(i);
                upper[i] = Math.incrementExact(area.getHigh(i));
            }
        }
        if (subsampling == null) {
            subsampling = new long[dimension];
            Arrays.fill(subsampling, 1);
        }
        final var region = new Region(size, lower, upper, subsampling);
        /*
         * If this variable uses the unlimited dimension, we have to skip the records of all other unlimited variables
         * before to reach the next record of this variable.  Current implementation can do that only if the number of
         * bytes to skip is a multiple of the data type size. It should be the case most of the time because variables
         * in netCDF files have a 4 bytes padding. It may not work however if the variable uses {@code long} or
         * {@code double} type.
         */
        if (isUnlimited()) {
            if (offsetToNextRecord < 0) {
                throw canNotComputePosition(null);
            }
            region.setAdditionalByteOffset(dimensions.length - 1, offsetToNextRecord);
        }
        Object array = reader.read(region);
        replaceNaN(array);
        if (area == null && array instanceof double[]) {
            /*
             * If we can convert a double[] array to a float[] array, we should do that before
             * to invoke `setValues(array)` - we cannot rely on data.compress(tolerance). The
             * reason is because we assume that float[] arrays are accurate in base 10 even if
             * the data were originally stored as doubles. The Vector class does not make such
             * assumption since it is specific to what we observe with netCDF files. To enable
             * this assumption, we need to convert to float[] before createDecimalVector(…).
             */
            final float[] copy = ArraysExt.copyAsFloatsIfLossless((double[]) array);
            if (copy != null) array = copy;
        }
        return array;
    }

    /**
     * Creates an array of character strings from a "two-dimensional" array of bytes stored in a flat array.
     * For each element, leading and trailing spaces and control codes are trimmed.
     * The array does not contain null element but may contain empty strings.
     *
     * @param  array     the "two-dimensional" array of characters stored in a flat {@code byte[]} array.
     * @param  count     number of string elements (size of first dimension).
     * @param  length    number of characters in each element (size of second dimension).
     * @return array of character strings.
     */
    @Override
    protected String[] createStringArray(final Object array, final int count, final int length) {
        final byte[] chars = (byte[]) array;
        final Charset encoding = ((ChannelDecoder) decoder).getEncoding();
        final String[] strings = new String[count];
        int lower = 0;
        String previous = "";                       // For sharing same `String` instances when same value is repeated.
        if (StoreUtilities.basedOnASCII(encoding)) {
            int plo = 0, phi = 0;                   // Index range of bytes used for building the previous string.
            for (int i=0; i<count; i++) {
                String element = "";
                final int upper = lower + length;
                for (int j=upper; --j >= lower;) {
                    if (Byte.toUnsignedInt(chars[j]) > ' ') {
                        while (Byte.toUnsignedInt(chars[lower]) <= ' ') lower++;
                        if (Arrays.equals(chars, lower, ++j, chars, plo, phi)) {
                            element = previous;
                        } else {
                            element  = new String(chars, lower, j - lower, encoding);
                            previous = element;
                            plo      = lower;
                            phi      = j;
                        }
                        break;
                    }
                }
                strings[i] = element;
                lower = upper;
            }
        } else {
            for (int i=0; i<count; i++) {
                final String element = new String(chars, lower, length, encoding).trim();
                if (!previous.equals(element)) previous = element;
                strings[i] = previous;
                lower += length;
            }
        }
        return strings;
    }

    /**
     * Returns a coordinate for this two-dimensional grid coordinate axis.
     * This is (indirectly) a callback method for {@link Grid#getAxes(Decoder)}.
     *
     * @throws ArithmeticException if the axis size exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    @Override
    protected double coordinateForAxis(final int j, final int i) throws IOException, DataStoreException {
        assert j >= 0 && j < dimensions[0].length : j;
        assert i >= 0 && i < dimensions[1].length : i;
        final long n = dimensions[1].length();
        return read().doubleValue(Math.toIntExact(i + n*j));
    }

    /**
     * Returns the error message for an unknown data type.
     */
    private String unknownType() {
        return decoder.resources().getString(Resources.Keys.UnsupportedDataType_3, getFilename(), name, dataType);
    }

    /**
     * Returns -1 if this variable is located before the other variable in the stream of bytes that make
     * the netCDF file, or +1 if it is located after.
     */
    @Override
    public int compareTo(final VariableInfo other) {
        int c = Long.compare(reader.getOrigin(), other.reader.getOrigin());
        if (c == 0) c = name.compareTo(other.name);                 // Should not happen, but we are paranoiac.
        return c;
    }

    /*
     * Do not override `Object.equals(Object)` and `Object.hashCode()`,
     * because variables are used as keys by `GridMapping.forVariable(…)`.
     */
}
