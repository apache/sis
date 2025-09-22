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
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Transformation;
import org.apache.sis.parameter.ParameterBuilder;


/**
 * The provider for <q>National Transformation version 1</q> (EPSG:9614).
 * This transform requires data that are not bundled by default with Apache SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlTransient
public final class NTv1 extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3485687719315248009L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        PARAMETERS = builder
                .addIdentifier("9614")
                .addName("NTv1")
                .addName("Geographic2D Offsets (NTv1)")
                .createGroup(NTv2.FILE);
    }

    /**
     * Creates a new provider.
     */
    public NTv1() {
        super(Transformation.class, PARAMETERS,
              EllipsoidalCS.class, false,
              EllipsoidalCS.class, false,
              (byte) 2);
    }

    /**
     * Creates a transform from the specified group of parameter values.
     *
     * @param  context  the parameter values together with its context.
     * @return the created math transform.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws FactoryException if an error occurred while loading the grid.
     */
    @Override
    public MathTransform createMathTransform(final Context context) throws FactoryException {
        return NTv2.createMathTransform(NTv1.class, context, 1);
    }
}
