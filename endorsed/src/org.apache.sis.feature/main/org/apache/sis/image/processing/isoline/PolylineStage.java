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
package org.apache.sis.image.processing.isoline;

import java.util.Map;
import java.awt.Shape;
import java.awt.geom.Path2D;
import org.apache.sis.util.Debug;


/**
 * Tells at which stage are the polylines represented by a Java2D {@link Shape}.
 * A set of polylines way still be under construction in {@link PolylineBuffer}
 * during iteration over pixel values, or the polylines may have been classified
 * as incomplete after iteration over a row, or the polylines may be final result.
 *
 * <p>This is used only for debugging purposes because end users should see only the final result.
 * This information allows {@code StepsViewer} (in test package) to use different colors for different stages.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 */
@Debug
enum PolylineStage {
    /**
     * The polylines are under construction in various {@link PolylineBuffer} instances.
     * This is the first stage, which happens during iteration over pixel values.
     */
    BUFFER,

    /**
     * The polylines are no longer in the buffers filled by the iteration over pixel values,
     * but are still incomplete. It happens when, after finishing iteration over a row, some
     * polylines will not be continued by iteration on the next row and those polylines have
     * not yet been closed as polygons. Those polyline fragments are moved to a "pending" list,
     * as they may be closed later after more polylines fragments become available.
     */
    FRAGMENT,

    /**
     * The polylines are final result to be show to user.
     */
    FINAL;

    /**
     * Returns the destination where to write polylines for this stage.
     *
     * @param  appendTo  map of path for different stages.
     * @return the path to use for writing polylines at this stage.
     */
    private Path2D destination(final Map<PolylineStage,Path2D> appendTo) {
        return appendTo.computeIfAbsent(this, (k) -> new Path2D.Float());
    }

    /**
     * Adds coordinates to the specified map.
     *
     * @param  appendTo     where to append the coordinates.
     * @param  coordinates  (x,y) tuples to append, starting with the coordinate at index 0.
     * @param  size         number of coordinates to add (twice the number of tuples).
     */
    final void add(final Map<PolylineStage,Path2D> appendTo, final double[] coordinates, final int size) {
        int i = 0;
        if (i < size) {
            final Path2D p = destination(appendTo);
            p.moveTo(coordinates[i++], coordinates[i++]);
            while (i < size) {
                p.lineTo(coordinates[i++], coordinates[i++]);
            }
        }
    }

    /**
     * Adds polylines in the values of the given map. Keys are ignored.
     *
     * @param  appendTo      where to append the coordinates.
     * @param  partialPaths  map of polylines to add.
     */
    final void add(final Map<PolylineStage,Path2D> appendTo, final Map<?,Fragments> partialPaths) {
        for (final Fragments fragment : partialPaths.values()) {
            for (final double[] coordinates : fragment) {
                if (coordinates != null) {
                    add(appendTo, coordinates, coordinates.length);
                }
            }
        }
    }

    /**
     * Adds polylines to the specified map.
     *
     * @param  appendTo   where to append the polylines.
     * @param  polylines  the polylines to append to the map, or {@code null} if none.
     */
    final void add(final Map<PolylineStage,Path2D> appendTo, final Shape polylines) {
        if (polylines != null) {
            destination(appendTo).append(polylines, false);
        }
    }
}
