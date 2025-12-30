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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import org.apache.sis.util.internal.shared.Strings;
import static org.apache.sis.coverage.grid.GridCoverage.BIDIMENSIONAL;

// Specific to the main branch:
import org.apache.sis.coverage.PointOutsideCoverageException;


/**
 * An iterator over the sample values evaluated at given coordinates.
 * This iterator has many layers. The first layer iterates over slices.
 * Then the second layer iterates over images, and a third layer over tiles.
 * A last layer returns null values for points that are outside the coverage.
 *
 * <h2>Limitations</h2>
 * Current implementation performs nearest-neighbor sampling only.
 * A future version may provide interpolations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-576">SIS-576</a>
 */
abstract class ValuesAtPointIterator implements Spliterator<double[]> {
    /**
     * The dimensions of grid dimensions taken as <var>x</var> and <var>y</var> image axes.
     * Static constants for now, may become configurable fields in the future.
     */
    private static final int X_DIMENSION = 0, Y_DIMENSION = 1;

    /**
     * A mask for deciding when a slice width or height become too large for a single read operation.
     * This is both for avoiding 32-bits integer overflow (keeping in mind that the maximal image size
     * may be the square of this size) and for reducing the risk of reading unnecessary tiles between
     * points separated by potentially large empty spaces.
     */
    private static final long MAXIMUM_SLICE_SIZE_MASK = ~0x7FFF;

    /**
     * The minimum and maximum value that we accept for a {@code double} value before rounding to {@code long}.
     * A margin of 2048 is added, which is the smallest change that can be applied on {@code Long.MIN/MAX_VALUE}
     * as given by {@code Math.ulp(Long.MIN_VALUE)}. We add this tolerance because this class aim to return the
     * {@code long} grid coordinate value closest to the {@code double} value and we consider an 1 <abbr>ULP</abbr>
     * error as close enough.
     */
    static final double DOMAIN_MINIMUM = Long.MIN_VALUE - 2048d,
                        DOMAIN_MAXIMUM = Long.MAX_VALUE + 2048d;

    /**
     * Grid coordinates of points to evaluate, rounded to nearest integers.
     * This array may be shared by many iterators and should not be modified.
     * This array is null in the particular case of the {@link Null} subclass.
     */
    protected final long[] nearestXY;

    /**
     * Index of the first grid coordinate of the next point to evaluate.
     * This is incremented by {@value GridCoverage2D#BIDIMENSIONAL} after each evaluation.
     */
    protected int indexOfXY;

    /**
     * Index after the last coordinate of the last point to evaluate.
     */
    protected final int limitOfXY;

    /**
     * Supplier of the exception to throw if a point is outside the coverage bounds.
     * The function argument is the index of the coordinate tuple which is outside the grid coverage.
     * If this supplier is {@code null}, then null arrays will be returned instead of throwing an exception.
     */
    protected final IntFunction<PointOutsideCoverageException> ifOutside;

    /**
     * Creates a new iterator which will traverses a subset of the given grid coordinates.
     * Subclasses should initialize {@link #indexOfXY} to the index of the first valid coordinate.
     *
     * @param nearestXY  grid coordinates of points to evaluate, or {@code null}.
     * @param limitOfXY  index after the last coordinate of the last point to evaluate.
     * @param ifOutside  supplier of exception for points outside the coverage bounds, or {@code null}.
     */
    protected ValuesAtPointIterator(final long[] nearestXY, final int limitOfXY,
                                    final IntFunction<PointOutsideCoverageException> ifOutside)
    {
        this.nearestXY = nearestXY;
        this.limitOfXY = limitOfXY;
        this.ifOutside = ifOutside;
    }

    /**
     * Creates a new iterator with the given grid coordinates. The {@code gridCoords} array is the result
     * of applying the inverse of the "grid to CRS" transform on user-supplied "real world" coordinates,
     * then resolving wraparounds. This constructor rounds these grid coordinates to nearest integers.
     *
     * @param  coverage    the coverage which will be evaluated.
     * @param  gridCoords  the grid coordinates as floating-point numbers.
     * @param  numPoints   number of points in the array.
     * @param  ifOutside   supplier of exception for points outside the coverage bounds, or {@code null}.
     * @return the iterator.
     */
    static ValuesAtPointIterator create(final GridCoverage coverage, final double[] gridCoords, int numPoints,
                                        final IntFunction<PointOutsideCoverageException> ifOutside)
    {
        return Slices.create(coverage, gridCoords, 0, numPoints, ifOutside).shortcut();
    }

    /**
     * Returns the number of remaining points to evaluate.
     */
    @Override
    public final long estimateSize() {
        return (limitOfXY - indexOfXY) / BIDIMENSIONAL;
    }

    /**
     * Returns the number of remaining points to evaluate.
     */
    @Override
    public final long getExactSizeIfKnown() {
        return estimateSize();
    }

    /**
     * Returns the characteristics of this iterator.
     * The iterator is {@link #SIZED} and {@link #ORDERED}.
     * Whether it is {@link #NONNULL} depends on whether there is an {@link #ifOutside} supplier.
     */
    @Override
    public final int characteristics() {
        return (ifOutside == null) ? (SIZED | SUBSIZED | ORDERED) : (SIZED | SUBSIZED | ORDERED | NONNULL);
    }

    /**
     * Returns a string representation of this iterator for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "estimateSize", estimateSize(), "nullIfOutside", ifOutside == null);
    }




    /**
     * Base class of iterators that contains other iterators.
     * A {@link Slices} is a group of two-dimensional images.
     * An {@link Image} is a group of tiles.
     */
    private static abstract class Group extends ValuesAtPointIterator {
        /**
         * Index of the first grid coordinate of the first point which is inside each child.
         * Each value in this array is the index of the first valid element of {@link #nearestXY}.
         * Values in this array shall be in strictly increasing order.
         */
        private final int[] firstGridCoordOfChildren;

        /**
         * Index of the next child in which to evaluate a sequence of points.
         * This is an index in the {@link #firstGridCoordOfChildren} array.
         */
        private int nextChildIndex;

        /**
         * Maximal value (exclusive) of {@link #nextChildIndex}.
         */
        private final int upperChildIndex;

        /**
         * Iterator over pixel values of the current child.
         * This is an instance of {@link Image}, {@link Tile} or {@link Null}.
         * This field become {@code null} when the iteration is finished.
         */
        protected ValuesAtPointIterator current;

        /**
         * Creates a new iterator over a subset of the given iterator.
         * This iterator will evaluate a prefix of the sequence of points of the given iterator.
         * Caller should change the parent iterator to a suffix after this constructor returned.
         * This constructor is for {@link #trySplit()} implementation only.
         *
         * @param  parent           the iterator from which to create a prefix.
         * @param  upperChildIndex  index after the last child that this iterator can use.
         */
        protected Group(final Group parent, final int upperChildIndex) {
            super(parent.nearestXY, parent.firstGridCoordOfChildren[upperChildIndex], parent.ifOutside);
            this.indexOfXY                = parent.indexOfXY;
            this.firstGridCoordOfChildren = parent.firstGridCoordOfChildren;
            this.current                  = parent.current;
            this.nextChildIndex           = parent.nextChildIndex;
            this.upperChildIndex          = upperChildIndex;
        }

        /**
         * Creates a new iterator which will traverse a subset of the given grid coordinates.
         *
         * @param nearestXY  grid coordinates of points to evaluate, or {@code null}.
         * @param limitOfXY  index after the last coordinate of the last point to evaluate.
         * @param ifOutside  supplier of exception for points outside the coverage bounds, or {@code null}.
         */
        protected Group(final long[] nearestXY,
                        final int    limitOfXY,
                        final int[]  firstGridCoordOfChildren,
                        final int    upperChildIndex,
                        final IntFunction<PointOutsideCoverageException> ifOutside)
        {
            super(nearestXY, limitOfXY, ifOutside);
            this.firstGridCoordOfChildren = firstGridCoordOfChildren;
            this.upperChildIndex = upperChildIndex;
        }

        /**
         * If this iterator can be replaced by a more direct one, returns the more direct iterator.
         * Otherwise returns {@code this} if there are more points to traverse.
         * May return {@code null} if the iteration is finished.
         */
        protected final ValuesAtPointIterator shortcut() {
            return (nextChildIndex >= upperChildIndex) ? current : this;
        }

        /**
         * Returns an iterator over the points to evaluate in the next child, or {@code null} if none.
         * Invoking this method may cause the loading or the computation of a tile.
         *
         * @throws PointOutsideCoverageException if a point is outside the grid coverage
         *         and the iterator is not configured for returning {@code null} in such case.
         */
        protected final ValuesAtPointIterator nextChild() {
            int childIndex = nextChildIndex;
            if (childIndex < upperChildIndex) {
                indexOfXY = firstGridCoordOfChildren[childIndex];
                int stopAtXY = (++nextChildIndex < upperChildIndex) ? firstGridCoordOfChildren[nextChildIndex] : limitOfXY;
                return createChild(childIndex, stopAtXY);
            }
            return null;
        }

        /**
         * Creates a child which will iterate over the {@link #nearestXY} coordinates
         * from {@link #indexOfXY} inclusive to {@code stopAtXY} exclusive.
         * Invoking this method may cause the loading or the computation of a tile.
         *
         * @param  childIndex  index of the child to create.
         * @param  stopAtXY    index after the last {@link #nearestXY} value to use.
         * @throws PointOutsideCoverageException if a point is outside the grid coverage
         *         and the iterator is not configured for returning {@code null} in such case.
         */
        abstract ValuesAtPointIterator createChild(int childIndex, int stopAtXY);

        /**
         * Creates a new iterator covering a prefix of the points.
         *
         * @param  stopAtXY  index after the last grid coordinates of the last point covered by the prefix
         * @return a new iterator covering the specified prefix of the points.
         */
        abstract Group createPrefix(int stopAtXY);

        /**
         * Tries to split this iterator. If successful, the returned iterator is a prefix
         * of the sequence of points to evaluate, and this iterator become the remaining.
         *
         * @return an iterator covering a prefix of the points, or {@code null} if this iterator cannot be split.
         */
        @Override
        public final Spliterator<double[]> trySplit() {
            if (current == null) {
                return null;
            }
            // Find the middle of the remaining number of points to evaluate.
            int i = nextChildIndex + (upperChildIndex - nextChildIndex) / 2;
            i = Arrays.binarySearch(firstGridCoordOfChildren, nextChildIndex, upperChildIndex, i);
            if (i < 0) i = ~i;   // Tild operator, not minus. It gives the insertion point.
            if (i > nextChildIndex && i < upperChildIndex) {
                final var prefix = createPrefix(i);
                nextChildIndex = prefix.upperChildIndex;
                indexOfXY = prefix.limitOfXY;
                current = nextChild();
                return prefix;
            } else {
                return current.trySplit();    // After this call, `current` become a suffix.
            }
        }

        /**
         * Sends the sample values to the caller if the iteration is not finished.
         */
        @Override
        public final boolean tryAdvance(final Consumer<? super double[]> action) {
            while (current != null) {
                if (current.tryAdvance(action)) {
                    return true;
                }
                current = nextChild();
            }
            return false;
        }

        /**
         * Sends remaining sample values to the caller until the end of the iteration.
         */
        @Override
        public final void forEachRemaining(final Consumer<? super double[]> action) {
            while (current != null) {
                current.forEachRemaining(action);
                current = nextChild();
            }
        }
    }




    /**
     * An iterator over the sample values of points contained in different slices of a <var>n</var>-dimensional cube.
     * This class searches for sequences of consecutive points that are located on the same two-dimensional slice,
     * then creates an {@link Image} iterator for each slice.
     */
    private static final class Slices extends Group {
        /**
         * The coverage from which to evaluate the pixel values.
         */
        private final GridCoverage coverage;

        /**
         * Indices of images in which sequences of points will be evaluated.
         * Some array elements may be {@code null} if a sequence of points is
         * outside the grid coverage or has NaN coordinate values.
         */
        private final GridExtent[] imageExtents;

        /**
         * Creates a new iterator over a subset of the given iterator.
         * This iterator will evaluate a prefix of the sequence of points of the given iterator.
         * Caller should change the parent iterator to a suffix after this constructor returned.
         * This constructor is for {@link #trySplit()} implementation only.
         *
         * @param  parent  the iterator from which to create a prefix.
         * @param  stopAt  index after the last image that this iterator can use.
         */
        private Slices(final Slices parent, final int stopAt) {
            super(parent, stopAt);
            this.coverage     = parent.coverage;
            this.imageExtents = parent.imageExtents;
        }

        /**
         * Workaround for RFE #4093999 ("Relax constraint on placement of this()/super() call in constructors").
         */
        private Slices(final GridCoverage coverage,
                       final long[]       nearestXY,
                       final int          limitOfXY,
                       final int[]        firstGridCoordOfChildren,
                       final int          upperChildIndex,
                       final GridExtent[] imageExtents,
                       final IntFunction<PointOutsideCoverageException> ifOutside)
        {
            super(nearestXY, limitOfXY, firstGridCoordOfChildren, upperChildIndex, ifOutside);
            this.coverage = coverage;
            this.imageExtents = imageExtents;
            current = nextChild();
        }

        /**
         * Creates a new iterator with the given grid coordinates. The {@code gridCoords} array is the result
         * of applying the inverse of the "grid to CRS" transform on user-supplied "real world" coordinates,
         * then resolving wraparounds. This constructor rounds these grid coordinates to nearest integers.
         *
         * @todo Retrofit in above constructor after RFE #4093999.
         *
         * @param coverage          the coverage which will be evaluated.
         * @param gridCoords        the grid coordinates as floating-point numbers.
         * @param gridCoordsOffset  index of the first grid coordinate value.
         * @param numPoints         number of points in the array.
         * @param ifOutside         supplier of exception for points outside the coverage bounds, or {@code null}.
         */
        static Slices create(final GridCoverage coverage, final double[] gridCoords, int gridCoordsOffset, int numPoints,
                             final IntFunction<PointOutsideCoverageException> ifOutside)
        {
            final int dimension  = coverage.gridGeometry.getDimension();
            final var extentLow  = new long[dimension];
            final var extentHigh = new long[dimension];
            final var nearestXY  = new long[numPoints * BIDIMENSIONAL];
            var imageExtents     = new GridExtent[1];   // Length is 1 in the common case of two-dimensional data.
            var imageFirstCoords = new int[1];
            int imageCount       = 0;
            int indexOfXY        = 0;
            int limitOfXY        = 0;
            while (--numPoints >= 0) {
                boolean wasOutside = false;
                for (int i=0; i<dimension; i++) {
                    double c = gridCoords[gridCoordsOffset + i];
                    wasOutside |= !(c >= DOMAIN_MINIMUM && c <= DOMAIN_MAXIMUM);    // Use `!` for catching NaN.
                    extentLow[i] = Math.round(c);
                }
                if (wasOutside && ifOutside != null) {
                    throw ifOutside.apply(indexOfXY / BIDIMENSIONAL);
                }
                long lowerX, upperX, lowerY, upperY;
                nearestXY[limitOfXY++] = lowerX = upperX = extentLow[X_DIMENSION];
                nearestXY[limitOfXY++] = lowerY = upperY = extentLow[Y_DIMENSION];
                gridCoordsOffset += dimension;
changeOfSlice:  while (numPoints != 0) {
                    boolean isValid = true;
                    // Note: following code assumes that X_DIMENSION and Y_DIMENSION are the two first dimensions.
                    for (int i = BIDIMENSIONAL; i < dimension; i++) {
                        double c = gridCoords[gridCoordsOffset + i];
                        isValid &= (c >= DOMAIN_MINIMUM && c <= DOMAIN_MAXIMUM);
                        if (Math.round(c) != extentLow[i]) {
                            break changeOfSlice;
                        }
                    }
                    final double cx = gridCoords[gridCoordsOffset + X_DIMENSION];
                    final double cy = gridCoords[gridCoordsOffset + Y_DIMENSION];
                    isValid &= (cx >= DOMAIN_MINIMUM && cx <= DOMAIN_MAXIMUM) &&
                               (cy >= DOMAIN_MINIMUM && cy <= DOMAIN_MAXIMUM);
                    if (isValid == wasOutside) {
                        break;
                    }
                    final long x = Math.round(cx);
                    final long y = Math.round(cy);
                    final long xmin = Math.min(x, lowerX);
                    final long ymin = Math.min(y, lowerY);
                    final long xmax = Math.max(x, upperX);
                    final long ymax = Math.max(y, upperY);
                    if ((((xmax - xmin) | (ymax - ymin)) & MAXIMUM_SLICE_SIZE_MASK) != 0) {
                        break;
                    }
                    lowerX = xmin;
                    lowerY = ymin;
                    upperX = xmax;
                    upperY = ymax;
                    nearestXY[limitOfXY++] = x;
                    nearestXY[limitOfXY++] = y;
                    gridCoordsOffset += dimension;
                    numPoints--;
                }
                /*
                 * Reached the end of a sequence of points in the same image. We will read later a rectangular
                 * region containing all these points. We do not try to merge with an hypothetic reuse of this
                 * image later in the iteration, because it would increase the risk that we load too much data
                 * if the points are spread in distant regions. Instead, we rely on `ComputedImage` cache.
                 */
                if (imageCount >= imageExtents.length) {
                    imageExtents = Arrays.copyOf(imageExtents, imageExtents.length * 2);
                    imageFirstCoords = Arrays.copyOf(imageFirstCoords, imageFirstCoords.length * 2);
                }
                if (wasOutside) {
                    // Leave `imageExtents[imageCount]` to null.
                    imageFirstCoords[imageCount++] = indexOfXY;
                    indexOfXY = limitOfXY;
                    continue;
                }
                System.arraycopy(extentLow, 0, extentHigh, 0, extentHigh.length);
                extentLow [X_DIMENSION] = lowerX;
                extentLow [Y_DIMENSION] = lowerY;
                extentHigh[X_DIMENSION] = upperX;
                extentHigh[Y_DIMENSION] = upperY;
                imageExtents[imageCount] = new GridExtent(null, extentLow, extentHigh, true);
                imageFirstCoords[imageCount++] = indexOfXY;

                // Make grid coordinates relative to the region that we requested.
                while (indexOfXY < limitOfXY) {
                    nearestXY[indexOfXY++] -= lowerX;
                    nearestXY[indexOfXY++] -= lowerY;
                }
            }
            return new Slices(coverage, nearestXY, limitOfXY, imageFirstCoords, imageCount, imageExtents, ifOutside);
        }

        /**
         * Creates a child which will iterate over the {@link #nearestXY} coordinates
         * from {@link #indexOfXY} inclusive to {@code stopAtXY} exclusive.
         */
        @Override
        final ValuesAtPointIterator createChild(final int childIndex, final int stopAtXY) {
            final GridExtent extent = imageExtents[childIndex];
            if (extent != null) try {
                return Image.create(this, stopAtXY, coverage.render(extent)).shortcut();
            } catch (DisjointExtentException cause) {
                if (ifOutside != null) {
                    var e = ifOutside.apply(indexOfXY / BIDIMENSIONAL);
                    e.initCause(cause);
                    throw e;
                }
            }
            return new Null(indexOfXY, stopAtXY);
        }

        /**
         * Creates a new iterator covering a prefix of the points.
         * This is invoked by {@link #trySplit()} implementation.
         */
        @Override
        final Group createPrefix(int stopAtXY) {
            return new Slices(this, stopAtXY);
        }
    }




    /**
     * An iterator over the sample values of point contained in the same image.
     * For performance reasons, this method recycles the same array in calls to
     * {@link Consumer#accept(Object)}.
     */
    private static final class Image extends Group {
        /**
         * The image from which to evaluate the pixel values.
         */
        private final RenderedImage image;

        /**
         * Indices of tiles in which sequences of points will be evaluated.
         * The values in this array are (<var>x</var>, <var>y</var>) pairs.
         * Therefore, the length of this array is at least twice the number
         * of tiles where to get values.
         */
        private final int[] tileIndices;

        /**
         * Whether a tile at a given index does not exist. It happens when a sequence of points
         * is outside the image and the iterator is allowed to return {@code null} in such cases.
         * The corresponding values of {@link #tileIndices} are meaningless and should be ignored.
         */
        private final BitSet tileIsAbsent;

        /**
         * Creates a new iterator over a subset of the given iterator.
         * This iterator will evaluate a prefix of the sequence of points of the given iterator.
         * Caller should change the parent iterator to a suffix after this constructor returned.
         * This constructor is for {@link #trySplit()} implementation only.
         *
         * @param  parent  the iterator from which to create a prefix.
         * @param  stopAt  index after the last tile that this iterator can use.
         */
        private Image(final Image parent, final int stopAt) {
            super(parent, stopAt);
            this.image        = parent.image;
            this.tileIndices  = parent.tileIndices;
            this.tileIsAbsent = parent.tileIsAbsent;
        }

        /**
         * Workaround for RFE #4093999 ("Relax constraint on placement of this()/super() call in constructors").
         */
        private Image(final RenderedImage image,
                      final long[] nearestXY,
                      final int    limitOfXY,
                      final int[]  firstGridCoordOfChildren,
                      final int    upperChildIndex,
                      final int[]  tileIndices,
                      final BitSet tileIsAbsent,
                      final IntFunction<PointOutsideCoverageException> ifOutside)
        {
            super(nearestXY, limitOfXY, firstGridCoordOfChildren, upperChildIndex, ifOutside);
            this.image        = image;
            this.tileIndices  = tileIndices;
            this.tileIsAbsent = tileIsAbsent;
            current = nextChild();
        }

        /**
         * Creates a new iterator for the specified image.
         *
         * @todo Retrofit in above constructor after RFE #4093999.
         *
         * @param parent     the parent iterator from which to inherit the grid coordinates.
         * @param limitOfXY  index after the last coordinate of the last point to evaluate.
         * @param image      the image from which to get the sample values.
         */
        static Image create(final ValuesAtPointIterator parent, final int limitOfXY, final RenderedImage image) {
            final long xmin            = image.getMinX();
            final long ymin            = image.getMinY();
            final long xmax            = image.getWidth()  + xmin;
            final long ymax            = image.getHeight() + ymin;
            final long tileWidth       = image.getTileWidth();
            final long tileHeight      = image.getTileHeight();
            final long tileGridXOffset = image.getTileGridXOffset();
            final long tileGridYOffset = image.getTileGridYOffset();
            int[] tileIndices = new int[BIDIMENSIONAL];
            int[] tileFirstCoords = new int[1];
            final var tileIsAbsent = new BitSet();
            final long[] nearestXY = parent.nearestXY;
            int tileCount, indexOfXY = parent.indexOfXY;
nextTile:   for (tileCount = 0; indexOfXY < limitOfXY; tileCount++) {
                if (tileCount >= tileFirstCoords.length) {
                    tileFirstCoords = Arrays.copyOf(tileFirstCoords, tileFirstCoords.length * 2);
                    tileIndices = Arrays.copyOf(tileIndices, tileIndices.length * 2);
                }
                tileFirstCoords[tileCount] = indexOfXY;
                boolean wasOutside = false;
                do {
                    long x = nearestXY[indexOfXY++];
                    long y = nearestXY[indexOfXY++];
                    if (x >= xmin && x < xmax && y >= ymin && y < ymax) {
                        if (wasOutside) {
                            indexOfXY -= BIDIMENSIONAL;  // Push back those valid coordinates.
                            break;
                        }
                        final long tileX    = Math.floorDiv(x - tileGridXOffset, tileWidth);
                        final long tileY    = Math.floorDiv(y - tileGridYOffset, tileHeight);
                        final long tileXMin = Math.max(xmin,  tileX    * tileWidth  + tileGridXOffset);
                        final long tileYMin = Math.max(ymin,  tileY    * tileHeight + tileGridYOffset);
                        final long tileXMax = Math.min(xmax, (tileX+1) * tileWidth  + tileGridXOffset);
                        final long tileYMax = Math.min(ymax, (tileY+1) * tileHeight + tileGridYOffset);
                        while (indexOfXY < limitOfXY) {
                            x = nearestXY[indexOfXY++];
                            y = nearestXY[indexOfXY++];
                            if (x < tileXMin || x >= tileXMax || y < tileYMin || y >= tileYMax) {
                                indexOfXY -= BIDIMENSIONAL;  // Push back those invalid coordinates.
                                break;
                            }
                        }
                        final int i = tileCount * BIDIMENSIONAL;
                        tileIndices[i  ] = Math.toIntExact(tileX);
                        tileIndices[i+1] = Math.toIntExact(tileY);
                        continue nextTile;
                    }
                    /*
                     * Found a point outside the image. By setting `wasOutside`, we change the behavior
                     * of this loop for searching the end of the sequence of points that are outside
                     * (instead of the end of the sequence of points that are on the same tile).
                     */
                    if (parent.ifOutside != null) {
                        throw parent.ifOutside.apply(indexOfXY / BIDIMENSIONAL);
                    }
                    wasOutside = true;
                } while (indexOfXY < limitOfXY);
                tileIsAbsent.set(tileCount);
            }
            return new Image(image, nearestXY, limitOfXY, tileFirstCoords, tileCount, tileIndices, tileIsAbsent, parent.ifOutside);
        }

        /**
         * Creates a child which will iterate over the {@link #nearestXY} coordinates
         * from {@link #indexOfXY} inclusive to {@code stopAtXY} exclusive.
         */
        @Override
        final ValuesAtPointIterator createChild(int i, final int stopAtXY) {
            if (tileIsAbsent.get(i)) {
                return new Null(indexOfXY, stopAtXY);
            }
            i *= BIDIMENSIONAL;
            final int tileX = tileIndices[i];
            final int tileY = tileIndices[i+1];
            return new Tile(this, indexOfXY, stopAtXY, image.getTile(tileX, tileY));
        }

        /**
         * Creates a new iterator covering a prefix of the points.
         * This is invoked by {@link #trySplit()} implementation.
         */
        @Override
        final Group createPrefix(int stopAtXY) {
            return new Image(this, stopAtXY);
        }
    }




    /**
     * An iterator over the sample values of point contained in the same tile.
     * For performance reasons, this method recycles the same array in calls to
     * {@link Consumer#accept(Object)}.
     */
    private static final class Tile extends ValuesAtPointIterator {
        /**
         * The tile from which to get the sample values.
         */
        private final Raster tile;

        /**
         * The sample values.
         */
        private double[] samples;

        /**
         * Creates a new iterator for the specified tile.
         *
         * @param parent     the parent iterator from which to inherit the grid coordinates.
         * @param indexOfXY  index of the first coordinate of the next point to evaluate.
         * @param limitOfXY  index after the last coordinate of the last point to evaluate.
         * @param tile       the tile from which to get the sample values.
         */
        Tile(final ValuesAtPointIterator parent, final int indexOfXY, final int limitOfXY, final Raster tile) {
            super(parent.nearestXY, limitOfXY, null);
            this.indexOfXY = indexOfXY;
            this.tile = tile;
        }

        /**
         * Splits this iterator if there is enough points (an arbitrary threshold) to make it worth.
         * If successful, the returned iterator is the prefix of the sequence of points to evaluate,
         * and this iterator become the suffix.
         */
        @Override
        public Spliterator<double[]> trySplit() {
            final int start = indexOfXY;
            final int half = ((limitOfXY - start) / 2) & ~1;        // Must be even.
            if (half >= 10) {    // Arbitrary threshold.
                return new Tile(this, start, indexOfXY += half, tile);  // Prefix.
            }
            return null;
        }

        /**
         * Sends the sample values to the caller if the iteration is not finished.
         * For performance reasons, this method recycles the same array in calls to {@code accept}.
         */
        @Override
        public boolean tryAdvance(final Consumer<? super double[]> action) {
            if (indexOfXY < limitOfXY) {
                final int x = Math.toIntExact(nearestXY[indexOfXY++]);
                final int y = Math.toIntExact(nearestXY[indexOfXY++]);
                action.accept(samples = tile.getPixel(x, y, samples));
                return true;
            }
            return false;
        }

        /**
         * Sends remaining sample values to the caller until the end of the iteration.
         * For performance reasons, this method recycles the same array in calls to {@code accept}.
         */
        @Override
        public void forEachRemaining(final Consumer<? super double[]> action) {
            while (indexOfXY < limitOfXY) {
                final int x = Math.toIntExact(nearestXY[indexOfXY++]);
                final int y = Math.toIntExact(nearestXY[indexOfXY++]);
                action.accept(samples = tile.getPixel(x, y, samples));
            }
        }
    }




    /**
     * An iterator which returns only null values.
     * This is used for a sequence of points outside the image.
     */
    private static final class Null extends ValuesAtPointIterator {
        /**
         * Creates a new iterator of null elements.
         *
         * @param indexOfXY  index of the first coordinate of the next point to evaluate.
         * @param limitOfXY  index after the last coordinate of the last point to evaluate.
         */
        Null(final int indexOfXY, final int limitOfXY) {
            super(null, limitOfXY, null);
            this.indexOfXY = indexOfXY;
        }

        /**
         * Do not split this iterator as the caller would usually do nothing anyway.
         * It does not seem worth to let this class be parallelized.
         */
        @Override
        public Spliterator<double[]> trySplit() {
            return null;
        }

        /**
         * Sends a {@code null} array to the caller if the iteration is not finished.
         */
        @Override
        public boolean tryAdvance(final Consumer<? super double[]> action) {
            if (indexOfXY < limitOfXY) {
                indexOfXY += BIDIMENSIONAL;
                action.accept(null);
                return true;
            }
            return false;
        }

        /**
         * Sends {@code null} arrays to the caller until the end of the iteration.
         */
        @Override
        public void forEachRemaining(final Consumer<? super double[]> action) {
            while (indexOfXY < limitOfXY) {
                indexOfXY += BIDIMENSIONAL;
                action.accept(null);
            }
        }
    }
}
