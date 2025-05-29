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

import java.util.Map;
import java.io.Serializable;
import java.io.InvalidClassException;
import java.io.ObjectStreamException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.privy.CoordinateOperations;
import org.apache.sis.referencing.privy.ExtendedPrecisionMatrix;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.privy.Constants;


/**
 * Base class of conversions between coordinate systems.
 * Each subclass should have a singleton instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class CoordinateSystemTransform extends AbstractMathTransform {
    /**
     * Number of input and output dimensions.
     *
     * @see #getSourceDimensions()
     * @see #getTargetDimensions()
     */
    private final int dimension;

    /**
     * Index of dimensions having linear units in the coordinate system identified by {@code -linearTransformPosition}.
     * This is used for optimizing the concatenation of this transform by an affine transform.
     *
     * @see #tryConcatenate(Joiner)
     */
    private final int[] linearDimensions;

    /**
     * Relative index of the affine transform doing conversion of linear coordinates.
     * If the inputs of this transform are linear coordinate values in a Cartesian coordinate system,
     * then this field shall be -1. If the above-cited linear coordinates are rather in the outputs,
     * then this field shall be +1.
     *
     * <p>The transform at the relative index {@code -linearTransformPosition} should be a mix
     * of angular and linear coordinate values. The coordinates at the positions identified by
     * {@link #linearDimensions} are linear values, and all other coordinates are angular values.
     */
    private final byte linearTransformPosition;

    /**
     * An operation method that describe this coordinate system conversion.
     * This is used for providing a value in {@link DefaultMathTransformFactory#getLastMethodUsed()}.
     */
    final transient OperationMethod method;

    /**
     * The {@linkplain #method} augmented with one pass through dimension.
     * May be the same instance as {@link #method} if that method is already 3D.
     *
     * <div class="note"><b>Note:</b> if {@link #method} is "Polar to Cartesian",
     * then {@code method3D} is "Cylindrical to Cartesian".</div>
     *
     * This method is used for {@link org.opengis.referencing.operation.CoordinateOperation} WKT formatting.
     * Contrarily to {@link #method}, this {@code method3D} is never used for {@link MathTransform} WKT.
     * Instead, the latter case is represented by a concatenation of {@link #method} with a pass-through.
     */
    final transient OperationMethod method3D;

    /**
     * An empty contextual parameter, used only for representing conversion from degrees to radians.
     */
    final transient ContextualParameters context;

    /**
     * The complete transform, including conversion between degrees and radians.
     *
     * @see #completeTransform(MathTransformFactory)
     */
    private transient volatile MathTransform complete;

    /**
     * The {@link #complete} transform in a {@link PassThroughTransform} with a 1 trailing coordinate.
     * This is used for supporting the cylindrical case on top the polar case.
     *
     * @see #passthrough(MathTransformFactory)
     */
    private transient volatile MathTransform passthrough;

    /**
     * Creates a new conversion between two types of coordinate system.
     * Subclasses may need to invoke {@link ContextualParameters#normalizeGeographicInputs(double)}
     * or {@link ContextualParameters#denormalizeGeographicOutputs(double)} after this constructor.
     */
    CoordinateSystemTransform(final String method, final String method3D, final int dimension,
                              final int[] linearDimensions, final byte linearTransformPosition)
    {
        this.dimension = dimension;
        this.linearDimensions = linearDimensions;
        this.linearTransformPosition = linearTransformPosition;
        this.method = method(method);
        this.method3D = (method3D != null) ? method(method3D) : this.method;
        this.context = new ContextualParameters(this.method.getParameters(), dimension, dimension);
    }

    /**
     * Creates an operation method of the given name.
     */
    private static OperationMethod method(final String name) {
        final Map<String,?> properties = Map.of(DefaultParameterDescriptorGroup.NAME_KEY,
                new ImmutableIdentifier(Citations.SIS, Constants.SIS, name));
        final var descriptor = new DefaultParameterDescriptorGroup(properties, 1, 1);
        return new DefaultOperationMethod(properties, descriptor);
    }

    /**
     * Returns the complete transform, including conversion between degrees and radians units.
     */
    final MathTransform completeTransform(final MathTransformFactory factory) throws FactoryException {
        MathTransform tr = complete;
        if (tr == null) {
            tr = context.completeTransform(factory, this);
            if (CoordinateOperations.isDefaultInstance(factory)) {
                // No need to synchronize since DefaultMathTransformFactory returns unique instances.
                complete = tr;
            }
        }
        return tr;
    }

    /**
     * Returns the cylindrical, including conversion between degrees and radians units.
     * This method is legal only for {@link PolarToCartesian} or {@link CartesianToPolar}.
     */
    final MathTransform passthrough(final MathTransformFactory factory) throws FactoryException {
        MathTransform tr = passthrough;
        if (tr == null) {
            tr = factory.createPassThroughTransform(0, completeTransform(factory), 1);
            if (CoordinateOperations.isDefaultInstance(factory)) {
                // No need to synchronize since DefaultMathTransformFactory returns unique instances.
                passthrough = tr;
            }
        }
        return tr;
    }

    /**
     * Returns the number of dimensions in the source coordinate tuples.
     * Shall be equal to {@code getSourceCS().getDimension()}.
     */
    @Override
    public final int getSourceDimensions() {
        return dimension;
    }

    /**
     * Returns the number of dimensions in the target coordinate tuples.
     * Shall be equal to {@code getTargetCS().getDimension()}.
     */
    @Override
    public final int getTargetDimensions() {
        return dimension;
    }

    /**
     * Returns the empty set of parameter values.
     */
    @Override
    public final ParameterValueGroup getParameterValues() {
        return context;
    }

    /**
     * Returns the contextual parameters. This is used for telling to the Well Known Text (WKT) formatter that this
     * {@code CoordinateSystemTransform} transform is usually preceeded or followed by a conversion between degree
     * and radian units of measurement.
     */
    @Override
    protected final ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Optimizes concatenation by transferring scale factors from the Cartesian CS side to the spherical,
     * cylindrical or polar CS side. The rational is that the affine transform on the latter side will be
     * needed anyway for the conversions between radians and degrees.
     */
    @Override
    protected final void tryConcatenate(final Joiner info) throws FactoryException {
        // Do nothing if there is no linear transform for angular values.
concat: if (info.isLinear(-linearTransformPosition, true)) {
            final var linear = ExtendedPrecisionMatrix.castOrWrap(MathTransforms.getMatrix(
                                    info.getTransform(linearTransformPosition).orElse(null)));
            if (linear != null) {
                final int n = linear.getNumRow();
                if (n == linear.getNumCol()) {
                    Number scale = null;
                    for (int j=0; j<n; j++) {
                        for (int i=0; i<n; i++) {
                            final Number e = linear.getElementOrNull(j,i);
                            if ((e == null) == (i == j)) break concat;          // Abort if matrix is not diagonal.
                            if (e != null) {
                                if (i < n-1) {
                                    if (scale == null) scale = e;
                                    else if (!scale.equals(e)) break concat;    // Abort if non-uniform scale.
                                } else if (e.doubleValue() != 1) break concat;  // Abort if transform is not affine.
                            }
                        }
                    }
                    /*
                     * The transform is affine and applies an uniform scale in all dimensions.
                     * Replace it by the same scale in all output dimensions having a linear unit.
                     */
                    if (scale != null) {
                        final MatrixSIS angular = Matrices.create(n, n, ExtendedPrecisionMatrix.CREATE_IDENTITY);
                        for (int j : linearDimensions) {
                            angular.setNumber(j, j, scale);
                        }
                        MathTransform first = info.factory.createAffineTransform(angular);
                        MathTransform other = this;
                        if (linearTransformPosition < 0) {
                            other = first;
                            first = this;
                        }
                        first = info.factory.createConcatenatedTransform(first, other);
                        info.replace(linearTransformPosition, first);
                        return;
                    }
                }
            }
        }
        super.tryConcatenate(info);
    }

    /**
     * The object to use as a proxy during serialization.
     * For avoiding complain about the lack of default constructor in the super class.
     */
    static final class Proxy implements Serializable {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -2177879597869330855L;

        /** The type of transform to reconstitute at deserialization. */
        private final Class<? extends CoordinateSystemTransform> type;

        /** Creates a new proxy. */
        Proxy(final Class<? extends CoordinateSystemTransform> type) {
            this.type = type;
        }

        /**
         * Returns the singleton instance on deserialization.
         */
        private Object readResolve() throws ObjectStreamException {
            if (type == CartesianToSpherical.class) return CartesianToSpherical.INSTANCE;
            if (type == SphericalToCartesian.class) return SphericalToCartesian.INSTANCE;
            if (type ==     PolarToCartesian.class) return     PolarToCartesian.INSTANCE;
            if (type ==     CartesianToPolar.class) return     CartesianToPolar.INSTANCE;
            throw new InvalidClassException(type.getCanonicalName(), type.toString());
        }
    }
}
