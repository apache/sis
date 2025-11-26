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

import java.util.Set;
import java.io.Closeable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.storage.geotiff.base.Resources;


/**
 * Base class of GeoTIFF image reader and writer.
 * Those readers and writers are <strong>not</strong> thread safe.
 * The {@link GeoTiffStore} class is responsible for synchronization if needed.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class IOBase implements Closeable {
    /**
     * The magic number for big-endian TIFF files or little-endian TIFF files.
     */
    protected static final short BIG_ENDIAN = 0x4D4D, LITTLE_ENDIAN = 0x4949;

    /**
     * The magic number for classic (32 bits) or big TIFF (64 bits) files.
     */
    protected static final short CLASSIC = 42, BIG_TIFF= 43;

    /**
     * The store which created this reader or writer.
     * This is also the synchronization lock.
     */
    public final GeoTiffStore store;

    /**
     * For subclass constructors.
     *
     * @param  store  the store which created this reader or writer.
     */
    protected IOBase(final GeoTiffStore store) {
        this.store = store;
    }

    /**
     * Returns the modifiers (BigTIFF, <abbr>COG</abbr>…) used by this reader or writer.
     *
     * @see GeoTiffStore#getModifiers()
     */
    public abstract Set<FormatModifier> getModifiers();

    /**
     * Returns the resources to use for formatting error messages.
     */
    final Errors errors() {
        return Errors.forLocale(store.getLocale());
    }

    /**
     * Returns the GeoTIFF-specific resource for error messages and warnings.
     */
    final Resources resources() {
        return Resources.forLocale(store.getLocale());
    }
}
