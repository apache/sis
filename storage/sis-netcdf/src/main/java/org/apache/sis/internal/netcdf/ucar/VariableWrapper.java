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
package org.apache.sis.internal.netcdf.ucar;

import java.util.List;
import java.util.Collection;
import java.io.File;
import java.io.IOException;
import javax.measure.Unit;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Group;
import ucar.nc2.Attribute;
import ucar.nc2.VariableIF;
import ucar.nc2.dataset.Enhancements;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.EnhanceScaleMissing;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateUnit;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.math.Vector;
import org.apache.sis.internal.netcdf.DataType;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Grid;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;


/**
 * A {@link Variable} backed by the UCAR netCDF library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
final class VariableWrapper extends Variable {
    /**
     * The netCDF variable. This is typically an instance of {@link VariableEnhanced}.
     */
    private final VariableIF variable;

    /**
     * The variable without enhancements. May be the same instance than {@link #variable}
     * if that variable was not enhanced. This field is preferred to {@code variable} for
     * fetching attribute values because the {@code "scale_factor"} and {@code "add_offset"}
     * attributes are hidden by {@link VariableEnhanced}. In order to allow metadata reader
     * to find them, we query attributes in the original variable instead.
     */
    private final VariableIF raw;

    /**
     * The values of the whole variable, or {@code null} if not yet read. This vector should be assigned only
     * for relatively small variables, or for variables that are critical to the use of other variables
     * (for example the values in coordinate system axes).
     */
    private transient Vector values;

    /**
     * Creates a new variable wrapping the given netCDF interface.
     */
    VariableWrapper(final Decoder decoder, VariableIF v) {
        super(decoder);
        variable = v;
        if (v instanceof VariableEnhanced) {
            v = ((VariableEnhanced) v).getOriginalVariable();
            if (v == null) {
                v = variable;
            }
        }
        raw = v;
    }

    /**
     * Returns the name of the netCDF file containing this variable, or {@code null} if unknown.
     */
    @Override
    public String getFilename() {
        if (variable instanceof ucar.nc2.Variable) {
            final String name = Utils.nonEmpty(((ucar.nc2.Variable) variable).getDatasetLocation());
            if (name != null) {
                return name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf(File.separatorChar)) + 1);
            }
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
        String symbol = variable.getUnitsString();
        return (symbol != null && (symbol = symbol.trim()).isEmpty()) ? null : symbol;
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
        final DataType type;
        switch (variable.getDataType()) {
            case STRING: return DataType.STRING;
            case CHAR:   return DataType.CHAR;
            case BYTE:   type = DataType.BYTE;   break;
            case SHORT:  type = DataType.SHORT;  break;
            case INT:    type = DataType.INT;    break;
            case LONG:   type = DataType.INT64;  break;
            case FLOAT:  return DataType.FLOAT;
            case DOUBLE: return DataType.DOUBLE;
            default:     return DataType.UNKNOWN;
        }
        return type.unsigned(variable.isUnsigned());
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
        return variable.isCoordinateVariable();
    }

    /**
     * Returns a builder for the grid geometry of this variable, or {@code null} if this variable is not a data cube.
     * This method searches for a grid previously computed by {@link DecoderWrapper#getGrids()}, keeping in mind that
     * the UCAR library sometime builds {@link CoordinateSystem} instances with axes in different order than what we
     * would expect. This method delegates to the super-class method only if the grid requires a different analysis
     * than the one performed by UCAR library.
     *
     * <p>This method should be invoked by {@link #getGridGeometry()} only once.
     * For that reason, it does not need to cache the value.</p>
     *
     * @see DecoderWrapper#getGrids()
     */
    @Override
    protected Grid getGrid(final Adjustment adjustment) throws IOException, DataStoreException {
        /*
         * In some netCDF files, more than one grid could be associated to a variable. If the names of the
         * variables to use as coordinate system axes have been specified, use those names for filtering.
         * Otherwise no filtering is applied (which is the common case). If more than one grid fit, take
         * the first grid having the largest number of dimensions.
         *
         * This block duplicates work done in super.getGrid(…), except that it focuses on the set of coordinate
         * systems identified by UCAR for this variable while super.getGrid(…) inspects all dimensions found in
         * the file. Note that those coordinate systems may have been set by the user.
         */
        if (variable instanceof Enhancements) {
            final Grid[] grids = decoder.getGrids();    // Must be first for forcing some UCAR CS constructions.
            final List<CoordinateSystem> systems = ((Enhancements) variable).getCoordinateSystems();
            if (!systems.isEmpty()) {
                GridWrapper grid = null;
                final String[] axisNames = decoder.convention().namesOfAxisVariables(this);
                for (final Grid candidate : grids) {
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
        return (GridWrapper) super.getGrid(adjustment);
    }

    /**
     * Returns the dimensions of this variable in the order they are declared in the netCDF file.
     * The dimensions are those of the grid, not the dimensions (or axes) of the coordinate system.
     * In ISO 19123 terminology, the dimension lengths give the upper corner of the grid envelope plus one.
     * The lower corner is always (0, 0, …, 0).
     */
    @Override
    public List<org.apache.sis.internal.netcdf.Dimension> getGridDimensions() {
        return DimensionWrapper.wrap(variable.getDimensions());
    }

    /**
     * Returns the names of all attributes associated to this variable.
     *
     * @return names of all attributes associated to this variable.
     */
    @Override
    public Collection<String> getAttributeNames() {
        return toNames(variable.getAttributes());
    }

    /**
     * Returns the type of the attribute of the given name,
     * or {@code null} if the given attribute is not found.
     */
    @Override
    public Class<?> getAttributeType(final String attributeName) {
        return getAttributeType(raw.findAttributeIgnoreCase(attributeName));
    }

    /**
     * Implementation of {@link #getAttributeType(String)} shared with {@link GroupWrapper}.
     */
    static Class<?> getAttributeType(final Attribute attribute) {
        if (attribute != null) {
            switch (attribute.getDataType()) {
                case BYTE:   return Byte.class;
                case SHORT:  return Short.class;
                case INT:    return Integer.class;
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
     * Returns the sequence of values for the given attribute, or an empty array if none.
     * The elements will be of class {@link String} if {@code numeric} is {@code false},
     * or {@link Number} if {@code numeric} is {@code true}.
     */
    @Override
    public Object[] getAttributeValues(final String attributeName, final boolean numeric) {
        return getAttributeValues(raw.findAttributeIgnoreCase(attributeName), numeric);
    }

    /**
     * Implementation of {@link #getAttributeValues(String, boolean)} shared with {@link GroupWrapper}.
     */
    static Object[] getAttributeValues(final Attribute attribute, final boolean numeric) {
        if (attribute != null) {
            boolean hasValues = false;
            final Object[] values = new Object[attribute.getLength()];
            for (int i=0; i<values.length; i++) {
                if (numeric) {
                    values[i] = Utils.fixSign(attribute.getNumericValue(i), attribute.isUnsigned());
                    hasValues |= (values[i] != null);
                } else {
                    Object value = attribute.getValue(i);
                    if (value != null) {
                        String text = Utils.nonEmpty(value.toString());
                        if (text != null) {
                            values[i] = text;
                            hasValues = true;
                        }
                    }
                }
            }
            if (hasValues) {
                return values;
            }
        }
        return new Object[0];
    }

    /**
     * Returns the names of all attributes in the given list.
     */
    static List<String> toNames(final List<Attribute> attributes) {
        final String[] names = new String[attributes.size()];
        for (int i=0; i<names.length; i++) {
            names[i] = attributes.get(i).getShortName();
        }
        return UnmodifiableArrayList.wrap(names);
    }

    /**
     * Returns the minimum and maximum values as determined by UCAR library, or inferred from the integer type otherwise.
     * This method is invoked only as a fallback; we give precedence to the range computed by Apache SIS instead than the
     * range provided by UCAR because we need the range of packed values instead than the range of converted values. Only
     * if Apache SIS can not determine that range, that method is invoked.
     */
    @Override
    protected NumberRange<?> getRangeFallback() {
        if (variable instanceof EnhanceScaleMissing) {
            final EnhanceScaleMissing ev = (EnhanceScaleMissing) variable;
            if (ev.hasInvalidData()) {
                // Returns a MeasurementRange instance for signaling the caller that this is converted values.
                return MeasurementRange.create(ev.getValidMin(), true, ev.getValidMax(), true, getUnit());
            }
        }
        return super.getRangeFallback();
    }

    /**
     * Reads all the data for this variable and returns them as an array of a Java primitive type.
     * Multi-dimensional variables are flattened as a one-dimensional array (wrapped in a vector).
     * This method may replace fill/missing values by NaN values and caches the returned vector.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Vector read() throws IOException {
        if (values == null) {
            final Array array = variable.read();                // May be already cached by the UCAR library.
            values = createDecimalVector(get1DJavaArray(array), variable.isUnsigned());
            values = SHARED_VECTORS.unique(values);
        }
        return values;
    }

    /**
     * Reads a subsampled sub-area of the variable.
     * Array elements are in inverse of netCDF order.
     *
     * @param  area         indices of cell values to read along each dimension, in "natural" order.
     * @param  subsampling  subsampling along each dimension. 1 means no subsampling.
     * @return the data as an array of a Java primitive type.
     */
    @Override
    public Vector read(final GridExtent area, final int[] subsampling) throws IOException, DataStoreException {
        int n = area.getDimension();
        final int[] lower = new int[n];
        final int[] size  = new int[n];
        final int[] sub   = new int[n--];
        for (int i=0; i<=n; i++) {
            final int j = (n - i);
            lower[j] = Math.toIntExact(area.getLow(i));
            size [j] = Math.toIntExact(area.getSize(i));
            sub  [j] = subsampling[i];
        }
        final Array array;
        try {
            array = variable.read(new Section(lower, size, sub));
        } catch (InvalidRangeException e) {
            throw new DataStoreException(e);
        }
        return Vector.create(get1DJavaArray(array), variable.isUnsigned());
    }

    /**
     * Returns the one-dimensional Java array for the given UCAR array, avoiding copying if possible.
     * If {@link #hasRealValues()} returns {@code true}, then this method replaces fill and missing
     * values by {@code NaN} values.
     */
    private Object get1DJavaArray(final Array array) {
        final Object data = array.get1DJavaArray(array.getElementType());
        replaceNaN(data);
        return data;
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
    protected boolean trySetTransform(final Matrix gridToCRS, final int srcDim, final int tgtDim, final Vector values)
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
                 * The UCAR library sometime left those information uninitialized.
                 * If it seems to be the case, fallback on our own code.
                 */
            }
        }
        return super.trySetTransform(gridToCRS, srcDim, tgtDim, values);
    }

    /**
     * Returns {@code true} if this Apache SIS variable is a wrapper for the given UCAR variable.
     */
    final boolean isWrapperFor(final VariableIF v) {
        return (variable == v) || (raw == v);
    }
}
