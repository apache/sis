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

import java.util.Locale;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collector;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.coverage.j2d.TileOpExecutor;


/**
 * An image which wraps an existing image unchanged, except for properties which are computed
 * on the fly when first requested. All {@link RenderedImage} methods delegate to the wrapped
 * image except {@link #getSources()} and the methods for getting the property names or values.
 *
 * <p>The name of the computed property is given by {@link #getComputedPropertyName()}.
 * In addition this method automatically creates another property with the same name
 * and {@value #ERRORS_SUFFIX} suffix. That property will contain a {@link LogRecord}
 * with the exception that occurred during tile computations, if any.
 * The computation results are cached by this class.</p>
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
abstract class AnnotatedImage implements RenderedImage {
    /**
     * The suffix to add to property name for errors that occurred during computation.
     */
    public static final String ERRORS_SUFFIX = ".errors";

    /**
     * The source image from which to compute the property.
     */
    protected final RenderedImage source;

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
     * Whether parallel execution is authorized for the {@linkplain #source} image.
     */
    private final boolean parallel;

    /**
     * Creates a new annotated image wrapping the given image.
     * The annotations are the additional properties computed by the subclass.
     *
     * @param  source    the image to wrap for adding properties (annotations).
     * @parma  parallel  whether parallel execution is authorized.
     */
    protected AnnotatedImage(final RenderedImage source, final boolean parallel) {
        this.source   = source;
        this.parallel = parallel;
        result = Image.UndefinedProperty;
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
     * If the properties should be computed on a subset of the tiles,
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
    public String[] getPropertyNames() {
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
     * method invokes {@link #computeProperty()} and caches the result.
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
                    if (result == Image.UndefinedProperty) try {
                        result = computeProperty();
                    } catch (Exception e) {
                        result = null;
                        if (errors != null) {
                            errors.getThrown().addSuppressed(e);
                        } else {
                            /*
                             * Stores the given exception in a log record. We use a log record in order to initialize
                             * the timestamp and thread ID to the values they had at the time the first error occurred.
                             */
                            final LogRecord record = Errors.getResources((Locale) null).getLogRecord(
                                                        Level.WARNING, Errors.Keys.CanNotCompute_1, cn);
                            record.setThrown(e);
                            setError(record);
                        }
                    }
                    return isProperty ? result : errors;
                }
            }
        }
        return source.getProperty(name);
    }

    /**
     * Invoked by {@link TileOpExecutor} if an error occurred while processing tiles.
     * Can also be invoked by {@link #getProperty(String)} directly.
     * This method shall be invoked at most once.
     *
     * @param  record  a description of the error that occurred.
     */
    private synchronized void setError(final LogRecord record) {
        if (errors != null) {
            throw new IllegalStateException();      // Should never happen.
        }
        /*
         * Complete record with source identification as if the error occurred from
         * above `getProperty(String)` method (this is always the case, indirectly).
         */
        record.setSourceClassName(AnnotatedImage.class.getCanonicalName());
        record.setSourceMethodName("getProperty");
        record.setLoggerName(Modules.RASTER);
        errors = record;
    }

    /**
     * Invoked when the property needs to be computed. If the property can not be computed,
     * then the result will be {@code null} and the exception thrown by this method will be
     * wrapped in a property of the same name with the {@value #ERRORS_SUFFIX} suffix.
     *
     * <p>The default implementation makes the following choice:</p>
     * <ul class="verbose">
     *   <li>If {@link #parallel} is {@code true} and the {@linkplain #getAreaOfInterest() area of interest}
     *       covers at least two tiles and {@link #collector()} returned a non-null value, then this method
     *       distributes the calculation of many threads using the functions provided by the collector.
     *       See {@link #collector()} Javadoc for more information.</li>
     *   <li>Otherwise this method delegates to {@link #computeSequentially()}.</li>
     * </ul>
     *
     * @return the property value (may be {@code null}).
     * @throws Exception if an error occurred while computing the property.
     */
    protected Object computeProperty() throws Exception {
        if (parallel) {
            final TileOpExecutor executor = new TileOpExecutor(source, getAreaOfInterest());
            if (executor.isMultiTiled()) {
                final Collector<? super Raster,?,?> collector = collector();
                if (collector != null) {
                    return executor.executeOnReadable(source, collector(), this::setError);
                }
            }
        }
        return computeSequentially();
    }

    /**
     * Invoked when the property needs to be computed sequentially (all computations in current thread).
     * If the property can not be computed, then the result will be {@code null} and the exception thrown
     * by this method will be wrapped in a property of the same name with the {@value #ERRORS_SUFFIX} suffix.
     *
     * <p>This method is invoked when this class does not support parallel execution ({@link #collector()}
     * returned {@code null}), or when it is not worth to parallelize (image has only one tile), or when
     * the {@linkplain #source} image may be non-thread safe ({@link #parallel} is {@code false}).</p>
     *
     * @return the property value (may be {@code null}).
     * @throws Exception if an error occurred while computing the property.
     */
    protected abstract Object computeSequentially() throws Exception;

    /**
     * Returns the function to execute for computing the property value, together with other required functions
     * (supplier of accumulator, combiner, finisher). Those functions allow multi-threaded property calculation.
     * This collector is used in a way similar to {@link java.util.stream.Stream#collect(Collector)}. A typical
     * approach is two define 3 private methods in the subclass as below (where <var>P</var> is the type of the
     * property to compute):
     *
     * {@preformat java
     *     private P createAccumulator() {
     *         // Create an object holding the information to be computed by a single thread.
     *         // This is invoked for each worker thread before the worker starts its execution.
     *     }
     *
     *     private static P combine(P previous, P computed) {
     *         // Invoked after a thread finished to process all its tiles and
     *         // wants to combine its result with the result of another thread.
     *     }
     *
     *     private static void compute(P accumulator, Raster tile) {
     *         // Perform the actual computation using one tile and update the accumulator with the result.
     *         // The accumulator may already contain data, which need to be augmented (not overwritten).
     *     }
     *
     *     &#64;Override
     *     protected Collector<Raster,P,P> collector() {
     *         return Collector.of(this::createAccumulator, MyClass::compute, MyClass::combine);
     *     }
     * }
     *
     * @return functions for multi-threaded computation of property value, or {@code null} if unsupported.
     */
    protected Collector<? super Raster, ?, ?> collector() {
        return null;
    }

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

    /**
     * Returns a string representation of this image for debugging purpose.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(100).append(AnnotatedImage.class.getSimpleName()).append('[');
        if (result != Image.UndefinedProperty) {
            buffer.append("Cached ");
        }
        return buffer.append("[\"").append(getComputedPropertyName()).append("\" on ").append(source).append(']').toString();
    }
}
