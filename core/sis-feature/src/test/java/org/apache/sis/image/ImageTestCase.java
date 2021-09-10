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

import java.io.File;
import java.io.IOException;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.ImagingOpException;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestConfiguration;
import org.junit.AfterClass;

import static java.lang.StrictMath.round;
import static org.junit.Assert.assertNotNull;


/**
 * Base class for tests applied on images. This base class provides a {@link #viewEnabled}
 * field initialized to {@code false}. If this field is set to {@code true}, then calls to
 * the {@link #showCurrentImage(String)} method will show the {@linkplain #image}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public abstract strictfp class ImageTestCase extends TestCase {
    /**
     * Small value for comparisons of sample values. Since most grid coverage implementations in
     * Apache SIS store real values as {@code float} numbers, this {@code SAMPLE_TOLERANCE} value
     * must be of the order of magnitude of {@code float} precision, not {@code double}.
     *
     * The current value is {@code Math.ulp(255f)}.
     * This is okay for tests on sample values in the (-256 … 256) range where 256 is exclusive.
     */
    protected static final float SAMPLE_TOLERANCE = 1.5258789E-5f;

    /**
     * The image being tested, or {@code null} if not yet defined.
     */
    protected RenderedImage image;

    /**
     * Set to {@code true} for enabling the display of test images. The default value is {@code false},
     * unless the {@value TestConfiguration#SHOW_WIDGET_KEY} system property has been set to {@code "true"}.
     *
     * @see TestConfiguration#SHOW_WIDGET_KEY
     */
    protected boolean viewEnabled;

    /**
     * Set to {@code true} if we have shown at least one image.
     * This is used for avoiding useless {@link TestViewer} class loading in {@link #waitForFrameDisposal()}.
     */
    private static volatile boolean viewUsed;

    /**
     * Creates a new test case.
     */
    protected ImageTestCase() {
        viewEnabled = Boolean.getBoolean(TestConfiguration.SHOW_WIDGET_KEY);
    }

    /**
     * Displays the current {@linkplain #image} if {@link #viewEnabled} is set to {@code true},
     * otherwise does nothing. This method is mostly for debugging purpose.
     *
     * @param title the window title.
     */
    protected final synchronized void showCurrentImage(final String title) {
        final RenderedImage image = this.image;
        assertNotNull("An image must be set.", image);
        if (viewEnabled) {
            viewUsed = true;
            TestViewer.show(title, image);
        }
    }

    /**
     * Saves the current image as a PNG image in the given file. This is sometime useful for visual
     * check purpose, and is used only as a helper tools for tuning the test suites. Floating-point
     * images are converted to grayscale before to be saved.
     *
     * @param  filename  the name (optionally with its path) of the file to create.
     * @throws ImagingOpException if an error occurred while writing the file.
     */
    protected final synchronized void saveCurrentImage(final String filename) throws ImagingOpException {
        try {
            savePNG(image, new File(filename));
        } catch (IOException e) {
            throw new ImagingOpException(e.toString());
        }
    }

    /**
     * Implementation of {@link #saveCurrentImage(String)}, to be shared by the widget
     * shown by {@link #showCurrentImage(String)}.
     */
    static void savePNG(final RenderedImage image, final File file) throws IOException {
        assertNotNull("An image must be set.", image);
        if (!ImageIO.write(image, "png", file)) {
            savePNG(image.getData(), file);
        }
    }

    /**
     * Saves the first band of the given raster as a PNG image in the given file.
     * This is sometime useful for visual check purpose, and is used only as a helper tools
     * for tuning the test suites. The raster is converted to grayscale before to be saved.
     *
     * @param  raster  the raster to write in PNG format.
     * @param  file    the file to create.
     * @throws IOException if an error occurred while writing the file.
     */
    private static void savePNG(final Raster raster, final File file) throws IOException {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        final int xmin   = raster.getMinX();
        final int ymin   = raster.getMinY();
        final int width  = raster.getWidth();
        final int height = raster.getHeight();
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                final float value = raster.getSampleFloat(x + xmin, y + ymin, 0);
                if (value < min) min = value;
                if (value > min) max = value;
            }
        }
        final float scale = 255 / (max - min);
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster dest = image.getRaster();
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                final double value = raster.getSampleDouble(x + xmin, y + ymin, 0);
                dest.setSample(x, y, 0, round((value - min) * scale));
            }
        }
        if (!ImageIO.write(image, "png", file)) {
            throw new IIOException("No suitable PNG writer found.");
        }
    }

    /**
     * If a frame has been created by {@link #showCurrentImage(String)},
     * waits for its disposal before to move to the next test class.
     */
    @AfterClass
    public static void waitForFrameDisposal() {
        if (viewUsed) {
            TestViewer.waitForFrameDisposal();
        }
    }
}
