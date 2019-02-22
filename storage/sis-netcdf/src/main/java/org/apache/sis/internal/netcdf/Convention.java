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

import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.awt.image.DataBuffer;
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
 * Instances of this class must be immutable and thread-safe.
 * This class does not encapsulate all conventions needed for understanding a netCDF file,
 * but only conventions that are more likely to need to be overridden for some data producers.
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
    static final Convention DEFAULT = new Convention();

    /**
     * Names of groups where to search for metadata, in precedence order.
     * The {@code null} value stands for global attributes.
     *
     * <p>REMINDER: if modified, update {@link org.apache.sis.storage.netcdf.MetadataReader} class javadoc too.</p>
     */
    private static final String[] SEARCH_PATH = {"NCISOMetadata", "CFMetadata", null, "THREDDSMetadata"};

    /**
     * Names of attributes where to fetch minimum and maximum sample values, in preference order.
     *
     * @see #validRange(Variable)
     */
    private static final String[] RANGE_ATTRIBUTES = {
        "valid_range",      // Expected "reasonable" range for variable.
        "actual_range",     // Actual data range for variable.
        "valid_min",        // Fallback if "valid_range" is not specified.
        "valid_max"
    };

    /**
     * Names of attributes where to fetch missing or pad values. Order matter since it determines the bits to be set in the
     * map returned by {@link #nodataValues(Variable)}. The main bit is bit #0, which identifies the background value.
     */
    private static final String[] NODATA_ATTRIBUTES = {
        CDM.FILL_VALUE,
        CDM.MISSING_VALUE
    };

    /**
     * For subclass constructors.
     */
    protected Convention() {
    }

    /**
     * Finds the convention to apply to the file opened by the given decoder, or {@code null} if none.
     * This method does not change the state of the given {@link Decoder}.
     */
    static Convention find(final Decoder decoder) {
        final Iterator<Convention> it;
        Convention c;
        synchronized (AVAILABLES) {
            it = AVAILABLES.iterator();
            if (!it.hasNext()) {
                return DEFAULT;
            }
            c = it.next();
        }
        /*
         * We want the call to isApplicableTo(…) to be outside the synchronized block in order to avoid contentions.
         * This is also a safety against dead locks if that method acquire other locks. Only Iterator methods should
         * be invoked inside the synchronized block.
         */
        while (!c.isApplicableTo(decoder)) {
            synchronized (AVAILABLES) {
                if (!it.hasNext()) {
                    c = DEFAULT;
                    break;
                }
                c = it.next();
            }
        }
        return c;
    }

    /**
     * Detects if this set of conventions applies to the given netCDF file.
     * This method shall not change the state of the given {@link Decoder}.
     *
     * @param  decoder  the netCDF file to test.
     * @return {@code true} if this set of conventions can apply.
     */
    protected boolean isApplicableTo(final Decoder decoder) {
        return false;
    }

    /**
     * Specifies a list of groups where to search for named attributes, in preference order.
     * The {@code null} name stands for the root group.
     *
     * @return  name of groups where to search in for global attributes, in preference order.
     *          Never null, never empty, but can contain null values to specify root as search path.
     *
     * @see Decoder#setSearchPath(String...)
     */
    public String[] getSearchPath() {
        return SEARCH_PATH.clone();
    }

    /**
     * Returns the name of an attribute in this convention which is equivalent to the attribute of given name in CF-convention.
     * The given parameter is a name from <cite>CF conventions</cite> or from <cite>Attribute Convention for Dataset Discovery
     * (ACDD)</cite>. Some of those attribute names are listed in the {@link org.apache.sis.storage.netcdf.AttributeNames} class.
     *
     * <p>In current version of netCDF reader, this method is invoked only for global attributes,
     * not for the attributes on variables.</p>
     *
     * <p>The default implementation returns {@code name} unchanged.</p>
     *
     * @param  name  an attribute name from CF or ACDD convention.
     * @return the attribute name expected to be found in a netCDF file structured according this {@code Convention}.
     *         If this convention does not know about attribute of the given name, then {@code name} is returned unchanged.
     */
    public String mapAttributeName(final String name) {
        return name;
    }

    /**
     * Returns whether the given variable is used as a coordinate system axis, a coverage or something else.
     * In particular this method shall return {@link VariableRole#AXIS} if the given variable seems to be a
     * coordinate system axis instead than the actual data. By netCDF convention, coordinate system axes
     * have the name of one of the dimensions defined in the netCDF header.
     *
     * <p>The default implementation returns {@link VariableRole#COVERAGE} if the given variable can be used
     * for generating an image, by checking the following conditions:</p>
     *
     * <ul>
     *   <li>Images require at least {@value Grid#MIN_DIMENSION} dimensions of size equals or greater than {@value Grid#MIN_SPAN}.
     *       They may have more dimensions, in which case a slice will be taken later.</li>
     *   <li>Exclude axes. Axes are often already excluded by the above condition because axis are usually 1-dimensional,
     *       but some axes are 2-dimensional (e.g. a localization grid).</li>
     *   <li>Excludes characters, strings and structures, which can not be easily mapped to an image type.
     *       In addition, 2-dimensional character arrays are often used for annotations and we do not want
     *       to confuse them with images.</li>
     * </ul>
     *
     * @param  variable  the variable for which to get the role, or {@code null}.
     * @return role of the given variable, or {@code null} if the given variable was null.
     */
    public VariableRole roleOf(final Variable variable) {
        if (variable == null) {
            return null;
        }
        if (variable.isCoordinateSystemAxis()) {
            return VariableRole.AXIS;
        }
        int numVectors = 0;                                     // Number of dimension having more than 1 value.
        for (final Dimension dimension : variable.getGridDimensions()) {
            if (dimension.length() >= Grid.MIN_SPAN) {
                numVectors++;
            }
        }
        if (numVectors >= Grid.MIN_DIMENSION) {
            final DataType dataType = variable.getDataType();
            if (dataType.rasterDataType != DataBuffer.TYPE_UNDEFINED) {
                return VariableRole.COVERAGE;
            }
        }
        return VariableRole.OTHER;
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
     * @param  data  the variable for which the list of axis variables are desired, in CRS order.
     * @return names of the variables containing axis values, or {@code null} if this
     *         method performs applies no special convention for the given variable.
     */
    public String[] namesOfAxisVariables(Variable data) {
        return null;
    }

    /**
     * Returns the attribute-specified name of the dimension at the given index, or {@code null} if unspecified.
     * This is not the name of the dimension encoded in netCDF binary file format, but rather a name specified
     * by a customized attribute. This customized name can be used when the dimensions of the raster data are
     * not the same than the dimensions of the localization grid. In such case, the names returned by this method
     * are used for mapping the raster dimensions to the localization grid dimensions.
     *
     * <div class="note"><b>Example:</b>
     * consider the following netCDF file (simplified):
     *
     * {@preformat netcdf
     *   dimensions:
     *     grid_y =  161 ;
     *     grid_x =  126 ;
     *     data_y = 1599 ;
     *     data_x = 1250 ;
     *   variables:
     *     float Latitude(grid_y, grid_x) ;
     *       long_name = "Latitude (degree)" ;
     *       dim0 = "Line grids" ;
     *       dim1 = "Pixel grids" ;
     *       resampling_interval = 10 ;
     *     float Longitude(grid_y, grid_x) ;
     *       long_name = "Longitude (degree)" ;
     *       dim0 = "Line grids" ;
     *       dim1 = "Pixel grids" ;
     *       resampling_interval = 10 ;
     *     ushort SST(data_y, data_x) ;
     *       long_name = "Sea Surface Temperature" ;
     *       dim0 = "Line grids" ;
     *       dim1 = "Pixel grids" ;
     * }
     *
     * In this case, even if {@link #namesOfAxisVariables(Variable)} explicitly returns {@code {"Latitude", "Longitude"}}
     * we are still unable to associate the {@code SST} variable to those axes because they have no dimension in common.
     * However if we interpret {@code dim0} and {@code dim1} attributes as <cite>"Name of dimension 0"</cite> and
     * <cite>"Name of dimension 1"</cite> respectively, then we can associate the same dimension <strong>names</strong>
     * to all those variables: namely {@code "Line grids"} and {@code "Pixel grids"}. Using those names, we deduce that
     * the {@code (data_y, data_x)} dimensions in the {@code SST} variable are mapped to the {@code (grid_y, grid_x)}
     * dimensions in the localization grid.</div>
     *
     * This feature is an extension to CF-conventions.
     *
     * @param  dataOrAxis  the variable for which to get the attribute-specified name of the dimension.
     * @param  index       zero-based index of the dimension for which to get the name.
     * @return dimension name as specified by attributes, or {@code null} if none.
     */
    public String nameOfDimension(final Variable dataOrAxis, final int index) {
        return dataOrAxis.getAttributeAsString("dim" + index);
    }

    /**
     * Returns the factor by which to multiply a grid index in order to get the corresponding data index.
     * This is usually 1, meaning that there is an exact match between grid indices and data indices.
     * This value may be different than 1 if the localization grid is smaller than the data grid,
     * as documented in the {@link #nameOfDimension(Variable, int)}.
     *
     * <p>Default implementation returns the inverse of {@code "resampling_interval"} attribute value.
     * This feature is an extension to CF-conventions.</p>
     *
     * @param  axis  the axis for which to get the "grid indices to data indices" scale factor.
     * @return the "grid indices to data indices" scale factor, or {@link Double#NaN} if none.
     */
    public double gridToDataIndices(final Variable axis) {
        return axis.getAttributeAsNumber("resampling_interval");
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
     * @param  data  the variable to get valid range of values for.
     *               This is usually a variable containing raster data.
     * @return the range of valid values, or {@code null} if unknown.
     *
     * @see Variable#getRangeFallback()
     */
    public NumberRange<?> validRange(final Variable data) {
        Number minimum = null;
        Number maximum = null;
        Class<? extends Number> type = null;
        for (final String attribute : RANGE_ATTRIBUTES) {
            for (final Object element : data.getAttributeValues(attribute, true)) {
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
                if (rangeType >= data.getDataType().number &&
                    rangeType >= Math.max(Numbers.getEnumConstant(data.getAttributeType(CDM.SCALE_FACTOR)),
                                          Numbers.getEnumConstant(data.getAttributeType(CDM.ADD_OFFSET))))
                {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    final NumberRange<?> range = new MeasurementRange(type, minimum, true, maximum, true, data.getUnit());
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
     * This is a helper method for {@link #validRange(Variable)}.
     */
    @SuppressWarnings("unchecked")
    private static int compare(final Number n1, final Number n2) {
        return ((Comparable) n1).compareTo((Comparable) n2);
    }

    /**
     * Returns all no-data values declared for the given variable, or an empty map if none.
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
     *
     * @param  data  the variable for which to get no-data values.
     * @return no-data values with bitmask of their roles or textual descriptions.
     */
    public Map<Number,Object> nodataValues(final Variable data) {
        final Map<Number,Object> pads = new LinkedHashMap<>();
        for (int i=0; i < NODATA_ATTRIBUTES.length; i++) {
            for (final Object value : data.getAttributeValues(NODATA_ATTRIBUTES[i], true)) {
                if (value instanceof Number) {
                    pads.merge((Number) value, 1 << i, (v1, v2) -> ((Integer) v1) | ((Integer) v2));
                }
            }
        }
        return pads;
    }

    /**
     * Builds the function converting values from their packed formats in the variable to "real" values.
     * The transfer function is typically built from the {@code "scale_factor"} and {@code "add_offset"}
     * attributes associated to the given variable, but other conventions could use different attributes.
     * The returned function will be a component of the {@link org.apache.sis.coverage.SampleDimension}
     * to be created for each variable.
     *
     * <p>This method is invoked only if {@link #validRange(Variable)} returned a non-null value.
     * Since a transfer function is assumed to exist in such case (even if that function is identity),
     * this method shall never return {@code null}.</p>
     *
     * @param  data  the variable from which to determine the transfer function.
     *               This is usually a variable containing raster data.
     *
     * @return a transfer function built from the attributes defined in the given variable. Never null;
     *         if no information is found in the given {@code data} variable, then the return value
     *         shall be an identity function.
     */
    public TransferFunction transferFunction(final Variable data) {
        /*
         * If scale_factor and/or add_offset variable attributes are present, then this is
         * a "packed" variable. Otherwise the transfer function is the identity transform.
         */
        final TransferFunction tr = new TransferFunction();
        final double scale  = data.getAttributeAsNumber(CDM.SCALE_FACTOR);
        final double offset = data.getAttributeAsNumber(CDM.ADD_OFFSET);
        if (!Double.isNaN(scale))  tr.setScale (scale);
        if (!Double.isNaN(offset)) tr.setOffset(offset);
        return tr;
    }
}
