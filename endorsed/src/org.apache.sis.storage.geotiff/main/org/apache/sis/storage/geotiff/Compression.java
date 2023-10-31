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
package org.apache.sis.storage.geotiff;

import java.io.Serializable;
import java.util.OptionalInt;
import java.util.zip.Deflater;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.Strings;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.geotiff.base.Predictor;
import org.apache.sis.io.stream.InternalOptionKey;


/**
 * The compression method used for writing GeoTIFF files.
 * This class specifies only the compressions supported by the Apache SIS writer.
 * The Apache SIS reader supports more compression methods, but they are not listed in this class.
 *
 * <p>The compression to use can be specified as an option when opening the data store.
 * For example for writing a TIFF file without compression, the following code can be used:</p>
 *
 * {@snippet lang="java" :
 *     var file = Path.of("my_output_file.tiff");
 *     var connector = new StorageConnector(file);
 *     connector.setOption(Compression.OPTION_KEY, Compression.NONE);
 *     try (GeoTiffStore ds = new GeoTiffStore(null, connector)) {
 *         // Write data here.
 *     }
 *     }
 *
 * If no compression is explicitly specified, Apache SIS uses by default the {@link #DEFLATE} compression.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class Compression implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3916905136793784898L;

    /**
     * No compression, but pack data into bytes as tightly as possible.
     */
    public static final Compression NONE = new Compression(
            org.apache.sis.storage.geotiff.base.Compression.NONE,
            0, Predictor.NONE);

    /**
     * Deflate compression (like ZIP format) with a default compression level and a default predictor.
     * This is the compression used by default by the Apache SIS GeoTIFF writer.
     */
    public static final Compression DEFLATE = new Compression(
            org.apache.sis.storage.geotiff.base.Compression.DEFLATE,
            Deflater.DEFAULT_COMPRESSION, Predictor.HORIZONTAL_DIFFERENCING);

    /**
     * The key for declaring the compression at store creation time.
     * See class Javadoc for usage example.
     *
     * @see StorageConnector#setOption(OptionKey, Object)
     */
    public static final OptionKey<Compression> OPTION_KEY = new InternalOptionKey<>("TIFF_COMPRESSION", Compression.class);

    /**
     * The compression method.
     */
    final org.apache.sis.storage.geotiff.base.Compression method;

    /**
     * The compression level from 0 to 9 inclusive, or -1 for default.
     */
    final int level;

    /**
     * The predictor to apply before compression.
     */
    final Predictor predictor;

    /**
     * Creates a new instance.
     *
     * @param  method     the compression method.
     * @param  level      the compression level, or -1 for default.
     * @param  predictor  the predictor to apply before compression.
     */
    private Compression(final org.apache.sis.storage.geotiff.base.Compression method, final int level, final Predictor predictor) {
        this.method    = method;
        this.level     = level;
        this.predictor = predictor;
    }

    /**
     * Returns an instance with the specified compression level.
     * The value can range from {@value Deflater#BEST_SPEED} to {@value Deflater#BEST_COMPRESSION} inclusive.
     * A value of {@value Deflater#NO_COMPRESSION} means no compression.
     * A value of {@value Deflater#DEFAULT_COMPRESSION} resets the default compression.
     *
     * @param  value  the new compression level (0-9), or -1 for the default compression.
     * @return a compression of the specified level.
     * @throws IllegalArgumentException if the given value is not in the expected range.
     *
     * @see Deflater#BEST_SPEED
     * @see Deflater#BEST_COMPRESSION
     * @see Deflater#NO_COMPRESSION
     */
    public Compression withLevel(final int value) {
        if (value == level) return this;
        ArgumentChecks.ensureBetween("level", Deflater.DEFAULT_COMPRESSION, Deflater.BEST_COMPRESSION, value);
        return new Compression(method, (byte) value, predictor);
    }

    /**
     * Returns the current compression level.
     * The returned value is between 0 and 9 inclusive.
     *
     * @return the current compression level, or an empty value for the default level.
     */
    public OptionalInt level() {
        return (level >= 0) ? OptionalInt.of(level) : OptionalInt.empty();
    }

    /**
     * Returns an instance with the specified predictor. A predictor is a mathematical
     * operator that is applied to the image data before an encoding scheme is applied.
     * Predictors can improve the result of some compression algorithms.
     *
     * <p>The given predictor may be ignored if it is unsupported by this compression.
     * For example invoking this method on {@link #NONE} has no effect.</p>
     *
     * <p>The constants defined in this {@code Compression} class are already defined
     * with suitable predictors. This method usually do not need to be invoked.</p>
     *
     * @param  value  one of the {@code PREDICTOR_*} constants in {@link BaselineTIFFTagSet}.
     * @return a compression using the specified predictor.
     * @throws IllegalArgumentException if the given value is not valid.
     *
     * @see BaselineTIFFTagSet#PREDICTOR_NONE
     * @see BaselineTIFFTagSet#PREDICTOR_HORIZONTAL_DIFFERENCING
     */
    public Compression withPredictor(final int value) {
        if (value == predictor.code || !usePredictor()) {
            return this;
        }
        return new Compression(method, level, Predictor.supported(value));
    }

    /**
     * Returns the current predictor.
     * The returned value is one of the {@code PREDICTOR_*} constants defined in {@link BaselineTIFFTagSet}.
     *
     * @return one of the {@code PREDICTOR_*} constants, or empty if predictor does not apply to this compression.
     */
    public OptionalInt predictor() {
        return usePredictor() ? OptionalInt.of(predictor.code) : OptionalInt.empty();
    }

    /**
     * {@return whether the compression method may use predictor}.
     */
    final boolean usePredictor() {
        return !org.apache.sis.storage.geotiff.base.Compression.NONE.equals(method);
    }

    /**
     * Compares this compression with the given object for equality.
     *
     * @param  other  the object to compare with this compression.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof Compression) {
            final var c = (Compression) other;
            return (level == c.level) && method.equals(c.method) && predictor.equals(c.predictor);
        }
        return false;
    }

    /**
     * Returns a hash code value for this compression.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return method.hashCode() + predictor.hashCode() + level;
    }

    /**
     * Returns a string representation of this compression.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(Compression.class, "method", method,
                "level", (level != 0) ? Integer.valueOf(level) : null,
                "predictor", usePredictor() ? predictor : null);
    }
}
