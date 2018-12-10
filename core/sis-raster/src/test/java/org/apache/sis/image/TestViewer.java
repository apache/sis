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
import java.util.concurrent.CountDownLatch;
import java.lang.reflect.InvocationTargetException;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import static java.lang.StrictMath.*;


/**
 * A viewer for images being tested. This can be used for visual verification.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final strictfp class TestViewer {
    /**
     * The global image viewer where to collect all test images.
     */
    private static volatile TestViewer global;

    /**
     * Shows the given image.
     *
     * @param  title  title of the new internal window.
     * @param  image  image to display in the internal window.
     */
    static void show(final String title, final RenderedImage image) {
        try {
            EventQueue.invokeAndWait(() -> {
                TestViewer viewer = global;
                if (viewer == null) {
                    viewer = new TestViewer("Apache SIS tests");
                    global = viewer;
                }
                viewer.addImage(image, String.valueOf(title));
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * A lock used for waiting that at least one frame has been closed.
     */
    private final CountDownLatch lock;

    /**
     * The frame showing the images.
     */
    private final JFrame frame;

    /**
     * The desktop pane where to show each images.
     */
    private final JDesktopPane desktop;

    /**
     * The location of the next internal frame to create.
     */
    private int location;

    /**
     * Creates a new viewer and show it immediately.
     */
    private TestViewer(final String title) {
        lock = new CountDownLatch(1);
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                frame.removeWindowListener(this);
                lock.countDown();
                frame.dispose();
            }
        });
        desktop = new JDesktopPane();
        desktop.setSize(800, 600);
        final JMenuBar menuBar = new JMenuBar();
        {
            final JMenu menu = new JMenu("File");
            if (true) {
                final JMenuItem item = new JMenuItem("Save as PNG");
                item.addActionListener((ActionEvent e) -> savePNG());
                menu.add(item);
            }
            menuBar.add(menu);
        }
        frame.setJMenuBar(menuBar);
        frame.add(desktop);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Creates and shows a new internal frame for the given image.
     */
    private void addImage(final RenderedImage image, final String title) {
        final JInternalFrame internal = new JInternalFrame(title, true, true);
        internal.add(new ImagePanel(image));
        internal.pack();
        internal.show();
        desktop.add(internal);
        if (location > min(desktop.getWidth()  - internal.getWidth(),
                           desktop.getHeight() - internal.getHeight()))
        {
            location = 0;
        }
        internal.setLocation(location, location);
        location += 30;
        internal.toFront();
    }


    /**
     * A panel showing an image. Created by {@link #addImage(RenderedImage, String)}.
     */
    @SuppressWarnings("serial")
    private static final strictfp class ImagePanel extends JPanel {
        /** The image to show. */
        private final RenderedImage image;

        /** Creates a viewer for the given image. */
        ImagePanel(final RenderedImage image) {
            this.image = image;
            setPreferredSize(new Dimension(max(300, image.getWidth()), max(30, image.getHeight())));
        }

        /** Paints the image. */
        @Override public void paint(final Graphics graphics) {
            super.paint(graphics);
            final Graphics2D gr = (Graphics2D) graphics;
            gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            final double width  = image.getWidth();
            final double height = image.getHeight();
            final double scale  = min(getWidth() / width, getHeight() / height);
            final AffineTransform gridToPanel = new AffineTransform(
                    scale, 0, 0, scale,
                    0.5*(getWidth()  - scale*width),
                    0.5*(getHeight() - scale*height));
            gr.drawRenderedImage(image, gridToPanel);
        }
    }

    /**
     * Returns the image of the currently selected frame.
     */
    private RenderedImage getSelectedImage() {
        final JInternalFrame frame = desktop.getSelectedFrame();
        if (frame != null) {
            return ((ImagePanel) frame.getContentPane().getComponent(0)).image;
        }
        return null;
    }

    /**
     * Saves the image of the currently selected frame.
     */
    private void savePNG() {
        final RenderedImage image = getSelectedImage();
        if (image != null) {
            final File file = new File(System.getProperty("user.home"), "ImageTest.png");
            final String title, message;
            final int type;
            if (file.exists()) {
                type    = JOptionPane.WARNING_MESSAGE;
                title   = "Confirm overwrite";
                message = "File " + file + " exists. Overwrite?";
            } else {
                type    = JOptionPane.QUESTION_MESSAGE;
                title   = "Confirm write";
                message = "Save in " + file + '?';
            }
            if (JOptionPane.showInternalConfirmDialog(desktop, message, title,
                    JOptionPane.YES_NO_OPTION, type) == JOptionPane.OK_OPTION)
            {
                try {
                    ImageTestCase.savePNG(image, file);
                } catch (IOException e) {
                    JOptionPane.showInternalMessageDialog(desktop, e.toString(),
                            "Error", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }

    /**
     * Waits for the frame disposal.
     */
    static void waitForFrameDisposal() {
        final TestViewer viewer = global;
        if (viewer != null) try {
            viewer.lock.await();
            global = null;
        } catch (InterruptedException e) {
            // It is okay to continue. JUnit will close all windows.
        }
    }
}
