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
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Empty;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.MultiCurve;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.MultiSurface;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;


/**
 * Writer of GML 2.0 geometries.
 *
 * <p>Usage:</p>
 * {@snippet lang="java" :
 *     try (GML2Writer writer = new GML2Writer(outputStream)) {
 *         writer.writeGeometry(geometry);
 *     }
 *     }
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GML2Writer extends AbstractGMLWriter {
    /**
     * Creates a new writer using the given StAX cursor.
     * The XML document header is written immediately by this constructor.
     *
     * @param  writer  the StAX cursor to write to.
     * @throws XMLStreamException if the XML document header cannot be written.
     */
    public GML2Writer(final XMLStreamWriter writer) throws XMLStreamException {
        super(writer);
    }

    /**
     * Creates a new writer for the given output stream.
     *
     * @param  out  the stream to write to.
     * @throws XMLStreamException if the StAX cursor cannot be created.
     */
    public GML2Writer(final OutputStream out) throws XMLStreamException {
        this(XMLOutputFactory.newInstance().createXMLStreamWriter(out, "UTF-8"));
    }

    @Override
    protected GMLVersion getVersion() {
        return GMLVersion.V2;
    }

    /**
     * Returns the GML 2.0 namespace.
     */
    @Override
    protected String namespace() {
        return GML2Tags.NAMESPACE_2;
    }

    /**
     * Writes a {@code <gml:Point>} element.
     */
    @Override
    protected void writePoint(final Point g, final String srsName, final boolean declareNamespace) throws XMLStreamException {
        writeStart(GML2Tags.POINT, srsName, declareNamespace);
        writeCoordinates(g.asPointSequence());
        writer.writeEndElement();
    }

    /**
     * Writes a {@code <gml:LineString>} element.
     */
    @Override
    protected void writeLineString(final LineString g, final String srsName, final boolean declareNamespace) throws XMLStreamException {
        writeStart(GML2Tags.LINE_STRING, srsName, declareNamespace);
        writeCoordinates(g.getPoints());
        writer.writeEndElement();
    }

    /**
     * Writes a {@code <gml:LinearRing>} element.
     */
    @Override
    protected void writeLinearRing(final LinearRing g, final String srsName, final boolean declareNamespace) throws XMLStreamException {
        writeStart(GML2Tags.LINEAR_RING, srsName, declareNamespace);
        writeCoordinates(g.getPoints());
        writer.writeEndElement();
    }

    /**
     * Writes a {@code <gml:Polygon>} element, with its {@code outerBoundaryIs} and, if any,
     * {@code innerBoundaryIs} children.
     */
    @Override
    protected void writePolygon(final Polygon g, final String srsName, final boolean declareNamespace) throws XMLStreamException {
        writeStart(GML2Tags.POLYGON, srsName, declareNamespace);
        writer.writeStartElement(GML2Tags.OUTER_BOUNDARY_IS);
        writeLinearRing(g.getExteriorRing(), null, false);
        writer.writeEndElement();
        final int n = g.getNumInteriorRing();
        for (int i=0; i<n; i++) {
            writer.writeStartElement(GML2Tags.INNER_BOUNDARY_IS);
            writeLinearRing(g.getInteriorRingN(i), null, false);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Writes a {@code <gml:Box>} element, with its two corners in a single
     * {@code <gml:coordinates>} element.
     *
     * <p>GML 2.0's {@code gml:coordinates} places no upper bound on the width of a tuple, so a box
     * of more than three dimensions is written in full rather than truncated. Note that a GML 2.0
     * parser built strictly to the schema's {@code CoordType} may only understand the first three
     * ordinates.</p>
     */
    @Override
    protected void writeBBox(final BBox g, final String srsName, final boolean declareNamespace) throws XMLStreamException {
        writeStart(GML2Tags.BOX, srsName, declareNamespace);
        final int dim = g.getDimension();
        final StringBuilder sb = new StringBuilder();
        for (int i=0; i<dim; i++) {
            if (i != 0) sb.append(GML2Tags.DEFAULT_CS);
            sb.append(g.getMinimum(i));
        }
        sb.append(GML2Tags.DEFAULT_TS);
        for (int i=0; i<dim; i++) {
            if (i != 0) sb.append(GML2Tags.DEFAULT_CS);
            sb.append(g.getMaximum(i));
        }
        writer.writeStartElement(GML2Tags.COORDINATES);
        writer.writeCharacters(sb.toString());
        writer.writeEndElement();
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
     * Writes a {@code <gml:MultiLineString>} element, with its {@code lineStringMember} children.
     * Only a collection whose every member is a {@link LineString} can be written: GML 2.0 has no
     * {@code MultiCurve}.
     */
    @Override
    protected void writeMultiCurve(final MultiCurve<?> g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        for (int i=0, n=g.getNumGeometries(); i<n; i++) {
            if (!(g.getGeometryN(i) instanceof LineString)) {
                throw new DataStoreContentException("GML 2.0 has no representation for a curve collection"
                        + " whose member " + i + " is a " + g.getGeometryN(i).getGeometryType()
                        + "; only line strings can be written. No tessellation is performed.");
            }
        }
        writeMembers(GML2Tags.MULTI_LINE_STRING, GML2Tags.LINE_STRING_MEMBER, g, srsName, declareNamespace);
    }

    /**
     * Writes a {@code <gml:MultiPolygon>} element, with its {@code polygonMember} children.
     * Only a collection whose every member is a {@link Polygon} can be written: GML 2.0 has no
     * {@code MultiSurface}.
     */
    @Override
    protected void writeMultiSurface(final MultiSurface<?> g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        for (int i=0, n=g.getNumGeometries(); i<n; i++) {
            if (!(g.getGeometryN(i) instanceof Polygon)) {
                throw new DataStoreContentException("GML 2.0 has no representation for a surface collection"
                        + " whose member " + i + " is a " + g.getGeometryN(i).getGeometryType()
                        + "; only polygons can be written. No tessellation is performed.");
            }
        }
        writeMembers(GML2Tags.MULTI_POLYGON, GML2Tags.POLYGON_MEMBER, g, srsName, declareNamespace);
    }

    /**
     * Writes a {@code <gml:MultiGeometry>} element, with its {@code geometryMember} children.
     * Members may be of any GML 2.0 geometry type.
     */
    @Override
    protected void writeMultiGeometry(final GeometryCollection<?> g, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        writeMembers(GML2Tags.MULTI_GEOMETRY, GML2Tags.GEOMETRY_MEMBER, g, srsName, declareNamespace);
    }

    /**
     * Writes an empty geometry as the GML element it was read from, if known, or as an empty
     * {@code <gml:MultiGeometry>} otherwise — the only GML 2.0 geometry element whose content
     * model allows zero children.
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
     * Writes a {@code <gml:coordinates>} element containing the given coordinate tuples, using the
     * GML 2.0 default separators (a comma between the ordinates of a tuple, a space between tuples).
     */
    private void writeCoordinates(final PointSequence points) throws XMLStreamException {
        final StringBuilder sb = new StringBuilder();
        for (int i=0, n=points.size(); i<n; i++) {
            if (i != 0) sb.append(GML2Tags.DEFAULT_TS);
            final var tuple = points.getPosition(i);
            for (int d=0, dim=tuple.getDimension(); d<dim; d++) {
                if (d != 0) sb.append(GML2Tags.DEFAULT_CS);
                sb.append(tuple.get(d));
            }
        }
        writer.writeStartElement(GML2Tags.COORDINATES);
        writer.writeCharacters(sb.toString());
        writer.writeEndElement();
    }
}
