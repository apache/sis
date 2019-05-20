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
package org.apache.sis.test.widget;

import java.awt.Shape;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.apache.sis.test.TestConfiguration;


/**
 * Methods showing windows for performing visual checks.
 * Methods in this class block until user closes the window.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class VisualCheck {
    /**
     * Whether to show widgets.
     */
    public static final boolean SHOW_WIDGET = Boolean.getBoolean(TestConfiguration.SHOW_WIDGET_KEY);

    /**
     * Do not allows instantiation of this class.
     */
    private VisualCheck() {
    }

    /**
     * Visualizes the given shapes in a window. The shapes are resized to fill most of the window,
     * with <var>y</var> axis oriented toward up. The bounding box is drawn in gray color behind the shapes.
     *
     * @param  shapes  the shapes to visualize.
     */
    public static void show(final Shape... shapes) {
        show(new ShapeViewer(shapes));
    }

    /**
     * Shows the given panel and blocks current thread until user closes the window.
     */
    private static void show(final JPanel viewer) {
        final WindowAdapter lock = new WindowAdapter() {
            @Override public synchronized void windowClosed(WindowEvent e) {
                notifyAll();
            }
        };
        final JFrame frame = new JFrame("ShapeViewer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(lock);
        frame.add(viewer);
        frame.setSize(600, 400);
        frame.setVisible(true);
        try {
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            // Ignore.
        }
    }
}
