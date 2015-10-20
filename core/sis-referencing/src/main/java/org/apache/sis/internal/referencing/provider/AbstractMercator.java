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

import java.util.List;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.CylindricalProjection;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.projection.Mercator;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;


/**
 * Base class of providers for all Mercator projections, and for Mercator-like projections.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@XmlTransient
class AbstractMercator extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4478846770971053309L;

    /**
     * The operation parameter descriptor for the <cite>False easting</cite> (FE) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_EASTING = Equirectangular.FALSE_EASTING;

    /**
     * The operation parameter descriptor for the <cite>False northing</cite> (FN) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_NORTHING = Equirectangular.FALSE_NORTHING;

    /**
     * Returns the given descriptor as an array, excluding the two first elements which are assumed
     * to be the axis lengths.This method assumes that all elements in the given list are instances
     * of {@link ParameterDescriptor}.
     *
     * @throws ArrayStoreException if a {@code descriptors} element is not an instance of {@link ParameterDescriptor}.
     */
    @SuppressWarnings("SuspiciousToArrayCall")
    static ParameterDescriptor<?>[] toArray(List<GeneralParameterDescriptor> descriptors) {
        descriptors = descriptors.subList(2, descriptors.size());
        return descriptors.toArray(new ParameterDescriptor<?>[descriptors.size()]);  // Intentional array subtype.
    }

    /**
     * For subclass constructors only.
     */
    AbstractMercator(final ParameterDescriptorGroup parameters) {
        super(parameters);
    }

    /**
     * Returns the operation type for this map projection.
     *
     * @return {@code CylindricalProjection.class}
     */
    @Override
    public final Class<CylindricalProjection> getOperationType() {
        return CylindricalProjection.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return The map projection created from the given parameter values.
     */
    @Override
    protected NormalizedProjection createProjection(final Parameters parameters) {
        return new Mercator(this, parameters);
    }
}
