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

import java.util.Objects;
import java.util.Vector;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.apache.sis.util.Disposable;


/**
 * An image which wraps an existing image unchanged, except for properties and/or color model.
 * All {@link RenderedImage} methods related to coordinate systems (pixel coordinates or tile
 * indices), and all methods fetching tiles, delegate to the wrapped image.
 *
 * <h2>Design note</h2>
 * Most non-abstract methods are final because {@link PixelIterator} (among others) relies
 * on the fact that it can unwrap this image and still get the same pixel values.
 *
 * <h2>Relationship with other classes</h2>
 * This class is similar to {@link SourceAlignedImage} except that it does not extend {@link ComputedImage}
 * and forwards {@link #getTile(int, int)}, {@link #getData()} and other data methods to the source image.
 *
 * <h2>Requirements for subclasses</h2>
 * All subclasses shall override {@link #equals(Object)} and {@link #hashCode()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class ImageAdapter extends PlanarImage {
    /**
     * The source image wrapped by this adapter.
     */
    protected final RenderedImage source;

    /**
     * Creates a new wrapper for the given image.
     *
     * @param  source  the image to wrap.
     */
    protected ImageAdapter(final RenderedImage source) {
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Returns the {@linkplain #source} of this image in an vector of length 1.
     *
     * @return the unique {@linkplain #source} of this image.
     */
    @Override
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public final Vector<RenderedImage> getSources() {
        final var sources = new Vector<RenderedImage>(1);
        sources.add(source);
        return sources;
    }

    /**
     * Returns the names of properties of wrapped image.
     *
     * @return all recognized property names, or {@code null} if none.
     */
    @Override
    public String[] getPropertyNames() {
        return source.getPropertyNames();
    }

    /**
     * Gets a property from this image or from its source.
     *
     * @param  name  name of the property to get.
     * @return the property for the given name ({@code null} is a valid result),
     *         or {@link Image#UndefinedProperty} if the given name is not a recognized property name.
     */
    @Override
    public Object getProperty(final String name) {
        return source.getProperty(name);
    }

    /**
     * Returns the color model of this image.
     */
    @Override
    public ColorModel getColorModel() {
        return source.getColorModel();
    }

    /** Delegates to the wrapped image. */
    @Override public final SampleModel    getSampleModel()           {return source.getSampleModel();}
    @Override public final int            getWidth()                 {return source.getWidth();}
    @Override public final int            getHeight()                {return source.getHeight();}
    @Override public final int            getMinX()                  {return source.getMinX();}
    @Override public final int            getMinY()                  {return source.getMinY();}
    @Override public final int            getNumXTiles()             {return source.getNumXTiles();}
    @Override public final int            getNumYTiles()             {return source.getNumYTiles();}
    @Override public final int            getMinTileX()              {return source.getMinTileX();}
    @Override public final int            getMinTileY()              {return source.getMinTileY();}
    @Override public final int            getTileWidth()             {return source.getTileWidth();}
    @Override public final int            getTileHeight()            {return source.getTileHeight();}
    @Override public final int            getTileGridXOffset()       {return source.getTileGridXOffset();}
    @Override public final int            getTileGridYOffset()       {return source.getTileGridYOffset();}
    @Override public final Raster         getTile(int tx, int ty)    {return source.getTile(tx, ty);}
    @Override public final Raster         getData()                  {return source.getData();}
    @Override public final Raster         getData(Rectangle region)  {return source.getData(region);}
    @Override public final WritableRaster copyData(WritableRaster r) {return source.copyData(r);}

    /**
     * Notifies the source image that tiles will be computed soon in the given region.
     * If the source image is an instance of {@link PlanarImage}, then this method
     * forwards the notification to it. Otherwise default implementation does nothing.
     */
    @Override
    protected Disposable prefetch(final Rectangle tiles) {
        if (source instanceof PlanarImage) {
            /*
             * Forwarding directly is possible because the contract
             * of this class said that tile indices must be the same.
             */
            return ((PlanarImage) source).prefetch(tiles);
        } else {
            return super.prefetch(tiles);
        }
    }

    /**
     * Compares the given object with this image for equality. This method should be quick and compare
     * how images compute their values from their sources; it should not compare the actual pixel values.
     *
     * <p>The default implementation returns {@code true} if the given object is non-null, is an instance
     * of the exact same class as this image and the {@linkplain #source} of both images are equal.
     * Subclasses should override this method if more properties need to be compared.</p>
     *
     * @param  object  the object to compare with this image.
     * @return {@code true} if the given object is an image performing the same calculation as this image.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            return source.equals(((ImageAdapter) object).source);
        }
        return false;
    }

    /**
     * Returns a hash code value for this image. This method should be quick, for example using
     * only a description of the operation to be done (e.g. implementation class, parameters).
     * This method should not compute the hash code from sample values.
     *
     * <p>The default implementation computes a hash code based on the {@link #source} hash code and
     * this image class. Subclasses should override this method if more properties need to be hashed.</p>
     *
     * @return a hash code value based on a description of the operation performed by this image.
     */
    @Override
    public int hashCode() {
        return source.hashCode() ^ getClass().hashCode();
    }

    /**
     * Returns a string representation of this image for debugging purpose.
     */
    @Override
    public String toString() {
        final var buffer = new StringBuilder(100);
        final Class<?> subtype = appendStringContent(buffer.append('['));
        return buffer.insert(0, subtype.getSimpleName()).append(" on ").append(source).append(']').toString();
    }

    /**
     * Appends a content to show in the {@link #toString()} representation,
     * after the class name and before the string representation of the wrapped image.
     *
     * @param  buffer  where to start writing content of {@link #toString()} representation.
     * @return name of the class to show in the {@link #toString()} representation.
     */
    abstract Class<? extends ImageAdapter> appendStringContent(StringBuilder buffer);
}
