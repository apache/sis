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
package org.apache.sis.internal.referencing.j2d;

import java.awt.geom.AffineTransform;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.internal.system.Semaphores;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * An affine transform that remember the parameters used for its construction.
 * Those parameters may be very different than the usual affine transform parameters.
 *
 * For example an {@link org.apache.sis.referencing.operation.projection.Equirectangular} projection
 * can be expressed as an affine transform. In such case, the same affine transform can be described
 * by two equivalent set of parameters:
 *
 * <ul>
 *   <li>The {@code "elt_0_0"}, {@code "elt_0_1"}, <i>etc.</i> parameters inherited from the parent class.</li>
 *   <li>The {@code "semi_major"}, {@code "semi_minor"}, <i>etc.</i> parameters which, when used with the
 *       Equirectangular operation method, produced this affine transform.</li>
 * </ul>
 *
 * This {@code ParameterizedAffine} class can be used when we want to describe this affine transform
 * by the Equirectangular set of parameters instead than the generic Affine set of parameters.
 * In such case, we must give a reference to an object able to provide those parameters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class ParameterizedAffine extends AffineTransform2D {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 906346920928432466L;

    /**
     * The (presumed immutable) parameters used for creating this transform.
     */
    private final ParameterValueGroup parameters;

    /**
     * {@code true} if {@link #parameters} provides an accurate description of this transform, or
     * {@code false} if this transform may be different than the one described by the parameters.
     * This field may be {@code false} for example after an Equirectangular projection has been
     * concatenated with other affine transforms for unit conversions, axis swapping, <i>etc</i>.
     */
    private final boolean isDefinitive;

    /**
     * Creates a new transform from the given affine and parameters.
     *
     * @param transform    The affine transform to copy.
     * @param parameters   The parameters to remember. It is caller's responsibility to provide an immutable instance.
     * @param isDefinitive {@code true} if {@code parameters} provides an accurate description of {@code transform}, or
     *                     {@code false} if the transform may be different than the one described by {@code parameters}.
     */
    public ParameterizedAffine(final AffineTransform transform, final ParameterValueGroup parameters, final boolean isDefinitive) {
        super(transform);
        this.parameters   = parameters;
        this.isDefinitive = isDefinitive;
    }

    /**
     * Returns the given transform associated to the same parameters than this {@code ParameterizedAffine},
     * if possible. If the given transform is not affine, then it is returned unchanged.
     *
     * @param  transform The transform to be at least partially described by {@link #parameters}.
     * @return A copy of the given affine transform associated to the parameter of this object,
     *         or the given transform unchanged if it was not affine.
     */
    public MathTransform newTransform(final MathTransform transform) {
        if (transform instanceof AffineTransform) {
            return new ParameterizedAffine((AffineTransform) transform, parameters, false);
        } else {
            return transform;
        }
    }

    /**
     * Returns the parameter descriptors for this map projection.
     *
     * @return The map projection parameters if they are an accurate description of this transform,
     *         or the generic affine parameters in case of doubt.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return isDefinitive || Semaphores.query(Semaphores.ENCLOSED_IN_OPERATION)  // See comment in getParameterValues().
               ? parameters.getDescriptor() : super.getParameterDescriptors();
    }

    /**
     * Returns the parameter values for this map projection.
     *
     * <p><b>Hack:</b> this method normally returns the matrix parameters in case of doubt. However if
     * {@link Semaphores#ENCLOSED_IN_OPERATION} is set, then this method returns the map projection parameters
     * even if they are not a complete description of this math transform. This internal hack shall be used
     * only by {@link org.apache.sis.referencing.operation.AbstractCoordinateOperation}.</p>
     *
     * <p><b>Use case of above hack:</b> consider an "Equidistant Cylindrical (Spherical)" map projection
     * from a {@code GeographiCRS} base using (latitude, longitude) axis order. We need to concatenate an
     * affine transform performing the axis swapping before the actual map projection. The concatenated
     * transform is part of {@code SingleOperation}, which is itself part of {@code ProjecteCRS}.
     * Consequently we have two conflicting needs:</p>
     *
     * <ul>
     *   <li>If this method is queried from a {@code SingleOperation} instance (usually indirectly as part of a
     *     {@code ProjectedCRS}), then we want to return the "Equidistant Cylindrical (Spherical)" map projection
     *     parameters without bothering about axis swapping, because the later is described by the {@code Axis["…"]}
     *     elements in the enclosing {@code ProjectedCRS} instance.</li>
     *   <li>But if this {@code MathTransform} is formatted directly (not as a component of {@code ProjectedCRS}),
     *     then we want to format it as a matrix, otherwise the users would have no way to see that an axis swapping
     *     has been applied.</li>
     * </ul>
     *
     * The {@code Semaphores.ENCLOSED_IN_OPERATION} flag is SIS internal mechanism for distinguish the two above-cited
     * cases.
     *
     * @return The map projection parameters if they are an accurate description of this transform,
     *         or the generic affine parameters in case of doubt.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return isDefinitive || Semaphores.query(Semaphores.ENCLOSED_IN_OPERATION)
               ? parameters : super.getParameterValues();
    }

    /**
     * Compares this affine transform with the given object for equality.
     * Parameters are compared only if the other object is also an instance of {@code ParameterizedAffine}
     * in order to preserve the {@link AffineTransform#equals(Object)} <cite>symmetricity</cite> contract.
     *
     * @param  object The object to compare with this transform for equality.
     * @return {@code true} if the given object is of appropriate class (as explained in the
     *         {@link AffineTransform2D#equals(Object)} documentation) and the coefficients are the same.
     */
    @Override
    public boolean equals(final Object object) {
        if (super.equals(object)) {
            if (object instanceof ParameterizedAffine) {
                final ParameterizedAffine that = (ParameterizedAffine) object;
                return (this.isDefinitive == that.isDefinitive) &&
                       Objects.equals(this.parameters, that.parameters);
            }
            return true;
        }
        return false;
    }

    /*
     * Intentionally no hashCode() method. See AffineTransform2D.equals(Object) for explanation.
     */
}
