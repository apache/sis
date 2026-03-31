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
package org.apache.sis.storage.tiling;

import java.io.Serializable;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.Shapes2D;
import org.apache.sis.util.internal.shared.Strings;


/**
 * Notifies listeners that the process of reading a tile has started.
 * This event contains the bounding box of the tile in real world coordinates.
 * Because this event may be sent early in the reading process, the associated
 * {@link Tile} is generally not known yet.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   1.7
 * @version 1.7
 */
public class TileReadEvent extends StoreEvent {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4912145530425375808L;

    /**
     * Contextual information shared by all events emitted by the same tile iterator.
     * Contains the conversion from pixel coordinates to real world coordinates.
     */
    static final class Context implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 6873392524154427892L;

        /**
         * Zero-based index of the pyramid level of the tile which is read.
         * The level with coarsest resolution (the overview) is the level 0.
         *
         * @see #getPyramidLevel()
         * @see #getResolution()
         */
        final int pyramidLevel;

        /**
         * The two-dimensional grid geometry of the slice of the resource which is represented as an image.
         */
        final GridGeometry sliceGeometry;

        /**
         * Lowest coordinates of the region which has been requested by the user for producing an image.
         * The pixel coordinates (0,0) correspond to the lowest coordinates of the requested extent.
         */
        private final long offsetX, offsetY;

        /**
         * Coordinate operation from the <abbr>CRS</abbr> of the coverage to the <abbr>CRS</abbr>
         * given in the last call to the {@code imageToObjective(…)} method.
         *
         * @see #imageToObjective(CoordinateReferenceSystem)
         */
        private transient CoordinateOperation crsToObjective;

        /**
         * Conversion from pixel coordinates to "real world" coordinates in a user-specified <abbr>CRS</abbr>.
         * That user-specified <abbr>CRS</abbr> is called "objective <abbr>CRS</abbr>" because it is often the
         * <abbr>CRS</abbr> using for rendering purposes.
         */
        @SuppressWarnings("serial")     // Most SIS implementations are serializable.
        private transient MathTransform2D imageToObjective;

        /**
         * Creates a new context.
         *
         * @param  pyramidLevel  index of the pyramid level of the tile which is read, where 0 is the level with coarsest resolution.
         * @param  domain        the grid geometry of the coverage of which a slice is rendered as an image.
         * @param  aoi           the coordinates requested by the user.
         * @param  xDimension    dimension of the grid which is mapped to the <var>x</var> axis in rendered images.
         * @param  yDimension    dimension of the grid which is mapped to the <var>y</var> axis in rendered images.
         * @throws RuntimeException if a slice cannot be created in the given dimensions.
         */
        Context(final int pyramidLevel, final GridGeometry domain, final GridExtent aoi, final int xDimension, final int yDimension) {
            this.pyramidLevel = pyramidLevel;
            sliceGeometry = domain.selectDimensions(xDimension, yDimension);
            offsetX = aoi.getLow(xDimension);
            offsetY = aoi.getLow(yDimension);
        }

        /**
         * Returns the transform from pixel coordinates to real world coordinates in the given <abbr>CRS</abbr>.
         *
         * @param  crs  the two-dimensional <abbr>CRS</abbr> of the desired bounding box.
         * @return transform from pixel coordinates to real world coordinates in the given <abbr>CRS</abbr>.
         * @throws TransformException if the transform cannot be computed.
         */
        final synchronized MathTransform2D imageToObjective(final CoordinateReferenceSystem crs) throws TransformException {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            CoordinateOperation crsToObjective = this.crsToObjective;
            if (crsToObjective == null || !CRS.equivalent(crsToObjective.getTargetCRS(), crs)) {
                crsToObjective = sliceGeometry.createChangeOfCRS(crs);
                MathTransform tr = MathTransforms.translation(offsetX, offsetY);
                tr = MathTransforms.concatenate(tr, sliceGeometry.getGridToCRS(PixelInCell.CELL_CORNER));
                tr = MathTransforms.concatenate(tr, crsToObjective.getMathTransform());
                imageToObjective = MathTransforms.bidimensional(tr);
                this.crsToObjective = crsToObjective;   // Store only after the rest was successful.
            }
            return imageToObjective;
        }
    }

    /**
     * Contextual information shared by all events emitted by the same tile iterator.
     * Contains the conversion from pixel coordinates to real world coordinates.
     * Using a shared instance allow to reuse the cached coordinate operation.
     */
    private final Context context;

    /**
     * Bounds of the tile in pixel coordinates.
     *
     * Note: there is no public <abbr>API</abbr> yet for fetching this value
     * because the pixel coordinates are not necessarily the same as the grid
     * coordinates of the resource, which may confuse users.
     */
    private final Rectangle rasterBounds;

    /**
     * Creates a new event about a tile which will be read or has been read.
     *
     * @param  source        the resource where the event occurred.
     * @param  context       contextual information shared by all events emitted by the same tile iterator.
     * @param  rasterBounds  bounds of the tile in pixel coordinates.
     */
    TileReadEvent(final Resource source, final Context context, final Rectangle rasterBounds) {
        super(source);
        this.context = context;
        this.rasterBounds = rasterBounds;
    }

    /**
     * Returns the zero-based index of the pyramid level of the tile which is read.
     * This is typically the index in the {@linkplain TiledGridCoverageResource#getAvailableResolutions()
     * list of resource's resolution} where the values returned by {@link #getResolution()} can be found.
     * The level with coarsest resolution (the overview) is the level 0.
     *
     * @return zero-based index of the pyramid level of the tile which is read.
     *
     * @see TiledGridCoverageResource.Pyramid#forPyramidLevel(int)
     */
    public int getPyramidLevel() {
        return context.pyramidLevel;
    }

    /**
     * Returns the resolution in units of the coverage <abbr>CRS</abbr>.
     * The length of the returned array should be 2.
     *
     * @return the resolution in units of the coverage <abbr>CRS</abbr>.
     */
    public double[] getResolution() {
        return context.sliceGeometry.getResolution(true);
    }

    /**
     * Computes the bounds of the tile in the given two-dimensional <abbr>CRS</abbr>.
     * If the use of the given <abbr>CRS</abbr> may change straight lines into curves
     * (as with some map projections), the returned bounding box contains fully (on a
     * best-effort basis) the tile.
     *
     * @param  crs  the two-dimensional <abbr>CRS</abbr> of the desired bounding box.
     * @return real world coordinates of the tile expressed in the given <abbr>CRS</abbr>.
     * @throws TransformException if the tile bounds cannot be transformed to the given <abbr>CRS</abbr>.
     */
    public Rectangle2D bounds(final CoordinateReferenceSystem crs) throws TransformException {
        return Shapes2D.transform(context.imageToObjective(crs), rasterBounds, null);
    }

    /**
     * Computes the outline of the tile in the given two-dimensional <abbr>CRS</abbr>.
     * The returned shape may have curved lines if the use of the given <abbr>CRS</abbr>
     * implies a map projection.
     *
     * @param  crs  the two-dimensional <abbr>CRS</abbr> of the desired outline.
     * @return real world coordinates of the tile expressed in the given <abbr>CRS</abbr>.
     * @throws TransformException if the tile bounds cannot be transformed to the given <abbr>CRS</abbr>.
     */
    public Shape outline(final CoordinateReferenceSystem crs) throws TransformException {
        return context.imageToObjective(crs).createTransformedShape(rasterBounds);
    }

    /**
     * Returns a string representation of this event for debugging purposes.
     *
     * @return a string representation of this event.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(),
                "x",          rasterBounds.x,
                "y",          rasterBounds.y,
                "width",      rasterBounds.width,
                "height",     rasterBounds.height,
                "resolution", getResolution());
    }
}
