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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.CompoundCurve;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurvePolygon;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.MultiPolygon;
import org.apache.sis.geometries.MultiPolyhedron;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.PolyhedralSurface;
import org.apache.sis.geometries.Polyhedron;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.TIN;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.curve.ArcByBulge;
import org.apache.sis.geometries.curve.ArcByCenterPoint;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Reader of GML 3 geometries (3.0, 3.1 and 3.2).
 *
 * <p>Usage:</p>
 * {@snippet lang="java" :
 *     try (GML3Reader reader = new GML3Reader(inputStream)) {
 *         Geometry geometry = reader.readGeometry();
 *     }
 *     }
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GML3Reader extends AbstractGMLReader {
    /**
     * Creates a new reader using the given StAX cursor.
     * The cursor can be positioned anywhere before the geometry root element.
     *
     * @param  reader  the StAX cursor to read from.
     */
    public GML3Reader(final XMLStreamReader reader) {
        super(reader);
    }

    /**
     * Creates a new reader for the given input stream.
     *
     * @param  in  the stream to read from.
     * @throws XMLStreamException if the StAX cursor cannot be created.
     */
    public GML3Reader(final InputStream in) throws XMLStreamException {
        this(XMLInputFactory.newInstance().createXMLStreamReader(in));
    }

    @Override
    protected GMLVersion getVersion() {
        return GMLVersion.V3;
    }

    /**
     * Returns {@code true} if the given namespace can be considered as a GML namespace.
     * Both the versioned GML 3.2 namespace and the unversioned one used by GML 3.0 and 3.1
     * are accepted, as is a missing or empty namespace.
     */
    @Override
    protected boolean isGML(final String ns) {
        return (ns == null) || ns.isEmpty()
                || GML3Tags.NAMESPACE_3.equals(ns)
                || GML2Tags.NAMESPACE_2.equals(ns);
    }

    /**
     * Parses the geometry element at the current cursor position. The cursor must be on the
     * {@link #START_ELEMENT} event of the geometry to parse. After this method returns, the
     * cursor is on the matching {@link #END_ELEMENT} event.
     *
     * @param  inherited  the CRS inherited from an enclosing geometry element, or {@code null} if none.
     */
    @Override
    protected Geometry parseGeometryElement(final CoordinateReferenceSystem inherited)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        assert reader.isStartElement();
        if (!isGML(reader.getNamespaceURI())) {
            throw new DataStoreContentException(unsupportedElement());
        }
        final String srsName = reader.getAttributeValue(null, GML2Tags.SRS_NAME);
        final CoordinateReferenceSystem crs = GMLCRS.resolve(srsName, inherited);
        final String element = reader.getLocalName();
        final Geometry geometry;
        switch (element) {
            case GML2Tags.POINT:               geometry = parsePoint(crs);                          break;
            case GML2Tags.LINE_STRING:         geometry = parseLineString(crs);                     break;
            case GML2Tags.LINEAR_RING:         geometry = parseLinearRing(crs);                     break;
            case GML2Tags.POLYGON:             geometry = parsePolygon(crs);                        break;
            case GML2Tags.BOX:                 geometry = parseBox(crs);                            break;
            case GML3Tags.ENVELOPE:            geometry = parseEnvelope(crs);                       break;
            case GML2Tags.MULTI_POINT:         geometry = parseMultiPoint(crs);                     break;
            case GML2Tags.MULTI_LINE_STRING:   geometry = parseMultiCurve(crs, GML2Tags.MULTI_LINE_STRING);   break;
            case GML3Tags.MULTI_CURVE:         geometry = parseMultiCurve(crs, GML3Tags.MULTI_CURVE);         break;
            case GML2Tags.MULTI_POLYGON:       geometry = parseMultiSurface(crs, GML2Tags.MULTI_POLYGON);     break;
            case GML3Tags.MULTI_SURFACE:       geometry = parseMultiSurface(crs, GML3Tags.MULTI_SURFACE);     break;
            case GML2Tags.MULTI_GEOMETRY:      geometry = parseMultiGeometry(crs);                  break;
            case GML3Tags.CURVE:               geometry = parseCurve(crs);                          break;
            case GML3Tags.COMPOSITE_CURVE:     geometry = parseCompositeCurve(crs);                 break;
            case GML3Tags.RING:                geometry = parseRing(crs);                           break;
            case GML3Tags.ORIENTABLE_CURVE:    geometry = parseOrientableCurve(crs);                break;
            case GML3Tags.SURFACE:             geometry = parseSurface(crs, GML3Tags.SURFACE);      break;
            case GML3Tags.TRIANGULATED_SURFACE:
            case GML3Tags.TIN:                 geometry = parseSurface(crs, element);               break;
            case GML3Tags.COMPOSITE_SURFACE:   geometry = parseCompositeSurface(crs);               break;
            case GML3Tags.ORIENTABLE_SURFACE:  geometry = parseOrientableSurface(crs);              break;
            case GML3Tags.SOLID:               geometry = parseSolid(crs);                          break;
            case GML3Tags.COMPOSITE_SOLID:     geometry = parseMultiSolid(crs, GML3Tags.COMPOSITE_SOLID); break;
            case GML3Tags.MULTI_SOLID:         geometry = parseMultiSolid(crs, GML3Tags.MULTI_SOLID);     break;
            default: throw new DataStoreContentException(unsupportedElement());
        }
        return annotate(geometry, element, srsName);
    }

    /**
     * Returns the error message for a curve or surface kind whose Apache SIS interface exists but
     * has no implementation, and whose parameterisation is a design question in its own right
     * (splines, clothoids, whole circles, offset curves, gridded surfaces).
     *
     * <p>The wording says <em>deferred</em>, not impossible: nothing here is beyond the model, it
     * simply has not been built. What is never done is silently substituting an approximation —
     * no curve is linearised and no surface tessellated behind the caller's back.</p>
     */
    private String notImplemented(final String construct) {
        return "Reading the GML 3 " + construct + " element <" + reader.getLocalName() + "> is not implemented yet."
             + " Its Apache SIS geometry interface exists but has no implementation, and no linear"
             + " approximation is substituted for it.";
    }

    /**
     * Recognises the GML 3 coordinate encodings, then falls back on the GML 2.0 ones.
     */
    @Override
    protected boolean parseCoordinateElement(final PositionListBuilder target,
            final CoordinateReferenceSystem inScope)
            throws XMLStreamException, DataStoreContentException
    {
        switch (reader.getLocalName()) {
            case GML3Tags.POS_LIST: parsePosList(target, inScope); return true;
            case GML3Tags.POS:      parsePos(target);              return true;
            default: return super.parseCoordinateElement(target, inScope);
        }
    }

    /**
     * Parses a {@code <gml:posList>} element, appending its coordinate tuples to the given builder.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     *
     * <p>The number of ordinates per tuple is determined, in order of preference, by: the
     * {@code srsDimension} attribute; the dimension of the CRS in scope; then 2, then 3. Each
     * candidate is accepted only if it divides the number of tokens present, so a three-dimensional
     * {@code posList} under a two-dimensional {@code srsName} — which real GML documents do contain
     * — is read at its true width rather than rejected. An explicit {@code srsDimension} is always
     * tried first and is never second-guessed.</p>
     */
    private void parsePosList(final PositionListBuilder target, final CoordinateReferenceSystem inScope)
            throws XMLStreamException, DataStoreContentException
    {
        final Integer declared = srsDimension();
        final String text = reader.getElementText().trim();
        if (text.isEmpty()) {
            return;
        }
        final String[] tokens = text.split("\\s+");
        final int width = tupleWidth(tokens.length, declared, inScope);
        final double[] ordinates = new double[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            ordinates[i] = Double.parseDouble(tokens[i]);
        }
        target.addFlat(ordinates, width);
    }

    /**
     * Chooses the number of ordinates per tuple for a flat list of {@code count} tokens.
     *
     * @param  count     the number of whitespace-separated tokens found.
     * @param  declared  the {@code srsDimension} attribute value, or {@code null} if absent or unparseable.
     * @param  inScope   the CRS in scope, or {@code null} if none.
     * @throws DataStoreContentException if no candidate width divides {@code count}.
     */
    private int tupleWidth(final int count, final Integer declared, final CoordinateReferenceSystem inScope)
            throws DataStoreContentException
    {
        if (declared != null) {
            if (declared > 0 && count % declared == 0) {
                return declared;
            }
            throw new DataStoreContentException("A GML 3 posList declaring srsDimension=\"" + declared
                    + "\" must contain a multiple of " + declared + " ordinates, but contains " + count + ".");
        }
        if (inScope != null) {
            final int d = inScope.getCoordinateSystem().getDimension();
            if (d > 0 && count % d == 0) {
                return d;
            }
        }
        if (count % 2 == 0) return 2;
        if (count % 3 == 0) return 3;
        throw new DataStoreContentException("Cannot determine the coordinate dimension of a GML 3 posList of "
                + count + " ordinates: it is a multiple of neither 2 nor 3, and no usable srsDimension"
                + " attribute or coordinate reference system is in scope.");
    }

    /**
     * Parses a {@code <gml:pos>} element, appending its single coordinate tuple to the given
     * builder. The width is the number of tokens present; unlike the GML 3 schema, no upper bound
     * is imposed. The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private void parsePos(final PositionListBuilder target) throws XMLStreamException, DataStoreContentException {
        final String text = reader.getElementText().trim();
        if (text.isEmpty()) {
            throw new DataStoreContentException("A GML 3 pos element must contain at least two ordinates.");
        }
        final String[] tokens = text.split("\\s+");
        if (tokens.length < 2) {
            throw new DataStoreContentException("A GML 3 pos element must contain at least two ordinates.");
        }
        final double[] ordinates = new double[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            ordinates[i] = Double.parseDouble(tokens[i]);
        }
        target.add(ordinates);
    }

    /**
     * Returns the value of the {@code srsDimension} attribute on the current element,
     * or {@code null} if the attribute is absent or not a number.
     */
    private Integer srsDimension() {
        final String s = reader.getAttributeValue(null, GML3Tags.SRS_DIMENSION);
        if (s != null) try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            // Treat as absent: the width is then inferred from the token count.
        }
        return null;
    }

    /**
     * Parses a {@code <gml:Point>} element.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private Point parsePoint(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final PositionListBuilder coordinates = parseCoordinateSequence(GML2Tags.POINT, crs);
        if (coordinates.size() != 1) {
            throw new DataStoreContentException("A GML 3 Point must contain exactly one coordinate tuple.");
        }
        return GeometryFactory.createPoint(coordinates.build(crs));
    }

    /**
     * Parses a {@code <gml:LineString>} element.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private LineString parseLineString(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final PositionListBuilder coordinates = parseCoordinateSequence(GML2Tags.LINE_STRING, crs);
        if (coordinates.size() < 2) {
            throw new DataStoreContentException("A GML 3 LineString must contain at least two coordinate tuples.");
        }
        return GeometryFactory.createLineString(coordinates.build(crs));
    }

    /**
     * Parses a {@code <gml:LinearRing>} element.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private LinearRing parseLinearRing(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final PositionListBuilder coordinates = parseCoordinateSequence(GML2Tags.LINEAR_RING, crs);
        if (coordinates.size() < 4) {
            throw new DataStoreContentException("A GML 3 LinearRing must contain at least four coordinate tuples.");
        }
        return GeometryFactory.createLinearRing(coordinates.build(crs));
    }

    /**
     * Parses a legacy {@code <gml:Box>} element into a {@link BBox}.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private BBox parseBox(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final PositionListBuilder coordinates = parseCoordinateSequence(GML2Tags.BOX, crs);
        if (coordinates.size() != 2) {
            throw new DataStoreContentException("A GML 3 Box must contain exactly two coordinate tuples.");
        }
        return GML2Reader.newBBox(GMLCRS.forDimension(crs, coordinates.dimension()), coordinates.toFlatArray());
    }

    /**
     * Parses a {@code <gml:Envelope>} element into a {@link BBox}. The corners may be given either
     * as {@code <gml:lowerCorner>}/{@code <gml:upperCorner>} or, in older documents, as two
     * {@code <gml:pos>} or {@code <gml:coord>} elements. The cursor must be on the element's
     * {@link #START_ELEMENT} event.
     */
    private BBox parseEnvelope(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final PositionListBuilder corners = new PositionListBuilder();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML3Tags.LOWER_CORNER:
                        case GML3Tags.UPPER_CORNER: parsePos(corners); break;
                        default: {
                            if (!parseCoordinateElement(corners, crs)) {
                                skipUntilEnd();
                            }
                            break;
                        }
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML3Tags.ENVELOPE.equals(reader.getLocalName())) {
                        if (corners.size() != 2) {
                            throw new DataStoreContentException("A GML 3 Envelope must contain a lowerCorner"
                                    + " and an upperCorner, but " + corners.size() + " corners were found.");
                        }
                        return GML2Reader.newBBox(GMLCRS.forDimension(crs, corners.dimension()), corners.toFlatArray());
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:Polygon>} element, accepting both the GML 3 boundary tags
     * ({@code exterior}/{@code interior}) and the legacy GML 2.0 tags ({@code outerBoundaryIs}/
     * {@code innerBoundaryIs}). The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private Surface parsePolygon(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        return parseRingBoundedSurface(crs, GML2Tags.POLYGON);
    }

    /**
     * Parses an element whose content model is that of a {@code gml:Polygon} — one
     * {@code exterior} boundary and any number of {@code interior} ones — and returns the most
     * specific surface type those boundaries allow.
     *
     * <p>When every boundary is a {@code gml:LinearRing} the result is a {@link Polygon}. When any
     * boundary is a non-linear {@code gml:Ring} it is a {@link CurvePolygon} instead, since a
     * {@code Polygon}'s rings are {@link LinearRing}s by definition. Both the GML 3
     * ({@code exterior}/{@code interior}) and the legacy GML 2.0
     * ({@code outerBoundaryIs}/{@code innerBoundaryIs}) boundary tags are accepted.</p>
     *
     * @param  enclosingTagName  local name of the element being parsed: {@code gml:Polygon},
     *         {@code gml:PolygonPatch}, {@code gml:Triangle} or {@code gml:Rectangle}.
     */
    private Surface parseRingBoundedSurface(final CoordinateReferenceSystem crs, final String enclosingTagName)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        Curve shell = null;
        List<Curve> holes = null;
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML3Tags.EXTERIOR:
                        case GML2Tags.OUTER_BOUNDARY_IS: {
                            shell = parseRingProperty(reader.getLocalName(), crs);
                            break;
                        }
                        case GML3Tags.INTERIOR:
                        case GML2Tags.INNER_BOUNDARY_IS: {
                            if (holes == null) holes = new ArrayList<>();
                            holes.add(parseRingProperty(reader.getLocalName(), crs));
                            break;
                        }
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (enclosingTagName.equals(reader.getLocalName())) {
                        if (shell == null) {
                            throw new DataStoreContentException("A GML 3 " + enclosingTagName
                                    + " must contain an exterior element.");
                        }
                        final boolean linear = (shell instanceof LinearRing)
                                && (holes == null || allInstanceOf(holes, LinearRing.class));
                        if (linear) {
                            final List<LinearRing> linearHoles;
                            if (holes == null) {
                                linearHoles = null;
                            } else {
                                linearHoles = new ArrayList<>(holes.size());
                                for (final Curve hole : holes) {
                                    linearHoles.add((LinearRing) hole);
                                }
                            }
                            return GeometryFactory.createPolygon((LinearRing) shell, linearHoles);
                        }
                        return GeometryFactory.createCurvePolygon(shell, holes);
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses an {@code <gml:exterior>}, {@code <gml:interior>}, {@code <gml:outerBoundaryIs>} or
     * {@code <gml:innerBoundaryIs>} element, which must contain exactly one boundary: either a
     * {@code <gml:LinearRing>} or a general {@code <gml:Ring>}. The cursor must be on the wrapper
     * element's {@link #START_ELEMENT} event. After this method returns, the cursor is on the
     * matching {@link #END_ELEMENT} event of the wrapper.
     */
    private Curve parseRingProperty(final String wrapperTagName, final CoordinateReferenceSystem inherited)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        Curve ring = null;
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String name = reader.getLocalName();
                    if (GML2Tags.LINEAR_RING.equals(name) || GML3Tags.RING.equals(name)) {
                        final String srsName = reader.getAttributeValue(null, GML2Tags.SRS_NAME);
                        final CoordinateReferenceSystem crs = GMLCRS.resolve(srsName, inherited);
                        ring = annotate(GML2Tags.LINEAR_RING.equals(name) ? parseLinearRing(crs) : parseRing(crs),
                                        name, srsName);
                    } else {
                        skipUntilEnd();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (wrapperTagName.equals(reader.getLocalName())) {
                        if (ring == null) {
                            throw new DataStoreContentException("A GML 3 " + wrapperTagName
                                    + " must contain a LinearRing or Ring element.");
                        }
                        return ring;
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:MultiPoint>} element, accepting both the per-member
     * {@code pointMember} form and the compact {@code pointMembers} list form.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private MultiPoint parseMultiPoint(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Point> members = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML2Tags.POINT_MEMBER:  members.add(parseMember(Point.class, GML2Tags.POINT_MEMBER, crs)); break;
                        case GML3Tags.POINT_MEMBERS: parseMemberList(Point.class, GML3Tags.POINT_MEMBERS, crs, members); break;
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML2Tags.MULTI_POINT.equals(reader.getLocalName())) {
                        return GeometryFactory.createMultiPoint(aggregateCRS(crs, members),
                                members.toArray(Point[]::new));
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:MultiCurve>} element, or the deprecated-but-legal
     * {@code <gml:MultiLineString>}. Both the per-member ({@code curveMember},
     * {@code lineStringMember}) and the compact ({@code curveMembers}) forms are accepted.
     *
     * <p>The result is a {@link org.apache.sis.geometries.MultiLineString} when every member is a
     * {@link LineString}, and the more general {@link org.apache.sis.geometries.MultiCurve}
     * otherwise. The cursor must be on the element's {@link #START_ELEMENT} event.</p>
     */
    private Geometry parseMultiCurve(final CoordinateReferenceSystem crs, final String enclosingTagName)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Curve> members = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML2Tags.LINE_STRING_MEMBER:
                        case GML3Tags.CURVE_MEMBER:  members.add(parseMember(Curve.class, reader.getLocalName(), crs)); break;
                        case GML3Tags.CURVE_MEMBERS: parseMemberList(Curve.class, GML3Tags.CURVE_MEMBERS, crs, members); break;
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (enclosingTagName.equals(reader.getLocalName())) {
                        final CoordinateReferenceSystem fallback = aggregateCRS(crs, members);
                        if (allInstanceOf(members, LineString.class)) {
                            return GeometryFactory.createMultiLineString(fallback,
                                    members.toArray(LineString[]::new));
                        }
                        return GeometryFactory.createMultiCurve(fallback, members.toArray(Curve[]::new));
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:MultiSurface>} element, or the deprecated-but-legal
     * {@code <gml:MultiPolygon>}. Both the per-member ({@code surfaceMember},
     * {@code polygonMember}) and the compact ({@code surfaceMembers}) forms are accepted.
     *
     * <p>The result is a {@link org.apache.sis.geometries.MultiPolygon} when every member is a
     * {@link Polygon}, and the more general {@link org.apache.sis.geometries.MultiSurface}
     * otherwise. The cursor must be on the element's {@link #START_ELEMENT} event.</p>
     */
    private Geometry parseMultiSurface(final CoordinateReferenceSystem crs, final String enclosingTagName)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Surface> members = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML2Tags.POLYGON_MEMBER:
                        case GML3Tags.SURFACE_MEMBER:  members.add(parseMember(Surface.class, reader.getLocalName(), crs)); break;
                        case GML3Tags.SURFACE_MEMBERS: parseMemberList(Surface.class, GML3Tags.SURFACE_MEMBERS, crs, members); break;
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (enclosingTagName.equals(reader.getLocalName())) {
                        final CoordinateReferenceSystem fallback = aggregateCRS(crs, members);
                        if (allInstanceOf(members, Polygon.class)) {
                            return GeometryFactory.createMultiPolygon(fallback,
                                    members.toArray(Polygon[]::new));
                        }
                        return GeometryFactory.createMultiSurface(fallback, members.toArray(Surface[]::new));
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:MultiGeometry>} element, accepting both the per-member
     * {@code geometryMember} form and the compact {@code geometryMembers} list form.
     * Members may be of any supported GML geometry type.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private Geometry parseMultiGeometry(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Geometry> members = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML2Tags.GEOMETRY_MEMBER:  members.add(parseMember(Geometry.class, GML2Tags.GEOMETRY_MEMBER, crs)); break;
                        case GML3Tags.GEOMETRY_MEMBERS: parseMemberList(Geometry.class, GML3Tags.GEOMETRY_MEMBERS, crs, members); break;
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML2Tags.MULTI_GEOMETRY.equals(reader.getLocalName())) {
                        return GeometryFactory.createGeometryCollection(aggregateCRS(crs, members),
                                members.toArray(Geometry[]::new));
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Returns {@code true} if every element of the given list is an instance of the given type.
     * An empty list satisfies every type, which lets an empty collection be built as the more
     * specific of the two candidate types.
     */
    private static boolean allInstanceOf(final List<?> members, final Class<?> type) {
        for (final Object member : members) {
            if (!type.isInstance(member)) {
                return false;
            }
        }
        return true;
    }

    // ////////////////////////////////////////////////////////////////////////
    // Curves other than gml:LineString and gml:LinearRing ////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Parses a {@code <gml:Curve>} element, whose {@code <gml:segments>} holds one or more curve
     * segments joined end to end.
     *
     * <p>A single segment is returned on its own — a {@code gml:Curve} wrapping one
     * {@code gml:LineStringSegment} is just a line string — so only a genuinely multi-segment curve
     * becomes a {@link CompoundCurve}. That collapsing is what makes the common case round-trip to a
     * useful type, at the cost of not reproducing the {@code gml:Curve} wrapper on the way out
     * unless the element hint recorded by {@link #annotate annotate(…)} is honoured.</p>
     */
    private Curve parseCurve(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Curve> segments = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (GML3Tags.SEGMENTS.equals(reader.getLocalName())) {
                        parseSegments(crs, segments);
                    } else {
                        skipUntilEnd();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML3Tags.CURVE.equals(reader.getLocalName())) {
                        if (segments.isEmpty()) {
                            throw new DataStoreContentException("A GML 3 Curve must contain at least one segment.");
                        }
                        return (segments.size() == 1) ? segments.get(0)
                                : GeometryFactory.createCompoundCurve(crs, segments.toArray(Curve[]::new));
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:segments>} element, appending one curve per segment to the given list.
     * The cursor must be on the {@code segments} element's {@link #START_ELEMENT} event.
     */
    private void parseSegments(final CoordinateReferenceSystem crs, final List<Curve> addTo)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String segment = reader.getLocalName();
                    switch (segment) {
                        /*
                         * A geodesic string interpolates along the ellipsoid rather than in a
                         * straight line, but `DefaultLineString` cannot record that: its
                         * interpolation is fixed to LINEAR. The vertices are kept and the
                         * interpolation is lost -- a genuine, documented degradation.
                         */
                        case GML3Tags.LINE_STRING_SEGMENT:
                        case GML3Tags.GEODESIC_STRING:
                        case GML3Tags.GEODESIC: {
                            addTo.add(GeometryFactory.createLineString(parseCoordinateSequence(segment, crs).build(crs)));
                            break;
                        }
                        /*
                         * gml:Arc is the three-point case of gml:ArcString; both carry their
                         * control points the same way and mean the same thing.
                         */
                        case GML3Tags.ARC:
                        case GML3Tags.ARC_STRING: {
                            addTo.add(GeometryFactory.createCircularString(parseCoordinateSequence(segment, crs).build(crs)));
                            break;
                        }
                        case GML3Tags.ARC_BY_CENTER_POINT: {
                            addTo.add(parseArcByCenterPoint(crs));
                            break;
                        }
                        case GML3Tags.ARC_BY_BULGE: {
                            addTo.add(parseArcByBulge(crs));
                            break;
                        }
                        case GML3Tags.CIRCLE:
                        case GML3Tags.CIRCLE_BY_CENTER_POINT:
                        case GML3Tags.CUBIC_SPLINE:
                        case GML3Tags.BSPLINE:
                        case GML3Tags.BEZIER:
                        case GML3Tags.CLOTHOID:
                        case GML3Tags.OFFSET_CURVE: {
                            throw new DataStoreContentException(notImplemented("curve segment"));
                        }
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML3Tags.SEGMENTS.equals(reader.getLocalName())) {
                        return;
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:ArcByCenterPoint>} curve segment: the centre of a circle, that circle's
     * radius, and the bearings at which the arc starts and ends. The cursor must be on the
     * element's {@link #START_ELEMENT} event, and is left on its matching {@link #END_ELEMENT}.
     *
     * <p>The centre may be given either as a coordinate-carrying child ({@code gml:pos} and,
     * tolerantly, the GML 2.0 encodings) or as a {@code gml:pointProperty} holding a
     * {@code gml:Point}. The {@code numArc} and {@code interpolation} attributes are ignored: the
     * schema fixes both, so they carry no information.</p>
     *
     * <p>Both angles are required here even though the schema makes them optional, because an arc
     * with no angular extent is not an arc. The element that means <q>the whole circle</q> is
     * {@code gml:CircleByCenterPoint}, which is a different element and is reported as deferred.</p>
     */
    private ArcByCenterPoint parseArcByCenterPoint(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final PositionListBuilder coordinates = new PositionListBuilder();
        Point center     = null;
        double radius    = Double.NaN;
        Unit<?> unit     = null;
        double startAngle = Double.NaN;
        double endAngle   = Double.NaN;
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML3Tags.POINT_PROPERTY: {
                            center = parseMember(Point.class, GML3Tags.POINT_PROPERTY, crs);
                            break;
                        }
                        case GML3Tags.RADIUS: {
                            unit   = unitOfMeasure();       // Before `measure(…)`, which consumes the element.
                            radius = measure(GML3Tags.RADIUS);
                            break;
                        }
                        case GML3Tags.START_ANGLE: startAngle = angle(GML3Tags.START_ANGLE); break;
                        case GML3Tags.END_ANGLE:   endAngle   = angle(GML3Tags.END_ANGLE);   break;
                        default: {
                            if (!parseCoordinateElement(coordinates, crs)) {
                                skipUntilEnd();
                            }
                            break;
                        }
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML3Tags.ARC_BY_CENTER_POINT.equals(reader.getLocalName())) {
                        if (center == null) {
                            if (coordinates.size() != 1) {
                                throw new DataStoreContentException("A GML 3 ArcByCenterPoint must give its"
                                        + " centre as either a pointProperty or exactly one coordinate tuple,"
                                        + " but " + coordinates.size() + " tuples were found.");
                            }
                            center = GeometryFactory.createPoint(coordinates.build(crs));
                        }
                        if (Double.isNaN(radius)) {
                            throw new DataStoreContentException("A GML 3 ArcByCenterPoint must contain a radius element.");
                        }
                        if (Double.isNaN(startAngle) || Double.isNaN(endAngle)) {
                            throw new DataStoreContentException("A GML 3 ArcByCenterPoint must contain both a"
                                    + " startAngle and an endAngle element.");
                        }
                        try {
                            return GeometryFactory.createArcByCenterPoint(center, radius, unit, startAngle, endAngle);
                        } catch (IllegalArgumentException e) {
                            throw new DataStoreContentException(e.getMessage(), e);
                        }
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:ArcByBulge>} curve segment: the two end points of an arc, the distance
     * by which it bulges away from the chord joining them, and the direction of that bulge. The
     * cursor must be on the element's {@link #START_ELEMENT} event, and is left on its matching
     * {@link #END_ELEMENT}.
     *
     * <p>The {@code gml:normal} is required, as the schema requires it: without it the two arcs
     * joining the end points cannot be told apart, and picking one would be a coin toss dressed up
     * as a geometry. The {@code numArc} and {@code interpolation} attributes are ignored, both
     * being fixed by the schema.</p>
     */
    private ArcByBulge parseArcByBulge(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final PositionListBuilder coordinates = new PositionListBuilder();
        double bulge = Double.NaN;
        double[] normal = null;
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML3Tags.BULGE:  bulge  = measure(GML3Tags.BULGE);         break;
                        case GML3Tags.NORMAL: normal = ordinates(GML3Tags.NORMAL);      break;
                        default: {
                            if (!parseCoordinateElement(coordinates, crs)) {
                                skipUntilEnd();
                            }
                            break;
                        }
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML3Tags.ARC_BY_BULGE.equals(reader.getLocalName())) {
                        if (coordinates.size() != 2) {
                            throw new DataStoreContentException("A GML 3 ArcByBulge must contain exactly two"
                                    + " coordinate tuples, its start and its end, but " + coordinates.size()
                                    + " were found.");
                        }
                        if (Double.isNaN(bulge)) {
                            throw new DataStoreContentException("A GML 3 ArcByBulge must contain a bulge element.");
                        }
                        if (normal == null) {
                            throw new DataStoreContentException("A GML 3 ArcByBulge must contain a normal element.");
                        }
                        final Vector<?> direction = Vectors.createDouble(normal.length);
                        direction.set(normal);
                        try {
                            return GeometryFactory.createArcByBulge(coordinates.build(crs), bulge, direction);
                        } catch (IllegalArgumentException e) {
                            throw new DataStoreContentException(e.getMessage(), e);
                        }
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Returns the unit declared by the {@code uom} attribute of the element the cursor is on, or
     * {@code null} if the attribute is absent or empty. An absent unit is not an error: GML
     * documents in the wild routinely omit it, and it then means the units of the coordinate
     * system axes.
     */
    private Unit<?> unitOfMeasure() throws DataStoreContentException {
        final String uom = reader.getAttributeValue(null, GML3Tags.UOM);
        if (uom == null || uom.isBlank()) {
            return null;
        }
        try {
            return Units.valueOf(uom.trim());
        } catch (MeasurementParseException e) {
            throw new DataStoreContentException("Cannot interpret \"" + uom + "\" as the unit of measurement"
                    + " of a GML 3 <" + reader.getLocalName() + "> element.", e);
        }
    }

    /**
     * Reads the text content of the element the cursor is on as a single number. The cursor must be
     * on the element's {@link #START_ELEMENT} event, and is left on its matching
     * {@link #END_ELEMENT}.
     */
    private double measure(final String tagName) throws XMLStreamException, DataStoreContentException {
        final String text = reader.getElementText().trim();
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new DataStoreContentException("A GML 3 <" + tagName + "> element must contain a number,"
                    + " but contains \"" + text + "\".", e);
        }
    }

    /**
     * Reads an angle-valued element, converted to the decimal degrees that
     * {@link ArcByCenterPoint} reports. The cursor must be on the element's
     * {@link #START_ELEMENT} event, and is left on its matching {@link #END_ELEMENT}.
     */
    private double angle(final String tagName) throws XMLStreamException, DataStoreContentException {
        final Unit<?> unit = unitOfMeasure();        // Before `measure(…)`, which consumes the element.
        final double value = measure(tagName);
        if (unit == null || Units.DEGREE.equals(unit)) {
            return value;
        }
        try {
            return Units.ensureAngular(unit).getConverterTo(Units.DEGREE).convert(value);
        } catch (IllegalArgumentException e) {
            throw new DataStoreContentException("The unit \"" + unit + "\" declared by a GML 3 <" + tagName
                    + "> element is not an angular unit.", e);
        }
    }

    /**
     * Reads the text content of the element the cursor is on as a whitespace-separated list of
     * numbers, such as the {@code gml:normal} of an arc by bulge. The cursor must be on the
     * element's {@link #START_ELEMENT} event, and is left on its matching {@link #END_ELEMENT}.
     */
    private double[] ordinates(final String tagName) throws XMLStreamException, DataStoreContentException {
        final String text = reader.getElementText().trim();
        if (text.isEmpty()) {
            throw new DataStoreContentException("A GML 3 <" + tagName + "> element must contain at least one number.");
        }
        final String[] tokens = text.split("\\s+");
        final double[] values = new double[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            try {
                values[i] = Double.parseDouble(tokens[i]);
            } catch (NumberFormatException e) {
                throw new DataStoreContentException("A GML 3 <" + tagName + "> element must contain only"
                        + " numbers, but contains \"" + tokens[i] + "\".", e);
            }
        }
        return values;
    }

    /**
     * Parses a {@code <gml:CompositeCurve>} element, whose {@code curveMember} children are joined
     * end to end into a single curve. Unlike {@code gml:MultiCurve}, the members of a composite
     * curve are contiguous, which is why this becomes a {@link CompoundCurve} and not a
     * {@link org.apache.sis.geometries.MultiCurve}.
     */
    private CompoundCurve parseCompositeCurve(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Curve> members = parseCurveMembers(crs, GML3Tags.COMPOSITE_CURVE);
        return GeometryFactory.createCompoundCurve(aggregateCRS(crs, members), members.toArray(Curve[]::new));
    }

    /**
     * Parses a {@code <gml:Ring>} element: a closed sequence of {@code curveMember} children.
     *
     * <p>A ring whose every member is a {@link LineString} could in principle be flattened into a
     * single {@link LinearRing}, but that would fuse vertex sequences that the document kept
     * separate; the components are preserved as a {@link CompoundCurve} instead. A ring made of one
     * linear ring is the exception, since there is nothing to fuse.</p>
     */
    private Curve parseRing(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Curve> members = parseCurveMembers(crs, GML3Tags.RING);
        if (members.size() == 1 && members.get(0) instanceof LinearRing single) {
            return single;
        }
        return GeometryFactory.createCompoundCurve(aggregateCRS(crs, members), members.toArray(Curve[]::new));
    }

    /**
     * Parses an {@code <gml:OrientableCurve>} element. With {@code orientation="-"} the base curve
     * is wrapped so that its reversed traversal stays visible to callers; with {@code "+"} — the
     * schema default — the base curve is returned unchanged, since a positively oriented curve is
     * indistinguishable from the curve itself.
     */
    private Curve parseOrientableCurve(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final boolean reversed = GML3Tags.ORIENTATION_REVERSED.equals(
                reader.getAttributeValue(null, GML3Tags.ORIENTATION));
        final Curve base = parseSingleProperty(Curve.class, GML3Tags.BASE_CURVE,
                GML3Tags.ORIENTABLE_CURVE, crs);
        return reversed ? GeometryFactory.createReversed(base) : base;
    }

    // ////////////////////////////////////////////////////////////////////////
    // Surfaces other than gml:Polygon ////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Parses a {@code <gml:Surface>}, {@code <gml:TriangulatedSurface>} or {@code <gml:Tin>}
     * element, whose {@code <gml:patches>} holds the patches that together form the surface.
     *
     * <p>The result is the most specific type the patches allow: a lone planar patch is returned as
     * a {@link Polygon} on its own, all-triangular patches become a {@link TIN}, other planar
     * patches a {@link PolyhedralSurface}. A patch with a non-linear boundary forces a
     * {@link org.apache.sis.geometries.MultiSurface} instead, because
     * {@code PolyhedralSurface<T extends Polygon>} cannot hold a {@link CurvePolygon} — and with it
     * the guarantee that the patches are contiguous is lost. That is a real degradation, not a
     * relabelling, and the writer consequently emits {@code gml:MultiSurface} for such a surface.</p>
     */
    private Geometry parseSurface(final CoordinateReferenceSystem crs, final String enclosingTagName)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Surface> patches = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (GML3Tags.PATCHES.equals(reader.getLocalName())) {
                        parsePatches(crs, patches);
                    } else {
                        skipUntilEnd();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (enclosingTagName.equals(reader.getLocalName())) {
                        return assembleSurface(crs, patches, enclosingTagName);
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Builds the most specific surface type that the given patches allow. See
     * {@link #parseSurface parseSurface(…)} for the rules and what each choice costs.
     */
    private Geometry assembleSurface(final CoordinateReferenceSystem crs, final List<Surface> patches,
            final String enclosingTagName) throws DataStoreContentException
    {
        final CoordinateReferenceSystem fallback = aggregateCRS(crs, patches);
        /*
         * A gml:Tin or gml:TriangulatedSurface says what its patches must be, so it is validated
         * first and kept as a TIN even when it holds a single triangle: unlike gml:Surface, the
         * element name is itself information worth keeping.
         */
        final boolean triangulated = GML3Tags.TIN.equals(enclosingTagName)
                                  || GML3Tags.TRIANGULATED_SURFACE.equals(enclosingTagName);
        if (triangulated) {
            if (!allInstanceOf(patches, Triangle.class)) {
                throw new DataStoreContentException("A GML 3 <" + enclosingTagName
                        + "> must contain only gml:Triangle patches.");
            }
            return GeometryFactory.createTIN(fallback, patches.toArray(Triangle[]::new));
        }
        if (!allInstanceOf(patches, Polygon.class)) {
            return GeometryFactory.createMultiSurface(fallback, patches.toArray(Surface[]::new));
        }
        if (patches.size() == 1) {
            return patches.get(0);
        }
        if (!patches.isEmpty() && allInstanceOf(patches, Triangle.class)) {
            return GeometryFactory.createTIN(fallback, patches.toArray(Triangle[]::new));
        }
        return GeometryFactory.createPolyhedralSurface(fallback, patches.toArray(Polygon[]::new));
    }

    /**
     * Parses a {@code <gml:patches>} element, appending one surface per patch to the given list.
     * The cursor must be on the {@code patches} element's {@link #START_ELEMENT} event.
     */
    private void parsePatches(final CoordinateReferenceSystem crs, final List<Surface> addTo)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String patch = reader.getLocalName();
                    switch (patch) {
                        case GML3Tags.POLYGON_PATCH:
                        case GML2Tags.POLYGON:
                        case GML3Tags.RECTANGLE: {
                            addTo.add(parseRingBoundedSurface(crs, patch));
                            break;
                        }
                        case GML3Tags.TRIANGLE: {
                            final Surface s = parseRingBoundedSurface(crs, patch);
                            if (!(s instanceof Polygon p)) {
                                throw new DataStoreContentException("A GML 3 Triangle patch must have a linear boundary.");
                            }
                            addTo.add(GeometryFactory.createTriangle(p.getExteriorRing()));
                            break;
                        }
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML3Tags.PATCHES.equals(reader.getLocalName())) {
                        return;
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:CompositeSurface>} element, whose {@code surfaceMember} children are
     * contiguous and together form one surface — the same contract as
     * {@link PolyhedralSurface}, which is therefore what this becomes. A
     * {@link org.apache.sis.geometries.MultiSurface} would be the wrong choice: it explicitly
     * places no contiguity constraint on its members, discarding the strongest thing the document
     * says.
     */
    private Geometry parseCompositeSurface(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Surface> members = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML3Tags.SURFACE_MEMBER:  members.add(parseMember(Surface.class, GML3Tags.SURFACE_MEMBER, crs)); break;
                        case GML3Tags.SURFACE_MEMBERS: parseMemberList(Surface.class, GML3Tags.SURFACE_MEMBERS, crs, members); break;
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML3Tags.COMPOSITE_SURFACE.equals(reader.getLocalName())) {
                        final CoordinateReferenceSystem fallback = aggregateCRS(crs, members);
                        if (allInstanceOf(members, Polygon.class)) {
                            return GeometryFactory.createPolyhedralSurface(fallback, members.toArray(Polygon[]::new));
                        }
                        return GeometryFactory.createMultiSurface(fallback, members.toArray(Surface[]::new));
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses an {@code <gml:OrientableSurface>} element. As for
     * {@link #parseOrientableCurve parseOrientableCurve(…)}, {@code orientation="-"} wraps the base
     * surface and {@code "+"} returns it unchanged.
     */
    private Surface parseOrientableSurface(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final boolean reversed = GML3Tags.ORIENTATION_REVERSED.equals(
                reader.getAttributeValue(null, GML3Tags.ORIENTATION));
        final Surface base = parseSingleProperty(Surface.class, GML3Tags.BASE_SURFACE,
                GML3Tags.ORIENTABLE_SURFACE, crs);
        return reversed ? GeometryFactory.createReversed(base) : base;
    }

    // ////////////////////////////////////////////////////////////////////////
    // Solids /////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Parses a {@code <gml:Solid>} element into a {@link Polyhedron}: its {@code exterior} shell
     * and any {@code interior} shell become the exterior and interior shells of the polyhedron.
     *
     * <p>{@link Polyhedron} is used rather than the more abstract
     * {@link org.apache.sis.geometries.Solid} because it is the type that actually matches a GML
     * solid: it is defined by its bounding shells, one for one with GML's model, whereas
     * {@code Solid} describes a solid through interpolation, control points and knots and has no
     * notion of a shell.</p>
     */
    private Polyhedron parseSolid(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        MultiPolygon exterior = null;
        List<MultiPolygon> interiors = null;
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML3Tags.EXTERIOR: {
                            exterior = parseShellProperty(GML3Tags.EXTERIOR, crs);
                            break;
                        }
                        case GML3Tags.INTERIOR: {
                            if (interiors == null) interiors = new ArrayList<>();
                            interiors.add(parseShellProperty(GML3Tags.INTERIOR, crs));
                            break;
                        }
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML3Tags.SOLID.equals(reader.getLocalName())) {
                        if (exterior == null) {
                            throw new DataStoreContentException("A GML 3 Solid must contain an exterior shell.");
                        }
                        return GeometryFactory.createPolyhedron(exterior, interiors);
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses an {@code <gml:exterior>} or {@code <gml:interior>} element of a solid, which holds
     * exactly one {@code <gml:Shell>}. The cursor must be on the wrapper's
     * {@link #START_ELEMENT} event, and is left on its matching {@link #END_ELEMENT}.
     */
    private MultiPolygon parseShellProperty(final String wrapperTagName, final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        MultiPolygon shell = null;
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (GML3Tags.SHELL.equals(reader.getLocalName())) {
                        shell = parseShell(crs);
                    } else {
                        skipUntilEnd();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (wrapperTagName.equals(reader.getLocalName())) {
                        if (shell == null) {
                            throw new DataStoreContentException("A GML 3 " + wrapperTagName
                                    + " of a solid must contain a Shell element.");
                        }
                        return shell;
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:Shell>} element: a closed set of polygons, held here as a
     * {@link MultiPolygon}. An empty shell is accepted, since the schema allows it.
     */
    private MultiPolygon parseShell(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Polygon> faces = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML3Tags.SURFACE_MEMBER:  faces.add(parseMember(Polygon.class, GML3Tags.SURFACE_MEMBER, crs)); break;
                        case GML3Tags.SURFACE_MEMBERS: parseMemberList(Polygon.class, GML3Tags.SURFACE_MEMBERS, crs, faces); break;
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML3Tags.SHELL.equals(reader.getLocalName())) {
                        return GeometryFactory.createMultiPolygon(aggregateCRS(crs, faces),
                                faces.toArray(Polygon[]::new));
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:CompositeSolid>} or {@code <gml:MultiSolid>} element into a
     * {@link MultiPolyhedron}. Both map onto the same type: a composite solid's members are
     * contiguous and a multi-solid's need not be, but the Apache SIS model carries no such
     * distinction, so it survives only in the element hint recorded for the writer.
     */
    private MultiPolyhedron parseMultiSolid(final CoordinateReferenceSystem crs, final String enclosingTagName)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Polyhedron> members = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML3Tags.SOLID_MEMBER:  members.add(parseMember(Polyhedron.class, GML3Tags.SOLID_MEMBER, crs)); break;
                        case GML3Tags.SOLID_MEMBERS: parseMemberList(Polyhedron.class, GML3Tags.SOLID_MEMBERS, crs, members); break;
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (enclosingTagName.equals(reader.getLocalName())) {
                        return GeometryFactory.createMultiPolyhedron(aggregateCRS(crs, members),
                                members.toArray(Polyhedron[]::new));
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // Shared helpers /////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Parses the {@code curveMember} / {@code curveMembers} children of the given element.
     * The cursor must be on that element's {@link #START_ELEMENT} event, and is left on its
     * matching {@link #END_ELEMENT}.
     */
    private List<Curve> parseCurveMembers(final CoordinateReferenceSystem crs, final String enclosingTagName)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Curve> members = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML3Tags.CURVE_MEMBER:  members.add(parseMember(Curve.class, GML3Tags.CURVE_MEMBER, crs)); break;
                        case GML3Tags.CURVE_MEMBERS: parseMemberList(Curve.class, GML3Tags.CURVE_MEMBERS, crs, members); break;
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (enclosingTagName.equals(reader.getLocalName())) {
                        return members;
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a wrapper element holding exactly one geometry of the given type, such as
     * {@code <gml:baseCurve>} inside a {@code <gml:OrientableCurve>}. The cursor must be on the
     * <em>enclosing</em> element's {@link #START_ELEMENT} event, and is left on its matching
     * {@link #END_ELEMENT}.
     */
    private <G extends Geometry> G parseSingleProperty(final Class<G> type, final String propertyTagName,
            final String enclosingTagName, final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        G value = null;
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (propertyTagName.equals(reader.getLocalName())) {
                        value = parseMember(type, propertyTagName, crs);
                    } else {
                        skipUntilEnd();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (enclosingTagName.equals(reader.getLocalName())) {
                        if (value == null) {
                            throw new DataStoreContentException("A GML 3 " + enclosingTagName
                                    + " must contain a " + propertyTagName + " element.");
                        }
                        return value;
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }
}
