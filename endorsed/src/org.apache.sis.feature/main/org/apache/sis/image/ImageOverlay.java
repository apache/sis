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
package org.apache.sis.image;

import java.awt.Image;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.BiConsumer;
import javax.measure.Quantity;
import javax.measure.UnconvertibleException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.math.Statistics;
import org.apache.sis.measure.Quantities;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.image.privy.ImageUtilities;


/**
 * An overlay of an arbitrary number of images. All images have the same pixel coordinate system,
 * but potentially different bounding boxes, tile sizes and tile indices. Source images are drawn
 * in reverse order: the last source image is drawn first, and the first source image is drawn last
 * on top of all other images. All images are considered fully opaque, including the alpha channel
 * which is handled as an ordinary band.
 * The requirements are:
 *
 * <ul>
 *   <li>All source images shall have the same pixel coordinate systems (but not necessarily the same tile matrix).</li>
 *   <li>All source images shall have the same number of bands (but not necessarily the same sample model).</li>
 *   <li>All source images should have equivalent color model, otherwise color consistency is not guaranteed.</li>
 *   <li>At least one image shall intersect the given bounds.</li>
 * </ul>
 *
 * This class can also be opportunistically used for reformatting a single image.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ImageOverlay extends MultiSourceImage {
    /**
     * The valid area.
     *
     * @see #getValidArea()
     */
    private final Shape validArea;

    /**
     * The contribution of each source as the valid area of a source minus all previous contributions.
     * The length of this array is the number of sources, in same order.
     */
    private final Area[] contributions;

    /**
     * Creates a new image overlay or returns one of the given sources if equivalent.
     * All source images shall have the same pixels coordinate system and the same number of bands.
     * The returned image may have less sources than the specified ones if this method determines
     * that some sources will never be drawn. This method may return {@code sources[0]} directly.
     *
     * @param  sources       the images to overlay. Null array elements are ignored.
     * @param  bounds        range of pixel coordinates, or {@code null} for the union of all source images.
     * @param  sampleModel   the sample model, of {@code null} for automatic.
     * @param  colorizer     function for deriving a color model, of {@code null} for automatic.
     * @param  autoTileSize  whether this method is allowed to change the tile size.
     * @param  parallel      whether parallel computation is allowed.
     * @return the image overlay, or one of the given sources if only one is suitable.
     * @throws IllegalArgumentException if there is an incompatibility between some source images
     *         or if no image intersect the bounds.
     */
    static RenderedImage create(RenderedImage[] sources, Rectangle bounds, SampleModel sampleModel,
            final Colorizer colorizer, final boolean autoTileSize, final boolean parallel)
    {
        final Area aoi = (bounds != null) ? new Area(bounds) : null;
        Area[] contributions = new Area[sources.length];
        final Area validArea = new Area();
        ColorModel colorModel = null;
        /*
         * Filter the source images for keeping only the ones that intersect the bounds.
         * Check image compatibility (number of bands) and color model in the same loop.
         * If there is only one image left after filtering, it may be returned directly.
         */
        int numBands=0, count=0;
        sources = sources.clone();
        for (final RenderedImage source : sources) {
            final int n = ImageUtilities.getNumBands(source);
            if (n == 0) continue;       // Skip null elements.
            if (n != numBands) {
                if (numBands != 0) {
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.UnexpectedNumberOfBands_2, numBands, n));
                }
                numBands = n;
            }
            /*
             * If the current source does not intersect the specified area of interest, or if a previous source
             * fully overlaps the current source, then the latter image will never be drawn and can be omitted.
             */
            final Area area = new Area(ImageUtilities.getValidArea(source));
            if (aoi != null) area.intersect(aoi);
            if (area.isEmpty()) continue;         // Source does not intersect the specified bounds.
            final Area contrib = new Area(area);
            contrib.subtract(validArea);
            if (contrib.isEmpty()) continue;      // The new source is fully masked by previous sources.
            validArea.add(area);
            contributions[count] = contrib;
            sources[count++] = source;
            /*
             * The default sample model is selected after filtering because the choice of a sample model
             * does not change the visual, while it has an incidence on performance: it is better if the
             * tile matrix of this image matches the tile matrix of the main image. The choice of a color
             * model may change the visual, but is kept together with the sample model for simplicity and
             * for reducing the risk that an image is rendered with the wrong colors.
             */
            if (sampleModel == null) {
                sampleModel = source.getSampleModel();      // Should never be null.
            }
            if (colorModel == null) {
                final ColorModel candidate = source.getColorModel();
                if (candidate != null && candidate.isCompatibleSampleModel(sampleModel)) {
                    colorModel = candidate;
                }
            }
        }
        /*
         * Except if there is no image, the sample model should be non-null at this point.
         * The color model is optionally specified in the colorizer, which we check now
         * with the current color model used only as a fallback.
         */
        if (count == 0) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.SourceImagesDoNotIntersect));
        }
        final RenderedImage main = sources[0];
        if (colorizer != null) {
            colorModel = colorizer.apply(new Colorizer.Target(sampleModel, main)).orElse(colorModel);
        }
        if (count == 1 && sampleModel.equals(main.getSampleModel())) {
            return (colorModel != null) ? RecoloredImage.apply(main, colorModel) : main;
        }
        sources = ArraysExt.resize(sources, count);
        contributions = ArraysExt.resize(contributions, count);
        /*
         * The valid area defines the size of the combined image, unless specified otherwise.
         * If the tile size is not a divisor of the image size, try to find a better tile size.
         */
        if (bounds == null) {
            bounds = validArea.getBounds();
        }
        if (autoTileSize) {
            var tileSize = new Dimension(sampleModel.getWidth(), sampleModel.getHeight());
            if ((bounds.width % tileSize.width) != 0 || (bounds.height % tileSize.height) != 0) {
                tileSize = ImageLayout.DEFAULT.withPreferredTileSize(tileSize).suggestTileSize(bounds.width, bounds.height);
                sampleModel = sampleModel.createCompatibleSampleModel(tileSize.width, tileSize.height);
            }
        }
        var minTile = new Point(ImageUtilities.pixelToTileX(main, bounds.x),
                                ImageUtilities.pixelToTileY(main, bounds.y));
        return ImageProcessor.unique(new ImageOverlay(
                sources, contributions, validArea, bounds, minTile, sampleModel, colorModel, parallel));
    }

    /**
     * Creates a new image overlay.
     */
    private ImageOverlay(final RenderedImage[] sources, final Area[] contributions, final Area validArea, final Rectangle bounds,
                         final Point minTile, final SampleModel sampleModel, final ColorModel colorModel,
                         final boolean parallel)
    {
        super(sources, bounds, minTile, sampleModel, colorModel, parallel);
        this.validArea = validArea.isRectangular() ? validArea.getBounds2D() : validArea;
        this.contributions = contributions;
    }

    /**
     * Returns a shape containing all pixels that are valid in this image.
     *
     * @return the valid area of the source converted to the coordinate system of this resampled image.
     */
    @Override
    public Shape getValidArea() {
        Shape domain = validArea;
        if (domain instanceof Area) {
            domain = (Area) ((Area) domain).clone();    // Cloning an Area is cheap.
        } else if (domain instanceof Rectangle2D) {
            domain = (Rectangle2D) ((Rectangle2D) domain).clone();
        }
        return domain;
    }

    /**
     * Returns the names of all recognized properties, or {@code null} if this image has no properties.
     * The implementation iterates over all sources images on the assumption that there is not many of them.
     * We do not cache the result for making sure that any change in the sources is reflected here.
     */
    @Override
    public String[] getPropertyNames() {
        final int n = getNumSources();
        final var count = new LinkedHashMap<String,Integer>();
        for (int i=0; i<n; i++) {
            final String[] names = getSource(i).getPropertyNames();
            if (names != null) {
                for (String name : names) {
                    /*
                     * This switch shall contain the same cases as in the `getProperty(String)` method.
                     * For properties considered present as soon as it is defined in at least one source,
                     * we set the count directly to `n`. For properties that must be present in all sources,
                     * we count their occurrences.
                     */
                    switch (name) {
                        case GRID_GEOMETRY_KEY:
                        case SAMPLE_DIMENSIONS_KEY:
                        case POSITIONAL_ACCURACY_KEY: count.put(name, n); break;
                        case SAMPLE_RESOLUTIONS_KEY:
                        case STATISTICS_KEY: count.merge(name, 1, Math::addExact); break;
                    }
                }
            }
        }
        count.values().removeIf((v) -> v != n);
        return count.isEmpty() ? null : count.keySet().toArray(String[]::new);
    }

    /**
     * Gets the property of the given name. Each property is derived from the source images in its own way.
     * For example, {@link #STATISTICS_KEY} is computed by combining the statistics provided by each source,
     * while {@link #SAMPLE_RESOLUTIONS_KEY} takes for each band the minimal values of all sources.
     *
     * <h4>Implementation note</h4>
     * This method does not cache the property values on the assumption that there is not many sources,
     * that these sources already have their own cache and that merging the values is efficient enough.
     * This approach avoids the need to clone the cached values and to respond to events that may change
     * the cached values.
     *
     * @param  name  name of the property to compute.
     * @return property value (may be {@code null}), or {@link Image#UndefinedProperty} if none.
     */
    @Override
    public Object getProperty(final String key) {
        switch (key) {
            case GRID_GEOMETRY_KEY:       // Fall through
            case SAMPLE_DIMENSIONS_KEY:   return getConstantProperty(key);
            case POSITIONAL_ACCURACY_KEY: return getCombinedProperty(key, Quantity[].class,   (q) -> q.clone(),            ImageOverlay::combine, false);
            case SAMPLE_RESOLUTIONS_KEY:  return getCombinedProperty(key, double[].class,     double[]::clone,             ImageOverlay::combine, true);
            case STATISTICS_KEY:          return getCombinedProperty(key, Statistics[].class, StatisticsCalculator::clone, ImageOverlay::combine, true);
            default:                      return Image.UndefinedProperty;
        }
    }

    /**
     * Returns a property value which is expected to be constant in all source images, ignoring undefined values.
     * If the property is not constant, then this method returns {@link Image#UndefinedProperty}.
     *
     * @param  key  name of the property to get.
     * @return property value (may be {@code null}), or {@link Image#UndefinedProperty} if none.
     */
    private Object getConstantProperty(final String key) {
        Object result = Image.UndefinedProperty;
        final int n = getNumSources();
        for (int i=0; i<n; i++) {
            final Object c = getSource(i).getProperty(key);
            if (c != Image.UndefinedProperty) {
                if (result == Image.UndefinedProperty) {
                    result = c;
                } else if (!Objects.deepEquals(result, c)) {
                    return Image.UndefinedProperty;
                }
            }
        }
        return result;
    }

    /**
     * Returns a property value which is computed by combining the values from all source images.
     * Undefined values are ignored if {@code required} is false. If {@code required} is true,
     * then any missing value will cause this method to return {@link Image#UndefinedProperty}.
     *
     * @param  <V>       compile-time value of the {@code type} argument.
     * @param  key       name of the property to get.
     * @param  type      type of values to combine. Often an array type.
     * @param  cloner    method creating a clone of the first value found.
     * @param  combiner  method updating the clone with more values.
     * @param  required  whether the property must be provided in all images for being considered defined.
     * @return property value, or {@link Image#UndefinedProperty} if none.
     */
    private <V> Object getCombinedProperty(final String key, final Class<V> type,
            final Function<V,V> cloner, final BiConsumer<V,V> combiner, final boolean required)
    {
        V result = null;
        final int n = getNumSources();
        for (int i=0; i<n; i++) {
            final Object value = getSource(i).getProperty(key);
            if (type.isInstance(value)) {
                @SuppressWarnings("unchecked")
                final V c = (V) value;
                if (result == null) {
                    result = cloner.apply(c);
                } else try {
                    combiner.accept(result, c);
                } catch (UnconvertibleException e) {
                    Logging.recoverableException(ImageUtilities.LOGGER, ImageOverlay.class, "getProperty", e);
                    return Image.UndefinedProperty;
                }
            } else if (required) {
                return Image.UndefinedProperty;
            }
        }
        return (result != null) ? result : Image.UndefinedProperty;
    }

    /**
     * Combines the statistics of previous source images with statistics of a new source image.
     * This method is invoked for computing the {@value #STATISTICS_KEY} property.
     *
     * @param result  combination done so for.
     * @param more    statistics of another source to combine.
     */
    private static void combine(final Statistics[] result, final Statistics[] more) {
        for (int i = Math.min(result.length, more.length); --i >= 0;) {
            result[i].combine(more[i]);
        }
    }

    /**
     * Combines the resolution of previous source images with resolution of a new source image.
     * This method is invoked for computing the {@value #SAMPLE_RESOLUTIONS_KEY} property.
     * The minimum value is retained because this property is about resolution, not accuracy.
     * It is used for computing the number of fraction digits needed to distinguish the values of two cells.
     *
     * @param result  combination done so for.
     * @param more    resolution of another source to combine.
     */
    private static void combine(final double[] result, final double[] more) {
        for (int i = Math.min(result.length, more.length); --i >= 0;) {
            final double value = more[i];
            final double previous = result[i];
            if (value < previous || Double.isNaN(previous)) {
                result[i] = value;
            }
        }
    }

    /**
     * Combines the positional accuracy of previous source images with accuracy of a new source image.
     * This method is invoked for computing the {@value #POSITIONAL_ACCURACY_KEY} property.
     *
     * <p>This method signature is unsafe. However, Apache <abbr>SIS</abbr> implementation of
     * the {@link Quantities#min(Quantity, Quantity)} method performs the required checks.</p>
     *
     * @param  result  combination done so for.
     * @param  more    positional accuracy of another source to combine.
     * @throws UnconvertibleException if the quantities are not comparable.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})    // See method Javadoc.
    private static void combine(final Quantity[] result, final Quantity[] more) {
        for (int i = Math.min(result.length, more.length); --i >= 0;) {
            result[i] = Quantities.max(result[i], more[i]);
        }
    }

    /**
     * Computes the tile at specified indices.
     *
     * @param  tileX   the column index of the tile to compute.
     * @param  tileY   the row index of the tile to compute.
     * @param  target  if the tile already exists but needs to be updated, the tile to update. Otherwise {@code null}.
     * @return computed tile for the given indices (cannot be null).
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY, WritableRaster target) {
        if (target == null) {
            target = createTile(tileX, tileY);
        }
        final Rectangle aoi = target.getBounds();
        final int n = getNumSources();
        for (int i=n; --i >= 0;) {
            if (contributions[i].intersects(aoi)) {
                final RenderedImage source = getSource(i);
                final Rectangle bounds = getBounds();
                ImageUtilities.clipBounds(source, bounds);
                if (!bounds.isEmpty()) {
                    copyData(bounds, source, target);
                }
            }
        }
        return target;
    }

    /**
     * Notifies the source images that tiles will be computed soon in the given region.
     * This method forwards the notification to all images that are instances of {@link PlanarImage}.
     */
    @Override
    protected Disposable prefetch(final Rectangle tiles) {
        final Rectangle aoi = ImageUtilities.tilesToPixels(this, tiles);
        final int n = getNumSources();
        final var sources = new RenderedImage[n];
        int count = 0;
        for (int i=0; i<n; i++) {
            final RenderedImage source = getSource(i);
            if (source instanceof PlanarImage) {
                if (contributions[i].intersects(aoi)) {
                    sources[count++] = source;
                }
            }
        }
        return new MultiSourcePrefetch(ArraysExt.resize(sources, count), aoi).run(parallel);
    }
}
