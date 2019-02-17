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
package org.apache.sis.internal.netcdf;

import java.util.Iterator;
import org.apache.sis.internal.referencing.LazySet;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.Numbers;
import ucar.nc2.constants.CDM;


/**
 * Extends the CF-Conventions with some conventions particular to a data producer.
 * By default, Apache SIS netCDF reader applies the <a href="http://cfconventions.org">CF conventions</a>.
 * But some data producers does not provides all necessary information for allowing Apache SIS to read the
 * netCDF file. Some information may be missing because considered implicit by the data producer.
 * This class provides a mechanism for supplying the implicit values.
 * Conventions can be registered in a file having this exact path:
 *
 * <blockquote><pre>META-INF/services/org.apache.sis.internal.netcdf.Convention</pre></blockquote>
 *
 * <p><b>This is an experimental class for internal usage only (for now).</b>
 * The API of this class is likely to change in any future Apache SIS version.
 * This class may become public (in a modified form) in the future if we gain
 * enough experience about extending netCDF conventions.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.0
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-315">SIS-315</a>
 *
 * @since 1.0
 * @module
 */
public class Convention {
    /**
     * All conventions found on the classpath.
     */
    private static final LazySet<Convention> AVAILABLES = new LazySet<>(Convention.class);

    /**
     * The convention to use when no specific conventions were found.
     */
    private static final Convention DEFAULT = new Convention();

    /**
     * Names of attributes where to fetch minimum and maximum sample values, in preference order.
     *
     * @see #getValidValues(Variable)
     */
    private static final String[] RANGE_ATTRIBUTES = {
        "valid_range",      // Expected "reasonable" range for variable.
        "actual_range",     // Actual data range for variable.
        "valid_min",        // Fallback if "valid_range" is not specified.
        "valid_max"
    };

    /**
     * For subclass constructors.
     */
    protected Convention() {
    }

    /**
     * Finds the convention to apply to the file opened by the given decoder, or {@code null} if none.
     */
    static synchronized Convention find(final Decoder decoder) {
        final Iterator<Convention> it;
        Convention c;
        synchronized (AVAILABLES) {
            it = AVAILABLES.iterator();
            if (!it.hasNext()) return DEFAULT;
            c = it.next();
        }
        while (!c.isApplicableTo(decoder)) {
            synchronized (AVAILABLES) {
                if (!it.hasNext()) return DEFAULT;
                c = it.next();
            }
        }
        return c;
    }

    /**
     * Detects if this set of conventions applies to the given netCDF file.
     *
     * @param  decoder  the netCDF file to test.
     * @return {@code true} if this set of conventions can apply.
     */
    protected boolean isApplicableTo(final Decoder decoder) {
        return false;
    }

    /**
     * Returns the role of the given variable. In particular, this method shall return
     * {@link VariableRole#AXIS} if the given variable seems to be a coordinate system axis.
     *
     * @param  variable  the variable for which to get the role, or {@code null}.
     * @return role of the given variable, or {@code null} if the given variable was null.
     */
    public VariableRole roleOf(final Variable variable) {
        return (variable != null) ? variable.getRole() : null;
    }

    /**
     * Returns the names of the variables containing data for all dimension of a variable.
     * Each netCDF variable can have an arbitrary number of dimensions identified by their name.
     * The data for a dimension are usually stored in a variable of the same name, but not always.
     * This method gives an opportunity for subclasses to select the axis variables using other criterion.
     * This happen for example if a netCDF file defines two grids for the same dimensions.
     * The order in returned array will be the axis order in the Coordinate Reference System.
     *
     * <p>The default implementation returns {@code null}.</p>
     *
     * @param  variable  the variable for which the list of axis variables are desired, in CRS order.
     * @return names of the variables containing axis values, or {@code null} if this
     *         method performs applies no special convention for the given variable.
     */
    public String[] namesOfAxisVariables(Variable variable) {
        return null;
    }

    /**
     * Returns the range of valid values, or {@code null} if unknown.
     * The default implementation takes the range of values from the following properties, in precedence order:
     *
     * <ol>
     *   <li>{@code "valid_range"}  — expected "reasonable" range for variable.</li>
     *   <li>{@code "actual_range"} — actual data range for variable.</li>
     *   <li>{@code "valid_min"}    — ignored if {@code "valid_range"} is present, as specified in UCAR documentation.</li>
     *   <li>{@code "valid_max"}    — idem.</li>
     * </ol>
     *
     * Whether the returned range is a range of packed values or a range of real values is ambiguous.
     * An heuristic rule is documented in UCAR {@link ucar.nc2.dataset.EnhanceScaleMissing} interface.
     * If both type of ranges are available, then this method should return the range of packed value.
     * Otherwise if this method returns the range of real values, then that range shall be an instance
     * of {@link MeasurementRange} for allowing the caller to distinguish the two cases.
     *
     * @param  source  the variable to get valid range of values for.
     * @return the range of valid values, or {@code null} if unknown.
     *
     * @see Variable#getRangeFallback()
     */
    public NumberRange<?> getValidValues(final Variable source) {
        Number minimum = null;
        Number maximum = null;
        Class<? extends Number> type = null;
        for (final String attribute : RANGE_ATTRIBUTES) {
            for (final Object element : source.getAttributeValues(attribute, true)) {
                if (element instanceof Number) {
                    Number value = (Number) element;
                    if (element instanceof Float) {
                        final float fp = (Float) element;
                        if      (fp == +Float.MAX_VALUE) value = Float.POSITIVE_INFINITY;
                        else if (fp == -Float.MAX_VALUE) value = Float.NEGATIVE_INFINITY;
                    } else if (element instanceof Double) {
                        final double fp = (Double) element;
                        if      (fp == +Double.MAX_VALUE) value = Double.POSITIVE_INFINITY;
                        else if (fp == -Double.MAX_VALUE) value = Double.NEGATIVE_INFINITY;
                    }
                    type = Numbers.widestClass(type, value.getClass());
                    minimum = Numbers.cast(minimum, type);
                    maximum = Numbers.cast(maximum, type);
                    value   = Numbers.cast(value,   type);
                    if (!attribute.endsWith("max") && (minimum == null || compare(value, minimum) < 0)) minimum = value;
                    if (!attribute.endsWith("min") && (maximum == null || compare(value, maximum) > 0)) maximum = value;
                }
            }
            if (minimum != null && maximum != null) {
                /*
                 * Heuristic rule defined in UCAR documentation (see EnhanceScaleMissing interface):
                 * if the type of the range is equal to the type of the scale, and the type of the
                 * data is not wider, then assume that the minimum and maximum are real values.
                 */
                final int rangeType = Numbers.getEnumConstant(type);
                if (rangeType >= source.getDataType().number &&
                    rangeType >= Math.max(Numbers.getEnumConstant(source.getAttributeType(CDM.SCALE_FACTOR)),
                                          Numbers.getEnumConstant(source.getAttributeType(CDM.ADD_OFFSET))))
                {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    final NumberRange<?> range = new MeasurementRange(type, minimum, true, maximum, true, source.getUnit());
                    return range;
                } else {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    final NumberRange<?> range = new NumberRange(type, minimum, true, maximum, true);
                    return range;
                }
            }
        }
        return null;
    }

    /**
     * Compares two numbers which shall be of the same class.
     * This is a helper method for {@link #getValidValues(Variable)}.
     */
    @SuppressWarnings("unchecked")
    private static int compare(final Number n1, final Number n2) {
        return ((Comparable) n1).compareTo((Comparable) n2);
    }

    /**
     * Builds the function converting values from their packed formats in the variable to "real" values.
     * The transfer function is typically built from the {@code "scale_factor"} and {@code "add_offset"}
     * attributes associated to the given variable, but other conventions could use different attributes.
     * The returned function will be a component of the {@link org.apache.sis.coverage.SampleDimension}
     * to be created for each variable.
     *
     * @param  source  the variable from which to determine the transfer function.
     *
     * @return a transfer function built from the attributes defined in the given variable. Never null;
     *         if no information is found in the given {@code source} variable, then the return value
     *         shall be an identity function.
     */
    public TransferFunction getTransferFunction(final Variable source) {
        /*
         * If scale_factor and/or add_offset variable attributes are present, then this is
         * a "packed" variable. Otherwise the transfer function is the identity transform.
         */
        final TransferFunction tr = new TransferFunction();
        final double scale  = source.getAttributeAsNumber(CDM.SCALE_FACTOR);
        final double offset = source.getAttributeAsNumber(CDM.ADD_OFFSET);
        if (!Double.isNaN(scale))  tr.setScale (scale);
        if (!Double.isNaN(offset)) tr.setOffset(offset);
        return tr;
    }
}