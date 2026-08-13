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
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.MultiLineString;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.MultiPolygon;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Reader of GML 2.0 geometries.
 *
 * <p>Usage:</p>
 * {@snippet lang="java" :
 *     try (GML2Reader reader = new GML2Reader(inputStream)) {
 *         Geometry geometry = reader.readGeometry();
 *     }
 *     }
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GML2Reader extends AbstractGMLReader {
    /**
     * Creates a new reader using the given StAX cursor.
     * The cursor can be positioned anywhere before the geometry root element.
     *
     * @param  reader  the StAX cursor to read from.
     */
    public GML2Reader(final XMLStreamReader reader) {
        super(reader);
    }

    /**
     * Creates a new reader for the given input stream.
     *
     * @param  in  the stream to read from.
     * @throws XMLStreamException if the StAX cursor cannot be created.
     */
    public GML2Reader(final InputStream in) throws XMLStreamException {
        this(XMLInputFactory.newInstance().createXMLStreamReader(in));
    }

    @Override
    protected GMLVersion getVersion() {
        return GMLVersion.V2;
    }

    /**
     * Returns {@code true} if the given namespace can be considered as the GML 2.0 namespace.
     * A missing or empty namespace is tolerated, for interoperability with GML 2.0 documents
     * that omit the {@code xmlns} declaration.
     */
    @Override
    protected boolean isGML(final String ns) {
        return (ns == null) || ns.isEmpty() || GML2Tags.NAMESPACE_2.equals(ns);
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
            case GML2Tags.POINT:             geometry = parsePoint(crs);           break;
            case GML2Tags.LINE_STRING:       geometry = parseLineString(crs);      break;
            case GML2Tags.LINEAR_RING:       geometry = parseLinearRing(crs);      break;
            case GML2Tags.POLYGON:           geometry = parsePolygon(crs);         break;
            case GML2Tags.BOX:               geometry = parseBox(crs);             break;
            case GML2Tags.MULTI_POINT:       geometry = parseMultiPoint(crs);      break;
            case GML2Tags.MULTI_LINE_STRING: geometry = parseMultiLineString(crs); break;
            case GML2Tags.MULTI_POLYGON:     geometry = parseMultiPolygon(crs);    break;
            case GML2Tags.MULTI_GEOMETRY:    geometry = parseMultiGeometry(crs);   break;
            default: throw new DataStoreContentException(unsupportedElement());
        }
        return annotate(geometry, element, srsName);
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
            throw new DataStoreContentException("A GML 2.0 Point must contain exactly one coordinate tuple.");
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
            throw new DataStoreContentException("A GML 2.0 LineString must contain at least two coordinate tuples.");
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
            throw new DataStoreContentException("A GML 2.0 LinearRing must contain at least four coordinate tuples.");
        }
        return GeometryFactory.createLinearRing(coordinates.build(crs));
    }

    /**
     * Parses a {@code <gml:Polygon>} element.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private Polygon parsePolygon(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        LinearRing shell = null;
        List<LinearRing> holes = null;
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML2Tags.OUTER_BOUNDARY_IS: {
                            shell = parseBoundary(GML2Tags.OUTER_BOUNDARY_IS, crs);
                            break;
                        }
                        case GML2Tags.INNER_BOUNDARY_IS: {
                            if (holes == null) holes = new ArrayList<>();
                            holes.add(parseBoundary(GML2Tags.INNER_BOUNDARY_IS, crs));
                            break;
                        }
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML2Tags.POLYGON.equals(reader.getLocalName())) {
                        if (shell == null) {
                            throw new DataStoreContentException("A GML 2.0 Polygon must contain an outerBoundaryIs element.");
                        }
                        return GeometryFactory.createPolygon(shell, holes);
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses an {@code <gml:outerBoundaryIs>} or {@code <gml:innerBoundaryIs>} element, which must
     * contain exactly one {@code <gml:LinearRing>} child. The cursor must be on the wrapper element's
     * {@link #START_ELEMENT} event. After this method returns, the cursor is on the matching
     * {@link #END_ELEMENT} event of the wrapper.
     */
    private LinearRing parseBoundary(final String wrapperTagName, final CoordinateReferenceSystem inherited)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        LinearRing ring = null;
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (GML2Tags.LINEAR_RING.equals(reader.getLocalName())) {
                        final String srsName = reader.getAttributeValue(null, GML2Tags.SRS_NAME);
                        ring = annotate(parseLinearRing(GMLCRS.resolve(srsName, inherited)),
                                        GML2Tags.LINEAR_RING, srsName);
                    } else {
                        skipUntilEnd();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (wrapperTagName.equals(reader.getLocalName())) {
                        if (ring == null) {
                            throw new DataStoreContentException("A GML 2.0 " + wrapperTagName + " must contain a LinearRing element.");
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
     * Parses a {@code <gml:Box>} element into a {@link BBox}, the Apache SIS envelope geometry.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private BBox parseBox(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final PositionListBuilder coordinates = parseCoordinateSequence(GML2Tags.BOX, crs);
        if (coordinates.size() != 2) {
            throw new DataStoreContentException("A GML 2.0 Box must contain exactly two coordinate tuples.");
        }
        return newBBox(GMLCRS.forDimension(crs, coordinates.dimension()), coordinates.toFlatArray());
    }

    /**
     * Creates a bounding box from the lower corner ordinates followed by the upper corner ordinates,
     * translating the {@code IllegalArgumentException} of an inverted box into a content exception.
     */
    static BBox newBBox(final CoordinateReferenceSystem crs, final double[] corners) throws DataStoreContentException {
        try {
            return new BBox(crs, corners);
        } catch (IllegalArgumentException e) {
            throw new DataStoreContentException("Invalid GML envelope: the lower corner is greater than the upper corner.", e);
        }
    }

    /**
     * Parses a {@code <gml:MultiPoint>} element.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private MultiPoint parseMultiPoint(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Point> members = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (GML2Tags.POINT_MEMBER.equals(reader.getLocalName())) {
                        members.add(parseMember(Point.class, GML2Tags.POINT_MEMBER, crs));
                    } else {
                        skipUntilEnd();
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
     * Parses a {@code <gml:MultiLineString>} element.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private MultiLineString parseMultiLineString(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<LineString> members = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (GML2Tags.LINE_STRING_MEMBER.equals(reader.getLocalName())) {
                        members.add(parseMember(LineString.class, GML2Tags.LINE_STRING_MEMBER, crs));
                    } else {
                        skipUntilEnd();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML2Tags.MULTI_LINE_STRING.equals(reader.getLocalName())) {
                        return GeometryFactory.createMultiLineString(aggregateCRS(crs, members),
                                members.toArray(LineString[]::new));
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:MultiPolygon>} element.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private MultiPolygon parseMultiPolygon(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Polygon> members = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (GML2Tags.POLYGON_MEMBER.equals(reader.getLocalName())) {
                        members.add(parseMember(Polygon.class, GML2Tags.POLYGON_MEMBER, crs));
                    } else {
                        skipUntilEnd();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML2Tags.MULTI_POLYGON.equals(reader.getLocalName())) {
                        return GeometryFactory.createMultiPolygon(aggregateCRS(crs, members),
                                members.toArray(Polygon[]::new));
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:MultiGeometry>} element. Members may be of any GML 2.0 geometry type.
     * The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    private Geometry parseMultiGeometry(final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        final List<Geometry> members = new ArrayList<>();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (GML2Tags.GEOMETRY_MEMBER.equals(reader.getLocalName())) {
                        members.add(parseMember(Geometry.class, GML2Tags.GEOMETRY_MEMBER, crs));
                    } else {
                        skipUntilEnd();
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
}
