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
import java.util.Arrays;
import java.util.Objects;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ArraysExt;


/**
 * A transform having sub-areas where more accurate transforms can be used.
 * The general transform must be a reasonable approximation of the specialized transforms.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
class SpecializableTransform extends AbstractMathTransform {
    /**
     * The generic transform to use if there is no suitable specialization.
     */
    private final MathTransform generic;

    /**
     * The region where a transform is valid, together with the transform.
     * Contains also a chain of {@code SubArea}s fully included in this area.
     * Shall be unmodified after {@link SpecializableTransform} construction.
     */
    private static final class SubArea extends GeneralEnvelope {
        /**
         * The transform to apply in this area.
         */
        final MathTransform transform;

        /**
         * Specialization, or {@code null} if none. If non-null, that sub-area shall be fully included
         * in this {@code SubArea}. The specialization may itself contain another specialization. This
         * form a chain from this wider area to smallest area, where each step is a smaller area.
         *
         * <p>Note that this is note a substitute for an R-Tree. This is an optimization for the common
         * case where coordinates are close to each other (e.g. when iterating in a geometry), in which
         * case we can check immediately for inclusion in smaller areas before to check the wider area.</p>
         */
        private SubArea specialization;

        /**
         * Creates a new area where a transform is valid.
         */
        SubArea(final Envelope area, final MathTransform transform) {
            super(area);
            this.transform = transform;
        }

        /**
         * Tries to add a nested sub-area (a specialization of a specialization).
         *
         * @return whether the given area has been added.
         */
        boolean addSpecialization(final SubArea candidate) {
            if (specialization == null) {
                if (!contains(candidate)) {
                    return false;
                }
            } else if (!candidate.addSpecialization(specialization)) {
                return specialization.addSpecialization(candidate);
            }
            specialization = candidate;
            return true;
        }

        /**
         * Sets the CRS of all given ares to a common value. An exception is thrown if incompatible CRS are found.
         * This method does not verify the number of dimensions; this check should have been done by the caller.
         */
        static void uniformize(final SubArea[] domains) throws InvalidGeodeticParameterException {
            CoordinateReferenceSystem common = null;
            for (SubArea area : domains) {
                do {
                    final CoordinateReferenceSystem crs = area.getCoordinateReferenceSystem();
                    if (common == null) {
                        common = crs;
                    } else if (crs != null && !Utilities.equalsIgnoreMetadata(common, crs)) {
                        throw new InvalidGeodeticParameterException(Errors.format(Errors.Keys.MismatchedCRS));
                    }
                } while ((area = area.specialization) != null);
            }
            for (SubArea area : domains) {
                do area.setCoordinateReferenceSystem(common);
                while ((area = area.specialization) != null);
            }
        }

        /**
         * Returns the area that contains the given position, or {@code null} if none.
         * This method may be replaced by an R-Tree in a future Apache SIS version.
         */
        static SubArea find(final SubArea[] domains, final DirectPosition pos) {
            for (SubArea area : domains) {
                if (area.contains(pos)) {
                    SubArea next = area.specialization;
                    while (next != null && next.contains(pos)) {
                        area = next;
                        next = next.specialization;
                    }
                    return area;
                }
            }
            return null;
        }

        /**
         * Returns the area that contains the given position, looking only in the given area or its specializations.
         * Returns {@code null} if no area has been found.
         */
        static SubArea find(SubArea area, final DirectPosition pos) {
            SubArea found = null;
            while (area.contains(pos)) {
                found = area;
                area = area.specialization;
            }
            return found;
        }

        /**
         * For {@link SpecializableTransform#computeHashCode()} implementation.
         */
        @Override
        public int hashCode() {
            int code = super.hashCode() ^ transform.hashCode();
            if (specialization != null) {
                code += 37 * specialization.hashCode();
            }
            return code;
        }

        /**
         * For {@link SpecializableTransform#equals(Object)} implementation.
         */
        @Override
        public boolean equals(final Object obj) {
            if (super.equals(obj)) {
                final SubArea other = (SubArea) obj;
                return transform.equals(other.transform) && Objects.equals(specialization, other.specialization);
            }
            return false;
        }

        /**
         * Formats this envelope as a "{@code DOMAIN}" element (non-standard).
         * This is used by {@link SpecializableTransform#formatTo(Formatter)}.
         */
        @Override
        protected String formatTo(final Formatter formatter) {
            super.formatTo(formatter);
            return "Domain";
        }
    }

    /**
     * Domains where specialized transforms are valid. The array should be very small.
     * In current implementation, elements in this array shall not overlap.
     * This array may be replaced by an R-Tree in a future Apache SIS version.
     */
    private final SubArea[] domains;

    /**
     * Creates a new transform with the given generic transform and some amount of specializations.
     */
    SpecializableTransform(final MathTransform generic, final Map<Envelope,MathTransform> specializations)
            throws InvalidGeodeticParameterException
    {
        this.generic = generic;
        final int sourceDim = generic.getSourceDimensions();
        final int targetDim = generic.getTargetDimensions();
        int n = 0;
        final SubArea[] areas = new SubArea[specializations.size()];
next:   for (final Map.Entry<Envelope,MathTransform> e : specializations.entrySet()) {
            final MathTransform tr = e.getValue();
            ensureDimensionMatches(0, sourceDim, tr.getSourceDimensions());
            ensureDimensionMatches(1, targetDim, tr.getTargetDimensions());
            final SubArea area = new SubArea(e.getKey(), tr);
            if (area.getDimension() != sourceDim) {
                throw new InvalidGeodeticParameterException(Errors.format(Errors.Keys.MismatchedDimension_3,
                            "envelope", sourceDim, area.getDimension()));
            }
            for (int i=0; i<n; i++) {
                if (areas[i].addSpecialization(area)) {
                    continue next;
                }
            }
            for (int i=0; i<n; i++) {
                if (area.intersects(areas[n])) {
                    // Pending implementation of R-Tree in Apache SIS.
                    throw new InvalidGeodeticParameterException("Current implementation does not accept overlapping envelopes.");
                }
            }
            areas[n++] = area;
        }
        domains = ArraysExt.resize(areas, n);
        SubArea.uniformize(domains);
    }

    /**
     * Helper method for verifying transform dimension consistency.
     *
     * @param  type  0 if verifying source dimension, or 1 if verifying target dimension.
     */
    private static void ensureDimensionMatches(final int type, final int expected, final int actual)
            throws InvalidGeodeticParameterException
    {
        if (expected != actual) {
            throw new InvalidGeodeticParameterException(Resources.format(
                    Resources.Keys.MismatchedTransformDimension_3, type, expected, actual));
        }
    }

    /**
     * Gets the dimension of input points.
     */
    @Override
    public final int getSourceDimensions() {
        return generic.getSourceDimensions();
    }

    /**
     * Gets the dimension of output points.
     */
    @Override
    public final int getTargetDimensions() {
        return generic.getTargetDimensions();
    }

    /**
     * Returns the transform to use for the given domain.
     */
    private MathTransform forDomain(final SubArea domain) {
        return (domain != null) ? domain.transform : generic;
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     * This method delegates to the most specialized transform.
     */
    @Override
    public final DirectPosition transform(final DirectPosition ptSrc, DirectPosition ptDst) throws TransformException {
        return forDomain(SubArea.find(domains, ptSrc)).transform(ptSrc, ptDst);
    }

    /**
     * Gets the derivative of this transform at a point.
     * This method delegates to the most specialized transform.
     */
    @Override
    public final Matrix derivative(final DirectPosition point) throws TransformException {
        return forDomain(SubArea.find(domains, point)).derivative(point);
    }

    /**
     * Transforms a single coordinate point in an array, and optionally computes the transform
     * derivative at that location. This method delegates to the most specialized transform.
     */
    @Override
    public final Matrix transform(final double[] srcPts, final int srcOff,
                                  final double[] dstPts, final int dstOff,
                                  boolean derivate) throws TransformException
    {
        final DirectPositionView pos = new DirectPositionView.Double(srcPts, srcOff, generic.getSourceDimensions());
        final MathTransform tr = forDomain(SubArea.find(domains, pos));
        if (tr instanceof AbstractMathTransform) {
            return ((AbstractMathTransform) tr).transform(srcPts, srcOff, dstPts, dstOff, derivate);
        } else {
            tr.transform(srcPts, srcOff, dstPts, dstOff, 1);
            return derivate ? tr.derivative(pos) : null;
        }
    }

    /**
     * Call of a {@code MathTransform.transform(…)} method with source and target arrays fixed at
     * {@code TransformCall} creation time. This is used for allowing the same implementation to
     * be shared by most {@code transform(…)} methods in {@link SpecializableTransform}.
     */
    @FunctionalInterface
    private interface TransformCall {
        /** Performs the transform at the given offsets. */
        void apply(MathTransform tr, int srcOff, int dstOff, int numPts) throws TransformException;
    }

    /**
     * Transforms a list of coordinate points. This method delegates to the most specialized transform,
     * with single {@code transform(…)} calls for coordinate sequences as long as possible.
     *
     * @param  transform  caller for a {@code MathTransform.transform(…)} method.
     * @param  src        a window over the source points. May be backed by a {@code float[]} or {@code double[]} array.
     * @param  dstOff     where to write the first coordinate in the target array.
     * @param  srcInc     the source dimension, negative if we must iterate backward.
     * @param  dstInc     the target dimension, negative if we must iterate backward.
     * @param  numPts     number of points to transform.
     */
    private void transform(final TransformCall transform, final DirectPositionView src,
            int dstOff, int srcInc, int dstInc, int numPts) throws TransformException
    {
        final boolean downard = (srcInc < 0);
        SubArea domain = SubArea.find(domains, src);
        while (numPts > 0) {
            int srcOff = src.offset;
            final MathTransform tr;
            if (domain == null) {
                tr = generic;                               // The transform to apply when no specialization is found.
                do {                                        // Count how many points will use that transform.
                    src.offset += srcInc;
                    if (--numPts <= 0) break;
                    domain = SubArea.find(domains, src);    // More expansive check than the case where domain is non-null.
                } while (domain == null);
            } else {
                final SubArea previous = domain;
                tr = domain.transform;                      // The specialized transform to apply.
                do {                                        // Count how many points will use that transform.
                    src.offset += srcInc;
                    if (--numPts <= 0) break;
                    domain = SubArea.find(domain, src);     // Cheaper check compared to the case where domain is null.
                } while (domain == previous);
                if (domain == null) {
                    domain = SubArea.find(domains, src);    // Need to update with the more expansive check.
                }
            }
            final int num = (src.offset - srcOff) / srcInc;
            int dstLow = dstOff;
            dstOff += dstInc * num;
            if (downard) {
                srcOff = src.offset - srcInc;
                dstLow = dstOff     - dstInc;
            }
            transform.apply(tr, srcOff, dstLow, num);
        }
    }

    /**
     * Transforms a list of coordinate points.
     * This method delegates to the most specialized transform.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        int srcInc = getSourceDimensions();
        int dstInc = getTargetDimensions();
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, srcInc, dstOff, dstInc, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts-1) * srcInc; srcInc = -srcInc;
                    dstOff += (numPts-1) * dstInc; dstInc = -dstInc;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*srcInc);
                    srcOff = 0;
                    break;
                }
            }
        }
        final double[] refPts = srcPts;
        transform((tr, src, dst, num) -> tr.transform(refPts, src, dstPts, dst, num),
                  new DirectPositionView.Double(srcPts, srcOff, Math.abs(srcInc)),
                  dstOff, srcInc, dstInc, numPts);
    }

    /**
     * Transforms a list of coordinate points.
     * This method delegates to the most specialized transform.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        int srcInc = getSourceDimensions();
        int dstInc = getTargetDimensions();
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, srcInc, dstOff, dstInc, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts-1) * srcInc; srcInc = -srcInc;
                    dstOff += (numPts-1) * dstInc; dstInc = -dstInc;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*srcInc);
                    srcOff = 0;
                    break;
                }
            }
        }
        final float[] refPts = srcPts;
        transform((tr, src, dst, num) -> tr.transform(refPts, src, dstPts, dst, num),
                  new DirectPositionView.Float(srcPts, srcOff, Math.abs(srcInc)),
                  dstOff, srcInc, dstInc, numPts);
    }

    /**
     * Transforms a list of coordinate points. This method delegates to the most specialized transform,
     * with single {@code transform(…)} calls for coordinate sequences as long as possible.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final float [] dstPts, int dstOff, int numPts) throws TransformException
    {
        final int srcDim = getSourceDimensions();
        final int dstDim = getTargetDimensions();
        transform((tr, src, dst, num) -> tr.transform(srcPts, src, dstPts, dst, num),
                  new DirectPositionView.Double(srcPts, srcOff, srcDim),
                  dstOff, srcDim, dstDim, numPts);
    }

    /**
     * Transforms a list of coordinate points. This method delegates to the most specialized transform,
     * with single {@code transform(…)} calls for coordinate sequences as long as possible.
     */
    @Override
    public void transform(final float [] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        final int srcDim = getSourceDimensions();
        final int dstDim = getTargetDimensions();
        transform((tr, src, dst, num) -> tr.transform(srcPts, src, dstPts, dst, num),
                  new DirectPositionView.Float(srcPts, srcOff, srcDim),
                  dstOff, srcDim, dstDim, numPts);
    }

    /**
     * Computes a hash value for this transform.
     * This method is invoked by {@link #hashCode()} when first needed.
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() + 7*generic.hashCode() ^ Arrays.hashCode(domains);
    }

    /**
     * Compares the specified object with this math transform for equality.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final SpecializableTransform other = (SpecializableTransform) object;
            return Utilities.deepEquals(generic, other.generic, mode) &&
                   Utilities.deepEquals(domains, other.domains, mode);
        }
        return false;
    }

    /**
     * Formats the inner part of a <cite>Well Known Text</cite> version 1 (WKT 1) element.
     *
     * <div class="note"><b>Compatibility note:</b>
     * The {@code SPECIALIZABLE_MT} element formatted here is an Apache SIS-specific extension.</div>
     *
     * @param  formatter  the formatter to use.
     * @return the WKT element name, which is {@code "Specializable_MT"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        for (final SubArea domain : domains) {
            formatter.newLine(); formatter.append(generic);
            formatter.newLine(); formatter.append(domain);
            formatter.newLine(); formatter.append(domain.transform);
        }
        formatter.setInvalidWKT(SpecializableTransform.class, null);
        return "Specializable_MT";
    }
}
