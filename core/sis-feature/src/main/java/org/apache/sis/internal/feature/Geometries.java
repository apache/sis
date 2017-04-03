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

import java.util.logging.Level;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Envelope2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.Static;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Loggers;


/**
 * Utility methods on geometric objects defined in libraries outside Apache SIS.
 * We use this class for isolating dependencies from the {@code org.apache.feature} package
 * to ESRI's API or to Java Topology Suite (JTS) API.
 * This gives us a single place to review if we want to support different geometry libraries,
 * or if Apache SIS come with its own implementation.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
public final class Geometries extends Static {
    /**
     * The geometry object from Java Topology Suite (JTS),
     * or {@code null} if the JTS library is not on the classpath.
     */
    private static final Class<?> JTS;

    /**
     * Getter methods on JTS envelopes, or {@code null} if the JTS library is not on the classpath.
     * Each methods take no argument and return a {@code double} value.
     */
    private static final Method INTERNAL, MIN_X, MIN_Y, MAX_X, MAX_Y;

    static {
        Class<?> type;
        Method genv, xmin, ymin, xmax, ymax;
        try {
            final Class<?> envt;
            type = Class.forName("com.vividsolutions.jts.geom.Geometry");
            genv = type.getMethod("getEnvelopeInternal", (Class[]) null);
            envt = genv.getReturnType();
            xmin = envt.getMethod("getMinX", (Class[]) null);
            ymin = envt.getMethod("getMinY", (Class[]) null);
            xmax = envt.getMethod("getMaxX", (Class[]) null);
            ymax = envt.getMethod("getMaxY", (Class[]) null);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Logging.getLogger(Loggers.GEOMETRY).log(Level.CONFIG, e.toString());
            type = null;
            genv = null;
            xmin = null;
            xmax = null;
            ymin = null;
            ymax = null;
        }
        JTS = type;
        INTERNAL = genv;
        MIN_X = xmin;
        MIN_Y = ymin;
        MAX_X = xmax;
        MAX_Y = ymax;
    }

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
        return Geometry.class.isAssignableFrom(type) || (JTS != null && JTS.isAssignableFrom(type));
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
        final double xmin, ymin, xmax, ymax;
        if (geometry instanceof Geometry) {
            final Envelope2D bounds = new Envelope2D();
            ((Geometry) geometry).queryEnvelope2D(bounds);
            if (bounds.isEmpty()) {                                     // Test if there is NaN values.
                return null;
            }
            xmin = bounds.xmin;
            ymin = bounds.ymin;
            xmax = bounds.xmax;
            ymax = bounds.ymax;
        } else if (JTS != null && JTS.isInstance(geometry)) {
            try {
                final Object env = INTERNAL.invoke(geometry, (Object[]) null);
                xmin = (Double) MIN_X.invoke(env, (Object[]) null);
                ymin = (Double) MIN_Y.invoke(env, (Object[]) null);
                xmax = (Double) MAX_X.invoke(env, (Object[]) null);
                ymax = (Double) MAX_Y.invoke(env, (Object[]) null);
            } catch (ReflectiveOperationException e) {
                if (e instanceof InvocationTargetException) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                }
                // Should never happen unless JTS's API changed.
                throw (Error) new IncompatibleClassChangeError(e.toString()).initCause(e);
            }
        } else {
            return null;
        }
        final GeneralEnvelope env = new GeneralEnvelope(2);
        env.setRange(0, xmin, xmax);
        env.setRange(1, ymin, ymax);
        return env;
    }
}
