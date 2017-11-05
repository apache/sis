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

import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Longitude;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.operation.projection.ZonedGridSystem;


/**
 * The provider for <cite>"Transverse Mercator Zoned Grid System"</cite> projection (EPSG:9824).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@XmlTransient
public final class ZonedTransverseMercator extends AbstractProvider {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4555131921419380461L;

    /**
     * The operation parameter descriptor for the <cite>Initial longitude</cite> (λ₁) parameter value.
     */
    public static final ParameterDescriptor<Double> INITIAL_LONGITUDE;

    /**
     * The operation parameter descriptor for the <cite>Zone width</cite> (W) parameter value.
     */
    public static final ParameterDescriptor<Double> ZONE_WIDTH;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        INITIAL_LONGITUDE = builder.addIdentifier("8830").addName("Initial longitude")
                .createBounded(Longitude.MIN_VALUE, Longitude.MAX_VALUE, Longitude.MIN_VALUE, Units.DEGREE);

        ZONE_WIDTH = builder.addIdentifier("8831").addName("Zone width")
                .createStrictlyPositive(6, Units.DEGREE);

        PARAMETERS = builder
                .addIdentifier("9824")
                .addName("Transverse Mercator Zoned Grid System")
                .createGroupForMapProjection(
                        TransverseMercator.LATITUDE_OF_ORIGIN,
                        INITIAL_LONGITUDE,
                        ZONE_WIDTH,
                        TransverseMercator.SCALE_FACTOR,
                        TransverseMercator.FALSE_EASTING,
                        TransverseMercator.FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public ZonedTransverseMercator() {
        super(2, 2, PARAMETERS);
    }

    /**
     * Returns the operation type for this projection. We do not classify this operation as a cylindrical projection
     * for now because of the discontinuities between zones. But we may revisit that choice in any future SIS version.
     *
     * @return {@code Projection.class} or a sub-type.
     */
    @Override
    public Class<? extends Projection> getOperationType() {
        return Projection.class;
    }

    /**
     * Notifies {@code DefaultMathTransformFactory} that this projection requires
     * values for the {@code "semi_major"} and {@code "semi_minor"} parameters.
     *
     * @return 1, meaning that the operation requires a source ellipsoid.
     */
    @Override
    public final int getEllipsoidsMask() {
        return 1;
    }

    /**
     * Creates a map projection from the specified group of parameter values.
     *
     * @param  factory     the factory to use for creating and concatenating the (de)normalization transforms.
     * @param  parameters  the group of parameter values.
     * @return the map projection created from the given parameter values.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws FactoryException if the map projection can not be created.
     */
    @Override
    public final MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup parameters)
            throws ParameterNotFoundException, FactoryException
    {
        return new ZonedGridSystem(this, Parameters.castOrWrap(parameters), factory);
    }
}
