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
package org.apache.sis.internal.feature.jts;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * A thin wrapper that adapts a JTS geometry to the Shape interface so that the geometry can be used
 * by java2d without coordinate cloning.
 *
 * @author Johann Sorel (Puzzle-GIS + Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
abstract class AbstractJTSShape<T extends Geometry> implements Shape, Cloneable {

    static final Logger LOGGER = Logging.getLogger("org.geotoolkit.geometry");

    /** The wrapped JTS geometry */
    protected final T geometry;

    /** An additional AffineTransform */
    protected final MathTransform transform;

    public AbstractJTSShape(final T geom) {
        this(geom, null);
    }

    /**
     * Creates a new GeometryJ2D object.
     *
     * @param geom - the wrapped geometry
     */
    public AbstractJTSShape(final T geom, final MathTransform trs) {
        this.geometry = geom;
        this.transform = (trs == null) ? JTSPathIterator.IDENTITY : trs;
    }

    /**
     * @return the current wrapped geometry
     */
    public T getGeometry() {
        return geometry;
    }

    public MathTransform getTransform() {
        return transform;
    }

    protected MathTransform getInverse(){
        try {
            return transform.inverse();
        } catch (org.opengis.referencing.operation.NoninvertibleTransformException ex) {
            Logging.getLogger("org.geotoolkit.display2d.primitive.jts").log(Level.WARNING, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean contains(final Rectangle2D r) {
        return contains(r.getMinX(),r.getMinY(),r.getWidth(),r.getHeight());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean contains(final Point2D p) {
        final MathTransform inverse = getInverse();
        if (inverse != null) {
            final double[] a = new double[]{p.getX(), p.getY()};
            safeTransform(inverse, a, a);
            final Coordinate coord = new Coordinate(a[0], a[1]);
            final Geometry point = geometry.getFactory().createPoint(coord);
            return geometry.contains(point);
        }

        //inverse transform could not be computed
        //fallback on AWT geometries
        return new GeneralPath(this).contains(p);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean contains(final double x, final double y) {
        return contains(new Point2D.Double(x, y));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean contains(final double x, final double y, final double w, final double h) {
        return intersectOrContains(x, y, w, h, false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Rectangle2D getBounds2D() {
        if (geometry == null) return null;

        final Envelope env = geometry.getEnvelopeInternal();
        final double[] p1 = new double[]{env.getMinX(), env.getMinY()};
        safeTransform(transform,p1, p1);
        final double[] p2 = new double[]{env.getMaxX(), env.getMaxY()};
        safeTransform(transform,p2, p2);

        final Rectangle2D rect = new Rectangle2D.Double(p1[0], p1[1], 0, 0);
        rect.add(p2[0],p2[1]);
        return rect;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PathIterator getPathIterator(final AffineTransform at, final double flatness) {
        return getPathIterator(at);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean intersects(final Rectangle2D r) {
        return intersects(r.getX(),r.getY(),r.getWidth(),r.getHeight());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean intersects(final double x, final double y, final double w, final double h) {
        return intersectOrContains(x, y, w, h, true);
    }

    /**
     * Test rectangle intersection or containment.
     *
     * @param x left coordinate
     * @param y bottom coordinate
     * @param w width
     * @param h height
     * @param intersect true for intersection, false for contains
     * @return true
     */
    private boolean intersectOrContains(final double x, final double y, final double w, final double h, boolean intersect) {
        final MathTransform inverse = getInverse();
        if (inverse != null) {
            final double[] p1 = new double[]{x, y};
            safeTransform(inverse, p1, p1);
            final double[] p2 = new double[]{x + w, y + h};
            safeTransform(inverse, p2, p2);

            final Coordinate[] coords = {
                new Coordinate(p1[0], p1[1]),
                new Coordinate(p1[0], p2[1]),
                new Coordinate(p2[0], p2[1]),
                new Coordinate(p2[0], p1[1]),
                new Coordinate(p1[0], p1[1])
            };
            final LinearRing lr = geometry.getFactory().createLinearRing(coords);
            final Geometry rect = geometry.getFactory().createPolygon(lr, null);
            return intersect ? geometry.intersects(rect) : geometry.contains(rect);
        }

        //inverse transform could not be computed
        //fallback on AWT geometries
        final GeneralPath path = new GeneralPath(this);
        return intersect ? path.intersects(x, y, w, h) : path.contains(x, y, w, h);
    }

    @Override
    public AbstractJTSShape clone()  {
        return null; //TODO
    }


    protected void safeTransform(MathTransform trs, double[] in, double[] out) {
        try {
            trs.transform(in, 0, out, 0, 1);
        } catch (TransformException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(),ex);
            Arrays.fill(out, Double.NaN);
        }
    }

    protected void safeTransform(double[] in, int offset, float[] out, int outOffset, int nb) {
        try {
            transform.transform(in, offset, out, outOffset, nb);
        } catch (TransformException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(),ex);
            Arrays.fill(out, outOffset, outOffset+nb*2, Float.NaN);
        }
    }

    protected void safeTransform(double[] in, int offset, double[] out, int outOffset, int nb) {
        try {
            transform.transform(in, offset, out, outOffset, nb);
        } catch (TransformException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(),ex);
            Arrays.fill(out, outOffset, outOffset+nb*2, Double.NaN);
        }
    }
}
