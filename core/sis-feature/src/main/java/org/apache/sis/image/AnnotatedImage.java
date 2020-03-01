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
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Filter;
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
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.coverage.j2d.TileOpExecutor;


/**
 * An image which wraps an existing image unchanged, except for properties which are computed
 * on the fly when first requested. All {@link RenderedImage} methods delegate to the wrapped
 * image except {@link #getSources()} and the methods for getting the property names or values.
 *
 * <p>The name of the computed property is given by {@link #getComputedPropertyName()}.
 * If an exception is thrown during calculation and {@link #failOnException} is {@code false},
 * then {@code AnnotatedImage} automatically creates another property with the same name and
 * {@value #WARNINGS_SUFFIX} suffix. That property will contain the exception encapsulated
 * in a {@link LogRecord} in order to retain additional information such as the instant when
 * the first error occurred.</p>
 *
 * <p>The computation results are cached by this class. The cache strategy assumes that the
 * property value depend only on sample values, not on properties of the source image.</p>
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
     * A property with suffix is automatically created if an exception is thrown during
     * computation and {@link #failOnException} is {@code false}.
     */
    public static final String WARNINGS_SUFFIX = ".warnings";

    /**
     * An arbitrary value that we use for storing {@code null} values in the {@linkplain #cache}.
     */
    private static final Object NULL = Void.TYPE;

    /**
     * Cache of properties already computed for images. That map shall contain computation results only,
     * never the {@link AnnotatedImage} instances that computed those results, as doing so would create
     * memory leak (because of the {@link #source} reference preventing the key to be garbage-collected).
     * All accesses to this cache shall be synchronized on the {@code CACHE} instance.
     *
     * <p>In current implementation we cache only the values that have been computed without warnings.
     * We do that because otherwise, an {@code AnnotatedImage} with {@link #failOnException} flag set
     * could wrongly return a partially computed value if that value has been cached by another image
     * instance with the {@link #failOnException} flag unset. As a consequence of this policy, if the
     * computation failed for some tiles, that computation will be redone again for the same property
     * every time it is requested, until it eventually fully succeeds and the result become cached.</p>
     */
    private static final WeakHashMap<RenderedImage, Cache<String,Object>> CACHE = new WeakHashMap<>();

    /**
     * Cache of property values computed for the {@linkplain #source} image. This is an entry from the
     * global {@link #CACHE}. This cache is shared by all {@link AnnotatedImage} instances wrapping the
     * same {@linkplain #source} image in order to avoid computing the same property many times if an
     * {@code AnnotatedImage} wrapper is recreated many times for the same operation on the same image.
     *
     * <p>Note that {@code null} is a valid result. Since {@link Cache} can not store null values,
     * those results are replaced by {@link #NULL}.</p>
     */
    private final Cache<String,Object> cache;

    /**
     * The source image from which to compute the property.
     */
    protected final RenderedImage source;

    /**
     * The errors that occurred while computing the result, or {@code null} if none or not yet determined.
     * This field is never set if {@link #failOnException} is {@code true}.
     */
    private volatile LogRecord errors;

    /**
     * Whether parallel execution is authorized for the {@linkplain #source} image.
     * If {@code true}, then {@link RenderedImage#getTile(int, int)} implementation should be concurrent.
     */
    private final boolean parallel;

    /**
     * Whether errors occurring during computation should be propagated instead than wrapped in a {@link LogRecord}.
     */
    private final boolean failOnException;

    /**
     * Creates a new annotated image wrapping the given image.
     * The annotations are the additional properties computed by the subclass.
     *
     * @param  source           the image to wrap for adding properties (annotations).
     * @param  parallel         whether parallel execution is authorized.
     * @param  failOnException  whether errors occurring during computation should be propagated.
     */
    protected AnnotatedImage(final RenderedImage source, final boolean parallel, final boolean failOnException) {
        this.source          = source;
        this.parallel        = parallel;
        this.failOnException = failOnException;
        /*
         * The `this.source` field should be as specified, even if it is another `AnnotatedImage`,
         * for allowing computation of properties managed by those other instances. However we try
         * to apply the cache on a deeper source if possible, for increasing the chances that the
         * cache is shared by all images using the same data. This is okay if calculation depends
         * only on sample value, not on other data.
         */
        if (source instanceof AnnotatedImage) {
            cache = ((AnnotatedImage) source).cache;        // Cache for the source of the source.
        } else synchronized (CACHE) {
            cache = CACHE.computeIfAbsent(source, (k) -> new Cache<>(8, 1000, true));
        }
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
     * Returns the name of the property which is computed by this image.
     *
     * @return name of property computed by this image. Shall not be null.
     */
    protected abstract String getComputedPropertyName();

    /**
     * Returns an array of names recognized by {@link #getProperty(String)}.
     * The default implementation returns the {@linkplain #source} properties names
     * followed by {@link #getComputedPropertyName()}. If that property has already
     * been computed and an error occurred, then the names returned by this method
     * will include the property name with {@value #WARNINGS_SUFFIX} suffix.
     *
     * @return all recognized property names.
     */
    @Override
    public String[] getPropertyNames() {
        final String[] names = new String[(errors != null) ? 2 : 1];
        names[0] = getComputedPropertyName();
        if (errors != null) {
            names[1] = names[0] + WARNINGS_SUFFIX;
        }
        return ArraysExt.concatenate(source.getPropertyNames(), names);
    }

    /**
     * Returns whether the given name is the name of the error property.
     * The implementation of this method avoids the creation of concatenated string.
     *
     * @param  cn    name of the computed property.
     * @param  name  the property name to test (may be {@code null}).
     * @return whether {@code name} is {@code cn} + {@value #WARNINGS_SUFFIX}.
     */
    private static boolean isErrorProperty(final String cn, final String name) {
        return name != null && name.length() == cn.length() + WARNINGS_SUFFIX.length()
                            && name.startsWith(cn) && name.endsWith(WARNINGS_SUFFIX);
    }

    /**
     * Gets a property from this image or from its source. If the given name is for the property
     * to be computed by this class and if that property has not been computed before, then this
     * method invokes {@link #computeProperty(Rectangle)} with a {@code null} "area of interest"
     * argument value. That {@code computeProperty(…)} result will be cached.
     *
     * @param  name  name of the property to get.
     * @return the property for the given name ({@code null} is a valid result),
     *         or {@link Image#UndefinedProperty} if the given name is not a recognized property name.
     */
    @Override
    public final Object getProperty(final String name) {
        Object value;
        final String key = getComputedPropertyName();
        if (key.equals(name)) {
            /*
             * Get the previously computed value. Note that the value may have been computed by another
             * `AnnotatedImage` instance of the same class wrapping the same image, which is why we do
             * not store the result in this class.
             */
            value = cache.peek(key);
            if (value == null) try {
                boolean success = false;
                final Cache.Handler<Object> handler = cache.lock(key);
                try {
                    value = handler.peek();
                    if (value == null) {
                        value = computeProperty(null);
                        if (value == null) value = NULL;
                        success = (errors == null);
                    }
                } finally {
                    handler.putAndUnlock(success ? value : null);       // Cache only if no error occurred.
                }
                if (value == NULL) value = null;
                else value = cloneProperty(key, value);
            } catch (Exception e) {
                /*
                 * Stores the given exception in a log record. We use a log record in order to initialize
                 * the timestamp and thread ID to the values they had at the time the first error occurred.
                 */
                if (failOnException) {
                    throw (ImagingOpException) new ImagingOpException(
                            Errors.format(Errors.Keys.CanNotCompute_1, key)).initCause(e);
                }
                synchronized (this) {
                    LogRecord record = errors;
                    if (record != null) {
                        record.getThrown().addSuppressed(e);
                    } else {
                        record = Errors.getResources((Locale) null).getLogRecord(Level.WARNING, Errors.Keys.CanNotCompute_1, key);
                        record.setThrown(e);
                        setError(record);
                    }
                }
            }
        } else if (isErrorProperty(key, name)) {
            value = errors;
        } else {
            value = source.getProperty(name);
        }
        return value;
    }

    /**
     * Invoked by {@link TileOpExecutor} if an error occurred during calculation on a tiles.
     * Can also be invoked by {@link #getProperty(String)} directly if the error occurred
     * outside {@link TileOpExecutor}. This method shall be invoked at most once.
     *
     * @param  record  a description of the error that occurred.
     */
    private void setError(final LogRecord record) {
        /*
         * Complete record with source identification as if the error occurred from
         * above `getProperty(String)` method (this is always the case, indirectly).
         */
        record.setSourceClassName(AnnotatedImage.class.getCanonicalName());
        record.setSourceMethodName("getProperty");
        record.setLoggerName(Modules.RASTER);
        synchronized (this) {
            if (errors == null) {
                errors = record;
            } else {
                throw new IllegalStateException();      // If it happens, this is a bug in thie AnnotatedImage class.
            }
        }
    }

    /**
     * If an error occurred, logs the message. The log record is cleared by this method call
     * and will no longer be reported, unless the property is recomputed.
     *
     * @param  classe   the class to report as the source of the logging message.
     * @param  method   the method to report as the source of the logging message.
     * @param  handler  where to send the log message, or {@code null} for the standard logger.
     */
    final void logAndClearError(final Class<?> classe, final String method, final Filter handler) {
        final LogRecord record;
        synchronized (this) {
            record = errors;
            errors = null;
        }
        if (record != null) {
            if (handler == null || handler.isLoggable(record)) {
                Logging.log(classe, method, record);
            }
        }
    }

    /**
     * Invoked when the property needs to be computed. If the property can not be computed,
     * then the result will be {@code null} and the exception thrown by this method will be
     * wrapped in a property of the same name with the {@value #WARNINGS_SUFFIX} suffix.
     *
     * <p>The default implementation makes the following choice:</p>
     * <ul class="verbose">
     *   <li>If {@link #parallel} is {@code true}, {@link #collector()} returns a non-null value
     *       and the area of interest covers at least two tiles, then this method distributes
     *       calculation on many threads using the functions provided by the collector.
     *       See {@link #collector()} Javadoc for more information.</li>
     *   <li>Otherwise this method delegates to {@link #computeSequentially(Rectangle)}.</li>
     * </ul>
     *
     * The {@code areaOfInterest} argument is {@code null} by default, which means to calculate
     * the property on all tiles. This argument exists for allowing subclasses to override this
     * method and invoke {@code super.computeProperty(…)} with a sub-region to compute.
     *
     * @param  areaOfInterest  pixel coordinates of the region of interest, or {@code null} for the whole image.
     * @return the computed property value. Note that {@code null} is a valid result.
     * @throws Exception if an error occurred while computing the property.
     */
    protected Object computeProperty(final Rectangle areaOfInterest) throws Exception {
        if (parallel) {
            final TileOpExecutor executor = new TileOpExecutor(source, areaOfInterest);
            if (executor.isMultiTiled()) {
                final Collector<? super Raster,?,?> collector = collector();
                if (collector != null) {
                    return executor.executeOnReadable(source, collector(), failOnException ? null : this::setError);
                }
            }
        }
        return computeSequentially(areaOfInterest);
    }

    /**
     * Invoked when the property needs to be computed sequentially (all computations in current thread).
     * If the property can not be computed, then the result will be {@code null} and the exception thrown
     * by this method will be wrapped in a property of the same name with the {@value #WARNINGS_SUFFIX} suffix.
     *
     * <p>This method is invoked when this class does not support parallel execution ({@link #collector()}
     * returned {@code null}), or when it is not worth to parallelize (image has only one tile), or when
     * the {@linkplain #source} image may be non-thread safe ({@link #parallel} is {@code false}).</p>
     *
     * @param  areaOfInterest  pixel coordinates of the region of interest, or {@code null} for the whole image.
     *         This is the argument given to {@link #computeProperty(Rectangle)} and can usually be ignored
     *         (because always {@code null}) if that method has not been overridden.
     * @return the computed property value. Note that {@code null} is a valid result.
     * @throws Exception if an error occurred while computing the property.
     */
    protected abstract Object computeSequentially(Rectangle areaOfInterest) throws Exception;

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

    /**
     * Invoked when a property of the given name has been requested and that property is cached.
     * If the property is mutable, subclasses may want to clone it before to return it to users.
     * The default implementation returns {@code value} unchanged.
     *
     * @param  name   the property name.
     * @param  value  the property value (never {@code null}).
     * @return the property value to give to user.
     */
    protected Object cloneProperty(final String name, final Object value) {
        return value;
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
        if (cache.containsKey(getComputedPropertyName())) {
            buffer.append("Cached ");
        }
        return buffer.append("[\"").append(getComputedPropertyName()).append("\" on ").append(source).append(']').toString();
    }
}
