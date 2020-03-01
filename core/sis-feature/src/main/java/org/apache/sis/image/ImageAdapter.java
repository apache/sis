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

import java.util.Vector;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;


/**
 * An image which wraps an existing image unchanged, except for properties and/or color model.
 * All {@link RenderedImage} methods related to coordinate systems (pixel coordinates or tile
 * indices), and all methods fetching tiles, delegate to the wrapped image.
 *
 * <div class="note"><b>Design note:</b>
 * most non-abstract methods are final because {@link PixelIterator} (among others) relies
 * on the fact that it can unwrap this image and still get the same pixel values.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
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
        this.source = source;
    }

    /**
     * Returns the {@linkplain #source} of this image in an vector of length 1.
     *
     * @return the unique {@linkplain #source} of this image.
     */
    @Override
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public final Vector<RenderedImage> getSources() {
        final Vector<RenderedImage> sources = new Vector<>(1);
        sources.add(source);
        return sources;
    }

    /**
     * Returns the names of properties of wrapped image.
     *
     * @return all recognized property names.
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
     * Returns a string representation of this image for debugging purpose.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(100);
        final Class<?> subtype = stringStart(buffer.append('['));
        return buffer.insert(0, subtype.getSimpleName()).append(" on ").append(source).append(']').toString();
    }

    /**
     * Appends a content to show in the {@link #toString()} representation,
     * after the class name and before the string representation of the wrapped image.
     *
     * @param  buffer  where to start writing content of {@link #toString()} representation.
     * @return name of the class to show in the {@link #toString()} representation.
     */
    abstract Class<? extends ImageAdapter> stringStart(StringBuilder buffer);
}
