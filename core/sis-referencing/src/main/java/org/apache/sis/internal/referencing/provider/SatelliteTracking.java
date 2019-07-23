/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sis.internal.referencing.provider;

import javax.xml.bind.annotation.XmlTransient;
import org.apache.sis.measure.Units;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.projection.ConicSatelliteTracking;
import org.apache.sis.referencing.operation.projection.CylindricalSatelliteTracking;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;

/**
 * The provider for <cite>Satellite-Tracking projections"</cite>.
 *
 * <cite>
 * - All groundtracks for satellites orbiting the Earth with the same orbital
 * parameters are shown as straight lines on the map.
 *
 * - Cylindrical {@link CylindricalSatelliteTracking} or conical
 * {@link ConicSatelliteTracking} form available.
 *
 * - Neither conformal nor equal-area.
 *
 * - All meridians are equally spaced straight lines, parallel on cylindrical
 * form and converging to a common point on conical form.
 *
 * - All parallels are straight and parallel on cylindrical form and are
 * concentric circular arcs on conical form. Parallels are unequally spaced.
 *
 * - Conformality occurs along two chosen parallels. Scale is correct along one
 * of these parameters on the conical form and along both on the cylindrical
 * form.
 *
 * Developed 1977 by Snyder
 * </cite>
 *
 * <cite> These formulas are confined to circular orbits and the SPHERICAL
 * Earth.</cite>
 *
 * <cite>The ascending and descending groundtracks meet at the northern an
 * southern tracking limits, lats. 80.9°N and S for landsat 1, 2 and 3. The map
 * Projection does not extend closer to the poles.</cite>
 *
 * This projection method has no associated EPSG code.
 *
 * Earth radius is normalized. Its value is 1 and is'nt an input parameter.
 *
 * =============================================================================
 * REMARK : The parameters associated with the satellite (and its orbit) could
 * be aggregate in class of the kind : Satellite or SatelliteOrbit.
 * =============================================================================
 *
 * @see <cite>Map Projections - A Working Manual</cite> By John P. Snyder
 * @author Matthieu Bastianelli (Geomatys)
 * @version 1.0
 */
@XmlTransient
public class SatelliteTracking extends MapProjection {

    /**
     * Name of this projection.
     */
    public static final String NAME = "Satellite-Tracking";

    /**
     * The operation parameter descriptor for the <cite>Longitude of projection
     * center</cite> (λ₀) parameter value. Valid values range is [-180 … 180]°
     * and default value is 0°.
     */
    public static final ParameterDescriptor<Double> CENTRAL_MERIDIAN = ESRI.CENTRAL_MERIDIAN;

    /**
     * The operation parameter descriptor for the <cite>Latitude of 1st standard
     * parallel</cite>. For conical satellite-tracking projection : first
     * parallel of conformality with true scale.
     *
     * Valid values range is [-90 … 90]° and default value is the value given to
     * the {@link #LATITUDE_OF_FALSE_ORIGIN} parameter.
     */
    public static final ParameterDescriptor<Double> STANDARD_PARALLEL_1 = LambertConformal2SP.STANDARD_PARALLEL_1;

    /**
     * The operation parameter descriptor for the <cite>second parallel of
     * conformality but without true scale</cite> parameter value for conic
     * projection. Valid values range is [-90 … 90]° and default value is the
     * opposite value given to the {@link #STANDARD_PARALLEL_1} parameter.
     */
    public static final ParameterDescriptor<Double> STANDARD_PARALLEL_2 = LambertConformal2SP.STANDARD_PARALLEL_2;

    /**
     * Latitude Crossing the central meridian at the desired origin of
     * rectangular coordinates.
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN = TransverseMercator.LATITUDE_OF_ORIGIN;;

    /**
     * The operation parameter descriptor for the <cite> Angle of inclination
     * between the plane of the Earth's Equator and the plane of the satellite
     * orbit</cite> parameter value. <cite> It's measured counterclockwise from
     * the Equatorto the orbit plane at the ascending node (99.092° for Landsat
     * 1, 2, 3</cite>
     */
    public static final ParameterDescriptor<Double> SATELLITE_ORBIT_INCLINATION;

    /**
     * The operation parameter descriptor for the <cite> time required for
     * revolution of the satellite</cite> parameter value.
     */
    public static final ParameterDescriptor<Double> SATELLITE_ORBITAL_PERIOD;

    /**
     * The operation parameter descriptor for the <cite> length of earth's
     * rotation with respect to the precessed ascending node</cite> parameter
     * value. The ascending node is the point on the satellite orbit at which
     * the satellite crosses the Earth's equatorial plane in a northerly
     * direction.
     */
    public static final ParameterDescriptor<Double> ASCENDING_NODE_PERIOD; //Constant for landsat but is it the case for every satellite with circular orbital?

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setCodeSpace(null, null)
                .setRequired(true);

        SATELLITE_ORBIT_INCLINATION = builder
                .addName("satellite_orbit_inclination")
                .setDescription("Angle of inclination between the plane of the Earth's Equator and the plane of the satellite orbit")
                .create(0, Units.RADIAN);

        SATELLITE_ORBITAL_PERIOD = builder
                .addName("satellite_orbital_period")
                .setDescription("Time required for revolution of the satellite")
                .createStrictlyPositive(103.267, Units.MINUTE); //Or hours? Seconds?

        ASCENDING_NODE_PERIOD = builder
                .addName("ascending_node_period")
                .setDescription("Length of Earth's rotation with respect to the precessed ascending node")
                .createStrictlyPositive(98.884, Units.MINUTE);
                
        PARAMETERS = builder.addName(NAME)
                .createGroupForMapProjection(CENTRAL_MERIDIAN,
                        STANDARD_PARALLEL_1, STANDARD_PARALLEL_2,
                        SATELLITE_ORBIT_INCLINATION, SATELLITE_ORBITAL_PERIOD,
                        ASCENDING_NODE_PERIOD, LATITUDE_OF_ORIGIN);

    }

    /**
     * Constructs a new provider.
     */
    public SatelliteTracking() {
        super(PARAMETERS);
    }

    /**
     * {@inheritDoc}
     *
     * @return the map projection created from the given parameter values.
     */
    @Override
    protected NormalizedProjection createProjection(final Parameters parameters) throws ParameterNotFoundException {
        ArgumentChecks.ensureNonNull("Parameters", parameters);

        if (parameters.getValue(STANDARD_PARALLEL_2) == -parameters.getValue(STANDARD_PARALLEL_1)) { 
            return new org.apache.sis.referencing.operation.projection.CylindricalSatelliteTracking(this, parameters);
        } else {
            return new org.apache.sis.referencing.operation.projection.ConicSatelliteTracking(this, parameters);
        }
    }

}
