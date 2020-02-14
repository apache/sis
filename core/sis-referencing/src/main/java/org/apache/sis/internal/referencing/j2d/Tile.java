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
package org.apache.sis.internal.referencing.j2d;

import java.util.Locale;
import java.util.Collection;
import java.util.Optional;
import java.io.Writer;
import java.io.StringWriter;
import java.io.Serializable;
import java.io.IOException;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import javax.imageio.ImageReader;                           // For javadoc
import org.opengis.metadata.spatial.PixelOrientation;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.io.TableAppender;


/**
 * A tile identified by a location, a dimension and a subsampling.
 * This class can be used for constructing a mosaic or a pyramid of images.
 * While the Javadoc discusses image I/O operations, this {@code Tile} class is not restricted to imagery.
 * This class is also used for managing tiles in a datum shift file encoded in NTv2 format.
 *
 * <p>Each tile contains the following:</p>
 * <ul class="verbose">
 *   <li><b>A format name or a provider of {@link ImageReader}</b> (optional).
 *   The same format is typically used for every tiles, but this is not mandatory.
 *   An {@linkplain ImageReader image reader} can be instantiated before a tile is read.</li>
 *
 *   <li><b>An image input</b> (optional), typically a {@link java.nio.file.Path} or {@link java.net.URL}.
 *   The input is often different for every tile to be read, but this is not mandatory. For example tiles
 *   could be stored at different {@linkplain #getImageIndex() image index} in the same file.</li>
 *
 *   <li><b>An image index</b> to be given to {@link ImageReader#read(int)} for reading the tile.
 *   This index is often 0.</li>
 *
 *   <li><b>The upper-left corner</b> in the destination image as a {@link Point},
 *   or the upper-left corner together with the image size as a {@link Rectangle}.
 *   If the upper-left corner has been given as a point, then the
 *   {@linkplain ImageReader#getWidth(int) width} and {@linkplain ImageReader#getHeight(int) height}
 *   may be obtained from the image reader when first needed, which may have a slight performance cost.
 *   If the upper-left corner has been given as a rectangle instead, then this performance cost is avoided
 *   but the user is responsible for the accuracy of the information provided.
 *
 *     <div class="note"><b>NOTE:</b>
 *     the upper-left corner is the {@linkplain #getLocation() location} of this tile in the
 *     {@linkplain javax.imageio.ImageReadParam#setDestination destination image} when no
 *     {@linkplain javax.imageio.ImageReadParam#setDestinationOffset destination offset} are specified.
 *     If the user specified a destination offset, then the tile location will be translated accordingly
 *     for the image being read.
 *     </div></li>
 *
 *   <li><b>The subsampling relative to the tile having the best resolution.</b>
 *   This is not the subsampling to apply when reading this tile, but rather the subsampling that we would
 *   need to apply on the tile having the finest resolution in order to produce an image equivalent to this tile.
 *   The subsampling is (1,1) for the tile having the finest resolution, (2,3) for an overview having
 *   half the width and third of the height for the same geographic extent, <i>etc.</i>
 *   (note that overviews are not required to have the same geographic extent - the above is just an example).
 *
 *     <div class="note"><b>NOTE 1:</b>
 *     the semantic assumes that overviews are produced by subsampling, not by interpolation or pixel averaging.
 *     The later are not prohibited, but doing so introduce some subsampling-dependent variations in images read,
 *     which would not be what we would expect from a strictly compliant {@link ImageReader}.</div>
 *
 *     <div class="note"><b>NOTE 2:</b>
 *     tile {@linkplain #getLocation() location} and {@linkplain #getRegion() region} coordinates should be
 *     specified in the overview pixel units - they should <em>not</em> be pre-multiplied by subsampling.
 *     This multiplication should be performed automatically by a {@code TileManager} when comparing regions
 *     from tiles at different subsampling levels.
 *     </div></li>
 * </ul>
 *
 * The tiles are not required to be arranged on a regular grid, but performances may be better if they are.
 * {@link MosaicCalculator} is responsible for analyzing the layout of a collection of tiles.
 *
 * <h2>Multi-threading</h2>
 * This class is thread-safe. In addition {@code Tile} instances can be considered as immutable after construction.
 * However some properties may be available only after the tiles have been processed by a {@link MosaicCalculator},
 * or only after {@link #fetchSize()} has been invoked.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class Tile implements Comparable<Tile>, Serializable {
    /**
     * For cross-version compatibility during serialization.
     */
    private static final long serialVersionUID = 1638238437701248681L;

    /**
     * The upper-left corner in the mosaic (destination image). Should be considered as final,
     * since this class is supposed to be mostly immutable. However the value can be changed
     * by {@link #translate(int, int)} before the {@code Tile} instance is made public.
     *
     * @see #getLocation()
     * @see #getRegion()
     */
    private int x, y;

    /**
     * The size of the tile, or 0 if not yet computed.
     *
     * @see #getSize()
     * @see #getRegion()
     */
    private int width, height;

    /**
     * The subsampling relative to the tile having the finest resolution. If this tile is the one with
     * finest resolution, then the value shall be 1. Should never be 0 or negative, except if the value
     * has not yet been computed.
     *
     * <p>This field should be considered as final. It is not final only because
     * {@link MosaicCalculator} may compute this value automatically.</p>
     *
     * @see #getSubsampling()
     */
    private int xSubsampling, ySubsampling;

    /**
     * The "grid to real world" transform, used by {@link MosaicCalculator} in order to compute
     * the {@linkplain #getRegion() region} for this tile. This field is set to {@code null} when
     * {@link MosaicCalculator}'s work is in progress, and set to a new value on completion.
     *
     * <p><b>Note:</b> {@link MosaicCalculator} really needs a new instance for each tile.
     * No caching allowed before {@link MosaicCalculator} processing.
     * Caching is allowed <em>after</em> {@link MosaicCalculator} processing is completed.</p>
     */
    private AffineTransform gridToCRS;

    /**
     * Creates a tile for the given tile location. This constructor can be used when the size of
     * the tile is unknown. This tile size will be fetched automatically by {@link #fetchSize()}
     * when {@link #getSize()} or {@link #getRegion()} is invoked for the first time.
     *
     * @param location     the upper-left corner in the mosaic (destination image).
     * @param subsampling  the subsampling relative to the tile having the finest resolution,
     *                     or {@code null} if none. If non-null, width and height shall be strictly positive.
     *                     This argument can be understood as pixel size relative to finest resolution.
     */
    public Tile(final Point location, final Dimension subsampling) {
        ArgumentChecks.ensureNonNull("location", location);
        x = location.x;
        y = location.y;
        setSubsampling(subsampling);
    }

    /**
     * Creates a tile for the given region. This constructor should be used when the size of the tile is known.
     * This information avoid the cost of fetching the size when {@link #getSize()} or {@link #getRegion()} is
     * first invoked.
     *
     * @param region       the region (location and size) in the mosaic (destination image).
     * @param subsampling  the subsampling relative to the tile having the finest resolution,
     *                     or {@code null} if none. If non-null, width and height shall be strictly positive.
     *                     This argument can be understood as pixel size relative to finest resolution.
     * @throws IllegalArgumentException if the given region {@linkplain Rectangle#isEmpty() is empty}.
     */
    public Tile(final Rectangle region, final Dimension subsampling) {
        ArgumentChecks.ensureNonNull("location", region);
        x      = region.x;
        y      = region.y;
        width  = region.width;
        height = region.height;
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "region"));
        }
        setSubsampling(subsampling);
    }

    /**
     * Creates a tile for the given region and <cite>"grid to real world"</cite> transform.
     * This constructor can be used when the {@linkplain #getLocation() location} of the tile is unknown.
     * The location and subsampling will be computed automatically when this tile will be processed by a
     * {@link MosaicCalculator}.
     *
     * <p>When using this constructor, the {@link #getLocation()}, {@link #getRegion()} and
     * {@link #getSubsampling()} methods will throw an {@link IllegalStateException} until
     * this tile has been processed by a {@link MosaicCalculator}, which will compute those
     * values automatically.</p>
     *
     * @param region     the tile region, or {@code null} if unknown.
     *                   The (<var>x</var>,<var>y</var> location of this region is typically (0,0).
     *                   The final location will be computed when this tile will be given to a {@link MosaicCalculator}.
     * @param gridToCRS  the <cite>"grid to real world"</cite> transform mapping pixel
     *                   {@linkplain PixelOrientation#UPPER_LEFT upper left} corner.
     */
    public Tile(final Rectangle region, final AffineTransform gridToCRS) {
        ArgumentChecks.ensureNonNull("gridToCRS", gridToCRS);
        if (region != null) {
            x      = region.x;
            y      = region.y;
            width  = Math.max(region.width,  0);                        // Empty region authorized.
            height = Math.max(region.height, 0);
        }
        this.gridToCRS = new AffineTransform(gridToCRS);                // Really need a new instance - no cache
    }

    /**
     * Creates a new tile for the given final transform.
     * This is used for storing {@link MosaicCalculator} results.
     */
    Tile(final AffineTransform gridToCRS, final Rectangle region) {
        this.x         = region.x;
        this.y         = region.y;
        this.width     = region.width;
        this.height    = region.height;
        this.gridToCRS = gridToCRS;                                     // Should be an AffineTransform2D instance.
        setSubsampling(null);
    }

    /**
     * Checks if the location, region, and subsampling can be returned. Throws an exception if this
     * tile has been created without location and not yet processed by {@link MosaicCalculator}.
     */
    private void ensureDefined() throws IllegalStateException {
        if (xSubsampling == 0 || ySubsampling == 0) {
            throw new IllegalStateException();
        }
    }

    /**
     * Returns the tile upper-left corner coordinates in the mosaic.
     *
     * @return the tile upper-left corner.
     * @throws IllegalStateException if this tile has been {@linkplain #Tile(Rectangle, AffineTransform)
     *         created without location} and has not yet been processed by {@link MosaicCalculator}.
     *
     * @see javax.imageio.ImageReadParam#setDestinationOffset(Point)
     */
    public synchronized Point getLocation() throws IllegalStateException {
        ensureDefined();
        return new Point(x, y);
    }

    /**
     * Returns the image size. If this tile has been created with the {@linkplain #Tile(Rectangle, Dimension)
     * constructor expecting a rectangle}, then the dimension of that rectangle is returned.
     * Otherwise {@link #fetchSize()} is invoked and the result is cached for future usage.
     *
     * <p>At the difference of {@link #getLocation()} and {@link #getRegion()}, this method never
     * throw {@link IllegalStateException} because the tile size does not depend on the processing
     * performed by {@link MosaicCalculator}.</p>
     *
     * @return the tile size.
     * @throws IOException if an I/O operation was required for fetching the tile size and that operation failed.
     * @throws IllegalStateException if this class does not have sufficient information for providing a tile size.
     */
    public synchronized Dimension getSize() throws IOException {
        // No call to ensureDefined().
        if ((width | height) == 0) {
            final Dimension size = fetchSize();
            width  = size.width;
            height = size.height;
        }
        return new Dimension(width, height);
    }

    /**
     * Invoked when the tile size need to be read or computed. The default implementation throws
     * {@link IllegalStateException} since this base class has no information for computing a tile size.
     * Subclasses can override and, for example, get the size with {@link ImageReader#getWidth(int)} and
     * {@link ImageReader#getHeight(int)}.
     *
     * @return the tile size.
     * @throws IOException if an I/O operation was required for fetching the tile size and that operation failed.
     * @throws IllegalStateException if this class does not have sufficient information for providing a tile size.
     */
    protected Dimension fetchSize() throws IOException {
        throw new IllegalStateException();
    }

    /**
     * Returns the upper-left corner location in the mosaic together with the tile size.
     * If this tile has been created with the {@linkplain #Tile(Rectangle, Dimension)
     * constructor expecting a rectangle}, a copy of the specified rectangle is returned.
     * Otherwise {@link #fetchSize()} is invoked and the result is cached for future usage.
     *
     * @return the region in the mosaic (destination image).
     * @throws IOException if an I/O operation was required for fetching the tile size and that operation failed.
     * @throws IllegalStateException if this tile has been {@linkplain #Tile(Rectangle, AffineTransform) created
     *         without location} and has not yet been processed by {@link MosaicCalculator}, of if this tile does
     *         not have enough information for providing a tile size.
     *
     * @see javax.imageio.ImageReadParam#setSourceRegion(Rectangle)
     */
    public synchronized Rectangle getRegion() throws IllegalStateException, IOException {
        ensureDefined();
        if ((width | height) == 0) {
            final Dimension size = fetchSize();
            width  = size.width;
            height = size.height;
        }
        return new Rectangle(x, y, width, height);
    }

    /**
     * Returns the {@linkplain #getRegion() region} multiplied by the subsampling.
     * This is this tile coordinates in the units of the tile having the finest resolution,
     * as opposed to other methods which are always in units relative to this tile.
     *
     * @return the region in units relative to the tile having the finest resolution.
     * @throws IOException if an I/O operation was required for fetching the tile size and that operation failed.
     * @throws IllegalStateException if this tile has been {@linkplain #Tile(Rectangle, AffineTransform) created
     *         without location} and has not yet been processed by {@link MosaicCalculator}, of if this tile does
     *         not have enough information for providing a tile size.
     */
    public synchronized Rectangle getAbsoluteRegion() throws IOException {
        final Rectangle region = getRegion();
        final int sx = xSubsampling;
        final int sy = ySubsampling;
        region.x      *= sx;
        region.y      *= sy;
        region.width  *= sx;
        region.height *= sy;
        return region;
    }

    /**
     * Invoked by {@link MosaicCalculator} only. No other caller allowed.
     * {@link #setSubsampling(Dimension)} must be invoked prior this method.
     *
     * <p>Note that invoking this method usually invalidate {@link #gridToCRS}.
     * Calls to this method should be followed by {@link #translate(int, int)}
     * for fixing the "gridToCRS" value.</p>
     *
     * @param  region  the region to assign to this tile in units of tile having finest resolution.
     * @throws ArithmeticException if {@link #setSubsampling(Dimension)} method has not be invoked.
     */
    final void setAbsoluteRegion(final Rectangle region) throws ArithmeticException {
        assert Thread.holdsLock(this);
        final int sx = xSubsampling;
        final int sy = ySubsampling;
        assert (region.width % sx) == 0 && (region.height % sy) == 0 : region;
        x      = region.x      / sx;
        y      = region.y      / sy;
        width  = region.width  / sx;
        height = region.height / sy;
    }

    /**
     * Returns the subsampling relative to the tile having the finest resolution.
     * The return value can be interpreted as "pixel size" relative to tiles having the finest resolution.
     * This method never return {@code null}, and the width and height shall never be smaller than 1.
     *
     * @return the subsampling along <var>x</var> and <var>y</var> axes.
     * @throws IllegalStateException if this tile has been {@linkplain #Tile(Rectangle, AffineTransform)
     *         created without location} and has not yet been processed by {@link MosaicCalculator}.
     *
     * @see javax.imageio.ImageReadParam#setSourceSubsampling(int, int, int, int)
     */
    public synchronized Dimension getSubsampling() throws IllegalStateException {
        ensureDefined();
        return new Dimension(xSubsampling, ySubsampling);
    }

    /**
     * Sets the subsampling to the given dimension.
     * Invoked by constructors and {@link MosaicCalculator} only.
     */
    final void setSubsampling(final Dimension subsampling) throws IllegalStateException {
        // No assert Thread.holdsLock(this) because invoked from constructors.
        if ((xSubsampling | ySubsampling) != 0) {
            throw new IllegalStateException();                          // Should never happen.
        }
        if (subsampling != null) {
            ArgumentChecks.ensureBetween("width",  0, 0xFFFF, subsampling.width);
            ArgumentChecks.ensureBetween("height", 0, 0xFFFF, subsampling.height);
            xSubsampling = subsampling.width;
            ySubsampling = subsampling.height;
        } else {
            xSubsampling = ySubsampling = 1;
        }
    }

    /**
     * If the user-supplied transform is waiting for processing by {@link MosaicCalculator}, returns it.
     * Otherwise returns {@code null}. This method is for internal usage by {@link MosaicCalculator} only.
     *
     * <p>This method clears the {@link #gridToCRS} field before to return. This is a way to tell that
     * processing is in progress, and also a safety against transform usage while it may become invalid.</p>
     *
     * @return the transform, or {@code null} if none. This method does not clone the returned value -
     *         {@link MosaicCalculator} will reference and modify directly that transform.
     */
    final synchronized AffineTransform getPendingGridToCRS() {
        if ((xSubsampling | ySubsampling) != 0) {
            // No transform waiting to be processed.
            return null;
        }
        final AffineTransform at = gridToCRS;
        gridToCRS = null;
        return at;
    }

    /**
     * Returns the <cite>"grid to real world"</cite> transform, or {@code null} if unknown.
     * This transform is derived from the value given to the constructor, but may not be identical
     * since it may have been {@linkplain AffineTransform#translate(double, double) translated}
     * in order to get a uniform grid geometry for every tiles.
     *
     * <div class="note"><b>Tip:</b>
     * the <a href="https://en.wikipedia.org/wiki/World_file">World File</a> coefficients of this tile
     * (i.e. the <cite>grid to CRS</cite> transform that we would have if the pixel in the upper-left
     * corner always had indices (0,0)) can be computed as below:
     *
     * {@preformat java
     *     Point location = tile.getLocation();
     *     AffineTransform gridToCRS = new AffineTransform(tile.getGridToCRS());
     *     gridToCRS.translate(location.x, location.y);
     * }
     * </div>
     *
     * @return the <cite>"grid to real world"</cite> transform mapping pixel
     *         {@linkplain PixelOrientation#UPPER_LEFT upper left} corner, or {@code null} if undefined.
     * @throws IllegalStateException if this tile has been {@linkplain #Tile(Rectangle, AffineTransform)
     *         created without location} and has not yet been processed by {@link MosaicCalculator}.
     */
    public synchronized AffineTransform2D getGridToCRS() throws IllegalStateException {
        ensureDefined();
        /*
         * The cast should not fail: if the `gridToCRS` is the one specified at construction time,
         * then `ensureDefined()` should have thrown an IllegalStateException. Otherwise this tile
         * have been processed by `MosaicCalculator`, which has set an `AffineTransform2D` instance.
         * If we get a ClassCastException below, then there is a bug in our pre/post conditions.
         */
        return (AffineTransform2D) gridToCRS;
    }

    /**
     * Sets the new <cite>"grid to real world"</cite> transform to use after the translation performed by
     * {@link #translate(int, int)}, if any. The given instance should be immutable; it will not be cloned.
     *
     * @param  at  the <cite>"grid to real world"</cite> transform mapping pixel
     *             {@linkplain PixelOrientation#UPPER_LEFT upper left} corner.
     * @throws IllegalStateException if another transform was already assigned to this tile.
     */
    final void setGridToCRS(final AffineTransform at) throws IllegalStateException {
        assert Thread.holdsLock(this);
        if (gridToCRS == null) {
            gridToCRS = at;
        } else if (!gridToCRS.equals(at)) {
            throw new IllegalStateException();
        }
    }

    /**
     * Translates this tile. For internal usage by {@link MosaicCalculator} only.
     *
     * <p>Reminder: {@link #setGridToCRS(AffineTransform)} should be invoked after this method.</p>
     *
     * @param  dx  the translation to apply on <var>x</var> values (often 0).
     * @param  dy  the translation to apply on <var>y</var> values (often 0).
     */
    final void translate(final int dx, final int dy) {
        assert Thread.holdsLock(this);
        x += dx;
        y += dy;
        gridToCRS = null;
    }

    /**
     * Returns a name for the tile format or tile input, or an empty value if none.
     * The format name can be inferred for example from an {@link javax.imageio.spi.ImageReaderSpi}.
     * The input name is typically (but not necessarily) a file name or URL.
     *
     * @param  input  {@code false} for the file format name, or {@code true} for the file input name.
     * @return the format or input name.
     */
    public Optional<String> getName(final boolean input) {
        return Optional.empty();
    }

    /**
     * Returns the image index to be given to the image reader for reading this tile.
     * The default implementation returns 0.
     *
     * @return the image index, numbered from 0.
     *
     * @see ImageReader#read(int)
     */
    public int getImageIndex() {
        return 0;
    }

    /**
     * Compares two tiles for optimal order in sequential reads. Default implementation sorts by
     * increasing {@linkplain #getImageIndex image index}. This ordering allows efficient access
     * for tiles that are stored sequentially in a file.
     *
     * <p>For tiles having the same image index, additional criterion are used like increasing
     * subsampling, increasing <var>y</var> then increasing <var>x</var> coordinates.
     * But the actual set of additional criterion may change in any future version.
     *
     * @param  other  the tile to compare with.
     * @return -1 if this tile should be read before {@code other},
     *         +1 if it should be read after or 0 if equal.
     */
    @Override
    public final int compareTo(final Tile other) {
        int c = getImageIndex() - other.getImageIndex();
        if (c == 0) {
            /*
             * From this point it does not matter much for disk access. But we continue to
             * define criterions for consistency with `equals(Object)` method. We compare
             * subsampling first because it may be undefined while it is needed for (x,y)
             * ordering. Undefined subsampling will be ordered first (this is arbitrary).
             */
            final int sy =  this.ySubsampling;
            final int oy = other.ySubsampling;
            c = sy - oy;
            if (c == 0) {
                final int sx =  this.xSubsampling;
                final int ox = other.xSubsampling;
                c = sx - ox;
                if (c == 0) {
                    c = (y * sy) - (other.y * oy);
                    if (c == 0) {
                        c = (x * sx) - (other.x * ox);
                    }
                }
            }
        }
        return c;
    }

    /**
     * Compares this tile with the specified one for equality. Two tiles are considered equal
     * if they are of the same class and have the same {@linkplain #getRegion() region} and
     * same {@linkplain #getSubsampling() subsampling}. Subclasses should override if there
     * is more properties to compare such as image format and index.
     *
     * @param  object  the object to compare with.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            final Tile that = (Tile) object;
            if (this.x == that.x  &&  this.y == that.y &&
                this.xSubsampling == that.xSubsampling &&
                this.ySubsampling == that.ySubsampling)
            {
                /*
                 * Compares width and height only if they are defined in both tiles.  We do not
                 * invoke `getRegion()` because it may be expensive and useless anyway: If both
                 * tiles have the same image reader, image index and input, then logically they
                 * must have the same size - invoking `getRegion()` would read exactly the same
                 * image twice.
                 */
                return (width  == 0 || that.width  == 0 || width  == that.width) &&
                       (height == 0 || that.height == 0 || height == that.height);
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for this tile.
     */
    @Override
    public int hashCode() {
        return x + 37*y;
    }

    /**
     * Returns a string representation of this tile for debugging purposes.
     *
     * @return a string representation of this tile.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(Classes.getShortClassName(this)).append('[');
        if ((xSubsampling | ySubsampling) != 0) {
            buffer.append("location=(");
            if (width == 0 && height == 0) {
                final Point location = getLocation();
                buffer.append(location.x).append(',').append(location.y);
            } else try {
                final Rectangle region = getRegion();
                buffer.append(region.x).append(',').append(region.y)
                      .append("), size=(").append(region.width).append(',').append(region.height);
            } catch (IOException e) {
                /*
                 * Should not happen since we checked that `getRegion()` should be easy.
                 * If it happen anyway, put the exception message at the place where
                 * coordinates were supposed to appear, so we can debug.
                 */
                buffer.append(e);
            }
            final Dimension subsampling = getSubsampling();
            buffer.append("), subsampling=(").append(subsampling.width)
                  .append(',').append(subsampling.height).append(')');
        } else {
            /*
             * Location and subsampling not yet computed, so don't display it. We can not
             * invoke `getRegion()` neither since it would throw an IllegalStateException.
             * Since we have to read the fields directly, make sure that this instance is
             * not a subclass, otherwise those values may be wrong.
             */
            if ((width != 0 || height != 0) && getClass() == Tile.class) {
                buffer.append("size=(").append(width).append(',').append(height).append(')');
            }
        }
        return buffer.append(']').toString();
    }

    /**
     * Returns a string representation of a collection of tiles. The tiles are formatted in a
     * table in iteration order. Tip: consider sorting the tiles before to invoke this method;
     * tiles are {@link Comparable} for this purpose.
     *
     * <p>This method is not public because it can consume a large amount of memory (the underlying
     * {@link StringBuffer} can be quite large). Users are encouraged to use the method expecting a
     * {@link Writer}, which may be expensive too but less than this method.</p>
     *
     * @param tiles    the tiles to format in a table.
     * @param maximum  the maximum number of tiles to format. If there is more tiles, a message will be
     *                 formatted below the table. A reasonable value like 5000 is recommended because
     *                 attempt to format millions of tiles leads to {@link OutOfMemoryError}.
     * @return a string representation of the given tiles as a table.
     */
    static String toString(final Collection<Tile> tiles, final int maximum) {
        final StringWriter writer = new StringWriter();
        try {
            writeTable(tiles, writer, maximum);
        } catch (IOException e) {
            // Should never happen since we are writing to a StringWriter.
            throw new AssertionError(e);
        }
        return writer.toString();
    }

    /**
     * Formats a collection of tiles in a table. The tiles are appended in iteration order.
     * Tip: consider sorting the tiles before to invoke this method;
     * tiles are {@link Comparable} for this purpose.
     *
     * @param tiles    the tiles to format in a table.
     * @param out      where to write the table.
     * @param maximum  the maximum number of tiles to format. If there is more tiles, a message will be
     *                 formatted below the table. A reasonable value like 5000 is recommended because
     *                 attempt to format millions of tiles leads to {@link OutOfMemoryError}.
     * @throws IOException if an error occurred while writing to the given writer.
     */
    public static void writeTable(final Collection<Tile> tiles, final Writer out, final int maximum) throws IOException {
        int remaining = maximum;
        final TableAppender table = new TableAppender(out);
        table.setMultiLinesCells(false);
        table.nextLine('═');
        table.append("Format\tInput\tindex\tx\ty\twidth\theight\tdx\tdy\n");
        table.nextLine('─');
        table.setMultiLinesCells(true);
        for (final Tile tile : tiles) {
            if (--remaining < 0) {
                break;
            }
            table.setCellAlignment(TableAppender.ALIGN_LEFT);
            tile.getName(false).ifPresent(table::append); table.nextColumn();
            tile.getName(true) .ifPresent(table::append); table.nextColumn();
            table.setCellAlignment(TableAppender.ALIGN_RIGHT);
            table.append(String.valueOf(tile.getImageIndex()));
            table.nextColumn();
            /*
             * Extract now the tile information that we are going to format. Those information may
             * be replaced by the information provided by getter methods (they should be the same,
             * unless a subclass override those methods).
             */
            int x            = tile.x;
            int y            = tile.y;
            int width        = tile.width;
            int height       = tile.height;
            int xSubsampling = tile.xSubsampling;
            int ySubsampling = tile.ySubsampling;
            try {
                final Dimension subsampling = tile.getSubsampling();
                xSubsampling = subsampling.width;
                ySubsampling = subsampling.height;
                try {
                    final Rectangle region = tile.getRegion();
                    x      = region.x;
                    y      = region.y;
                    width  = region.width;
                    height = region.height;
                } catch (IOException e) {
                    /*
                     * The (x,y) are likely to be correct since only (width,height) are read
                     * from the image file. So set only (width,height) to "unknown" and keep
                     * the remaining, with (x,y) obtained from direct access to Tile fields.
                     */
                    width  = 0;
                    height = 0;
                }
            } catch (IllegalStateException e) {
                // Ignore. Format using the information read from the fields as a fallback.
            }
            table.append(String.valueOf(x));
            table.nextColumn();
            table.append(String.valueOf(y));
            if ((width | height) != 0) {
                table.nextColumn();
                table.append(String.valueOf(width));
                table.nextColumn();
                table.append(String.valueOf(height));
            } else {
                table.nextColumn();
                table.nextColumn();
            }
            if ((xSubsampling | ySubsampling) != 0) {
                table.nextColumn();
                table.append(String.valueOf(xSubsampling));
                table.nextColumn();
                table.append(String.valueOf(ySubsampling));
            }
            table.nextLine();
        }
        table.nextLine('═');
        /*
         * Table completed. Flushs to the writer and appends additional text if we have
         * not formatted every tiles. IOException may be thrown starting from this point
         * (the above code is not expected to thrown any IOException).
         */
        table.flush();
        if (remaining < 0) {
            out.write(Vocabulary.getResources((Locale) null).getString(Vocabulary.Keys.More_1, tiles.size() - maximum));
            out.write(System.lineSeparator());
        }
    }
}
