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
package org.apache.sis.referencing.operation;

import java.util.Map;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CylindricalProjection;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;


/**
 * Cylindrical map projections.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see org.apache.sis.referencing.crs.DefaultProjectedCRS
 * @see <a href="http://mathworld.wolfram.com/CylindricalProjection.html">Cylindrical projection on MathWorld</a>
 */
@XmlTransient
final class DefaultCylindricalProjection extends DefaultProjection implements CylindricalProjection {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -969486613826553580L;

    /**
     * Creates a projection from the given properties.
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  sourceCRS   the source CRS.
     * @param  targetCRS   the target CRS.
     * @param  method      the coordinate operation method.
     * @param  transform   transform from positions in the source CRS to positions in the target CRS.
     */
    public DefaultCylindricalProjection(final Map<String,?>   properties,
                                        final GeographicCRS   sourceCRS,
                                        final ProjectedCRS    targetCRS,
                                        final OperationMethod method,
                                        final MathTransform   transform)
    {
        super(properties, sourceCRS, targetCRS, method, transform);
    }

    /**
     * Creates a new projection with the same values than the specified one, together with the
     * specified source and target CRS. While the source conversion can be an arbitrary one, it
     * is typically a defining conversion.
     *
     * @param  definition  the defining conversion.
     * @param  sourceCRS   the source CRS.
     * @param  targetCRS   the target CRS.
     * @param  factory     the factory to use for creating a transform from the parameters or for performing axis changes.
     * @param  actual      an array of length 1 where to store the actual operation method used by the math transform factory.
     */
    DefaultCylindricalProjection(final Conversion definition,
                                 final CoordinateReferenceSystem sourceCRS,
                                 final CoordinateReferenceSystem targetCRS,
                                 final MathTransformFactory factory,
                                 final OperationMethod[] actual) throws FactoryException
    {
        super(definition, sourceCRS, targetCRS, factory, actual);
    }

    /**
     * Creates a new coordinate operation with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  operation  the coordinate operation to copy.
     */
    protected DefaultCylindricalProjection(final CylindricalProjection operation) {
        super(operation);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code CylindricalProjection.class}.
     *
     * @return {@code CylindricalProjection.class}.
     */
    @Override
    public Class<? extends CylindricalProjection> getInterface() {
        return CylindricalProjection.class;
    }
}
