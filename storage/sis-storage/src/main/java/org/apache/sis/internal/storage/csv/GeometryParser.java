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
package org.apache.sis.internal.storage.csv;

import org.apache.sis.internal.converter.SurjectiveConverter;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.util.CharSequences;
import org.apache.sis.math.Vector;


/**
 * The converter to use for converting a text into a geometry.
 * The geometry class depends on the library available at runtime.
 *
 * @param  <G>  the geometry class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class GeometryParser<G> extends SurjectiveConverter<String,G> {
    /**
     * The unique instance using the default geometry library.
     */
    private static final GeometryParser<?> INSTANCE = new GeometryParser<>(Geometries.implementation(null), (short) 2);

    /**
     * The factory to use for creating polylines.
     */
    private final Geometries<G> geometries;

    /**
     * The number of dimensions other than time in the coordinate reference system.
     * Shall be 2 or 3 according Moving Features CSV encoding specification, but Apache SIS
     * may be tolerant to other values (depending on the backing geometry library).
     */
    private final short spatialDimensionCount;

    /**
     * Creates a new converter from CSV encoded trajectories to geometries.
     */
    private GeometryParser(final Geometries<G> geometries, final short spatialDimensionCount) {
        this.geometries = geometries;
        this.spatialDimensionCount = spatialDimensionCount;
    }

    /**
     * Returns a parser instance for the given geometry factory.
     */
    static GeometryParser<?> instance(final Geometries<?> geometries, final short spatialDimensionCount) {
        return (spatialDimensionCount == 2 && INSTANCE.geometries == geometries)
               ? INSTANCE : new GeometryParser<>(geometries, spatialDimensionCount);
    }

    /**
     * Returns the type of elements to convert.
     */
    @Override
    public Class<String> getSourceClass() {
        return String.class;
    }

    /**
     * Returns the type of converted elements. The returned type shall be the same than
     * the type selected by {@code Store.parseFeatureType(…)} for the "trajectory" column.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<G> getTargetClass() {
        return (Class<G>) geometries.polylineClass;
    }

    /**
     * Converts an element from the CSV file to the geometry type.
     */
    @Override
    public G apply(final String text) {
        /*
         * We could avoid the "unchecked" warning by using getTargetClass().cast(…), but it would be
         * a false sense of safety since 'getTargetClass()' is itself unchecked. The real check will
         * be performed by DefaultFeatureType.setPropertyValue(…) anyway.
         */
        return geometries.createPolyline(spatialDimensionCount,
                Vector.create(CharSequences.parseDoubles(text, Store.ORDINATE_SEPARATOR), false));
    }
}
