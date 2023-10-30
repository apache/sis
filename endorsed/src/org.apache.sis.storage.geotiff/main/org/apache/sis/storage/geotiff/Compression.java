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
import org.apache.sis.util.internal.Strings;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.io.stream.InternalOptionKey;
import org.apache.sis.util.ArgumentChecks;


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
    public static final Compression NONE = new Compression(org.apache.sis.storage.geotiff.base.Compression.NONE, 0);

    /**
     * Deflate compression, like ZIP format.
     * This is the default compression method.
     */
    public static final Compression DEFLATE = new Compression(org.apache.sis.storage.geotiff.base.Compression.DEFLATE, Deflater.DEFAULT_COMPRESSION);

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
     * The compression level, or -1 for default.
     */
    final int level;

    /**
     * Creates a new instance.
     *
     * @param  method  the compression method.
     */
    private Compression(final org.apache.sis.storage.geotiff.base.Compression method, final int level) {
        this.method = method;
        this.level  = level;
    }

    /**
     * Returns an instance with the specified compression level.
     * Value 0 means no compression. A value of -1 resets the default compression.
     *
     * @param  value  the new compression level (0-9).
     * @return a compression of the specified level.
     */
    public Compression withLevel(final int value) {
        if (value == level) return this;
        ArgumentChecks.ensureBetween("level", Deflater.DEFAULT_COMPRESSION, Deflater.BEST_COMPRESSION, value);
        return new Compression(method, value);
    }

    /**
     * Returns the current compression level.
     *
     * @return the current compression level, or an empty value for the default level.
     */
    public OptionalInt level() {
        return (level >= 0) ? OptionalInt.of(level) : OptionalInt.empty();
    }

    /*
     * TODO: add `withPredictor(Predictor)` method.
     */

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
            return method.equals(c.method) && level == c.level;
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
        return method.hashCode() + level;
    }

    /**
     * Returns a string representation of this compression.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(Compression.class, "method", method,
                "level", (level != 0) ? Integer.valueOf(level) : null);
    }
}
