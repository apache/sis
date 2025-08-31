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

import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.referencing.privy.GeodeticObjectBuilder;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.crs.AbstractCRS;


/**
 * Merges the CRS declared in grid mapping attributes with the CRS inferred from coordinate variables.
 * The former (called "explicit CRS") may have map projection parameters that are difficult to infer
 * from the coordinate variables. The latter (called "implicit CRS") have better name and all required
 * dimensions, while the explicit CRS often has only the horizontal dimensions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CRSMerger extends GeodeticObjectBuilder {
    /**
     * Creates a new builder for the given netCDF reader.
     */
    CRSMerger(final Decoder decoder) {
        super(decoder, decoder.listeners.getLocale());
    }

    /**
     * Replaces the component starting at given index by the given component, possibly with adjusted longitude range.
     * The implicit CRS has been inferred from coordinate variables, while the explicit CRS has been inferred from
     * the grid mapping attributes. The explicit CRS is the one to use, but its longitude range is the default one
     * because thar range depends on the coordinate variable, which was inspected by the implicit CRS.
     *
     * @param  implicit        the coordinate reference system in which to replace a component.
     * @param  firstDimension  index of the first dimension to replace.
     * @param  explicit        the component to insert in place of the CRS component at given index.
     * @return a CRS with the component replaced.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateReferenceSystem replaceComponent(final CoordinateReferenceSystem implicit,
            final int firstDimension, CoordinateReferenceSystem explicit) throws FactoryException
    {
        final CoordinateSystem cs = implicit.getCoordinateSystem();
        if (cs instanceof EllipsoidalCS) {
            final int i = AxisDirections.indexOfColinear(cs, AxisDirection.EAST);
            if (i >= 0 && cs.getAxis(i).getMinimumValue() >= 0) {       // The `i >= 0` check is paranoiac.
                explicit = AbstractCRS.castOrCopy(explicit).forConvention(AxesConvention.POSITIVE_RANGE);
            }
        }
        final CoordinateReferenceSystem result = super.replaceComponent(implicit, firstDimension, explicit);
        return CRS.equivalent(implicit, result) ? implicit : result;
    }
}
