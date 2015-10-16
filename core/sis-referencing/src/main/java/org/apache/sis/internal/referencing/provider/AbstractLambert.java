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
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.ConicProjection;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.projection.LambertConicConformal;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;


/**
 * Base class of providers for all Lambert Conical projections.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@XmlTransient
class AbstractLambert extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4270971056061114243L;

    /**
     * The operation parameter descriptor for the <cite>False easting</cite> (FE) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_EASTING = Mercator1SP.FALSE_EASTING;

    /**
     * The operation parameter descriptor for the <cite>False northing</cite> (FN) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_NORTHING = Mercator1SP.FALSE_NORTHING;

    /**
     * For subclass constructors only.
     */
    AbstractLambert(final ParameterDescriptorGroup parameters) {
        super(parameters);
    }

    /**
     * Returns the operation type for this map projection.
     *
     * @return {@code ConicProjection.class}
     */
    @Override
    public final Class<ConicProjection> getOperationType() {
        return ConicProjection.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return The map projection created from the given parameter values.
     */
    @Override
    protected final NormalizedProjection createProjection(final Parameters parameters) {
        return new LambertConicConformal(this, parameters);
    }
}
