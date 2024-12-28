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
package org.apache.sis.storage.gdal;

import java.util.Set;
import java.util.HashSet;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Array;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ConcurrentReadException;
import org.apache.sis.geometry.wrapper.Capability;
import org.apache.sis.geometry.wrapper.Dimensions;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.storage.panama.Resources;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;


/**
 * Iterator over the features returned by <abbr>OGR</abbr>.
 * The current implementation supports only sequential accesses.
 * All usages of this iterator must be done in the same thread.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class FeatureIterator implements Spliterator<Feature>, Runnable {
    /**
     * {@code OGRwkbGeometryType} enumeration value specific to <abbr>GDAL</abbr>.
     */
    private static final int LINEAR_RING = 101;

    /**
     * The source of features. This object provides the feature type,
     * the fields to read, the synchronization lock and the native functions.
     */
    private final FeatureLayer layer;

    /**
     * The number of elements, or -1 if too expansive to compute.
     */
    private final long count;

    /**
     * The arena used for allocating the buffers in this class, or {@code null} if none (because not needed).
     */
    private Arena arena;

    /**
     * A buffer where some <abbr>GDAL</abbr> functions will write their outputs, or {@code null} if not needed.
     */
    private MemorySegment buffer;

    /**
     * Buffers for extracting the date components of a field, or {@code null} if no date is expected.
     * There is no {@code year} component because {@link #buffer} can be used directly for the years.
     *
     * <h4>Mapping to geometry coordinates</h4>
     * The same buffer and the same slices are opportunistically used for geometry coordinates as below.
     * This mapping relies on the fact that {@link Double#BYTES} is twice the value of {@link Integer#BYTES}.
     * Therefore, we start with the buffer, skip one slice, take the next slice, skip one slice, <i>etc.</i>
     *
     * <ol>
     *   <li><var>x</var>: {@link #buffer}</li>
     *   <li><var>y</var>: {@link #day}</li>
     *   <li><var>z</var>: {@link #minute}</li>
     *   <li><var>m</var>: {@link #timezone}</li>
     * </ol>
     *
     * @see FieldAccessor#NUM_DATE_COMPONENTS
     */
   private MemorySegment month, day, hour, minute, second, timezone;

    /**
     * Unsupported types encountered during the iteration.
     * Used for avoiding to emit the same warning more than once.
     */
    private final Set<String> unsupportedTypes;

    /**
     * Whether the iterator has been initialized. After initialization, no other iteration
     * can be executed on the same {@link FeatureLayer} until {@link #run()} is executed.
     */
    private boolean initialized;

    /**
     * Creates a new iterator for the given layer.
     *
     * @param  layer  the source of features.
     * @throws Throwable if an error occurred while estimating the number of features.
     */
    FeatureIterator(final FeatureLayer layer) throws DataStoreException {
        this.layer = layer;
        final OGR ogr = layer.OGR();
        try {
            count = (long) ogr.getFeatureCount.invokeExact(layer.handle, 0);
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        unsupportedTypes = new HashSet<>();
    }

    /**
     * Allocates the buffer if needed (if at least one field is a date/time, an array or a geometry).
     * Tries to allocate only the required amount of memory. In the case of geometries, that amount is a guess.
     * This method shall be invoked after construction. It is not part of the constructor for allowing the call
     * to {@link #run()} if an exception it thrown.
     *
     * <p>This method must be invoked in a block synchronized on {@code layer.store}.</p>
     *
     * @throws ConcurrentReadException if another iteration is already in progress.
     * @throws Throwable if an error occurred while reseting the stream.
     */
    private void initialize(final OGR ogr) throws Throwable {
        assert Thread.holdsLock(layer.store);
        if (layer.iterationInProgress != null) {
            throw new ConcurrentReadException(Resources.forLocale(layer.store.getLocale())
                    .getString(Resources.Keys.UnsupportedConcurrentIteration));
        }
        initialized = true;     // Set now for making sure that this method is not executed twice.
        int bufferSize = 0;
        for (FieldAccessor<?> field : layer.fields) {
            bufferSize = Math.max(bufferSize, field.bufferSize());
        }
        if (bufferSize >= Integer.BYTES) {
            arena  = layer.store.changeArena(arena, true);
            buffer = arena.allocate(bufferSize);
            sliceBuffers();
        }
        layer.iterationInProgress = this;
        ogr.resetReading.invoke(layer.handle);          // Must be done after `getFeatureCount`.
    }

    /**
     * Creates the memory segments for the coordinates and date components.
     * This method needs to be invoked every times that {@link #buffer} changed.
     */
    private void sliceBuffers() {
        if (buffer.byteSize() > Integer.BYTES) {
            for (int i=1; ; i++) {
                final MemorySegment s = buffer.asSlice(i * Integer.BYTES, 1);
                switch (i) {
                    default: throw new AssertionError(i);
                    case 1:  month    = s; break;
                    case 2:  day      = s; break;   // Also used for Y coordinate values.
                    case 3:  hour     = s; break;
                    case 4:  minute   = s; break;   // Also used for Z coordinate values.
                    case 5:  second   = s; break;
                    case 6:  timezone = s; return;  // Also used for M coordinate values.
                }
            }
        }
    }

    /**
     * Do not split the iterator, because this implementation requires sequential accesses.
     */
    @Override
    public Spliterator<Feature> trySplit() {
        return null;
    }

    /**
     * Returns the number of features if known, or {@link Long#MAX_VALUE} otherwise.
     */
    @Override
    public long estimateSize() {
        return (count >= 0) ? count : Long.MAX_VALUE;
    }

    /**
     * Returns the number of features if known, or {@code -1} otherwise.
     */
    @Override
    public long getExactSizeIfKnown() {
        return count;
    }

    /**
     * Specifies that this iterator never returns null elements.
     * May also specify that the number of features is known.
     */
    @Override
    public int characteristics() {
        return (count >= 0) ? SIZED | NONNULL : NONNULL;
    }

    /**
     * Fetches the next feature instance.
     *
     * @param  action  the action to perform with the next feature.
     * @return whether a feature instance has been found.
     */
    @Override
    public boolean tryAdvance(final Consumer<? super Feature> action) {
        try {
            synchronized (layer.store) {
                final OGR ogr = layer.OGR();
                if (!initialized) {
                    initialize(ogr);
                }
                return advance(ogr, action);
            }
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
    }

    /**
     * Executes the given action on all remaining feature instances.
     *
     * @param action the action to perform on all remaining features.
     */
    @Override
    @SuppressWarnings("empty-statement")
    public void forEachRemaining(final Consumer<? super Feature> action) {
        try {
            synchronized (layer.store) {
                final OGR ogr = layer.OGR();
                if (!initialized) {
                    initialize(ogr);
                }
                while (advance(ogr, action));
            }
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
    }

    /**
     * Executes the given action on the next feature instance.
     *
     * @param  ogr     pointers to native functions.
     * @param  action  the action to perform with the next feature.
     * @return whether a feature instance has been found.
     * @throws Throwable if a call to a native method failed.
     */
    private boolean advance(final OGR ogr, final Consumer<? super Feature> action) throws Throwable {
        final MemorySegment handle = (MemorySegment) ogr.getNextFeature.invokeExact(layer.handle);
        if (GDAL.isNull(handle)) {
            return false;
        }
        final Feature feature;
        try {
            feature = layer.type.newInstance();
            for (FieldAccessor<?> field : layer.fields) {
                feature.setPropertyValue(field.name(), field.getValue(this, ogr, handle));
            }
        } finally {
            ogr.destroyFeature.invokeExact(handle);
        }
        action.accept(feature);
        return true;
    }

    /**
     * Returns a buffer where some <abbr>GDAL</abbr> functions will write their outputs.
     * This method shall be invoked only from instances where {@link FieldAccessor#bufferSize()}
     * returned a non-zero value.
     */
    final MemorySegment buffer() {
        return buffer;
    }

    /**
     * Returns whether the field in the given feature has a date.
     * If true, the date components are stored in the temporal buffers of this layer.
     * This method must be invoked in a block synchronized on {@link FeatureLayer#store}.
     *
     * @param  ogr      pointers to native functions.
     * @param  feature  pointer to the {@code OGRFeatureH} instance.
     * @return whether the field has date components in the given feature instance.
     * @throws Throwable if an error occurred during the native method call.
     */
    final boolean hasDate(final OGR ogr, final MemorySegment feature, final int index) throws Throwable {
        return ((int) ogr.getFieldAsDateTime.invokeExact(feature, index,
                buffer, month, day, hour, minute, second, timezone)) != 0;
    }

    /**
     * Creates a local date from the current field values.
     * The {@link #hasDate(OGR, MemorySegment, int)} method must have been invoked before this method.
     *
     * @return the local date of the field checked by the last call to {@link #hasDate(OGR, MemorySegment, int)}. Never null.
     * @throws NullPointerException if the temporal buffers are null (no dates where expected for the feature type).
     */
    final LocalDate date() {
        return LocalDate.of(buffer.get(ValueLayout.JAVA_INT, 0),
                            month .get(ValueLayout.JAVA_INT, 0),
                            day   .get(ValueLayout.JAVA_INT, 0));
    }

    /**
     * Creates a local time from the current field values.
     * The {@link #hasDate(OGR, MemorySegment, int)} method must have been invoked before this method.
     *
     * @return the local time of the field checked by the last call to {@link #hasDate(OGR, MemorySegment, int)}. Never null.
     * @throws NullPointerException if the temporal buffers are null (no dates where expected for the feature type).
     */
    final LocalTime time() {
        int sec;
        float withMillis =  second.get(ValueLayout.JAVA_FLOAT, 0);
        return LocalTime.of(hour  .get(ValueLayout.JAVA_INT, 0),
                            minute.get(ValueLayout.JAVA_INT, 0),
                            sec = (int) withMillis,
                            Math.round((withMillis - sec) * Constants.NANOS_PER_SECOND));
    }

    /**
     * Returns the timezone of current field values, or {@code null} if local or unknown.
     * The {@link #hasDate(OGR, MemorySegment, int)} method must have been invoked before this method.
     *
     * @return the timezone of the field checked by the last call to {@link #hasDate(OGR, MemorySegment, int)}, or null if local.
     * @throws NullPointerException if the temporal buffers are null (no dates where expected for the feature type).
     */
    final ZoneOffset timezone() {
        return timezone.get(ValueLayout.JAVA_INT, 0) == 100 ? ZoneOffset.UTC : null;
    }

    /**
     * Creates the Java geometry object from the given {@code OGRGeometryH}.
     *
     * @param  ogr   pointers to native functions.
     * @param  geom  pointer to the {@code OGRGeometryH} instance.
     * @return the Java geometry object, or {@code null} if none or not supported.
     * @throws ArrayStoreException if the geometry is some kind of collection but contains a child of unexpected type.
     * @throws Throwable if an error occurred during a native method call or Java geometry construction.
     */
    final Object geometry(final OGR ogr, final MemorySegment geom) throws Throwable {
        assert Thread.holdsLock(layer.store);
        final Geometries<?> library = layer.library;
        final int code = (int) ogr.getGeometryType.invokeExact(geom);
        final var type = (code == LINEAR_RING) ? GeometryType.LINESTRING : GeometryType.forBinaryType(code);
        final boolean isPolygon = (type == GeometryType.POLYGON);
        if (isPolygon || type.isCollection) {
            /*
             * If the geometry is some kind of collection (with polygon considered as a collection of linear rings),
             * invoke this method recursively for all child components. Note that `LinearRing` is not a standard type,
             * so we replace it by the `LineString` parent type.
             */
            final GeometryType componentType;
            if (isPolygon) {
                componentType = GeometryType.LINESTRING;
            } else {
                componentType = type.component();
                if (componentType == null) {
                    unsupportedType(type.name);
                    return null;
                }
            }
            final Class<?> componentClass = library.getGeometryClass(componentType);
            final int n = (int) ogr.getGeometryCount.invokeExact(geom);
            if (n <= 0) {
                return null;
            }
            final Object[] children = (Object[]) Array.newInstance(componentClass, n);
            for (int i=0; i<n; i++) {
                var child = (MemorySegment) ogr.getGeometryRef.invokeExact(geom, i);
                children[i] = geometry(ogr, child);         // May throw ArrayStoreException.
            }
            return library.getGeometry(library.createFromComponents(type, children));
        }
        /*
         * Case of Point and LineString/LinearRing. The buffer capacity must be verified.
         * If the capacity is not sufficient, create a new buffer with the lifetime of this method call only.
         * We do not use the iterator lifetime for avoiding memory leaks, because buffers may be created often
         * if the geometry sizes are increasing in each iteration (buffers are released only in `Arena.close()`).
         */
        int pointCount = (int) ogr.getPointCount.invokeExact(geom);
        if (pointCount <= 0) {      // `OGR_G_GetPointCount` returns 0 if not Point or LineString/LinearRing.
            return null;
        }
        final boolean hasZ    = GeometryType.hasZ(code) && library.supports(Capability.Z_COORDINATE);
        final boolean hasM    = GeometryType.hasM(code) && library.supports(Capability.M_COORDINATE);
        final var  dimensions = Dimensions.forZorM(hasZ, hasM);
        final int  stride     = dimensions.count * Double.BYTES;
        final long capacity   = Math.multiplyFull(pointCount, stride);
        if (buffer.byteSize() < capacity) {
            arena  = layer.store.changeArena(arena, true);
            buffer = arena.allocate(capacity);
        }
        /*
         * (cx, cy, cz, cm) are pointers to the same buffer with an offset of `sizeof(double)` between each.
         * We opportunistically reuse the slices already prepared for dates, by choosing those having the right offset.
         */
        final MemorySegment cx = buffer;
        final MemorySegment cy = day;         // See field javadoc.
        final MemorySegment cz = hasZ ? minute : MemorySegment.NULL;
        final MemorySegment cm = hasM ? (hasZ ? timezone : minute) : MemorySegment.NULL;
        pointCount = (int) ogr.getPoints.invokeExact(geom, cx, stride, cy, stride, cz, stride, cm, stride);
        assert Math.multiplyFull(pointCount, stride) <= cx.byteSize() : pointCount;
        if (pointCount <= 0) {
            return null;
        }
        if (pointCount == 1) {
            double x = cx.get(ValueLayout.JAVA_DOUBLE, 0);
            double y = cy.get(ValueLayout.JAVA_DOUBLE, 0);
            if (hasZ) {
                double z = cz.get(ValueLayout.JAVA_DOUBLE, 0);
                return library.createPoint(x, y, z);
            } else {
                return library.createPoint(x, y);
            }
        }
        /*
         * Case of string line having more than 1 point. The `DoubleBuffer` wraps directly the memory segment.
         * The caller method shall copy the coordinate values, because the buffer will not be valid anymore
         * shortly after this methd call.
         */
        @SuppressWarnings("restricted")
        final MemorySegment coords = cx.reinterpret(Math.multiplyFull(pointCount, stride));
        final DoubleBuffer coordinates = coords.asByteBuffer().order(ByteOrder.nativeOrder()).asDoubleBuffer();
        DoubleBuffer closingPoint = null;
        if (code == LINEAR_RING && pointCount >= 3) {
            /*
             * Polylines are automatically recognized as linear rings if the last point is equal to
             * the first point. If this is not the case while a linear ring was expected, copy that
             * point in a separated vector. The two vectors will be concatenated by `createPolyline`.
             */
            final int last = pointCount - dimensions.count;
            final double[] point = new double[dimensions.count];
            for (int i=0; i<point.length; i++) {
                point[i] = coordinates.get(i);
                if (closingPoint == null && !Numerics.equalsIgnoreZeroSign(point[i], coordinates.get(last + i))) {
                    closingPoint = DoubleBuffer.wrap(point);    // Okay even if we didn't finished to fill the array.
                }
            }
        }
        switch (type) {
            case POINT:      return library.createPoint(false, dimensions, coordinates);
            case MULTIPOINT: return library.createMultiPoint(false, dimensions, coordinates);
            default:         return library.createPolyline(false, false, dimensions, coordinates, closingPoint);
        }
    }

    /**
     * Logs a warning saying that the given type is unsupported.
     * Each type is warned only once per iteration.
     */
    private void unsupportedType(final String type) {
        if (unsupportedTypes.add(type)) {
            layer.store.warning(FeatureLayer.class, "features",
                    new LogRecord(Level.WARNING, layer.errors().getString(Errors.Keys.UnsupportedType_1, type)));
        }
    }

    /**
     * Releases the native memory allocated for this iterator and marks the iteration as completed.
     * Call to this method should be registered with {@link java.util.stream.Stream#onClose(Runnable)}.
     *
     * <h4>Design note</h4>
     * We could register this method for invocation by the garbage collector when the users forgot to invoke
     * {@code Stream.close()}. But we don't in order to throw {@code ConcurrentReadException} systematically,
     * for letting users know that they have a bug.
     */
    @Override
    public void run() {
        synchronized (layer.store) {
            layer.iterationInProgress = null;
            arena = layer.store.changeArena(arena, false);
        }
    }
}
