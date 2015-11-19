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

import javax.measure.unit.Unit;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.DatumShiftGridFile;
import org.apache.sis.internal.referencing.provider.FranceGeocentricInterpolation;
import org.apache.sis.internal.referencing.provider.GeocentricAffineBetweenGeographic;
import org.apache.sis.internal.referencing.provider.Molodensky;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Debug;


/**
 * Transforms between two geographic CRS by performing geocentric translations interpolated from a grid file.
 * This transform is used mainly for <cite>"France geocentric interpolation"</cite> (ESPG:9655) datum shifts,
 * but Apache SIS implementation allows the use for other regions.
 *
 * <div class="section">Algorithm</div>
 * This class transforms two- or three- dimensional coordinates from a geographic CRS to another geographic CRS.
 * The changes between source and target coordinates are small (usually less than 400 metres), but vary for every
 * position. Those changes are provided in a {@linkplain DatumShiftGrid datum shift grid}, usually loaded from one
 * or two files.
 *
 * <p>Many datum shift grids like NADCON and NTv2 apply the changes directly on geographic coordinates.
 * This relatively simple case is handled by {@link InterpolatedTransform}.
 * But the {@code InterpolatedGeocentricTransform} algorithm uses the grid in a more complex way:</p>
 *
 * <ol>
 *   <li>Convert input geographic coordinate (λ,φ) to geocentric coordinate (X,Y,Z).</li>
 *   <li>Ask {@link DatumShiftGrid} for the offset to apply for coordinate (λ,φ).
 *       But instead of returning a (Δλ, Δφ) offset, the grid shall return a (ΔX, ΔY, ΔZ) offset.</li>
 *   <li>Convert the shifted geocentric coordinate (X + ΔX, Y + ΔY, Z + ΔZ) back to a geographic coordinate.</li>
 * </ol>
 *
 * <div class="note"><b>Source:</b> IGN document {@code NTG_88.pdf},
 * <cite>"Grille de paramètres de transformation de coordonnées"</cite>
 * at <a href="http://www.ign.fr">http://www.ign.fr</a>.
 * Note however that the signs of (ΔX, ΔY, ΔZ) values expected by this class are the opposite of the
 * signs used in NTG_88 document. This is because NTG_88 grid defines shifts from target to source,
 * while this class expects shifts from source to target.</div>
 *
 * Above algorithm is not the same as a (theoretical) {@link EllipsoidToCentricTransform} →
 * {@link InterpolatedTransform} → (inverse of {@code EllipsoidToCentricTransform}) concatenation
 * because the {@code DatumShiftGrid} inputs are geographic coordinates even if the interpolated
 * grid values are in geocentric space.
 *
 * <div class="note"><b>Implementation note:</b>
 * while this transformation is conceptually defined as a translation in geocentric coordinates, the current
 * Apache SIS implementation rather uses the Molodensy (non-abridged) approximation for performance reasons.
 * Errors are less than 1 centimetre for the France geocentric interpolation.
 * By comparison, the finest accuracy reported in the France grid file is 5 centimetres.</div>
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
     * Parameter descriptor, created only when first needed.
     *
     * @see #getParameterDescriptors()
     */
    private static ParameterDescriptorGroup DESCRIPTOR, INTERNAL;

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
     * @param grid        The grid of datum shifts from source to target datum.
     *                    The {@link DatumShiftGrid#offsetAt DatumShiftGrid.offsetAt(…)} method shall expect
     *                    input geographic coordinates in <strong>radians</strong> and computes (ΔX, ΔY, ΔZ)
     *                    shifts from <em>source</em> to <em>target</em> in the unit of source ellipsoid axes.
     *
     * @see #createGeodeticTransformation(MathTransformFactory, Ellipsoid, boolean, Ellipsoid, boolean, DatumShiftGrid)
     */
    protected InterpolatedGeocentricTransform(final Ellipsoid source, final boolean isSource3D,
                                              final Ellipsoid target, final boolean isTarget3D,
                                              final DatumShiftGrid grid)
    {
        super(source, isSource3D,
              target, isTarget3D,
              grid.getAverageOffset(0),
              grid.getAverageOffset(1),
              grid.getAverageOffset(2),
              false,    // Non-abridged Molodensky
              descriptor(grid));

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
     * Note however that the given {@code grid} instance shall expect geographic coordinates (λ,φ)
     * in <strong>radians</strong>.
     *
     * @param factory     The factory to use for creating the transform.
     * @param source      The source ellipsoid.
     * @param isSource3D  {@code true} if the source coordinates have a height.
     * @param target      The target ellipsoid.
     * @param isTarget3D  {@code true} if the target coordinates have a height.
     * @param grid        The grid of datum shifts from source to target datum.
     *                    The {@link DatumShiftGrid#offsetAt DatumShiftGrid.offsetAt(…)} method shall expect
     *                    input geographic coordinates in <strong>radians</strong> and computes (ΔX, ΔY, ΔZ)
     *                    shifts from <em>source</em> to <em>target</em> in the unit of source ellipsoid axes.
     * @return The transformation between geographic coordinates in degrees.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform createGeodeticTransformation(final MathTransformFactory factory,
            final Ellipsoid source, final boolean isSource3D,
            final Ellipsoid target, final boolean isTarget3D,
            final DatumShiftGrid grid) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("grid", grid);
        final InterpolatedGeocentricTransform tr;
        tr = new InterpolatedGeocentricTransform(source, isSource3D, target, isTarget3D, grid);
        return tr.context.completeTransform(factory, tr);
    }

    /**
     * Returns the parameter descriptor to declare for the given grid. This method returns the descriptor of
     * <cite>"France geocentric interpolation"</cite> (ESPG:9655) only if the given grid is recognized as a
     * grid created by the {@link FranceGeocentricInterpolation} for a geographic area likely to be France.
     * Otherwise a more neutral (but non-standard) descriptor is returned.
     */
    private static ParameterDescriptorGroup descriptor(final DatumShiftGrid grid) {
        if (grid instanceof DatumShiftGridFile) {
            return FranceGeocentricInterpolation.PARAMETERS;        // Defined by EPSG.
        }
        synchronized (InterpolatedGeocentricTransform.class) {
            if (DESCRIPTOR == null) {
                DESCRIPTOR = createDescriptor(true);                // Non-standard.
            }
            return DESCRIPTOR;
        }
    }

    /**
     * Creates a Apache SIS descriptor for the {@code InterpolatedGeocentricTransform} parameters.
     * The returned descriptor is non-standard, but allows {@code InterpolatedGeocentricTransform}
     * to be used for other geographic areas than France.
     *
     * @param internal {@code true} for internal parameters, or {@code false} for contextual parameters.
     * @return The parameter descriptor.
     */
    private static ParameterDescriptorGroup createDescriptor(final boolean internal) {
        final ParameterDescriptor<?>[] param = new ParameterDescriptor<?>[] {
                GeocentricAffineBetweenGeographic.DIMENSION,
                GeocentricAffineBetweenGeographic.SRC_SEMI_MAJOR,
                GeocentricAffineBetweenGeographic.SRC_SEMI_MINOR,
                GeocentricAffineBetweenGeographic.TGT_SEMI_MAJOR,
                GeocentricAffineBetweenGeographic.TGT_SEMI_MINOR,
                FranceGeocentricInterpolation.FILE
        };
        if (internal) {
            param[3] = Molodensky.AXIS_LENGTH_DIFFERENCE;
            param[4] = Molodensky.FLATTENING_DIFFERENCE;
        }
        return new ParameterBuilder().setRequired(true)
                .setCodeSpace(Citations.SIS, Constants.SIS)
                .addName("Geocentric interpolation")
                .createGroup(param);
    }

    /**
     * Invoked by the constructor for setting the contextual parameters.
     *
     * @param pg   Where to set the parameters.
     * @param unit Ignored.
     * @param Δf   Ignored.
     */
    @Override
    void setContextualParameters(final Parameters pg, final Unit<?> unit, final double Δf) {
        super.setContextualParameters(pg, unit, Δf);
        // TODO
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

    /**
     * Returns a description of the internal parameters of this {@code InterpolatedGeocentricTransform} transform.
     * The returned group contains parameter descriptors for the number of dimensions and the eccentricity.
     *
     * <div class="note"><b>Note:</b>
     * this method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.</div>
     *
     * @return A description of the internal parameters.
     */
    @Debug
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        synchronized (InterpolatedGeocentricTransform.class) {
            if (INTERNAL == null) {
                INTERNAL = createDescriptor(true);
            }
            return INTERNAL;
        }
    }
}
