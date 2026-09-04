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
package org.apache.sis.gml;

import java.io.OutputStream;
import java.util.function.IntFunction;
import javax.measure.Unit;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.CompoundCurve;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurvePolygon;
import org.apache.sis.geometries.Empty;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.MultiCurve;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.MultiPolygon;
import org.apache.sis.geometries.MultiPolyhedron;
import org.apache.sis.geometries.MultiSurface;
import org.apache.sis.geometries.Orientable;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.PolyhedralSurface;
import org.apache.sis.geometries.Polyhedron;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.TIN;
import org.apache.sis.geometries.curve.ArcByBulge;
import org.apache.sis.geometries.curve.ArcByCenterPoint;
import org.apache.sis.geometries.conics.CircularString;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;


/**
 * Writer of GML 3 geometries.
 *
 * <p>Usage:</p>
 * {@snippet lang="java" :
 *     try (GML3Writer writer = new GML3Writer(outputStream)) {
 *         writer.writeGeometry(geometry);
 *     }
 *     }
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GML3Writer extends AbstractGMLWriter {
    /**
     * Creates a new writer using the given StAX cursor.
     * The XML document header is written immediately by this constructor.
     *
     * @param  writer  the StAX cursor to write to.
     * @throws XMLStreamException if the XML document header cannot be written.
     */
    public GML3Writer(final XMLStreamWriter writer) throws XMLStreamException {
        super(writer);
    }

    /**
     * Creates a new writer for the given output stream.
     *
     * @param  out  the stream to write to.
     * @throws XMLStreamException if the StAX cursor cannot be created.
     */
    public GML3Writer(final OutputStream out) throws XMLStreamException {
        this(XMLOutputFactory.newInstance().createXMLStreamWriter(out, "UTF-8"));
    }

    @Override
    protected GMLVersion getVersion() {
        return GMLVersion.V3;
    }

    /**
     * Returns the GML 3.2 namespace.
     */
    @Override
    protected String namespace() {
        return GML3Tags.NAMESPACE_3;
    }

    /**
     * Writes a {@code <gml:Point>} element.
     */
    @Override
    protected void writePoint(final Point g, final String srsName, final boolean declareNamespace) throws XMLStreamException {
        writeStart(GML2Tags.POINT, srsName, declareNamespace);
        writePos(GML3Tags.POS, g.getPosition());
        writer.writeEndElement();
    }

    /**
     * Writes a {@code <gml:LineString>} element.
     */
    @Override
    protected void writeLineString(final LineString g, final String srsName, final boolean declareNamespace) throws XMLStreamException {
        writeStart(GML2Tags.LINE_STRING, srsName, declareNamespace);
        writePosList(g.getPoints());
        writer.writeEndElement();
    }

    /**
     * Writes a {@code <gml:LinearRing>} element.
     */
    @Override
    protected void writeLinearRing(final LinearRing g, final String srsName, final boolean declareNamespace) throws XMLStreamException {
        writeStart(GML2Tags.LINEAR_RING, srsName, declareNamespace);
        writePosList(g.getPoints());
        writer.writeEndElement();
    }

    /**
     * Writes a {@code <gml:Polygon>} element, with its {@code exterior} and, if any,
     * {@code interior} children.
     */
    @Override
    protected void writePolygon(final Polygon g, final String srsName, final boolean declareNamespace) throws XMLStreamException {
        writeStart(GML2Tags.POLYGON, srsName, declareNamespace);
        writer.writeStartElement(GML3Tags.EXTERIOR);
        writeLinearRing(g.getExteriorRing(), null, false);
        writer.writeEndElement();
        final int n = g.getNumInteriorRing();
        for (int i=0; i<n; i++) {
            writer.writeStartElement(GML3Tags.INTERIOR);
            writeLinearRing(g.getInteriorRingN(i), null, false);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Writes a {@code <gml:Envelope>} element, with its {@code lowerCorner} and {@code upperCorner}
     * children. This is the GML 3 spelling of what GML 2.0 calls a {@code gml:Box}; all the
     * dimensions of the envelope are written, not just the horizontal ones.
     */
    @Override
    protected void writeBBox(final BBox g, final String srsName, final boolean declareNamespace) throws XMLStreamException {
        writeStart(GML3Tags.ENVELOPE, srsName, declareNamespace);
        final int dim = g.getDimension();
        writeCorner(GML3Tags.LOWER_CORNER, g, dim, true);
        writeCorner(GML3Tags.UPPER_CORNER, g, dim, false);
        writer.writeEndElement();
    }

    /**
     * Writes one corner of an envelope as a {@code lowerCorner} or {@code upperCorner} element.
     */
    private void writeCorner(final String tagName, final BBox g, final int dim, final boolean lower)
            throws XMLStreamException
    {
        final StringBuilder sb = new StringBuilder();
        for (int i=0; i<dim; i++) {
            if (i != 0) sb.append(' ');
            sb.append(lower ? g.getMinimum(i) : g.getMaximum(i));
        }
        writer.writeStartElement(tagName);
        writer.writeCharacters(sb.toString());
        writer.writeEndElement();
    }

    /**
     * Writes a {@code <gml:MultiPoint>} element, with its {@code pointMember} children.
     */
    @Override
    protected void writeMultiPoint(final MultiPoint<?> g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writeMembers(GML2Tags.MULTI_POINT, GML2Tags.POINT_MEMBER, g, srsName, declareNamespace);
    }

    /**
     * Writes a {@code <gml:MultiCurve>} element, with its {@code curveMember} children.
     * This is the non-deprecated GML 3.2 spelling; the legacy {@code MultiLineString} is never
     * emitted, even for a {@link org.apache.sis.geometries.MultiLineString}.
     */
    @Override
    protected void writeMultiCurve(final MultiCurve<?> g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writeMembers(GML3Tags.MULTI_CURVE, GML3Tags.CURVE_MEMBER, g, srsName, declareNamespace);
    }

    /**
     * Writes a {@code <gml:MultiSurface>} element, with its {@code surfaceMember} children.
     * This is the non-deprecated GML 3.2 spelling; the legacy {@code MultiPolygon} is never
     * emitted, even for a {@link org.apache.sis.geometries.MultiPolygon}.
     */
    @Override
    protected void writeMultiSurface(final MultiSurface<?> g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writeMembers(GML3Tags.MULTI_SURFACE, GML3Tags.SURFACE_MEMBER, g, srsName, declareNamespace);
    }

    /**
     * Writes a {@code <gml:MultiGeometry>} element, with its {@code geometryMember} children.
     * Members may be of any supported GML geometry type.
     */
    @Override
    protected void writeMultiGeometry(final GeometryCollection<?> g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writeMembers(GML2Tags.MULTI_GEOMETRY, GML2Tags.GEOMETRY_MEMBER, g, srsName, declareNamespace);
    }

    /**
     * Writes an empty geometry as the GML element it was read from, if known, or as an empty
     * {@code <gml:MultiGeometry>} otherwise — the only GML geometry element whose content model
     * allows zero children.
     */
    @Override
    protected void writeEmpty(final Empty g, final String srsName, final boolean declareNamespace) throws XMLStreamException {
        final String element = GMLCRS.sourceElement(g);
        writeStart((element != null) ? element : GML2Tags.MULTI_GEOMETRY, srsName, declareNamespace);
        writer.writeEndElement();
    }

    /**
     * Writes a collection element with one wrapper element per member.
     */
    private void writeMembers(final String tagName, final String memberTagName, final GeometryCollection<?> g,
            final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writeStart(tagName, srsName, declareNamespace);
        for (int i=0, n=g.getNumGeometries(); i<n; i++) {
            writer.writeStartElement(memberTagName);
            writeGeometryElement(g.getGeometryN(i), null, false);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Writes a single coordinate tuple as a {@code <gml:pos>} element. The tuple is
     * self-describing by token count, so no {@code srsDimension} attribute is needed.
     */
    private void writePos(final String tagName, final Tuple<?> position) throws XMLStreamException {
        final StringBuilder sb = new StringBuilder();
        appendTuple(sb, position);
        writer.writeStartElement(tagName);
        writer.writeCharacters(sb.toString());
        writer.writeEndElement();
    }

    /**
     * Writes a {@code <gml:posList>} element containing the given coordinate tuples, with an
     * explicit {@code srsDimension} attribute taken from the width of the sequence itself.
     */
    private void writePosList(final PointSequence points) throws XMLStreamException {
        writer.writeStartElement(GML3Tags.POS_LIST);
        writer.writeAttribute(GML3Tags.SRS_DIMENSION, String.valueOf(points.getDimension()));
        final StringBuilder sb = new StringBuilder();
        appendSequence(sb, points);
        writer.writeCharacters(sb.toString());
        writer.writeEndElement();
    }

    /**
     * Writes the curve, surface and solid kinds that GML 3 can express and GML 2.0 cannot.
     */
    @Override
    protected boolean writeExtendedGeometry(final Geometry geometry, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        if (geometry instanceof Curve c && c.getOrientationSign() == Orientable.Sign.NEGATIVE) {
            writeOriented(GML3Tags.ORIENTABLE_CURVE, GML3Tags.BASE_CURVE, c.getPrimitive(), srsName, declareNamespace);
        } else if (geometry instanceof Surface s && s.getOrientationSign() == Orientable.Sign.NEGATIVE) {
            writeOriented(GML3Tags.ORIENTABLE_SURFACE, GML3Tags.BASE_SURFACE, s.getPrimitive(), srsName, declareNamespace);
        } else if (geometry instanceof CircularString g) {           // Before Curve.
            writeCircularString(g, srsName, declareNamespace);
        } else if (geometry instanceof ArcByCenterPoint g) {         // Before Curve.
            writeArcByCenterPoint(g, srsName, declareNamespace);
        } else if (geometry instanceof ArcByBulge g) {               // Before Curve.
            writeArcByBulge(g, srsName, declareNamespace);
        } else if (geometry instanceof CompoundCurve g) {            // Before Curve.
            writeCompoundCurve(g, srsName, declareNamespace);
        } else if (geometry instanceof CurvePolygon g) {             // Before Surface.
            writeCurvePolygon(g, srsName, declareNamespace);
        } else if (geometry instanceof TIN g) {                      // Before PolyhedralSurface.
            writePatches(g.getNumPatches(), g::getPatchN, GML3Tags.TRIANGLE, srsName, declareNamespace);
        } else if (geometry instanceof PolyhedralSurface<?> g) {     // Before Surface.
            writePatches(g.getNumPatches(), g::getPatchN, GML3Tags.POLYGON_PATCH, srsName, declareNamespace);
        } else if (geometry instanceof Polyhedron g) {
            writeSolid(g, srsName, declareNamespace);
        } else if (geometry instanceof MultiPolyhedron g) {          // Before GeometryCollection.
            writeMembers(GML3Tags.COMPOSITE_SOLID, GML3Tags.SOLID_MEMBER, g, srsName, declareNamespace);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Writes a {@code <gml:OrientableCurve>} or {@code <gml:OrientableSurface>} element with
     * {@code orientation="-"} around the geometry it reverses.
     */
    private void writeOriented(final String tagName, final String basePropertyName, final Geometry base,
            final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writeStart(tagName, srsName, declareNamespace);
        writer.writeAttribute(GML3Tags.ORIENTATION, GML3Tags.ORIENTATION_REVERSED);
        writer.writeStartElement(basePropertyName);
        writeGeometryElement(base, null, false);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    /**
     * Writes a circular string as a {@code <gml:Curve>} whose single segment is a
     * {@code <gml:ArcString>} — or a {@code <gml:Arc>} when there is exactly one arc, which is the
     * spelling a reader of {@code gml:Arc} produced in the first place.
     */
    private void writeCircularString(final CircularString g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException
    {
        writeStart(GML3Tags.CURVE, srsName, declareNamespace);
        writer.writeStartElement(GML3Tags.SEGMENTS);
        writeArcSegment(g);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    /**
     * Writes one {@code <gml:Arc>} or {@code <gml:ArcString>} segment for the given circular string.
     */
    private void writeArcSegment(final CircularString g) throws XMLStreamException {
        writer.writeStartElement((g.getNumArcs() == 1) ? GML3Tags.ARC : GML3Tags.ARC_STRING);
        writePosList(g.getPoints());
        writer.writeEndElement();
    }

    /**
     * Writes a centre-point arc as a {@code <gml:Curve>} whose single segment is a
     * {@code <gml:ArcByCenterPoint>}, which is the element it was read from and the only GML
     * spelling of this parameterisation.
     */
    private void writeArcByCenterPoint(final ArcByCenterPoint g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException
    {
        writeStart(GML3Tags.CURVE, srsName, declareNamespace);
        writer.writeStartElement(GML3Tags.SEGMENTS);
        writer.writeStartElement(GML3Tags.ARC_BY_CENTER_POINT);
        writePos(GML3Tags.POS, g.getCenter().getPosition());
        final Unit<?> unit = g.getRadiusUnit();
        writeMeasure(GML3Tags.RADIUS, g.getRadius(), (unit != null) ? unit.toString() : null);
        writeMeasure(GML3Tags.START_ANGLE, g.getStartAngle(), GML3Tags.UOM_DEGREE);
        writeMeasure(GML3Tags.END_ANGLE,   g.getEndAngle(),   GML3Tags.UOM_DEGREE);
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
    }

    /**
     * Writes a bulge arc as a {@code <gml:Curve>} whose single segment is a
     * {@code <gml:ArcByBulge>}.
     */
    private void writeArcByBulge(final ArcByBulge g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException
    {
        writeStart(GML3Tags.CURVE, srsName, declareNamespace);
        writer.writeStartElement(GML3Tags.SEGMENTS);
        writer.writeStartElement(GML3Tags.ARC_BY_BULGE);
        writePosList(g.getPoints());
        writeMeasure(GML3Tags.BULGE, g.getBulge(), null);
        writePos(GML3Tags.NORMAL, g.getNormal());
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
    }

    /**
     * Writes an element holding a single number, with a {@code uom} attribute when a unit is known.
     * The attribute is omitted for an unknown unit rather than filled in with a guess: GML then
     * means the units of the coordinate system axes, which is exactly what a value with no unit of
     * its own is.
     *
     * <p>The attribute is derived from the unit itself, so a document that spelled it as an
     * authority code — {@code uom="urn:ogc:def:uom:EPSG::9001"} — comes back with the symbol
     * ({@code uom="m"}). The unit is the same one; only the spelling is normalised.</p>
     *
     * @param  uom  the unit of measurement to declare, or {@code null} to omit the attribute.
     */
    private void writeMeasure(final String tagName, final double value, final String uom) throws XMLStreamException {
        writer.writeStartElement(tagName);
        if (uom != null) {
            writer.writeAttribute(GML3Tags.UOM, uom);
        }
        writer.writeCharacters(String.valueOf(value));
        writer.writeEndElement();
    }

    /**
     * Writes a compound curve as a {@code <gml:CompositeCurve>} with one {@code curveMember} per
     * component.
     *
     * <p>A {@code gml:Curve} with several {@code gml:segments} would be an equally faithful
     * encoding of the same geometry, and is in fact where a multi-segment compound curve came from
     * on the way in. {@code gml:CompositeCurve} is chosen because it can hold components of any
     * kind, including ones that are not GML curve <em>segments</em> at all — a
     * {@code gml:LineString} component, for instance. The consequence is that a
     * {@code gml:Curve} with two segments does not round-trip to a {@code gml:Curve}; it comes back
     * as a {@code gml:CompositeCurve} describing the same shape.</p>
     */
    private void writeCompoundCurve(final CompoundCurve g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writeStart(GML3Tags.COMPOSITE_CURVE, srsName, declareNamespace);
        for (int i=0, n=g.getNumCurves(); i<n; i++) {
            writer.writeStartElement(GML3Tags.CURVE_MEMBER);
            writeGeometryElement(g.getCurveN(i), null, false);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Writes a curve-bounded surface as a {@code <gml:Polygon>} whose boundaries are
     * {@code <gml:Ring>} elements rather than {@code <gml:LinearRing>} ones.
     */
    private void writeCurvePolygon(final CurvePolygon g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writeStart(GML2Tags.POLYGON, srsName, declareNamespace);
        writer.writeStartElement(GML3Tags.EXTERIOR);
        writeBoundary(g.getExteriorRing());
        writer.writeEndElement();
        for (final Curve interior : g.getInteriorRings()) {
            writer.writeStartElement(GML3Tags.INTERIOR);
            writeBoundary(interior);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Writes one boundary of a surface: a {@code <gml:LinearRing>} when it is linear, a
     * {@code <gml:Ring>} of {@code curveMember} components otherwise.
     */
    private void writeBoundary(final Curve boundary)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        if (boundary instanceof LinearRing ring) {
            writeLinearRing(ring, null, false);
        } else if (boundary instanceof CompoundCurve compound) {
            writer.writeStartElement(GML3Tags.RING);
            for (int i=0, n=compound.getNumCurves(); i<n; i++) {
                writer.writeStartElement(GML3Tags.CURVE_MEMBER);
                writeGeometryElement(compound.getCurveN(i), null, false);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        } else {
            // A single non-linear curve closing on itself: still a ring, with one member.
            writer.writeStartElement(GML3Tags.RING);
            writer.writeStartElement(GML3Tags.CURVE_MEMBER);
            writeGeometryElement(boundary, null, false);
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    /**
     * Writes a {@code <gml:Surface>} element whose {@code <gml:patches>} holds one element of the
     * given name per patch.
     *
     * @param  patchTagName  {@code gml:PolygonPatch}, or {@code gml:Triangle} for a triangulated surface.
     */
    private void writePatches(final int count, final IntFunction<? extends Polygon> patches,
            final String patchTagName, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writeStart(GML3Tags.SURFACE, srsName, declareNamespace);
        writer.writeStartElement(GML3Tags.PATCHES);
        for (int i=0; i<count; i++) {
            final Polygon patch = patches.apply(i);
            writer.writeStartElement(patchTagName);
            writer.writeStartElement(GML3Tags.EXTERIOR);
            writeLinearRing(patch.getExteriorRing(), null, false);
            writer.writeEndElement();
            final int holes = patch.getNumInteriorRing();
            for (int h=0; h<holes; h++) {
                writer.writeStartElement(GML3Tags.INTERIOR);
                writeLinearRing(patch.getInteriorRingN(h), null, false);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndElement();
    }

    /**
     * Writes a polyhedron as a {@code <gml:Solid>} element, with one {@code <gml:Shell>} per
     * bounding shell.
     */
    private void writeSolid(final Polyhedron g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writeStart(GML3Tags.SOLID, srsName, declareNamespace);
        writer.writeStartElement(GML3Tags.EXTERIOR);
        writeShell(g.getExteriorShell());
        writer.writeEndElement();
        for (final MultiPolygon interior : g.getInteriorShells()) {
            writer.writeStartElement(GML3Tags.INTERIOR);
            writeShell(interior);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Writes one {@code <gml:Shell>} element, with one {@code surfaceMember} per face.
     */
    private void writeShell(final MultiPolygon shell)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writer.writeStartElement(GML3Tags.SHELL);
        for (int i=0, n=shell.getNumGeometries(); i<n; i++) {
            writer.writeStartElement(GML3Tags.SURFACE_MEMBER);
            writeGeometryElement(shell.getGeometryN(i), null, false);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
