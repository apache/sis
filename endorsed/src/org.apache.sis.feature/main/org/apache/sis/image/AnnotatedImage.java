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

import java.util.Arrays;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collector;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.ImagingOpException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.image.internal.shared.TileOpExecutor;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.util.internal.shared.Strings;


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
 * <h2>Design note</h2>
 * Most non-abstract methods are final because {@link PixelIterator} (among others) relies
 * on the fact that it can unwrap this image and still get the same pixel values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class AnnotatedImage extends ImageAdapter {
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
    private static final WeakHashMap<RenderedImage, Cache<Object, Object>> CACHE = new WeakHashMap<>();

    /**
     * Cache of property values computed for the {@linkplain #source} image. This is an entry from the
     * global {@link #CACHE}. This cache is shared by all {@link AnnotatedImage} instances wrapping the
     * same {@linkplain #source} image in order to avoid computing the same property many times if an
     * {@code AnnotatedImage} wrapper is recreated many times for the same operation on the same image.
     *
     * <p>Note that {@code null} is a valid result. Since {@link Cache} cannot store null values,
     * those results are replaced by {@link #NULL}.</p>
     *
     * <p>Keys are {@link String} instances containing directly the property name when {@link #areaOfInterest}
     * and {@link #getExtraParameter()} are {@code null}, or {@link CacheKey} instances otherwise.</p>
     */
    private final Cache<Object, Object> cache;

    /**
     * Keys in the {@link AnnotatedImage#cache} when {@link AnnotatedImage#areaOfInterest} is non-null.
     */
    private static final class CacheKey {
        /** The property name (never null). */
        private final String property;

        /** The area of interest, or null if none. */
        private final Shape areaOfInterest;

        /** Parameter specific to subclass, or null if none. */
        private final Object[] extraParameter;

        /** Creates a new key for the given property and AOI. */
        CacheKey(final String property, final Shape areaOfInterest, final Object[] extraParameter) {
            this.property       = property;
            this.areaOfInterest = areaOfInterest;
            this.extraParameter = extraParameter;
        }

        /** Returns a hash code value for this key. */
        @Override public int hashCode() {
            return property.hashCode()
                    + 19 * Objects.hashCode(areaOfInterest)
                    + 37 *  Arrays.hashCode(extraParameter);

        }

        /** Compares this key with the given object for equality. */
        @Override public boolean equals(final Object obj) {
            if (obj instanceof CacheKey) {
                final CacheKey other = (CacheKey) obj;
                return property.equals(other.property)
                        && Objects.equals(areaOfInterest, other.areaOfInterest)
                        &&  Arrays.equals(extraParameter, other.extraParameter);
            }
            return false;
        }

        /** Returns a string representation of this key for debugging purpose. */
        @Override public String toString() {
            return Strings.toString(getClass(), "property", property, "areaOfInterest", areaOfInterest);
        }
    }

    /**
     * Pixel coordinates of the region for which to compute the values, or {@code null} for the whole image.
     * If non-null, the {@link Shape#contains(double, double)} method may be invoked for testing if a pixel
     * shall be included in the computation or not.
     *
     * <p>This shape should not be modified, either by this class or by the caller who provided the shape.
     * The {@code Shape} implementation shall be thread-safe, assuming its state stay unmodified, unless
     * the {@link #parallel} argument specified to the constructor was {@code false}.</p>
     *
     * <p>If {@code areaOfInterest} is {@code null}, then {@link #boundsOfInterest} is always {@code null}.
     * However, the converse is not necessarily true.</p>
     */
    protected final Shape areaOfInterest;

    /**
     * Bounds of {@link #areaOfInterest} intersected with image bounds, or {@code null} for the whole image.
     * If the area of interest fully contains those bounds, then {@link #areaOfInterest} is set to the same
     * reference than {@code boundsOfInterest}. Subclasses can use {@code areaOfInterest == boundsOfInterest}
     * for quickly testing if the area of interest is rectangular.
     *
     * <p>If {@link #areaOfInterest} is {@code null}, then {@code boundsOfInterest} is always {@code null}.
     * However, the converse is not necessarily true.</p>
     */
    protected final Rectangle boundsOfInterest;

    /**
     * The errors that occurred while computing the result, or {@code null} if none or not yet determined.
     * This field is never set if {@link #failOnException} is {@code true}.
     */
    private volatile ErrorHandler.Report errors;

    /**
     * Whether parallel execution is authorized for the {@linkplain #source} image.
     * If {@code true}, then {@link RenderedImage#getTile(int, int)} implementation should be concurrent.
     */
    private final boolean parallel;

    /**
     * Whether errors occurring during computation should be propagated instead of wrapped in a {@link LogRecord}.
     */
    private final boolean failOnException;

    /**
     * Creates a new annotated image wrapping the given image.
     * The annotations are the additional properties computed by the subclass.
     *
     * @param  source           the image to wrap for adding properties (annotations).
     * @param  areaOfInterest   pixel coordinates of AOI, or {@code null} for the whole image.
     * @param  parallel         whether parallel execution is authorized.
     * @param  failOnException  whether errors occurring during computation should be propagated.
     */
    protected AnnotatedImage(RenderedImage source, Shape areaOfInterest,
                             final boolean parallel, final boolean failOnException)
    {
        super(source);
        Rectangle bounds = null;
        if (areaOfInterest != null) {
            bounds = areaOfInterest.getBounds();
            ImageUtilities.clipBounds(source, bounds);
            if (bounds.isEmpty()) {
                bounds.x = getMinX();
                bounds.y = getMinY();
                bounds.width  = 0;
                bounds.height = 0;
            }
            if (areaOfInterest.contains(bounds)) {
                areaOfInterest = bounds;
            }
            /*
             * If the rectangle contains the full image, replace them by a null value.
             * It allows optimizations (avoid the need to check for point inclusion)
             * and allows the cache to detect that a value already exist.
             */
            if (bounds.x == getMinX() && bounds.width  == getWidth() &&
                bounds.y == getMinY() && bounds.height == getHeight())
            {
                if (bounds == areaOfInterest) {
                    areaOfInterest = null;
                }
                bounds = null;
            }
        }
        this.boundsOfInterest = bounds;
        this.areaOfInterest   = areaOfInterest;
        this.parallel         = parallel;
        this.failOnException  = failOnException;
        /*
         * The `this.source` field should be as specified, even if it is another `AnnotatedImage`,
         * for allowing computation of properties managed by those other instances. However, we try
         * to apply the cache on a deeper source if possible, for increasing the chances that the
         * cache is shared by all images using the same data. This is okay if calculation depends
         * only on sample value, not on other data.
         */
        while (source instanceof ImageAdapter) {
            if (source instanceof AnnotatedImage) {
                cache = ((AnnotatedImage) source).cache;        // Cache for the source of the source.
                return;
            }
            source = ((ImageAdapter) source).source;
        }
        synchronized (CACHE) {
            cache = CACHE.computeIfAbsent(source, (k) -> new Cache<>(8, 200, false));
        }
    }

    /**
     * Returns an optional parameter specific to subclass. This is used for caching purpose
     * and for {@link #equals(Object)} and {@link #hashCode()} method implementations only,
     * i.e. for distinguishing between two {@code AnnotatedImage} instances that are identical
     * except for subclass-defined parameters.
     *
     * <h4>API note</h4>
     * The return value is an array because there is typically one parameter value per band.
     * This method will not modify the returned array.
     *
     * @return subclass specific extra parameter, or {@code null} if none.
     */
    Object[] getExtraParameter() {
        return null;
    }

    /**
     * If the source image is the same operation for the same area of interest, returns that source.
     * Otherwise returns {@code this} or a previous instance doing the same operation as {@code this}.
     *
     * @see #equals(Object)
     */
    final RenderedImage unique() {
        if (source.getClass() == getClass() && equalParameters((AnnotatedImage) source)) {
            return source;
        } else {
            return ImageProcessor.unique(this);
        }
    }

    /**
     * Returns the key to use for entries in the {@link #cache} map.
     *
     * @param  property  value of {@link #getPropertyNames()}.
     */
    private Object getCacheKey(final String property) {
        final Object[] extraParameter = getExtraParameter();
        return (areaOfInterest != null || extraParameter != null)
                ? new CacheKey(property, areaOfInterest, extraParameter) : property;
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
        final boolean hasErrors = (errors != null);
        final String[] names = new String[hasErrors ? 2 : 1];
        names[0] = getComputedPropertyName();
        if (hasErrors) {
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
     * method invokes {@link #computeProperty()} and caches its result.
     *
     * @param  name  name of the property to get.
     * @return the property for the given name ({@code null} is a valid result),
     *         or {@link Image#UndefinedProperty} if the given name is not a recognized property name.
     */
    @Override
    public final Object getProperty(final String name) {
        Object value;
        final String property = getComputedPropertyName();
        if (property.equals(name)) {
            /*
             * Get the previously computed value. Note that the value may have been computed by another
             * `AnnotatedImage` instance of the same class wrapping the same image, which is why we do
             * not store the result in this class.
             */
            final Object key = getCacheKey(property);
            value = cache.peek(key);
            if (value == null) {
                boolean success = false;
                final Cache.Handler<Object> handler = cache.lock(key);
                try {
                    value = handler.peek();
                    if (value == null) try {
                        value = computeProperty();
                        if (value == null) value = NULL;
                        success = (errors == null);
                    } catch (Exception e) {
                        if (failOnException) {
                            throw (ImagingOpException) new ImagingOpException(
                                    Errors.format(Errors.Keys.CanNotCompute_1, property)).initCause(e);
                        }
                        /*
                         * Stores the exception in a log record. We use a log record in order to initialize
                         * the timestamp and thread ID to the values they had at the time the error occurred.
                         * We do not synchronize because all worker threads should have finished now.
                         */
                        ErrorHandler.Report report = errors;
                        if (report == null) {
                            errors = report = new ErrorHandler.Report();
                        }
                        report.add(null, e, () -> Errors.forLocale(null)
                                .createLogRecord(Level.WARNING, Errors.Keys.CanNotCompute_1, property));
                    }
                } finally {
                    handler.putAndUnlock(success ? value : null);       // Cache only if no error occurred.
                }
                if (value == NULL) value = null;
                else value = cloneProperty(property, value);
            }
        } else if (isErrorProperty(property, name)) {
            value = errors;
        } else {
            value = source.getProperty(name);
        }
        return value;
    }

    /**
     * If an error occurred, logs the message with the specified class and method as the source.
     * The {@code classe} and {@code method} arguments overwrite the {@link LogRecord#getSourceClassName()}
     * and {@link LogRecord#getSourceMethodName()} values. The log record is cleared by this method call
     * and will no longer be reported, unless the property is recomputed.
     *
     * <h4>Context of use</h4>
     * This method should be invoked only on images that are going to be disposed after the caller extracted
     * the computed property value. This method should not be invoked on image accessible by the user,
     * because clearing the error may be surprising.
     *
     * @param  classe   the class to report as the source of the logging message.
     * @param  method   the method to report as the source of the logging message.
     * @param  handler  where to send the log message.
     */
    final void logAndClearError(final Class<?> classe, final String method, final ErrorHandler handler) {
        final ErrorHandler.Report report = errors;
        if (report != null) {
            synchronized (report) {
                final LogRecord record = report.getDescription();
                record.setSourceClassName(classe.getCanonicalName());
                record.setSourceMethodName(method);
                errors = null;
            }
            handler.handle(report);
        }
    }

    /**
     * Invoked when the property needs to be computed. If the property cannot be computed,
     * then the result will be {@code null} and the exception thrown by this method will be
     * wrapped in a property of the same name with the {@value #WARNINGS_SUFFIX} suffix.
     *
     * <p>The default implementation makes the following choice:</p>
     * <ul class="verbose">
     *   <li>If {@link #parallel} is {@code true}, {@link #collector()} returns a non-null value
     *       and the area of interest covers at least two tiles, then this method distributes
     *       calculation on many threads using the functions provided by the collector.
     *       See {@link #collector()} Javadoc for more information.</li>
     *   <li>Otherwise this method delegates to {@link #computeSequentially()}.</li>
     * </ul>
     *
     * @return the computed property value. Note that {@code null} is a valid result.
     * @throws Exception if an error occurred while computing the property.
     */
    protected Object computeProperty() throws Exception {
        if (parallel) {
            final TileOpExecutor executor = new TileOpExecutor(source, boundsOfInterest);
            if (executor.isMultiTiled()) {
                final Collector<? super Raster,?,?> collector = collector();
                if (collector != null) {
                    if (!failOnException) {
                        executor.setErrorHandler((e) -> errors = e, AnnotatedImage.class, "getProperty");
                    }
                    executor.setAreaOfInterest(source, areaOfInterest);
                    return executor.executeOnReadable(source, collector);
                }
            }
        }
        return computeSequentially();
    }

    /**
     * Invoked when the property needs to be computed sequentially (all computations in current thread).
     * If the property cannot be computed, then the result will be {@code null} and the exception thrown
     * by this method will be wrapped in a property of the same name with the {@value #WARNINGS_SUFFIX} suffix.
     *
     * <p>This method is invoked when this class does not support parallel execution ({@link #collector()}
     * returned {@code null}), or when it is not worth to parallelize (image has only one tile), or when
     * the {@linkplain #source} image may be non-thread safe ({@link #parallel} is {@code false}).</p>
     *
     * @return the computed property value. Note that {@code null} is a valid result.
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
     * {@snippet lang="java" :
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
     *     @Override
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

    /**
     * Appends the name of the computed property in the {@link #toString()} representation,
     * after the class name and before the string representation of the wrapped image.
     */
    @Override
    final Class<AnnotatedImage> appendStringContent(final StringBuilder buffer) {
        final String property = getComputedPropertyName();
        if (cache.containsKey(getCacheKey(property))) {
            buffer.append("Cached ");
        }
        buffer.append('"').append(property).append('"');
        return AnnotatedImage.class;
    }

    /**
     * Returns a hash code value for this image. This method should be quick;
     * it should not compute the hash code from sample values.
     *
     * @return a hash code value based on a description of the operation performed by this image.
     */
    @Override
    public final int hashCode() {
        return super.hashCode() + Objects.hashCode(areaOfInterest) + Boolean.hashCode(failOnException);
    }

    /**
     * Compares the given object with this image for equality. This method should be quick and compare
     * how images compute their values from their sources; it should not compare the actual pixel values.
     *
     * <h4>Requirements for subclasses</h4>
     * Subclasses should override {@link #getExtraParameter()} instead of this method.
     *
     * @param  object  the object to compare with this image.
     * @return {@code true} if the given object is an image performing the same calculation as this image.
     *
     * @see #getExtraParameter()
     */
    @Override
    public final boolean equals(final Object object) {
        return super.equals(object) && equalParameters((AnnotatedImage) object);
    }

    /**
     * Returns {@code true} if the area of interest and some other fields are equal.
     * The {@link #boundsOfInterest} is omitted because it is derived from {@link #areaOfInterest}.
     * The {@link #errors} is omitted because it is part of computation results.
     */
    private boolean equalParameters(final AnnotatedImage other) {
        return parallel == other.parallel && failOnException == other.failOnException
                && Objects.equals(areaOfInterest,      other.areaOfInterest)
                &&  Arrays.equals(getExtraParameter(), other.getExtraParameter());
    }
}
