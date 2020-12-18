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
package org.apache.sis.swing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import org.apache.sis.test.visual.Visualization;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Base class for tests on widgets. When executed as a JUnit test, this class displays nothing.
 * It merely:
 *
 * <ul>
 *   <li>Ensure that no exception is thrown while creating the widget.</li>
 *   <li>Ensure that no exception is thrown while painting in a buffered image.</li>
 * </ul>
 *
 * However subclasses provide a main method for running also the test as a classical application,
 * in which case the widget is shown. Note that {@code TestCase} is only for "testing the tests",
 * i.e. tests the widgets used for testing Apache SIS. We do not distribute those widgets,
 * so tests of the {@code org.apache.sis.swing} package do not need to be extensive.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
public abstract strictfp class TestCase {
    /**
     * The type of the widget being tested.
     */
    private final Class<?> testing;

    /**
     * Number of invocation of {@link #create(int)} to perform.
     */
    private final int numTests;

    /**
     * Creates a new instance of {@code TestCase} which will invoke {@link #create(int)} only once.
     *
     * @param  testing  the class of widget being tested.
     */
    protected TestCase(final Class<? extends JComponent> testing) {
        this(testing, 1);
    }

    /**
     * Creates a new instance of {@code TestCase}.
     *
     * @param  testing   the class of widget being tested.
     * @param  numTests  number of invocation of {@link #create(int)} to perform.
     */
    protected TestCase(final Class<? extends JComponent> testing, final int numTests) {
        assertTrue(JComponent.class.isAssignableFrom(testing));
        assertTrue(numTests >= 1);
        this.testing  = testing;
        this.numTests = numTests;
    }

    /**
     * Creates the widget.
     *
     * @param  index  widget index from 0 inclusive to the value given at construction time, exclusive.
     * @return the widget to show.
     */
    protected abstract JComponent create(int index);

    /**
     * Creates the widget and paints in a buffered image. This test is useful only for widget
     * implementations that override their {@link JComponent#paint(Graphics)} method.
     */
    @Test
    public final void testPaint() {
        assertTrue(testing.desiredAssertionStatus());
        for (int i=0; i<numTests; i++) {
            final JComponent component = create(i);
            component.setSize(component.getPreferredSize());
            component.setVisible(true);
            final int width  = component.getWidth();
            final int height = component.getHeight();
            final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D gr = image.createGraphics();
            try {
                component.print(gr);
            } finally {
                gr.dispose();
            }
        }
    }

    /**
     * Shows the widget. This method can be invoked from the {@code main} method of subclasses.
     */
    public final void show() {
        new Visualization(testing) {
            @Override protected JComponent create(int index) {
                return TestCase.this.create(index);
            }
        }.show();
    }
}
