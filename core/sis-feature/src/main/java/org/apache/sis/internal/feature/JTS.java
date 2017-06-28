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

import java.util.Iterator;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.math.Vector;


/**
 * Centralizes some usages of JTS geometry API by Apache SIS.
 * We use this class for isolating dependencies from the {@code org.apache.feature} package
 * to ESRI's API or to Java Topology Suite (JTS) API.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 *
 * @todo avoid use of reflection and use JTS API directly after JTS released
 *       a new version of the library under BSD-like license.
 */
final class JTS extends Geometries<Object> {
    /**
     * Getter methods on JTS envelopes.
     * Each methods take no argument and return a {@code double} value.
     */
    private final Method getEnvelopeInternal, getMinX, getMinY, getMaxX, getMaxY;

    /**
     * Creates the singleton instance.
     */
    JTS() throws ClassNotFoundException, NoSuchMethodException {
        super(/*GeometryLibrary.JTS, */ null,                               // TODO
              (Class) Class.forName("com.vividsolutions.jts.geom.Geometry"),    // TODO
              Class.forName("com.vividsolutions.jts.geom.Point"),
              Class.forName("com.vividsolutions.jts.geom.LineString"),
              Class.forName("com.vividsolutions.jts.geom.Polygon"));
        getEnvelopeInternal = rootClass.getMethod("getEnvelopeInternal", (Class[]) null);
        final Class<?> envt = getEnvelopeInternal.getReturnType();
        getMinX = envt.getMethod("getMinX", (Class[]) null);
        getMinY = envt.getMethod("getMinY", (Class[]) null);
        getMaxX = envt.getMethod("getMaxX", (Class[]) null);
        getMaxY = envt.getMethod("getMaxY", (Class[]) null);
    }

    /**
     * If the given object is a JTS geometry and its envelope is non-empty, returns
     * that envelope as an Apache SIS implementation. Otherwise returns {@code null}.
     *
     * @param  geometry  the geometry from which to get the envelope, or {@code null}.
     * @return the envelope of the given object, or {@code null} if the object is not
     *         a recognized geometry or its envelope is empty.
     */
    @Override
    final GeneralEnvelope tryGetEnvelope(final Object geometry) {
        final double xmin, ymin, xmax, ymax;
        if (rootClass.isInstance(geometry)) {
            try {
                final Object env = getEnvelopeInternal.invoke(geometry, (Object[]) null);
                xmin = (Double) getMinX.invoke(env, (Object[]) null);
                ymin = (Double) getMinY.invoke(env, (Object[]) null);
                xmax = (Double) getMaxX.invoke(env, (Object[]) null);
                ymax = (Double) getMaxY.invoke(env, (Object[]) null);
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
            final GeneralEnvelope env = new GeneralEnvelope(2);
            env.setRange(0, xmin, xmax);
            env.setRange(1, ymin, ymax);
            return env;
        }
        return null;
    }

    /**
     * If the given point is an implementation of this library, returns its coordinate.
     * Otherwise returns {@code null}. If non-null, the returned array may have a length of 2 or 3.
     */
    @Override
    final double[] tryGetCoordinate(final Object point) {
        return null;   // TODO - see class javadoc
    }

    /**
     * Creates a two-dimensional point from the given coordinate.
     */
    @Override
    public Object createPoint(double x, double y) {
        throw unsupported(2);   // TODO - see class javadoc
    }

    /**
     * Creates a polyline from the given ordinate values.
     * Each {@link Double#NaN} ordinate value start a new path.
     * The implementation returned by this method must be an instance of {@link #rootClass}.
     */
    @Override
    public Object createPolyline(final int dimension, final Vector... ordinates) {
        // TODO - see class javadoc
        throw unsupported(dimension);
    }

    /**
     * Merges a sequence of points or paths if the first instance is an implementation of this library.
     *
     * @throws ClassCastException if an element in the iterator is not a JTS geometry.
     */
    @Override
    final Object tryMergePolylines(final Object first, final Iterator<?> polylines) {
        throw unsupported(2);   // TODO - see class javadoc
    }
}
