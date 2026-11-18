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
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.TileObserver;
import java.lang.ref.WeakReference;


/**
 * An image which wraps an existing image and keep the same pixel coordinate systems and tile indices.
 * All {@link RenderedImage} methods related to coordinate systems (pixel coordinates or tile indices)
 * delegate to the wrapped image.
 * This class is similar to {@link SourceAlignedImage} except that it does not extend {@link ComputedImage}.
 *
 * <p>All subclasses shall override {@link #equals(Object)} and {@link #hashCode()}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class ImageAdapter extends PlanarImage {
    /**
     * The source image wrapped by this adapter.
     */
    protected final RenderedImage source;

    /**
     * The listener registered on source image, or {@code null} if none.
     * This listener use weak reference, therefore it should not cause
     * retention of large amount of memory.
     */
    private TileObserver sourceListener;

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

    /**
     * Returns the sample model of this image.
     */
    @Override
    public final SampleModel getSampleModel() {
        return source.getSampleModel();
    }

    /** Delegates to the wrapped image. */
    @Override public final int getWidth()           {return source.getWidth();}
    @Override public final int getHeight()          {return source.getHeight();}
    @Override public final int getMinX()            {return source.getMinX();}
    @Override public final int getMinY()            {return source.getMinY();}
    @Override public final int getNumXTiles()       {return source.getNumXTiles();}
    @Override public final int getNumYTiles()       {return source.getNumYTiles();}
    @Override public final int getMinTileX()        {return source.getMinTileX();}
    @Override public final int getMinTileY()        {return source.getMinTileY();}
    @Override public final int getTileWidth()       {return source.getTileWidth();}
    @Override public final int getTileHeight()      {return source.getTileHeight();}
    @Override public final int getTileGridXOffset() {return source.getTileGridXOffset();}
    @Override public final int getTileGridYOffset() {return source.getTileGridYOffset();}

    /**
     * Returns whether this image may notify its observers about tile updates.
     * A value of {@code false} means that this image will not be notified about changes in its source,
     * therefore this image will not forward tile update events, therefore observers registered on this
     * class can be ignored in order to reduce the risk of memory leaks (listeners can be other images).
     * In case of doubt (implementations that we do not control), conservatively returns {@code true}.
     *
     * @return whether tile update events may occur.
     */
    @Override
    final boolean mayFireTileUpdates() {
        assert Thread.holdsLock(this);
        if (sourceListener == null) {
            final var listener = new Listener(this);
            if (addTileObserver(source, listener)) {
                sourceListener = listener;
            }
        }
        return sourceListener != null;
    }

    /**
     * A listener using weak reference for notifying the enclosed image that a source changed.
     */
    private static final class Listener extends WeakReference<ImageAdapter> implements Observer {
        /**
         * Creates a new listener which will notify the given image.
         *
         * @param  target  the image to notify when a source changed.
         */
        Listener(final ImageAdapter target) {
            super(target);
        }

        /**
         * Invoked when a source image changed.
         * The enclosed image is notified only after the tile calculation finished.
         *
         * @param source   the image that owns the updated tile.
         * @param tileX    the <var>x</var> index of the tile that is being updated.
         * @param tileY    the <var>y</var> index of the tile that is being updated.
         * @param writing  {@code true} before writing, {@code false} after update.
         */
        @Override
        public void tileUpdate(final RenderedImage source, final int tileX, final int tileY, final boolean writing) {
            final ImageAdapter target = get();
            if (target != null) {
                target.fireTileUpdate(tileX, tileY, writing);
            } else {
                removeTileObserver(source, this);
            }
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
