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

import java.util.Locale;
import java.util.Random;
import java.io.Console;
import java.io.PrintWriter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.apache.sis.util.CharSequences;


/**
 * Visual tests of the {@link ShapeUtilities} class. This "test" is not executed by the Maven build.
 * It is rather designed for explicit execution from an IDE or the command line for visual inspection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@SuppressWarnings("serial")
final strictfp class ShapeUtilitiesViewer extends JPanel {
    /**
     * The {@link ShapeUtilities} methods to test.
     */
    private static enum Method {
        INTERSECTION_POINT,
        NEAREAST_COLINEAR_POINT,
        COLINEAR_POINT,
        FIT_PARABOL,
        FIT_HORIZONTAL_PARABOL,
        CIRCLE_CENTRE
    };

    /**
     * Radius (in pixels) of points to drawn.
     */
    private static final int POINT_RADIUS = 4;

    /**
     * The visual representation of the arguments given to the tested method.
     */
    private final Path2D input;

    /**
     * The visual representation of the value returned by the tested method.
     */
    private final Path2D output;

    /**
     * {@code true} if we should fill the output, or {@code false} for drawing the contour only.
     */
    private boolean fillOutput;

    /**
     * Random number generator to use in the visual test.
     */
    private final Random random;

    /**
     * Where to write the coordinate points.
     */
    private final PrintWriter out;

    /**
     * Creates a new panel where to paint the input and output values.
     */
    private ShapeUtilitiesViewer() {
        setBackground(Color.BLACK);
        input  = new Path2D.Float();
        output = new Path2D.Float();
        random = new Random();
        final Console console = System.console();
        out = (console != null) ? console.writer() : new PrintWriter(System.out);
    }

    /**
     * Append the given point to the given path.
     */
    private static void addPoint(final Path2D addTo, final double x, final double y) {
        addTo.append(new Ellipse2D.Double(x - POINT_RADIUS, y - POINT_RADIUS, 2*POINT_RADIUS, 2*POINT_RADIUS), false);
    }

    /**
     * Append the given point to the given path.
     */
    private static void addPoint(final Path2D addTo, final Point2D point) {
        if (point != null) {
            addPoint(addTo, point.getX(), point.getY());
        }
    }

    /**
     * Assigns random values to the points.
     */
    @SuppressWarnings("fallthrough")
    final void assignRandomPoints(final Method method) {
        input .reset();
        output.reset();
        fillOutput = true;
        boolean horizontal = false;
        final int x      = getX();
        final int y      = getY();
        final int width  = getWidth();
        final int height = getHeight();
        final int x1 = x + random.nextInt(width);
        final int y1 = y + random.nextInt(height);
        final int x2 = x + random.nextInt(width);
        final int y2 = y + random.nextInt(height);
        final int x3 = x + random.nextInt(width);
        final int y3 = y + random.nextInt(height);
        final int x4 = x + random.nextInt(width);
        final int y4 = y + random.nextInt(height);
        switch (method) {
            case INTERSECTION_POINT: {
                input.moveTo(x1, y1);
                input.lineTo(x2, y2);
                input.moveTo(x3, y3);
                input.lineTo(x4, y4);
                addPoint(output, ShapeUtilities.intersectionPoint(x1, y1, x2, y2, x3, y3, x4, y4));
                out.printf(Locale.ENGLISH, "intersectionPoint(%d, %d, %d, %d, %d, %d, %d, %d)%n", x1, y1, x2, y2, x3, y3, x4, y4);
                break;
            }
            case NEAREAST_COLINEAR_POINT: {
                input.moveTo(x1, y1);
                input.lineTo(x2, y2);
                addPoint(input, x3, y3);
                addPoint(output, ShapeUtilities.nearestColinearPoint(x1, y1, x2, y2, x3, y3));
                out.printf(Locale.ENGLISH, "nearestColinearPoint(%d, %d, %d, %d, %d, %d)%n", x1, y1, x2, y2, x3, y3);
                break;
            }
            case COLINEAR_POINT: {
                final double distance = StrictMath.hypot(x4, y4);
                input.moveTo(x1, y1);
                input.lineTo(x2, y2);
                addPoint(input, x3, y3);
                addPoint(output, ShapeUtilities.colinearPoint(x1, y1, x2, y2, x3, y3, distance));
                out.printf(Locale.ENGLISH, "colinearPoint(%d, %d, %d, %d, %d, %d, %g)%n", x1, y1, x2, y2, x3, y3, distance);
                break;
            }
            case FIT_HORIZONTAL_PARABOL: {
                horizontal = true;
                // Fall through
            }
            case FIT_PARABOL: {
                addPoint(input, x1, y1);
                addPoint(input, x2, y2);
                addPoint(input, x3, y3);
                output.append(ShapeUtilities.fitParabol(x1, y1, x2, y2, x3, y3, horizontal), false);
                out.printf(Locale.ENGLISH, "fitParabol(%d, %d, %d, %d, %d, %d, %b)%n", x1, y1, x2, y2, x3, y3, horizontal);
                fillOutput = false;
                break;
            }
            case CIRCLE_CENTRE: {
                addPoint(input, x1, y1);
                addPoint(input, x2, y2);
                addPoint(input, x3, y3);
                addPoint(output, ShapeUtilities.circleCentre(x1, y1, x2, y2, x3, y3));
                out.printf(Locale.ENGLISH, "circleCentre(%d, %d, %d, %d, %d, %d)%n", x1, y1, x2, y2, x3, y3);
                break;
            }
        }
        out.flush();
        repaint();
    }

    /**
     * Paints the visual test.
     *
     * @param graphics Where to paint the test.
     */
    @Override
    protected void paintComponent(final Graphics graphics) {
        super.paintComponent(graphics);
        final Graphics2D g = (Graphics2D) graphics;
        if (fillOutput) {
            g.setColor(Color.ORANGE);
            g.fill(output);
        }
        g.setColor(Color.RED);
        g.draw(output);
        g.setColor(Color.CYAN);
        g.fill(input);
        g.setColor(Color.BLUE);
        g.draw(input);
    }

    /**
     * Creates a button for testing with new points.
     */
    private JButton createButtonForNextTest(final Method method) {
        final JButton button = new JButton(CharSequences.camelCaseToSentence(
                method.name().toLowerCase(Locale.ENGLISH)).toString());
        button.addActionListener(new ActionListener () {
            @Override public void actionPerformed(ActionEvent e) {
                assignRandomPoints(method);
            }
        });
        return button;
    }

    /**
     * Shows the viewer.
     *
     * @param args Ignored.
     */
    public static void main(final String[] args) {
        final ShapeUtilitiesViewer viewer = new ShapeUtilitiesViewer();
        final JPanel buttons = new JPanel(new GridLayout(2,3));
        for (final Method method : Method.values()) {
            buttons.add(viewer.createButtonForNextTest(method));
        }
        final JFrame frame = new JFrame("ShapeUtilities");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(viewer, BorderLayout.CENTER);
        frame.add(buttons, BorderLayout.SOUTH);
        frame.setSize(600, 400);
        frame.setVisible(true);
    }
}
