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
package org.apache.sis.internal.feature.j2d;


/**
 * A polygons as a Java2D {@link java.awt.Shape}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class Polygon extends Polyline {
    /**
     * Creates a new polygon with the given coordinates.
     * The {@code coordinates} array shall not be empty.
     *
     * @param  coordinates  the coordinate values as (x,y) tuples.
     * @param  size         number of valid value in {@code coordinates} array.
     */
    Polygon(final double[] coordinates, final int size) {
        super(coordinates, size);
    }
}
