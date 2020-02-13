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
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.io.Serializable;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.geometry.MismatchedReferenceSystemException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;


/**
 * A transform having sub-areas where more accurate transforms can be used.
 * The global transform must be a reasonable approximation of the specialized transforms.
 * The lower and upper values of given envelopes are inclusive.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see MathTransforms#specialize(MathTransform, Map)
 *
 * @since 1.0
 * @module
 */
class SpecializableTransform extends AbstractMathTransform implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7379277748632094312L;

    /**
     * The global transform to use if there is no suitable specialization.
     */
    private final MathTransform global;

    /**
     * The region where a transform is valid, together with the transform.
     * Contains also a chain of {@code SubArea}s fully included in this area.
     * Shall be unmodified after {@link SpecializableTransform} construction.
     */
    @SuppressWarnings("CloneableImplementsClone")                               // We will not use clone().
    private static final class SubArea extends GeneralEnvelope {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 4197316795428796526L;

        /**
         * The transform to apply in this area.
         */
        final MathTransform transform;

        /**
         * The inverse of the transform, computed when first needed.
         * Synchronization for multi-threading is done (indirectly) in {@link SpecializableTransform#inverse()}.
         *
         * @see #createInverse(SubArea)
         */
        MathTransform inverse;

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
        static void uniformize(final SubArea[] domains) {
            CoordinateReferenceSystem common = null;
            for (SubArea area : domains) {
                do {
                    final CoordinateReferenceSystem crs = area.getCoordinateReferenceSystem();
                    if (common == null) {
                        common = crs;
                    } else if (crs != null && !Utilities.equalsIgnoreMetadata(common, crs)) {
                        throw new MismatchedReferenceSystemException(Errors.format(Errors.Keys.MismatchedCRS));
                    }
                } while ((area = area.specialization) != null);
            }
            for (SubArea area : domains) {
                do area.setCoordinateReferenceSystem(common);
                while ((area = area.specialization) != null);
            }
        }

        /**
         * Creates the inverse transforms. This method should be invoked only once when first needed
         * in a block synchronized (indirectly) by {@link SpecializableTransform#inverse()}.
         */
        static void createInverse(SubArea area) throws NoninvertibleTransformException {
            do {
                area.inverse = area.transform.inverse();
                area = area.specialization;
            } while (area != null);
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
                if (area == null) break;
            }
            return found;
        }

        /**
         * Formats the given area and its transform as a pseudo-WKT.
         * For {@link SpecializableTransform#formatTo(Formatter)} implementation only.
         */
        static void format(SubArea area, final Formatter formatter) {
            while (area != null) {
                formatter.newLine(); formatter.append(area);
                formatter.newLine(); formatter.append(area.transform);
                area = area.specialization;
            }
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
     * The inverse of this transform, computed when first needed.
     * Part of serialization for avoiding rounding error issues.
     *
     * @see #inverse()
     */
    private MathTransform inverse;

    /**
     * Creates a new transform with the given global transform and some amount of specializations.
     *
     * @param  global  the transform to use globally where there is no suitable specialization.
     * @param  specializations  more accurate transforms available in sub-areas.
     */
    SpecializableTransform(final MathTransform global, final Map<Envelope,MathTransform> specializations) {
        this.global = global;
        final int sourceDim = global.getSourceDimensions();
        final int targetDim = global.getTargetDimensions();
        final List<SubArea> areas = new ArrayList<>(specializations.size());
        for (final Map.Entry<Envelope,MathTransform> entry : specializations.entrySet()) {
            MathTransform tr = entry.getValue();
            ensureDimensionMatches(0, sourceDim, tr.getSourceDimensions());
            ensureDimensionMatches(1, targetDim, tr.getTargetDimensions());
            SubArea[] inherited = null;
            if (tr instanceof SpecializableTransform) {
                inherited = ((SpecializableTransform) tr).domains;
                tr = ((SpecializableTransform) tr).global;
            }
            final SubArea area = new SubArea(entry.getKey(), tr);
            addSpecialization(area, areas, sourceDim);
            /*
             * At this point we are usually done for the current SubArea. But if the given MathTransform
             * is another SpecializableTransform, then instead of storing nested SpecializableTransforms
             * we will store directly the specializations that it contains.  This will reduce the amount
             * of steps when transforming coordinates.
             */
            if (inherited != null) {
                for (final SubArea other : inherited) {
                    final SubArea e = new SubArea(other, other.transform);
                    e.intersect(area);
                    addSpecialization(e, areas, sourceDim);
                }
            }
        }
        domains = areas.toArray(new SubArea[areas.size()]);
        SubArea.uniformize(domains);
    }

    /**
     * Helper method for verifying transform dimension consistency.
     *
     * @param  type  0 if verifying source dimension, or 1 if verifying target dimension.
     */
    private static void ensureDimensionMatches(final int type, final int expected, final int actual) {
        if (expected != actual) {
            throw new MismatchedDimensionException(Resources.format(
                    Resources.Keys.MismatchedTransformDimension_3, type, expected, actual));
        }
    }

    /**
     * Verifies if the given {@code area} has the expected number of dimensions,
     * then adds it to {@code domains} list (eventually as a child of an existing node).
     *
     * @param  area     the new sub-area to add.
     * @param  domains  where to add the sub-area (not necessarily directly; maybe as a child of an existing node).
     * @param  dim      expected number of dimensions, for verification purpose.
     */
    private static void addSpecialization(final SubArea area, final List<SubArea> domains, final int dim) {
        if (!area.isEmpty()) {
            if (area.getDimension() != dim) {
                throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_3,
                            "envelope", dim, area.getDimension()));
            }
            for (final SubArea previous : domains) {
                if (previous.addSpecialization(area)) {
                    return;
                }
            }
            for (final SubArea previous : domains) {
                if (area.intersects(previous)) {
                    // Pending implementation of R-Tree in Apache SIS.
                    throw new IllegalArgumentException("Current implementation does not accept overlapping envelopes.");
                }
            }
            domains.add(area);
        }
    }

    /**
     * Gets the dimension of input points.
     */
    @Override
    public final int getSourceDimensions() {
        return global.getSourceDimensions();
    }

    /**
     * Gets the dimension of output points.
     */
    @Override
    public final int getTargetDimensions() {
        return global.getTargetDimensions();
    }

    /**
     * Returns the transform to use for the given domain.
     */
    private MathTransform forDomain(final SubArea domain) {
        return (domain != null) ? domain.transform : global;
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
        final DirectPositionView pos = new DirectPositionView.Double(srcPts, srcOff, global.getSourceDimensions());
        final MathTransform tr = forDomain(SubArea.find(domains, pos));
        if (tr instanceof AbstractMathTransform) {
            return ((AbstractMathTransform) tr).transform(srcPts, srcOff, dstPts, dstOff, derivate);
        } else {
            Matrix derivative = derivate ? tr.derivative(pos) : null;       // Must be before transform(srcPts, …).
            if (dstPts != null) {
                tr.transform(srcPts, srcOff, dstPts, dstOff, 1);
            }
            return derivative;
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
                tr = global;                                // The transform to apply when no specialization is found.
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
    public final void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, final int numPts)
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
    public final void transform(float[] srcPts, int srcOff, final float[] dstPts, int dstOff, final int numPts)
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
    public final void transform(final double[] srcPts, final int srcOff,
                                final float [] dstPts, final int dstOff,
                                final int numPts) throws TransformException
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
    public final void transform(final float [] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final int numPts) throws TransformException
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
    protected final int computeHashCode() {
        return super.computeHashCode() + 7*global.hashCode() ^ Arrays.hashCode(domains);
    }

    /**
     * Compares the specified object with this math transform for equality.
     */
    @Override
    public final boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final SpecializableTransform other = (SpecializableTransform) object;
            return Utilities.deepEquals(global, other.global, mode) &&
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
    protected final String formatTo(final Formatter formatter) {
        formatter.newLine();
        formatter.append(global);
        for (SubArea domain : domains) {
            SubArea.format(domain, formatter);
        }
        formatter.setInvalidWKT(SpecializableTransform.class, null);
        return "Specializable_MT";
    }

    /**
     * Returns the inverse of this transform.
     */
    @Override
    public synchronized MathTransform inverse() throws NoninvertibleTransformException {
        if (inverse == null) {
            inverse = createInverse();
        }
        return inverse;
    }

    /**
     * Invoked at construction time for creating the inverse transform.
     * Overridden by {@link SpecializableTransform2D} for the two-dimensional variant.
     */
    Inverse createInverse() throws NoninvertibleTransformException {
        return new Inverse(this);
    }

    /**
     * The inverse of {@link SpecializableTransform}.
     */
    static class Inverse extends AbstractMathTransform.Inverse implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 1060617594604917167L;

        /**
         * The enclosing transform.
         */
        private final SpecializableTransform forward;

        /**
         * The inverse of {@link SpecializableTransform#global}.
         */
        private final MathTransform global;

        /**
         * Creates the inverse of a specialized transform having the given properties.
         */
        Inverse(final SpecializableTransform forward) throws NoninvertibleTransformException {
            this.forward = forward;
            this.global = forward.global.inverse();
            for (final SubArea domain : forward.domains) {
                SubArea.createInverse(domain);
            }
        }

        /**
         * Returns the inverse of this math transform.
         */
        @Override
        public MathTransform inverse() {
            return forward;
        }

        /**
         * Inverse transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
         */
        @Override
        public final DirectPosition transform(final DirectPosition ptSrc, DirectPosition ptDst) throws TransformException {
            final double[] source = ptSrc.getCoordinate();      // Needs to be first in case ptDst overwrites ptSrc.
            ptDst = global.transform(ptSrc, ptDst);
            final SubArea domain = SubArea.find(forward.domains, ptDst);
            if (domain != null) {
                ptDst = domain.inverse.transform(new DirectPositionView.Double(source), ptDst);
            }
            return ptDst;
        }

        /**
         * Gets the inverse derivative of this transform at a point.
         * This method is overridden for consistency.
         */
        @Override
        public final Matrix derivative(final DirectPosition point) throws TransformException {
            return transform(point.getCoordinate(), 0, null, 0, true);
        }

        /**
         * Inverse transforms a single coordinate point in an array, and optionally computes the transform
         * derivative at that location.
         */
        @Override
        public final Matrix transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, final boolean derivate)
                throws TransformException
        {
            final int srcInc = global.getSourceDimensions();
            final int dstInc = global.getTargetDimensions();
            if (dstPts == null) {
                dstPts = new double[dstInc];                    // Needed for checking if inside a sub-area.
                dstOff = 0;
            } else if (srcPts == dstPts && srcOff + srcInc > dstOff && srcOff < dstOff + dstInc) {
                srcPts = Arrays.copyOfRange(srcPts, srcOff, srcInc);
                srcOff = 0;
            }
            /*
             * Above 'srcPts' dhould keep the source coordinates unchanged even if the source and destination
             * given in arguments overlap.  We need this stability because the source coordinates may be used
             * twice, if 'secondTry' become true.
             */
            MathTransform tr = global;
            boolean secondTry = false;
            Matrix derivative;
            do {
                if (tr instanceof AbstractMathTransform) {
                    derivative = ((AbstractMathTransform) tr).transform(srcPts, srcOff, dstPts, dstOff, derivate);
                } else {
                    tr.transform(srcPts, srcOff, dstPts, dstOff, 1);
                    derivative = derivate ? tr.derivative(new DirectPositionView.Double(srcPts, srcOff, srcInc)) : null;
                }
                if (secondTry) break;
                final SubArea domain = SubArea.find(forward.domains, new DirectPositionView.Double(dstPts, dstOff, dstInc));
                if (domain != null) {
                    tr = domain.inverse;
                    secondTry = true;
                }
            } while (secondTry);
            return derivative;
        }

        /**
         * Invoked for transforming, then verifying if more appropriate transform exists for the result.
         * This implementation is similar to the algorithm applied by {@link SpecializableTransform} parent
         * class, except that {@link SubArea} is verified <em>after</em> transformations instead than before.
         */
        private void transform(final TransformCall transform, final double[] dstPts,
                int srcOff, int dstOff, int srcInc, int dstInc, int numPts) throws TransformException
        {
            final SubArea[] domains = forward.domains;
            transform.apply(global, srcOff, dstOff, numPts);
            final DirectPositionView dst = new DirectPositionView.Double(dstPts, dstOff, dstInc);
            while (numPts > 0) {
                SubArea domain = SubArea.find(domains, dst);
                if (domain == null) {
                    dst.offset += dstInc;
                    numPts--;
                    continue;
                }
                do {
                    final SubArea specialized = domain;             // Contains the specialized transform to use.
                    int num = (dst.offset - dstOff) / dstInc;       // Number of points that are not retransformeD.
                    srcOff += num * srcInc;                         // Skip the source coordinates that are not retransformed.
                    dstOff = dst.offset;                            // Destination index of the first coordinate to retransform.
                    do {
                        dst.offset += dstInc;                       // Destination index after the last coordinate to transform.
                        if (--numPts <= 0) {
                            domain = null;
                            break;
                        }
                        domain = SubArea.find(domain, dst);
                    } while (domain == specialized);
                    num = (dst.offset - dstOff) / dstInc;           // Number of points to retransform.
                    transform.apply(specialized.inverse, srcOff, dstOff, num);
                    srcOff += srcInc * num;
                    dstOff = dst.offset;
                } while (domain != null);
            }
        }

        /**
         * Inverse transforms a list of coordinate points.
         * The transformed points are written directly in the destination array.
         */
        @Override
        public void transform(double[] srcPts, int srcOff, final double[] dstPts, final int dstOff, final int numPts)
                throws TransformException
        {
            if (numPts <= 0) return;
            final int srcInc = global.getSourceDimensions();
            final int dstInc = global.getTargetDimensions();
            if (srcPts == dstPts) {
                final int srcEnd = srcOff + numPts*srcInc;
                if (srcEnd > dstOff || dstOff + numPts*dstInc > srcOff) {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcEnd);
                    srcOff = 0;
                }
            }
            final double[] refPts = srcPts;
            transform((tr, src, dst, num) -> tr.transform(refPts, src, dstPts, dst, num),
                      dstPts, srcOff, dstOff, srcInc, dstInc, numPts);
        }

        /**
         * Inverse transforms a list of coordinate points. This method uses an temporary {@code double[]} buffer
         * for testing {@code SubArea} inclusion with full precision before to cast to {@code float} values.
         */
        @Override
        public void transform(final float[] srcPts, int srcOff,
                              final float[] dstPts, int dstOff, int numPts)
                throws TransformException
        {
            if (numPts <= 0) return;
            final int srcInc = global.getSourceDimensions();
            final int dstInc = global.getTargetDimensions();
            final double[] buffer = new double[numPts * dstInc];
            transform((tr, src, dst, num) -> tr.transform(srcPts, src, buffer, dst, num),
                      buffer, srcOff, 0, srcInc, dstInc, numPts);

            numPts *= dstInc;
            for (int i=0; i<numPts; i++) {
                dstPts[dstOff++] = (float) buffer[i];
            }
        }

        /**
         * Inverse transforms a list of coordinate points. This method uses an temporary {@code double[]} buffer
         * for testing {@code SubArea} inclusion with full precision before to cast to {@code float} values.
         */
        @Override
        public void transform(final double[] srcPts, int srcOff,
                              final float [] dstPts, int dstOff, int numPts) throws TransformException
        {
            if (numPts <= 0) return;
            final int srcInc = global.getSourceDimensions();
            final int dstInc = global.getTargetDimensions();
            final double[] buffer = new double[numPts * dstInc];
            transform((tr, src, dst, num) -> tr.transform(srcPts, src, buffer, dst, num),
                      buffer, srcOff, 0, srcInc, dstInc, numPts);

            numPts *= dstInc;
            for (int i=0; i<numPts; i++) {
                dstPts[dstOff++] = (float) buffer[i];
            }
        }

        /**
         * Inverse transforms a list of coordinate points.
         * The transformed points are written directly in the destination array.
         */
        @Override
        public void transform(final float [] srcPts, int srcOff,
                              final double[] dstPts, int dstOff, int numPts) throws TransformException
        {
            if (numPts <= 0) return;
            final int srcInc = global.getSourceDimensions();
            final int dstInc = global.getTargetDimensions();
            transform((tr, src, dst, num) -> tr.transform(srcPts, src, dstPts, dst, num),
                      dstPts, srcOff, dstOff, srcInc, dstInc, numPts);
        }
    }
}
