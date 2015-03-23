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
package org.apache.sis.referencing.operation.projection;

import java.io.Serializable;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform2D;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Numerics;

import static java.lang.Math.*;
import static java.lang.Double.*;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import java.util.Objects;


/**
 * Base class for conversion services between ellipsoidal and cartographic projections.
 * This conversion works on a normalized spaces, where angles are expressed in radians and
 * computations are performed for a sphere having a semi-major axis of 1. More specifically:
 *
 * <ul class="verbose">
 *   <li>On input, the {@link #transform(double[], int, double[], int, boolean) transform(…)} method
 *   expects (<var>longitude</var>, <var>latitude</var>) angles in <strong>radians</strong>.
 *   Longitudes have the <cite>central meridian</cite> removed before the transform method is invoked.
 *   The conversion from degrees to radians and the longitude rotation are applied by the
 *   {@linkplain ContextualParameters#normalization(boolean) normalize} affine transform.</li>
 *
 *   <li>On output, the {@link #transform(double[],int,double[],int,boolean) transform(…)} method returns
 *   (<var>easting</var>, <var>northing</var>) values on a sphere or ellipse having a semi-major axis length of 1.
 *   The multiplication by the scale factor and the false easting/northing offsets are applied by the
 *   {@linkplain ContextualParameters#normalization(boolean) denormalize} affine transform.</li>
 * </ul>
 *
 * {@code UnitaryProjection} does not store the above cited parameters (central meridian, scale factor, <i>etc.</i>)
 * on intend, in order to make clear that those parameters are not used by subclasses.
 * The ability to recognize two {@code UnitaryProjection}s as {@linkplain #equals(Object, ComparisonMode) equivalent}
 * without consideration for the scale factor (among other) allow more efficient concatenation in some cases
 * (typically some combinations of inverse projection followed by a direct projection).
 *
 * <p>All angles (either fields, method parameters or return values) in this class and subclasses are
 * in radians. This is the opposite of {@link Parameters} where all angles are in CRS-dependent units,
 * typically decimal degrees.</p>
 *
 * <div class="section">Serialization</div>
 * Serialization of this class is appropriate for short-term storage or RMI use, but may not be compatible
 * with future versions. For long term storage, WKT (Well Know Text) or XML are more appropriate.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  André Gosselin (MPO)
 * @author  Rueben Schulz (UBC)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://mathworld.wolfram.com/MapProjection.html">Map projections on MathWorld</a>
 */
public abstract class UnitaryProjection extends AbstractMathTransform2D implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1969740225939106310L;

    /**
     * Tolerance in the correctness of argument values provided to the mathematical functions defined in this class.
     */
    private static final double ARGUMENT_TOLERANCE = 1E-15;

    /**
     * Maximum difference allowed when comparing longitudes or latitudes in radians.
     * A tolerance of 1E-6 is about 0.2 second of arcs, which is about 6 kilometers
     * (computed from the standard length of nautical mile).
     *
     * <p>Some formulas use this tolerance value for testing sines or cosines of an angle.
     * In the sine case, this is justified because sin(θ) ≅ θ when θ is small.
     * Similar reasoning applies to cosine with cos(θ) ≅ θ + π/2 when θ is small.</p>
     */
    static final double ANGLE_TOLERANCE = 1E-6;

    /**
     * Difference allowed in iterative computations. A value of 1E-10 causes the {@link #φ(double)} function
     * to compute the latitude at a precision of 1E-10 radians, which is slightly smaller than one millimetre.
     */
    static final double ITERATION_TOLERANCE = 1E-10;

    /**
     * Maximum number of iterations for iterative computations.
     */
    static final int MAXIMUM_ITERATIONS = 15;

    /**
     * Maximum difference allowed when comparing real numbers (other cases).
     */
    static final double EPSILON = 1E-7;

    /**
     * The parameters used for creating this projection. They are used for formatting <cite>Well Known Text</cite> (WKT)
     * and error messages. Subclasses shall not use the values defined in this object for computation purpose, except at
     * construction time.
     */
    protected final ContextualParameters parameters;

    /**
     * Ellipsoid excentricity, equal to <code>sqrt({@linkplain #excentricitySquared})</code>.
     * Value 0 means that the ellipsoid is spherical.
     */
    protected final double excentricity;

    /**
     * The square of excentricity: ℯ² = (a²-b²)/a² where
     * <var>ℯ</var> is the {@linkplain #excentricity excentricity},
     * <var>a</var> is the <cite>semi-major</cite> axis length and
     * <var>b</var> is the <cite>semi-minor</cite> axis length.
     */
    protected final double excentricitySquared;

    /**
     * The inverse of this map projection.
     */
    private final MathTransform2D inverse;

    /**
     * Constructs a new map projection from the supplied parameters.
     *
     * @param method Description of the map projection parameters.
     * @param parameters The parameters of the projection to be created.
     */
    protected UnitaryProjection(final OperationMethod method, final Parameters parameters) {
        this.parameters = new ContextualParameters(method);
        ensureNonNull("parameters", parameters);
        final double a = parameters.doubleValue(MapProjection.SEMI_MAJOR);
        final double b = parameters.doubleValue(MapProjection.SEMI_MINOR);
        excentricitySquared = 1.0 - (b*b) / (a*a);
        excentricity = sqrt(excentricitySquared);
        inverse = new Inverse();
    }

    /**
     * Returns the parameters used for creating the complete map projection. Those parameters describe a sequence
     * of <cite>normalize</cite> → {@code this} → <cite>denormalize</cite> transforms. They are used for formatting
     * <cite>Well Known Text</cite> (WKT) and error messages. Subclasses shall not use the values defined in the
     * returned object for computation purpose, except at construction time.
     *
     * @return The parameters values for the sequence of <cite>normalize</cite> → {@code this} → <cite>denormalize</cite>
     *         transforms, or {@code null} if unspecified.
     */
    @Override
    protected final ContextualParameters getContextualParameters() {
        return parameters;
    }

    /**
     * Returns a copy of the parameter values for this projection.
     * This base class supplies a value for the following parameters:
     *
     * <ul>
     *   <li>Semi-major axis length, which is set to 1.</li>
     *   <li>Semi-minor axis length, which is set to
     *       <code>sqrt(1 - {@linkplain #excentricitySquared ℯ²})</code>.</li>
     * </ul>
     *
     * Subclasses must complete.
     *
     * @return A copy of the parameter values for this unitary projection.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        final ParameterDescriptorGroup descriptor = super.getParameterDescriptors();
        final ParameterValueGroup values = descriptor.createValue();
        values.parameter(Constants.SEMI_MAJOR).setValue(1.0);
        values.parameter(Constants.SEMI_MINOR).setValue(sqrt(1 - excentricitySquared));
        return values;
    }

    /**
     * Returns {@code true} if this projection is done on a sphere rather than an ellipsoid.
     * Projections on spheres have an {@linkplain #excentricity} equals to zero.
     *
     * @return {@code true} if this projection is on a sphere.
     */
    public final boolean isSpherical() {
        return excentricity == 0;
    }

    /**
     * Converts a single coordinate in {@code srcPts} at the given offset and stores the result
     * in {@code dstPts} at the given offset. In addition, opportunistically computes the
     * transform derivative if requested.
     *
     * <div class="section">Normalization</div>
     * The input ordinates are (<var>λ</var>,<var>φ</var>) (the variable names for <var>longitude</var> and
     * <var>latitude</var> respectively) angles in radians.
     * Input coordinate shall have the <cite>central meridian</cite> removed from the longitude by the caller
     * before this method is invoked. After this method is invoked, the caller will need to multiply the output
     * coordinate by the global <cite>scale factor</cite>
     * and apply the (<cite>false easting</cite>, <cite>false northing</cite>) offset.
     * This means that projections that implement this method are performed on a sphere or ellipse
     * having a semi-major axis length of 1.
     *
     * <div class="note"><b>Note:</b> in <a href="http://trac.osgeo.org/proj/">PROJ.4</a>, the same standardization,
     * described above, is handled by {@code pj_fwd.c}.</div>
     *
     * <div class="section">Argument checks</div>
     * The input longitude and latitude are usually (but not always) in the range [-π … π] and [-π/2 … π/2] respectively.
     * However values outside those ranges are accepted on the assumption that most implementations use those values
     * only in trigonometric functions like {@linkplain Math#sin(double) sine} and {@linkplain Math#cos(double) cosine}.
     * If this assumption is not applicable to a particular subclass, then it is implementor's responsibility to check
     * the range.
     *
     * @param srcPts   The array containing the source point coordinate, as (<var>longitude</var>, <var>latitude</var>)
     *                 angles in <strong>radians</strong>.
     * @param srcOff   The offset of the single coordinate to be converted in the source array.
     * @param dstPts   The array into which the converted coordinate is returned (may be the same than {@code srcPts}).
     *                 Ordinates will be expressed in a dimensionless unit, as a linear distance on a unit sphere or ellipse.
     * @param dstOff   The offset of the location of the converted coordinate that is stored in the destination array.
     * @param derivate {@code true} for computing the derivative, or {@code false} if not needed.
     * @return The matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinate can not be converted.
     */
    @Override
    public abstract Matrix transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, boolean derivate)
            throws ProjectionException;

    /**
     * Inverse converts the single coordinate in {@code srcPts} at the given offset and stores the result in
     * {@code ptDst} at the given offset. The output ordinates are (<var>longitude</var>, <var>latitude</var>)
     * angles in radians, usually (but not necessarily) in the range [-π … π] and [-π/2 … π/2] respectively.
     *
     * <div class="section">Normalization</div>
     * Input coordinate shall have the (<cite>false easting</cite>, <cite>false northing</cite>) removed
     * by the caller and the result divided by the global <cite>scale factor</cite> before this method is invoked.
     * After this method is invoked, the caller will need to add the <cite>central meridian</cite> to the longitude
     * in the output coordinate. This means that projections that implement this method are performed on a sphere
     * or ellipse having a semi-major axis of 1.
     *
     * <div class="note"><b>Note:</b> in <a href="http://trac.osgeo.org/proj/">PROJ.4</a>, the same standardization,
     * described above, is handled by {@code pj_inv.c}.</div>
     *
     * @param srcPts The array containing the source point coordinate, as linear distance on a unit sphere or ellipse.
     * @param srcOff The offset of the point to be converted in the source array.
     * @param dstPts The array into which the converted point coordinate is returned (may be the same than {@code srcPts}).
     *               Ordinates will be (<var>longitude</var>, <var>latitude</var>) angles in <strong>radians</strong>.
     * @param dstOff The offset of the location of the converted point that is stored in the destination array.
     * @throws ProjectionException if the point can't be converted.
     */
    protected abstract void inverseTransform(double[] srcPts, int srcOff, double[] dstPts, int dstOff)
            throws ProjectionException;

    /**
     * Returns the inverse of this map projection.
     * Subclasses do not need to override this method, as they should override
     * {@link #inverseTransform(double[], int, double[], int) inverseTransform(…)} instead.
     *
     * @return The inverse of this map projection.
     */
    @Override
    public MathTransform2D inverse() {
        return inverse;
    }

    /**
     * Inverse of a normalized map projection.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.6
     * @version 0.6
     * @module
     */
    private final class Inverse extends AbstractMathTransform2D.Inverse {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -9138242780765956870L;

        /**
         * Default constructor.
         */
        public Inverse() {
            UnitaryProjection.this.super();
        }

        /**
         * Inverse transforms the specified {@code srcPts} and stores the result in {@code dstPts}.
         * If the derivative has been requested, then this method will delegate the derivative
         * calculation to the enclosing class and inverts the resulting matrix.
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                      double[] dstPts,       int dstOff,
                                final boolean derivate) throws TransformException
        {
            if (!derivate) {
                inverseTransform(srcPts, srcOff, dstPts, dstOff);
                return null;
            } else {
                if (dstPts == null) {
                    dstPts = new double[2];
                    dstOff = 0;
                }
                inverseTransform(srcPts, srcOff, dstPts, dstOff);
                return Matrices.inverse(UnitaryProjection.this.transform(dstPts, dstOff, null, 0, true));
            }
        }
    }

    /**
     * Computes a hash code value for this unitary projection.
     * The default implementation computes a value from the parameters given at construction time.
     *
     * @return The hash code value.
     */
    @Override
    protected int computeHashCode() {
        return parameters.hashCode() + 31 * super.computeHashCode();
    }

    /**
     * Compares the given object with this transform for equivalence. The default implementation checks if
     * {@code object} is an instance of the same class than {@code this}, then compares the excentricity.
     *
     * <p>If this method returns {@code true}, then for any given identical source position, the two compared
     * unitary projections shall compute the same target position. Many of the {@linkplain Parameters projection
     * parameters} used for creating the unitary projections are irrelevant and don not need to be known.
     * Those projection parameters will be compared only if the comparison mode is {@link ComparisonMode#STRICT}
     * or {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}.</p>
     *
     * <div class="note"><b>Example:</b>
     * a {@linkplain Mercator Mercator} projection can be created in the 2SP case with a <cite>standard parallel</cite>
     * value of 60°. The same projection can also be created in the 1SP case with a <cite>scale factor</cite> of 0.5.
     * Nevertheless those two unitary projections applied on a sphere gives identical results. Considering them as
     * equivalent allows the referencing module to transform coordinates between those two projections more efficiently.
     * </div>
     *
     * @param object The object to compare with this unitary projection for equivalence.
     * @param mode The strictness level of the comparison. Default to {@link ComparisonMode#STRICT}.
     * @return {@code true} if the given object is equivalent to this unitary projection.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (super.equals(object, mode)) {
            final double e1, e2;
            final UnitaryProjection that = (UnitaryProjection) object;
            if (mode.ordinal() < ComparisonMode.IGNORE_METADATA.ordinal()) {
                if (!Objects.equals(parameters, that.parameters)) {
                    return false;
                }
                e1 = this.excentricitySquared;
                e2 = that.excentricitySquared;
            } else {
                e1 = this.excentricity;
                e2 = that.excentricity;
            }
            /*
             * There is no need to compare both 'excentricity' and 'excentricitySquared' since
             * the former is computed from the later. In strict comparison mode, we are better
             * to compare the 'excentricitySquared' since it is the original value from which
             * the other value is derived. However in approximative comparison mode, we need
             * to use the 'excentricity', otherwise we would need to take the square of the
             * tolerance factor before comparing 'excentricitySquared'.
             */
            return Numerics.epsilonEqual(e1, e2, mode);
        }
        return false;
    }




    //////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                          ////////
    ////////                           FORMULAS FROM SNYDER                           ////////
    ////////                                                                          ////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Computes functions (9-13) and (15-9) from Snyder.
     * This function is part of the following chapters:
     *
     * <ul>
     *   <li>(9-13) in the <cite>Oblique Mercator projection</cite> chapter.</li>
     *   <li>(15-9) in the <cite>Lambert Conformal Conic projection</cite> chapter.</li>
     *   <li>Negative of part of (7-7) in the <cite>Mercator projection</cite> chapter.
     *       In the case of Mercator projection, this is {@code exp(-y)} where <var>y</var>
     *       is the northing on the unit ellipse.</li>
     * </ul>
     *
     * This function is the converse of {@link #φ(double)}.
     *
     * <div class="section">Properties</div>
     * This function has a periodicity of 2π. The result is always a positive value when φ is valid
     * (more on it below). More specifically its behavior at some particular points is:
     *
     * <ul>
     *   <li>If φ is NaN or infinite, then the result is NaN.</li>
     *   <li>If φ is π/2,  then the result is close to 0.</li>
     *   <li>If φ is 0,    then the result is close to 1.</li>
     *   <li>If φ is -π/2, then the result tends toward positive infinity.
     *       The actual result is not infinity however, but some large value like 1E+10.</li>
     *   <li>If φ, after removal of any 2π periodicity, still outside the [-π/2 … π/2] range,
     *       then the result is a negative number. If the caller is going to compute the logarithm
     *       of the returned value as in the Mercator projection, he will get NaN.</li>
     * </ul>
     *
     * <div class="section">Relationship with other formulas</div>
     * Function (3-1) from Snyder gives the <cite>conformal latitude</cite> χ as (Adam, 1921, p.18, 84):
     *
     * <blockquote>
     * χ = 2∙arctan( tan(π/4 + φ/2) ∙ [(1 - ℯ∙sinφ)/(1 + ℯ∙sinφ)]^(ℯ/2) )  -  π/2
     * </blockquote>
     *
     * This method can compute the part inside the {@code arctan(…)} expression as below
     * (note the negative sign in front of φ):
     *
     * <blockquote>
     * χ = 2∙arctan( t(-φ, sinφ) )  -  π/2
     * </blockquote>
     *
     * @param  φ    The latitude in radians.
     * @param  sinφ The sine of the φ argument (often already computed by the caller).
     * @return Function 9-13 or 15-9, or the negative of function 7-7 from Snyder.
     */
    final double t(final double φ, double sinφ) {
        sinφ *= excentricity;
        return tan(PI/4 - 0.5*φ) / pow((1-sinφ) / (1+sinφ), 0.5*excentricity);
    }

    /**
     * Iteratively solve equation (7-9) from Snyder (<cite>Mercator projection</cite> chapter).
     * This is the converse of the above {@link #t(double, double)} function.
     * The input should be a positive number, otherwise the result will be either outside
     * the [-π/2 … π/2] range, or will be NaN. Its behavior at some particular points is:
     *
     * <ul>
     *   <li>If {@code t} is zero, then the result is close to π/2.</li>
     *   <li>If {@code t} is 1, then the result is close to zero.</li>
     *   <li>If {@code t} is positive infinity, then the result is close to -π/2.</li>
     * </ul>
     *
     * @param  t The value returned by {@link #t(double, double)}.
     * @return The latitude in radians.
     * @throws ProjectionException if the iteration does not converge.
     */
    final double φ(final double t) throws ProjectionException {
        final double hℯ = 0.5 * excentricity;
        double φ = (PI/2) - 2*atan(t);
        for (int i=0; i<MAXIMUM_ITERATIONS; i++) {
            final double ℯsinφ = excentricity * sin(φ);
            final double Δφ = abs(φ - (φ = PI/2 - 2*atan(t * pow((1 - ℯsinφ)/(1 + ℯsinφ), hℯ))));
            if (Δφ <= ITERATION_TOLERANCE) {
                return φ;
            }
        }
        if (isNaN(t)) {
            return NaN;
        }
        throw new ProjectionException(Errors.Keys.NoConvergence);
    }

    /**
     * Gets the partial derivative of the {@link #t(double, double)} method divided by {@code t}.
     * Callers must multiply the return value by {@code t} in order to get the actual value.
     *
     * @param  φ    The latitude.
     * @param  sinφ the sine of latitude.
     * @param  cosφ The cosine of latitude.
     * @return The {@code t(φ, sinφ)} derivative at the specified latitude.
     */
    final double dt_dφ(final double φ, final double sinφ, final double cosφ) {
        final double t = (1 - sinφ) / cosφ;
        return excentricitySquared*cosφ / (1 - excentricitySquared * (sinφ*sinφ)) - 0.5*(t + 1/t);
    }
}
