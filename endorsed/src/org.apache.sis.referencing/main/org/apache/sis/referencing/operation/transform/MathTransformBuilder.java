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
package org.apache.sis.referencing.operation.transform;

import java.util.Objects;
import java.util.Optional;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.Classes;
import org.apache.sis.util.internal.shared.Strings;

// Specific to the main branch:
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.parameter.ParameterValueGroup;


/**
 * Builder of a parameterized math transform using a method identified by a name or code.
 * A builder instance is created by a call to {@link DefaultMathTransformFactory#builder(String)}.
 * The {@linkplain #parameters() parameters} are set to default values and should be modified
 * in-place by the caller. If the transform requires semi-major and semi-minor axis lengths,
 * those parameters can be set directly or {@linkplain #setSourceAxes indirectly}.
 * Then, the transform is created by a call to {@link #create()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public abstract class MathTransformBuilder {
    /**
     * The factory to use for building the transform.
     */
    protected final MathTransformFactory factory;

    /**
     * The provider that created the parameterized {@link MathTransform} instance, or {@code null}
     * if this information does not apply. This is initially set to the operation method specified
     * in the call to {@link #builder(String)}, but may be modified by {@link #create()}.
     *
     * <p>This operation method is usually an instance of {@link MathTransformProvider},
     * but not necessarily.</p>
     *
     * @see #getMethod()
     */
    protected OperationMethod provider;

    /**
     * Creates a new builder.
     *
     * @param  factory  factory to use for building the transform.
     */
    protected MathTransformBuilder(final MathTransformFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    /**
     * Returns the operation method used for creating the math transform from the parameter values.
     * This is initially the operation method specified in the call to {@link #builder(String)},
     * but may change after the call to {@link #create()} if the method has been adjusted because
     * of the parameter values.
     *
     * @return the operation method used for creating the math transform from the parameter values.
     */
    public final Optional<OperationMethod> getMethod() {
        return Optional.ofNullable(provider);
    }

    /**
     * Returns the parameter values of the transform to create.
     * Those parameters are initialized to default values, which may be implementation or method depend.
     * User-supplied values should be set directly in the returned instance with codes like
     * <code>parameter(</code><var>name</var><code>).setValue(</code><var>value</var><code>)</code>.
     *
     * @return the parameter values of the transform to create. Values should be set in-place.
     */
    public abstract ParameterValueGroup parameters();

    /**
     * Gives hints about axis lengths and their orientations in input coordinates.
     * The action performed by this call depends on the {@linkplain #getMethod() operation method}.
     * For map projections, the action may include something equivalent to the following code:
     *
     * {@snippet lang="java" :
     * parameters().parameter("semi_major").setValue(ellipsoid.getSemiMajorAxis(), ellipsoid.getAxisUnit());
     * parameters().parameter("semi_minor").setValue(ellipsoid.getSemiMinorAxis(), ellipsoid.getAxisUnit());
     * }
     *
     * For geodetic datum shifts, the action may be similar to above code but with different parameter names:
     * {@code "src_semi_major"} and {@code "src_semi_minor"}. Other operation methods may ignore the arguments.
     *
     * <h4>Axis order, units and direction</h4>
     * By default, the source axes of a parameterized transform are normalized to <var>east</var>,
     * <var>north</var>, <var>up</var> (if applicable) directions with units in degrees and meters.
     * If this requirement is ambiguous, for example because the operation method uses incompatible
     * axis directions or units, then the {@code cs} argument should be non-null for allowing the
     * implementation to resolve that ambiguity.
     *
     * @param  cs         the coordinate system defining source axis order and units, or {@code null} if none.
     * @param  ellipsoid  the ellipsoid providing source semi-axis lengths, or {@code null} if none.
     */
    public void setSourceAxes(CoordinateSystem cs, Ellipsoid ellipsoid) {
    }

    /**
     * Gives hints about axis lengths and their orientations in output coordinates.
     * The action performed by this call depends on the {@linkplain #getMethod() operation method}.
     * For datum shifts, the action may include something equivalent to the following code:
     *
     * {@snippet lang="java" :
     * parameters().parameter("tgt_semi_major").setValue(ellipsoid.getSemiMajorAxis(), ellipsoid.getAxisUnit());
     * parameters().parameter("tgt_semi_minor").setValue(ellipsoid.getSemiMinorAxis(), ellipsoid.getAxisUnit());
     * }
     *
     * <h4>Axis order, units and direction</h4>
     * By default, the target axes of a parameterized transform are normalized to <var>east</var>,
     * <var>north</var>, <var>up</var> (if applicable) directions with units in degrees and meters.
     * If this requirement is ambiguous, for example because the operation method uses incompatible
     * axis directions or units, then the {@code cs} argument should be non-null for allowing the
     * implementation to resolve that ambiguity.
     *
     * @param  cs         the coordinate system defining target axis order and units, or {@code null} if none.
     * @param  ellipsoid  the ellipsoid providing target semi-axis lengths, or {@code null} if none.
     */
    public void setTargetAxes(CoordinateSystem cs, Ellipsoid ellipsoid) {
    }

    /**
     * Creates the parameterized transform. The operation method is given by {@link #getMethod()}
     * and the parameter values should have been set on the group returned by {@link #parameters()}
     * before to invoke this constructor.
     * Example:
     *
     * {@snippet lang="java" :
     * MathTransformFactory  factory = ...;
     * MathTransformBuilder builder = factory.builder("Transverse_Mercator");
     * ParameterValueGroup   pg = builder.parameters();
     * pg.parameter("semi_major").setValue(6378137.000);
     * pg.parameter("semi_minor").setValue(6356752.314);
     * MathTransform mt = builder.create();
     * }
     *
     * @return the parameterized transform.
     * @throws FactoryException if the transform creation failed.
     *         This exception is thrown if some required parameters have not been supplied, or have illegal values.
     */
    public abstract MathTransform create() throws FactoryException;

    /**
     * Eventually replaces the given transform by a unique instance. The replacement is done
     * only if the {@linkplain #factory} is an instance of {@link DefaultMathTransformFactory}
     * and {@linkplain DefaultMathTransformFactory#caching(boolean) caching} is enabled.
     *
     * <p>This is a helper method for {@link #create()} implementations.</p>
     *
     * @param  result  the newly created transform.
     * @return a transform equals to the given transform (may be the given transform itself).
     */
    protected MathTransform unique(MathTransform result) {
        if (factory instanceof DefaultMathTransformFactory) {
            final var df = (DefaultMathTransformFactory) factory;
            df.lastMethod.set(getMethod().orElse(null));
            result = df.unique(result);
        }
        return result;
    }

    /**
     * Returns a string representation of this builder for debugging purposes.
     *
     * @return a string representation of this builder.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(),
                "factory", Classes.getShortClassName(factory),
                "method", IdentifiedObjects.getDisplayName(provider, null));
    }
}
