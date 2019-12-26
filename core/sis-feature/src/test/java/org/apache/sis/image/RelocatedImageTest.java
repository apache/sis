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
package org.apache.sis.image;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.opengis.coverage.grid.SequenceType;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link RelocatedImage} implementation.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class RelocatedImageTest extends TestCase {

    @Test
    public void iteratorTest() {
        final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY);
        image.getRaster().setSample(0, 0, 0, 1);
        image.getRaster().setSample(1, 0, 0, 2);
        image.getRaster().setSample(0, 1, 0, 3);
        image.getRaster().setSample(1, 1, 0, 4);

        final RenderedImage trs = RelocatedImage.moveTo(image, -10, -20);

        final PixelIterator ite = new PixelIterator.Builder().setIteratorOrder(SequenceType.LINEAR).create(trs);
        assertTrue(ite.next());
        assertEquals(new Point(-10, -20), ite.getPosition());
        assertEquals(1, ite.getSample(0));
        assertTrue(ite.next());
        assertEquals(new Point(-9, -20), ite.getPosition());
        assertEquals(2, ite.getSample(0));
        assertTrue(ite.next());
        assertEquals(new Point(-10, -19), ite.getPosition());
        assertEquals(3, ite.getSample(0));
        assertTrue(ite.next());
        assertEquals(new Point(-9, -19), ite.getPosition());
        assertEquals(4, ite.getSample(0));
        assertFalse(ite.next());
    }
}
