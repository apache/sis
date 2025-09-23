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
package org.apache.sis.geometries;

import java.awt.Shape;
import java.util.Iterator;
import java.util.OptionalInt;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.DistanceOperatorName;
import org.apache.sis.filter.sqlmm.SQLMM;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.internal.shared.ArraySequence;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.util.Debug;


/**
 * The wrapper of SIS geometries.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class Wrapper extends GeometryWrapper {
    /**
     * The wrapped implementation.
     */
    private final Geometry geometry;

    /**
     * Creates a new wrapper around the given geometry.
     *
     * @param  geometry  the geometry to wrap.
     */
    Wrapper(final Geometry geometry) {
        this.geometry = geometry;
        crs = geometry.getCoordinateReferenceSystem();
    }

    /**
     * Creates a new wrapper with the same <abbr>CRS</abbr> than the given wrapper.
     *
     * @param  source    the source wrapper from which is derived the geometry.
     * @param  geometry  the geometry to wrap.
     */
    private Wrapper(final Wrapper source, final Geometry geometry) {
        this.geometry = geometry;
        this.crs = source.crs;
    }

    /**
     * Returns the implementation-dependent factory of geometric object.
     */
    @Override
    protected Geometries<Geometry> factory() {
        return GeometryFactory.INSTANCE;
    }

    /**
     * Returns the geometry specified at construction time.
     */
    @Override
    protected Object implementation() {
        return geometry;
    }

    /**
     * Returns the Spatial Reference System Identifier (SRID) if available.
     * This is <em>not</em> necessarily an EPSG code, even it is common practice to use
     * the same numerical values as EPSG. Note that the absence of SRID does not mean
     * that {@link #getCoordinateReferenceSystem()} would return no CRS.
     */
    @Override
    public OptionalInt getSRID() {
        return OptionalInt.empty();
    }

    /**
     * Sets the coordinate reference system. This method overwrites any previous user object.
     * This is okay for the context in which Apache SIS uses this method, which is only for
     * newly created geometries.
     */
    @Override
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
        super.setCoordinateReferenceSystem(crs);
        geometry.setCoordinateReferenceSystem(crs);
    }

    /**
     * Returns the dimension of the coordinates that define this geometry.
     */
    @Override
    public int getCoordinateDimension() {
        return getCoordinatesDimension(geometry);
    }

    /**
     * Gets the number of dimensions of geometry vertex (sequence of coordinate tuples), which can be 2 or 3.
     *
     * @param  geometry  the geometry for which to get <em>vertex</em> (not topological) dimension.
     * @return vertex dimension of the given geometry.
     */
    private static int getCoordinatesDimension(final Geometry geometry) {
        return geometry.getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
    }

    /**
     * Returns the envelope of SIS geometry. Never null, but may be empty.
     */
    @Override
    public GeneralEnvelope getEnvelope() {
        return new GeneralEnvelope(geometry.getEnvelope());
    }

    /**
     * Returns the centroid of the wrapped geometry as a direct position.
     */
    @Override
    public DirectPosition getCentroid() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * If the wrapped geometry is a point, returns its coordinates. Otherwise returns {@code null}.
     * If non-null, the returned array may have a length of 2 or 3.
     */
    @Override
    public double[] getPointCoordinates() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns all coordinate tuples in the wrapped geometry.
     * This method is currently used for testing purpose only.
     */
    @Debug
    @Override
    public double[] getAllCoordinates() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Merges a sequence of points or paths after the wrapped geometry.
     *
     * @throws ClassCastException if an element in the iterator is not a SIS geometry.
     */
    @Override
    public Geometry mergePolylines(final Iterator<?> polylines) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Applies a filter predicate between this geometry and another geometry.
     * This method assumes that the two geometries are in the same CRS (this is not verified).
     *
     * <p><b>Note:</b> No operations are supported at this time.</p>
     *
     * @throws ClassCastException if the given wrapper is not for the same geometry library.
     */
    @Override
    protected boolean predicateSameCRS(final SpatialOperatorName type, final GeometryWrapper other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Applies a filter predicate between this geometry and another geometry within a given distance.
     * This method assumes that the two geometries are in the same CRS and that the unit of measurement
     * is the same for {@code distance} than for axes (this is not verified).
     *
     * @throws ClassCastException if the given wrapper is not for the same geometry library.
     */
    @Override
    protected boolean predicateSameCRS(final DistanceOperatorName type,
                    final GeometryWrapper other, final double distance)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Applies a SQLMM operation on this geometry.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry, or {@code null} if the operation requires only one geometry.
     * @param  argument   an operation-specific argument, or {@code null} if not applicable.
     * @return result of the specified operation.
     * @throws ClassCastException if the operation can only be executed on some specific argument types
     *         (for example geometries that are polylines) and one of the argument is not of that type.
     */
    @Override
    protected Object operationSameCRS(final SQLMM operation, final GeometryWrapper other, final Object argument) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Converts the wrapped geometry to the specified type.
     * If the geometry is already of that type, it is returned unchanged.
     * Otherwise coordinates are copied in a new geometry of the requested type.
     *
     * <p>The following conversions are illegal and will cause an {@link IllegalArgumentException} to be thrown:</p>
     * <ul>
     *   <li>From point to polyline or polygon.</li>
     *   <li>From geometry collection (except multi-point) to polyline.</li>
     *   <li>From geometry collection (except multi-point and multi-line string) to polygon.</li>
     *   <li>From geometry collection containing nested collections.</li>
     * </ul>
     *
     * The conversion from {@link MultiLineString} to {@link Polygon} is defined as following:
     * the first {@link LineString} is taken as the exterior {@link LinearRing} and all others
     * {@link LineString}s are interior {@link LinearRing}s.
     * This rule is defined by some SQLMM operations.
     *
     * @param  target  the desired type.
     * @return the converted geometry.
     * @throws IllegalArgumentException if the geometry cannot be converted to the specified type.
     */
    @Override
    public GeometryWrapper toGeometryType(final GeometryType target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Transforms this geometry using the given coordinate operation.
     * If the operation is {@code null}, then the geometry is returned unchanged.
     * If the geometry uses a different CRS than the source CRS of the given operation
     * and {@code validate} is {@code true},
     * then a new operation to the target CRS will be automatically computed.
     *
     * @param  operation  the coordinate operation to apply, or {@code null}.
     * @param  validate   whether to validate the operation source CRS.
     * @throws FactoryException if transformation to the target CRS cannot be found.
     * @throws TransformException if the geometry cannot be transformed.
     */
    @Override
    public GeometryWrapper transform(final CoordinateOperation operation, final boolean validate)
            throws FactoryException, TransformException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Transforms this geometry to the specified Coordinate Reference System (CRS).
     * If the given CRS is null or is the same CRS as current one, the geometry is returned unchanged.
     *
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry (may be the same geometry instance), or {@code null}.
     * @throws TransformException if this geometry cannot be transformed.
     */
    @Override
    public GeometryWrapper transform(final CoordinateReferenceSystem targetCRS) throws TransformException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Transforms this geometry using the given transform.
     * If the transform is {@code null}, then the geometry is returned unchanged.
     *
     * @param  transform  the math transform to apply, or {@code null}.
     * @return the transformed geometry (may be the same geometry instance, but never {@code null}).
     * @throws TransformException if the geometry cannot be transformed.
     */
    @Override
    public GeometryWrapper transform(final MathTransform transform) throws FactoryException, TransformException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns a view over the SIS geometry as a Java2D shape. Changes in the SIS geometry
     * after this method call may be reflected in the returned shape in an unspecified way.
     *
     * @return a view over the geometry as a Java2D shape.
     */
    @Override
    public Shape toJava2D() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the WKT representation of the wrapped geometry.
     */
    @Override
    public String formatWKT(final double flatness) {
        return geometry.asText();
    }

    /**
     * View SIS Geometry as a JTS Geometry.
     * Only the matching JTS geometry types are supported.
     * The created geometry references the original geometry PointSequences, so modifications
     * are forwarded to the original but all metadata change, like the CRS, will not be preserved if changed
     * after the JTS view has been made.
     *
     * @param gf optional creation factory.
     * @return JTS geometry view of the given geometry
     */
    public org.locationtech.jts.geom.Geometry asJTS(org.locationtech.jts.geom.GeometryFactory gf) {
        return asJTS(geometry, gf);
    }

    /**
     * View SIS Geometry as a JTS Geometry.
     * Only the matching JTS geometry types are supported.
     * The created geometry references the original geometry PointSequences, so modifications
     * are forwarded to the original but all metadata change, like the CRS, will not be preserved if changed
     * after the JTS view has been made.
     *
     * @param geometry to convert
     * @param gf optional creation factory.
     * @return JTS geometry view of the given geometry
     */
    public static org.locationtech.jts.geom.Geometry asJTS(Geometry geometry, org.locationtech.jts.geom.GeometryFactory gf) {
        if (gf == null) gf = new org.locationtech.jts.geom.GeometryFactory();
        final org.locationtech.jts.geom.Geometry jts;

        if (geometry instanceof Point cdt){
            jts = gf.createPoint(new JTSSequence(cdt.asPointSequence()));
        } else if (geometry instanceof LinearRing cdt) {
            jts = gf.createLinearRing(new JTSSequence(cdt.getPoints()));
        } else if (geometry instanceof LineString cdt) {
            jts = gf.createLineString(new JTSSequence(cdt.getPoints()));
        } else if (geometry instanceof Polygon cdt) {
            final org.locationtech.jts.geom.LinearRing exterior = (org.locationtech.jts.geom.LinearRing) asJTS(cdt.getExteriorRing(), gf);
            final org.locationtech.jts.geom.LinearRing[] inners = new org.locationtech.jts.geom.LinearRing[cdt.getNumInteriorRing()];
            for (int i = 0; i < inners.length; i++) {
                inners[i] = (org.locationtech.jts.geom.LinearRing) asJTS(cdt.getInteriorRingN(i), gf);
            }
            jts = gf.createPolygon(exterior, inners);
        } else if (geometry instanceof MultiPoint<?> cdt) {
            jts = gf.createMultiPoint(new JTSSequence(cdt.asPointSequence()));
        } else if (geometry instanceof MultiLineString cdt) {
            final org.locationtech.jts.geom.LineString[] children = new org.locationtech.jts.geom.LineString[cdt.getNumGeometries()];
            for (int i = 0; i < children.length; i++) {
                children[i] = (org.locationtech.jts.geom.LineString) asJTS(cdt.getGeometryN(i), gf);
            }
            jts = gf.createMultiLineString(children);
        } else if (geometry instanceof MultiPolygon cdt) {
            final org.locationtech.jts.geom.Polygon[] children = new org.locationtech.jts.geom.Polygon[cdt.getNumGeometries()];
            for (int i = 0; i < children.length; i++) {
                children[i] = (org.locationtech.jts.geom.Polygon) asJTS(cdt.getGeometryN(i), gf);
            }
            jts = gf.createMultiPolygon(children);
        } else if (geometry instanceof MultiPolygon cdt) {
            final org.locationtech.jts.geom.Geometry[] children = new org.locationtech.jts.geom.Geometry[cdt.getNumGeometries()];
            for (int i = 0; i < children.length; i++) {
                children[i] = (org.locationtech.jts.geom.Geometry) asJTS(cdt.getGeometryN(i), gf);
            }
            jts = gf.createGeometryCollection(children);
        } else {
            throw new UnsupportedOperationException("Geometry type " + geometry.getClass().getName() + " has no match in JTS library");
        }

        jts.setUserData(geometry.getCoordinateReferenceSystem());
        return jts;
    }
    /**
     * Decorate a PointSequence as a JTS CoordinateSequence
     */
    private static class JTSSequence implements CoordinateSequence {

        private final PointSequence ps;

        public JTSSequence(PointSequence ps) {
            this.ps = ps;
        }

        @Override
        public int getDimension() {
            return ps.getDimension();
        }

        @Override
        public Coordinate getCoordinate(int i) {
            return getCoordinateCopy(i);
        }

        @Override
        public Coordinate getCoordinateCopy(int i) {
            final Coordinate crd = createCoordinate();
            getCoordinate(i, crd);
            return crd;
        }

        @Override
        public void getCoordinate(int idx, Coordinate crdnt) {
            Tuple tuple = ps.getPosition(idx);
            for (int i = 0; i < tuple.getDimension(); i++) {
                crdnt.setOrdinate(i, tuple.get(i));
            }
        }

        @Override
        public double getX(int i) {
            return ps.getPosition(i).get(0);
        }

        @Override
        public double getY(int i) {
            return ps.getPosition(i).get(1);
        }

        @Override
        public double getOrdinate(int i, int i1) {
            return ps.getPosition(i).get(i1);
        }

        @Override
        public int size() {
            return ps.size();
        }

        @Override
        public void setOrdinate(int i, int i1, double d) {
            Tuple position = ps.getPosition(i);
            position.set(i1, d);
            ps.setPosition(i, position);
        }

        @Override
        public Coordinate[] toCoordinateArray() {
            final Coordinate[] coords = new Coordinate[ps.size()];
            for (int i = 0;  i < coords.length; i++) {
                coords[i] = getCoordinateCopy(i);
            }
            return coords;
        }

        @Override
        public Envelope expandEnvelope(Envelope envlp) {
            final org.opengis.geometry.Envelope env = ps.getEnvelope();
            envlp.expandToInclude(new Envelope(env.getMinimum(0), env.getMaximum(0), env.getMinimum(1), env.getMaximum(1)));
            return envlp;
        }

        @Override
        public CoordinateSequence copy() {
            final PointSequence cp = new ArraySequence(ps.getAttributeArray(AttributesType.ATT_POSITION));
            return new JTSSequence(cp);
        }

        public Object clone(){
            return copy();
        }
    }

}
