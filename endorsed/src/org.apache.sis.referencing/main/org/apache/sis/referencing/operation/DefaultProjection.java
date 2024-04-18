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
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;


/**
 * A conversion from (<var>longitude</var>, <var>latitude</var>) coordinates to Cartesian coordinates
 * (<var>x</var>,<var>y</var>).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see org.apache.sis.referencing.crs.DefaultProjectedCRS
 */
@XmlTransient
final class DefaultProjection extends DefaultConversion implements Projection {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7176751851369816864L;

    /**
     * Creates a projection from the given properties.
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  sourceCRS   the source CRS.
     * @param  targetCRS   the target CRS.
     * @param  method      the coordinate operation method.
     * @param  transform   transform from positions in the source CRS to positions in the target CRS.
     */
    public DefaultProjection(final Map<String,?>   properties,
                             final GeographicCRS   sourceCRS,
                             final ProjectedCRS    targetCRS,
                             final OperationMethod method,
                             final MathTransform   transform)
    {
        super(properties, sourceCRS, targetCRS, null, method, transform);
    }

    /**
     * Creates a new projection with the same values as the specified one, together with the
     * specified source and target CRS. While the source conversion can be an arbitrary one,
     * it is typically a defining conversion.
     *
     * @param  definition  the defining conversion.
     * @param  sourceCRS   the source CRS.
     * @param  targetCRS   the target CRS.
     * @param  factory     the factory to use for creating a transform from the parameters or for performing axis changes.
     * @throws IllegalArgumentException if the source or targe CRS is not of the expected types.
     */
    DefaultProjection(final Conversion definition,
                      final CoordinateReferenceSystem sourceCRS,
                      final CoordinateReferenceSystem targetCRS,
                      final MathTransformFactory factory) throws FactoryException
    {
        super(definition, sourceCRS, targetCRS, factory);
        ArgumentChecks.ensureCanCast("sourceCRS", GeographicCRS.class, sourceCRS);
        ArgumentChecks.ensureCanCast("targetCRS", ProjectedCRS .class, targetCRS);
    }

    /**
     * Creates a new coordinate operation with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation
     * into a SIS one, usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  operation  the coordinate operation to copy.
     */
    protected DefaultProjection(final Projection operation) {
        super(operation);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     *
     * @return the conversion interface implemented by this class.
     */
    @Override
    public Class<? extends Projection> getInterface() {
        return Projection.class;
    }
}
