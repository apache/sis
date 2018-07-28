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
package org.apache.sis.internal.referencing.provider;

import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.Static;


/**
 * Constants for projections defined by ESRI but not by EPSG.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class ESRI extends Static {
    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    static final ParameterDescriptor<Double> CENTRAL_MERIDIAN;

    /**
     * The operation parameter descriptor for the <cite>False easting</cite> (FE) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    static final ParameterDescriptor<Double> FALSE_EASTING;

    /**
     * The operation parameter descriptor for the <cite>False northing</cite> (FN) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    static final ParameterDescriptor<Double> FALSE_NORTHING;
    static {
        final ParameterBuilder builder = MapProjection.builder();
        FALSE_EASTING  = asPrimary(Equirectangular.FALSE_EASTING,  builder);
        FALSE_NORTHING = asPrimary(Equirectangular.FALSE_NORTHING, builder);

        final ParameterDescriptor<Double> template = Equirectangular.LONGITUDE_OF_ORIGIN;
        CENTRAL_MERIDIAN = MapProjection.createLongitude(builder
                .addName(MapProjection.sameNameAs(Citations.ESRI,  template))
                .addName(MapProjection.sameNameAs(Citations.OGC,   template))
                .addName(MapProjection.sameNameAs(Citations.PROJ4, template)));
    }

    /**
     * Do not allow instantiation of this class.
     */
    private ESRI() {
    }

    /**
     * Returns the same parameter than the given one, except that the alias of the ESRI authority
     * is promoted as the primary name. The old primary name and identifiers (which are usually the
     * EPSG ones) are discarded.
     *
     * @param  template  the parameter from which to copy the names and identifiers.
     * @param  builder   an initially clean builder where to add the names.
     * @return the given {@code builder}, for method call chaining.
     */
    @SuppressWarnings("unchecked")
    static ParameterDescriptor<Double> asPrimary(final ParameterDescriptor<Double> template, final ParameterBuilder builder) {
        return MapProjection.alternativeAuthority(template, Citations.ESRI, builder).createBounded((MeasurementRange<Double>)
                ((DefaultParameterDescriptor<Double>) template).getValueDomain(), template.getDefaultValue());
    }
}
