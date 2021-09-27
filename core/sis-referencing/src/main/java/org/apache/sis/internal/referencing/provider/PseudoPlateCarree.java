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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The <cite>"Pseudo Plate Carrée"</cite> pseudo-projection (EPSG:9825). This is only the identity transform.
 * The semi-major and semi-minor axis lengths are ignored (they could be fixed to 1) but nevertheless declared
 * for allowing netCDF file encoding to declare the ellipsoid in pseudo-projection parameters.
 *
 * <p>We do not declare that operation method as a {@link org.opengis.referencing.operation.Projection} because
 * axis units are degrees.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see Equirectangular
 *
 * @since 1.0
 * @module
 */
@XmlTransient
public final class PseudoPlateCarree extends AbstractProvider {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -421300231924004828L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS = builder()
            .addName("Pseudo Plate Carree").addIdentifier("9825")
            .addName(Citations.NETCDF, "latitude_longitude").createGroupForMapProjection();

    /**
     * Constructs a new provider.
     */
    public PseudoPlateCarree() {
        super(2, 2, PARAMETERS);
    }

    /**
     * Returns the operation type. We do not declare that operation method as a
     * {@link org.opengis.referencing.operation.Projection} because axis units are degrees.
     *
     * @return interface implemented by all coordinate operations that use this method.
     */
    @Override
    public final Class<Conversion> getOperationType() {
        return Conversion.class;
    }

    /**
     * Creates an Pseudo Plate Carrée projection from the specified group of parameter values.
     *
     * @param  factory     ignored.
     * @param  parameters  ignored.
     * @return the identity transform.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup parameters) {
        return MathTransforms.identity(2);
    }
}
