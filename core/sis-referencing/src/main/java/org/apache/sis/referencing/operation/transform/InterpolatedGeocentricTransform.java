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

import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.FranceGeocentricInterpolation;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.util.ArgumentChecks;


/**
 * Transforms between two geographic CRS by performing geocentric translations interpolated from a grid file.
 * This transform is used by <cite>"France geocentric interpolation"</cite> (ESPG:9655) datum shift.
 *
 * <div class="note"><b>Implementation note:</b>
 * while this transformation is conceptually defined as a translation in geocentric coordinates, the current
 * Apache SIS implementation rather uses the Molodensy (non-abridged) approximation for performance reasons.
 * Errors should be lower than the grid accuracy.</div>
 *
 * @author  Simon Reynard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class InterpolatedGeocentricTransform extends MolodenskyFormula {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5691721806681489940L;

    /**
     * The grid of datum shifts from source datum to target datum.
     */
    protected final DatumShiftGrid grid;

    /**
     * Creates a transform from the specified parameters.
     * This {@code InterpolatedGeocentricTransform} class expects ordinate values in the following order and units:
     * <ol>
     *   <li>longitudes in <strong>radians</strong> relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in <strong>radians</strong>,</li>
     *   <li>optionally heights above the ellipsoid, in same units than the source ellipsoid axes.</li>
     * </ol>
     *
     * For converting geographic coordinates in degrees, {@code InterpolatedGeocentricTransform} instances
     * need to be concatenated with the following affine transforms:
     *
     * <ul>
     *   <li><cite>Normalization</cite> before {@code InterpolatedGeocentricTransform}:<ul>
     *     <li>Conversion of (λ,φ) from degrees to radians.</li>
     *   </ul></li>
     *   <li><cite>Denormalization</cite> after {@code InterpolatedGeocentricTransform}:<ul>
     *     <li>Conversion of (λ,φ) from radians to degrees.</li>
     *   </ul></li>
     * </ul>
     *
     * After {@code InterpolatedGeocentricTransform} construction,
     * the full conversion chain including the above affine transforms can be created by
     * <code>{@linkplain #getContextualParameters()}.{@linkplain ContextualParameters#completeTransform
     * completeTransform}(factory, this)}</code>.
     *
     * @param source      The source ellipsoid.
     * @param isSource3D  {@code true} if the source coordinates have a height.
     * @param target      The target ellipsoid.
     * @param isTarget3D  {@code true} if the target coordinates have a height.
     * @param tX          Initial geocentric <var>X</var> translation in same units than the source ellipsoid axes.
     * @param tY          Initial geocentric <var>Y</var> translation in same units than the source ellipsoid axes.
     * @param tZ          Initial geocentric <var>Z</var> translation in same units than the source ellipsoid axes.
     * @param grid        The grid of datum shifts from source to target datum.
     *
     * @see #createGeodeticTransformation(MathTransformFactory, Ellipsoid, boolean, Ellipsoid, boolean, double, double, double, DatumShiftGrid)
     */
    protected InterpolatedGeocentricTransform(final Ellipsoid source, final boolean isSource3D,
                                              final Ellipsoid target, final boolean isTarget3D,
                                              final double tX, final double tY, final double tZ,
                                              final DatumShiftGrid grid)
    {
        super(source, isSource3D, target, isTarget3D, tX, tY, tZ, false, false, FranceGeocentricInterpolation.PARAMETERS);
        ArgumentChecks.ensureNonNull("grid", grid);
        this.grid = grid;
    }

    /**
     * Creates a transformation between two from geographic CRS. This factory method combines the
     * {@code InterpolatedGeocentricTransform} instance with the steps needed for converting values between
     * degrees to radians. The transform works with input and output coordinates in the following units:
     *
     * <ol>
     *   <li>longitudes in <strong>degrees</strong> relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in <strong>degrees</strong>,</li>
     *   <li>optionally heights above the ellipsoid, in same units than the source ellipsoids axes.</li>
     * </ol>
     *
     * @param factory     The factory to use for creating the transform.
     * @param source      The source ellipsoid.
     * @param isSource3D  {@code true} if the source coordinates have a height.
     * @param target      The target ellipsoid.
     * @param isTarget3D  {@code true} if the target coordinates have a height.
     * @param tX          Initial geocentric <var>X</var> translation in same units than the source ellipsoid axes.
     * @param tY          Initial geocentric <var>Y</var> translation in same units than the source ellipsoid axes.
     * @param tZ          Initial geocentric <var>Z</var> translation in same units than the source ellipsoid axes.
     * @param grid        The grid of datum shifts from source to target datum.
     * @return The transformation between geographic coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform createGeodeticTransformation(final MathTransformFactory factory,
            final Ellipsoid source, final boolean isSource3D,
            final Ellipsoid target, final boolean isTarget3D,
            final double tX, final double tY, final double tZ,
            final DatumShiftGrid grid) throws FactoryException
    {
        final InterpolatedGeocentricTransform tr;
        tr = new InterpolatedGeocentricTransform(source, isSource3D, target, isTarget3D, tX, tY, tZ, grid);
        return tr.context.completeTransform(factory, tr);
    }

    /**
     * Transforms the (λ,φ) or (λ,φ,<var>h</var>) coordinates between two geographic CRS,
     * and optionally returns the derivative at that location.
     *
     * @return {@inheritDoc}
     * @throws TransformException if the point can not be transformed or
     *         if a problem occurred while calculating the derivative.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        final double[] offset = new double[3];
        final double λ = srcPts[srcOff];
        final double φ = srcPts[srcOff+1];
        grid.offsetAt(Math.toDegrees(λ), Math.toDegrees(φ), offset);    // TODO: avoid conversion to degrees.
        return transform(λ, φ, isSource3D ? srcPts[srcOff+2] : 0,
                dstPts, dstOff, offset[0], offset[1], offset[2], derivate);
    }
}
