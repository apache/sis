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
package org.apache.sis.pending.geoapi.evolution;

import java.lang.reflect.Method;
import static java.util.logging.Logger.getLogger;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Modules;


/**
 * Temporary methods used until a new major GeoAPI release provides the missing functionalities.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Interim {
    /**
     * Do not allow instantiation of this class.
     */
    private Interim() {
    }

    /**
     * Returns the return type of the given method, or the interim type if the method is annotated
     * with {@link InterimType}.
     *
     * @param  method  the method from which to get the return type.
     * @return the return type or the interim type.
     */
    public static Class<?> getReturnType(final Method method) {
        final InterimType an = method.getAnnotation(InterimType.class);
        return (an != null) ? an.value() : method.getReturnType();
    }

    /**
     * Invokes {@code Geometry.getEnvelope()} if that method exists.
     *
     * @param  geometry  the geometry from which to get the envelope.
     * @return the geometry envelope, or {@code null} if none.
     */
    public static Envelope getEnvelope(final Geometry geometry) {
        try {
            return (Envelope) geometry.getClass().getMethod("getEnvelope").invoke(geometry);
        } catch (ReflectiveOperationException | ClassCastException e) {
            Logging.recoverableException(getLogger(Modules.METADATA), Interim.class, "getEnvelope", e);
            return null;
        }
    }
}
