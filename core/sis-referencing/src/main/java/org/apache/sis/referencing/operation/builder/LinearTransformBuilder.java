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
package org.apache.sis.referencing.operation.builder;

import java.io.IOException;
import java.util.Arrays;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;

import org.apache.sis.io.TableAppender;
import org.apache.sis.math.Line;
import org.apache.sis.math.Plane;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.NullArgumentException;
import org.opengis.referencing.operation.TransformException;

import static java.lang.Math.sqrt;

/**
 * Creates a linear (usually affine) transform which will map approximatively the given source points to
 * the given target points. The transform coefficients are determined using a <cite>least squares</cite>
 * estimation method, with the assumption that source points are precise and all uncertainty is in the
 * target points.
 *
 * <div class="note"><b>Implementation note:</b>
 * The quantity that current implementation tries to minimize is not strictly the squared Euclidian distance.
 * The current implementation rather processes each target dimension independently, which may not give the same
 * result than if we tried to minimize the squared Euclidian distances by taking all dimensions in account together.
 * This algorithm may change in future SIS versions.
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see LinearTransform
 * @see Line
 * @see Plane
 */
public class LinearTransformBuilder {

    /**
     * Tolerance use to define if points coordinates are considered as regular or not.
     * A coordinate is define as regular if between its value and the integer troncated
     * coordinate value is lesser than tolerance.
     */
    private static double COORDS_TOLERANCE = 1E-12;

    /**
     * Define grid size for each dimension.
     */
    private int[] gridSize;

    /**
     * Define number of expected points exprimate by {@link #gridSize}.
     * @see #getLength(int, int, int[])
     */
    private int gridLength;

    /**
     * Define the current index of inserted source and target points.
     * @see #addNoRegularPoints(double[], double[])
     */
    private int noneRegularPointPosition = 0;

    /**
     * The arrays of source ordinate values, for example (x[], y[]).
     * This is {@code null} if not yet specified.
     */
    private double[][] sources;

    /**
     * The arrays of target ordinate values, for example (x[], y[], z[]).
     * This is {@code null} if not yet specified.
     */
    private double[][] targets;

    /**
     * The transform created by the last call to {@link #create()}.
     */
    private LinearTransform transform;

    /**
     * An estimation of the Pearson correlation coefficient for each target dimension.
     * This is {@code null} if not yet specified.
     */
    private double[] correlation;

    /**
     * Creates a new linear transform builder.
     */
    public LinearTransformBuilder() {
    }

    /**
     * Define this LinearTransform as a regular (2D or more dimension) grid and stipulate its size and the dimension of source points.<br>
     * The dimension of source points is given by sizes array length.<br>
     *
     * For example if you want stipulate 2D grid of width w and height h sizes array equal to {w, h}.
     *
     * @param sizes array which contain grid size for each dimension.
     */
    public LinearTransformBuilder(final int ...sizes) {
        ArgumentChecks.ensureNonNull("sizes", sizes);
        if (sizes.length < 2)
            throw new MismatchedDimensionException("Grid shall specify at least 2 Dimension grid. "
                    + "Expected grid array size 2 or more, found : "+sizes.length);

        gridLength = getLength(0, sizes.length, sizes);

        if (gridLength < 4) {
            throw new IllegalArgumentException("Impossible to define regular grid with "
                    + "less than 4 points. Grid size = "+Arrays.toString(sizes));
        }

        for (int size : sizes) {
            if (gridLength <= size) {
                //-- means all point are stored only on one axis and do not represente multi-dimensionnal grid.
                throw new MismatchedDimensionException("All grid points are referenced on only one axis. "
                        + "Impossible to build regular grid : "+Arrays.toString(sizes));
            }
        }
        gridSize   = sizes.clone();
    }

    //------------------------- private  static ------------------------------//

    /**
     * Extracts the ordinate values of the given points into separated arrays, one for each dimension.
     *
     * @param points The points from which to extract the ordinate values.
     * @param dimension The expected number of dimensions.
     */
    private static double[][] toArrays(final DirectPosition[] points, final int dimension) {
        final int length = points.length;
        final double[][] ordinates = new double[dimension][length];
        for (int j=0; j<length; j++) {
            final DirectPosition p = points[j];
            final int d = p.getDimension();
            if (d != dimension) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "points[" + j + ']', dimension, d));
            }
            for (int i=0; i<dimension; i++) {
                ordinates[i][j] = p.getOrdinate(i);
            }
        }
        return ordinates;
    }

    /**
     * Returns result of geometric serie computing, from stored array coefficient.<br>
     * Computing begin at <var>inclusiveBeginIndex</var> to <var>exclusiveEndIndex - 1</var>.
     *
     * <pre>
     * In other word :
     *
     *         n = exclusiveEndIndex - 1
     *        ─┬──┬─
     *         │  │ array[i] = array[inclusiveBeginIndex] x ... x array[exclusiveEndIndex - 1]
     *
     *         i = inclusiveBeginIndex
     * </pre>
     *
     *
     * @param inclusiveBeginIndex first <strong>INCLUSIVE</strong> index to begin serie computing.
     * @param exclusiveEndIndex last <strong>EXCLUSIVE</strong> index to terminate serie computing.
     * @param array array which contains all coefficients.
     * @return Serie computing result.
     * @throws NullArgumentException if array is {@code null}.
     * @throws IllegalArgumentException if indexes are out of array boundary.
     */
    private static int getLength(final int inclusiveBeginIndex, final int exclusiveEndIndex,
                                 final int[] array) {
        ArgumentChecks.ensureNonNull("array", array);
        ArgumentChecks.ensureBetween("inclusiveBeginIndex", 0, array.length, inclusiveBeginIndex);
        ArgumentChecks.ensureBetween("exclusiveEndIndex", 0, array.length, exclusiveEndIndex);
        int len = 1;
        for (int s = inclusiveBeginIndex; s < exclusiveEndIndex; s++) {
            final int size = array[s];
            ArgumentChecks.ensureStrictlyPositive("Grid size at index : "+s, size);
            len *= size;
        }
        return len;
    }

    /**
     * Returns {@code true} if this coords are considered as regular.<br>
     * In other words return true if all values into the coords array is filled by {@link Integer} values.
     *
     * @param coords
     * @return
     */
    private static boolean isRegular(final double[] coords) {
        for (double coord : coords) {
            final int c = (int) coord;
            if (Math.abs(coord - c) > COORDS_TOLERANCE)
                return false;
        }
        return true;
    }

    /**
     * Increase the length of the array on the 2nd array dimension.<br>
     * Stored datas from given array are copied into new array.
     *
     * @param array reference array.
     * @param newNumberPoints new array length.
     * @return array with increased length.
     */
    private static double[][] increasePointLength(final double[][] array, final int newNumberPoints) {
        final double[][] result = new double[array.length][];
        for (int d = 0; d < array.length; d++) {
            result[d] = Arrays.copyOf(array[d], newNumberPoints);
        }
        return result;
    }


    //------------------------------- private --------------------------------//

    /**
     * Convert <strong>Integer</strong> source coordinates into array index position.
     *
     * @param sourceCoords coordinates from sources point.
     * @return index position into source points array.
     * @throws NullArgumentException if <var>sourceCoords</var> is {@code null}.
     * @throws IllegalArgumentException if <var>sourceCoords</var> do not contains only integers values.
     */
    private int getRegularArrayPosition(final double... sourceCoords) {
        ArgumentChecks.ensureNonNull("sourceCoords", sourceCoords);
        if (!isRegular(sourceCoords))
            throw new IllegalArgumentException("Impossible to define appropriate regular "
                    + "array position from no integer coordinates. Found : "+Arrays.toString(sourceCoords));
        int index = 0;
        for (int sc = 0; sc < sourceCoords.length; sc++) {
            index += ((int)(sourceCoords[sc])) * getLength(0, sc, gridSize);
        }
        return index;
    }

    /**
     * Build source coordinates from regular array index.<br>
     * The returned array has a length equals to source point dimension number.
     *
     * @param index stored point array index.
     * @return array source coordinate from index computing.
     * @throws IllegalArgumentException if <var>index</var> is not eaqual or greater than 0.
     */
    private double[] getRegularSourcePositionFromIndex(final int index) {
        ArgumentChecks.ensurePositive("index", index);
        int rest = index;
        final double[] coords = new double[gridSize.length];
        for (int gid = gridSize.length - 1; gid >= 0; gid--) {
            final int div = getLength(0, gid, gridSize);
            final int nb = rest / div;
            coords[gid] = nb;
            rest = rest % div;
        }
        assert rest == 0;
        return coords;
    }

    /**
     * Add couple of points into none regular grid.
     *
     * @param sourceCoords grid point source coordinates.
     * @param targetCoords target point coordinates.
     */
    private void addNoRegularPoints(final double[] sourceCoords, final double[] targetCoords) {
        assert targets != null && targets.length == targetCoords.length;
        assert sources != null && sources.length == sourceCoords.length;

        for (int sd = 0; sd < sourceCoords.length; sd++) {
            sources[sd][noneRegularPointPosition] = sourceCoords[sd];
        }

        for (int td = 0; td < targetCoords.length; td++) {
            targets[td][noneRegularPointPosition] = targetCoords[td];
        }
        noneRegularPointPosition++;
    }

    /**
     * Add couple of points into regular grid.
     *
     * @param sourceCoords
     * @param targetCoords
     */
    private void addRegularPoints(final double[] sourceCoords, final double[] targetCoords) {
        assert sources == null;
        assert targets != null;

        final int targetIndex = getRegularArrayPosition(sourceCoords);

        for (int td = 0; td < targetCoords.length; td++) {
            targets[td][targetIndex] = targetCoords[td];
        }
    }

    /**
     * Create a 2D array of dimension length for the first ordinate and grid length for the second.
     *
     * @param array pointer which will be affected.
     * @param dimension array dimension.
     * @see #getLength(int, int, int[])
     */
    private static double[][] createArray(final int dimension, final int length) {
        double[][] array = new double[dimension][];
        for (int d = 0; d < dimension; d++) {
            final double[] coord = new double[length];
            Arrays.fill(coord, Double.NaN);
            array[d] = coord;
        }
        return array;
    }

    /**
     * Decant all targets and sources points from regular grid into another none regular grid.
     */
    private void decantTargetArray() {
        assert sources != null;
        //-- keep in memory precedently insertions
        final int tCLen = targets.length;
        final double[][] tartar = targets.clone();

        //-- init target
        targets = createArray(gridLength, tCLen);

        //-- fill new none regular grid with precedently inserted points
        final double[] tartarDim0 = tartar[0];
        for (int tid = 0; tid < tartarDim0.length; tid++) {
            if (!Double.isNaN(tartarDim0[tid])) {
                //-- if first target ordinate not NAN
                //-- add into target array directly
                for (int i = 0; i < tCLen; i++) {
                    final double coordi = tartar[i][tid];
                    assert !Double.isNaN(coordi);
                    targets[i][noneRegularPointPosition] = coordi;
                }
                //-- compute source array coordinates
                final double[] srcPoint = getRegularSourcePositionFromIndex(tid);
                assert srcPoint.length == gridSize.length;
                //-- add directly into source array
                for (int srcd = 0; srcd < gridSize.length; srcd++) {
                    sources[srcd][noneRegularPointPosition] = srcPoint[srcd];
                }
                noneRegularPointPosition++;
            }
        }
        gridLength = 0;
        gridSize   = null;
    }

    /**
     * Notifies this localization grid that a coordinate is about to be changed. This method
     * invalidate any transforms previously created.
     */
    private void notifyChanges() {
        transform   = null;
        correlation = null;
    }

    /**
     * Fits a plane through the longitude or latitude values. More specifically, find
     * coefficients <var>c</var>, <var>cx</var> and <var>cy</var> for the following
     * equation:
     *
     * {@preformat math
     *     [longitude or latitude] = c + cx*x + cy*y
     * }
     *
     * where <var>x</var> and <var>cx</var> are grid coordinates.
     * Coefficients are computed using the least-squares method.
     *
     *
     * @param grid   The grid to process, either {@link #gridX} or {@link #gridY}.
     * @param offset 0 for fitting longitude values, or 1 for fitting latitude values
     *               (assuming that "real world" coordinates are longitude and latitude values).
     * @param coeff  An array of length 6 in which to store plane's coefficients.
     *               Coefficients will be store in the following order:
     *               {@code coeff[0 + offset] = cx;}
     *               {@code coeff[2 + offset] = cy;}
     *               {@code coeff[4 + offset] = c;}
     */
    private double fitPlane(final double[] grid, final int offset, final double[] coeff) {
        final int width  = gridSize[0];
        final int height = gridSize[1];
        /*
         * Computes the sum of x, y and z values. Computes also the sum of x*x, y*y, x*y, z*x
         * and z*y values. When possible, we will avoid to compute the sum inside the loop and
         * use the following identities instead:
         *
         *           1 + 2 + 3 ... + n    =    n*(n+1)/2              (arithmetic series)
         *        1² + 2² + 3² ... + n²   =    n*(n+0.5)*(n+1)/3
         */
        double x,y,z, z2, xx,yy, xy, zx,zy;
        z = zx = zy = z2 = 0; // To be computed in the loop.
        int n = 0;
        for (int yi=0; yi<height; yi++) {
            for (int xi=0; xi<width; xi++) {
                assert getRegularArrayPosition(xi, yi) == n : n;
                final double zi = grid[n];
                if (Double.isNaN(zi))
                    throw new IllegalStateException("The point at coordinate : ("+xi+", "+yi+") is not referenced.");
                z  += zi;
                z2 += zi * zi; //-- prepare correlation computing
                zx += zi*xi;
                zy += zi*yi;
                n++;
            }
        }
        assert n == width * height : n;

        x  = (n * (double) (width -1))            / 2;
        y  = (n * (double) (height-1))            / 2;
        xx = (n * (width -0.5) * (width -1))      / 3;
        yy = (n * (height-0.5) * (height-1))      / 3;
        xy = (n * (double)((height-1)*(width-1))) / 4;
        /*
         * Solves the following equations for cx and cy:
         *
         *    ( zx - z*x )  =  cx*(xx - x*x) + cy*(xy - x*y)
         *    ( zy - z*y )  =  cx*(xy - x*y) + cy*(yy - y*y)
         */
        //-- plan coefficients computing
        final double pzx = zx - z*x/n;
        final double pzy = zy - z*y/n;
        final double pxx = xx - x*x/n;
        final double pxy = xy - x*y/n;
        final double pyy = yy - y*y/n;
        final double den = (pxy * pxy - pxx * pyy);
        final double cy  = (pzx * pxy - pzy * pxx) / den;
        final double cx  = (pzy * pxy - pzx * pyy) / den;
        final double c   = (z - (cx * x + cy * y)) / n;
        coeff[0 + offset] = cx;
        coeff[1 + offset] = cy;
        coeff[2 + offset] = c;

        /*
         * At this point, the model is computed. Now computes an estimation of the Pearson
         * correlation coefficient. Note that both the z array and the z computed from the
         * model have the same average, called sum_z below (the name is not true anymore).
         *
         * We do not use double-double arithmetic here since the Pearson coefficient is
         * for information purpose (quality estimation).
         */
        final double mean_x = x / n;
        final double mean_y = y / n;
        final double mean_z = z / n;

        final double sum_ds2 = cx*cx*xx + 2*cx*cy*xy + cy*cy*yy
                                + (cx*mean_x + cy*mean_y) * (cx*(n*mean_x - 2*x) + cy*(n*mean_y - 2*y));
        final double sum_dz2 = z2 + mean_z * (n * mean_z - 2 * z);
        final double sum_dsz = cx*(zx - mean_z*x) + cy*(zy - mean_z*y) - (cy*mean_y + cx*mean_x)*(n*mean_z - z);

        return Math.min(sum_dsz / sqrt(sum_ds2 * sum_dz2), 1);
    }


    //-------------------------- package protected ---------------------------//

    /**
     * Returns {@code true} if this class is considered as valid.
     * This object is valid if its attributs are consistent into regular grid
     * mode or consistent into none regular grid mode.
     * @return {@code true} if all attributs are consistent else {@code false}.
     */
    boolean isValid() {

        if (sources == null
         && targets == null
         && gridSize == null)
            return true;

        //-- sources
        if (sources == null) {
            if (gridSize == null)
                throw new AssertionError("Impossible to have null sources points and grid size not defined.");
            if (noneRegularPointPosition != 0)
                throw new AssertionError("With grid define as regular noneRegularPointPosition attribut should be 0. Found : "+noneRegularPointPosition);
            if (gridLength == 0)
                throw new AssertionError("With grid define as regular gridLength should be equals to gridsize length. "
                        + "Expected : "+getLength(0, gridSize.length, gridSize)+", found : "+gridLength);
        } else {
            if (gridSize != null)
                throw new AssertionError("Impossible to have defined sources points and grid size defined.");
            if (gridLength != 0)
                throw new AssertionError("With grid define as none regular gridLength should be equals to 0. Found : "+gridLength);
            if (targets == null)
                throw new AssertionError("Target points should never be null");
            if (targets[0].length != sources[0].length)
                throw new AssertionError("With grid defined as none regular, sources points and targets points "
                        + "should own same points number. Sources points length : "+sources[0].length+", targets points length : "+targets[0].length);
            if (sources[0].length != noneRegularPointPosition)
                throw new AssertionError("With grid define as none regular sources points length and noneRegularPointPosition should be equals. "
                        + "Sources points length : "+sources[0].length+", noneRegularPointPosition : "+noneRegularPointPosition);
        }

        //-- targets
        if (targets != null) {
            if (sources == null) {
                if (gridSize != null) {
                    if (gridLength != targets[0].length)
                        throw new AssertionError("Target number points do not match with expected stipulate regular grid size."
                                + " Grid size : ("+Arrays.toString(gridSize)+"), expected target points numbers : "
                                +gridLength+", target points found : "+targets[0].length);
                } else {
                    throw new AssertionError("Only targets points are defined, please set grid size or set source points.");
                }
            } else {
                if (sources[0].length != targets[0].length)
                    throw new AssertionError("Target number points do not match with expected sources number points."
                                + " Sources points number found : "+sources[0].length+", target points number found : "+targets[0].length);
                if (gridSize != null)
                    throw new AssertionError("With grid define as none regular gridSize should be null. Found : "+Arrays.toString(gridSize));
                if (gridLength != 0)
                    throw new AssertionError("With grid define as none regular gridLength should be equals to 0. Found : "+gridLength);
            }
        }
        return true;
    }

    //------------------------------- public ---------------------------------//

    /**
     * Set all source and target points from an array which contain all of them.
     * {@preformat text
     * Array should be organize as follow :
     *
     *       ┌──┬─    ─┬────┬──┬────┬─    ─┬────┬──┬────┬─   ─┬─────
     *       │  │ .... │src0│..│srcN│ .... │tar0│..│tarN│ ... │... next points
     *       └──┴─    ─┴────┴──┴────┴─    ─┴────┴──┴────┴─   ─┴─────
     *       └────────>└────────────>
     *     sourceOffset sourceDimension
     *
     *       └────────────────────────────>└────────────>
     *          TargetOffset                TargetDimension
     *
     *       └────────────────────────────────────────────────>
     *                      Tie Point Length
     * }
     *
     * <p><b>Limitation:</b> in current implementation, the source points must be one or two-dimensional.
     * This restriction may be removed in a future SIS version.</p>
     *
     * <p>Moreover sourceDimension equal to zero is allowed. In this case source point will be considered as a regular integer grid.</p>
     *
     * @param sourceOffset array offset of first source point coordinate.
     * @param sourceDimension source point dimension number.
     * @param targetOffset array offset of first target point coordinate.
     * @param targetdimension target point dimension number.
     * @param tiePointLength array length of one point element.
     * @param tiePoints tie point array.
     * @throws IllegalArgumentException if points array haven't got length multiple of tiePointLength.
     */
    public void setModelTiePoints(final int sourceOffset,   final int sourceDimension,
                                  final int targetOffset,   final int targetdimension,
                                  final int tiePointLength, final double[] tiePoints)
    {
        ArgumentChecks.ensurePositive("sourceOffset", sourceOffset);
        ArgumentChecks.ensureStrictlyPositive("sourceDimension", sourceDimension);
        ArgumentChecks.ensureStrictlyPositive("targetOffset", targetOffset);
        ArgumentChecks.ensureStrictlyPositive("targetdimension", targetdimension);
        ArgumentChecks.ensureStrictlyPositive("tiePointLength", tiePointLength);
        ArgumentChecks.ensureNonNull("tiePoints", tiePoints);

        final int tiePointsLen = tiePoints.length;
        if (tiePointsLen % tiePointLength != 0)
            throw new IllegalArgumentException("tiePoint array should have array length multiple of tiePointLenth."
                    + " Array length : "+tiePointsLen+"  One point length = "+tiePointLength);

        //-- verify source and targets offsets
        final int min = Math.max(sourceOffset, targetOffset);
        final int max = Math.min(sourceOffset + sourceDimension, targetOffset + targetdimension);

        if (min < max)
            throw new MismatchedDimensionException("Source offset, dimension indexes overlaps target offset, dimension indexes."
                    + "Source offset : "+sourceOffset
                    + ", Source dimension : "+sourceDimension
                    + ", Target offset : "+targetOffset
                    + ", Target dimension : "+targetdimension);

        final int nbTiePoints = tiePointsLen / tiePointLength;

        if (gridSize == null) {
            //-- maybe its not efficient
            if (sources == null)
                sources = createArray(sourceDimension, nbTiePoints);
            else
                sources = increasePointLength(sources, sources[0].length + nbTiePoints);

            if (targets == null)
                targets = createArray(targetdimension, nbTiePoints);
            else
                targets = increasePointLength(targets, targets[0].length + nbTiePoints);
        }

        final double[] srcPt    = new double[sourceDimension];
        final double[] targetPt = new double[targetdimension];

        for (int pt = 0; pt < nbTiePoints; pt ++) {

            final int originPt  = pt * tiePointLength;
            //-- source
            final int srcOffset = originPt + sourceOffset;
            System.arraycopy(tiePoints, srcOffset, srcPt, 0, sourceDimension);
            //-- target
            final int tarOffset = originPt + targetOffset;
            System.arraycopy(tiePoints, tarOffset, targetPt, 0, targetdimension);

            setPoints(srcPt, targetPt);
        }
        assert isValid();
        notifyChanges();
    }

    /**
     * Sets source and target point. The number of target points shall be the same than the number of source points.
     * Target points can have any number of dimensions (not necessarily 2), but all targets points shall have
     * the same number of dimensions.
     * The number of source points shall be the same than the number of target points.
     *
     * <p><b>Limitation:</b> in current implementation, the source points must be one or two-dimensional.
     * This restriction may be removed in a future SIS version.</p>
     *
     * @param sourcePoint
     * @param targetPoint
     */
    private void setPoints(final double[] sourcePoint, final double[] targetPoint) {
        ArgumentChecks.ensureNonNull("sourcePoint", sourcePoint);
        ArgumentChecks.ensureNonNull("targetPoint", targetPoint);

        final int tCLen = targetPoint.length;
        if (gridSize == null) {
            assert sources != null;
            assert targets != null;
        }

        if (targets != null && tCLen != targets.length)
                throw new MismatchedDimensionException("TargetPoint must be same dimension than grid. "
                        + "Expected : "+targets.length+", found : "+tCLen);

        if (sources != null) {
            //-- source array not null means none regular grid.
            assert targets != null;
            final int sCLen = sourcePoint.length;
            if (sCLen != sources.length)
                throw new MismatchedDimensionException("SourceCoords must be same dimension than grid. "
                        + "Expected : "+sources.length+", found : "+sCLen);
            addNoRegularPoints(sourcePoint, targetPoint);

        } else {
            //-- sources array is null means regular grid
            assert gridSize != null;
            assert noneRegularPointPosition == 0;
            if (isRegular(sourcePoint)) {
                //-- if source point contains only integer values means continue as regular grid.

                if (targets == null)
                    //-- if first point insertion
                    targets = createArray(tCLen, gridLength);

                addRegularPoints(sourcePoint, targetPoint);

            } else {
                //-- current point is not regular

                //-- create source array
                sources = createArray(sourcePoint.length, gridLength);//-- gridSize len == source dimension

                if (targets == null) {
                    //-- if first point insertion
                    targets = createArray(tCLen, gridLength);
                } else {
                    //-- if they exist some precedently inserted point we must "decant"
                    //-- precedently regular inserted points, into none regular grid
                    decantTargetArray();
                }
                assert gridSize == null;
                //-- add current none regular point
                addNoRegularPoints(sourcePoint, targetPoint);
            }
        }
        notifyChanges();
    }

    /**
     * Sets source and target point. The number of target points shall be the same than the number of source points.
     * Target points can have any number of dimensions (not necessarily 2), but all targets points shall have
     * the same number of dimensions.
     * The number of source points shall be the same than the number of target points.
     *
     * <p><b>Limitation:</b> in current implementation, the source points must be one or two-dimensional.
     * This restriction may be removed in a future SIS version.</p>
     *
     * @param sourcePoints
     * @param targetPoints
     */
    public void setPoints(final DirectPosition[] sourcePoints, final DirectPosition[] targetPoints) {
        ArgumentChecks.ensureNonNull("sourcePoints", sourcePoints);
        ArgumentChecks.ensureNonNull("targetPoints", targetPoints);
        if (sourcePoints.length != targetPoints.length)
            throw new IllegalArgumentException("Source and target array points shall be the same number of points. "
                    + "Source number points : "+sourcePoints.length+", Target number of points : "+targetPoints.length);
        if (sourcePoints.length == 0) {
            noneRegularPointPosition = 0;
            sources = null;
            targets = null;
            transform   = null;
            correlation = null;
        } else {
            if (gridSize == null) {
                //-- maybe its not efficient
                if (sources == null)
                    sources = createArray(sourcePoints[0].getDimension(), sourcePoints.length);
                else
                    sources = increasePointLength(sources, sources[0].length + sourcePoints.length);

                if (targets == null)
                    targets = createArray(targetPoints[0].getDimension(), targetPoints.length);
                else
                    targets = increasePointLength(targets, targets[0].length + targetPoints.length);
            }
            for (int p = 0; p < sourcePoints.length; p++) {
                setPoints(sourcePoints[p].getCoordinate(), targetPoints[p].getCoordinate());
            }
        }
    }

    /**
     * Sets the source points. The number of points shall be the same than the number of target points.
     *
     * <p><b>Limitation:</b> in current implementation, the source points must be one or two-dimensional.
     * This restriction may be removed in a future SIS version.</p>
     *
     * @param  points The source points, assumed precise.
     * @throws MismatchedDimensionException if at least one point does not have the expected number of dimensions.
     */
    public void setSourcePoints(final DirectPosition... points)
            throws MismatchedDimensionException
    {
        ArgumentChecks.ensureNonNull("points", points);
        if (points.length != 0) {
            sources = toArrays(points, points[0].getDimension() == 1 ? 1 : 2);
        } else {
            sources = null;
        }
        //-- when this setter is used, mandatory pass into no regular grid.
        gridSize = null;
        gridLength = 0;
        noneRegularPointPosition = points.length;
        transform   = null;
        correlation = null;
    }

    /**
     * Sets the target points. The number of points shall be the same than the number of source points.
     * Target points can have any number of dimensions (not necessarily 2), but all points shall have
     * the same number of dimensions.
     *
     * @param  points The target points, assumed uncertain.
     * @throws MismatchedDimensionException if not all points have the same number of dimensions.
     */
    public void setTargetPoints(final DirectPosition... points)
            throws MismatchedDimensionException
    {
        ArgumentChecks.ensureNonNull("points", points);
        if (points.length != 0) {
            targets = toArrays(points, points[0].getDimension());
        } else {
            targets = null;
        }
        //-- when this setter is used, mandatory pass into no regular grid.
        gridSize = null;
        gridLength = 0;
        noneRegularPointPosition = points.length;
        transform   = null;
        correlation = null;
    }

    /*
     * No getters yet because we did not determined what they should return.
     * Array? Collection? Map<source,target>?
     */

    /**
     * Creates a linear transform approximation from the source points to the target points.
     * This method assumes that source points are precise and all uncertainty is in the target points.
     *
     * @return The fitted linear transform.
     */
    public LinearTransform create() {
        if (transform == null) {
            isValid();
            final double[][] sources = this.sources;  // Protect from changes.
            final double[][] targets = this.targets;

            if ((gridSize == null && (sources == null || targets == null))) {
                throw new IllegalStateException(Errors.format(
                        Errors.Keys.MissingValueForProperty_1, (sources == null) ? "sources" : "targets"));
            }

            final int sourceDim = (gridSize != null) ? gridSize.length : sources.length;
            final int targetDim = targets.length;
            correlation = new double[targetDim];
            final MatrixSIS matrix = Matrices.createZero(targetDim + 1, sourceDim + 1);
            switch (sourceDim) {
                case 1: {
                    final Line line = new Line();
                    for (int j=0; j<targets.length; j++) {
                        correlation[j] = line.fit(sources[0], targets[j]);
                        matrix.setElement(j, 0, line.slope());
                        matrix.setElement(j, 1, line.y0());
                    }
                    break;
                }
                case 2: {

                    if (sources == null) {
                        //-- means regular grid
                        //-- correlation ??
                        double[] elements = new double[(targetDim + 1)*(sourceDim + 1)];
                        for (int j = 0; j < targets.length; j++) {
                            correlation[j] = fitPlane(targets[j], j * (sourceDim + 1), elements);
                        }
                        matrix.setElements(elements);
                    } else {
                        final Plane plan = new Plane();
                        for (int j=0; j<targets.length; j++) {
                            correlation[j] = plan.fit(sources[0], sources[1], targets[j]);
                            matrix.setElement(j, 0, plan.slopeX());
                            matrix.setElement(j, 1, plan.slopeY());
                            matrix.setElement(j, 2, plan.z0());
                        }
                    }
                    break;
                }
                default: throw new AssertionError(sourceDim); // Should have been verified by setSourcePoints(…) method.
            }
            matrix.setElement(targetDim, sourceDim, 1);
            transform = MathTransforms.linear(matrix);
        }
        return transform;
    }

    /**
     *
     * @param translationUnit
     * @return
     * @throws TransformException
     */
    public DatumShiftGrid getResidus(final Unit<? extends Quantity> translationUnit)
            throws TransformException {
        if (gridSize == null)
            throw new IllegalStateException("Impossible to compute Datum grid from none regular grid, the grid should be regular.");

        if (transform == null)
            transform = create();
        assert isValid();

        final DoubleDatumShiftGrid ddsg = new DoubleDatumShiftGrid(Unit.ONE, MathTransforms.identity(gridSize.length),
                                                                    gridSize, translationUnit,
                                                                    targets, COORDS_TOLERANCE);
        return ddsg;
    }

    /**
     * Returns the correlation coefficients of the last transform created by {@link #create()},
     * or {@code null} if none. If non-null, the array length is equals to the number of target
     * dimensions.
     *
     * @return Estimation of correlation coefficients for each target dimension, or {@code null}.
     */
    public double[] correlation() {
        return (correlation != null) ? correlation.clone() : null;
    }

    /**
     * Returns a string representation of this builder for debugging purpose.
     *
     * @return A string representation of this builder.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(Classes.getShortClassName(this)).append('[');
        if (sources != null) {
            buffer.append(sources[0].length).append(" points");
        }
        buffer.append(']');
        if (transform != null) {
            final String lineSeparator = System.lineSeparator();
            buffer.append(':').append(lineSeparator);
            final TableAppender table = new TableAppender(buffer, " ");
            table.setMultiLinesCells(true);
            table.append(Matrices.toString(transform.getMatrix()));
            table.nextColumn();
            table.append(lineSeparator);
            table.append("  ");
            table.append(Vocabulary.format(Vocabulary.Keys.Correlation));
            table.append(" =");
            table.nextColumn();
            table.append(Matrices.create(correlation.length, 1, correlation).toString());
            try {
                table.flush();
            } catch (IOException e) {
                throw new AssertionError(e); // Should never happen since we wrote into a StringBuilder.
            }
        }
        return buffer.toString();
    }
}
