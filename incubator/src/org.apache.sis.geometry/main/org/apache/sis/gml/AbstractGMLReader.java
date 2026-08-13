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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Base class of the version-specific GML geometry readers.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class AbstractGMLReader implements GMLGeometryReader, XMLStreamConstants {
    /**
     * The underlying StAX cursor.
     */
    protected final XMLStreamReader reader;

    /**
     * Creates a new reader using the given StAX cursor.
     *
     * @param  reader  the StAX cursor to read from.
     */
    protected AbstractGMLReader(final XMLStreamReader reader) {
        this.reader = reader;
    }

    /**
     * Returns a the GML version handled by this reader.
     */
    protected abstract GMLVersion getVersion();

    /**
     * Returns {@code true} if the given namespace can be considered as a GML namespace
     * handled by this reader. A missing or empty namespace is tolerated by both versions,
     * for interoperability with documents that omit the {@code xmlns} declaration.
     */
    protected abstract boolean isGML(String namespace);

    /**
     * Parses the geometry element at the current cursor position. The cursor must be on the
     * {@link #START_ELEMENT} event of the geometry to parse. After this method returns, the
     * cursor is on the matching {@link #END_ELEMENT} event.
     *
     * @param  inherited  the CRS inherited from an enclosing geometry element, or {@code null} if none.
     */
    protected abstract Geometry parseGeometryElement(CoordinateReferenceSystem inherited)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    /**
     * Appends the coordinates of the coordinate-carrying element at the current cursor position to
     * the given builder. The cursor is on a {@link #START_ELEMENT} event whose local name was not
     * recognised by {@link #parseCoordinateSequence parseCoordinateSequence(…)} itself.
     *
     * <p>Implementations should delegate to {@code super.parseCoordinateElement(target, inScope)}
     * for the encodings shared with GML 2.0.</p>
     *
     * @param  target   where to append the coordinates read.
     * @param  inScope  the CRS resolved for the enclosing geometry element, or {@code null} if none.
     *                  Used only to infer a tuple width that the document leaves implicit.
     * @return whether the element was recognised and fully consumed. When {@code false}, the caller
     *         skips the element.
     */
    protected boolean parseCoordinateElement(final PositionListBuilder target,
            final CoordinateReferenceSystem inScope)
            throws XMLStreamException, DataStoreContentException
    {
        switch (reader.getLocalName()) {
            case GML2Tags.COORDINATES: parseCoordinatesText(target); return true;
            case GML2Tags.COORD:       parseCoord(target);           return true;
            default: return false;
        }
    }

    /**
     * Reads the next GML geometry element.
     *
     * @return the geometry read from the underlying StAX cursor.
     */
    @Override
    public final Geometry readGeometry()
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        moveToGeometryRoot();
        return parseGeometryElement(null);
    }

    /**
     * Moves the cursor to the first {@link #START_ELEMENT}, skipping the XML prologue
     * (declaration, comments, DTD, etc.). Does nothing if the cursor is already there.
     */
    protected final void moveToGeometryRoot() throws XMLStreamException, DataStoreContentException {
        int type = reader.getEventType();
        while (type != START_ELEMENT) {
            if (!reader.hasNext()) {
                throw new DataStoreContentException("No GML geometry element found.");
            }
            type = reader.next();
        }
    }

    /**
     * Returns the error message for an element that is not a supported geometry.
     */
    protected final String unsupportedElement() {
        return "Unsupported " + getVersion().name() + " geometry element: \"" + reader.getLocalName() + "\".";
    }

    /**
     * Returns the error message for a truncated document.
     */
    protected final String endOfDocument() {
        return "Unexpected end of document while parsing a " + getVersion().name() + " geometry.";
    }

    /**
     * Records, in the user properties of the given geometry, the local name of the element it was
     * read from and the verbatim {@code srsName} attribute value, so that a writer can reproduce
     * the original spelling. Both are advisory; this method is a no-op for a geometry whose
     * {@link Geometry#userProperties()} is null.
     *
     * <p>Must be called while the cursor is still on the element being parsed, or with the values
     * captured beforehand.</p>
     *
     * @param  geometry     the geometry to annotate.
     * @param  elementName  local name of the GML element the geometry was read from.
     * @param  srsName      the verbatim {@code srsName} attribute value, or {@code null} if absent.
     * @return the given geometry, for chaining.
     */
    protected static <G extends Geometry> G annotate(final G geometry, final String elementName, final String srsName) {
        final Map<String,Object> properties = geometry.userProperties();
        if (properties != null) {
            properties.put(GMLCRS.ELEMENT_KEY, elementName);
            if (srsName != null) {
                properties.put(GMLCRS.SRS_NAME_KEY, srsName);
            }
        }
        return geometry;
    }

    /**
     * Verifies that all the members of an aggregate share one coordinate reference system, and
     * returns the CRS that the aggregate should report when it turns out to be empty.
     *
     * <p>An Apache SIS aggregate reports the CRS of its first element, so a document that declares
     * {@code srsName="EPSG:4326"} on one member and {@code srsName="EPSG:3857"} on the next would
     * otherwise yield a geometry silently claiming to be entirely in the first CRS. With no member
     * at all there is nothing to report, hence the fallback returned here.</p>
     *
     * @param  crs      the CRS resolved for the aggregate element itself, or {@code null} if none.
     * @param  members  the members parsed so far.
     * @return the CRS to use when {@code members} is empty, never null.
     * @throws DataStoreContentException if two members declare non-equivalent CRS.
     */
    protected final CoordinateReferenceSystem aggregateCRS(final CoordinateReferenceSystem crs,
            final List<? extends Geometry> members) throws DataStoreContentException
    {
        if (!members.isEmpty()) {
            final CoordinateReferenceSystem first = members.get(0).getCoordinateReferenceSystem();
            for (int i = 1, n = members.size(); i < n; i++) {
                final CoordinateReferenceSystem other = members.get(i).getCoordinateReferenceSystem();
                if (!CRS.equivalent(first, other)) {
                    throw new DataStoreContentException("All members of a " + getVersion().name()
                            + " geometry collection must share the same coordinate reference system,"
                            + " but member 0 uses \"" + first.getName().getCode() + "\" and member "
                            + i + " uses \"" + other.getName().getCode() + "\".");
                }
            }
        }
        return (crs != null) ? crs : Geometries.getUndefinedCRS(2);
    }

    /**
     * Reads a coordinate sequence, accepting every coordinate encoding that
     * {@link #parseCoordinateElement parseCoordinateElement(…)} recognises, until the enclosing
     * element's end tag is reached. The cursor must be on the enclosing element's
     * {@link #START_ELEMENT} event. After this method returns, the cursor is on the enclosing
     * element's {@link #END_ELEMENT} event.
     *
     * @param  enclosingTagName  local name of the element whose end tag stops the reading.
     * @param  inScope           the CRS resolved for that element, or {@code null} if none.
     */
    protected final PositionListBuilder parseCoordinateSequence(final String enclosingTagName,
            final CoordinateReferenceSystem inScope)
            throws XMLStreamException, DataStoreContentException
    {
        final PositionListBuilder coordinates = new PositionListBuilder();
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (!parseCoordinateElement(coordinates, inScope)) {
                        skipUntilEnd();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (enclosingTagName.equals(reader.getLocalName())) {
                        return coordinates;
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a {@code <gml:coord>} element, appending its single coordinate tuple to the given
     * builder. The cursor must be on the element's {@link #START_ELEMENT} event.
     */
    protected final void parseCoord(final PositionListBuilder target)
            throws XMLStreamException, DataStoreContentException
    {
        Double x = null, y = null, z = null;
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case GML2Tags.X: x = Double.valueOf(reader.getElementText().trim()); break;
                        case GML2Tags.Y: y = Double.valueOf(reader.getElementText().trim()); break;
                        case GML2Tags.Z: z = Double.valueOf(reader.getElementText().trim()); break;
                        default: skipUntilEnd(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (GML2Tags.COORD.equals(reader.getLocalName())) {
                        if (x == null || y == null) {
                            throw new DataStoreContentException("A " + getVersion().name()
                                    + " coord element must contain X and Y children.");
                        }
                        if (z != null) {
                            target.add(x, y, z);
                        } else {
                            target.add(x, y);
                        }
                        return;
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses the text content of a {@code <gml:coordinates>} element, honoring the
     * {@code decimal}, {@code cs} and {@code ts} attributes, and appends the resulting
     * coordinate tuples to the given builder. The cursor must be on the element's
     * {@link #START_ELEMENT} event.
     */
    protected final void parseCoordinatesText(final PositionListBuilder target)
            throws XMLStreamException, DataStoreContentException
    {
        final char decimal = firstCharOrDefault(reader.getAttributeValue(null, GML2Tags.DECIMAL), GML2Tags.DEFAULT_DECIMAL);
        final char cs      = firstCharOrDefault(reader.getAttributeValue(null, GML2Tags.CS),      GML2Tags.DEFAULT_CS);
        final char ts      = firstCharOrDefault(reader.getAttributeValue(null, GML2Tags.TS),      GML2Tags.DEFAULT_TS);
        final String text = reader.getElementText();
        for (final String tuple : split(text, ts)) {
            if (tuple.isEmpty()) continue;
            final List<String> ordinates = split(tuple, cs);
            if (ordinates.size() < 2) {
                throw new DataStoreContentException("Invalid " + getVersion().name()
                        + " coordinate tuple: \"" + tuple + "\".");
            }
            final double[] values = new double[ordinates.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = parseOrdinate(ordinates.get(i), decimal);
            }
            target.add(values);
        }
    }

    /**
     * Parses a collection member wrapper element (e.g. {@code <gml:pointMember>}), which must
     * contain exactly one geometry child of the given type. The cursor must be on the wrapper
     * element's {@link #START_ELEMENT} event. After this method returns, the cursor is on the
     * matching {@link #END_ELEMENT} event of the wrapper.
     */
    protected final <G extends Geometry> G parseMember(final Class<G> type, final String wrapperTagName,
            final CoordinateReferenceSystem inherited)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        Geometry geometry = null;
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    geometry = parseGeometryElement(inherited);
                    break;
                }
                case END_ELEMENT: {
                    if (wrapperTagName.equals(reader.getLocalName())) {
                        if (!type.isInstance(geometry)) {
                            throw new DataStoreContentException("Expected a " + type.getSimpleName()
                                    + " inside <" + wrapperTagName + ">.");
                        }
                        return type.cast(geometry);
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Parses a plural collection member wrapper element (e.g. {@code <gml:pointMembers>}), which
     * may contain any number of geometry children of the given type, and appends them to the given
     * list. The cursor must be on the wrapper element's {@link #START_ELEMENT} event. After this
     * method returns, the cursor is on the matching {@link #END_ELEMENT} event of the wrapper.
     */
    protected final <G extends Geometry> void parseMemberList(final Class<G> type, final String wrapperTagName,
            final CoordinateReferenceSystem inherited, final List<G> addTo)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final Geometry geometry = parseGeometryElement(inherited);
                    if (!type.isInstance(geometry)) {
                        throw new DataStoreContentException("Expected a " + type.getSimpleName()
                                + " inside <" + wrapperTagName + ">.");
                    }
                    addTo.add(type.cast(geometry));
                    break;
                }
                case END_ELEMENT: {
                    if (wrapperTagName.equals(reader.getLocalName())) {
                        return;
                    }
                    break;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Skips the element that was just started (the current event must be {@link #START_ELEMENT}),
     * until the matching {@link #END_ELEMENT}, accounting for nested elements of the same name.
     */
    protected final void skipUntilEnd() throws XMLStreamException {
        int depth = 1;
        while (depth > 0 && reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: depth++; break;
                case END_ELEMENT:   depth--; break;
                case END_DOCUMENT:  return;
            }
        }
    }

    /**
     * Returns the first character of the given text, or the given default if the text is null or empty.
     */
    protected static char firstCharOrDefault(final String text, final char byDefault) {
        return (text != null && !text.isEmpty()) ? text.charAt(0) : byDefault;
    }

    /**
     * Parses a single ordinate value, replacing the given decimal separator by {@code '.'} if needed.
     */
    protected static double parseOrdinate(final String text, final char decimal) {
        return Double.parseDouble((decimal == '.') ? text.trim() : text.trim().replace(decimal, '.'));
    }

    /**
     * Splits the given text on the given separator character, trimming each part.
     * Unlike {@link String#split(String)}, this method treats the separator as a
     * literal character (as mandated by the {@code cs}/{@code ts} attribute semantics).
     */
    protected static List<String> split(final String text, final char separator) {
        final List<String> parts = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == separator) {
                parts.add(text.substring(start, i).trim());
                start = i + 1;
            }
        }
        parts.add(text.substring(start).trim());
        return parts;
    }

    /**
     * Closes the underlying StAX cursor.
     *
     * @throws XMLStreamException if an error occurred while closing the cursor.
     */
    @Override
    public final void close() throws XMLStreamException {
        reader.close();
    }
}
