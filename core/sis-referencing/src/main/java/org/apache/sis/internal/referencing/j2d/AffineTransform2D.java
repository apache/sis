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

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Workaround;

import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;


/**
 * Transforms two-dimensional coordinate points using an affine transform. This class both extends
 * {@link AffineTransform} and implements {@link MathTransform2D}, so it can be used as a bridge
 * between Java2D and the referencing module. Note that this bridge role involve a tricky issue with
 * the {@link #equals(Object) equals} method, hopefully to occur only in exceptional corner cases.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 *
 * @see ProjectiveTransform
 */
public class AffineTransform2D extends ImmutableAffineTransform
        implements MathTransform2D, LinearTransform, LenientComparable, Parameterized
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5299837898367149069L;

    /**
     * The matrix, or {@code null} if not yet computed.
     *
     * <p>This field is also used for determining if the affine transform is mutable.
     * If this field is {@code null} (which should be a temporary state), then this means that
     * this affine transform is still under construction. This field <strong>must</strong> be
     * set to a non-null value before an {@link AffineTransform2D} instance is published.</p>
     *
     * @see #getMatrix()
     */
    private final AffineMatrix matrix;

    /**
     * The inverse transform. This field will be computed only when needed.
     */
    private transient volatile AffineTransform2D inverse;

    /**
     * Constructs a new affine transform with the same coefficients than the specified transform.
     *
     * @param transform The affine transform to copy.
     */
    public AffineTransform2D(final AffineTransform transform) {
        super(transform);
        forcePositiveZeros();   // Must be invoked before to set the 'matrix' value.
        matrix = new AffineMatrix(this, null);
    }

    /**
     * Constructs a new {@code AffineTransform2D} from the given 9 or 18 values.
     *
     * @param elements The matrix elements, optionally with error terms.
     */
    public AffineTransform2D(final double[] elements) {
        super(pz(elements[0]), pz(elements[3]),
              pz(elements[1]), pz(elements[4]),
              pz(elements[2]), pz(elements[5]));
        matrix = new AffineMatrix(this, elements);
    }

    /**
     * Constructs a new {@code AffineTransform2D} from 6 values representing the 6 specifiable
     * entries of the 3×3 transformation matrix. Those values are given unchanged to the
     * {@link AffineTransform#AffineTransform(double,double,double,double,double,double) super
     * class constructor}.
     *
     * @param m00 the X coordinate scaling.
     * @param m10 the Y coordinate shearing.
     * @param m01 the X coordinate shearing.
     * @param m11 the Y coordinate scaling.
     * @param m02 the X coordinate translation.
     * @param m12 the Y coordinate translation.
     */
    public AffineTransform2D(double m00, double m10, double m01, double m11, double m02, double m12) {
        super(pz(m00), pz(m10), pz(m01), pz(m11), pz(m02), pz(m12));
        matrix = new AffineMatrix(this, null);
    }

    /**
     * Makes sure that the zero is positive. We do that in order to workaround a JDK 6 to 8 bug, where
     * {@link AffineTransform#hashCode()} is inconsistent with {@link AffineTransform#equals(Object)}
     * if there is zeros of opposite sign.
     *
     * <p>The inconsistency is in the use of {@link Double#doubleToLongBits(double)} for hash code and
     * {@code ==} for testing equality. The former is sensitive to the sign of 0 while the later is not.</p>
     */
    @Workaround(library="JDK", version="8") // Last verified in 1.8.0_05.
    private static double pz(final double value) {
        return (value != 0) ? value : 0;
    }

    /**
     * Ensures that this transform contains only positive zeros.
     */
    public final void forcePositiveZeros() {
        super.setTransform(pz(super.getScaleX()),     pz(super.getShearY()),
                           pz(super.getShearX()),     pz(super.getScaleY()),
                           pz(super.getTranslateX()), pz(super.getTranslateY()));
    }

    /**
     * Throws an {@link UnsupportedOperationException} when a mutable method
     * is invoked, since {@code AffineTransform2D} must be immutable.
     *
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    protected final void checkPermission() throws UnsupportedOperationException {
        if (matrix != null) {
            super.checkPermission();
        }
    }

    /**
     * Returns {@code true} since this transform is affine.
     */
    @Override
    public final boolean isAffine() {
        return true;
    }

    /**
     * Returns the parameter descriptors for this math transform.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Affine.getProvider(2, 2, true).getParameters();
    }

    /**
     * Returns the matrix elements as a group of parameters values. The number of parameters
     * depends on the matrix size. Only matrix elements different from their default value
     * will be included in this group.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return Affine.parameters(matrix);
    }

    /**
     * Gets the dimension of input points, which is fixed to 2.
     */
    @Override
    public final int getSourceDimensions() {
        return 2;
    }

    /**
     * Gets the dimension of output points, which is fixed to 2.
     */
    @Override
    public final int getTargetDimensions() {
        return 2;
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     */
    @Override
    public final DirectPosition transform(final DirectPosition ptSrc, DirectPosition ptDst) {
        ensureDimensionMatches("ptSrc", 2, ptSrc);
        /*
         * Try to write directly in the destination point if possible. Following
         * code avoid the creation of temporary objects (except if ptDst is null).
         */
        if (ptDst == ptSrc) {
            if (ptSrc instanceof Point2D) {
                final Point2D point = (Point2D) ptSrc;
                super.transform(point, point);
                return ptSrc;
            }
        } else {
            if (ptDst == null) {
                final DirectPosition2D point = new DirectPosition2D(ptSrc.getOrdinate(0), ptSrc.getOrdinate(1));
                super.transform(point, point);
                return point;
            }
            ensureDimensionMatches("ptDst", 2, ptDst);
            if (ptDst instanceof Point2D) {
                final Point2D point = (Point2D) ptDst;
                point.setLocation(ptSrc.getOrdinate(0), ptSrc.getOrdinate(1));
                super.transform(point, point);
                return ptDst;
            }
        }
        /*
         * At this point, we have no choice to create a temporary Point2D.
         */
        final Point2D.Double point = new Point2D.Double(ptSrc.getOrdinate(0), ptSrc.getOrdinate(1));
        super.transform(point, point);
        ptDst.setOrdinate(0, point.x);
        ptDst.setOrdinate(1, point.y);
        return ptDst;
    }

    /**
     * Transforms the specified shape. This method creates a simpler shape then the default
     * {@linkplain AffineTransform#createTransformedShape(Shape) super-class implementation}.
     * For example if the given shape is a rectangle and this affine transform has no scale or
     * shear, then the returned shape will be an instance of {@link java.awt.geom.Rectangle2D}.
     *
     * @param  shape Shape to transform.
     * @return Transformed shape, or {@code shape} if this transform is the identity transform.
     */
    @Override
    public final Shape createTransformedShape(final Shape shape) {
        return AffineTransforms2D.transform(this, shape, false);
    }

    /**
     * Returns this transform as an affine transform matrix.
     */
    @Override
    public final Matrix getMatrix() {
        return matrix;
    }

    /**
     * Gets the derivative of this transform at a point.
     * For an affine transform, the derivative is the same everywhere.
     */
    @Override
    public final Matrix derivative(final Point2D point) {
        return new Matrix2(getScaleX(), getShearX(),
                           getShearY(), getScaleY());
    }

    /**
     * Gets the derivative of this transform at a point.
     * For an affine transform, the derivative is the same everywhere.
     */
    @Override
    public final Matrix derivative(final DirectPosition point) {
        return derivative((Point2D) null);
    }

    /**
     * Creates the inverse transform of this object.
     *
     * @throws NoninvertibleTransformException if this transform can not be inverted.
     */
    @Override
    public final AffineTransform2D inverse() throws NoninvertibleTransformException {
        if (inverse == null) {
            if (super.isIdentity()) {
                inverse = this;
            } else synchronized (this) {
                /*
                 * Double check idiom. Was deprecated before Java 5 (couldn't work reliably).
                 * Is okay with the new memory model since Java 5 provided that the field is
                 * declared volatile (Joshua Bloch, "Effective Java" second edition).
                 */
                if (inverse == null) {
                    /*
                     * In a previous version, we were using the Java2D code as below:
                     *
                     *     AffineTransform2D work = new AffineTransform2D(this, true);
                     *     work.invert();
                     *     work.forcePositiveZeros();
                     *     work.freeze();
                     *
                     * Current version now uses the SIS code instead in order to get the double-double precision.
                     * It usually does not make a difference in the result of the matrix inversion, when ignoring
                     * the error terms.  But those error terms appear to be significant later, when the result of
                     * this matrix inversion is multiplied with other matrices: the double-double accuracy allows
                     * us to better detect the terms that are 0 or 1 after matrix concatenation.
                     */
                    final AffineTransform2D work = new AffineTransform2D(
                            ((ExtendedPrecisionMatrix) Matrices.inverse(matrix)).getExtendedElements());
                    work.inverse = this;
                    inverse = work;                 // Set only on success.
                }
            }
        }
        return inverse;
    }

    /**
     * Compares this affine transform with the given object for equality. This method behaves as documented
     * in the {@link LinearTransform#equals(Object, ComparisonMode) LinearTransform} interface, except for
     * the following case: if the given mode is {@link ComparisonMode#STRICT}, then this method delegates
     * to {@link #equals(Object)}. The later method has different rules than the ones documented in the
     * {@code LinearTransform} interface, because of the {@code AffineTransform} inheritance.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {                       // Slight optimization
            return true;
        }
        if (mode == ComparisonMode.STRICT) {
            return equals(object);
        }
        if (object instanceof LinearTransform) {
            final Matrix m2 = ((LinearTransform) object).getMatrix();
            return AffineTransforms2D.toMatrix(this).equals(m2, mode);
        }
        return false;
    }

    /**
     * Compares this affine transform with the given object for equality. The comparison is performed in the same way
     * than {@link AffineTransform#equals(Object)} with one additional rule: if the other object is also an instance
     * of {@code AffineTransform2D}, then the two objects must be of the exact same class.
     *
     * <p>Most SIS implementations require that the objects being compared are unconditionally of the same class in
     * order to be considered equal. However many JDK implementations, including {@link AffineTransform}, do not have
     * this requirement. Consequently the above condition (i.e. require the same class only if the given object is an
     * {@code AffineTransform2D} or a subclass) is necessary in order to preserve the <cite>symmetricity</cite> contract
     * of {@link Object#equals(Object)}.</p>
     *
     * <p>A side-effect of this implementation is that the <cite>transitivity</cite> contract of
     * {@code Object.equals(Object)} may be broken is some corner cases. This contract said:</p>
     *
     * <blockquote>
     * <code>a.equals(b)</code> and <code>b.equals(c)</code> implies <code>a.equals(c)</code>
     * </blockquote>
     *
     * Assuming that <var>a</var>, <var>b</var> and <var>c</var> are instances of {@code AffineTransform}
     * (where "instance of <var>T</var>" means <var>T</var> or any subclass of <var>T</var>), then the transitivity
     * contract is broken if and only if exactly two of those objects are instance of {@code AffineTransform2D} and
     * those two objects are not of the same class. Note that this implies that at least one subclass of
     * {@code AffineTransform2D} is used.
     *
     * <p>In the vast majority of cases, the transitivity contract is <strong>not</strong> broken and the users can
     * ignore this documentation. The transitivity contract is typically not broken either because we usually don't
     * subclass {@code AffineTransform2D}, or because we don't mix {@code AffineTransform} with {@code AffineTransform2D}
     * subclasses in the same collection.</p>
     *
     * <p>This special case exists in order to allow developers to attach additional information to their own subclass
     * of {@code AffineTransform2D}, and still distinguish their specialized subclass from ordinary affine transforms
     * in a pool of {@code MathTransform} instances. The main application is the
     * {@linkplain org.apache.sis.referencing.operation.projection.Equirectangular} map projection,
     * which can be simplified to an affine transform but still needs to remember the projection parameters.</p>
     *
     * @param  object The object to compare with this affine transform for equality.
     * @return {@code true} if the given object is of appropriate class (as explained in the
     *         above documentation) and the affine transform coefficients are the same.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != this) {
            if (!super.equals(object)) {
                return false;
            }
            if (object instanceof AffineTransform2D) {
                return object.getClass() == getClass();
            }
        }
        return true;
    }

    /*
     * Intentionally no hashCode() method. See equals(Object) for explanation.
     */

    /**
     * Returns a new affine transform which is a modifiable copy of this transform. This implementation always
     * returns an instance of {@link AffineTransform}, <strong>not</strong> {@code AffineTransform2D}, because
     * the later is unmodifiable and cloning it make little sense.
     *
     * @return A modifiable copy of this affine transform.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public AffineTransform clone() {
        return new AffineTransform(this);
    }

    /**
     * Returns the WKT for this transform.
     */
    @Override
    public String toWKT() {
        final Formatter formatter = new Formatter();
        formatter.append(this);
        return formatter.toWKT();
    }

    /**
     * Returns a string representation of this transform as a matrix, for consistency with other
     * {@link LinearTransform} implementations in Apache SIS.
     */
    @Override
    public String toString() {
        return Matrices.toString(getMatrix());
    }
}
