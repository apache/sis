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
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.privy.Constants;


/**
 * The provider for <q>Spherical 2D to 3D conversion</q>.
 * This operation is an Apache <abbr>SIS</abbr> extension.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see Spherical3Dto2D
 * @see Geographic2Dto3D
 */
@XmlTransient
public final class Spherical2Dto3D extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2320384527305401074L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder().setCodeSpace(Citations.SIS, Constants.SIS);
        PARAMETERS = builder.addName("Spherical2D to 3D conversion").createGroupForMapProjection();
    }

    /**
     * The canonical instance of this operation method.
     *
     * @see #provider()
     */
    private static final Spherical2Dto3D INSTANCE = new Spherical2Dto3D();

    /**
     * Returns the canonical instance of this operation method.
     * This method is invoked by {@link java.util.ServiceLoader} using reflection.
     *
     * @return the canonical instance of this operation method.
     */
    public static Spherical2Dto3D provider() {
        return INSTANCE;
    }

    /**
     * Creates a new provider.
     *
     * @todo Make this constructor private after we stop class-path support.
     *       Instantiate {@code Spherical2Dto3D} directly with the parameters.
     */
    public Spherical2Dto3D() {
        super(Conversion.class, PARAMETERS,
              SphericalCS.class, true,
              SphericalCS.class, false,
              (byte) 2);
    }

    /**
     * Returns the inverse of this operation.
     */
    @Override
    public AbstractProvider inverse() {
        return Spherical3Dto2D.provider();
    }

    /**
     * Returns the operation method which is the closest match for the given transform.
     * This is an adjustment based on the number of dimensions only, on the assumption
     * that the given transform has been created by this provider or a compatible one.
     */
    @Override
    public AbstractProvider variantFor(final MathTransform transform) {
        return transform.getSourceDimensions() > transform.getTargetDimensions() ? Spherical3Dto2D.provider() : this;
    }

    /**
     * Creates the transform for the ellipsoid specified in the given parameters.
     *
     * @param  context  the parameter values together with its context.
     * @return the math transform for the given parameter values.
     * @throws FactoryException if an error occurred while creating the transform.
     */
    @Override
    public MathTransform createMathTransform(final Context context) throws FactoryException {
        try {
            return inverse().createMathTransform(context).inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);      // Should never happen.
        }
    }
}
