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
package org.apache.sis.internal.coverage.j2d;

import java.util.Locale;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.ImagingOpException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.Modules;


/**
 * An image which wraps an existing image unchanged, except for a property which is computed
 * on the fly when first requested. All methods delegate to the wrapped image except the one
 * for getting the property value and {@link #getSources()}.
 *
 * <p>The name of the computed property is given by {@link #getComputedPropertyName()}.
 * In addition this method automatically creates another property with the same name
 * and {@value #ERRORS_SUFFIX} suffix. That property will contain a {@link LogRecord}
 * with the exception that occurred during tile computations, if any.
 * The computation results are cached by this class.</p>
 *
 * <div class="note"><b>Design note:</b>
 * most non-abstract methods are final because {@link org.apache.sis.image.PixelIterator}
 * (among others) relies on the fact that it can unwrap this image and still get the same
 * pixel values.</div>
 *
 * This class implements various {@link java.util.function} interfaces for implementation convenience
 * (would not be recommended for public API, but this is an internal class). Users should not rely on
 * this fact. Compared to lambda functions, this is one less level of indirection and makes stack traces
 * a little bit shorter to analyze in case of exceptions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <A>  type of the thread-local object (the accumulator) for holding intermediate results during computation.
 *              This is usually the final type of the property value, but not necessarily.
 *
 * @since 1.1
 * @module
 */
public abstract class PropertyCalculator<A> implements RenderedImage,
        Supplier<A>, BinaryOperator<A>, BiConsumer<A, Raster>, Consumer<LogRecord>
{
    /**
     * The suffix to add to property name for errors that occurred during computation.
     */
    public static final String ERRORS_SUFFIX = ".errors";

    /**
     * The source image from which to compute the property.
     */
    public final RenderedImage source;

    /**
     * The computation result, or {@link Image#UndefinedProperty} if not yet computed.
     * Note that {@code null} is a valid result.
     */
    private Object result;

    /**
     * The errors that occurred while computing the result, or {@code null} if none
     * or not yet determined.
     */
    private LogRecord errors;

    /**
     * Creates a new calculator wrapping the given image.
     *
     * @param  source  the image to wrap.
     */
    protected PropertyCalculator(final RenderedImage source) {
        this.source = source;
        result = Image.UndefinedProperty;
    }

    /**
     * Returns the source of this image. The default implementation
     * returns {@link #source} in an vector of length 1.
     *
     * @return the source (usually only {@linkplain #source}) of this image.
     */
    @Override
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public final Vector<RenderedImage> getSources() {
        final Vector<RenderedImage> sources = new Vector<>(1);
        sources.add(source);
        return sources;
    }

    /**
     * If the property should be computed on a subset of the tiles,
     * the pixel coordinates of the region intersecting those tiles.
     * The default implementation returns {@code null}.
     *
     * @return pixel coordinates of the region of interest, or {@code null} for the whole image.
     */
    protected Rectangle getAreaOfInterest() {
        return null;
    }

    /**
     * Returns the name of the property which is computed by this image.
     *
     * @return name of property computed by this image. Shall not be null.
     */
    protected abstract String getComputedPropertyName();

    /**
     * Returns an array of names recognized by {@link #getProperty(String)}.
     * The default implementation returns the {@linkplain #source} properties names
     * followed by {@link #getComputedPropertyName()} and the error property name.
     *
     * @return all recognized property names.
     */
    @Override
    public final String[] getPropertyNames() {
        final String name = getComputedPropertyName();
        return ArraysExt.concatenate(source.getPropertyNames(), new String[] {name, name + ERRORS_SUFFIX});
    }

    /**
     * Returns whether the given name is the name of the error property.
     * The implementation of this method avoids the creation of concatenated string.
     *
     * @param  cn    name of the computed property.
     * @param  name  the property name to test.
     * @return whether {@code name} is {@code cn} + {@value #ERRORS_SUFFIX}.
     */
    private static boolean isErrorProperty(final String cn, final String name) {
        return name.length() == cn.length() + ERRORS_SUFFIX.length() &&
                    name.startsWith(cn) && name.endsWith(ERRORS_SUFFIX);
    }

    /**
     * Gets a property from this image or from its source. If the given name is for the property
     * to be computed by this class and if that property has not been computed before, then this
     * method starts computation now and caches the result.
     *
     * @param  name  name of the property to get.
     * @return the property for the given name (may be {@code null}).
     */
    @Override
    public final Object getProperty(final String name) {
        if (name != null) {
            final String cn = getComputedPropertyName();
            final boolean isProperty = cn.equals(name);
            if (isProperty || isErrorProperty(cn, name)) {
                synchronized (this) {
                    if (result == Image.UndefinedProperty) {
                        final TileOpExecutor executor = new TileOpExecutor(source, getAreaOfInterest());
                        result = executor.executeOnReadable(source, Collector.of(this, this, this), this);
                    }
                    return isProperty ? result : errors;
                }
            }
        }
        return source.getProperty(name);
    }

    /**
     * Invoked by {@link TileOpExecutor} if an error occurred while processing tiles.
     * This method should be invoked at most once.
     *
     * @param  record  a description of the error that occurred.
     */
    @Override
    public final synchronized void accept(final LogRecord record) {
        if (errors != null) {
            throw new IllegalStateException();      // Should never happen.
        }
        /*
         * Completes the record with source identification as if the
         * error occurred from above `getProperty(String)` method.
         */
        record.setSourceClassName(RenderedImage.class.getCanonicalName());
        record.setSourceMethodName("getProperty");
        record.setLoggerName(Modules.RASTER);
        errors = record;
    }

    /**
     * Invoked for creating the object holding the information to be computed by a single thread.
     * This method will be invoked for each worker thread before the worker starts its execution.
     *
     * @return a thread-local variable holding information computed by a single thread.
     *         May be {@code null} is such objects are not needed.
     */
    @Override
    public abstract A get();

    /**
     * Invoked after a thread finished to process all its tiles and wants to combine its result with the
     * result of another thread. This method is invoked only if {@link #get()} returned a non-null value.
     * This method does not need to be thread-safe; synchronizations will be done by the caller.
     *
     * @param  previous  the result of another thread (never {@code null}).
     * @param  computed  the result computed by current thread (never {@code null}).
     * @return combination of the two results. May be one of the {@code previous} or {@code computed} instances.
     */
    @Override
    public abstract A apply(A previous, A computed);

    /**
     * Executes this operation on the given tile. This method may be invoked from any thread.
     * If an exception occurs during computation, that exception will be logged or wrapped in
     * an {@link ImagingOpException} by the caller {@link TileOpExecutor}.
     *
     * @param  accumulator  the thread-local variable created by {@link #get()}. May be {@code null}.
     * @param  tile         the tile on which to perform a computation.
     * @throws RuntimeException if the calculation failed.
     */
    @Override
    public abstract void accept(A accumulator, Raster tile);

    /** Delegates to the wrapped image. */
    @Override public final ColorModel     getColorModel()            {return source.getColorModel();}
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
    @Override public final String         toString()                 {return source.toString();}
}
