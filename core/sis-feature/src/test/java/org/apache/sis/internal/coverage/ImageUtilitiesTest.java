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
package org.apache.sis.internal.coverage;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ImageUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class ImageUtilitiesTest extends TestCase {
    /**
     * Tests {@link ImageUtilities#bandNames(RenderedImage)}.
     */
    @Test
    public void testBandNames() {
        RenderedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        assertArrayEquals(new short[] {
            Vocabulary.Keys.Red,
            Vocabulary.Keys.Green,
            Vocabulary.Keys.Blue,
            Vocabulary.Keys.Transparency
        }, ImageUtilities.bandNames(image));
        /*
         * Same as above, but without alpha channel.
         */
        image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        assertArrayEquals(new short[] {
            Vocabulary.Keys.Red,
            Vocabulary.Keys.Green,
            Vocabulary.Keys.Blue
        }, ImageUtilities.bandNames(image));
        /*
         * Same as above but in sample values packed in reverse order. Note that while values
         * are packed in BGR order, the sample model is still providing the values in RGB order.
         * For that reason, the band order below is the same than in above test.
         */
        image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_BGR);
        assertArrayEquals(new short[] {
            Vocabulary.Keys.Red,
            Vocabulary.Keys.Green,
            Vocabulary.Keys.Blue
        }, ImageUtilities.bandNames(image));
        /*
         * One-banded image.
         */
        image = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
        assertArrayEquals(new short[] {
            Vocabulary.Keys.Gray
        }, ImageUtilities.bandNames(image));
    }
}
