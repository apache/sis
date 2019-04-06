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

import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.VariableEnhanced;
import org.apache.sis.internal.netcdf.Convention;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.VariableRole;
import org.apache.sis.util.CharSequences;


/**
 * A UCAR coordinate system builder which uses Apache SIS mechanism for identifying
 * which variable may be an axis variable.
 *
 * <div class="note"><b>Note:</b>
 * this could could be registered as a {@link ucar.nc2.dataset.CoordSysBuilderIF} service
 * and automatically loaded by the UCAR library using {@link java.util.ServiceLoader}.
 * The UCAR library then invoke the following method by reflection:
 *
 * {@preformat java
 *     public static boolean isMine(NetcdfFile file) {…}
 * }
 *
 * However we rather create instances of this class explicitly when required in order to
 * avoid interfering with UCAR global configuration (users want to apply their own settings)
 * and because we need to specify the {@link DecoderWrapper}.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see <a href="https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/tutorial/CoordSysBuilder.html">UCAR tutorial</a>
 *
 * @since 1.0
 * @module
 */
final class CSBuilderFallback extends CoordSysBuilder {
    /**
     * The decoder for which to apply this fallback.
     */
    private final DecoderWrapper decoder;

    /**
     * Creates a new UCAR coordinate system builder for the given decoder.
     */
    CSBuilderFallback(final DecoderWrapper decoder) {
        this.decoder = decoder;
    }

    /**
     * Delegates to {@link Convention#roleOf(Variable)} in order to determine which variables are axes.
     */
    @Override
    protected void findCoordinateAxes(final NetcdfDataset ds) {
        for (final VarProcess vp : varList) {
            if (!vp.isCoordinateVariable) {
                final VariableWrapper variable = decoder.getWrapperFor(vp.v);
                if (variable.getRole() == VariableRole.AXIS) {
                    vp.isCoordinateVariable = true;
                }
            }
        }
        super.findCoordinateAxes(ds);
    }

    /**
     * Identifies what kind of axis the given variable is.
     */
    @Override
    protected AxisType getAxisType(final NetcdfDataset ds, final VariableEnhanced variable) {
        final String name = variable.getShortName();
        if (CharSequences.startsWith(name, "Longitude", true)) return AxisType.Lon;
        if (CharSequences.startsWith(name, "Latitude",  true)) return AxisType.Lat;
        return super.getAxisType(ds, variable);
    }
}
