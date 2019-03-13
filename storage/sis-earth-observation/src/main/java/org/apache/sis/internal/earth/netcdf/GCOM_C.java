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
package org.apache.sis.internal.earth.netcdf;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.apache.sis.storage.netcdf.AttributeNames;
import org.apache.sis.internal.netcdf.Convention;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.VariableRole;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.measure.NumberRange;


/**
 * Global Change Observation Mission - Climate (GCOM-C) conventions. This class provides customizations to the netCDF reader
 * for decoding <cite>Shikisai</cite> GCOM-C files produced by Japan Aerospace Exploration Agency (JAXA), version 1.00.
 * The file format is HDF5 and variables are like below (simplified):
 *
 * {@preformat text
 *     group: Geometry_data {
 *         variables:
 *             float Latitude(161, 126)
 *             string Unit = "degree"
 *             string Dim0 = "Line grids"
 *             string Dim1 = "Pixel grids"
 *             int Resampling_interval = 10
 *         float Longitude(161, 126)
 *             string Unit = "degree"
 *             string Dim0 = "Line grids"
 *             string Dim1 = "Pixel grids"
 *             int Resampling_interval = 10
 *     }
 *     group: Image_data {
 *         variables:
 *             ushort SST(1599, 1250)                   // Note: different size than (latitude, longitude) variables.
 *             string dim0 = "Line grids"
 *             string dim1 = "Piexl grids"              // Note: typo in "Pixel"
 *             ushort Error_DN           = 65535
 *             ushort Land_DN            = 65534
 *             ushort Cloud_error_DN     = 65533
 *             ushort Retrieval_error_DN = 65532
 *             ushort Maximum_valid_DN   = 65531
 *             ushort Minimum_valid_DN   = 0
 *             float  Slope              = 0.0012
 *             float  Offset             = -10
 *             string Unit               = "degree"
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
 *     }
 * }
 *
 * Observations:
 * <ul class="verbose">
 *   <li>There is no {@code convention} attribute, so we have to rely on something else for detecting this convention.</li>
 *   <li>The size of latitude and longitude variables is not the same than the size of image data.
 *       This particularity is handled by {@link #gridToDataIndices(Variable)}.</li>
 *   <li>The {@code dim0} and {@code dim1} attribute names in image data have a different case
 *       than the attributes in longitude and latitude variables. Furthermore a value contains a typo.
 *       This particularity is handled by {@link #nameOfDimension(Variable, int)}.</li>
 *   <li>The {@code Slope} and {@code Offset} attribute names are different than the names defined in CF-Convention.
 *       This particularity is handled by {@link #transferFunction(Variable)}.</li>
 *   <li>There is more than one sentinel values for missing values.
 *       All attribute names for missing values have {@code "_DN"} suffix.
 *       This particularity is handled by {@link #nodataValues(Variable)}.</li>
 *   <li>The global attributes have different names than CF-Convention.
 *       This particularity is handled by {@link #mapAttributeName(String)}.</li>
 * </ul>
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see <a href="http://global.jaxa.jp/projects/sat/gcom_c/">SHIKISAI (GCOM-C) on JAXA</a>
 * @see <a href="https://en.wikipedia.org/wiki/Global_Change_Observation_Mission">GCOM on Wikipedia</a>
 *
 * @since 1.0
 * @module
 */
public final class GCOM_C extends Convention {
    /**
     * Sentinel value to search in the {@code "Satellite"} attribute for determining if GCOM-C conventions apply.
     */
    private static final Pattern SENTINEL_VALUE = Pattern.compile(".*\\bGCOM-C\\b.*");

    /**
     * Mapping from ACDD or CF-Convention attribute names to names of attributes used by GCOM-C.
     */
    private static final Map<String,String> ATTRIBUTES;
    static {
        final Map<String,String> m = new HashMap<>();
        m.put(AttributeNames.TITLE,               "Product_name");             // identification­Info / citation / title
        m.put(AttributeNames.PRODUCT_VERSION,     "Product_version");          // identification­Info / citation / edition
        m.put(AttributeNames.IDENTIFIER.TEXT,     "Product_file_name");        // identification­Info / citation / identifier / code
        m.put(AttributeNames.DATE_CREATED,        "Processing_UT");            // identification­Info / citation / date
        m.put(AttributeNames.CREATOR.INSTITUTION, "Processing_organization");  // identification­Info / citation / citedResponsibleParty
        m.put(AttributeNames.SUMMARY,             "Dataset_description");      // identification­Info / abstract
        m.put(AttributeNames.PLATFORM.TEXT,       "Satellite");                // acquisition­Information / platform / identifier
        m.put(AttributeNames.INSTRUMENT.TEXT,     "Sensor");                   // acquisition­Information / platform / instrument / identifier
        m.put(AttributeNames.PROCESSING_LEVEL,    "Product_level");            // content­Info / processing­Level­Code
        m.put(AttributeNames.SOURCE,              "Input_files");              // data­Quality­Info / lineage / source / description
        m.put(AttributeNames.TIME.MINIMUM,        "Scene_start_time");         // identification­Info / extent / temporal­Element / extent
        m.put(AttributeNames.TIME.MAXIMUM,        "Scene_end_time");           // identification­Info / extent / temporal­Element / extent
        ATTRIBUTES = m;
    }

    /**
     * Names of attributes for sample values having "no-data" meaning.
     * All those names have {@value #SUFFIX} suffix.
     */
    private static final String[] NO_DATA = {
        "Error_DN",
        "Land_DN",
        "Cloud_error_DN",
        "Retrieval_error_DN"
    };

    /**
     * Suffix of all attribute names enumerated in {@link #NO_DATA}.
     */
    private static final String SUFFIX = "_DN";

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
     * @param  name  an attribute name from CF or ACDD convention.
     * @return the attribute name expected to be found in a netCDF file for GCOM-C, or {@code name} if unknown.
     */
    @Override
    public String mapAttributeName(final String name) {
        return ATTRIBUTES.getOrDefault(name, name);
    }

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
            if ("QA_flag".equals(dataOrAxis.getName())) {       // Missing Dim0 and Dim1 for this variable in GCOM-C version 1.00.
                switch (index) {
                    case 0: name = "Line grids";  break;
                    case 1: name = "Pixel grids"; break;
                }
            }
        } else if ("Piexl grids".equalsIgnoreCase(name)) {      // Typo in GCOM-C version 1.00.
            name = "Pixel grids";
        }
        return name;
    }

    /**
     * Returns whether the given variable is used as a coordinate system axis, a coverage or something else.
     *
     * @param  variable  the variable for which to get the role, or {@code null}.
     * @return role of the given variable, or {@code null} if the given variable was null.
     */
    @Override
    public VariableRole roleOf(final Variable variable) {
        VariableRole role = super.roleOf(variable);
        if (role == VariableRole.COVERAGE) {
            /*
             * Exclude (for now) some variables associated to longitude and latitude: Obs_time, Sensor_zenith, Solar_zenith.
             * In a future version we should probably keep them but store them in their own resource aggregate.
             */
            final String group = variable.getGroupName();
            if ("Geometry_data".equalsIgnoreCase(group)) {
                role = VariableRole.OTHER;
            }
        }
        return role;
    }

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
            final double min = data.getAttributeAsNumber("Minimum_valid_DN");
            final double max = data.getAttributeAsNumber("Maximum_valid_DN");
            if (Double.isFinite(min) && Double.isFinite(max)) {
                range = NumberRange.createBestFit(min, true, max, true);
            }
        }
        return range;
    }

    /**
     * Returns all no-data values declared for the given variable, or an empty map if none.
     * The map keys are the no-data values (pad sample values or missing sample values).
     * The map values are {@link String} instances containing the description of the no-data value.
     *
     * @param  data  the variable for which to get no-data values.
     * @return no-data values with textual descriptions.
     */
    @Override
    public Map<Number,Object> nodataValues(final Variable data) {
        final Map<Number, Object> pads = super.nodataValues(data);
        for (String name : NO_DATA) {
            final double value = data.getAttributeAsNumber(name);
            if (Double.isFinite(value)) {
                if (name.endsWith(SUFFIX)) {
                    name = name.substring(0, name.length() - SUFFIX.length());
                }
                pads.put(value, name.replace('_', ' '));
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
            final double slope  = data.getAttributeAsNumber("Slope");
            final double offset = data.getAttributeAsNumber("Offset");
            if (Double.isFinite(slope))  tr.setScale (slope);
            if (Double.isFinite(offset)) tr.setOffset(offset);
        }
        return tr;
    }
}
