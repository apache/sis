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


/**
 * Base class of GeoTIFF image reader and writer.
 * Those readers and writers are <strong>not</strong> thread safe.
 * The {@link GeoTiffStore} class is responsible for synchronization if needed.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
abstract class GeoTIFF implements Closeable {
    /**
     * The timezone specified at construction time, or {@code null} for the default.
     * This is not yet configurable, but may become in a future version.
     */
    private static final TimeZone TIMEZONE = null;

    /**
     * The store which created this reader or writer.
     */
    final GeoTiffStore owner;

    /**
     * The object to use for parsing and formatting dates. Created when first needed.
     */
    private transient DateFormat dateFormat;

    /**
     * For subclass constructors.
     */
    GeoTIFF(final GeoTiffStore owner) {
        this.owner = owner;
    }

    /**
     * Returns the resources to use for formatting error messages.
     */
    final Errors errors() {
        return Errors.getResources(owner.getLocale());
    }

    /**
     * Returns the object to use for parsing and formatting dates.
     */
    final DateFormat getDateFormat() {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat("yyy:MM:dd HH:mm:ss", Locale.US);
            if (TIMEZONE != null) {
                dateFormat.setTimeZone(TIMEZONE);
            }
        }
        return dateFormat;
    }
}
