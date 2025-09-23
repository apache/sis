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
import java.util.Arrays;
import java.util.Objects;
import java.io.Serializable;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.internal.RTreeNode;
import org.apache.sis.referencing.internal.shared.DirectPositionView;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;


/**
 * A transform having sub-areas where more accurate transforms can be used.
 * The global transform must be a reasonable approximation of the specialized transforms.
 * The lower and upper values of given envelopes are inclusive.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see MathTransforms#specialize(MathTransform, Map)
 */
class SpecializableTransform extends AbstractMathTransform implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7379277748632094312L;

    /**
     * The global transform to use if there is no suitable specialization.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final MathTransform global;

    /**
     * The region where a transform is valid, together with the transform.
     * Contains also a chain of {@code SubArea}s fully included in this area.
     * Shall be unmodified after {@link SpecializableTransform} construction.
     */
    @SuppressWarnings("CloneableImplementsClone")                               // We will not use clone().
    private static final class SubArea extends RTreeNode {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -7668993675003269862L;

        /**
         * The transform to apply in this area.
         */
        @SuppressWarnings("serial")         // Most SIS implementations are serializable.
        final MathTransform transform;

        /**
         * The inverse of the transform, computed when first needed.
         * Synchronization for multi-threading is done (indirectly) in {@link SpecializableTransform#inverse()}.
         *
         * @see #createInverseTransform()
         */
        @SuppressWarnings("serial")         // Most SIS implementations are serializable.
        MathTransform inverse;

        /**
         * Creates a new area where a transform is valid.
         */
        SubArea(final Envelope area, final MathTransform transform) {
            super(area);
            this.transform = transform;
        }

        /**
         * Creates the inverse transforms. This method should be invoked only once when first needed
         * in a block synchronized (indirectly) by {@link SpecializableTransform#inverse()}.
         */
        final void createInverseTransform() throws NoninvertibleTransformException {
            inverse = transform.inverse();
            for (final RTreeNode node : getChildren()) {
                ((SubArea) node).createInverseTransform();
            }
        }

        /**
         * Formats this area and its transform as a pseudo-WKT.
         * For {@link SpecializableTransform#formatTo(Formatter)} implementation only.
         */
        final void format(final Formatter formatter) {
            formatter.newLine(); formatter.append(this);
            formatter.newLine(); formatter.append(transform);
        }

        /**
         * For {@link SpecializableTransform#computeHashCode()} implementation.
         */
        @Override
        public int hashCode() {
            return super.hashCode() ^ transform.hashCode();
        }

        /**
         * For {@link SpecializableTransform#equals(Object)} implementation.
         */
        @Override
        public boolean equals(final Object obj) {
            return super.equals(obj) && transform.equals(((SubArea) obj).transform);
        }
    }

    /**
     * Domains where specialized transforms are valid. This is the root of an R-Tree.
     * May be {@code null} if there is no R-Tree, in which case {@link #global} should be used instead.
     */
    private final RTreeNode domains;

    /**
     * The inverse of this transform, computed when first needed.
     * This object is included in serialization for avoiding rounding error issues.
     *
     * @see #inverse()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private MathTransform inverse;

    /**
     * Creates a new transform with the given global transform and some number of specializations.
     *
     * @param  global  the transform to use globally where there is no suitable specialization.
     * @param  specializations  more accurate transforms available in sub-areas.
     */
    SpecializableTransform(final MathTransform global, final Map<Envelope,MathTransform> specializations) {
        this.global = global;
        SubArea root = null;
        final int sourceDim = global.getSourceDimensions();
        final int targetDim = global.getTargetDimensions();
        for (final Map.Entry<Envelope,MathTransform> entry : specializations.entrySet()) {
            MathTransform tr = entry.getValue();
            ArgumentChecks.ensureDimensionsMatch("specializations", sourceDim, targetDim, tr);
            /*
             * If the given MathTransform is another SpecializableTransform, then instead of storing nested
             * SpecializableTransforms we will store directly the specializations that it contains. It will
             * reduce the amountof steps when transforming coordinates.
             */
            List<SubArea> inherited = List.of();
            if (tr instanceof SpecializableTransform) {
                inherited = ((SpecializableTransform) tr).roots();
                tr        = ((SpecializableTransform) tr).global;
            }
            final SubArea area = new SubArea(entry.getKey(), tr);
            ArgumentChecks.ensureDimensionMatches("envelope", sourceDim, area);
            if (!area.isEmpty()) {
                if (root == null) root = area;
                else root.addNode(area);
                /*
                 * If the transform was another SpecializableTransform, copies the nested RTreeNode.
                 * A copy is necessary: we shall not modify the nodes of the given transform.
                 */
                for (final SubArea other : inherited) {
                    final SubArea e = new SubArea(other, other.transform);
                    e.intersect(area);
                    if (!e.isEmpty()) {
                        root.addNode(e);
                    }
                }
            }
        }
        domains = (root != null) ? root.finish() : null;
    }

    /**
     * Returns the {@link SubArea} instances at the root of this class. This is the {@link #domains} node,
     * unless that node is a synthetic node created by {@link RTreeNode} when it needs to contain more than
     * one children.
     */
    @SuppressWarnings("unchecked")
    private List<SubArea> roots() {
        if (domains == null) {
            return List.of();
        } else if (domains instanceof SubArea) {
            return List.of((SubArea) domains);
        } else {
            /*
             * We are cheating here since we have a `List<RTreeNode>`. But this `SpecializableTransform`
             * class adds only `SubArea` instance in this list, so a ClassCastException in the code that
             * use this list would be a bug in our conditions of `RTreeNode` use.
             */
            return (List) domains.getChildren();
        }
    }

    /**
     * If this transform has no children, then returns the transform that we should use instead.
     * Otherwise returns {@code null}.
     */
    final MathTransform getSubstitute() {
        return (domains == null) ? global : null;
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
     * Returns the node that contains the given position, or {@code null} if none.
     * This method searches from the root of the tree. It should be invoked only when
     * we do not know what was the last search result.
     */
    private SubArea locate(final DirectPosition pos) {
        /*
         * All nodes should be SubArea instances, except in some circumstances the root node.
         * That root node may be returned by `RTreeNode.locate(…)` if given position is inside
         * the union of all bounding boxes, but not in the bounding box of any specific grid.
         * In such case the caller will fallback on `GridGroup.interpolateInCell(…)`
         * which perform a more extensive search for the nearest grid.
         */
        final RTreeNode node = RTreeNode.locate(domains, pos);
        return (node instanceof SubArea) ? (SubArea) node : null;
    }

    /**
     * Returns the transform to use for the given position, or {@link #global} if none.
     */
    private MathTransform forDomain(final DirectPosition pos) {
        final RTreeNode domain = RTreeNode.locate(domains, pos);
        return (domain instanceof SubArea) ? ((SubArea) domain).transform : global;
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     * This method delegates to the most specialized transform.
     */
    @Override
    public final DirectPosition transform(final DirectPosition ptSrc, DirectPosition ptDst) throws TransformException {
        return forDomain(ptSrc).transform(ptSrc, ptDst);
    }

    /**
     * Gets the derivative of this transform at a point.
     * This method delegates to the most specialized transform.
     */
    @Override
    public final Matrix derivative(final DirectPosition point) throws TransformException {
        return forDomain(point).derivative(point);
    }

    /**
     * Transforms a single coordinate tuple in an array, and optionally computes the transform
     * derivative at that location. This method delegates to the most specialized transform.
     */
    @Override
    public final Matrix transform(final double[] srcPts, final int srcOff,
                                  final double[] dstPts, final int dstOff,
                                  boolean derivate) throws TransformException
    {
        final DirectPositionView pos = new DirectPositionView.Double(srcPts, srcOff, global.getSourceDimensions());
        final MathTransform tr = forDomain(pos);
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
     * Transforms a list of coordinate tuples. This method delegates to the most specialized transform,
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
        SubArea domain = locate(src);
        while (numPts > 0) {
            int srcOff = src.offset;
            final MathTransform tr;
            if (domain == null) {
                tr = global;                                // The transform to apply when no specialization is found.
                do {                                        // Count how many points will use that transform.
                    src.offset += srcInc;
                    if (--numPts <= 0) break;
                    domain = locate(src);                   // More expensive check than the case where domain is non-null.
                } while (domain == null);
            } else {
                RTreeNode next = domain;
                tr = domain.transform;                      // The specialized transform to apply.
                do {                                        // Count how many points will use that transform.
                    src.offset += srcInc;
                    if (--numPts <= 0) break;
                    next = SubArea.locate(domain, src);     // Cheaper check compared to the case where domain is null.
                } while (next == domain);
                domain = (next instanceof SubArea) ? (SubArea) next : null;
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
     * Transforms a list of coordinate tuples.
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
     * Transforms a list of coordinate tuples.
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
     * Transforms a list of coordinate tuples. This method delegates to the most specialized transform,
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
     * Transforms a list of coordinate tuples. This method delegates to the most specialized transform,
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
        return super.computeHashCode() + 7*global.hashCode() ^ Objects.hashCode(domains);
    }

    /**
     * Compares the specified object with this math transform for equality.
     */
    @Override
    public final boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final SpecializableTransform other = (SpecializableTransform) object;
            return Utilities.deepEquals(global, other.global, mode) &&
                   Objects.equals(domains, other.domains);
        }
        return false;
    }

    /**
     * Formats the inner part of a <i>Well Known Text</i> version 1 (WKT 1) element.
     *
     * <h4>Compatibility note</h4>
     * The {@code SPECIALIZABLE_MT} element formatted here is an Apache SIS-specific extension.
     *
     * @param  formatter  the formatter to use.
     * @return the WKT element name, which is {@code "Specializable_MT"}.
     */
    @Override
    protected final String formatTo(final Formatter formatter) {
        formatter.newLine();
        formatter.append(global);
        RTreeNode.walk(domains, (node) -> {
            if (node instanceof SubArea) {
                ((SubArea) node).format(formatter);
            }
        });
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
        @SuppressWarnings("serial")         // Most SIS implementations are serializable.
        private final MathTransform global;

        /**
         * Creates the inverse of a specialized transform having the given properties.
         */
        Inverse(final SpecializableTransform forward) throws NoninvertibleTransformException {
            this.forward = forward;
            this.global = forward.global.inverse();
            for (final SubArea domain : forward.roots()) {
                domain.createInverseTransform();
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
            final SubArea domain = forward.locate(ptDst);
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
         * Inverse transforms a single coordinate tuple in an array, and optionally computes the transform
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
            } else if (srcPts == dstPts) {
                final int srcEnd = srcOff + srcInc;
                if (srcEnd > dstOff && srcOff < dstOff + dstInc) {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcEnd);
                    srcOff = 0;
                }
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
                final SubArea domain = forward.locate(new DirectPositionView.Double(dstPts, dstOff, dstInc));
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
         * class, except that {@link SubArea} is verified <em>after</em> transformations instead of before.
         */
        private void transform(final TransformCall transform, final double[] dstPts,
                int srcOff, int dstOff, int srcInc, int dstInc, int numPts) throws TransformException
        {
            transform.apply(global, srcOff, dstOff, numPts);
            final DirectPositionView dst = new DirectPositionView.Double(dstPts, dstOff, dstInc);
            while (numPts > 0) {
                SubArea domain = forward.locate(dst);
                if (domain == null) {
                    dst.offset += dstInc;                           // Skip point for which there is no specialized transform.
                    numPts--;
                    continue;
                }
                do {
                    RTreeNode next = domain;                        // The specialized transform to use in next iteration.
                    int num = (dst.offset - dstOff) / dstInc;       // Number of points skipped before this loop.
                    srcOff += num * srcInc;                         // Make source offset synchronized with target offset.
                    dstOff = dst.offset;                            // Destination index of the first coordinate to retransform.
                    do {
                        dst.offset += dstInc;                       // Destination index after the last coordinate to transform.
                        if (--numPts <= 0) {
                            next = null;                            // For telling the second `while` condition to stop.
                            break;
                        }
                        next = RTreeNode.locate(domain, dst);
                    } while (next == domain);                       // Continue until we find a change of specialized transform.
                    num = (dst.offset - dstOff) / dstInc;           // Number of points to transform.
                    transform.apply(domain.inverse, srcOff, dstOff, num);
                    domain = (next instanceof SubArea) ? (SubArea) next : null;
                    srcOff += srcInc * num;
                    dstOff = dst.offset;
                } while (domain != null);
            }
        }

        /**
         * Inverse transforms a list of coordinate tuples.
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
         * Inverse transforms a list of coordinate tuples. This method uses an temporary {@code double[]} buffer
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
         * Inverse transforms a list of coordinate tuples. This method uses an temporary {@code double[]} buffer
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
         * Inverse transforms a list of coordinate tuples.
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
