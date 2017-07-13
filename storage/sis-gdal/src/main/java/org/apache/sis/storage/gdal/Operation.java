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
package org.apache.sis.storage.gdal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.GeneralDirectPosition;


/**
 * A math transform which delegate its work to the Proj4 native library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
class Operation extends PJObject implements SingleOperation, MathTransform {
    /**
     * The source and target CRS.
     */
    final CRS source, target;

    /**
     * The inverse transform, created only when first needed.
     */
    private transient Operation inverse;

    /**
     * Creates a new operation for the given source and target CRS.
     */
    Operation(final ReferenceIdentifier name, final CRS source, final CRS target) {
        super(name);
        this.source = source;
        this.target = target;
    }

    /**
     * Returns the operation method.
     *
     * @todo Not yet implemented.
     */
    @Override
    public OperationMethod getMethod() {
        return null;
    }

    /**
     * Returns the parameter values defining this operation.
     *
     * @todo Not yet implemented.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return null;
    }

    /*
     * Trivial methods.
     */
    @Override public CoordinateReferenceSystem getSourceCRS() {return source;}
    @Override public CoordinateReferenceSystem getTargetCRS() {return target;}
    @Override public final int     getSourceDimensions()      {return source.getDimension();}
    @Override public final int     getTargetDimensions()      {return target.getDimension();}
    @Override public MathTransform getMathTransform()         {return this;}
    @Override public String        getOperationVersion()      {return PJ.getVersion();}
    @Override
    public Collection<PositionalAccuracy> getCoordinateOperationAccuracy() {
        return Collections.emptySet();
    }

    /**
     * Returns {@code true} if this transform is the identity transform. Note that a value of
     * {@code false} does not mean that the transform is not an identity transform, since this
     * case is a bit difficult to determine from Proj.4 API.
     */
    @Override
    public boolean isIdentity() {
        return source.pj.equals(target.pj) && source.getDimension() == target.getDimension();
    }

    /**
     * Transforms a single coordinate point.
     */
    @Override
    public DirectPosition transform(final DirectPosition ptSrc, DirectPosition ptDst)
            throws MismatchedDimensionException, TransformException
    {
        final int srcDim = source.getDimension();
        final int tgtDim = target.getDimension();
        if (ptSrc.getDimension() != srcDim) {
            throw new MismatchedDimensionException();
        }
        double[] ordinates = new double[Math.max(srcDim, tgtDim)];
        for (int i=0; i<srcDim; i++) {
            ordinates[i] = ptSrc.getOrdinate(i);
        }
        source.pj.transform(target.pj, ordinates.length, ordinates, 0, 1);
        if (ptDst != null) {
            if (ptDst.getDimension() != tgtDim) {
                throw new MismatchedDimensionException();
            }
            for (int i=0; i<tgtDim; i++) {
                ptDst.setOrdinate(i, ordinates[i]);
            }
        } else {
            if (ordinates.length != tgtDim) {
                ordinates = Arrays.copyOf(ordinates, tgtDim);
            }
            ptDst = new GeneralDirectPosition(ordinates);
        }
        return ptDst;
    }

    /**
     * Transforms an array of coordinate tuples.
     */
    @Override
    public void transform(final double[] srcPts, final int srcOff,
                          final double[] dstPts, final int dstOff,
                          final int numPts) throws TransformException
    {
        final int srcDim = source.getDimension();
        final int tgtDim = target.getDimension();
        if (srcDim == tgtDim) {
            if (srcPts != dstPts || srcOff != dstOff) {
                final int length = tgtDim * numPts;
                System.arraycopy(srcPts, srcOff, dstPts, dstOff, length);
            }
        } else {
            // TODO: need special check for overlapping arrays.
            throw new TransformException("Transformation between CRS of different dimensions not yet supported.");
        }
        source.pj.transform(target.pj, tgtDim, dstPts, dstOff, numPts);
    }

    /**
     * Transforms an array of coordinate tuples.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff,
                          final float[] dstPts, int dstOff,
                          int numPts) throws TransformException
    {
        if (numPts > 0) {
            final int srcDim = source.getDimension();
            final int tgtDim = target.getDimension();
            final int dimension = Math.min(srcDim, tgtDim);
            final int length = dimension * numPts;
            final double[] copy = new double[length];
            int skip = srcDim - dimension;
            int stop = (skip == 0) ? length : dimension;
            for (int i=0;;) {
                copy[i] = srcPts[srcOff + i];
                if (++i == stop) {
                    if (i == length) break;
                    srcOff += skip;
                    stop += dimension;
                }
            }
            source.pj.transform(target.pj, dimension, copy, 0, numPts);
            skip = tgtDim - dimension;
            stop = (skip == 0) ? length : dimension;
            for (int i=0;;) {
                dstPts[dstOff + i] = (float) copy[i];
                if (++i == stop) {
                    if (i == length) break;
                    dstOff += skip;
                    stop += dimension;
                }
            }
        }
    }

    /**
     * Transforms an array of coordinate tuples.
     */
    @Override
    public void transform(final float[]  srcPts, int srcOff,
                          final double[] dstPts, int dstOff,
                          final int numPts) throws TransformException
    {
        if (numPts > 0) {
            final int srcDim = source.getDimension();
            final int tgtDim = target.getDimension();
            final int dimension = Math.min(srcDim, tgtDim);
            final int skipS  = srcDim - dimension;
            final int skipT  = tgtDim - dimension;
            final int length = dimension * numPts;
            int stop = (skipS == 0 && skipT == 0) ? length : dimension;
            if (skipT != 0) {
                Arrays.fill(dstPts, dstOff, dstOff + tgtDim * numPts, Double.NaN);
            }
            for (int i=0;;) {
                dstPts[dstOff + i] = srcPts[srcOff + i];
                if (++i == stop) {
                    if (i == length) break;
                    srcOff += skipS;
                    dstOff += skipT;
                    stop += dimension;
                }
            }
            source.pj.transform(target.pj, tgtDim, dstPts, dstOff, numPts);
        }
    }

    /**
     * Transforms an array of coordinate tuples.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final float[]  dstPts, int dstOff,
                          final int numPts) throws TransformException
    {
        if (numPts > 0) {
            final int srcDim = source.getDimension();
            final int tgtDim = target.getDimension();
            final int dimension = Math.min(srcDim, tgtDim);
            final int length = dimension * numPts;
            final double[] copy;
            if (srcDim == dimension) {
                copy = Arrays.copyOfRange(srcPts, srcOff, srcOff + length);
            } else {
                copy = new double[length];
                for (int i=0; i!=length; i+=dimension) {
                    System.arraycopy(srcPts, srcOff, copy, i, dimension);
                    srcOff += srcDim;
                }
            }
            source.pj.transform(target.pj, dimension, copy, 0, numPts);
            final int skip = tgtDim - dimension;
            int stop = (skip == 0) ? length : dimension;
            for (int i=0;;) {
                dstPts[dstOff + i] = (float) copy[i];
                if (++i == stop) {
                    if (i == length) break;
                    dstOff += skip;
                    stop += dimension;
                }
            }
        }
    }

    /**
     * The Proj4 library does not provide derivative functions.
     */
    @Override
    public Matrix derivative(DirectPosition point) throws TransformException {
        throw new TransformException("Not supported yet.");
    }

    /**
     * Returns the inverse transform.
     */
    @Override
    public synchronized MathTransform inverse() {
        if (inverse == null) {
            inverse = new Operation(name, target, source);
            inverse.inverse = this;
        }
        return inverse;
    }

    /**
     * A specialization of {@link Operation} for map projections.
     */
    static final class Projection extends Operation implements org.opengis.referencing.operation.Projection {
        Projection(final ReferenceIdentifier name, final CRS.Geographic source, final CRS.Projected target) {
            super(name, source, target);
        }

        /**
         * Always {@code null} by definition for map projection, according ISO 19111.
         */
        @Override
        public String getOperationVersion() {
            return null;
        }

        /**
         * Returns the source CRS, which must be geographic or {@code null}.
         */
        @Override
        public final GeographicCRS getSourceCRS() {
            return (GeographicCRS) super.getSourceCRS();
        }

        /**
         * Returns the target CRS, which must be projected or {@code null}.
         */
        @Override
        public final ProjectedCRS getTargetCRS() {
            return (ProjectedCRS) super.getTargetCRS();
        }
    }
}
