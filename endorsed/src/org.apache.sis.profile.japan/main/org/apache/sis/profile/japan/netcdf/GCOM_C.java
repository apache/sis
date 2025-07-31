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
package org.apache.sis.profile.japan.netcdf;

import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;
import java.util.function.Function;
import java.awt.Color;
import ucar.nc2.constants.CF;
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.storage.netcdf.AttributeNames;
import org.apache.sis.storage.netcdf.base.Convention;
import org.apache.sis.storage.netcdf.base.Decoder;
import org.apache.sis.storage.netcdf.base.Variable;
import org.apache.sis.storage.netcdf.base.VariableRole;
import org.apache.sis.storage.netcdf.base.Linearizer;
import org.apache.sis.storage.netcdf.base.Node;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.provider.PseudoSinusoidal;
import org.apache.sis.referencing.operation.provider.Equirectangular;
import org.apache.sis.referencing.operation.provider.PolarStereographicA;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.coverage.Category;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Workaround;


/**
 * Global Change Observation Mission - Climate (GCOM-C) conventions. This class provides customizations to the netCDF reader
 * for decoding <cite>Shikisai</cite> GCOM-C files produced by Japan Aerospace Exploration Agency (JAXA), version 1.00.
 * The file format is HDF5 and variables are like below (simplified):
 *
 * <pre class="text">
 *     group: Geometry_data {
 *         variables:
 *             float Latitude(161, 126)
 *                 string Unit = "degree"
 *                 string Dim0 = "Line grids"
 *                 string Dim1 = "Pixel grids"
 *                 int Resampling_interval = 10
 *             float Longitude(161, 126)
 *                 string Unit = "degree"
 *                 string Dim0 = "Line grids"
 *                 string Dim1 = "Pixel grids"
 *                 int Resampling_interval = 10
 *     }
 *     group: Image_data {
 *         variables:
 *             ushort SST(1599, 1250)                   // Note: different size than (latitude, longitude) variables.
 *                 string dim0 = "Line grids"
 *                 string dim1 = "Piexl grids"          // Note: typo in "Pixel"
 *                 ushort Error_DN           = 65535
 *                 ushort Land_DN            = 65534
 *                 ushort Cloud_error_DN     = 65533
 *                 ushort Retrieval_error_DN = 65532
 *                 ushort Maximum_valid_DN   = 65531
 *                 ushort Minimum_valid_DN   = 0
 *                 float  Slope              = 0.0012
 *                 float  Offset             = -10
 *                 string Unit               = "degree"
 *     }
 *     group: Global_attributes {
 *         string :Algorithm_developer = "Japan Aerospace Exploration Agency (JAXA)"
 *         string :Dataset_description = "Sea surface temperatures determined by using the TIR 1 and 2 data of SGLI"
 *         string :Satellite           = "Global Change Observation Mission - Climate (GCOM-C)"
 *         string :Scene_start_time    = "20181201 09:14:16.797"
 *         string :Scene_end_time      = "20181201 09:18:11.980"
 *     }
 *     group: Processing_attributes {
 *         string :Contact_point = "JAXA/Earth Observation Research Center (EORC)"
 *         string :Processing_organization = "JAXA/GCOM-C science project"
 *         string :Processing_UT = "20181202 04:42:09"
 *     }</pre>
 *
 * Observations:
 * <ul class="verbose">
 *   <li>There is no {@code convention} attribute, so we have to rely on something else for detecting this convention.</li>
 *   <li>The size of latitude and longitude variables is not the same as the size of image data.
 *       This particularity is handled by {@link #gridToDataIndices(Variable)}.</li>
 *   <li>The {@code dim0} and {@code dim1} attribute names in image data have a different case
 *       than the attributes in longitude and latitude variables. Furthermore, a value contains a typo.
 *       This particularity is handled by {@link #nameOfDimension(Variable, int)}.</li>
 *   <li>The {@code Slope} and {@code Offset} attribute names are different than the names defined in CF-Convention.
 *       This particularity is handled by {@link #transferFunction(Variable)}.</li>
 *   <li>There is more than one sentinel values for missing values.
 *       All attribute names for missing values have {@code "_DN"} suffix.
 *       This particularity is handled by {@link #nodataValues(Variable)}.</li>
 *   <li>The global attributes have different names than CF-Convention.
 *       This particularity is handled by {@link #mapAttributeName(String, int)}.</li>
 * </ul>
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="http://global.jaxa.jp/projects/sat/gcom_c/">SHIKISAI (GCOM-C) on JAXA</a>
 * @see <a href="https://en.wikipedia.org/wiki/Global_Change_Observation_Mission">GCOM on Wikipedia</a>
 */
public final class GCOM_C extends Convention {
    /**
     * Sentinel value to search in the {@code "Satellite"} attribute for determining if GCOM-C conventions apply.
     */
    private static final Pattern SENTINEL_VALUE = Pattern.compile(".*\\bGCOM-C\\b.*");

    /**
     * Name of the variable storing data quality flags.
     */
    private static final String QA_FLAG = "QA_flag";

    /**
     * Mapping from ACDD or CF-Convention attribute names to names of attributes used by GCOM-C.
     * This map does not include attributes for geographic extent because the "Lower_left_latitude",
     * "Lower_left_longitude", "Lower_right_latitude", <i>etc.</i> attributes are difficult to use.
     * They are corners in the grid with no clear relationship with "real world" West and East bounds.
     * We have no way to detect anti-meridian spanning (the {@code left > right} test is useless) and
     * the minimal latitude may be in the middle of a border. Consequently, a bounding box made from
     * the corner minimal and maximal coordinates is not guaranteed to encompass the whole data,
     * and may even contain no data at all.
     */
    private static final Map<String,String> ATTRIBUTES = Map.ofEntries(
        Map.entry(AttributeNames.TITLE,               "Product_name"),              // identification­Info / citation / title
        Map.entry(AttributeNames.PRODUCT_VERSION,     "Product_version"),           // identification­Info / citation / edition
        Map.entry(AttributeNames.IDENTIFIER.TEXT,     "Product_file_name"),         // identification­Info / citation / identifier / code
        Map.entry(AttributeNames.DATE_CREATED,        "Processing_UT"),             // identification­Info / citation / date
        Map.entry(AttributeNames.CREATOR.INSTITUTION, "Processing_organization"),   // identification­Info / citation / citedResponsibleParty
        Map.entry(AttributeNames.SUMMARY,             "Dataset_description"),       // identification­Info / abstract
        Map.entry(AttributeNames.PLATFORM.TEXT,       "Satellite"),                 // acquisition­Information / platform / identifier
        Map.entry(AttributeNames.INSTRUMENT.TEXT,     "Sensor"),                    // acquisition­Information / platform / instrument / identifier
        Map.entry(AttributeNames.PROCESSING_LEVEL,    "Product_level"),             // content­Info / processing­Level­Code
        Map.entry(AttributeNames.SOURCE,              "Input_files"),               // data­Quality­Info / lineage / source / description
        Map.entry(AttributeNames.TIME.MINIMUM,        "Scene_start_time"),          // identification­Info / extent / temporal­Element / extent
        Map.entry(AttributeNames.TIME.MAXIMUM,        "Scene_end_time"));           // identification­Info / extent / temporal­Element / extent

    /**
     * Name of the group defining map projection parameters, localization grid, <i>etc</i>.
     */
    private static final String GEOMETRY_DATA = "Geometry_data";

    /**
     * Names of attributes for sample values having "no-data" meaning.
     * Pad values should be first, followed by missing values.
     * All those names have the {@value #SUFFIX} suffix.
     *
     * @see #nodataValues(Variable)
     */
    private static final String[] NO_DATA = {
        "Error_DN",                                 // Must be first: will be used as "no data" value.
        "Retrieval_error_DN",                       // First fallback if "Error_DN" is not found.
        "Cloud_error_DN",
        "Land_DN"
    };

    /**
     * Names of attributes for minimum and maximum valid sample values.
     */
    private static final String MINIMUM = "Minimum_valid_DN",
                                MAXIMUM = "Maximum_valid_DN";

    /**
     * Suffix of all attribute names enumerated in {@link #NO_DATA}.
     */
    private static final String SUFFIX = "_DN";

    /**
     * A "No data" value which should be present in all GCOM files but appear to be missing.
     * All values in the range {@value} to {@code 0xFFFF} will be "no data", unless the range
     * of valid values overlap.
     *
     * <p>This hack may be removed after VGI data files fixed their missing "no data" attribute.</p>
     */
    @Workaround(library = "Vegetation Index (VGI)", version = "3 (2021)")
    private static final int MISSING_NODATA = 0xFFFE;

    /**
     * Creates a new instance of GCOM-C conventions.
     */
    public GCOM_C() {
    }

    /**
     * Detects if this set of conventions applies to the given netCDF file.
     * This methods fetches the {@code "Global_attributes/Satellite"} attribute,
     * which is expected to contain the following value:
     *
     * <blockquote>Global Change Observation Mission - Climate (GCOM-C)</blockquote>
     *
     * We test only for presence of {@code "GCOM-C"} in order to allow
     * for some variations in exact text content.
     *
     * @param  decoder  the netCDF file to test.
     * @return {@code true} if this set of conventions can apply.
     */
    @Override
    protected boolean isApplicableTo(final Decoder decoder) {
        final String[] path = decoder.getSearchPath();
        decoder.setSearchPath("Global_attributes");
        final String s = decoder.stringValue("Satellite");
        decoder.setSearchPath(path);                            // Must reset the decoder in its original state.
        return (s != null) && SENTINEL_VALUE.matcher(s).matches();
    }

    /**
     * Specifies a list of groups where to search for named attributes, in preference order.
     * This is used for ISO 19115 metadata. The {@code null} name stands for the root group.
     *
     * @return  name of groups where to search for global attributes, in preference order.
     */
    @Override
    public String[] getSearchPath() {
        return new String[] {"Global_attributes", null, "Processing_attributes"};
    }

    /**
     * Returns the name of an attribute in this convention which is equivalent to the attribute of given name in CF-Convention.
     * The given parameter is a name from <cite>CF conventions</cite> or from <cite>Attribute Convention for Dataset Discovery
     * (ACDD)</cite>.
     *
     * @param  name   an attribute name from CF or ACDD convention.
     * @param  index  index of the element to get from the list of names.
     * @return the attribute name expected to be found in a netCDF file for GCOM-C, or {@code null} if none.
     */
    @Override
    public String mapAttributeName(final String name, final int index) {
        switch (index) {
            case 0: return name;
            case 1: return ATTRIBUTES.get(name);
            case 2: {
                if (AttributeNames.TIME.MINIMUM.equalsIgnoreCase(name)) return "Image_start_time";
                if (AttributeNames.TIME.MAXIMUM.equalsIgnoreCase(name)) return "Image_end_time";
                break;
            }
        }
        return null;
    }

    /**
     * Returns whether the given variable is used as a coordinate system axis, a coverage or something else.
     *
     * @param  variable  the variable for which to get the role, or {@code null}.
     * @return role of the given variable, or {@code null} if the given variable was null.
     */
    @Override
    public VariableRole roleOf(final Variable variable) {
        final VariableRole role = super.roleOf(variable);
        if (role == VariableRole.COVERAGE) {
            /*
             * Exclude (for now) some variables associated to longitude and latitude: Obs_time, Sensor_zenith, Solar_zenith.
             * In a future version we should probably keep them but store them in their own resource aggregate.
             */
            final String group = variable.getGroupName();
            if (GEOMETRY_DATA.equalsIgnoreCase(group)) {
                return VariableRole.OTHER;
            }
            if (QA_FLAG.equals(variable.getName())) {
                return VariableRole.DISCRETE_COVERAGE;
            }
        }
        return role;
    }


    // ┌────────────────────────────────────────────────────────────────────────────────────────────┐
    // │                                      COVERAGE DOMAIN                                       │
    // └────────────────────────────────────────────────────────────────────────────────────────────┘


    /**
     * Returns the attribute-specified name of the dimension at the given index, or {@code null} if unspecified.
     * See {@link Convention#nameOfDimension(Variable, int)} for a more detailed explanation of this information.
     * The implementation in this class fixes a typo found in some {@code "Dim1"} attribute values and generates
     * the values when they are known to be missing.
     *
     * @param  dataOrAxis  the variable for which to get the attribute-specified name of the dimension.
     * @param  index       zero-based index of the dimension for which to get the name.
     * @return dimension name as specified by attributes, or {@code null} if none.
     */
    @Override
    public String nameOfDimension(final Variable dataOrAxis, final int index) {
        String name = super.nameOfDimension(dataOrAxis, index);
        if (name == null) {
            if (QA_FLAG.equals(dataOrAxis.getName())) {
                /*
                 * The "QA_flag" variable is missing "Dim0" and "Dim1" attribute in GCOM-C version 1.00.
                 * However, not all GCOM-C files use a localization grid. We use the presence of spatial
                 * resolution attribute as a sentinel value for now.
                 */
                if (dataOrAxis.getAttributeType("Spatial_resolution") != null) {
                    switch (index) {
                        case 0: name = "Line grids";  break;
                        case 1: name = "Pixel grids"; break;
                    }
                }
            }
        } else if ("Piexl grids".equalsIgnoreCase(name)) {      // Typo in GCOM-C version 1.00.
            name = "Pixel grids";
        }
        return name;
    }

    /**
     * Returns the two-dimensional non-linear transforms to apply for making the localization grid more linear.
     * This method returns a singleton without specifying the identity transform as an acceptable alternative.
     * It means that the specified projection (UTM) is considered mandatory for this format.
     *
     * @param  decoder  the netCDF file for which to determine linearizers that may possibly apply.
     * @return enumeration of two-dimensional non-linear transforms to apply.
     *
     * @see #defaultHorizontalCRS(boolean)
     */
    @Override
    public Set<Linearizer> linearizers(final Decoder decoder) {
        return Set.of(new Linearizer(CommonCRS.WGS84, Linearizer.Type.UNIVERSAL));
    }

    /**
     * Returns the name of nodes (variables or groups) that may define the map projection parameters.
     * For GCOM files, this is {@value #GEOMETRY_DATA}.
     *
     * @param  data  the variable for which to get the grid mapping node.
     * @return name of nodes that may contain the grid mapping, or an empty set if none.
     */
    @Override
    public Set<String> nameOfMappingNode(final Variable data) {
        final var names = new LinkedHashSet<String>(4);
        names.add(GEOMETRY_DATA);
        names.addAll(super.nameOfMappingNode(data));            // Fallback if geometry data does not exist.
        return names;
    }

    /**
     * Returns the map projection definition for the given data variable.
     * This method expects the following attribute names in the {@value #GEOMETRY_DATA} group:
     *
     * <pre class="text">
     *     group: Geometry_data {
     *         // group attributes:
     *         string Image_projection      = "EQA (sinusoidal equal area) projection from 0-deg longitude"
     *         float  Upper_left_longitude  = 115.17541
     *         float  Upper_left_latitude   =  80.0
     *         float  Upper_right_longitude = 172.7631
     *         float  Upper_right_latitude  =  80.0
     *         float  Lower_left_longitude  =  58.47609
     *         float  Lower_left_latitude   =  70.0
     *         float  Lower_right_longitude =  87.714134
     *         float  Lower_right_latitude  =  70.0
     *     }</pre>
     *
     * @param  node  the group of variables from which to read attributes.
     * @return the map projection definition as a modifiable map, or {@code null} if none.
     */
    @Override
    public Map<String,Object> projection(final Node node) {
        final String name = node.getAttributeAsString("Image_projection");
        if (name == null) {
            return super.projection(node);
        }
        final String method;
        final int s = name.indexOf(' ');
        final String code = (s >= 0) ? name.substring(0, s) : name;
        if (code.equalsIgnoreCase("EQA")) {
            method = PseudoSinusoidal.NAME;
        } else if (code.equalsIgnoreCase("EQR")) {
            method = Equirectangular.NAME;
        } else if (code.equalsIgnoreCase("PS")) {
            method = PolarStereographicA.NAME;
        } else {
            return super.projection(node);
        }
        final var definition = new HashMap<String,Object>(4);
        definition.put(CF.GRID_MAPPING_NAME, method);
        definition.put(CONVERSION_NAME, name);
        return definition;
    }

    /**
     * The attributes storing values of the 4 corners in degrees with (latitude, longitude) axis order.
     * This is used by {@link #projection(Node)} for inferring a "grid to CRS" transform.
     */
    private static final String[] CORNERS = {
        "Upper_left_latitude",
        "Upper_left_longitude",
        "Upper_right_latitude",
        "Upper_right_longitude",
        "Lower_left_latitude",
        "Lower_left_longitude",
        "Lower_right_latitude",
        "Lower_right_longitude"
    };

    /**
     * Returns the <i>grid to CRS</i> transform for the given node.
     * This method is invoked after call to {@link #projection(Node)} resulted in creation of a projected CRS.
     * The {@linkplain ProjectedCRS#getBaseCRS() base CRS} shall have (latitude, longitude) axes in degrees.
     *
     * @param  node       the same node as the one given to {@link #projection(Node)}.
     * @param  baseToCRS  conversion from (latitude, longitude) in degrees to the projected CRS.
     * @return the "grid corner to CRS" transform, or {@code null} if none or unknown.
     * @throws TransformException if a coordinate operation was required but failed.
     */
    @Override
    public MathTransform gridToCRS(final Node node, final MathTransform baseToCRS) throws TransformException {
        final double[] corners = new double[CORNERS.length];
        for (int i=0; i<corners.length; i++) {
            corners[i] = node.getAttributeAsDouble(CORNERS[i]);
        }
        baseToCRS.transform(corners, 0, corners, 0, corners.length / 2);
        /*
         * Compute spans of data (typically in metres) as the average of the spans on both sides
         * (width as length of top and bottom edges, height as length of left and right edges).
         * This code assumes (easting, northing) axes — this is currently not verified.
         */
        double sx = ((corners[2] - corners[0]) + (corners[6] - corners[4])) / 2;
        double sy = ((corners[1] - corners[5]) + (corners[3] - corners[7])) / 2;
        /*
         * Transform the spans into pixel sizes (resolution), then build the transform.
         */
        sx /= (node.getAttributeAsDouble("Number_of_pixels") - 1);
        sy /= (node.getAttributeAsDouble("Number_of_lines")  - 1);
        if (Double.isFinite(sx) && Double.isFinite(sy)) {
            final Matrix3 m = new Matrix3();
            m.m00 =  sx;
            m.m11 = -sy;
            m.m02 = corners[0];
            m.m12 = corners[1];
            return MathTransforms.linear(m);
        }
        return super.gridToCRS(node, baseToCRS);
    }

    /**
     * Returns the default prime meridian, ellipsoid, datum or CRS to use if no information is found in the netCDF file.
     * GCOM documentation said that the datum is WGS 84.
     *
     * @param  spherical  ignored, since we assume an ellipsoid in all cases.
     * @return information about geodetic objects to use if no explicit information is found in the file.
     */
    @Override
    public CommonCRS defaultHorizontalCRS(final boolean spherical) {
        return CommonCRS.WGS84;
    }


    // ┌────────────────────────────────────────────────────────────────────────────────────────────┐
    // │                                       COVERAGE RANGE                                       │
    // └────────────────────────────────────────────────────────────────────────────────────────────┘


    /**
     * Returns the range of valid values, or {@code null} if unknown.
     *
     * @param  data  the variable to get valid range of values for.
     * @return the range of valid values, or {@code null} if unknown.
     */
    @Override
    public NumberRange<?> validRange(final Variable data) {
        NumberRange<?> range = super.validRange(data);
        if (range == null) {
            final Number min = data.getAttributeAsNumber(MINIMUM);
            final Number max = data.getAttributeAsNumber(MAXIMUM);
            if (min != null || max != null) {
                range = NumberRange.createBestFit(min, true, max, true);
            }
        }
        return range;
    }

    /**
     * Returns all no-data values declared for the given variable, or an empty map if none.
     * The map keys are the no-data values (pad sample values or missing sample values).
     * The map values are {@link String} instances containing the description of the no-data value,
     * except for {@code "Error_DN"} which is used as a more generic pad value.
     *
     * @param  data  the variable for which to get no-data values.
     * @return no-data values with textual descriptions.
     */
    @Override
    public Map<Number,Object> nodataValues(final Variable data) {
        boolean addMissingNodata = true;
        final Map<Number, Object> pads = super.nodataValues(data);
        for (int i=0; i<NO_DATA.length; i++) {
            String name = NO_DATA[i];
            final Number value = data.getAttributeAsNumber(name);
            if (value != null) {
                final Object label;
                if (i != 0) {
                    if (name.endsWith(SUFFIX)) {
                        name = name.substring(0, name.length() - SUFFIX.length());
                    }
                    label = name.replace('_', ' ');
                } else {
                    label = FILL_VALUE_MASK | MISSING_VALUE_MASK;
                }
                if (pads.putIfAbsent(value, label) == null && addMissingNodata) {
                    addMissingNodata = Math.floor(value.doubleValue()) > MISSING_NODATA;
                }
            }
        }
        /*
         * Workaround for missing "no data" attribute in VGI files. As of September 2022, GCOM files have a
         * "Land_DN" attribute for missing data caused by land, but no "Sea_DN" attribute for the converse.
         * As an heuristic rule, if valid values are short integers and "no data" values are either absent
         * of greater than `MISSING_NODATA`, add "missing values" category for all values up to 0xFFFF.
         */
        if (addMissingNodata) {
            final double valid = data.getAttributeAsDouble(MAXIMUM);
            if (valid >= 0x100 && valid < MISSING_NODATA) {
                int label = FILL_VALUE_MASK | MISSING_VALUE_MASK;
                for (int value=0xFFFF; value >= MISSING_NODATA; value--) {
                    pads.putIfAbsent(value, label);
                    label = MISSING_VALUE_MASK;
                }
            }
        }
        return pads;
    }

    /**
     * Builds the function converting values from their packed formats in the variable to "real" values.
     * This method is invoked only if {@link #validRange(Variable)} returned a non-null value.
     *
     * @param  data  the variable from which to determine the transfer function.
     * @return a transfer function built from the attributes defined in the given variable.
     */
    @Override
    public TransferFunction transferFunction(final Variable data) {
        final TransferFunction tr = super.transferFunction(data);
        if (tr.isIdentity()) {
            final double slope  = data.getAttributeAsDouble("Slope");
            final double offset = data.getAttributeAsDouble("Offset");
            if (Double.isFinite(slope))  tr.setScale (slope);
            if (Double.isFinite(offset)) tr.setOffset(offset);
        }
        return tr;
    }

    /**
     * Returns the unit of measurement to use as a fallback if it cannot be determined in a standard way.
     *
     * @param  data  the variable for which to get the unit of measurement.
     * @return the unit of measurement, or {@code null} if none or unknown.
     * @throws MeasurementParseException if the unit symbol cannot be parsed.
     */
    @Override
    public Unit<?> getUnitFallback(final Variable data) throws MeasurementParseException {
        if ("Image_data".equals(data.getGroupName())) {
            final String symbol = data.getAttributeAsString("Unit");
            if (symbol != null && !symbol.equalsIgnoreCase("NA")) {
                if (symbol.equalsIgnoreCase("degree")) {
                    return Units.CELSIUS;
                }
                return Units.valueOf(symbol);
            }
        }
        return super.getUnitFallback(data);
    }

    /**
     * Returns the colors to use for each category, or {@code null} for the default colors.
     *
     * @param  data  the variable for which to get the colors.
     * @return colors to use for each category, or {@code null} for the default.
     */
    @Override
    public Function<Category,Color[]> getColors(final Variable data) {
        if (QA_FLAG.equals(data.getName())) {
            return (category) -> {
                final NumberRange<?> range = category.getSampleRange();
                if (Objects.equals(range.getMinValue(), range.getMaxValue())) {
                    return null;        // A "no data" value.
                }
                /*
                 * Following colors are not really appropriate for "QA_flag" because that variable is a bitmask
                 * rather than a continuous coverage. Following code may be replaced by a better color palette
                 * in a future version.
                 */
                return new Color[] {
                    Color.BLUE, Color.CYAN, Color.YELLOW, Color.RED
                };
            };
        }
        return super.getColors(data);
    }
}
