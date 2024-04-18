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
package org.apache.sis.referencing.operation.provider;

import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The <q>Pseudo Plate Carrée</q> pseudo-projection (EPSG:9825). This is only the identity transform.
 * The semi-major and semi-minor axis lengths are ignored (they could be fixed to 1) but nevertheless declared
 * for allowing netCDF file encoding to declare the ellipsoid in pseudo-projection parameters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see Equirectangular
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
        super(Conversion.class, PARAMETERS,
              EllipsoidalCS.class, false,
              EllipsoidalCS.class, false,
              (byte) 2);
    }

    /**
     * Creates an Pseudo Plate Carrée projection from the specified group of parameter values.
     * The number of dimensions is determined from the target number of dimensions only.
     * This is a way to communicate to the caller what it needs for producing the requested output
     * (same policy as in {@link MapProjection#maybe3D(Context, MathTransform)}).
     *
     * @param  context  ignored.
     * @return the identity transform.
     */
    @Override
    public MathTransform createMathTransform(final Context context) {
        return MathTransforms.identity(context.getTargetDimensions().orElse(2));
    }
}
