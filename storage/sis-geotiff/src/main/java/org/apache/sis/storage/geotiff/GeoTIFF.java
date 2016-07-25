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

import java.io.Closeable;
import org.apache.sis.util.Localized;
import org.apache.sis.util.resources.Errors;


/**
 * Base class of GeoTIFF image reader and writer.
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
     * The locale for formatting error messages.
     */
    private final Localized owner;

    /**
     * For subclass constructors.
     */
    GeoTIFF(final Localized owner) {
        this.owner = owner;
    }

    /**
     * Returns the resources to use for formatting error messages.
     */
    final Errors errors() {
        return Errors.getResources(owner.getLocale());
    }
}
