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
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.awt.Color;
import ucar.nc2.constants.CDM;      // String constants are copied by the compiler with no UCAR reference left.
import ucar.nc2.constants.CF;       // idem
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.coverage.Category;
import org.apache.sis.image.privy.ColorModelFactory;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.Vector;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.system.Reflect;


/**
 * Extends the CF-Conventions with some conventions particular to a data producer.
 * By default, Apache SIS netCDF reader applies the <a href="http://cfconventions.org">CF conventions</a>.
 * But some data producers does not provides all necessary information for allowing Apache SIS to read the
 * netCDF file. Some information may be missing because considered implicit by the data producer.
 * This class provides a mechanism for supplying the implicit values. Conventions can be registered in
 * {@code module-info.java} as providers of the  {@code org.apache.sis.storage.netcdf.base.Convention} service.
 *
 * <p>Instances of this class must be immutable and thread-safe.
 * This class does not encapsulate all conventions needed for understanding a netCDF file,
 * but only conventions that are more likely to need to be overridden for some data producers.</p>
 *
 * <p><b>This is an experimental class for internal usage only (for now).</b>
 * The API of this class is likely to change in any future Apache SIS version.
 * This class may become public (in a modified form) in the future if we gain
 * enough experience about extending netCDF conventions.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-315">SIS-315</a>
 */
public class Convention {
    /**
     * All conventions found on the module path.
     */
    private static final ServiceLoader<Convention> AVAILABLES;
    static {
        ServiceLoader<Convention> loader;
        try {
            loader = ServiceLoader.load(Convention.class, Reflect.getContextClassLoader());
        } catch (SecurityException e) {
            Reflect.log(Convention.class, "<clinit>", e);
            loader = ServiceLoader.load(Convention.class);
        }
        AVAILABLES = loader;
    }

    /**
     * The convention to use when no specific conventions were found.
     */
    public static final Convention DEFAULT = new Convention();

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
        CDM.VALID_RANGE,    // Expected "reasonable" range for variable.
        "actual_range",     // Actual data range for variable.
        "valid_min",        // Fallback if "valid_range" is not specified.
        "valid_max"
    };

    /**
     * Names of attributes where to fetch missing or pad values. Order matter since it determines the bits to be set in the
     * map returned by {@link #nodataValues(Variable)}. The main bit is bit #0, which identifies the background value.
     */
    private static final String[] NODATA_ATTRIBUTES = {
        CDM.FILL_VALUE,         // Must be at index i=0 in order to get (1 << i) == PAD_VALUE_MASK.
        CDM.MISSING_VALUE       // Must be at index i=1 in order to get (1 << i) == MISSING_VALUE_MASK.
    };

    /**
     * Mask for pad values in the bits returned by {@link #nodataValues(Variable)}.
     * The difference with {@link #MISSING_VALUE_MASK} is that pad values may be used as background
     * values in regions outside the domain of validity, for example after a image reprojection.
     */
    protected static final int FILL_VALUE_MASK = 1;

    /**
     * Mask for missing values in the bits returned by {@link #nodataValues(Variable)}.
     */
    protected static final int MISSING_VALUE_MASK = 2;

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
     * <p>The returned names are conceptually a list. However, instead of returning a {@link java.util.List},
     * this method is invoked repeatedly with increasing index values until this method returns {@code null}.
     * Implementation are encouraged to return the {@code name} argument unchanged as the value at index 0.</p>
     *
     * <p>In current version of netCDF reader, this method is invoked only for global attributes,
     * not for the attributes on variables.</p>
     *
     * @param  name   an attribute name from CF or ACDD convention.
     * @param  index  index of the element to get from the list of names.
     * @return the attribute name expected to be found in a netCDF file structured according this {@code Convention},
     *         or {@code null} if there is no name mapping at the given index.
     */
    public String mapAttributeName(final String name, final int index) {
        return (index == 0) ? name : null;
    }

    /**
     * Returns whether the given variable is used as a coordinate system axis, a coverage or something else.
     * In particular this method shall return {@link VariableRole#AXIS} if the given variable seems to be a
     * coordinate system axis instead of the actual data. By netCDF convention, coordinate system axes
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
     *   <li>Excludes characters, strings and structures, which cannot be easily mapped to an image type.
     *       In addition, 2-dimensional character arrays are often used for annotations and we do not want
     *       to confuse them with images.</li>
     * </ul>
     *
     * <p>The default implementation returns {@link VariableRole#FEATURE_PROPERTY} if the given variable may be
     * values for one feature property of a feature set. This detection is based on the number of dimensions.</p>
     *
     * @param  variable  the variable for which to get the role.
     * @return role of the given variable.
     */
    public VariableRole roleOf(final Variable variable) {
        if (variable.isCoordinateSystemAxis()) {
            return VariableRole.AXIS;
        }
        final int n = variable.getNumDimensions();
        if (n == 1) {
            return VariableRole.FEATURE_PROPERTY;
        } else if (n != 0) {
            final DataType dataType = variable.getDataType();
            int numVectors = 0;                 // Number of dimension having more than 1 value.
            for (final Dimension dimension : variable.getGridDimensions()) {
                if (dimension.length() >= Grid.MIN_SPAN) {
                    numVectors++;
                }
            }
            if (numVectors >= Grid.MIN_DIMENSION) {
                if (dataType.rasterDataType != null) {
                    return VariableRole.COVERAGE;
                }
            }
            if (n == Variable.STRING_DIMENSION && dataType == DataType.CHAR) {
                return VariableRole.FEATURE_PROPERTY;
            }
        }
        return VariableRole.OTHER;
    }


    // ┌────────────────────────────────────────────────────────────────────────────────────────────┐
    // │                                      COVERAGE DOMAIN                                       │
    // └────────────────────────────────────────────────────────────────────────────────────────────┘


    /**
     * Returns the names of the variables containing data for all dimension of a variable.
     * Each netCDF variable can have an arbitrary number of dimensions identified by their name.
     * The data for a dimension are usually stored in a variable of the same name, but not always.
     * This method gives an opportunity for subclasses to select the axis variables using other criterion.
     * This happen for example if a netCDF file defines two grids for the same dimensions.
     * The order in returned array will be the axis order in the Coordinate Reference System.
     *
     * <p>This information is normally provided by the {@value ucar.nc2.constants.CF#COORDINATES} attribute,
     * which is processed by the UCAR library (which is why we do not read this attribute ourselves here).
     * This method is provided as a fallback when no such attribute is found.
     * The default implementation returns {@code null}.</p>
     *
     * @param  data  the variable for which the list of axis variables are desired, in CRS order.
     * @return names of the variables containing axis values, or {@code null} if this
     *         method applies no special convention for the given variable.
     */
    public String[] namesOfAxisVariables(Variable data) {
        return null;
    }

    /**
     * Returns the attribute-specified name of the dimension at the given index, or {@code null} if unspecified.
     * This is not the name of the dimension encoded in netCDF binary file format, but rather a name specified
     * by a customized attribute. This customized name can be used when the dimensions of the raster data are
     * not the same as the dimensions of the localization grid. In such case, the names returned by this method
     * are used for mapping the raster dimensions to the localization grid dimensions.
     *
     * <p>This feature is an extension to CF-conventions.</p>
     *
     * <h4>Example</h4>
     * Consider the following netCDF file (simplified):
     *
     * <pre class="text">
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
     *       dim1 = "Pixel grids" ;</pre>
     *
     * In this case, even if {@link #namesOfAxisVariables(Variable)} explicitly returns {@code {"Latitude", "Longitude"}}
     * we are still unable to associate the {@code SST} variable to those axes because they have no dimension in common.
     * However if we interpret {@code dim0} and {@code dim1} attributes as <q>Name of dimension 0</q> and
     * <q>Name of dimension 1</q> respectively, then we can associate the same dimension <strong>names</strong>
     * to all those variables: namely {@code "Line grids"} and {@code "Pixel grids"}. Using those names, we deduce that
     * the {@code (data_y, data_x)} dimensions in the {@code SST} variable are mapped to the {@code (grid_y, grid_x)}
     * dimensions in the localization grid.
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
     * <p>Default implementation returns the {@code "resampling_interval"} attribute value.
     * This feature is an extension to CF-conventions.</p>
     *
     * @param  axis  the axis for which to get the "grid indices to data indices" scale factor.
     * @return the "grid indices to data indices" scale factor, or {@link Double#NaN} if none.
     */
    public double gridToDataIndices(final Variable axis) {
        return axis.getAttributeAsDouble("resampling_interval");
    }

    /**
     * Returns an enumeration of two-dimensional non-linear transforms (usually map projections) that may result
     * in more linear localization grids. The enumerated transforms will be tested in "trials and errors" and the
     * one resulting in best {@linkplain org.apache.sis.math.Plane#fit linear regression correlation coefficients}
     * will be selected.
     *
     * <p>If the returned set is non-empty, exactly one of the linearizers will be applied. If not applying any
     * linearizer is an acceptable solution, then an identity linearizer should be explicitly returned.</p>
     *
     * <p>The returned set shall not contain two linearizers of the same {@linkplain Linearizer#type type}
     * because the types (not the full linearizers) are used in keys for caching localization grids.</p>
     *
     * <p>Default implementation returns an empty set.</p>
     *
     * @param  decoder  the netCDF file for which to get linearizer candidates.
     * @return enumeration of two-dimensional non-linear transforms to try on the localization grid.
     *
     * @see #defaultHorizontalCRS(boolean)
     */
    public Set<Linearizer> linearizers(final Decoder decoder) {
        return Set.of();
    }

    /**
     * Returns the name of nodes (variables or groups) that may define the map projection parameters.
     * The variables or groups will be inspected in the order they are declared in the returned set.
     * For each string in the set, {@link Decoder#findNode(String)} is invoked and the return value
     * (if non-null) is given to {@link #projection(Node)} until a non-null map is obtained.
     *
     * <p>The default implementation returns the value of {@link CF#GRID_MAPPING} attribute, or an empty set
     * if the given variable does not contain that attribute. Subclasses may override for example if grid
     * mapping information are hard-coded in a particular node for a specific product.</p>
     *
     * <h4>API note</h4>
     * This method name is singular because even if a set is returned, in the end only one value is used.
     *
     * @param  data  the variable for which to get the grid mapping node.
     * @return name of nodes that may contain the grid mapping, or an empty set if none.
     */
    public Set<String> nameOfMappingNode(final Variable data) {
        final String mapping = data.getAttributeAsString(CF.GRID_MAPPING);
        return (mapping != null) ? Set.of(mapping) : Set.of();
    }

    /**
     * The {@value} attribute name from CF-convention, defined here because not yet provided in {@link CF}.
     * Associated value shall be an instance of {@link Number}. This field may be removed in a future SIS
     * version if this constant become defined in {@link ucar.nc2.constants}.
     */
    protected static final String LONGITUDE_OF_PRIME_MERIDIAN = "longitude_of_prime_meridian";

    /**
     * Suffix of all attribute names used for CRS component names.
     */
    static final String NAME_SUFFIX = "_name";

    /**
     * The {@value} attribute name from CF-convention, defined here because not yet provided in {@link CF}.
     * Associated value shall be an instance of {@link String}. This field may be removed in a future SIS
     * version if this constant become defined in {@link ucar.nc2.constants}.
     */
    protected static final String ELLIPSOID_NAME      = "reference_ellipsoid_name",
                                  PRIME_MERIDIAN_NAME = "prime_meridian_name",
                                  GEODETIC_DATUM_NAME = "horizontal_datum_name",
                                  GEOGRAPHIC_CRS_NAME = "geographic_crs_name",
                                  PROJECTED_CRS_NAME  = "projected_crs_name";

    /**
     * The {@value} attribute name, not yet part of CF-convention.
     */
    protected static final String CONVERSION_NAME = "conversion_name";

    /**
     * The {@value} attribute name from CF-convention, defined here because not yet provided in {@link CF}.
     * Associated value shall be an instance of {@link BursaWolfParameters}.
     */
    protected static final String TOWGS84 = "towgs84";

    /**
     * Returns the map projection defined by the given node. The given {@code node} argument is one of the nodes
     * named by {@link #nameOfMappingNode(Variable)} (typically a variable referenced by {@value CF#GRID_MAPPING}
     * attribute on the data variable), or if no grid mapping attribute is found {@code node} may be directly the
     * data variable (not a CF-compliant approach, but found in practice). If non-null, the returned map contains
     * the following information ({@value CF#GRID_MAPPING_NAME} is mandatory, all other entries are optional):
     *
     * <table class="sis">
     *   <caption>Content of the returned map</caption>
     *   <tr>
     *     <th>Key</th>
     *     <th>Value type</th>
     *     <th>Description</th>
     *   </tr><tr>
     *     <td>{@value CF#GRID_MAPPING_NAME}</td>
     *     <td>{@link String}</td>
     *     <td>Operation method name <strong>(mandatory)</strong></td>
     *   </tr><tr>
     *     <td>{@code "*_name"}</td>
     *     <td>{@link String}</td>
     *     <td>Name of a component (datum, base CRS, …)</td>
     *   </tr><tr>
     *     <td>{@value #LONGITUDE_OF_PRIME_MERIDIAN}</td>
     *     <td>{@link Number}</td>
     *     <td>Value in degrees relative to reference meridian.</td>
     *   </tr><tr>
     *     <td>(projection-dependent)</td>
     *     <td>{@link Number} or {@code double[]}</td>
     *     <td>Map projection parameter values</td>
     *   </tr><tr>
     *     <td>{@value CF#SEMI_MAJOR_AXIS} and {@value CF#SEMI_MINOR_AXIS}</td>
     *     <td>{@link Number}</td>
     *     <td>Ellipsoid axis lengths.</td>
     *   </tr><tr>
     *     <td>{@value #TOWGS84}</td>
     *     <td>{@link BursaWolfParameters}</td>
     *     <td>Datum shift information.</td>
     *   </tr>
     * </table>
     *
     * The returned map must be modifiable for allowing callers to modify its content.
     *
     * @param  node  the {@value CF#GRID_MAPPING} variable (preferred) or the data variable (as a fallback) from which to read attributes.
     * @return the map projection definition as a modifiable map, or {@code null} if none.
     *
     * @see <a href="http://cfconventions.org/cf-conventions/cf-conventions.html#grid-mappings-and-projections">CF-conventions</a>
     */
    public Map<String,Object> projection(final Node node) {
        final String method = node.getAttributeAsString(CF.GRID_MAPPING_NAME);
        if (method == null) {
            return null;
        }
        final var definition = new LinkedHashMap<String,Object>();
        definition.put(CF.GRID_MAPPING_NAME, method);
        for (final String name : node.getAttributeNames()) try {
            final String nameLC = name.toLowerCase(Decoder.DATA_LOCALE);
            Object value;
            switch (nameLC) {
                case CF.GRID_MAPPING_NAME: continue;        // Already stored.
                case TOWGS84: {
                    /*
                     * Conversion to WGS 84 datum may be specified as Bursa-Wolf parameters. Encoding this information
                     * with the CRS is deprecated (the hard-coded WGS84 target datum is not always suitable) but still
                     * a common practice as of 2019. We require at least the 3 translation parameters.
                     */
                    final Vector values = node.getAttributeAsVector(name);
                    if (values == null || values.size() < 3) continue;
                    final var bp = new BursaWolfParameters(CommonCRS.WGS84.datum(), null);
                    bp.setValues(values.doubleValues());
                    value = bp;
                    break;
                }
                default: {
                    if (nameLC.endsWith(NAME_SUFFIX)) {
                        value = node.getAttributeAsString(name);
                        if (value == null) continue;
                    } else {
                        /*
                         * Assume that all map projection parameters in netCDF files are numbers or array of numbers.
                         * If values are array, then they are converted to an array of {@code double[]} type.
                         */
                        value = node.getAttributeValue(name);
                        if (value == null) continue;
                        if (value instanceof Vector) {
                            value = ((Vector) value).doubleValues();
                        }
                    }
                    break;
                }
            }
            if (definition.putIfAbsent(nameLC, value) != null) {
                node.error(Convention.class, "projection", null, Errors.Keys.DuplicatedIdentifier_1, name);
            }
        } catch (NumberFormatException e) {
            // May happen in the vector contains number stored as texts.
            node.decoder.illegalAttributeValue(name, node.getAttributeAsString(name), e);
        }
        return definition;
    }

    /**
     * Returns the <i>grid to CRS</i> transform for the given node. This method is invoked after call
     * to {@link #projection(Node)} method resulted in creation of a projected coordinate reference system.
     * The {@linkplain ProjectedCRS#getBaseCRS() base CRS} is fixed to (latitude, longitude) axes in degrees,
     * but the projected CRS axes may have any order and units. In the particular case of "latitude_longitude"
     * pseudo-projection, the "projected" CRS is actually a {@link GeographicCRS} instance.
     * The returned transform, if non-null, shall map cell corners.
     *
     * <p>The default implementation returns {@code null}.</p>
     *
     * <h4>API notes</h4>
     * <ul>
     *   <li>We do not provide a {@link ProjectedCRS} argument because of the {@code "latitude_longitude"} special case.</li>
     *   <li>Base CRS axis order is (latitude, longitude) for increasing the chances to have a CRS identified by EPSG.</li>
     * </ul>
     *
     * @param  node       the same node as the one given to {@link #projection(Node)}.
     * @param  baseToCRS  conversion from (latitude, longitude) in degrees to the projected CRS.
     * @return the <i>grid corner to CRS</i> transform, or {@code null} if none or unknown.
     * @throws TransformException if a coordinate operation was required but failed.
     */
    public MathTransform gridToCRS(final Node node, final MathTransform baseToCRS) throws TransformException {
        return null;
    }

    /**
     * Returns an identification of default geodetic components to use if no corresponding information is found in the
     * netCDF file. The default implementation returns <q>Unknown datum based upon the GRS 1980 ellipsoid</q>.
     * Note that the GRS 1980 ellipsoid is close to WGS 84 ellipsoid.
     *
     * <h4>Maintenance note</h4>
     * If this default is changed, search also for "GRS 1980" strings in {@link CRSBuilder} class.
     *
     * @param  spherical  whether to restrict the ellipsoid to a sphere.
     * @return information about geodetic objects to use if no explicit information is found in the file.
     */
    public CommonCRS defaultHorizontalCRS(final boolean spherical) {
        return spherical ? CommonCRS.SPHERE : CommonCRS.GRS1980;
    }


    // ┌────────────────────────────────────────────────────────────────────────────────────────────┐
    // │                                       COVERAGE RANGE                                       │
    // └────────────────────────────────────────────────────────────────────────────────────────────┘

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
     * An heuristic rule is documented in {@link ucar.nc2.dataset.EnhanceScaleMissingUnsigned} interface.
     * If both types of range are available, then this method should return the range of packed value.
     * Otherwise if this method returns the range of real values, then that range shall be an instance
     * of {@link MeasurementRange} for allowing the caller to distinguish the two cases.
     *
     * @param  data  the variable to get valid range of values for (usually a variable containing raster data).
     * @return the range of valid values, or {@code null} if unknown.
     *
     * @see Variable#getRangeFallback()
     */
    public NumberRange<?> validRange(final Variable data) {
        Number minimum = null;
        Number maximum = null;
        Class<? extends Number> type = null;
        for (final String attribute : RANGE_ATTRIBUTES) {
            final Vector values = data.getAttributeAsVector(attribute);
            if (values != null) {
                final int length = values.size();
                for (int i=0; i<length; i++) try {
                    Number value = values.get(i);           // May throw NumberFormatException if value was stored as text.
                    if (value instanceof Float) {
                        final float fp = (Float) value;
                        if      (fp == +Float.MAX_VALUE) value = Float.POSITIVE_INFINITY;
                        else if (fp == -Float.MAX_VALUE) value = Float.NEGATIVE_INFINITY;
                    } else if (value instanceof Double) {
                        final double fp = (Double) value;
                        if      (fp == +Double.MAX_VALUE) value = Double.POSITIVE_INFINITY;
                        else if (fp == -Double.MAX_VALUE) value = Double.NEGATIVE_INFINITY;
                    }
                    type    = Numbers.widestClass(type, value.getClass());
                    minimum = Numbers.cast(minimum, type);
                    maximum = Numbers.cast(maximum, type);
                    value   = Numbers.cast(value,   type);
                    if (!attribute.endsWith("max") && (minimum == null || compare(value, minimum) < 0)) minimum = value;
                    if (!attribute.endsWith("min") && (maximum == null || compare(value, maximum) > 0)) maximum = value;
                } catch (NumberFormatException e) {
                    data.decoder.illegalAttributeValue(attribute, values.stringValue(i), e);
                }
            }
            /*
             * Stop the loop and return a range as soon as we have enough information.
             * Note that we may loop over many attributes before to complete information.
             */
            if (minimum != null && maximum != null) {
                /*
                 * Heuristic rule defined in UCAR documentation (see EnhanceScaleMissingUnsigned):
                 * if the type of the range is equal to the type of the scale, and the type of the
                 * data is not wider, then assume that the minimum and maximum are real values.
                 */
                final Class<?> scaleType  = data.getAttributeType(CDM.SCALE_FACTOR);
                final Class<?> offsetType = data.getAttributeType(CDM.ADD_OFFSET);
                final int rangeType = Numbers.getEnumConstant(type);
                if ((scaleType != null || offsetType != null)
                        && rangeType >= data.getDataType().number
                        && rangeType >= Math.max(Numbers.getEnumConstant(scaleType),
                                                 Numbers.getEnumConstant(offsetType)))
                {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    final NumberRange<?> range = new MeasurementRange(type, minimum, true, maximum, true, data.getUnit());
                    return range;
                } else {
                    /*
                     * At this point, we determined that the range uses sample values (i.e. values before
                     * conversion to the unit of measurement). Before to return that range, check if the
                     * minimum or maximum value overlaps with a "no data" value. If yes, resolve the
                     * overlapping by making a range bound exclusive instead of inclusive.
                     */
                    boolean isMinIncluded = true;
                    boolean isMaxIncluded = true;
                    final Set<Number> nodataValues = data.getNodataValues().keySet();
                    if (!nodataValues.isEmpty()) {
                        final double minValue = minimum.doubleValue();
                        final double maxValue = maximum.doubleValue();
                        for (final Number pad : nodataValues) {
                            final double value = pad.doubleValue();
                            isMinIncluded &= (minValue != value);
                            isMaxIncluded &= (maxValue != value);
                        }
                    }
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    final NumberRange<?> range = new NumberRange(type, minimum, isMinIncluded, maximum, isMaxIncluded);
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
     *   <li>If bit 0 is set (mask {@value #FILL_VALUE_MASK}), then the value is a pad value. Those values can be used for background.</li>
     *   <li>If bit 1 is set (mask {@value #MISSING_VALUE_MASK}), then the value is a missing value.</li>
     * </ul>
     *
     * Pad values should be first in the map, followed by missing values.
     * The same value may have more than one role.
     *
     * <p>The default implementation returns a modifiable {@link LinkedHashMap}.
     * Subclasses can add their own entries to the returned map.</p>
     *
     * @param  data  the variable for which to get no-data values.
     * @return no-data values with bitmask of their roles or textual descriptions.
     */
    public Map<Number,Object> nodataValues(final Variable data) {
        final var pads = new LinkedHashMap<Number,Object>();
        for (int i=0; i < NODATA_ATTRIBUTES.length; i++) {
            final String name = NODATA_ATTRIBUTES[i];
            final Vector values = data.getAttributeAsVector(name);
            if (values != null) {
                final int length = values.size();
                for (int j=0; j<length; j++) try {
                    pads.merge(values.get(j), 1 << i, (v1, v2) -> ((Integer) v1) | ((Integer) v2));
                } catch (NumberFormatException e) {
                    data.decoder.illegalAttributeValue(name, values.stringValue(i), e);
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
     * <p>This method is invoked in contexts where a transfer function is assumed to exist, for example
     * because {@link #validRange(Variable)} returned a non-null value. Consequently, this method shall
     * never return {@code null}, but can return the identity function.</p>
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
        final var tr = new TransferFunction();
        final double scale  = data.getAttributeAsDouble(CDM.SCALE_FACTOR);
        final double offset = data.getAttributeAsDouble(CDM.ADD_OFFSET);
        if (!Double.isNaN(scale))  tr.setScale (scale);
        if (!Double.isNaN(offset)) tr.setOffset(offset);
        return tr;
    }

    /**
     * Returns the unit of measurement to use as a fallback if it cannot be determined in a standard way.
     * Default implementation returns {@code null}. Subclasses can override if the unit can be determined
     * in a way specific to this convention.
     *
     * @param  data  the variable for which to get the unit of measurement.
     * @return the unit of measurement, or {@code null} if none or unknown.
     * @throws MeasurementParseException if the unit symbol cannot be parsed.
     */
    public Unit<?> getUnitFallback(final Variable data) throws MeasurementParseException {
        return null;
    }

    /**
     * Returns the band to use for defining pixel colors when the image is displayed on screen.
     * All other bands, if any, will exist in the raster but be ignored at display time.
     * The default value is 0, the first (and often only) band.
     *
     * @return the band on which {@link #getColors(Variable)} will apply.
     */
    public int getVisibleBand() {
        return ColorModelFactory.DEFAULT_VISIBLE_BAND;
    }

    /**
     * Returns the colors to use for each category, or {@code null} for the default colors.
     *
     * @param  data  the variable for which to get the colors.
     * @return colors to use for each category, or {@code null} for the default.
     */
    public Function<Category,Color[]> getColors(final Variable data) {
        return null;
    }
}
