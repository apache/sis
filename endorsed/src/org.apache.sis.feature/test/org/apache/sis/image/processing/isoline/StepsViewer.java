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
import java.util.EnumMap;
import java.awt.Shape;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.EventQueue;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import javax.swing.Timer;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.ButtonModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.function.BiConsumer;
import java.util.concurrent.CountDownLatch;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 * A viewer for showing isoline generation step-by-step.
 * For enabling the use of this class, temporarily remove {@code private} and {@code final} keywords in
 * {@link Isolines#LISTENER}, then uncomment the {@link #setListener(StepsViewer)} constructor body.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("serial")
public final class StepsViewer extends JComponent implements BiConsumer<String,Isolines>, ChangeListener, ActionListener {
    /**
     * Sets the component to be notified after each row of isolines generated from the rendered image.
     * The body of this method is commented-out because {@link Isolines#LISTENER} is private and final.
     * The body should be uncommented only temporarily during debugging phases.
     */
    private static void setListener(final StepsViewer listener) {
        // Isolines.LISTENER = listener;
    }

    /**
     * Entry point for debugging. Edit this method body as needed for loading an image to use as test data.
     *
     * @param  args  ignored.
     * @throws Exception if an error occurred during I/O or isoline generation.
     */
    public static void main(final String[] args) throws Exception {
        // showStepByStep(local.test.DebugIsoline.data(), 0);
    }

    /**
     * Size of the window and spacing between borders and isolines. All values are in pixels.
     */
    private static final int CANVAS_WIDTH = 1600, CANVAS_HEIGHT = 1000, PADDING = 3;

    /**
     * Whether to flip X and/or Y axis.
     */
    private static final boolean FLIP_X = false, FLIP_Y = true;

    /**
     * Description of current step. This title is updated at each isoline generation step,
     * when {@link #accept(String, Shape)} is invoked.
     */
    private final JLabel stepTitle;

    /**
     * The button for moving to the next step. When this button is enabled, the isoline process is blocked
     * by {@link #blocker} until this button is pressed. When this button is pressed, the isoline process
     * continue until {@link #accept(String, Shape)} is invoked again.
     *
     * @see #actionPerformed(ActionEvent)
     */
    private final JButton next;

    /**
     * Simulate a "next" action after some delay. This is used when users keep the "Next" button pressed.
     */
    private final Timer delayedNext;

    /**
     * Blocks the isoline computation thread until the user is ready to see the next step.
     */
    private CountDownLatch blocker;

    /**
     * The isolines to show.
     */
    private final Map<PolylineStage,Path2D> isolines;

    /**
     * The colors to associate to the isoline for each stage.
     * Array indices are {@link PolylineStage#ordinal()} values.
     */
    private final Color[] stageColors;

    /**
     * Bounds of {@link #isolines}, slightly expanded for making easier to see.
     */
    private Rectangle bounds;

    /**
     * Conversion from pixel indices in the source image to pixel indices in the displayed window.
     */
    private final AffineTransform2D sourceToCanvas;

    /**
     * The image to show in background. This image has the size of the canvas.
     */
    private final BufferedImage background;

    /**
     * The final result returned by public API.
     */
    private Shape result;

    /**
     * Creates a new viewer.
     *
     * @param  data  the source of data for isolines.
     * @param  pane  the container where to add components.
     */
    private StepsViewer(final RenderedImage data, final Container pane) {
        isolines    = new EnumMap<>(PolylineStage.class);
        stageColors = new Color[] {Color.YELLOW, Color.PINK, Color.BLUE};
        /*
         * Computes a transform from indices in the data matrix to pixel coordinates in the canvas.
         */
        setMaximumSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
        final double scaleX = (CANVAS_WIDTH  - 2*PADDING) / (double) data.getWidth();
        final double scaleY = (CANVAS_HEIGHT - 2*PADDING) / (double) data.getHeight();
        sourceToCanvas = new AffineTransform2D(
                FLIP_X ? -scaleX : scaleX, 0, 0, FLIP_Y ? -scaleY : scaleY,
                scaleX * (PADDING + data.getMinX() + (FLIP_X ? data.getWidth()  : 0)),
                scaleY * (PADDING + data.getMinY() + (FLIP_Y ? data.getHeight() : 0)));
        /*
         * Creates a background image as a grayscale image with pixel values in the range 0 to 64.
         * It produces a dark image for making the isolines (in bright colors) easier to see.
         */
        background = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        {   // For keeping variable locale.
            final Graphics2D gh = background.createGraphics();
            gh.drawRenderedImage(data, sourceToCanvas);
            gh.dispose();
            final WritableRaster r = background.getRaster();
            int min = 256, max = 2;
            for (int y=0; y<CANVAS_HEIGHT; y++) {
                for (int x=0; x<CANVAS_WIDTH; x++) {
                    final int value = r.getSample(x, y, 0);
                    if (value != 0) {
                        if (value < min) min = value;
                        if (value > max) max = value;
                    }
                }
            }
            if (--min < max) {
                final float scale = 128f / (max - min);
                for (int y=0; y<CANVAS_HEIGHT; y++) {
                    for (int x=0; x<CANVAS_WIDTH; x++) {
                        final int value = r.getSample(x, y, 0);
                        r.setSample(x, y, 0, Math.max(Math.round(scale * (value - min)), 0));
                    }
                }
            }
        }
        /*
         * Swing controls.
         */
        stepTitle = new JLabel();
        next = new JButton("Next");
        next.setEnabled(false);
        next.addActionListener(this);
        next.getModel().addChangeListener(this);
        delayedNext = new Timer(1000, this::fastForward);       // 1 second delay before fast forward.
        delayedNext.setRepeats(false);

        final JPanel bar = new JPanel(new BorderLayout());
        bar .add(stepTitle, BorderLayout.CENTER);
        bar .add(next,      BorderLayout.EAST);
        pane.add(bar,       BorderLayout.NORTH);
        pane.add(this,      BorderLayout.CENTER);
    }

    /**
     * Generates isolines for the given image and show the result step by step.
     * The given image shall have only one band.
     *
     * @param  data    the source of data for isolines.
     * @param  levels  levels of isolones to generate.
     */
    public static void showStepByStep(final RenderedImage data, final double... levels) {
        assertEquals(1, data.getSampleModel().getNumBands(), "Unsupported number of bands.");
        final JFrame frame = new JFrame("Step-by-step isoline viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        final StepsViewer viewer = new StepsViewer(data, frame.getContentPane());
        final Isolines iso;
        try {
            setListener(viewer);
            frame.setVisible(true);
            frame.setSize(CANVAS_WIDTH, CANVAS_HEIGHT);
            iso = Isolines.generate(data, new double[][] {levels}, null)[0];
        } catch (TransformException e) {
            throw new AssertionError(e);        // Should not happen because we specified an identity transform.
        } finally {
            setListener(null);
        }
        final Path2D path = new Path2D.Float();
        for (final Shape shape : iso.polylines().values()) {
            path.append(shape, false);
        }
        path.transform(viewer.sourceToCanvas);
        viewer.result = path;
        viewer.repaint();
    }

    /**
     * Invoked when the isolines need to be drawn.
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D gh = (Graphics2D) g;
        gh.drawRenderedImage(background, new AffineTransform());
        if (bounds != null) {
            gh.setStroke(new BasicStroke(2));
            gh.setColor(Color.ORANGE);
            gh.draw(bounds);
        }
        for (final Map.Entry<PolylineStage,Path2D> entry : isolines.entrySet()) {
            final PolylineStage stage = entry.getKey();
            gh.setStroke(new BasicStroke((result == null) ? (stage == PolylineStage.BUFFER ? 2 : 0) : 5));
            gh.setColor((result == null) ? stageColors[stage.ordinal()] : Color.YELLOW);
            gh.draw(entry.getValue());
        }
        if (result != null) {
            gh.setStroke(new BasicStroke(2));
            gh.setColor(Color.BLUE);
            gh.draw(result);
        }
    }

    /**
     * Returns {@code true} if the shapes described by given iterators are equal.
     * This is used for deciding if it is worth to bother the user with a request
     * for pressing the "Next" button.
     */
    private static boolean equal(final PathIterator it1, final PathIterator it2) {
        final float[] a1 = new float[6];
        final float[] a2 = new float[6];
        while (!it1.isDone()) {
            if (it2.isDone()) return false;
            final int code = it1.currentSegment(a1);
            if (code != it2.currentSegment(a2)) {
                return false;
            }
            int n;
            switch (code) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:  n = 2; break;
                case PathIterator.SEG_QUADTO:  n = 4; break;
                case PathIterator.SEG_CUBICTO: n = 6; break;
                case PathIterator.SEG_CLOSE:   n = 0; break;
                default: throw new AssertionError(code);
            }
            while (--n >= 0) {
                if (Float.floatToIntBits(a1[n]) != Float.floatToIntBits(a2[n])) {
                    return false;
                }
            }
            it1.next();
            it2.next();
        }
        return it2.isDone();
    }

    /**
     * Invoked after a row has been processed during the isoline generation.
     * This is invoked from the main thread (<strong>not</strong> the Swing thread).
     *
     * @param  title      description of current state.
     * @param  generator  new generator of isolines.
     */
    @Override
    public void accept(final String title, final Isolines generator) {
        final Map<PolylineStage, Path2D> paths = generator.toRawPath();
        for (final Map.Entry<PolylineStage,Path2D> entry : paths.entrySet()) {
            entry.getValue().transform(sourceToCanvas);
        }
        try {
            final CountDownLatch c = new CountDownLatch(1);
            EventQueue.invokeLater(() -> {
                Rectangle b = null;
                boolean unchanged = true;
                for (final PolylineStage stage : PolylineStage.values()) {
                    final Path2D current = isolines.get(stage);
                    final Path2D update  = paths.get(stage);
                    if (unchanged && current != update && !(current != null && update != null &&
                            equal(current.getPathIterator(null), update.getPathIterator(null))))
                    {
                        unchanged = false;
                    }
                    if (update == null) {
                        isolines.remove(stage);
                    } else {
                        isolines.put(stage, update);
                        if (stage == PolylineStage.BUFFER) {
                            b = update.getBounds();
                            if (b.isEmpty()) {
                                b = null;
                            } else {
                                b.x      -= PADDING;
                                b.y      -= PADDING;
                                b.width  += PADDING * 2;
                                b.height += PADDING * 2;
                            }
                        }
                    }
                }
                bounds = b;
                if (unchanged) {
                    stepTitle.setText(title + " (no change)");
                    c.countDown();
                } else {
                    stepTitle.setText(title);
                    repaint();
                    assertNull(blocker);
                    if (next.getModel().isPressed()) {
                        c.countDown();
                    } else {
                        blocker = c;
                        next.setEnabled(true);
                    }
                }
            });
            c.await();
        } catch (InterruptedException  e) {
            throw new AssertionError(e);            // Stop the test.
        }
    }

    /**
     * Invoked by Swing when user presses the "Next" button.
     * This method resumes isoline computation.
     *
     * @param  event  ignored.
     */
    @Override
    public void actionPerformed(final ActionEvent event) {
        next.setEnabled(false);
        if (blocker != null) {
            blocker.countDown();
            blocker = null;
        }
    }

    /**
     * Invoked when the "Next" button is kept pressed.
     * The effect is to start the "fast forward" mode.
     * This method shall be invoked in Swing thread.
     *
     * @param  event  ignored.
     */
    private void fastForward(final ActionEvent event) {
        if (next.getModel().isPressed()) {
            if (blocker != null) {
                blocker.countDown();
                blocker = null;
            }
        }
    }

    /**
     * Invoked by Swing when the state of the "Next" button (pressed or not) changed.
     * If the button is pressed one second without being released, then we enter a
     * "fast forward" mode until the button is released.
     *
     * @param  event  ignored.
     */
    @Override
    public void stateChanged(final ChangeEvent event) {
        final ButtonModel m = (ButtonModel) event.getSource();
        if (m.isPressed()) {
            delayedNext.restart();
        } else {
            delayedNext.stop();
        }
    }
}
