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

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.regex.Pattern;
import org.apache.sis.storage.netcdf.AttributeNames;
import org.apache.sis.internal.netcdf.Convention;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.VariableRole;
import org.apache.sis.internal.netcdf.Linearizer;
import org.apache.sis.referencing.operation.transform.TransferFunction;


/**
 * Global Change Observation Mission - Water (GCOM-W) conventions. This class provides customizations to the netCDF reader
 * for decoding <cite>Shizuku</cite> GCOM-W files produced by Japan Aerospace Exploration Agency (JAXA), version 3.
 * The file format is HDF5 and variables are like below (simplified):
 *
 * {@preformat text
 *     variables:
 *         short "Geophysical Data"(1976, 243, 2)
 *             float SCALE FACTOR = 0.01
 *             string UNIT = "C"
 *         float "Latitude of Observation Point"(1976, 243)
 *             string UNIT = "deg"
 *         float "Longitude of Observation Point"(1976, 243)
 *             string UNIT = "deg"
 *
 *     // global attributes:
 *         string :GeophysicalName = "Sea Surface Temperature"
 *         string :ProductName = "AMSR2-L2"
 *         string :ProductVersion = "3"
 *         string :ObservationStartDateTime = "2018-11-01T00:08:02.028Z"
 *         string :ObservationEndDateTime = "2018-11-01T00:57:24.247Z"
 *         string :PlatformShortName = "GCOM-W1" ;
 *         string :SensorShortName = "AMSR2" ;
 * }
 *
 * Observations:
 * <ul class="verbose">
 *   <li>There is no {@code convention} attribute, so we have to rely on something else for detecting this convention.</li>
 *   <li>The attributes do not specify the "no data" value. A look in sample files suggest that -32768 is used.
 *       This particularity is handled by {@link #nodataValues(Variable)}.</li>
 *   <li>The global attributes have different names than CF-Convention.
 *       This particularity is handled by {@link #mapAttributeName(String)}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see <a href="http://global.jaxa.jp/projects/sat/gcom_w/">SHIZUKU (GCOM-W) on JAXA</a>
 * @see <a href="https://en.wikipedia.org/wiki/Global_Change_Observation_Mission">GCOM on Wikipedia</a>
 *
 * @since 1.0
 * @module
 */
public final class GCOM_W extends Convention {
    /**
     * Sentinel value to search in the {@code "PlatformShortName"} attribute for determining if GCOM-W conventions apply.
     */
    private static final Pattern SENTINEL_VALUE = Pattern.compile(".*\\bGCOM-W1\\b.*");

    /**
     * Mapping from ACDD or CF-Convention attribute names to names of attributes used by GCOM-W.
     */
    private static final Map<String,String> ATTRIBUTES;
    static {
        final Map<String,String> m = new HashMap<>();
        m.put(AttributeNames.TITLE,               "ProductName");              // identification­Info / citation / title
        m.put(AttributeNames.PRODUCT_VERSION,     "ProductVersion");           // identification­Info / citation / edition
        m.put(AttributeNames.IDENTIFIER.TEXT,     "GranuleID");                // identification­Info / citation / identifier / code
        m.put(AttributeNames.DATE_CREATED,        "ProductionDateTime");       // identification­Info / citation / date
        m.put(AttributeNames.TIME.MINIMUM,        "ObservationStartDateTime"); // identification­Info / extent / temporal­Element / extent
        m.put(AttributeNames.TIME.MAXIMUM,        "ObservationEndDateTime");   // identification­Info / extent / temporal­Element / extent
        m.put(AttributeNames.CREATOR.INSTITUTION, "ProcessingCenter");         // identification­Info / citation / citedResponsibleParty
        m.put(AttributeNames.SUMMARY,             "GeophysicalName");          // identification­Info / abstract
        m.put(AttributeNames.PLATFORM.TEXT,       "PlatformShortName");        // acquisition­Information / platform / identifier
        m.put(AttributeNames.INSTRUMENT.TEXT,     "SensorShortName");          // acquisition­Information / platform / instrument / identifier
        m.put(AttributeNames.SOURCE,              "InputFileName");            // data­Quality­Info / lineage / source / description
        ATTRIBUTES = m;
    }

    /**
     * Names of variables to use as axes (first word only).
     */
    private static final String[] AXES = {
        "Latitude",
        "Longitude"
    };

    /**
     * "No data" value to use when not specified in the file.
     * This value has been determined by looking at the content of a sample file.
     */
    private static final short NO_DATA = -32768;

    /**
     * Creates a new instance of GCOM-W conventions.
     */
    public GCOM_W() {
    }

    /**
     * Detects if this set of conventions applies to the given netCDF file.
     * This methods fetches the {@code "PlatformShortName"} attribute,
     * which is expected to contain the following value:
     *
     * <blockquote>GCOM-W1</blockquote>
     *
     * @param  decoder  the netCDF file to test.
     * @return {@code true} if this set of conventions can apply.
     */
    @Override
    protected boolean isApplicableTo(final Decoder decoder) {
        final String s = decoder.stringValue("PlatformShortName");
        return (s != null) && SENTINEL_VALUE.matcher(s).matches();
    }

    /**
     * Returns the name of an attribute in this convention which is equivalent to the attribute of given name in CF-Convention.
     * The given parameter is a name from <cite>CF conventions</cite> or from <cite>Attribute Convention for Dataset Discovery
     * (ACDD)</cite>.
     *
     * @param  name  an attribute name from CF or ACDD convention.
     * @return the attribute name expected to be found in a netCDF file for GCOM-W, or {@code name} if unknown.
     */
    @Override
    public String mapAttributeName(final String name) {
        return ATTRIBUTES.getOrDefault(name, name);
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
            final String name = variable.getName();
            for (final String c : AXES) {
                if (name.regionMatches(true, 0, c, 0, c.length())) {
                    role = VariableRole.AXIS;
                    break;
                }
            }
        }
        return role;
    }


    // ┌────────────────────────────────────────────────────────────────────────────────────────────┐
    // │                                      COVERAGE DOMAIN                                       │
    // └────────────────────────────────────────────────────────────────────────────────────────────┘


    /**
     * Returns an enumeration of two-dimensional non-linear transforms that may be tried in attempts to make
     * localization grid more linear.
     *
     * @param  decoder  the netCDF file for which to determine linearizers that may possibly apply.
     * @return enumeration of two-dimensional non-linear transforms to try.
     */
    @Override
    public Set<Linearizer> linearizers(final Decoder decoder) {
        return Collections.singleton(Linearizer.GROUND_TRACK);
    }


    // ┌────────────────────────────────────────────────────────────────────────────────────────────┐
    // │                                       COVERAGE RANGE                                       │
    // └────────────────────────────────────────────────────────────────────────────────────────────┘


    /**
     * Returns no-data values declared for the given variable.
     *
     * @param  data  the variable for which to get no-data values.
     * @return no-data values with a flag specifying their role.
     */
    @Override
    public Map<Number,Object> nodataValues(final Variable data) {
        final Map<Number, Object> pads = super.nodataValues(data);
        if (pads.isEmpty() && roleOf(data) == VariableRole.COVERAGE) {
            pads.put(NO_DATA, 3);
            /*
             * Value 3 stands for:
             *   - bit 0 set: NO_DATA is a pad value (can be used as background).
             *   - bit 1 set: NO_DATA is a missing value.
             */
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
            final double slope  = data.getAttributeAsNumber("SCALE FACTOR");
            final double offset = data.getAttributeAsNumber("OFFSET");
            if (Double.isFinite(slope))  tr.setScale (slope);
            if (Double.isFinite(offset)) tr.setOffset(offset);
        }
        return tr;
    }
}
