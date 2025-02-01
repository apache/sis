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
package org.apache.sis.storage.netcdf.ucar;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.io.File;
import java.io.IOException;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Group;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.EnhanceScaleMissingUnsigned;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateUnit;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import javax.measure.Unit;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.math.Vector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.netcdf.base.DataType;
import org.apache.sis.storage.netcdf.base.Decoder;
import org.apache.sis.storage.netcdf.base.Grid;
import org.apache.sis.storage.netcdf.base.GridAdjustment;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.pending.jdk.JDK19;


/**
 * A {@link org.apache.sis.storage.netcdf.base.Variable} backed by the UCAR netCDF library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
final class VariableWrapper extends org.apache.sis.storage.netcdf.base.Variable {
    /**
     * The netCDF variable. This is typically an instance of {@link VariableEnhanced}.
     */
    private final Variable variable;

    /**
     * The variable without enhancements. May be the same instance as {@link #variable}
     * if that variable was not enhanced. This field is preferred to {@code variable} for
     * fetching attribute values because the {@code "scale_factor"} and {@code "add_offset"}
     * attributes are hidden by {@link VariableEnhanced}. In order to allow metadata reader
     * to find them, we query attributes in the original variable instead.
     */
    private final Variable raw;

    /**
     * Creates a new variable wrapping the given netCDF interface.
     */
    VariableWrapper(final Decoder decoder, Variable v) {
        super(decoder);
        variable = v;
        if (v instanceof VariableEnhanced) {
            v = ((VariableEnhanced) v).getOriginalVariable();
            if (v == null) {
                v = variable;
            }
        }
        raw = v;
        /*
         * If the UCAR library recognizes this variable as an enumeration, we will use UCAR services.
         * Only if UCAR did not recognized the enumeration, fallback on Apache SIS implementation.
         */
        Map<Long,String> enumeration = null;
        if (variable.getDataType().isEnum()) {
            Map<Integer,String> m = variable.getEnumTypedef().getMap();
            if (m != null) {
                enumeration = JDK19.newHashMap(m.size());
                for (Map.Entry<Integer,String> entry : m.entrySet()) {
                    enumeration.put(Integer.toUnsignedLong(entry.getKey()), entry.getValue());
                }
            }
        }
        setEnumeration(enumeration);        // Use SIS fallback if `enumeration` is null.
    }

    /**
     * Returns the name of the netCDF file containing this variable, or {@code null} if unknown.
     */
    @Override
    public String getFilename() {
        final String name = Utils.nonEmpty(variable.getDatasetLocation());
        if (name != null) {
            return name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf(File.separatorChar)) + 1);
        }
        return super.getFilename();
    }

    /**
     * If this element is member of a group, returns the name of that group.
     * Otherwise returns {@code null}.
     */
    @Override
    public String getGroupName() {
        final Group parent = variable.getParentGroup();
        return (parent != null) ? parent.getShortName() : null;
    }

    /**
     * Returns the name of this variable.
     */
    @Override
    public String getName() {
        return variable.getShortName();
    }

    /**
     * Returns the description of this variable, or {@code null} if none.
     */
    @Override
    public String getDescription() {
        return Utils.nonEmpty(variable.getDescription());
    }

    /**
     * Returns the unit of measurement as a string, or {@code null} if none.
     * Note that the UCAR library represents missing unit by an empty string,
     * which is ambiguous with dimensionless unit.
     */
    @Override
    protected String getUnitsString() {
        return Strings.trimOrNull(variable.getUnitsString());
        // Do not replace "N/A" by null since it is a valid unit symbol.
    }

    /**
     * Parses the given unit symbol and set the {@link #epoch} if the parsed unit is a temporal unit.
     * This method is called by {@link #getUnit()}. This implementation delegates the work to the UCAR
     * library and converts the result to {@link Unit} and {@link java.time.Instant} objects.
     */
    @Override
    protected Unit<?> parseUnit(final String symbols) throws Exception {
        if (TIME_UNIT_PATTERN.matcher(symbols).matches()) {
            /*
             * UCAR library has two methods for getting epoch: getDate() and getDateOrigin().
             * The former adds to the origin the number that may appear before the unit, for example
             * "2 hours since 1970-01-01 00:00:00". If there is no such number, then the two methods
             * are equivalent. It is not clear that adding such number is the right thing to do.
             */
            final DateUnit temporal = new DateUnit(symbols);
            epoch = temporal.getDateOrigin().toInstant();
            return Units.SECOND.multiply(temporal.getTimeUnit().getValueInSeconds());
        } else {
            /*
             * For all other units, we get the base unit (meter, radian, Kelvin, etc.) and multiply by the scale factor.
             * We also need to take the offset in account for constructing the °C unit as a unit shifted from its Kelvin
             * base. The UCAR library does not provide method giving directly this information, so we infer it indirectly
             * by converting the 0 value.
             */
            final SimpleUnit ucar = SimpleUnit.factoryWithExceptions(symbols);
            if (ucar.isUnknownUnit()) {
                return Units.valueOf(symbols);
            }
            final String baseUnit = ucar.getUnitString();
            Unit<?> unit = Units.valueOf(baseUnit);
            final double scale  = ucar.getValue();
            final double offset = ucar.convertTo(0, SimpleUnit.factoryWithExceptions(baseUnit));
            unit = unit.shift(offset);
            if (!Double.isNaN(scale)) {
                unit = unit.multiply(scale);
            }
            return unit;
        }
    }

    /**
     * Returns the variable data type.
     * This method may return {@code UNKNOWN} if the datatype is unknown.
     *
     * @see #getAttributeType(String)
     */
    @Override
    public DataType getDataType() {
        switch (variable.getDataType()) {
            case STRING: return DataType.STRING;
            case CHAR:   return DataType.CHAR;
            case BYTE:   return DataType.BYTE;
            case UBYTE:  return DataType.UBYTE;
            case SHORT:  return DataType.SHORT;
            case USHORT: return DataType.USHORT;
            case INT:    return DataType.INT;
            case UINT:   return DataType.UINT;
            case LONG:   return DataType.INT64;
            case FLOAT:  return DataType.FLOAT;
            case DOUBLE: return DataType.DOUBLE;
            default:     return DataType.UNKNOWN;
        }
    }

    /**
     * Returns whether this variable can grow. A variable is unlimited if at least one of its dimension is unlimited.
     */
    @Override
    protected boolean isUnlimited() {
        return variable.isUnlimited();
    }

    /**
     * Returns {@code true} if this variable seems to be a coordinate system axis.
     */
    @Override
    protected boolean isCoordinateSystemAxis() {
        // `isCoordinateVariable()` is not sufficient in the case of "runtime" axis.
        return variable.isCoordinateVariable() || (variable instanceof CoordinateAxis)
                || variable.hasAttribute(_Coordinate.AxisType)
                || variable.hasAttribute(CF.AXIS);
    }

    /**
     * Returns the value of the {@code "_CoordinateAxisType"} or {@code "axis"} attribute, or {@code null} if none.
     * Note that a {@code null} value does not mean that this variable is not an axis.
     */
    @Override
    protected String getAxisType() {
        if (variable instanceof CoordinateAxis) {
            final AxisType type = ((CoordinateAxis) variable).getAxisType();
            if (type != null) {
                return type.name();
            }
        }
        final String type = getAttributeAsString(_Coordinate.AxisType);
        return (type != null) ? type : getAttributeAsString(CF.AXIS);
    }

    /**
     * Returns a builder for the grid geometry of this variable, or {@code null} if this variable is not a data cube.
     * This method searches for a grid previously computed by {@link DecoderWrapper#getGridCandidates()},
     * keeping in mind that the UCAR library sometimes builds {@link CoordinateSystem} instances with axes
     * in different order than what we would expect. This method delegates to the super-class method only
     * if the grid requires a different analysis than the one performed by UCAR library.
     *
     * <p>This method should be invoked by {@link #getGridGeometry()} only once.
     * For that reason, it does not need to cache the value.</p>
     *
     * @see DecoderWrapper#getGridCandidates()
     */
    @Override
    protected Grid findGrid(final GridAdjustment adjustment) throws IOException, DataStoreException {
        /*
         * In some netCDF files, more than one grid could be associated to a variable. If the names of the
         * variables to use as coordinate system axes have been specified, use those names for filtering.
         * Otherwise no filtering is applied (which is the common case). If more than one grid fit, take
         * the first grid having the largest number of dimensions.
         *
         * This block duplicates work done in super.findGrid(…), except that it focuses on the set of coordinate
         * systems identified by UCAR for this variable while super.findGrid(…) inspects all dimensions found in
         * the file. Note that those coordinate systems may have been set by the user.
         */
        if (variable instanceof VariableDS) {
            final List<CoordinateSystem> systems = ((VariableDS) variable).getCoordinateSystems();
            if (!systems.isEmpty()) {
                GridWrapper grid = null;
                final String[] axisNames = decoder.convention().namesOfAxisVariables(this);
                for (final Grid candidate : decoder.getGridCandidates()) {
                    final GridWrapper ordered = ((GridWrapper) candidate).forVariable(variable, systems, axisNames);
                    if (ordered != null && (grid == null || ordered.getSourceDimensions() > grid.getSourceDimensions())) {
                        grid = ordered;
                    }
                }
                if (grid != null) {
                    return grid;
                }
            }
        }
        /*
         * If we reach this point, we did not found a grid using the dimensions of this variable.
         * But maybe there is a grid using other dimensions (typically with a decimation) that we
         * can map to the variable dimension using attribute values. This mechanism is described
         * in Convention.nameOfDimension(…).
         */
        return (GridWrapper) super.findGrid(adjustment);
    }

    /**
     * Returns the number of grid dimensions. This is the size of the {@link #getGridDimensions()} list
     * but cheaper than a call to {@code getGridDimensions().size()}.
     *
     * @return number of grid dimensions.
     */
    @Override
    public int getNumDimensions() {
        return variable.getRank();
    }

    /**
     * Returns the dimensions of this variable in the order they are declared in the netCDF file.
     * The dimensions are those of the grid, not the dimensions (or axes) of the coordinate system.
     * In ISO 19123 terminology, the dimension lengths give the upper corner of the grid envelope plus one.
     * The lower corner is always (0, 0, …, 0).
     *
     * @see #getNumDimensions()
     */
    @Override
    public List<org.apache.sis.storage.netcdf.base.Dimension> getGridDimensions() {
        return DimensionWrapper.wrap(variable.getDimensions());
    }

    /**
     * Returns the names of all attributes associated to this variable.
     *
     * @return names of all attributes associated to this variable.
     */
    @Override
    public Collection<String> getAttributeNames() {
        return toNames(variable.attributes());
    }

    /**
     * Returns the type of the attribute of the given name,
     * or {@code null} if the given attribute is not found.
     */
    @Override
    public Class<?> getAttributeType(final String attributeName) {
        return getAttributeType(raw.attributes().findAttributeIgnoreCase(attributeName));
    }

    /**
     * Implementation of {@link #getAttributeType(String)} shared with {@link GroupWrapper}.
     */
    static Class<?> getAttributeType(final Attribute attribute) {
        if (attribute != null) {
            if (attribute.isArray()) {
                return Vector.class;
            }
            switch (attribute.getDataType()) {
                case BYTE:   return Byte.class;
                case UBYTE:
                case SHORT:  return Short.class;
                case USHORT:
                case INT:    return Integer.class;
                case UINT:
                case LONG:   return Long.class;
                case FLOAT:  return Float.class;
                case DOUBLE: return Double.class;
                case STRING: return String.class;
                default:     return Object.class;
            }
        }
        return null;
    }

    /**
     * Returns the single value or vector of values for the given attribute, or {@code null} if none.
     * The returned value can be an instance of {@link String}, {@link Number}, {@link Vector} or {@code String[]}.
     * The search is case-insensitive.
     *
     * @param  attributeName  the name of the attribute for which to get the values.
     * @return value(s) for the named attribute, or {@code null} if none.
     */
    @Override
    protected Object getAttributeValue(final String attributeName) {
        return getAttributeValue(raw.attributes().findAttributeIgnoreCase(attributeName));
    }

    /**
     * Implementation of {@link #getAttributeValue(String)} shared with {@link GroupWrapper}.
     */
    static Object getAttributeValue(final Attribute attribute) {
        if (attribute != null) {
            final int length = attribute.getLength();
            switch (length) {
                case 0: break;
                case 1: {
                    final Object value = attribute.getValue(0);
                    if (value instanceof String) {
                        return Utils.nonEmpty((String) value);
                    } else if (value instanceof Number) {
                        return Utils.fixSign((Number) value, attribute.getDataType().isUnsigned());
                    }
                    break;
                }
                default: {
                    if (attribute.isString()) {
                        boolean hasValues = false;
                        final String[] values = new String[length];
                        for (int i=0; i<length; i++) {
                            values[i] = Utils.nonEmpty(attribute.getStringValue(i));
                            hasValues |= (values[i] != null);
                        }
                        if (hasValues) {
                            return values;
                        }
                    } else {
                        final Array array = attribute.getValues();
                        return createDecimalVector(array.get1DJavaArray(
                                ucar.ma2.DataType.getType(array)),
                                attribute.getDataType().isUnsigned());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the names of all attributes in the given container.
     */
    static List<String> toNames(final Iterable<Attribute> attributes) {
        final List<String> names = new ArrayList<>();
        for (final Attribute at : attributes) {
            names.add(at.getShortName());
        }
        return names;
    }

    /**
     * Returns the minimum and maximum values as determined by UCAR library, or inferred from the integer type otherwise.
     * This method is invoked only as a fallback; we give precedence to the range computed by Apache SIS instead of the
     * range provided by UCAR because we need the range of packed values instead of the range of converted values. Only
     * if Apache SIS cannot determine that range, that method is invoked.
     */
    @Override
    protected NumberRange<?> getRangeFallback() {
        if (variable instanceof EnhanceScaleMissingUnsigned) {
            final EnhanceScaleMissingUnsigned ev = (EnhanceScaleMissingUnsigned) variable;
            if (ev.hasValidData()) {
                // Returns a MeasurementRange instance for signaling the caller that this is converted values.
                return MeasurementRange.create(ev.getValidMin(), true, ev.getValidMax(), true, getUnit());
            }
        }
        return super.getRangeFallback();
    }

    /**
     * Notifies the parent class that UCAR library may cache the values provided by this variable.
     * This is an indication that the parent class should not invoke {@link Vector#compress(double)}.
     * Compressing vectors is useful only if the original array is discarded.
     * But the UCAR library has its own cache mechanism which may keep references to the original arrays.
     * Consequently, compressing vectors may result in data being duplicated.
     */
    @Override
    protected boolean isExternallyCached() {
        return true;
    }

    /**
     * Reads all the data for this variable and returns them as an array of a Java primitive type.
     * Multi-dimensional variables are flattened as a one-dimensional array (wrapped in a vector).
     * This method may replace fill/missing values by NaN values and caches the returned vector.
     *
     * @see #read()
     */
    @Override
    protected Object readFully() throws IOException {
        return get1DJavaArray(variable.read());             // May be already cached by the UCAR library.
    }

    /**
     * Reads a subsampled sub-area of the variable.
     * Array elements are in inverse of netCDF order.
     *
     * @param  area         indices of cell values to read along each dimension, in "natural" order.
     * @param  subsampling  subsampling along each dimension, or {@code null} if none.
     * @return the data as a vector wrapping a Java array.
     * @throws ArithmeticException if an argument exceeds the capacity of 32 bits integer.
     */
    @Override
    public Vector read(final GridExtent area, final long[] subsampling) throws IOException, DataStoreException {
        final Object array = readArray(area, subsampling);
        return Vector.create(array, variable.getDataType().isUnsigned());
    }

    /**
     * Reads a subsampled sub-area of the variable and returns them as a list of any object.
     * Elements in the returned list may be {@link Number} or {@link String} instances.
     *
     * @param  area         indices of cell values to read along each dimension, in "natural" order.
     * @param  subsampling  subsampling along each dimension, or {@code null} if none.
     * @return the data as a list of {@link Number} or {@link String} instances.
     * @throws ArithmeticException if an argument exceeds the capacity of 32 bits integer.
     */
    @Override
    public List<?> readAnyType(final GridExtent area, final long[] subsampling) throws IOException, DataStoreException {
        final Object array = readArray(area, subsampling);
        final ucar.ma2.DataType type = variable.getDataType();
        if (type == ucar.ma2.DataType.CHAR && variable.getRank() >= STRING_DIMENSION) {
            return createStringList(array, area);
        }
        return Vector.create(array, type.isUnsigned());
    }

    /**
     * Reads the data from this variable and returns them as an array of a Java primitive type.
     * Multi-dimensional variables are flattened as a one-dimensional array (wrapped in a vector).
     * This method may replace fill/missing values by NaN values and caches the returned vector.
     *
     * @param  area         indices of cell values to read along each dimension, in "natural" order.
     * @param  subsampling  subsampling along each dimension, or {@code null} if none.
     * @return the data as an array of a Java primitive type.
     * @throws ArithmeticException if an argument exceeds the capacity of 32 bits integer.
     *
     * @see #read()
     * @see #read(GridExtent, long[])
     */
    private Object readArray(final GridExtent area, final long[] subsampling) throws IOException, DataStoreException {
        int n = area.getDimension();
        final int[] lower = new int[n];
        final int[] size  = new int[n];
        final int[] sub   = (subsampling != null) ? new int[n] : null;
        n--;
        for (int i=0; i<=n; i++) {
            final int j = (n - i);
            lower[j] = Math.toIntExact(area.getLow(i));
            size [j] = Math.toIntExact(area.getSize(i));
            if (sub != null) {
                sub[j] = Math.toIntExact(subsampling[i]);
            }
        }
        final Array array;
        try {
            array = variable.read(sub != null ? new Section(lower, size, sub) : new Section(lower, size));
        } catch (InvalidRangeException e) {
            throw new DataStoreException(e);
        }
        return get1DJavaArray(array);
    }

    /**
     * Returns the one-dimensional Java array for the given UCAR array, avoiding copying if possible.
     * If {@link #hasRealValues()} returns {@code true}, then this method replaces fill and missing
     * values by {@code NaN} values.
     */
    private Object get1DJavaArray(final Array array) {
        final Object data = array.get1DJavaArray(ucar.ma2.DataType.getType(array));
        replaceNaN(data);
        return data;
    }

    /**
     * Creates an array of character strings from a "two-dimensional" array of characters stored in a flat array.
     * For each element, leading and trailing spaces and control codes are trimmed.
     * The array does not contain null element but may contain empty strings.
     *
     * @param  array     the "two-dimensional" array of characters stored in a flat {@code char[]} array.
     * @param  count     number of string elements (size of first dimension).
     * @param  length    number of characters in each element (size of second dimension).
     * @return array of character strings.
     */
    @Override
    protected String[] createStringArray(final Object array, final int count, final int length) {
        final char[] chars = (char[]) array;
        final String[] strings = new String[count];
        String previous = "";                       // For sharing same `String` instances when same value is repeated.
        int plo = 0, phi = 0;                       // Index range of bytes used for building the previous string.
        int lower = 0;
        for (int i=0; i<count; i++) {
            String element = "";
            final int upper = lower + length;
            for (int j=upper; --j >= lower;) {
                if (chars[j] > ' ') {
                    while (chars[lower] <= ' ') lower++;
                    if (Arrays.equals(chars, lower, ++j, chars, plo, phi)) {
                        element = previous;
                    } else {
                        element  = new String(chars, lower, j - lower);
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
        return strings;
    }

    /**
     * Returns a coordinate for this two-dimensional grid coordinate axis.
     * This is (indirectly) a callback method for {@link Grid#getAxes(Decoder)}.
     */
    @Override
    protected double coordinateForAxis(final int j, final int i) {
        return (variable instanceof CoordinateAxis2D) ? ((CoordinateAxis2D) variable).getCoordValue(j, i) : Double.NaN;
    }

    /**
     * Sets the scale and offset coefficients in the given "grid to CRS" transform if possible.
     * This method is invoked only for variables that represent a coordinate system axis.
     */
    @Override
    protected boolean trySetTransform(final Matrix gridToCRS, final int srcDim, final int tgtDim, final Vector data)
            throws IOException, DataStoreException
    {
        if (variable instanceof CoordinateAxis1D) {
            final CoordinateAxis1D axis = (CoordinateAxis1D) variable;
            if (axis.isRegular()) {
                final double start     = axis.getStart();
                final double increment = axis.getIncrement();
                if (start != 0 || increment != 0) {
                    gridToCRS.setElement(tgtDim, srcDim, increment);
                    gridToCRS.setElement(tgtDim, gridToCRS.getNumCol() - 1, start);
                    return true;
                }
                /*
                 * The UCAR library sometimes left those information uninitialized.
                 * If it seems to be the case, fallback on our own code.
                 */
            }
        }
        return super.trySetTransform(gridToCRS, srcDim, tgtDim, data);
    }

    /**
     * Returns {@code true} if this Apache SIS variable is a wrapper for the given UCAR variable.
     */
    final boolean isWrapperFor(final Variable v) {
        return (variable == v) || (raw == v);
    }
}
