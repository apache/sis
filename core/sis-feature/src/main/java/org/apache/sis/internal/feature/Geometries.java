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
package org.apache.sis.internal.feature;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Envelope2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.Static;


/**
 * Utility methods on geometric objects defined in libraries outside Apache SIS.
 * We use this class for isolating dependencies from the {@code org.apache.feature} package
 * to ESRI's API or to Java Topology Suite (JTS) API.
 * This gives us a single place to review if we want to support different geometry libraries,
 * or if Apache SIS come with its own implementation.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class Geometries extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Geometries() {
    }

    /**
     * Returns {@code true} if the given type is one of the type known to Apache SIS.
     *
     * @param  type  the type to verify.
     * @return {@code true} if the given type is one of the geometry type known to SIS.
     */
    public static boolean isKnownType(final Class<?> type) {
        return Geometry.class.isAssignableFrom(type);
    }

    /**
     * If the given object is one of the recognized type and its envelope is non-empty,
     * returns that envelope as an Apache SIS implementation. Otherwise returns {@code null}.
     *
     * @param  geometry  the geometry from which to get the envelope, or {@code null}.
     * @return the envelope of the given object, or {@code null} if the object is not
     *         a recognized geometry or its envelope is empty.
     */
    public static GeneralEnvelope getEnvelope(final Object geometry) {
        if (geometry instanceof Geometry) {
            final Envelope2D bounds = new Envelope2D();
            ((Geometry) geometry).queryEnvelope2D(bounds);
            if (!bounds.isEmpty()) {                                    // Test if there is NaN values.
                final GeneralEnvelope env = new GeneralEnvelope(2);
                env.setRange(0, bounds.xmin, bounds.xmax);
                env.setRange(1, bounds.ymin, bounds.ymax);
                return env;
            }
        }
        return null;
    }
}
