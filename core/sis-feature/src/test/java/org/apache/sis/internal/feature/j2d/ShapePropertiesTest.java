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

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ShapeProperties}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class ShapePropertiesTest extends TestCase {
    /**
     * Tests with a line segment.
     */
    @Test
    public void testLinestring() {
        ShapeProperties p = new ShapeProperties(new Line2D.Double(10, 20, 50, 45));
        assertEquals("LINESTRING (10 20, 50 45)", p.toWKT(0.1));
    }

    /**
     * Tests with a rectangle.
     */
    @Test
    public void testPolygon() {
        ShapeProperties p = new ShapeProperties(new Rectangle2D.Double(-10, -20, 30, 50));
        assertEquals("POLYGON ((-10 -20, 20 -20, 20 30, -10 30, -10 -20))", p.toWKT(0.1));
    }

    /**
     * Tests with a two rectangles.
     */
    @Test
    public void testMultiPolygon() {
        Path2D.Double path = new Path2D.Double();
        path.append(new Rectangle2D.Double(-1, -2, 3, 6), false);
        path.append(new Rectangle2D.Double(5, 6, 2, 1), false);
        ShapeProperties p = new ShapeProperties(path);
        assertEquals("MULTIPOLYGON (((-1 -2, 2 -2, 2 4, -1 4, -1 -2)), ((5 6, 7 6, 7 7, 5 7, 5 6)))", p.toWKT(0.1));
    }
}
