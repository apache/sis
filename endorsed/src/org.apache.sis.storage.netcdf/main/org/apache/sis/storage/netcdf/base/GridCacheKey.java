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
package org.apache.sis.storage.netcdf.base;

import java.util.Set;
import java.util.Arrays;
import java.util.EnumSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.io.stream.ByteWriter;
import org.apache.sis.math.Vector;


/**
 * Cache management of localization grids. {@code GridCache} are used as keys in {@code HashMap}.
 * There are two level of caches:
 *
 * <ul>
 *   <li>Local to the {@link Decoder}. This avoid the need to compute MD5 sum of coordinate vectors.</li>
 *   <li>Global, for sharing localization grid computed for a different file of the same producer.</li>
 * </ul>
 *
 * The base class if for local cache. The inner class is for the global cache.
 * {@code GridCacheKey}s are associated to {@link GridCacheValue}s in a hash map.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
class GridCacheKey {
    /**
     * Size of cached localization grid, in number of cells.
     */
    private final int width, height;

    /**
     * The coordinate axes used for computing the localization grid. For local cache, it shall be {@link Axis} instances.
     * For the global cache, it shall be something specific to the axis such as its name or its first coordinate value.
     * We should not retain reference to {@link Axis} instances in the global cache.
     */
    private final Object xAxis, yAxis;

    /**
     * Creates a new key for caching a localization grid of the given size and built from the given axes.
     */
    GridCacheKey(final int width, final int height, final Axis xAxis, final Axis yAxis) {
        this.width  = width;
        this.height = height;
        this.xAxis  = xAxis;
        this.yAxis  = yAxis;
    }

    /**
     * Creates a global key from the given local key. This constructor is for {@link Global} construction only,
     * because the information stored by this constructor are not sufficient for testing if two grids are equal.
     * The {@link Global} subclass will add a MD5 checksum.
     */
    private GridCacheKey(final GridCacheKey keyLocal) {
        width  = keyLocal.width;
        height = keyLocal.height;
        xAxis  = id(keyLocal.xAxis);
        yAxis  = id(keyLocal.yAxis);
    }

    /**
     * Returns an identifier for the given axis. Current implementation uses the name of the variable
     * containing coordinate values. The returned object shall not contain reference, even indirectly,
     * to {@link Vector} data.
     */
    private static Object id(final Object axis) {
        return ((Axis) axis).getName();
    }

    /**
     * Returns the localization grid from the local cache if one exists, or {@code null} if none.
     * This method looks only in the local cache. For the global cache, see {@link Global#lock()}.
     */
    final GridCacheValue cached(final Decoder decoder) {
        return decoder.localizationGrids.get(this);
    }

    /**
     * Caches the given localization grid in the local caches.
     * This method is invoked after a new grid has been created.
     *
     * @param  decoder  the decoder with local cache.
     * @param  grid     the grid to cache.
     * @return the cached grid. Should be the given {@code grid} instance, unless another grid has been cached concurrently.
     */
    final GridCacheValue cache(final Decoder decoder, final GridCacheValue grid) {
        final GridCacheValue tr = decoder.localizationGrids.putIfAbsent(this, grid);
        return (tr != null) ? tr : grid;
    }

    /**
     * Key for localization grids in the global cache. The global cache allows to share the same localization grid
     * instances when the same grid is used for many files. This may happen for files originating from the same producer.
     * Callers should check in the local cache before to try the global cache.
     *
     * <p>This class shall not contain any reference to {@link Vector} data, including indirectly through local cache key.
     * This class tests vector equality with checksum.</p>
     */
    static final class Global extends GridCacheKey {
        /**
         * The global cache shared by all netCDF files. All grids are retained by weak references.
         */
        private static final Cache<GridCacheKey,GridCacheValue> CACHE = new Cache<>(12, 0, false);

        /**
         * The algorithms tried for making the localization grids more linear.
         * May be empty but shall not be null.
         */
        private final Set<Linearizer.Type> linearizerTypes;

        /**
         * Concatenation of the digests of the two vectors.
         */
        private final byte[] digest;

        /**
         * Creates a new global key derived from the given local key.
         * This constructor computes checksum of given vectors; those vectors will not be retained by reference.
         *
         * @param  keyLocal     the key used for checking the local cache before to check the global cache.
         * @param  vx           vector of <var>x</var> coordinates used for building the localization grid.
         * @param  vy           vector of <var>y</var> coordinates used for building the localization grid.
         * @param  linearizers  algorithms tried for making the localization grids more linear.
         */
        Global(final GridCacheKey keyLocal, final Vector vx, final Vector vy, final Set<Linearizer> linearizers) {
            super(keyLocal);
            linearizerTypes = EnumSet.noneOf(Linearizer.Type.class);
            for (final Linearizer linearizer : linearizers) {
                linearizerTypes.add(linearizer.type);
            }
            final MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                // Should not happen since every Java implementation shall support MD5, SHA-1 and SHA-256.
                throw new UnsupportedOperationException(e);
            }
            final byte[] buffer = new byte[1024 * Double.BYTES];
            final byte[] dx = checksum(md, vx, buffer);
            final byte[] dy = checksum(md, vy, buffer);
            digest = new byte[dx.length + dy.length];
            System.arraycopy(dx, 0, digest, 0, dx.length);
            System.arraycopy(dy, 0, digest, dx.length, dy.length);
        }

        /**
         * Computes the checksum for the given vector.
         *
         * @param  md      the digest algorithm to use.
         * @param  vector  the vector for which to compute a digest.
         * @param  buffer  temporary buffer used by this method.
         * @return the digest.
         */
        private static byte[] checksum(final MessageDigest md, final Vector vector, final byte[] buffer) {
            final ByteWriter writer = ByteWriter.create(vector, buffer);
            int n;
            while ((n = writer.write()) > 0) {
                md.update(buffer, 0, n);
            }
            return md.digest();
        }

        /**
         * Returns a handler for fetching the localization grid from the global cache if one exists, or computing it.
         * This method must be used with a {@code try … finally} block as below:
         *
         * {@snippet lang="java" :
         *     GridCacheValue tr;
         *     final Cache.Handler<GridCacheValue> handler = key.lock();
         *     try {
         *         tr = handler.peek();
         *         if (tr == null) {
         *             // compute the localization grid.
         *         }
         *     } finally {
         *         handler.putAndUnlock(tr);
         *     }
         *     }
         */
        final Cache.Handler<GridCacheValue> lock() {
            return CACHE.lock(this);
        }

        /**
         * Computes a hash code for this global key.
         * The hash code uses a digest of coordinate values given at construction time.
         */
        @Override public int hashCode() {
            return super.hashCode() + linearizerTypes.hashCode() + Arrays.hashCode(digest);
        }

        /**
         * Computes the equality test done by parent class. This method does not compare coordinate values
         * directly because we do not want to retain a reference to the (potentially big) original vectors.
         * Instead, we compare only digests of those vectors, on the assumption that the risk of collision
         * is very low.
         */
        @Override public boolean equals(final Object other) {
            if (super.equals(other)) {
                final Global that = (Global) other;
                if (linearizerTypes.equals(that.linearizerTypes)) {
                    return Arrays.equals(digest, that.digest);
                }
            }
            return false;
        }
    }

    /**
     * Returns a hash code value for this key.
     */
    @Override
    public int hashCode() {
        return 31*width + 37*height + 7*xAxis.hashCode() + yAxis.hashCode();
    }

    /**
     * Compares the given object with this key of equality.
     */
    @Override
    public boolean equals(final Object other) {
        if (other != null && other.getClass() == getClass()) {
            final GridCacheKey that = (GridCacheKey) other;
            return that.width == width && that.height == height && xAxis.equals(that.xAxis) && yAxis.equals(that.yAxis);
        }
        return false;
    }

    /**
     * Returns a string representation of this key for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "width", width, "height", height);
    }
}
