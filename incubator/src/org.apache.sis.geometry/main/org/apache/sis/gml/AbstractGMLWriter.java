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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.Empty;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.MultiCurve;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.MultiSurface;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Base class of GML geometry writers.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class AbstractGMLWriter implements GMLGeometryWriter {
    /**
     * The underlying StAX cursor.
     */
    protected final XMLStreamWriter writer;

    /**
     * Creates a new writer using the given StAX cursor.
     * The XML document header is written immediately by this constructor.
     *
     * @param  writer  the StAX cursor to write to.
     * @throws XMLStreamException if the XML document header cannot be written.
     */
    protected AbstractGMLWriter(final XMLStreamWriter writer) throws XMLStreamException {
        this.writer = writer;
        writer.writeStartDocument("UTF-8", "1.0");
    }

    /**
     * Returns a the GML version handled by this writer.
     */
    protected abstract GMLVersion getVersion();

    /**
     * Returns the XML namespace this writer emits its elements in.
     */
    protected abstract String namespace();

    /*
     * One hook per geometry kind that both GML versions can express. Each receives the
     * `srsName` value to write (or null to omit the attribute) and whether it should declare
     * the default namespace, which only the outermost element does.
     */

    protected abstract void writePoint(Point g, String srsName, boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    protected abstract void writeLineString(LineString g, String srsName, boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    protected abstract void writeLinearRing(LinearRing g, String srsName, boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    protected abstract void writePolygon(Polygon g, String srsName, boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    protected abstract void writeBBox(BBox g, String srsName, boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    protected abstract void writeMultiPoint(MultiPoint<?> g, String srsName, boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    protected abstract void writeMultiCurve(MultiCurve<?> g, String srsName, boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    protected abstract void writeMultiSurface(MultiSurface<?> g, String srsName, boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    protected abstract void writeMultiGeometry(GeometryCollection<?> g, String srsName, boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    protected abstract void writeEmpty(Empty g, String srsName, boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    /**
     * Writes a geometry kind beyond the set that both GML versions share. Subclasses override this
     * to widen their coverage; the default claims nothing, so the caller reports the gap without
     * approximating anything.
     *
     * <p>This hook is consulted <em>before</em> the {@link GeometryCollection} catch-all, because
     * {@link org.apache.sis.geometries.MultiPolyhedron} extends {@code GeometryCollection} and would
     * otherwise be swallowed by it. An implementation must therefore return {@code false} for a
     * plain {@code GeometryCollection} it does not specifically recognise, or collections would
     * never reach {@link #writeMultiGeometry writeMultiGeometry(…)}.</p>
     *
     * @return {@code true} if the geometry was written, {@code false} to let the caller continue
     *         the dispatch and, failing that, report it as unsupported.
     */
    protected boolean writeExtendedGeometry(final Geometry geometry, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        return false;
    }

    /**
     * Writes the given geometry as a GML element. The {@code srsName} attribute is derived from
     * {@link Geometry#getCoordinateReferenceSystem()} and from the verbatim value that a reader may
     * have recorded in the geometry's user properties.
     */
    @Override
    public final void writeGeometry(final Geometry geometry)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        ensureNonNull(geometry);
        writer.setDefaultNamespace(namespace());
        writeGeometryElement(geometry,
                GMLCRS.srsName(geometry.getCoordinateReferenceSystem(), geometry.userProperties()), true);
    }

    /**
     * Writes the given geometry as a GML element, using the given CRS for the {@code srsName}
     * attribute instead of the geometry's own.
     *
     * <p>A CRS supplied here is authoritative: the {@code srsName} is derived from it alone, and a
     * verbatim {@code srsName} that a reader may have recorded in the geometry's user properties is
     * deliberately <em>not</em> consulted. Honouring the recorded value would silently discard the
     * caller's explicit choice, which is the whole point of this overload.</p>
     */
    @Override
    public final void writeGeometry(final Geometry geometry, final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        ensureNonNull(geometry);
        writer.setDefaultNamespace(namespace());
        writeGeometryElement(geometry, GMLCRS.srsName(crs, null), true);
    }

    /**
     * Verifies that a geometry was actually given, before any of its properties are read.
     */
    private void ensureNonNull(final Geometry geometry) throws DataStoreContentException {
        if (geometry == null) {
            throw new DataStoreContentException("Cannot write a null geometry as " + getVersion().name() + '.');
        }
    }

    /**
     * Dispatches to the method matching the runtime type of the given geometry.
     */
    protected final void writeGeometryElement(final Geometry geometry, final String srsName, final boolean declareNamespace)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        ensureNonNull(geometry);        // A collection member may itself be null.
        /*
         * `BBox` is an envelope whose `getGeometryType()` claims to be a polygon, and `Empty` has no
         * coordinates at all; both are settled before the hierarchy walk below.
         */
        if (geometry instanceof BBox g) {
            writeBBox(g, srsName, declareNamespace);
        } else if (geometry instanceof Empty g) {
            writeEmpty(g, srsName, declareNamespace);
        } else if (geometry instanceof Point g) {
            writePoint(g, srsName, declareNamespace);
        } else if (geometry instanceof LinearRing g) {
            writeLinearRing(g, srsName, declareNamespace);
        } else if (geometry instanceof LineString g) {
            writeLineString(g, srsName, declareNamespace);
        } else if (geometry instanceof Polygon g) {
            writePolygon(g, srsName, declareNamespace);
        } else if (geometry instanceof MultiPoint<?> g) {
            writeMultiPoint(g, srsName, declareNamespace);
        } else if (geometry instanceof MultiCurve<?> g) {
            writeMultiCurve(g, srsName, declareNamespace);
        } else if (geometry instanceof MultiSurface<?> g) {
            writeMultiSurface(g, srsName, declareNamespace);
        } else if (!writeExtendedGeometry(geometry, srsName, declareNamespace)) {
            /*
             * Every remaining curve and surface kind -- CompoundCurve, CircularString, CurvePolygon,
             * PolyhedralSurface, the reversed-orientation wrappers -- is offered to the subclass
             * first, and only then reported. GeometryCollection is tested last because MultiPoint,
             * MultiCurve and MultiSurface all extend it.
             */
            if (geometry instanceof GeometryCollection<?> g) {
                writeMultiGeometry(g, srsName, declareNamespace);
            } else {
                throw new DataStoreContentException(unsupportedType(geometry));
            }
        }
    }

    /**
     * Returns the error message for a geometry kind this GML version cannot express.
     */
    protected final String unsupportedType(final Geometry geometry) {
        final String kind = (geometry instanceof Curve) ? "curve"
                          : (geometry instanceof Surface) ? "surface"
                          : "geometry";
        return getVersion().name() + " has no representation for the " + kind + " type "
                + geometry.getGeometryType() + " (" + geometry.getClass().getSimpleName()
                + "); no tessellation is performed.";
    }

    /**
     * Writes the start tag of a geometry element, with the {@code xmlns} declaration
     * (only if {@code declareNamespace} is true, i.e. only for the outermost element)
     * and the {@code srsName} attribute (only if non-null).
     */
    protected final void writeStart(final String tagName, final String srsName, final boolean declareNamespace)
            throws XMLStreamException
    {
        writer.writeStartElement(tagName);
        if (declareNamespace) {
            writer.writeDefaultNamespace(namespace());
        }
        if (srsName != null) {
            writer.writeAttribute(GML2Tags.SRS_NAME, srsName);
        }
    }

    /**
     * Appends the ordinates of one coordinate tuple to the given buffer, separated by spaces.
     */
    protected static void appendTuple(final StringBuilder sb, final Tuple<?> tuple) {
        for (int i = 0, n = tuple.getDimension(); i < n; i++) {
            if (i != 0) sb.append(' ');
            sb.append(tuple.get(i));
        }
    }

    /**
     * Appends every tuple of the given sequence to the given buffer, all separated by spaces.
     */
    protected static void appendSequence(final StringBuilder sb, final PointSequence points) {
        for (int i = 0, n = points.size(); i < n; i++) {
            if (i != 0) sb.append(' ');
            appendTuple(sb, points.getPosition(i));
        }
    }

    /**
     * Writes the end of the XML document and closes the underlying StAX cursor.
     *
     * @throws XMLStreamException if an error occurred while writing or closing the cursor.
     */
    @Override
    public final void close() throws XMLStreamException {
        writer.writeEndDocument();
        writer.close();
    }
}
