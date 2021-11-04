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

import java.util.Locale;
import java.util.TimeZone;
import java.io.Closeable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.geotiff.Resources;


/**
 * Base class of GeoTIFF image reader and writer.
 * Those readers and writers are <strong>not</strong> thread safe.
 * The {@link GeoTiffStore} class is responsible for synchronization if needed.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
abstract class GeoTIFF implements Closeable {
    /**
     * The timezone for the date and time parsing, or {@code null} for the default.
     * This is not yet configurable, but may become in a future version.
     */
    private static final TimeZone TIMEZONE = null;

    /**
     * The locale to use for parsers or formatter. This is <strong>not</strong> the locale
     * for warnings or other messages emitted to the users.
     */
    private static final Locale LOCALE = Locale.US;

    /**
     * The magic number for big-endian TIFF files or little-endian TIFF files.
     */
    static final short BIG_ENDIAN = 0x4D4D, LITTLE_ENDIAN = 0x4949;

    /**
     * The magic number for classic (32 bits) or big TIFF files.
     */
    static final short CLASSIC = 42, BIG_TIFF= 43;

    /**
     * The store which created this reader or writer.
     * This is also the synchronization lock.
     */
    final GeoTiffStore store;

    /**
     * The object to use for parsing and formatting dates. Created when first needed.
     */
    private transient DateFormat dateFormat;

    /**
     * For subclass constructors.
     */
    GeoTIFF(final GeoTiffStore store) {
        this.store = store;
    }

    /**
     * Returns the resources to use for formatting error messages.
     */
    final Errors errors() {
        return Errors.getResources(store.getLocale());
    }

    /**
     * Returns the GeoTIFF-specific resource for error messages and warnings.
     */
    final Resources resources() {
        return Resources.forLocale(store.getLocale());
    }

    /**
     * Returns the object to use for parsing and formatting dates.
     */
    final DateFormat getDateFormat() {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", LOCALE);
            if (TIMEZONE != null) {
                dateFormat.setTimeZone(TIMEZONE);
            }
        }
        return dateFormat;
    }
}
