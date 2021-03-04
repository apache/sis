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

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import org.apache.sis.util.CharSequences;


/**
 * Factories to be registered as service providers to the UCAR netCDF library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class FactoryForUCAR implements CoordSystemBuilderFactory {
    /**
     * Creates a new factory.
     */
    public FactoryForUCAR() {
    }

    /**
     * Returns a name for the convention recognized by this package.
     * This name may change in any future version.
     *
     * @return a name for the convention recognized by this package.
     */
    @Override
    public String getConventionName() {
        return "GCOM";
    }

    /**
     * Returns whether the given file seems to be a GCOM-W file.
     *
     * @param  file  the file to test.
     * @return whether the file seems supported by this factory.
     */
    @Override
    public boolean isMine(final NetcdfFile file) {
        final Attribute at = file.findGlobalAttributeIgnoreCase(GCOM_W.SENTINEL_ATTRIBUTE);
        if (at != null) {
            final String s = at.getStringValue();
            if (s != null) {
                return GCOM_W.SENTINEL_VALUE.matcher(s).matches();
            }
        }
        return false;
    }

    /**
     * Creates a coordinate system builder for the given dataset builder.
     *
     * @param  dsb  the dataset builder.
     * @return the coordinate system builder for the given dataset.
     */
    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder dsb) {         // TODO: add <?> with UCAR netCDF 6.
        return new CSBuilder(dsb);
    }

    /**
     * A UCAR coordinate system builder for GCOM-W data.
     */
    private static final class CSBuilder extends CoordSystemBuilder {
        /**
         * Creates a new UCAR coordinate system builder.
         */
        CSBuilder(final NetcdfDataset.Builder<?> dsb) {
            super(dsb);
        }

        /**
         * Applies {@link GCOM_W#roleOf GCOM-W} rules for determining which variables are axes.
         */
        @Override
        protected void identifyCoordinateAxes() {
            for (final VarProcess vp : varList) {
                if (!vp.isCoordinateVariable) {
                    vp.isCoordinateVariable = GCOM_W.isCoordinateAxis(vp.vb.shortName);
                }
            }
            super.identifyCoordinateAxes();
        }

        /**
         * Identifies what kind of axis the given variable is.
         * Only called for variables already identified as Coordinate Axes.
         */
        @Override
        protected AxisType getAxisType(final VariableDS.Builder variable) {       // TODO: add <?> with UCAR netCDF 6.
            final AxisType type = super.getAxisType(variable);
            if (type == null) {
                final String name = variable.shortName;
                if (CharSequences.startsWith(name, GCOM_W.LONGITUDE, true)) return AxisType.Lon;
                if (CharSequences.startsWith(name, GCOM_W.LATITUDE,  true)) return AxisType.Lat;
            }
            return type;
        }
    }
}
