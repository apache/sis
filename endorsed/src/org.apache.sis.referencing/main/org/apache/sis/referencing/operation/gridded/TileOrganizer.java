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
package org.apache.sis.referencing.operation.gridded;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.io.IOException;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.internal.ImmutableAffineTransform;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;


/**
 * Creates a collection of {@link Tile}s from their <i>grid to CRS</i> affine transforms.
 * When the {@link Rectangle} that describe the destination region is known for each tiles,
 * the {@link Tile#Tile(Rectangle, Dimension)} constructor should be invoked directly.
 * But in some cases the destination rectangle is not known directly. Instead we have a set of tiles,
 * all of them with an upper-left corner located at (0,0), but different <i>grid to CRS</i>
 * affine transforms read from <a href="https://en.wikipedia.org/wiki/World_file">World Files</a>.
 * This {@code TileOrganizer} class infers the destination regions automatically
 * from the set of affine transforms.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.storage.aggregate.CoverageAggregator
 */
public class TileOrganizer {
    /**
     * Small number for floating point comparisons.
     */
    private static final double EPS = 1E-10;

    /**
     * The location of the final bounding box (the one including every tiles).
     * Tiles will be translated as needed in order to fit this location. This
     * is usually zero, but not necessarily.
     */
    private final int xLocation, yLocation;

    /**
     * Tiles for which we should compute the bounding box after we have them all.
     * Their bounding box (region) will need to be adjusted for the affine transform.
     */
    private final Map<AffineTransform,Tile> tiles;

    /**
     * Creates an initially empty tile collection with the given location.
     *
     * @param  location  the location, or {@code null} for (0,0).
     */
    public TileOrganizer(final Point location) {
        if (location != null) {
            xLocation = location.x;
            yLocation = location.y;
        } else {
            xLocation = yLocation = 0;
        }
        /*
         * We really need an IdentityHashMap, not an ordinary HashMap, because we will
         * put many AffineTransforms that are equal in the sense of Object.equals, but
         * we still want to associate them to different Tile instances.
         */
        tiles = new IdentityHashMap<>();
    }

    /**
     * Returns the location of the tile collections to be created. The location is often
     * (0,0) as expected in {@link java.awt.image.BufferedImage}, but does not have to.
     *
     * @return origin of the tile collections to be created.
     */
    public Point getLocation() {
        return new Point(xLocation, yLocation);
    }

    /**
     * Adds a tile to the collection of tiles to process.
     * Each tile can be added only once.
     *
     * @param  tile  the tile to add.
     * @return {@code true} if the tile has been successfully added, or
     *         {@code false} if the tile does not need to be processed by this class.
     */
    public boolean add(final Tile tile) {
        final AffineTransform gridToCRS = tile.getPendingGridToCRS();
        if (gridToCRS == null) {
            return false;
        }
        if (tiles.putIfAbsent(gridToCRS, tile) != null) {
            throw new IllegalStateException();              // Tile already present.
        }
        return true;
    }

    /**
     * Returns the tiles. Keys are pyramid geometry (containing mosaic bounds and <i>grid to CRS</i>
     * transforms) and values are the tiles in that pyramid. This method usually returns a singleton map,
     * but more entries may be present if this method was not able to build a single pyramid using all
     * provided tiles.
     *
     * <p><strong>Invoking this method clear the collection</strong>. On return, this instance is empty.
     * This is because current implementation modify its workspace directly for efficiency.</p>
     *
     * @return all tiles added to this {@code TileOrganizer}, grouped by pyramids.
     * @throws IOException if a call to {@link Tile#getSize()} or {@link Tile#getRegion()} failed,
     *         and {@link #unavailableSize(Tile, IOException)} did not consumed the exception.
     */
    public Map<Tile, Tile[]> tiles() throws IOException {
        final var results = new HashMap<Tile, Tile[]>(4);
        for (final Map<AffineTransform,Dimension> tilesAT : computePyramidLevels(tiles.keySet())) {
            /*
             * Picks an affine transform to be used as the reference one. We need the finest one.
             * If more than one have the finest resolution, we pickup the one that will lead to a
             * (0,0) translation at the end of this method. This is because while the final result
             * is expected to have integer translation terms, the intermediates results before the
             * final translation may have fractional terms. Since those intermediate results are
             * stored as integers in Tile fields, it can leads to errors.
             */
            AffineTransform reference = null;
            double xMin  = Double.POSITIVE_INFINITY;
            double xLead = Double.POSITIVE_INFINITY;            // Minimum on the first row only.
            double yMin  = Double.POSITIVE_INFINITY;
            double scale = Double.POSITIVE_INFINITY;
            for (final AffineTransform tr : tilesAT.keySet()) {
                final double s = AffineTransforms2D.getScale(tr);
                double y = tr.getTranslateY(); if (tr.getScaleY() < 0 || tr.getShearY() < 0) y = -y;
                double x = tr.getTranslateX(); if (tr.getScaleX() < 0 || tr.getShearX() < 0) x = -x;
                if (!(Math.abs(s - scale) <= EPS)) {
                    if (!(s < scale)) continue;                 // '!' is for catching NaN.
                    scale = s;                                  // Found a smaller scale.
                    yMin = y;
                    xMin = x;
                } else {                                        // Found a transform with the same scale.
                    if (x < xMin) xMin = x;
                    if (!(Math.abs(y - yMin) <= EPS)) {
                        if (!(y < yMin)) continue;
                        yMin = y;                               // Found a smaller y.
                    } else if (!(x < xLead)) continue;
                }
                xLead = x;
                reference = tr;
            }
            /*
             * If there is missing tiles at the beginning of the first row, then the x location
             * of the first tile is greater than the "true" minimum. We will need to adjust.
             */
            if (reference == null) {
                continue;
            }
            xLead -= xMin;
            if (xLead > EPS) {
                final double[] matrix = new double[6];
                reference.getMatrix(matrix);
                matrix[4] = xMin;
                reference = new AffineTransform(matrix);
            } else {
                reference = new AffineTransform(reference);     // Protects from upcoming changes.
            }
            /*
             * Transform the image bounding box from its own space to the reference space.
             * If `computePyramidLevels(…)` did its job correctly, the transform should contain only a
             * scale and a translation - no shear (we do not put assertions because of rounding errors).
             * In such particular case, transforming a Rectangle2D is accurate. We round (we do not clip
             * as in the default Rectangle implementation) because we really expect integer results.
             */
            final AffineTransform toGrid;
            try {
                toGrid = reference.createInverse();
            } catch (NoninvertibleTransformException e) {
                throw new IllegalStateException(e);
            }
            int index = 0;
            Rectangle groupBounds = null;
            final Rectangle2D.Double envelope = new Rectangle2D.Double();
            final Tile[] tilesArray = new Tile[tilesAT.size()];
            for (final Map.Entry<AffineTransform,Dimension> entry : tilesAT.entrySet()) {
                final AffineTransform tr = entry.getKey();
                Tile tile = tiles.remove(tr);                   // Should never be null.
                tr.preConcatenate(toGrid);
                /*
                 * Compute the transformed bounds. If we fail to obtain it, there is probably something wrong
                 * with the tile but this is not fatal to this method. In such case we will transform only the
                 * location instead of the full box, which sometimes implies a lost of accuracy but not always.
                 */
                Rectangle bounds;
                synchronized (tile) {
                    tile.setSubsampling(entry.getValue());
                    try {
                        bounds = tile.getRegion();
                    } catch (IOException exception) {
                        if (!unavailableSize(tile, exception)) {
                            throw exception;
                        }
                        bounds = null;
                    }
                    if (bounds != null) {
                        AffineTransforms2D.transform(tr, bounds, envelope);
                        bounds.x      = Math.toIntExact(Math.round(envelope.x));
                        bounds.y      = Math.toIntExact(Math.round(envelope.y));
                        bounds.width  = Math.toIntExact(Math.round(envelope.width));
                        bounds.height = Math.toIntExact(Math.round(envelope.height));
                    } else {
                        final Point location = tile.getLocation();
                        tr.transform(location, location);
                        bounds = new Rectangle(location.x, location.y, 0, 0);
                    }
                    tile.setRegionOnFinestLevel(bounds);
                }
                if (groupBounds == null) {
                    groupBounds = bounds;
                } else {
                    groupBounds.add(bounds);
                }
                tilesArray[index++] = tile;
            }
            tilesAT.clear();                                            // Lets GC do its work.
            /*
             * Translate the tiles in such a way that the upper-left corner has the coordinates
             * specified by (xLocation, yLocation). Adjust the tile affine transform consequently.
             * After this block, tiles having the same subsampling will share the same immutable
             * affine transform instance.
             */
            if (groupBounds != null) {
                final int dx = xLocation - groupBounds.x;
                final int dy = yLocation - groupBounds.y;
                if ((dx | dy) != 0) {
                    reference.translate(-dx, -dy);
                    groupBounds.translate(dx, dy);
                }
                reference = new AffineTransform2D(reference);               // Make immutable.
                final Map<Dimension,Translation> pool = new HashMap<>();
                for (final Tile tile : tilesArray) {
                    final Dimension subsampling = tile.getSubsampling();
                    Translation translated = pool.get(subsampling);
                    if (translated == null) {
                        translated = new Translation(subsampling, reference, dx, dy);
                        pool.put(subsampling, translated);
                    }
                    translated.applyTo(tile);
                }
                results.put(new Tile(reference, groupBounds), tilesArray);
            }
        }
        return results;
    }


    /**
     * An affine transform which is translated relative to an original transform.
     * The translation terms are stored separately without modifying the transform.
     * This class if for internal use by {@link TileOrganizer} only.
     */
    private static final class Translation {
        /**
         * The translated "grid to real world" transform, as an immutable instance.
         */
        private final AffineTransform gridToCRS;

        /**
         * The translation in units of the level having finest resolution.
         * This is the same units as for tiles at subsampling (1,1).
         */
        private final int dx, dy;

        /**
         * Creates a new translated transform. The translation is specified in unit of the level
         * having finest resolution, i.e. in the same units as for tiles at subsampling (1,1).
         *
         * @param  subsampling  the {@linkplain Tile#getSubsampling() tile subsampling}.
         * @param  reference    the "grid to real world" transform at subsampling (1,1).
         * @param  dx           the translation along <var>x</var> axis in "finest units".
         * @param  dy           the translation along <var>y</var> axis in "finest units".
         */
        Translation(final Dimension subsampling, AffineTransform reference, int dx, int dy) {
            this.dx = dx / subsampling.width;                           // It is okay to round toward zero.
            this.dy = dy / subsampling.height;
            dx %= subsampling.width;
            dy %= subsampling.height;
            reference = new AffineTransform(reference);
            reference.scale(subsampling.width, subsampling.height);
            reference.translate(dx, dy);                                // Correction for non-integer division of (dx,dy).
            gridToCRS = new ImmutableAffineTransform(reference);
        }

        /**
         * Applies the translation and the new "grid to CRS" transform on the given tile.
         *
         * @param  tile  the tile on which to apply the translation.
         */
        final void applyTo(final Tile tile) {
            synchronized (tile) {
                tile.translate(dx, dy);
                tile.setGridToCRS(gridToCRS);
            }
        }
    }

    /**
     * Sorts affine transform by increasing X scales in absolute value.
     * For {@link #computePyramidLevels(Collection)} internal working only.
     */
    private static final Comparator<AffineTransform> X_COMPARATOR = new Comparator<AffineTransform>() {
        @Override public int compare(final AffineTransform tr1, final AffineTransform tr2) {
            return Double.compare(AffineTransforms2D.getScaleX0(tr1), AffineTransforms2D.getScaleX0(tr2));
        }
    };

    /**
     * Sorts affine transform by increasing Y scales in absolute value.
     * For {@link #computePyramidLevels(Collection)} internal working only.
     */
    private static final Comparator<AffineTransform> Y_COMPARATOR = new Comparator<AffineTransform>() {
        @Override public int compare(final AffineTransform tr1, final AffineTransform tr2) {
            return Double.compare(AffineTransforms2D.getScaleY0(tr1), AffineTransforms2D.getScaleY0(tr2));
        }
    };

    /**
     * From a set of arbitrary affine transforms, computes pyramid levels that can be given to
     * {@link Tile} constructors. This method tries to locate the affine transform with finest resolution.
     * This is typically (but not always, depending on rotation or axis flip) the transform with smallest
     * {@linkplain AffineTransform#getScaleX scale X} and {@linkplain AffineTransform#getScaleY scale Y}
     * coefficients in absolute value. That transform is given a "resolution" of (1,1) and stored in an
     * {@link IdentityHashMap}. Other transforms are stored in the same map with their resolution relative
     * to the first one, or discarded if the relative resolution is not an integer. In the latter case, the
     * transforms that were discarded from the first pass will be put in a new map to be added as the second
     * element in the returned list. A new pass is run, discarded transforms from the second pass are put in
     * the third element of the list, <i>etc</i>.
     *
     * @param  gridToCRS  the <i>grid to CRS</i> affine transforms computed from the image to use in a pyramid.
     *         The collection and the transform elements are not modified by this method (they may be modified by the
     *         caller however).
     * @return a subset of the given transforms with their relative resolution. This method typically returns one map,
     *         but more could be returned if the scale ratio is not an integer for every transforms.
     */
    private static List<Map<AffineTransform,Dimension>> computePyramidLevels(final Collection<AffineTransform> gridToCRS) {
        final List<Map<AffineTransform,Dimension>> results = new ArrayList<>(2);
        /*
         * First, compute the pyramid levels along the X axis. Transforms that we were unable
         * to classify will be discarded from the first run and put in a subsequent run.
         */
        AffineTransform[] transforms = gridToCRS.toArray(AffineTransform[]::new);
        Arrays.sort(transforms, X_COMPARATOR);
        int length = transforms.length;
        while (length != 0) {
            final Map<AffineTransform,Dimension> result = new IdentityHashMap<>();
            if (length <= (length = computePyramidLevels(transforms, length, result, false))) {
                throw new AssertionError(length);               // Should always be decreasing.
            }
            results.add(result);
        }
        /*
         * Next, compute the pyramid levels along the Y axis. If we fail to compute the
         * pyramid level for some AffineTransform, they will be removed from the map.
         * If a map became empty because of that, the whole map will be removed.
         */
        final Iterator<Map<AffineTransform,Dimension>> iterator = results.iterator();
        while (iterator.hasNext()) {
            final Map<AffineTransform,Dimension> result = iterator.next();
            length = result.size();
            transforms = result.keySet().toArray(transforms);
            Arrays.sort(transforms, 0, length, Y_COMPARATOR);
            length = computePyramidLevels(transforms, length, result, true);
            while (--length >= 0) {
                if (result.remove(transforms[length]) == null) {
                    throw new AssertionError(length);
                }
            }
            if (result.isEmpty()) {
                iterator.remove();
            }
        }
        return results;
    }

    /**
     * Computes the pyramid level for the given affine transforms along the X or Y axis,
     * and stores the result in the given map.
     *
     * @param  gridToCRS  the AffineTransform to analyze. This array <strong>must</strong>
     *                    be sorted along the dimension specified by {@code isY}.
     * @param  length     the number of valid entries in the {@code gridToCRS} array.
     * @param  result     an initially empty map in which to store the results.
     * @param  isY        {@code false} for analyzing the X axis, or {@code true} for the Y axis.
     * @return the number of entries remaining in {@code gridToCRS}.
     */
    private static int computePyramidLevels(final AffineTransform[] gridToCRS, final int length,
            final Map<AffineTransform,Dimension> result, final boolean isY)
    {
        int processing = 0;             // Index of the AffineTransform under process.
        int remaining  = 0;             // Count of AffineTransforms that this method did not processed.
        AffineTransform base;
        double scale, shear;
        boolean scaleIsNull, shearIsNull;
        for (;;) {
            if (processing >= length) {
                return remaining;
            }
            base = gridToCRS[processing++];
            if (isY) {
                scale = base.getScaleY();
                shear = base.getShearY();
            } else {
                scale = base.getScaleX();
                shear = base.getShearX();
            }
            scaleIsNull = Math.abs(scale) < EPS;
            shearIsNull = Math.abs(shear) < EPS;
            if (!(scaleIsNull & shearIsNull)) break;
            result.remove(base);
        }
        if (isY) {
            // If we get a NullPointerException here, it would be a bug in the algorithm.
            result.get(base).height = 1;
        } else {
            assert result.isEmpty() : result;
            result.put(base, new Dimension(1,0));
        }
        /*
         * From this point, consider 'base', 'scale', 'shear', 'scaleIsNull', 'shearIsNull' as final.
         * They describe the AffineTransform with finest resolution along one axis (X or Y).
         */
        while (processing < length) {
            final AffineTransform candidate = gridToCRS[processing++];
            final double scale2, shear2;
            if (isY) {
                scale2 = candidate.getScaleY();
                shear2 = candidate.getShearY();
            } else {
                scale2 = candidate.getScaleX();
                shear2 = candidate.getShearX();
            }
            final int level;
            if (scaleIsNull) {
                if (!(Math.abs(scale2) < EPS)) {
                    // Expected a null scale but was not.
                    gridToCRS[remaining++] = candidate;
                    continue;
                }
                level = level(shear2 / shear);
            } else {
                level = level(scale2 / scale);
                if (shearIsNull ? !(Math.abs(shear2) < EPS) : (level(shear2 / shear) != level)) {
                    // Expected (a null shear) : (the same pyramid level), but was not.
                    gridToCRS[remaining++] = candidate;
                    continue;
                }
            }
            if (level == 0) {
                // Not a pyramid level (the ratio is not an integer).
                gridToCRS[remaining++] = candidate;
                continue;
            }
            /*
             * Stores the pyramid level either as the width or as the height, depending on the `isY` value.
             * The map is assumed initially empty for the X values, and is assumed containing every required
             * entries for the Y values.
             */
            if (isY) {
                // If we get a NullPointerException here, it would be a bug in the algorithm.
                result.get(candidate).height = level;
            } else {
                if (result.put(candidate, new Dimension(level,0)) != null) {
                    throw new AssertionError(candidate);                                // Should never happen.
                }
            }
        }
        Arrays.fill(gridToCRS, remaining, length, null);
        return remaining;
    }

    /**
     * Computes the pyramid level from the ratio between two affine transform coefficients.
     * If the ratio has been computed from {@code entry2.scaleX / entry1.scaleX}, then a return value of:
     *
     * <ul>
     *   <li>1 means that both entries are at the same level.</li>
     *   <li>2 means that the second entry has pixels twice as large as first entry.</li>
     *   <li>3 means that the second entry has pixels three time larger than first entry.</li>
     *   <li><i>etc...</i></li>
     *   <li>A negative number means that the second entry has pixels smaller than first entry.</li>
     *   <li>0 means that the ratio between entries is not an integer number.</li>
     * </ul>
     *
     * @param  ratio  the ratio between affine transform coefficients.
     * @return the pixel size (actually subsampling) relative to the smallest pixel, or 0 if it cannot be computed.
     *         If the ratio is between 0 and 1, then this method returns a negative number.
     */
    private static int level(double ratio) {
        if (ratio > 0 && ratio < Double.POSITIVE_INFINITY) {
            /*
             * The 0.75 threshold could be anything between 0.5 and 1.
             * We take a middle value for being safe regarding rounding errors.
             */
            final boolean inverse = (ratio < 0.75);
            if (inverse) {
                ratio = 1 / ratio;
            }
            final double integer = Math.rint(ratio);
            if (integer < Integer.MAX_VALUE && Math.abs(ratio - integer) < EPS) {
                /*
                 * Found an integer ratio.
                 * Inverse the sign (just as a matter of convention) if smaller than 1.
                 */
                int level = (int) integer;
                if (inverse) {
                    level = -level;
                }
                return level;
            }
        }
        return 0;
    }

    /**
     * Invoked when an I/O error occurred in {@link Tile#getSize()} or {@link Tile#getRegion()}.
     * This error is non-fatal since {@code TileOrganizer} can fallback on calculation based
     * on tile location only (without size).
     *
     * <p>The default implementation returns {@code false}, which instructs the caller to let
     * the exception propagate.</p>
     *
     * @param  tile       the tile on which an error occurred.
     * @param  exception  the error that occurred.
     * @return {@code true} if the exception has been consumed, or {@code false} for re-throwing it.
     */
    protected boolean unavailableSize(final Tile tile, final IOException exception) {
        return false;
    }

    /**
     * Returns a string representation of the tiles contained in this object. Since this method is
     * for debugging purpose, only the first tiles may be formatted in order to avoid consuming to
     * much space in the debugger.
     */
    @Override
    public String toString() {
        return Tile.toString(tiles.values(), 400);
    }
}
