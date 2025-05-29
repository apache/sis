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
import java.io.Serializable;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.UnformattableObjectException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * The base class of math transform wrappers. This can be used as an opaque object for hiding the fact
 * that a given transform implements the {@link MathTransform2D} or {@link LinearTransform} interface,
 * in order to disable optimization paths in some tests.
 *
 * <p><strong>Do not implement {@link MathTransform2D} in this base class</strong>.
 * This wrapper is sometimes used for hiding the fact that a transform implements
 * the {@code MathTransform2D} interface, typically for testing a different code
 * path in a JUnit test.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class MathTransformWrapper extends FormattableObject implements MathTransform, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5180954407422828265L;

    /**
     * The math transform to which to delegate the work.
     */
    @SuppressWarnings("serial")             // Most Apache SIS implementations are serializable.
    public final MathTransform transform;

    /**
     * Creates a new wrapper which delegates its work to the specified math transform.
     *
     * @param transform the math transform to which to delegate the work.
     */
    public MathTransformWrapper(final MathTransform transform) {
        this.transform = Objects.requireNonNull(transform);
    }

    /**
     * Gets the dimension of input points.
     */
    @Override
    public final int getSourceDimensions() {
        return transform.getSourceDimensions();
    }

    /**
     * Gets the dimension of output points.
     */
    @Override
    public final int getTargetDimensions() {
        return transform.getTargetDimensions();
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     *
     * @throws MismatchedDimensionException if {@code ptSrc} or
     *         {@code ptDst} doesn't have the expected dimension.
     * @throws TransformException if the point cannot be transformed.
     */
    @Override
    public final DirectPosition transform(final DirectPosition ptSrc, final DirectPosition ptDst)
            throws MismatchedDimensionException, TransformException
    {
        return transform.transform(ptSrc, ptDst);
    }

    /**
     * Transforms many positions in a list of coordinate values.
     */
    @Override
    public final void transform(final double[] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final int numPts) throws TransformException
    {
        transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms many positions in a list of coordinate values.
     */
    @Override
    public final void transform(final float[] srcPts, final int srcOff,
                                final float[] dstPts, final int dstOff,
                                final int numPts) throws TransformException
    {
        transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms many positions in a list of coordinate values.
     */
    @Override
    public final void transform(final float [] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final int numPts) throws TransformException
    {
        transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms many positions in a list of coordinate values.
     */
    @Override
    public final void transform(final double[] srcPts, final int srcOff,
                                final float [] dstPts, final int dstOff,
                                final int numPts) throws TransformException
    {
        transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Gets the derivative of this transform at a point.
     */
    @Override
    public final Matrix derivative(final DirectPosition point) throws TransformException {
        return transform.derivative(point);
    }

    /**
     * Returns the inverse of this math transform.
     * The inverse is wrapped in a new {@code MathTransformWrapper} instance.
     */
    @Override
    public MathTransform inverse() throws NoninvertibleTransformException {
        return new MathTransformWrapper(transform.inverse());
    }

    /**
     * Tests whether this transform does not move any points.
     */
    @Override
    public final boolean isIdentity() {
        return transform.isIdentity();
    }

    /**
     * Compares the specified object with this math transform for equality.
     *
     * @param  object  the object to compare with this transform.
     * @return {@code true} if the given object is of the same class and if the wrapped transforms are equal.
     */
    @Override
    public final boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final MathTransformWrapper that = (MathTransformWrapper) object;
            return Objects.equals(this.transform, that.transform);
        }
        return false;
    }

    /**
     * Returns a hash code value for this math transform.
     */
    @Override
    public final int hashCode() {
        return getClass().hashCode() ^ transform.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Returns a <i>Well Known Text</i> (WKT) for this transform.
     *
     * @throws UnsupportedOperationException if this object cannot be formatted as WKT.
     */
    @Override
    public final String toWKT() throws UnsupportedOperationException {
        return transform.toWKT();
    }

    /**
     * Returns a string representation for this transform.
     */
    @Override
    public final String toString() {
        return transform.toString();
    }

    /**
     * Delegates the WKT formatting to the wrapped math transform. This class is usually used
     * with Apache SIS implementations of math transform, so the exception is unlikely to be thrown.
     *
     * @param  formatter the formatter to use.
     * @return the WKT element name, which is {@code "Param_MT"} in the default implementation.
     */
    @Override
    protected final String formatTo(final Formatter formatter) {
        if (transform instanceof AbstractMathTransform fmt) {
            return fmt.formatTo(formatter);
        }
        throw new UnformattableObjectException();
    }
}
