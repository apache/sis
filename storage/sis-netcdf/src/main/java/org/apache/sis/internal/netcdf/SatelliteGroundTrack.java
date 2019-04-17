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
package org.apache.sis.internal.netcdf;

import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform2D;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.builder.LocalizationGridBuilder;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.math.Vector;
import org.apache.sis.math.Line;


/**
 * An estimation of the position of the satellite for given row and column indices.
 * The calculation done in this class is very rough; the intent is not to give an exact answer,
 * but to convert grid indices to something roughly proportional to latitudes and longitudes
 * in order to make {@link LocalizationGridBuilder} work easier.
 *
 * <p>Current implementation is similar to a <a href="https://en.wikipedia.org/wiki/Sinusoidal_projection">sinusoidal projection</a>
 * in which the central meridian is oblique. That "oblique central meridian" is fitted (by linear regression) to the presumed
 * satellite trajectory. This model may change in any future SIS version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class SatelliteGroundTrack extends AbstractMathTransform2D {
    /**
     * Parameter descriptor for this transform.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = new ParameterBuilder().setRequired(true);
        final ParameterDescriptor<?>[] grids = new ParameterDescriptor<?>[] {
            builder.addName(Constants.CENTRAL_MERIDIAN + "_start").create(DirectPosition.class, null),
            builder.addName(Constants.CENTRAL_MERIDIAN + "_end")  .create(DirectPosition.class, null),
        };
        PARAMETERS = builder.addName("Satellite ground track").createGroup(grids);
    }

    /**
     * Parameters describing (at least partially) this transform.
     * They are used for formatting <cite>Well Known Text</cite> (WKT).
     *
     * @see #getContextualParameters()
     */
    private final ContextualParameters context;

    /**
     * Terms of the λ = <var>slope</var>⋅φ + λ₀ equation estimating the satellite longitude λ for a latitude φ.
     */
    private final double λ0, slope;

    /**
     * The inverse of this transform.
     */
    private final MathTransform2D inverse;

    /**
     * Creates a new instance of this transform.
     *
     * @param grid       localization grid containing longitude and latitude coordinates.
     * @param lonDim     dimension of the longitude coordinates in the given grid.
     * @param direction  0 if the ground track is on rows, of 1 if it is on columns.
     */
    private SatelliteGroundTrack(final LocalizationGridBuilder grid, final int lonDim, final int direction) throws TransformException {
        /*
         * We presume that the row or column in the middle of the localization grid give the
         * coordinates that are closest to coordinates of the actual satellite ground track.
         */
        final int median = (int) grid.getSourceEnvelope(false).getMedian(direction ^ 1);
        final Vector longitudes, latitudes;
        if (direction == 0) {
            longitudes = grid.getRow(lonDim,     median);
            latitudes  = grid.getRow(lonDim ^ 1, median);
        } else {
            longitudes = grid.getColumn(lonDim,     median);
            latitudes  = grid.getColumn(lonDim ^ 1, median);
        }
        final Line line = new Line();
        line.fit(latitudes, longitudes);
        λ0      = line.y0();                                // Longitude in degrees.
        slope   = Math.toRadians(line.slope());             // Take the slope as if all latitudes were given in radians.
        inverse = new Inverse();
        context = new ContextualParameters(PARAMETERS, 2, 2);
        final MatrixSIS normalize = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        normalize.convertAfter(1, DoubleDouble.createDegreesToRadians(), null);
        setPositionParameter(Constants.CENTRAL_MERIDIAN + "_start", line, latitudes.doubleValue(0));
        setPositionParameter(Constants.CENTRAL_MERIDIAN + "_end",   line, latitudes.doubleValue(latitudes.size() - 1));
    }

    /**
     * Sets the {@link DirectPosition} value of a parameter.
     */
    private void setPositionParameter(final String name, final Line line, final double φ) {
        final DirectPosition2D pos = new DirectPosition2D(φ, line.y(φ));
        context.parameter(name).setValue(pos);
    }

    /**
     * Creates a new instance of this transform, or returns {@code null} if no instance can be created.
     * The grid is presumed to contains latitude and longitude coordinates in decimal degrees.
     *
     * @param factory    the factory to use for creating transforms.
     * @param grid       localization grid containing longitude and latitude coordinates.
     * @param lonDim     dimension of the longitude coordinates in the given grid.
     * @param direction  0 if the ground track is on rows, of 1 if it is on columns.
     */
    static MathTransform create(final MathTransformFactory factory, final LocalizationGridBuilder grid,
            final int lonDim, final int direction) throws TransformException, FactoryException
    {
        final SatelliteGroundTrack tr = new SatelliteGroundTrack(grid, lonDim, direction);
        return tr.context.completeTransform(factory, tr);
    }

    /**
     * Returns the parameter values for this math transform.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return context;
    }

    /**
     * Returns the parameters used for creating the complete transformation. Those parameters describe a sequence
     * of <cite>normalize</cite> → {@code this} → <cite>denormalize</cite> transforms.
     */
    @Override
    protected ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Converts a single geographic coordinates into something hopefully more proportional to grid indices.
     * For each coordinate tuple in {@code srcPts}, the first coordinate value is longitude in degrees and
     * the second value is latitude in <strong>radians</strong>. The conversion from degrees to radians is
     * done by the concatenated transform.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        final double λ    = srcPts[srcOff  ];
        final double φ    = srcPts[srcOff+1];
        final double cosφ = Math.cos(φ);
        final double m    = φ * slope + λ0;                 // Central meridian at the given latitude.
        final double Δλ   = λ - m;
        if (dstPts != null) {
            dstPts[dstOff  ] = Δλ * cosφ + m;               // TODO: use Math.fma with JDK9.
            dstPts[dstOff+1] = φ;
        }
        if (!derivate) {
            return null;
        }
        final Matrix2 d = new Matrix2();
        d.m00 = cosφ;
        d.m01 = slope * (1 - cosφ) - Δλ * Math.sin(φ);
        return d;
    }

    /**
     * Returns the inverse of this transform.
     */
    @Override
    public MathTransform2D inverse() {
        return inverse;
    }

    /**
     * The inverse of {@link SatelliteGroundTrack} transform.
     */
    private final class Inverse extends AbstractMathTransform2D.Inverse {
        /**
         * Creates a new instance of the inverse transform.
         */
        Inverse() {
        }

        /**
         * Returns the inverse of this transform, which is the enclosing {@link SatelliteGroundTrack} transform.
         */
        @Override
        public MathTransform2D inverse() {
            return SatelliteGroundTrack.this;
        }

        /**
         * Converts grid indices to geographic coordinates (not necessarily in degrees units).
         * See {@link SatelliteGroundTrack#transform(double[], int, double[], int, boolean)}
         * for the units of measurement.
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final boolean derivate) throws TransformException
        {
            final double x    = srcPts[srcOff  ];
            final double φ    = srcPts[srcOff+1];
            final double cosφ = Math.cos(φ);
            final double m    = φ * slope + λ0;                 // Central meridian at the given latitude.
            final double Δx   = x - m;
            if (dstPts != null) {
                dstPts[dstOff  ] = Δx / cosφ + m;
                dstPts[dstOff+1] = φ;
            }
            if (!derivate) {
                return null;
            }
            final Matrix2 d = new Matrix2();
            d.m00 = 1 / cosφ;
            d.m01 = (Δx * Math.sin(φ) / cosφ - slope) / cosφ + slope;
            return d;
        }
    }
}
