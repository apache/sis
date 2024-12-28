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

import java.util.Map;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.feature.privy.FeatureUtilities;
import org.apache.sis.util.privy.Strings;


/**
 * Names and types of fields read by <abbr>OGR</abbr> with binding code for getting values.
 * This is a mapping to the {@code OGRFieldType} enumeration in the C/C++ <abbr>API</abbr>.
 * Each <abbr>OGR</abbr> type is associated to a {@linkplain #getJavaClass() Java class}
 * and to a Java code for getting the value from a given {@code OGRFeatureH} instance.
 *
 * <h4>Multi-threading</h4>
 * Instances of this class are immutable, thread-safe and shared by many feature layers.
 * Nevertheless, all methods having a {@link FeatureIterator} argument must be invoked in a
 * block synchronized on {@link FeatureLayer#store}, because {@code GDALDataset} is not thread-safe.
 *
 * @param  <V>  type of values read by this field.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
abstract class FieldAccessor<V> {
    /**
     * Number of fields added by Apache <abbr>SIS</abbr> compared to the <abbr>OGR</abbr> fields.
     * There is one additional field for the {@value AttributeConvention#IDENTIFIER} property.
     */
    static final int NUM_ADDITIONAL_FIELDS = 1;

    /**
     * Number of integer values expected by <abbr>GDAL</abbr> {@code OGR_F_GetFieldAsDateTimeEx}.
     * The {@code float} argument is counted as {@code int} because both types have the same size.
     * This information determines the length of {@link FeatureIterator#buffer}.
     */
    private static final int NUM_DATE_COMPONENTS = 7;

    /**
     * Name of this field. Each name in a {@code FeatureType} shall be unique.
     * This name may differ from the name declared by <abbr>GDAL</abbr> if name collisions were detected.
     *
     * @see #name()
     * @see #rename(Map, Integer)
     */
    private String name;

    /**
     * Index of this field in each {@code OGRFeatureH} instance.
     * This is not necessarily the index in <abbr>SIS</abbr> features,
     * because the latter have additional fields such as the identifier.
     */
    protected final int index;

    /**
     * Creates a new field.
     *
     * @param  name   name of the field.
     * @param  index  index of the field in {@code OGRFeatureH}.
     */
    protected FieldAccessor(final String name, final int index) {
        this.name  = name;
        this.index = index;
    }

    /**
     * Renames this field for avoiding name collision.
     * The algorithm in this method is not verify efficient, but should be okay in the common
     * case where there is only a few fields (typically only two) in conflict for a given name.
     *
     * @param  names  names that are already used.
     * @param  value  value to assign to the new name in the map.
     */
    final void rename(final Map<String,Integer> names, final Integer value) {
        final String base = name;
        int counter = 0;
        do name = base + FeatureUtilities.DISAMBIGUATION_SEQUENTIAL_NUMBER_PREFIX + (++counter);
        while (names.putIfAbsent(name, value) != null);
    }

    /**
     * Returns the name of this field. Each name in a {@code FeatureType} is unique.
     * This name may differ from the name declared by <abbr>GDAL</abbr> if name collisions were detected.
     */
    public final String name() {
        return name;
    }

    /**
     * Returns the minimal size of the native buffer that this field will need for fetching values.
     * A buffer is needed when invoking <abbr>GDAL</abbr> functions having arguments that are pointers
     * where the function will write the result. It may be, for example, where <abbr>GDAL</abbr> will
     * write the (year, month, day) components of a date. A value of 0 means that no buffer is needed.
     *
     * @see FeatureIterator#buffer
     */
    int bufferSize() {
        return 0;
    }

    /**
     * If the value class is a list, returns the type of list elements. Otherwise, returns {@code null}.
     * Arrays of primitive types are counted as a single instance with this method returning null.
     */
    Class<?> getElementClass() {
        return null;
    }

    /**
     * Returns the type of objects decoded by this field.
     */
    abstract Class<V> getJavaClass();

    /**
     * Gets a Java value from the <abbr>GDAL</abbr> {@code OGRFeatureH} instance
     * at the current iterator position. The result may be null.
     *
     * @param  source   the iterator which is creating a feature instance.
     * @param  ogr      pointers to native functions.
     * @param  feature  pointer to the {@code OGRFeatureH} instance.
     * @return value fetched from the feature (may be {@code null}).
     * @throws Throwable if an error occurred during the native method call.
     */
    abstract V getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable;

    /**
     * Returns whether the field in the given feature is non-null.
     * Must be invoked in a block synchronized on {@link FeatureLayer#store}.
     *
     * @param  ogr      pointers to native functions.
     * @param  feature  pointer to the {@code OGRFeatureH} instance.
     * @return whether the field is non-null in the given feature instance.
     * @throws Throwable if an error occurred during the native method call.
     */
    final boolean hasValue(final OGR ogr, final MemorySegment feature) throws Throwable {
        return ((int) ogr.isFieldNull.invokeExact(feature, index)) == 0;
    }

    /**
     * Field for feature identifiers.
     * This field is added by Apache SIS, this is not a field in <abbr>OGR</abbr> features.
     */
    static final class Identifier extends FieldAccessor<Long> {
        /** The singleton instance. */
        static final Identifier INSTANCE = new Identifier();

        private Identifier() {
            super(AttributeConvention.IDENTIFIER, -NUM_ADDITIONAL_FIELDS);
        }

        @Override
        Class<Long> getJavaClass() {
            return Long.class;
        }

        @Override
        Long getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            long id = (long) ogr.getFeatureId.invokeExact(feature);
            return (id >= 0) ? id : null;
        }
    }

    /**
     * {@code OFTString} (type 4): String of ASCII characters.
     * Also used for {@code OFTWideString} (type 6, deprecated).
     * Also used for all fields of unknown type.
     */
    private static final class Text extends FieldAccessor<String> {
        Text(String name, int index) {
            super(name, index);
        }

        @Override
        Class<String> getJavaClass() {
            return String.class;
        }

        @Override
        String getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            if (hasValue(ogr, feature)) {
                return GDAL.toString((MemorySegment) ogr.getFieldAsString.invokeExact(feature, index));
            }
            return null;
        }
    }

    /**
     * {@code OFTInteger} (type 0): 32-bits integer.
     */
    private static final class Integer32 extends FieldAccessor<Integer> {
        /** The {@code OGRFieldType} enumeration value. */
        static final int TYPE = 0;

        Integer32(String name, int index) {
            super(name, index);
        }

        @Override
        Class<Integer> getJavaClass() {
            return Integer.class;
        }

        @Override
        Integer getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            if (hasValue(ogr, feature)) {
                return (int) ogr.getFieldAsInteger.invokeExact(feature, index);
            }
            return null;
        }
    }

    /**
     * {@code OFTInteger64} (type 12): 64-bits integer.
     */
    private static final class Integer64 extends FieldAccessor<Long> {
        /** The {@code OGRFieldType} enumeration value. */
        static final int TYPE = 12;

        Integer64(String name, int index) {
            super(name, index);
        }

        @Override
        Class<Long> getJavaClass() {
            return Long.class;
        }

        @Override
        Long getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            if (hasValue(ogr, feature)) {
                return (long) ogr.getFieldAsLong.invokeExact(feature, index);
            }
            return null;
        }
    }

    /**
     * {@code OFTReal} (type 2): Double-precision floating point.
     */
    private static final class Real extends FieldAccessor<Double> {
        /** The {@code OGRFieldType} enumeration value. */
        static final int TYPE = 2;

        Real(String name, int index) {
            super(name, index);
        }

        @Override
        Class<Double> getJavaClass() {
            return Double.class;
        }

        @Override
        Double getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            if (hasValue(ogr, feature)) {
                return (double) ogr.getFieldAsDouble.invokeExact(feature, index);
            }
            return null;
        }
    }

    /**
     * {@code OFTDate} (type 9): Date.
     */
    private static final class Date extends FieldAccessor<LocalDate> {
        /** The {@code OGRFieldType} enumeration value. */
        static final int TYPE = 9;

        Date(String name, int index) {
            super(name, index);
        }

        @Override
        Class<LocalDate> getJavaClass() {
            return LocalDate.class;
        }

        @Override
        int bufferSize() {
            return NUM_DATE_COMPONENTS * Integer.BYTES;
        }

        @Override
        LocalDate getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            return source.hasDate(ogr, feature, index) ? source.date() : null;
        }
    }

    /**
     * {@code OFTTime} (type 10): Time.
     * The Java class may be {@link LocalTime} or {@link OffsetTime},
     * depending on whether the timezone if local or GMT.
     */
    private static final class Time extends FieldAccessor<Temporal> {
        /** The {@code OGRFieldType} enumeration value. */
        static final int TYPE = 10;

        Time(String name, int index) {
            super(name, index);
        }

        @Override
        Class<Temporal> getJavaClass() {
            return Temporal.class;
        }

        @Override
        int bufferSize() {
            return NUM_DATE_COMPONENTS * Integer.BYTES;
        }

        @Override
        Temporal getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            if (source.hasDate(ogr, feature, index)) {
                LocalTime  time = source.time();
                ZoneOffset zone = source.timezone();
                return (zone != null) ? time.atOffset(zone) : time;
            }
            return null;
        }
    }

    /**
     * {@code OFTDateTime} (type 11): Date and Time.
     * The Java class may be {@link LocalDateTime} or {@link OffsetDateTime},
     * depending on whether the timezone is local or GMT.
     */
    private static final class DateTime extends FieldAccessor<Temporal> {
        /** The {@code OGRFieldType} enumeration value. */
        static final int TYPE = 11;

        DateTime(String name, int index) {
            super(name, index);
        }

        @Override
        Class<Temporal> getJavaClass() {
            return Temporal.class;
        }

        @Override
        int bufferSize() {
            return NUM_DATE_COMPONENTS * Integer.BYTES;
        }

        @Override
        Temporal getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            if (source.hasDate(ogr, feature, index)) {
                LocalDateTime date = LocalDateTime.of(source.date(), source.time());
                ZoneOffset    zone = source.timezone();
                return (zone != null) ? date.atOffset(zone) : date;
            }
            return null;
        }
    }

    /**
     * {@code OFTStringList} (type 5): Array of strings.
     * Also used for {@code OFTWideStringList} (type 7, deprecated).
     */
    private static final class StringList extends FieldAccessor<List<String>> {
        /** The {@code OGRFieldType} enumeration value. */
        static final int TYPE = 5;

        StringList(String name, int index) {
            super(name, index);
        }

        @Override
        @SuppressWarnings("unchecked")
        Class<List<String>> getJavaClass() {
            return (Class) List.class;
        }

        @Override
        Class<?> getElementClass() {
            return String.class;
        }

        @Override
        int bufferSize() {
            return Integer.BYTES;
        }

        @Override
        List<String> getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            var p = (MemorySegment) ogr.getFieldAsStringList.invokeExact(feature, index);
            return GDAL.fromNullTerminatedStrings(p);
        }
    }

    /**
     * {@code OFTIntegerList} (type 1): Array of 32-bits integers.
     */
    private static final class IntegerList extends FieldAccessor<int[]> {
        /** The {@code OGRFieldType} enumeration value. */
        static final int TYPE = 1;

        IntegerList(String name, int index) {
            super(name, index);
        }

        @Override
        Class<int[]> getJavaClass() {
            return int[].class;
        }

        @Override
        int bufferSize() {
            return Integer.BYTES;
        }

        @Override
        @SuppressWarnings("restricted")
        int[] getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            final MemorySegment buffer = source.buffer();
            var p = (MemorySegment) ogr.getFieldAsIntegerList.invokeExact(feature, index, buffer);
            int n = buffer.get(ValueLayout.JAVA_INT, 0);
            if (n > 0) {
                return p.reinterpret(n * Integer.BYTES).toArray(ValueLayout.JAVA_INT);
            }
            return null;
        }
    }

    /**
     * {@code OFTInteger64List} (type 13): Array of 64-bits integers.
     */
    private static final class LongList extends FieldAccessor<long[]> {
        /** The {@code OGRFieldType} enumeration value. */
        static final int TYPE = 13;

        LongList(String name, int index) {
            super(name, index);
        }

        @Override
        Class<long[]> getJavaClass() {
            return long[].class;
        }

        @Override
        int bufferSize() {
            return Integer.BYTES;
        }

        @Override
        @SuppressWarnings("restricted")
        long[] getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            final MemorySegment buffer = source.buffer();
            var p = (MemorySegment) ogr.getFieldAsLongList.invokeExact(feature, index, buffer);
            int n = buffer.get(ValueLayout.JAVA_INT, 0);
            if (n > 0) {
                return p.reinterpret(n * Long.BYTES).toArray(ValueLayout.JAVA_LONG);
            }
            return null;
        }
    }

    /**
     * {@code OFTRealList} (type 3): Array of doubles.
     */
    private static final class RealList extends FieldAccessor<double[]> {
        /** The {@code OGRFieldType} enumeration value. */
        static final int TYPE = 3;

        RealList(String name, int index) {
            super(name, index);
        }

        @Override
        Class<double[]> getJavaClass() {
            return double[].class;
        }

        @Override
        int bufferSize() {
            return Integer.BYTES;
        }

        @Override
        @SuppressWarnings("restricted")
        double[] getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            final MemorySegment buffer = source.buffer();
            var p = (MemorySegment) ogr.getFieldAsDoubleList.invokeExact(feature, index, buffer);
            int n = buffer.get(ValueLayout.JAVA_INT, 0);
            if (n > 0) {
                return p.reinterpret(n * Double.BYTES).toArray(ValueLayout.JAVA_DOUBLE);
            }
            return null;
        }
    }

    /**
     * {@code OFTBinary} (type 8): Raw Binary data.
     */
    private static final class Binary extends FieldAccessor<ByteBuffer> {
        /** The {@code OGRFieldType} enumeration value. */
        static final int TYPE = 8;

        Binary(String name, int index) {
            super(name, index);
        }

        @Override
        Class<ByteBuffer> getJavaClass() {
            return ByteBuffer.class;
        }

        @Override
        int bufferSize() {
            return Integer.BYTES;
        }

        @Override
        @SuppressWarnings("restricted")
        ByteBuffer getValue(FeatureIterator source, OGR ogr, MemorySegment feature) throws Throwable {
            final MemorySegment buffer = source.buffer();
            var p = (MemorySegment) ogr.getFieldAsBinary.invokeExact(feature, index, buffer);
            int n = buffer.get(ValueLayout.JAVA_INT, 0);
            if (n > 0) {
                /*
                 * Do not use `p.reinterpret(n, source.arena, null).asByteBuffer()`
                 * because GDAL said that the array lifetime may be very brief.
                 * We need a copy.
                 */
                return ByteBuffer.wrap(p.reinterpret(n).toArray(ValueLayout.JAVA_BYTE));
            }
            return null;
        }
    }

    /**
     * Names and types of geometry fields read by <abbr>OGR</abbr>.
     * Those fields are not created by the static {@link #create create(…)} method,
     * as <abbr>GDAL</abbr> use a separated <abbr>API</abbr> for geometry fields.
     *
     * @param <G> the base class of all geometry objects (except point in some implementations).
     * @param <V> the type of geometry in this field. Usually assignable to {@code <G>}, but not always.
     */
    static final class Geometry<G,V> extends FieldAccessor<V> {
        /** The class of geometries in this field. */
        private final Class<V> javaClass;

        /** The coordinate reference system of the geometries in this field, or {@code null} if unknown. */
        final CoordinateReferenceSystem crs;

        /** Creates a new field for geometries of the specified type. */
        Geometry(String name, int index, Class<V> javaClass, CoordinateReferenceSystem crs) {
            super(name, index);
            this.javaClass = javaClass;
            this.crs = crs;
        }

        /**
         * Returns the class of geometries in this field.
         * This is provided by {@code OGRGeomFieldDefnH}.
         */
        @Override
        Class<V> getJavaClass() {
            return javaClass;
        }

        /**
         * Returns a default size of the native buffer that this field will need for fetching values.
         * If this buffer is not large enough, the iterator will expand it.
         */
        @Override
        int bufferSize() {
            return 64 * SpatialRef.BIDIMENSIONAL * Double.BYTES;
        }

        /**
         * Gets a geometry from the <abbr>GDAL</abbr> {@code OGRFeatureH} instance
         * at the current iterator position. The result may be null.
         *
         * @param  source   the iterator which is creating a feature instance.
         * @param  ogr      pointers to native functions.
         * @param  feature  pointer to the {@code OGRFeatureH} instance.
         * @return geometry fetched from the feature (may be {@code null}).
         * @throws Throwable if an error occurred during the native method call
         *         or during the construction of the corresponding Java object.
         */
        @Override
        @SuppressWarnings("AssertWithSideEffects")
        V getValue(final FeatureIterator source, final OGR ogr, final MemorySegment feature) throws Throwable {
            final var handle = (MemorySegment) ogr.getFeatureGeometry.invokeExact(feature);
            return GDAL.isNull(handle) ? null : javaClass.cast(source.geometry(ogr, handle));
        }
    }

    /**
     * Describes the field at the given index of the given layer.
     * This method should be invoked only for non-geometry fields.
     *
     * @param  ogr    pointers to native functions.
     * @param  layer  wrapper of the {@code OGRLayerH} instance.
     * @param  index  index of the field to describe.
     * @return a handler for getting the field value for each native feature instance.
     * @throws Throwable if an error occurred during a native method call.
     */
    static FieldAccessor<?> create(final OGR ogr, final MemorySegment layer, final int index) throws Throwable {
        final var definition = (MemorySegment) ogr.getFieldDefinition.invokeExact(layer, index);
        final String name = GDAL.toString((MemorySegment) ogr.getFieldName.invokeExact(definition));
        final int type = (int) ogr.getFieldType.invokeExact(definition);
        switch (type) {
            case 7:                 // Deprecated code for StringList.
            case StringList .TYPE:  return new StringList (name, index);
            case Integer32  .TYPE:  return new Integer32  (name, index);
            case Integer64  .TYPE:  return new Integer64  (name, index);
            case Real       .TYPE:  return new Real       (name, index);
            case Date       .TYPE:  return new Date       (name, index);
            case Time       .TYPE:  return new Time       (name, index);
            case DateTime   .TYPE:  return new DateTime   (name, index);
            case IntegerList.TYPE:  return new IntegerList(name, index);
            case LongList   .TYPE:  return new LongList   (name, index);
            case RealList   .TYPE:  return new RealList   (name, index);
            case Binary     .TYPE:  return new Binary     (name, index);
            default:                return new Text       (name, index);
        }
    }

    /**
     * Returns a string representation of this field for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "name", name, "index", index);
    }
}
