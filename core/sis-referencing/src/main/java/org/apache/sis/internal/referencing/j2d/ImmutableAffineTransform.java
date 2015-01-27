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
import java.awt.geom.NoninvertibleTransformException;
import org.apache.sis.util.resources.Errors;


/**
 * Overrides all mutable {@link AffineTransform} methods in order to check for permission
 * before changing the transform state. If {@link #checkPermission()} is defined to always
 * throw an exception (which is the default), then {@code AffineTransform} is immutable.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class ImmutableAffineTransform extends AffineTransform {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5215291166450556451L;

    /**
     * Constructs an identity affine transform. This is for subclasses usage only,
     * for subclasses that override the {@link #checkPermission()} method.
     */
    protected ImmutableAffineTransform() {
        super();
    }

    /**
     * Constructs a new transform that is a copy of the specified {@code AffineTransform} object.
     *
     * @param tr The affine transform to copy.
     */
    public ImmutableAffineTransform(final AffineTransform tr) {
        super(tr);
    }

    /**
     * Constructs a new transform from 6 values representing the 6 specifiable
     * entries of the 3×3 transformation matrix. Those values are given unchanged to the
     * {@linkplain AffineTransform#AffineTransform(double,double,double,double,double,double) super class constructor}.
     *
     * @param m00 the X coordinate scaling.
     * @param m10 the Y coordinate shearing.
     * @param m01 the X coordinate shearing.
     * @param m11 the Y coordinate scaling.
     * @param m02 the X coordinate translation.
     * @param m12 the Y coordinate translation.
     */
    public ImmutableAffineTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        super(m00, m10, m01, m11, m02, m12);
    }

    /**
     * Checks if the caller is allowed to change this {@code AffineTransform} state.
     * If this method is defined to thrown an exception in all case (which is the default),
     * then this {@code AffineTransform} is immutable.
     *
     * <p>An {@code ImmutableAffineTransform} may be temporary mutable at construction time,
     * but shall be back to immutable state before the instance is given to the user.</p>
     *
     * @throws UnsupportedOperationException if this affine transform is immutable.
     */
    protected void checkPermission() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableAffineTransform));
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before translating this transform.
     */
    @Override
    public final void translate(double tx, double ty) {
        checkPermission();
        super.translate(tx, ty);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before rotating this transform.
     */
    @Override
    public final void rotate(double theta) {
        checkPermission();
        super.rotate(theta);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before rotating this transform.
     */
    @Override
    public final void rotate(double theta, double anchorx, double anchory) {
        checkPermission();
        super.rotate(theta, anchorx, anchory);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before rotating this transform.
     */
    @Override
    public final void rotate(double vecx, double vecy) {
        checkPermission();
        super.rotate(vecx, vecy);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before rotating this transform.
     */
    @Override
    public final void rotate(double vecx, double vecy, double anchorx, double anchory) {
        checkPermission();
        super.rotate(vecx, vecy, anchorx, anchory);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before rotating this transform.
     */
    @Override
    public final void quadrantRotate(int numquadrants) {
        checkPermission();
        super.quadrantRotate(numquadrants);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before rotating this transform.
     */
    @Override
    public final void quadrantRotate(int numquadrants, double anchorx, double anchory) {
        checkPermission();
        super.quadrantRotate(numquadrants, anchorx, anchory);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before scaling this transform.
     */
    @Override
    public final void scale(double sx, double sy) {
        checkPermission();
        super.scale(sx, sy);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before shearing this transform.
     */
    @Override
    public final void shear(double shx, double shy) {
        checkPermission();
        super.shear(shx, shy);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setToIdentity() {
        checkPermission();
        super.setToIdentity();
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setToTranslation(double tx, double ty) {
        checkPermission();
        super.setToTranslation(tx, ty);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setToRotation(double theta) {
        checkPermission();
        super.setToRotation(theta);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setToRotation(double theta, double anchorx, double anchory) {
        checkPermission();
        super.setToRotation(theta, anchorx, anchory);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setToRotation(double vecx, double vecy) {
        checkPermission();
        super.setToRotation(vecx, vecy);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setToRotation(double vecx, double vecy, double anchorx, double anchory) {
        checkPermission();
        super.setToRotation(vecx, vecy, anchorx, anchory);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setToQuadrantRotation(int numquadrants) {
        checkPermission();
        super.setToQuadrantRotation(numquadrants);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setToQuadrantRotation(int numquadrants, double anchorx, double anchory) {
        checkPermission();
        super.setToQuadrantRotation(numquadrants, anchorx, anchory);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setToScale(double sx, double sy) {
        checkPermission();
        super.setToScale(sx, sy);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setToShear(double shx, double shy) {
        checkPermission();
        super.setToShear(shx, shy);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setTransform(AffineTransform Tx) {
        checkPermission();
        super.setTransform(Tx);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before setting this transform.
     */
    @Override
    public final void setTransform(double m00, double m10,
                             double m01, double m11,
                             double m02, double m12) {
        checkPermission();
        super.setTransform(m00, m10, m01, m11, m02, m12);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before concatenating this transform.
     */
    @Override
    public final void concatenate(AffineTransform Tx) {
        checkPermission();
        super.concatenate(Tx);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before concatenating this transform.
     */
    @Override
    public final void preConcatenate(AffineTransform Tx) {
        checkPermission();
        super.preConcatenate(Tx);
    }

    /**
     * Checks for {@linkplain #checkPermission() permission} before inverting this transform.
     *
     * @throws java.awt.geom.NoninvertibleTransformException If the matrix can not be inverted.
     */
    @Override
    public final void invert() throws NoninvertibleTransformException {
        checkPermission();
        super.invert();
    }
}
