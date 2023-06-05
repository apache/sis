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
package org.apache.sis.internal.referencing.j2d;

import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link AbstractShape} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 */
public final class AbstractShapeTest extends TestCase {
    /**
     * Tests {@link ShapeUtilities#isFloat(Object)}.
     */
    @Test
    public void testIsFloat() {
        assertTrue (AbstractShape.isFloat(new Point2D.Float()));
        assertFalse(AbstractShape.isFloat(new Point2D.Double()));
        assertTrue (AbstractShape.isFloat(new Line2D.Float()));
        assertFalse(AbstractShape.isFloat(new Line2D.Double()));
    }
}
