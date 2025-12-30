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
package org.apache.sis.storage.netcdf.base;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.lang.reflect.Array;
import java.io.IOException;
import java.time.Instant;
import ucar.nc2.constants.CDM;      // String constants are copied by the compiler with no UCAR reference left.
import ucar.nc2.constants.CF;       // idem
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.InternalDataStoreException;
import org.apache.sis.storage.netcdf.AttributeNames;
import org.apache.sis.storage.netcdf.internal.Resources;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.math.Vector;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.pending.jdk.JDK19;
import static org.apache.sis.storage.base.StoreUtilities.ALLOW_LAST_RESORT_STATISTICS;


/**
 * A netCDF variable created by {@link Decoder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public abstract class Variable extends Node {
    /**
     * Pool of vectors created by the {@link #read()} method. This pool is used for sharing netCDF coordinate axes,
     * since the same vectors tend to be repeated in many netCDF files produced by the same data producer. Because
     * those vectors can be large, sharing common instances may save a lot of memory.
     *
     * <p>All shared vectors shall be considered read-only.</p>
     *
     * @see #read()
     * @see #setValues(Object, boolean)
     */
    private static final WeakHashSet<Vector> SHARED_VECTORS = new WeakHashSet<>(Vector.class);

    /**
     * The pattern to use for parsing temporal units of the form "days since 1970-01-01 00:00:00".
     *
     * @see #parseUnit(String)
     * @see Decoder#numberToDate(String, Number[])
     */
    public static final Pattern TIME_UNIT_PATTERN = Pattern.compile("(.+)\\Wsince\\W(.+)", Pattern.CASE_INSENSITIVE);

    /**
     * Minimal number of dimension of a {@code char} array for considering this variable as a list of strings.
     * This constant is defined for making easier to locate codes that check if this variable is a string list.
     *
     * @see #isString()
     */
    protected static final int STRING_DIMENSION = 2;

    /**
     * The role of this variable (axis, coverage, feature, <i>etc.</i>), or {@code null} if not yet determined.
     *
     * @see #getRole()
     */
    private VariableRole role;

    /**
     * The unit of measurement, parsed from {@link #getUnitsString()} when first needed.
     * We do not try to parse the unit at construction time because this variable may be
     * never requested by the user.
     *
     * @see #getUnit()
     */
    private Unit<?> unit;

    /**
     * If the unit is a temporal unit of the form "days since 1970-01-01 00:00:00", the epoch.
     * Otherwise {@code null}. This value can be set by subclasses as a side-effect of their
     * {@link #parseUnit(String)} method implementation.
     */
    protected Instant epoch;

    /**
     * Whether an attempt to parse the unit has already be done. This is used for avoiding
     * to report the same failure many times when {@link #unit} stay null.
     *
     * @see #getUnit()
     */
    private boolean unitParsed;

    /**
     * All no-data values declared for this variable, or an empty map if none.
     * This is computed by {@link #getNodataValues()} and cached for efficiency and stability.
     * The meaning of entries in this map is described in {@link #getNodataValues()} method javadoc.
     *
     * @see #getNodataValues()
     */
    private Map<Number,Object> nodataValues;

    /**
     * The {@code flag_meanings} values (used for enumeration values),
     * or {@code null} if this variable is not an enumeration.
     *
     * @see #setEnumeration(Map)
     * @see #getEnumeration()
     */
    private Map<Long,String> enumeration;

    /**
     * The grid associated to this variable, or {@code null} if none or not yet computed.
     * The grid needs to be computed if {@link #gridDetermined} is {@code false}.
     *
     * @see #gridDetermined
     * @see #getGridGeometry()
     */
    private GridGeometry gridGeometry;

    /**
     * Whether {@link #gridGeometry} has been computed. Note that the result may still be {@code null}.
     *
     * @see #gridGeometry
     * @see #getGridGeometry()
     */
    private boolean gridDetermined;

    /**
     * If {@link #gridGeometry} has less dimensions than this variable, index of a grid dimension to take as raster bands.
     * Otherwise this field is left uninitialized. If set, the index is relative to "natural" order (reverse of netCDF order).
     *
     * @see #getBandStride()
     * @see RasterResource#bandDimension
     */
    int bandDimension;

    /**
     * The values of the whole variable, or {@code null} if not yet read. This vector should be assigned only
     * for relatively small variables, or for variables that are critical to the use of other variables
     * (for example the values in coordinate system axes).
     *
     * @see #read()
     * @see #setValues(Object, boolean)
     */
    private transient Vector values;

    /**
     * The {@linkplain #values} vector as a list of element of any type (not restricted to {@link Number} instances).
     * This is usually the same instance as {@link #values} because {@link Vector} implements {@code List<Number>}.
     * This is a different instance if this variable is a two-dimensional character array, in which case this field
     * is an instance of {@code List<String>}.
     *
     * <p>The difference between {@code values} and {@code valuesAnyType} is that {@code values.get(i)} may throw
     * {@link NumberFormatException} because it always tries to return its elements as {@link Number} instances,
     * while {@code valuesAnyType.get(i)} can return {@link String} instances.</p>
     *
     * @see #readAnyType()
     * @see #setValues(Object, boolean)
     */
    private transient List<?> valuesAnyType;

    /**
     * Creates a new variable.
     *
     * @param decoder  the netCDF file where this variable is stored.
     */
    protected Variable(final Decoder decoder) {
        super(decoder);
    }

    /**
     * Initializes the map of enumeration values. If the given map is non-null, then the enumerations are set
     * to the specified map (by direct reference, the map is not cloned). Otherwise this method auto-detects
     * if this variable is an enumeration.
     *
     * <p>This method is invoked by subclass constructors for completing {@code Variable} creation.
     * It should not be invoked after creation, for keeping {@link Variable} immutable.</p>
     *
     * @param  enumeration  the enumeration map, or {@code null} for auto-detection.
     *
     * @see #getEnumeration()
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    protected final void setEnumeration(Map<Long,String> enumeration) {
        if (enumeration == null) {
            String srcLabels, srcNumbers;           // For more accurate message in case of warning.
            CharSequence[] labels = getAttributeAsStrings(srcLabels = AttributeNames.FLAG_NAMES, ' ');
            if (labels == null) {
                labels = getAttributeAsStrings(srcLabels = AttributeNames.FLAG_MEANINGS, ' ');
                if (labels == null) return;
            }
            Vector numbers = getAttributeAsVector(srcNumbers = AttributeNames.FLAG_VALUES);
            if (numbers == null) {
                numbers = getAttributeAsVector(srcNumbers = AttributeNames.FLAG_MASKS);
            }
            int count = labels.length;
            if (numbers != null) {
                final int n = numbers.size();
                if (n != count) {
                    warning(Variable.class, "setEnumeration", null,
                            Resources.Keys.MismatchedAttributeLength_5,
                            getName(), srcNumbers, srcLabels, n, count);
                    if (n < count) count = n;
                }
            } else {
                numbers = Vector.createSequence(0, 1, count);
                warning(Variable.class, "setEnumeration", null,
                        Resources.Keys.MissingVariableAttribute_3,
                        getFilename(), getName(), AttributeNames.FLAG_VALUES);
            }
            /*
             * Copy (numbers, labels) entries in an HashMap with keys converted to 32-bits signed integer.
             * If a key cannot be converted, we will log a warning after all errors have been collected
             * in order to produce only one log message. We put a limit on the number of reported errors
             * for avoiding to flood the logger.
             */
            Exception     error    = null;
            StringBuilder invalids = null;
            enumeration = JDK19.newHashMap(count);
            for (int i=0; i<count; i++) try {
                final CharSequence label = labels[i];
                if (label != null) {
                    long value = numbers.longValue(i);
                    enumeration.merge(value, label.toString(), (o,n) -> {
                        return o.equals(n) ? o : o + " | " + n;
                    });
                }
            } catch (NumberFormatException | ArithmeticException e) {
                if (error == null) {
                    error = e;
                    invalids = new StringBuilder();
                } else {
                    error.addSuppressed(e);
                    final int length = invalids.length();
                    final boolean tooManyErrors = (length > 100);   // Arbitrary limit in number of characters.
                    if (tooManyErrors && invalids.charAt(length - 1) == '…') {
                        continue;
                    }
                    invalids.append(", ");
                    if (tooManyErrors) {
                        invalids.append('…');
                        continue;
                    }
                }
                invalids.append(numbers.stringValue(i));
            }
            if (invalids != null) {
                warning(Variable.class, "setEnumeration", error,
                        Resources.Keys.UnsupportedEnumerationValue_3, getName(), numbers.getElementType(), invalids);
            }
        }
        if (!enumeration.isEmpty()) {
            this.enumeration = enumeration;
        }
    }

    /**
     * Returns the name of the netCDF file containing this variable, or {@code null} if unknown.
     * This is used for information purpose or error message formatting only.
     *
     * @return name of the netCDF file containing this variable, or {@code null} if unknown.
     */
    public String getFilename() {
        return decoder.getFilename();
    }

    /**
     * Returns the name of this variable. May be used as sample dimension name in a raster.
     * The variable name should be unique in each netCDF file
     * (by contrast, {@link #getStandardName()} is not always unique).
     *
     * @return the name of this variable.
     */
    @Override
    public abstract String getName();

    /**
     * Returns the standard name if available, or the unique variable name otherwise.
     * May be used for {@link RasterResource#getIdentifier()}.
     * Standard name is preferred to variable name when controlled vocabulary is desired,
     * for example for more stable identifier or more consistency between similar data.
     *
     * <p>This method does not check the {@code "long_name"} attribute because the long
     * name is more like a sentence (e.g. <q>model wind direction at 10 m</q>)
     * while standard name and variable name are more like identifiers.
     * For the long name, use {@link #getDescription()} instead.</p>
     *
     * @return the standard name, or a fallback if there is no standard name.
     *
     * @see RasterResource#identifier
     */
    public final String getStandardName() {
        final String name = getAttributeAsString(CF.STANDARD_NAME);
        return (name != null) ? name : getName();
    }

    /**
     * Returns the description of this variable, or {@code null} if none.
     * May be used as a category name in a sample dimension of a {@link Raster}.
     * This information may be encoded in different attributes like {@code "description"}, {@code "title"},
     * {@code "long_name"} or {@code "standard_name"}. If the return value is non-null, then it should also
     * be non-empty.
     *
     * @return the description of this variable, or {@code null}.
     */
    public abstract String getDescription();

    /**
     * Returns the unit of measurement as a string, or {@code null} if none.
     * The empty string cannot be used for meaning "dimensionless unit"; some text is required.
     *
     * <p>Note: the UCAR library has its own API for handling units (e.g. {@link ucar.nc2.units.SimpleUnit}).
     * However, as of November 2018, this API does not allow us to identify the quantity type except for some
     * special cases. We will parse the unit symbol ourselves instead, but we still need the full unit string
     * for parsing also its {@linkplain Axis#direction direction}.</p>
     *
     * @return the unit of measurement, or {@code null}.
     *
     * @see #getUnit()
     */
    protected abstract String getUnitsString();

    /**
     * Parses the given unit symbol and set the {@link #epoch} if the parsed unit is a temporal unit.
     * This method is invoked by {@link #getUnit()} when first needed.
     *
     * @param  symbols  the unit symbol to parse.
     * @return the parsed unit.
     * @throws Exception if the unit cannot be parsed. This wide exception type is used by the UCAR library.
     *
     * @see #getUnit()
     */
    protected abstract Unit<?> parseUnit(String symbols) throws Exception;

    /**
     * Sets the unit of measurement to the given value. This method is not used in CF-compliant files.
     * It is reserved for the handling of some particular conventions, for example HYCOM.
     *
     * @param  unit   the new unit of measurement.
     * @param  epich  the epoch if the unit is temporal, or {@code null} otherwise.
     *
     * @see #getUnit()
     */
    final void setUnit(final Unit<?> unit, final Instant epoch) {
        this.unit  = unit;
        this.epoch = epoch;
        unitParsed = true;
    }

    /**
     * Returns the unit of measurement for this variable, or {@code null} if unknown.
     * This method parses the units from {@link #getUnitsString()} when first needed
     * and sets {@link #epoch} as a side-effect if the unit is temporal.
     *
     * @return the unit of measurement, or {@code null}.
     */
    public final Unit<?> getUnit() {
        if (!unitParsed) {
            unitParsed = true;                          // Set first for avoiding to report errors many times.
            String symbols = getUnitsString();
            Exception error = null;
            if (symbols != null) try {
                unit = parseUnit(symbols);
            } catch (Exception ex) {
                error = ex;
            }
            if (unit == null) try {
                unit = decoder.convention().getUnitFallback(this);
            } catch (MeasurementParseException ex) {
                if (error == null) error = ex;
                else error.addSuppressed(ex);
                if (symbols == null) {
                    symbols = ex.getParsedString();
                }
            }
            if (error != null) {
                error(Variable.class, "getUnit", error, Errors.Keys.CanNotAssignUnitToVariable_2, getName(), symbols);
            }
        }
        return unit;
    }

    /**
     * Returns {@code true} if this variable contains data that are already in the unit of measurement represented by
     * {@link #getUnit()}, except for the fill/missing values. If {@code true}, then replacing fill/missing values by
     * {@code NaN} is the only action needed for having converted values.
     *
     * <p>This method is for detecting when {@link RasterResource#getSampleDimensions()} should return sample dimensions
     * for already converted values. But to be consistent with {@code SampleDimension} contract, it requires fill/missing
     * values to be replaced by NaN. This is done by {@link #replaceNaN(Object)}.</p>
     *
     * @return whether this variable contains values in unit of measurement, ignoring fill and missing values.
     */
    final boolean hasRealValues() {
        final int n = getDataType().number;
        if (n == Numbers.FLOAT | n == Numbers.DOUBLE) {
            final Convention convention = decoder.convention();
            if (convention != Convention.DEFAULT) {
                return convention.transferFunction(this).isIdentity();
            }
            // Shortcut for common case.
            Number c = getAttributeAsNumber(CDM.SCALE_FACTOR);
            if (c == null || c.doubleValue() == 1) {
                c = getAttributeAsNumber(CDM.ADD_OFFSET);
                return c == null || c.doubleValue() == 0;
            }
        }
        return false;
    }

    /**
     * Returns the variable data type.
     *
     * @return the variable data type, or {@link DataType#UNKNOWN} if unknown.
     *
     * @see #getAttributeType(String)
     * @see #writeDataTypeName(StringBuilder)
     */
    public abstract DataType getDataType();

    /**
     * Returns whether this variable is used as a coordinate system axis, a coverage or something else.
     * The role is determined by {@linkplain Convention#roleOf conventions}, except {@link VariableRole#BOUNDS}
     * which is determined by this method (because it depends on other variables).
     *
     * @return role of this variable.
     *
     * @see Convention#roleOf(Variable)
     */
    public final VariableRole getRole() {
        if (role == null) {
            final String name = getName();
            for (final Variable variable : decoder.getVariables()) {
                if (name.equalsIgnoreCase(variable.getAttributeAsString(CF.BOUNDS))) {
                    role = VariableRole.BOUNDS;
                    return role;
                }
            }
            role = decoder.convention().roleOf(this);
        }
        return role;
    }

    /**
     * Returns {@code true} if this variable should be considered as a list of strings.
     *
     * <h4>Maintenance note</h4>
     * The implementation of this method is inlined in some places, when the code already
     * has the {@link DataType} value at hand. If this implementation is modified, search
     * for {@link #STRING_DIMENSION} usages.
     *
     * @see #STRING_DIMENSION
     */
    final boolean isString() {
        return getNumDimensions() >= STRING_DIMENSION && getDataType() == DataType.CHAR;
    }

    /**
     * Returns whether this variable can grow. A variable is unlimited if at least one of its dimension is unlimited.
     * In netCDF 3 classic format, only the first dimension can be unlimited.
     *
     * @return whether this variable can grow.
     *
     * @see Dimension#isUnlimited()
     */
    protected abstract boolean isUnlimited();

    /**
     * Returns whether this variable is used as a coordinate system axis.
     * By netCDF convention, coordinate system axes have the name of one of the dimensions defined in the netCDF header.
     * It may also be identified by the {@code "axis"} on this variable, or by the {@code "coordinates"} attributes on
     * other variables.
     *
     * <p>This method has protected access because it should not be invoked directly. Code using variable role should
     * invoke {@link Convention#roleOf(Variable)} instead, for allowing specialization by {@link Convention}.</p>
     *
     * @return whether this variable is a coordinate system axis.
     *
     * @see Convention#roleOf(Variable)
     */
    protected abstract boolean isCoordinateSystemAxis();

    /**
     * Returns the value of {@code "_CoordinateAxisType"} or {@code "axis"} attribute, or {@code null} if none.
     * Note that a {@code null} value does not mean that this variable is not an axis.
     *
     * <p>Possible values for {@code _CoordinateAxisType} attribute include {@code "lat"}, {@code "lon"},
     * {@code "GeoX"} and {@code "GeoY"}. But the possible values for {@code axis} attribute include only
     * {@code "X"} and {@code "Y"}, which is more ambiguous. Caller should try to complete the information
     * when the returned value is X or Y.</p>
     *
     * @return {@code "_CoordinateAxisType"} or {@code "axis"} attribute value, or {@code null} if none.
     */
    protected abstract String getAxisType();

    /**
     * Returns a builder for the grid geometry of this variable, or {@code null} if this variable is not a data cube.
     * Not all variables have a grid geometry. For example, collections of features do not have such grid.
     * This method should be invoked only once per variable, but the same builder may be returned by different variables.
     * The grid may have fewer {@linkplain Grid#getDimensions() dimensions} than this variable,
     * in which case the additional {@linkplain #getGridDimensions() variable dimensions} can be considered as bands.
     * The dimensions of the grid may have different {@linkplain Dimension#length() lengths} than the dimensions of
     * this variable, in which case {@link #getGridGeometry()} is responsible for concatenating a scale factor to the
     * "grid to CRS" transform.
     *
     * <p>The default implementation provided in this {@code Variable} base class could be sufficient, but subclasses
     * are encouraged to override with a more efficient implementation or by exploiting information not available to this
     * base class (for example UCAR {@link ucar.nc2.dataset.CoordinateSystem} objects) and invoke {@code super.findGrid(…)}
     * as a fallback. The default implementation tries to build a grid in the following ways:</p>
     *
     * <ol class="verbose">
     *   <li><b>Grid of same dimension as this variable:</b>
     *     iterate over {@link Decoder#getGridCandidates() all localization grids} and search for an element having
     *     the same dimensions as this variable, i.e. where {@link Grid#getDimensions()} contains the same elements
     *     than {@link #getGridDimensions()} (not necessarily in the same order). The {@link Grid#forDimensions(Dimension[])}
     *     method will be invoked for reordering dimensions in the right order.</li>
     *
     *   <li><b>Grid of different dimension than this variable:</b>
     *     if no localization grid has been found above, inspect {@linkplain Decoder#getVariables() all variables}
     *     that may potentially be an axis for this variable even if they do not use the same netCDF dimensions.
     *     Grids of different dimensions may exist if the netCDF files provides a decimated localization grid,
     *     for example where the longitudes and latitudes variables specify the values of only 1/10 of cells.
     *     This method tries to map the grid dimensions to variables dimensions through the mechanism documented in
     *     {@link Convention#nameOfDimension(Variable, int)}. This method considers that we have a mapping when two
     *     dimensions have the same "name" — not the usual {@linkplain Dimension#getName() name encoded in netCDF format},
     *     but rather the value of some {@code "dim"} attribute. If this method can map all dimensions of this variable to
     *     dimensions of a grid, then that grid is returned.</li>
     *
     *   <li>If a mapping cannot be established for all dimensions, this method returns {@code null}.</li>
     * </ol>
     *
     * Subclasses should override this class with a more direct implementation and invoke this implementation only as a fallback.
     * Typically, subclasses will handle case #1 in above list and this implementation is invoked for case #2.
     * This method should be invoked only once, so subclasses do not need to cache the value.
     *
     * @param  adjustment  subclasses shall ignore and pass verbatim to {@code super.findGrid(adjustment)}.
     * @return the grid geometry for this variable, or {@code null} if none.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     */
    protected Grid findGrid(final GridAdjustment adjustment) throws IOException, DataStoreException {
        final Convention convention = decoder.convention();
        /*
         * Collect all axis dimensions, in no particular order. We use this map for determining
         * if a dimension of this variable can be used as-is, without the need to search for an
         * association through Convention.nameOfDimension(…). It may be the case for example if
         * the variable has a vertical or temporal axis which has not been decimated contrarily
         * to longitude and latitude axes. Note that this map is recycled later for other use.
         */
        final var axes = new ArrayList<Variable>();
        final var domain = new HashMap<Object, Dimension>();
        for (final Variable candidate : decoder.getVariables()) {
            if (candidate.getRole() == VariableRole.AXIS) {
                axes.add(candidate);
                for (final Dimension dim : candidate.getGridDimensions()) {
                    domain.put(dim, dim);
                }
            }
        }
        /*
         * Get all dimensions of this variable in netCDF order, then replace them by dimensions from an axis variable.
         * If we are in the situation #1 documented in javadoc, `isIncomplete` will be `false` after execution of this
         * loop and all dimensions should be the same as the values returned by `Variable.getGridDimensions()`.
         */
        boolean isIncomplete = false;
        final List<Dimension> fromVariable = getGridDimensions();
        final Dimension[] dimensions = fromVariable.toArray(Dimension[]::new);
        for (int i=0; i<dimensions.length; i++) {
            isIncomplete |= ((dimensions[i] = domain.remove(dimensions[i])) == null);
        }
        /*
         * If there is at least one variable dimension that we did not found directly among axis dimensions, check if
         * we can relate dimensions indirectly by Convention.nameOfDimension(…). This is the situation #2 in javadoc.
         * We do not merge this loop with above loop because we want all dimensions recognized by situation #1 to be
         * removed before we attempt those indirect associations.
         */
        if (isIncomplete) {
            for (int i=0; i<dimensions.length; i++) {
                if (dimensions[i] == null) {
                    final String label = convention.nameOfDimension(this, i);
                    if (label == null) {
                        return null;  // No information allowing us to relate that variable dimension to a grid dimension.
                    }
                    /*
                     * The first time that we find a label that may allow us to associate this variable dimension with a
                     * grid dimension, build a map of all labels associated to dimensions. We reuse the existing `domain`
                     * map; there is no confusion since the keys are not of the same class.
                     */
                    if (isIncomplete) {
                        isIncomplete = false;                                           // Execute this block only once.
                        if (adjustment.mapLabelToGridDimensions(this, axes, domain, convention)) {
                            return null;                           // Warning message already emitted by GridAdjustment.
                        }
                    }
                    /*
                     * Remembers which dimension from the variable corresponds to a dimension from the grid.
                     * Those dimensions would have been the same if we were not in a situation where size of
                     * localization grid is not the same as the data variable size.
                     */
                    final Dimension  varDimension = fromVariable.get(i);
                    final Dimension gridDimension = domain.remove(label);
                    dimensions[i] = gridDimension;
                    if (gridDimension == null) {
                        warning(Variable.class, "getGridGeometry", null,  // Caller (indirectly) for this method.
                                Resources.Keys.CanNotRelateVariableDimension_3, getFilename(), getName(), label);
                        return null;
                    }
                    if (adjustment.gridToVariable.put(gridDimension, varDimension) != null) {
                        throw new InternalDataStoreException(errors().getString(Errors.Keys.ElementAlreadyPresent_1, gridDimension));
                    }
                }
            }
        }
        /*
         * At this point we finished collecting all dimensions to use in the grid. Search a grid containing
         * those dimensions in the same order (the order is enforced by Grid.forDimensions(…) method call).
         * If we find a grid meting all criteria, we return it immediately. Otherwise select a fallback in
         * the following precedence order:
         *
         *   1) grid having all axes requested by the customized convention (usually there is none).
         *   2) grid having the greatest number of dimensions.
         */
        Grid fallback = null;
        boolean fallbackMatches = false;
        final String[] axisNames = convention.namesOfAxisVariables(this);       // Usually null.
        for (final Grid candidate : decoder.getGridCandidates()) {
            final Grid grid = candidate.forDimensions(dimensions);
            if (grid != null) {
                final int     gridDimension = grid.getSourceDimensions();
                final boolean gridMatches   = grid.containsAllNamedAxes(axisNames);
                if (gridMatches && gridDimension == dimensions.length) {
                    return grid;                                                // Full match: no need to continue.
                }
                if (gridMatches | !fallbackMatches) {
                    /*
                     * If the grid contains all axes, it has precedence over previous grid unless that previous grid
                     * also contained all axes (gridMatches == fallbackMatches). In such case we keep the grid having
                     * the largest number of dimensions.
                     */
                    if (gridMatches != fallbackMatches || fallback == null || gridDimension > fallback.getSourceDimensions()) {
                        fallbackMatches = gridMatches;
                        fallback = grid;
                    }
                }
            }
        }
        return fallback;
    }

    /**
     * Returns the grid geometry for this variable, or {@code null} if this variable is not a data cube.
     * Not all variables have a grid geometry. For example, collections of features do not have such grid.
     * The same grid geometry may be shared by many variables.
     * The grid may have fewer {@linkplain Grid#getDimensions() dimensions} than this variable,
     * in which case the additional {@linkplain #getGridDimensions() variable dimensions} can be considered as bands.
     *
     * @return the grid geometry for this variable, or {@code null} if none.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     */
    public final GridGeometry getGridGeometry() throws IOException, DataStoreException {
        if (!gridDetermined) {
            gridDetermined = true;                      // Set first so we don't try twice in case of failure.
            final var gridMapping = GridMapping.forVariable(this);
            final var adjustment = new GridAdjustment();
            final Grid info = findGrid(adjustment);
            if (info != null) {
                /*
                 * This variable may have more dimensions than the grid. We need to reduce the list to the same
                 * dimensions than the ones in the grid.  We cannot take Grid.getDimensions() directly because
                 * those dimensions may not have the same length (this mismatch is handled in the next block).
                 */
                List<Dimension> dimensions = getGridDimensions();                       // In netCDF order.
                final int dataDimension = dimensions.size();
                if (dataDimension > info.getSourceDimensions()) {
                    boolean copied = false;
                    final List<Dimension> toKeep = info.getDimensions();                // Also in netCDF order.
                    final int numToKeep = toKeep.size();
                    for (int i=0; i<numToKeep; i++) {
                        Dimension expected = toKeep.get(i);
                        expected = adjustment.gridToVariable.getOrDefault(expected, expected);
                        /*
                         * At this point, `expected` is a dimension of the variable that we expect to find at
                         * current index `i`. If we do not find that dimension, then the unexpected dimension
                         * is assumed to be a band. We usually remove at most one element. If removal results
                         * in a list too short, it would be a bug in the way we computed `toKeep`.
                         */
                        while (!expected.equals(dimensions.get(i))) {
                            if (!copied) {
                                copied = true;
                                dimensions = new ArrayList<>(dimensions);
                            }
                            /*
                             * It is possible that we never reach this point if the unexpected dimension is last.
                             * However in such case the dimension to declare is the last one in netCDF order,
                             * which corresponds to the first dimension (i.e. dimension 0) in "natural" order.
                             * Since the `bandDimension` field is initialized to zero, its value is correct.
                             */
                            bandDimension = dataDimension - 1 - i;          // Convert netCDF order to "natural" order.
                            dimensions.remove(i);
                            for (int j = dimensions.size(); --j >= i;) {
                                dimensions.set(j, dimensions.get(j).decrementIndex());
                            }
                            if (dimensions.size() < numToKeep) {
                                throw new InternalDataStoreException();     // Should not happen (see above comment).
                            }
                        }
                    }
                    /*
                     * At this point `dimensions` may still be longer than `toKeep` but it does not matter.
                     * We only need that for any index i < numToKeep, dimensions.get(i) corresponds to the
                     * dimension at the same index in the grid.
                     */
                }
                /*
                 * Compare the size of the variable with the size of the localization grid.
                 * If they do not match, then there is a scale factor between the two that
                 * needs to be applied.
                 */
                GridGeometry grid = info.getGridGeometry(decoder);
                if (grid != null) {
                    if (grid.isDefined(GridGeometry.EXTENT)) {
                        GridExtent extent = grid.getExtent();
                        final var sizes = new long[extent.getDimension()];
                        boolean needsResize = false;
                        for (int i = sizes.length; --i >= 0;) {
                            final int d = (sizes.length - 1) - i;               // Convert "natural order" index into netCDF index.
                            sizes[i] = dimensions.get(d).length();
                            if (!needsResize) {
                                needsResize = (sizes[i] != extent.getSize(i));
                            }
                        }
                        if (needsResize) {
                            final double[] dataToGridIndices = adjustment.dataToGridIndices();
                            if (dataToGridIndices == null || dataToGridIndices.length < sizes.length) {
                                warning(Variable.class, "getGridGeometry", null,
                                        Resources.Keys.ResamplingIntervalNotFound_2, getFilename(), getName());
                                return null;
                            }
                            extent = extent.resize(sizes);
                            grid = GridAdjustment.scale(grid, extent, info.getAnchor(), dataToGridIndices);
                        }
                    }
                    /*
                     * At this point we finished to build a grid geometry from the information provided by axes.
                     * If there is grid mapping attributes (e.g. "EPSG_code", "ESRI_pe_string", "GeoTransform",
                     * "spatial_ref", etc.), substitute some parts of the grid geometry by the parts built from
                     * those attributes.
                     */
                    if (gridMapping != null) {
                        grid = gridMapping.adaptGridCRS(this, grid, info.getAnchor());
                    }
                }
                gridGeometry = grid;
            } else if (gridMapping != null) {
                gridGeometry = gridMapping.createGridCRS(this);
            }
        }
        return gridGeometry;
    }

    /**
     * Returns the number of sample values between two bands.
     * This method is meaningful only if {@link #bandDimension} ≥ 0.
     */
    final long getBandStride() throws IOException, DataStoreException {
        long length = 1;
        final GridExtent extent = getGridGeometry().getExtent();
        for (int i=bandDimension; --i >= 0;) {
            length = Math.multiplyExact(length, extent.getSize(i));
        }
        return length;
    }

    /**
     * Returns the number of grid dimensions. This is the size of the {@link #getGridDimensions()}
     * list but may be cheaper than a call to {@code getGridDimensions().size()}.
     *
     * @return number of grid dimensions.
     */
    public abstract int getNumDimensions();

    /**
     * Returns the dimensions of this variable in the order they are declared in the netCDF file.
     * The dimensions are those of the grid, not the dimensions of the coordinate system.
     * In ISO 19123 terminology, {@link Dimension#length()} on each dimension give the upper corner
     * of the grid envelope plus one. The lower corner is always (0, 0, …, 0).
     *
     * <p>If {@link #findGrid(GridAdjustment)} returns a non-null value, then the list returned by this method should
     * contain all dimensions returned by {@link Grid#getDimensions()}. It may contain more dimension however.
     * Those additional dimensions can be considered as bands. Furthermore, the dimensions of the {@code Grid}
     * may have a different {@linkplain Dimension#length() length} than the dimensions returned by this method.
     * If such length mismatch exists, then {@link #getGridGeometry()} will concatenate a scale factor to
     * the "grid to CRS" transform.</p>
     *
     * <h4>Usage</h4>
     * This information is used for completing ISO 19115 metadata, providing a default implementation of
     * {@link Convention#roleOf(Variable)} method or for building string representation of this variable
     * among others. Those tasks are mostly for information purpose, except if {@code Variable} subclass
     * failed to create a grid and we must rely on {@link #findGrid(GridAdjustment)} default implementation.
     * For actual georeferencing, use {@link #getGridGeometry()} instead.
     *
     * @return all dimensions of this variable, in netCDF order (reverse of "natural" order).
     *
     * @see #getNumDimensions()
     * @see Grid#getDimensions()
     */
    public abstract List<Dimension> getGridDimensions();

    /**
     * Returns the range of valid values, or {@code null} if unknown. This is a shortcut for
     * {@link Convention#validRange(Variable)} with a fallback on {@link #getRangeFallback()}.
     *
     * @return the range of valid values, or {@code null} if unknown.
     *
     * @see Convention#validRange(Variable)
     */
    final NumberRange<?> getValidRange() {
        NumberRange<?> range = decoder.convention().validRange(this);
        if (range == null) {
            range = getRangeFallback();
            if (ALLOW_LAST_RESORT_STATISTICS && range == null) try {
                range = read().range();
            } catch (DataStoreException | IOException e) {
                // It should be a fatal error, but maybe the user wants only metadata.
                error(Variable.class, "getValidRange", e, Errors.Keys.CanNotRead_1, getName());
            }
        }
        return range;
    }

    /**
     * Returns the range of values as determined by the data type or other means, or {@code null} if unknown.
     * This method is invoked only as a fallback if {@link Convention#validRange(Variable)} did not found a
     * range of values by application of CF conventions. The returned range may be a range of packed values
     * or a range of real values. In the latter case, the range shall be an instance of
     * {@link org.apache.sis.measure.MeasurementRange}.
     *
     * <p>The default implementation returns the range of values that can be stored with the {@linkplain #getDataType()
     * data type} of this variable, if that type is an integer type. The range of {@linkplain #getNodataValues() no data
     * values} are subtracted.</p>
     *
     * @return the range of valid values, or {@code null} if unknown.
     *
     * @see Convention#validRange(Variable)
     */
    protected NumberRange<?> getRangeFallback() {
        final DataType dataType = getDataType();
        if (dataType.isInteger) {
            final int size = dataType.size() * Byte.SIZE;
            if (size > 0 && size <= Long.SIZE) {
                long min = 0;
                long max = Numerics.bitmask(size) - 1;
                if (!dataType.isUnsigned) {
                    max >>>= 1;
                    min = ~max;
                }
                for (final Number value : getNodataValues().keySet()) {
                    final long n = value.longValue();
                    final long Δmin = (n - min);            // Should be okay even with long unsigned values.
                    final long Δmax = (max - n);
                    if (Δmin >= 0 && Δmax >= 0) {           // Test if the pad/missing value is inside range.
                        if (Δmin < Δmax) min = n + 1;       // Reduce the extremum closest to the pad value.
                        else             max = n - 1;
                    }
                }
                if (max > min) {        // Note: this will also exclude unsigned long if max > Long.MAX_VALUE.
                    if (min >= Integer.MIN_VALUE && max <= Integer.MAX_VALUE) {
                        return NumberRange.create((int) min, true, (int) max, true);
                    }
                    return NumberRange.create(min, true, max, true);
                }
            }
        }
        return null;
    }

    /**
     * Returns enumeration values (keys) and their meanings (values), or {@code null} if this
     * variable is not an enumeration. This method returns a direct reference to internal map
     * (no clone, no unmodifiable wrapper); <strong>Do not modify the returned map.</strong>
     *
     * @return the ordinals and values associated to ordinals, or {@code null} if none.
     *
     * @see #setEnumeration(Map)
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Map<Long,String> getEnumeration() {
        return enumeration;
    }

    /**
     * Returns all no-data values declared for this variable, or an empty map if none.
     * The map keys are the no-data values (pad sample values or missing sample values).
     * The map values can be either {@link String} or {@link org.opengis.util.InternationalString} values
     * containing the description of the no-data value, or an {@link Integer} set to a bitmask identifying
     * the role of the pad/missing sample value:
     *
     * <ul>
     *   <li>If bit 0 is set, then the value is a pad value. Those values can be used for background.</li>
     *   <li>If bit 1 is set, then the value is a missing value.</li>
     * </ul>
     *
     * Pad values should be first in the map, followed by missing values.
     * The same value may have more than one role.
     * The map returned by this method shall be stable, i.e. two invocations of this method shall return the
     * same entries in the same order. This is necessary for mapping "no data" values to the same NaN values,
     * since their {@linkplain MathFunctions#toNanFloat(int) ordinal values} are based on order.
     *
     * @return pad/missing values with bitmask of their role.
     *
     * @see Convention#nodataValues(Variable)
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Map<Number,Object> getNodataValues() {
        if (nodataValues == null) {
            nodataValues = Containers.unmodifiable(decoder.convention().nodataValues(this));
        }
        return nodataValues;
    }

    /**
     * Builds the function converting values from their packed formats in the variable to "real" values.
     * This method is invoked in contexts where a transfer function is assumed to exist. Consequently, it
     * shall never return {@code null}, but may return the identity function.
     */
    final TransferFunction getTransferFunction() {
        return decoder.convention().transferFunction(this);
    }

    /**
     * Returns whether values in this variable are cached by a system other than Apache SIS.
     * For example if data are read using UCAR library, that library provides its own cache.
     *
     * @return whether values are cached by a library other than Apache SIS.
     */
    protected boolean isExternallyCached() {
        return false;
    }

    /**
     * Reads all the data for this variable and returns them as a vector of numerical values.
     * Multi-dimensional variables are flattened as a one-dimensional array (wrapped in a vector).
     * Example:
     *
     * <pre class="text">
     *   DIMENSIONS:
     *     time: 3
     *     lat : 2
     *     lon : 4
     *
     *   VARIABLES:
     *     temperature (time,lat,lon)
     *
     *   DATA INDICES:
     *     (0,0,0) (0,0,1) (0,0,2) (0,0,3)
     *     (0,1,0) (0,1,1) (0,1,2) (0,1,3)
     *     (1,0,0) (1,0,1) (1,0,2) (1,0,3)
     *     (1,1,0) (1,1,1) (1,1,2) (1,1,3)
     *     (2,0,0) (2,0,1) (2,0,2) (2,0,3)
     *     (2,1,0) (2,1,1) (2,1,2) (2,1,3)</pre>
     *
     * If {@link #hasRealValues()} returns {@code true}, then this method shall
     * {@linkplain #replaceNaN(Object) replace fill values and missing values by NaN values}.
     * This method caches the returned vector since this method may be invoked often.
     * Because of caching, this method should not be invoked for large data array.
     * Callers shall not modify the returned vector.
     *
     * @return the data as a vector wrapping a Java array.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of the variable exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final Vector read() throws IOException, DataStoreException {
        if (values == null) {
            setValues(readFully(), false);
        }
        return values;
    }

    /**
     * Reads all the data for this variable and returns them as a list of any object.
     * The difference between {@code read()} and {@code readAnyType()} is that {@code vector.get(i)} may throw
     * {@link NumberFormatException} because it always tries to return its elements as {@link Number} instances,
     * while {@code list.get(i)} can return {@link String} instances.
     *
     * @todo Consider extending to {@link java.time} objects as well. It would be useful in particular for
     *       climatological data, where objects may be {@link java.time.Month} or {@link java.time.MonthDay}.
     *
     * @return the data as a list of numbers or strings.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of the variable exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final List<?> readAnyType() throws IOException, DataStoreException {
        if (valuesAnyType == null) {
            setValues(readFully(), false);
        }
        return valuesAnyType;
    }

    /**
     * Reads a subsampled sub-area of the variable.
     * Constraints on the argument values are:
     *
     * <ul>
     *   <li>Argument dimensions shall be equal to the size of the {@link #getGridDimensions()} list.</li>
     *   <li>For each index <var>i</var>, value of {@code area[i]} shall be in the range from 0 inclusive
     *       to {@code Integer.toUnsignedLong(getShape()[length - 1 - i])} exclusive.</li>
     *   <li>Values are in "natural" order (inverse of netCDF order).</li>
     * </ul>
     *
     * If the variable has more than one dimension, then the data are packed in a one-dimensional vector
     * in the same way as {@link #read()}. If {@link #hasRealValues()} returns {@code true}, then this
     * method shall {@linkplain #replaceNaN(Object) replace fill/missing values by NaN values}.
     *
     * @param  area         indices of cell values to read along each dimension, in "natural" order.
     * @param  subsampling  subsampling along each dimension, or {@code null} if none.
     * @return the data as a vector wrapping a Java array.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of the region to read exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    public abstract Vector read(GridExtent area, long[] subsampling) throws IOException, DataStoreException;

    /**
     * Reads a subsampled sub-area of the variable and returns them as a list of any object.
     * Elements in the returned list may be {@link Number} or {@link String} instances.
     *
     * @todo Consider extending to {@link java.time} objects as well.
     *
     * @param  area         indices of cell values to read along each dimension, in "natural" order.
     * @param  subsampling  subsampling along each dimension, or {@code null} if none.
     * @return the data as a list of {@link Number} or {@link String} instances.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of the region to read exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    public abstract List<?> readAnyType(GridExtent area, long[] subsampling) throws IOException, DataStoreException;

    /**
     * Reads all the data for this variable and returns them as an array of a Java primitive type.
     * This is the implementation of {@link #read()} method, invoked when the value is not cached.
     *
     * @return the data as an array of a Java primitive type.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     */
    protected abstract Object readFully() throws IOException, DataStoreException;

    /**
     * Sets the values in this variable. The values are normally read from the netCDF file by {@link #read()},
     * but this {@code setValues(…)} method may also be invoked if the caller wants to overwrite those values.
     *
     * @param  array          the values as an array of primitive type (for example {@code float[]}.
     * @param  forceNumerics  whether to force the replacement of character strings by real numbers.
     * @throws ArithmeticException if the dimensions of this variable are too large.
     */
    final void setValues(final Object array, final boolean forceNumerics) {
        final DataType dataType = getDataType();
        if (dataType == DataType.CHAR) {
            int n = getNumDimensions();
            if (n >= STRING_DIMENSION) {
                final List<Dimension> dimensions = getGridDimensions();
                final int length = Math.toIntExact(dimensions.get(--n).length());   // Number of characters per value.
                long count = dimensions.get(--n).length();                          // Number of values.
                while (n > 0) {                                                     // In case of matrix of strings.
                    count = Math.multiplyExact(count, dimensions.get(--n).length());
                }
                /*
                 * The character strings may have been replaced by real numbers by the caller.
                 * In such case, we need to take the vector as-is.
                 */
                if (forceNumerics) {
                    assert Array.getLength(array) == count : getName();
                    values = SHARED_VECTORS.unique(Vector.create(array, false));
                    valuesAnyType = values;
                    return;
                }
                /*
                 * Standard case. The `createStringArray(…)` method expects either `byte[]` or `char[]`,
                 * depending on the subclass. It may throw `ClassCastException` if the array is not of
                 * the expected class, but it should not happen unless there is a bug in our algorithm.
                 *
                 * The `Vector.create(…)` and `wrap(…)` method calls take the array reference without cloning it.
                 * Consequently, creating those two objects now (even if we may not use them) is reasonably cheap.
                 */
                assert Array.getLength(array) == count * length : getName();
                final String[] strings = createStringArray(array, Math.toIntExact(count), length);
                values        = Vector.create(strings, false);
                valuesAnyType = Containers.viewAsUnmodifiableList(strings);
                return;
            }
        }
        Vector data = createDecimalVector(array, dataType.isUnsigned);
        /*
         * Do not invoke Vector.compress(…) if data are externally cached. Compressing vectors is useful only when
         * original array is discarded. But the UCAR library has its own cache mechanism which may keep references
         * to the original arrays. Consequently, compressing vectors may result in data being duplicated.
         */
        if (!isExternallyCached()) {
            /*
             * This method is usually invoked with vector of increasing or decreasing values. Set a tolerance threshold to
             * the precision of greatest (in magnitude) number, provided that this precision is not larger than increment.
             * If values are not sorted in increasing or decreasing order, then the tolerance computed below may be smaller
             * than optimal value. This is okay because it will cause more conservative compression
             * (i.e. it does not increase the risk of data loss).
             */
            double tolerance = 0;
            if (Numbers.isFloat(data.getElementType())) {
                final int n = data.size() - 1;
                if (n >= 0) {
                    double first = data.doubleValue(0);
                    double last  = data.doubleValue(n);
                    double inc   = Math.abs((last - first) / n);
                    if (!Double.isNaN(inc)) {
                        double ulp = Math.ulp(Math.max(Math.abs(first), Math.abs(last)));
                        tolerance = Math.min(inc, ulp);
                    }
                }
            }
            data = data.compress(tolerance);
        }
        values = SHARED_VECTORS.unique(data);
        valuesAnyType = values;
    }

    /**
     * Creates an array of character strings from a "two-dimensional" array of characters stored in a flat array.
     * For each element, leading and trailing spaces and control codes are trimmed.
     * The array does not contain null element but may contain empty strings.
     *
     * <p>The implementation of this method is the same code duplicated in subclasses,
     * except that one subclass (the SIS implementation) expects a {@code byte[]} array
     * and the other subclass (the wrapper around UCAR library) expects a {@code char[]} array.</p>
     *
     * @param  chars   the "two-dimensional" array stored in a flat {@code byte[]} or {@code char[]} array.
     * @param  count   number of string elements (size of first dimension).
     * @param  length  number of characters in each element (size of second dimension).
     * @return array of character strings.
     */
    protected abstract String[] createStringArray(Object chars, int count, int length);

    /**
     * Creates a list of character strings from a "two-dimensional" array of characters stored in a flat array.
     *
     * @param  chars  the "two-dimensional" array stored in a flat {@code byte[]} or {@code char[]} array.
     * @param  area   the {@code area} argument given to the {@code read(…)} method that obtained the array.
     * @return list of character strings.
     */
    protected final List<String> createStringList(final Object chars, final GridExtent area) {
        final int length = Math.toIntExact(area.getSize(0));
        long count = area.getSize(1);
        for (int i = area.getDimension(); --i >= STRING_DIMENSION;) {   // As a safety, but should never enter in this loop.
            count = Math.multiplyExact(count, area.getSize(i));
        }
        return Containers.viewAsUnmodifiableList(createStringArray(chars, Math.toIntExact(count), length));
    }

    /**
     * Wraps the given data in a {@link Vector} with the assumption that accuracy in base 10 matters.
     * This method is suitable for coordinate axis variables, but should not be used for the main data.
     *
     * @param  data        the data to wrap in a vector.
     * @param  isUnsigned  whether the data type is an unsigned type.
     * @return vector wrapping the given data.
     */
    protected static Vector createDecimalVector(final Object data, final boolean isUnsigned) {
        if (data instanceof float[]) {
            return Vector.createForDecimal((float[]) data);
        } else {
            return Vector.create(data, isUnsigned);
        }
    }

    /**
     * Maybe replaces fill values and missing values by {@code NaN} values in the given array.
     * This method does nothing if {@link #hasRealValues()} returns {@code false}.
     * The NaN values used by this method must be consistent with the NaN values declared in
     * the sample dimensions created by {@link RasterResource}.
     *
     * @param  array  the array in which to replace fill and missing values.
     */
    protected final void replaceNaN(final Object array) {
        if (hasRealValues()) {
            int ordinal = 0;
            for (final Number value : getNodataValues().keySet()) {
                final float pad = MathFunctions.toNanFloat(ordinal++);      // Must be consistent with RasterResource.createSampleDimension(…).
                if (array instanceof float[]) {
                    ArraysExt.replace((float[]) array, value.floatValue(), pad);
                } else if (array instanceof double[]) {
                    ArraysExt.replace((double[]) array, value.doubleValue(), pad);
                }
            }
        }
    }

    /**
     * Returns a coordinate for this two-dimensional grid coordinate axis. This is (indirectly) a callback method
     * for {@link Grid#getAxes(Decoder)}. The (<var>i</var>, <var>j</var>) indices are grid indices <em>before</em>
     * they get reordered by the {@link Grid#getAxes(Decoder)} method. In the netCDF UCAR API, this method maps directly
     * to {@link ucar.nc2.dataset.CoordinateAxis2D#getCoordValue(int, int)}.
     *
     * @param  j  the slowest varying (left-most) index.
     * @param  i  the fastest varying (right-most) index.
     * @return the coordinate at the given index, or {@link Double#NaN} if it cannot be computed.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the axis size exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    protected abstract double coordinateForAxis(int j, int i) throws IOException, DataStoreException;

    /**
     * Sets the scale and offset coefficients in the given "grid to CRS" transform if possible.
     * Source and target dimensions given to this method are in "natural" order (reverse of netCDF order).
     * This method is invoked only for variables that represent a coordinate system axis.
     * Setting the coefficient is possible only if values in this variable are regular,
     * i.e. the difference between two consecutive values is constant.
     *
     * @param  gridToCRS  the matrix in which to set scale and offset coefficient.
     * @param  srcDim     the source dimension, which is a dimension of the grid. Identifies the matrix column of scale factor.
     * @param  tgtDim     the target dimension, which is a dimension of the CRS.  Identifies the matrix row of scale factor.
     * @param  data       the vector to use for computing scale and offset.
     * @return whether this method has successfully set the scale and offset coefficients.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     */
    protected boolean trySetTransform(final Matrix gridToCRS, final int srcDim, final int tgtDim, final Vector data)
            throws IOException, DataStoreException
    {
        final int n = data.size() - 1;
        if (n >= 0) {
            final double first = data.doubleValue(0);
            Number increment;
            if (n >= 1) {
                final double last = data.doubleValue(n);
                double error;
                if (getDataType() == DataType.FLOAT) {
                    error = Math.max(Math.ulp((float) first), Math.ulp((float) last));
                } else {
                    error = Math.max(Math.ulp(first), Math.ulp(last));
                }
                error = Math.max(Math.ulp(last - first), error) / n;
                increment = data.increment(error);                          // May return null.
            } else {
                increment = Double.NaN;
            }
            if (increment != null) {
                gridToCRS.setElement(tgtDim, srcDim, increment.doubleValue());
                gridToCRS.setElement(tgtDim, gridToCRS.getNumCol() - 1, first);
                return true;
            }
        }
        return false;
    }

    /**
     * Constructs the exception to throw when the variable position cannot be computed.
     *
     * @param  cause  the reason why we cannot compute the position, or {@code null}.
     * @return the exception to throw.
     */
    protected final DataStoreContentException canNotComputePosition(final ArithmeticException cause) {
        return new DataStoreContentException(decoder.resources().getString(
                Resources.Keys.CanNotComputeVariablePosition_2, getFilename(), getName()), cause);
    }

    /**
     * Appends the name of the variable data type as the name of the primitive type
     * followed by the span of each dimension (in unit of grid cells) between brackets.
     * Dimensions are listed in "natural" order (reverse of netCDF order).
     * Example: {@code "SHORT[360][180]"}.
     *
     * @param  buffer  the buffer when to append the name of the variable data type.
     */
    public final void writeDataTypeName(final StringBuilder buffer) {
        buffer.append(getDataType().name().toLowerCase(Decoder.DATA_LOCALE));
        final List<Dimension> dimensions = getGridDimensions();
        for (int i=dimensions.size(); --i>=0;) {
            dimensions.get(i).writeLength(buffer);
        }
    }

    /**
     * Returns a string representation of this variable for debugging purpose.
     *
     * @return a string representation of this variable.
     *
     * @see #writeDataTypeName(StringBuilder)
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(getName()).append(" : ");
        writeDataTypeName(buffer);
        if (isUnlimited()) {
            buffer.append(" (unlimited)");
        }
        return buffer.toString();
    }

    /*
     * Do not override `Object.equals(Object)` and `Object.hashCode()`,
     * because variables are used as keys by `GridMapping.forVariable(…)`.
     */
}
