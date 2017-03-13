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
package org.apache.sis.referencing.gazetteer;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.awt.Shape;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.gazetteer.Location;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.j2d.IntervalRectangle;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Debug;


/**
 * A Swing panel drawing {@link Location} instances.
 * This is used for debugging purpose only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@Debug
@SuppressWarnings("serial")
public final class LocationViewer extends JPanel {
    /**
     * Shows the locations tested by {@link MilitaryGridReferenceSystemTest#testIteratorNorthUTM()}.
     *
     * @param  args  ignored.
     * @throws Exception if an error occurred while transforming an envelope to the display CRS.
     */
    public static void main(final String[] args) throws Exception {
        final MilitaryGridReferenceSystem.Coder coder = new MilitaryGridReferenceSystem().createCoder();
        coder.setPrecision(100000);
        switch (1) {
            /*
             * UTM North: 3 zones (31, 32 and 33) and 3 latitude bands (T, U and V).
             * Include the Norway special case: in latitude band V, zone 32 is widened at the expense of zone 31.
             */
            case 1: {
                show(coder, new Envelope2D(CommonCRS.defaultGeographic(), 5, 47, 8, 10), CommonCRS.WGS84.universal(1, 9));
                break;
            }
            /*
             * UTM South: 3 zones (31, 32 and 33) and 2 latitude bands (G and H).
             */
            case 2: {
                show(coder, new Envelope2D(CommonCRS.defaultGeographic(), 5, -42, 8, 4), CommonCRS.WGS84.universal(1, 9));
                break;
            }
            /*
             * Crossing the anti-meridian. There is two columns of cells: on the west side and on the east side.
             */
            case 3: {
                final GeneralEnvelope ge = new GeneralEnvelope(CommonCRS.defaultGeographic());
                ge.setRange(0, 170, -175);
                ge.setRange(1,  40,   42);
                show(coder, ge, null);
                break;
            }
            /*
             * Polar case.
             */
            case 4: {
                show(coder, new Envelope2D(CommonCRS.defaultGeographic(), -180, 80, 360, 10), CommonCRS.WGS84.universal(90, 9));
                break;
            }
        }
    }

    /**
     * The coordinate reference system to use for displaying the shapes.
     */
    private final SingleCRS displayCRS;

    /**
     * The envelope projected to {@link #displayCRS}.
     */
    private Shape envelope;

    /**
     * The locations to show, together with their label.
     */
    private final Map<String,Shape> locations;

    /**
     * Bounding box of all shapes in the {@link #locations} map.
     */
    private Rectangle2D bounds;

    /**
     * Creates a new, initially empty, viewer. Locations must be added by calls to {@code addLocation(…)} methods
     * before the widget can be show.
     *
     * @param displayCRS  the coordinate reference system to use for displaying the location shapes.
     */
    public LocationViewer(final SingleCRS displayCRS) {
        this.displayCRS = displayCRS;
        this.locations  = new LinkedHashMap<>();
        setBackground(Color.BLACK);
    }

    /**
     * Shows all locations in the given area of interest.
     *
     * @param  coder           the encoder to use for computing locations and their envelopes.
     * @param  areaOfInterest  the geographic or projected area where to get locations.
     * @param  displayCRS      the CRS to use for displaying the location shapes, or {@code null} for the envelope CRS.
     * @throws FactoryException if a transformation to the display CRS can not be obtained.
     * @throws TransformException if an error occurred while transforming an envelope.
     */
    public static void show(final MilitaryGridReferenceSystem.Coder coder, final Envelope areaOfInterest,
            SingleCRS displayCRS) throws FactoryException, TransformException
    {
        if (displayCRS == null) {
            displayCRS = CRS.getHorizontalComponent(areaOfInterest.getCoordinateReferenceSystem());
        }
        final LocationViewer viewer = new LocationViewer(displayCRS);
        viewer.addLocations(coder, areaOfInterest);
        final JFrame frame = new JFrame("Locations viewer (debug)");
        frame.getContentPane().add(viewer);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        frame.setVisible(true);
    }

    /**
     * Adds all locations in the given area of interest.
     *
     * @param  coder           the encoder to use for computing locations and their envelopes.
     * @param  areaOfInterest  the geographic or projected area where to get locations.
     * @throws FactoryException if a transformation to the display CRS can not be obtained.
     * @throws TransformException if an error occurred while transforming an envelope.
     */
    public void addLocations(final MilitaryGridReferenceSystem.Coder coder, final Envelope areaOfInterest)
            throws FactoryException, TransformException
    {
        final Iterator<String> it = coder.encode(areaOfInterest);
        while (it.hasNext()) {
            final String code = it.next();
            addLocation(code, coder.decode(code));
        }
        envelope = ((MathTransform2D) CRS.findOperation(areaOfInterest.getCoordinateReferenceSystem(), displayCRS, null)
                        .getMathTransform()).createTransformedShape(new IntervalRectangle(areaOfInterest));
    }

    /**
     * Adds the location identified by the given label
     *
     * @param  label     a label that identify the location to add.
     * @param  location  the location to add to the list of locations shown by this widget.
     * @throws FactoryException if a transformation to the display CRS can not be obtained.
     * @throws TransformException if an error occurred while transforming an envelope.
     */
    public void addLocation(final String label, final Location location) throws FactoryException, TransformException {
        final Envelope envelope = location.getEnvelope();
        final MathTransform2D tr = (MathTransform2D) CRS.findOperation(
                envelope.getCoordinateReferenceSystem(), displayCRS, null).getMathTransform();
        final Shape shape = tr.createTransformedShape(new IntervalRectangle(envelope));
        if (locations.putIfAbsent(label, shape) != null) {
            throw new IllegalArgumentException("A location is already defined for " + label);
        }
        final Rectangle2D b = shape.getBounds2D();
        if (bounds == null) {
            bounds = b;
        } else {
            bounds.add(b);
        }
    }

    /**
     * Invoked by Swing for painting this widget.
     *
     * @param g  the graphic context where to paint.
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D gr = (Graphics2D) g;
        final AffineTransform oldTr = gr.getTransform();
        final AffineTransform tr = AffineTransform.getScaleInstance(
                getWidth()  / bounds.getWidth(),
               -getHeight() / bounds.getHeight());
        tr.translate(-bounds.getMinX(), -bounds.getMaxY());
        gr.transform(tr);
        gr.setStroke(new BasicStroke(0));
        if (envelope != null) {
            gr.setColor(Color.RED);
            gr.draw(envelope);
        }
        gr.setColor(Color.YELLOW);
        for (final Shape location : locations.values()) {
            gr.draw(location);
        }
        gr.setTransform(oldTr);
        gr.setColor(Color.CYAN);
        final Point2D.Double p = new Point2D.Double();
        for (final Map.Entry<String,Shape> entry : locations.entrySet()) {
            final Rectangle2D b = entry.getValue().getBounds2D();
            p.x = b.getCenterX();
            p.y = b.getCenterY();
            final Point2D pt = tr.transform(p, p);
            final String label = entry.getKey();
            gr.drawString(label, (float) (pt.getX() - 4.5*label.length()), (float) pt.getY());
        }
    }
}
