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
package org.apache.sis.internal.gpx;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;
import java.io.EOFException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.UnsupportedTemporalTypeException;
import javax.xml.transform.stax.StAXSource;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.bind.JAXBException;
import com.esri.core.geometry.Point;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.internal.xml.StaxStreamReader;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Version;
import org.apache.sis.xml.XML;

// Branch-dependent imports
import org.opengis.feature.Feature;


/**
 * Reader for GPX 1.0 and 1.1 files.
 * This reader is itself an iterator over all features found in the XML file.
 * Usage:
 *
 * {@preformat java
 *     final Reader   reader   = new Reader(dataStore, connector);
 *     final Version  version  = reader.getVersion();
 *     final Metadata metadata = reader.getMetadata();
 *     while (reader.hasNext()) {
 *         Feature feature = reader.next();
 *     }
 * }
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class GPXReader extends StaxStreamReader {
    /**
     * The {@link org.opengis.feature.FeatureType} for routes, tracks, way points, <i>etc</i>.
     */
    private final Types types;

    /**
     * Version of the GPX file, or {@code null} if unspecified.
     * Can be {@link GPXStore#V1_0} or {@link GPXStore#V1_1}.
     */
    private Version version;

    /**
     * Convenience flag set to {@code true} if the {@link #version} field is {@link GPXStore#V1_0},
     * or {@code false} if the version is {@link GPXStore#V1_1}.
     */
    private boolean isLegacy;

    /**
     * The metadata (ISO 19115 compatible), or {@code null} if none.
     */
    private Metadata metadata;

    /**
     * The feature to be returned by {@link #next()}.
     * This field is updated during iteration.
     */
    private Feature current;

    /**
     * Identifier of the last "way point" feature instance created.
     * We use sequential numbers starting from 1.
     */
    private int wayPointId;

    /**
     * Identifier of the last "route" feature instance created.
     * We use sequential numbers starting from 1.
     */
    private int routeId;

    /**
     * Identifier of the last "track" feature instance created.
     * We use sequential numbers starting from 1.
     */
    private int trackId;

    /**
     * Creates a new GPX reader from the given file, URL, stream or reader object.
     *
     * @param  owner      the data store for which this reader is created.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if the input type is not recognized.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     * @throws JAXBException if an error occurred while parsing GPX 1.1 metadata.
     * @throws EOFException if the file seems to be truncated.
     */
    public GPXReader(final GPXStore owner, final StorageConnector connector)
            throws DataStoreException, XMLStreamException, JAXBException, EOFException
    {
        super(owner, connector);
        types = Types.DEFAULT;
        /*
         * Skip comments, characters, entity declarations, etc. until we find the root element.
         * If that root is anything other than <gpx>, we consider that this is not a GPX file.
         */
        moveToRootElement(GPXReader::isNamespace, Tags.GPX);
        /*
         * If a version attribute is found on the <gpx> element, use that value for detecting
         * the GPX version. Otherwise use the namespace URL.  If the version is not found, we
         * leave the field to null (we do not assume any version). If a version is specified,
         * we require major.minor version 1.0 or 1.1 but accept any bug-fix versions.
         */
        final XMLStreamReader reader = getReader();
        String ver = reader.getAttributeValue(null, Attributes.VERSION);
        if (ver != null) {
            version = new Version(ver);
        } else {
            final String ns = reader.getNamespaceURI();
            if (ns != null) switch (ns) {
                case Tags.NAMESPACE_V10: version = GPXStore.V1_0; break;
                case Tags.NAMESPACE_V11: version = GPXStore.V1_1; break;
            }
        }
        if (version != null) {
            isLegacy = version.compareTo(GPXStore.V1_0, 2) <= 0;
            if (version.compareTo(GPXStore.V1_1, 2) > 0) {
                throw new DataStoreContentException(errors().getString(
                        Errors.Keys.UnsupportedFormatVersion_2, owner.getFormatName(), version));
            }
        }
        /*
         * Read metadata immediately, from current position until the beginning of way points, tracks or routes.
         * The metadata can appear in two forms:
         *
         *   - In GPX 1.0, they are declared directly in the <gpx> body.
         *     Those elements are parsed in the switch statement below.
         *
         *   - In GPX 1.1, they are declared in a <metadata> sub-element and their structure is a little bit
         *     more elaborated than it was in the previous version. We will use JAXB for parsing them.
         */
        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (isNamespace(reader.getNamespaceURI())) {
                        switch (reader.getLocalName()) {
                            case Tags.GPX: {
                                throw new DataStoreContentException(errors().getString(
                                        Errors.Keys.NestedElementNotAllowed_1, Tags.GPX));
                            }
                            case Tags.METADATA: {
                                metadata = (Metadata) XML.unmarshal(new StAXSource(reader), null);
                                return;
                            }
                            case Tags.NAME: {
                                metadata().name = reader.getElementText();
                                break;
                            }
                            case Tags.DESCRIPTION: {
                                metadata().description = reader.getElementText();
                                break;
                            }
                            case Tags.AUTHOR: {
                                if (metadata().author == null) metadata.author = new Person();
                                metadata.author.name = reader.getElementText();
                                break;
                            }
                            case Tags.EMAIL: {
                                if (metadata().author == null) metadata.author = new Person();
                                metadata.author.email = reader.getElementText();
                                break;
                            }
                            case Tags.URL: {
                                try {
                                    metadata().links.add(new Link(new URI(reader.getElementText())));
                                } catch (URISyntaxException ex) {
                                    throw new XMLStreamException(ex);
                                }
                                break;
                            }
                            case Tags.URL_NAME: {
                                //reader.getElementText();
                                break;
                            }
                            case Tags.TIME:     metadata().time     = parseTime(reader.getElementText()); break;
                            case Tags.KEYWORDS: metadata().keywords = Arrays.asList(reader.getElementText().split(" ")); break;
                            case Tags.BOUNDS:   metadata().bounds   = parseBound(); break;
                            case Tags.WAY_POINT:
                            case Tags.TRACKS:
                            case Tags.ROUTES: return;
                        }
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (isNamespace(reader.getNamespaceURI()) && Tags.GPX.equals(reader.getLocalName())) {
                        // TODO
                    }
                    break;
                }
            }
        }
    }

    /**
     * Returns {@code true} if the given namespace is a GPX namespace or is null.
     */
    private static boolean isNamespace(final String ns) {
        return (ns == null) || ns.startsWith(Tags.NAMESPACE + "/GPX/");
    }

    private Metadata metadata() {
        if (metadata == null) {
            metadata = new Metadata();
        }
        return metadata;
    }

    /**
     * Get GPX file version.
     * This method will return a result only if called only after the input has been set.
     *
     * @return Version or null if input is not set.
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Get GPX metadata.
     * This method will return a result only if called only after the input has been set.
     *
     * @return Metadata or null if input is not set.
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void close() throws IOException, XMLStreamException {
        super.close();
        metadata = null;
        current = null;
        wayPointId = 0;
        routeId = 0;
        trackId = 0;
    }

    /**
     * Returns true if there is a next feature in the stream.
     *
     * @return true if there is next feature
     * @throws XMLStreamException if xml parser encounter an invalid element
     *         or underlying stream caused an exception
     */
    public boolean hasNext() throws XMLStreamException {
        findNext();
        return current != null;
    }

    /**
     * Get next feature in the stream.
     *
     * @return GPX WayPoint, Route or track
     * @throws XMLStreamException if xml parser encounter an invalid element
     *         or underlying stream caused an exception
     */
    public Feature next() throws XMLStreamException {
        findNext();
        final Feature ele = current;
        current = null;
        return ele;
    }

    /**
     * Search for the next feature in the stax stream.
     * This method will set the current local property if there is one.
     */
    private void findNext() throws XMLStreamException {
        final XMLStreamReader reader = getReader();
        if (current != null) return;

        boolean first = true;
        while ( first || (current == null && reader.hasNext()) ) {
            final int type;
            if (first) {
                type = reader.getEventType();
                first = false;
            } else {
                type = reader.next();
            }
            if (type == START_ELEMENT) {
                final String localName = reader.getLocalName();
                if (Tags.WAY_POINT.equalsIgnoreCase(localName)) {
                    current = parseWayPoint(++wayPointId);
                    break;
                } else if (Tags.ROUTES.equalsIgnoreCase(localName)) {
                    current = parseRoute(++routeId);
                    break;
                } else if (Tags.TRACKS.equalsIgnoreCase(localName)) {
                    current = parseTrack(++trackId);
                    break;
                }
            }
        }
    }

    /**
     * Parse current metadata element.
     * The stax reader must be placed to the start element of the metadata.
     */
    private Metadata parseMetadata110() throws XMLStreamException, EOFException {
        final XMLStreamReader reader = getReader();
        final Metadata metadata = new Metadata();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case Tags.NAME: metadata.name = reader.getElementText(); break;
                        case Tags.DESCRIPTION: metadata.description = reader.getElementText(); break;
                        case Tags.AUTHOR: metadata.author = parsePerson(); break;
                        case Tags.COPYRIGHT: metadata.copyright = parseCopyright(); break;
                        case Tags.LINK: metadata.links.add(parseLink()); break;
                        case Tags.TIME: metadata.time = parseTime(reader.getElementText()); break;
                        case Tags.KEYWORDS: metadata.keywords = Arrays.asList(reader.getElementText().split(" ")); break;
                        case Tags.BOUNDS: metadata.bounds = parseBound(); break;
                    }
                    break;
                }
                case END_ELEMENT: {
                    switch (reader.getLocalName()) {
                        case Tags.METADATA:
                            // End of the metadata element
                            return metadata;
                    }
                    break;
                }
            }
        }
        throw new XMLStreamException("Error in xml file, relation tag without end.");
    }

    /**
     * Parse current copyright element.
     * The stax reader must be placed to the start element of the copyright.
     */
    private Copyright parseCopyright() throws XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Copyright copyright = new Copyright();
        copyright.author = reader.getAttributeValue(null, Attributes.AUTHOR);

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String localName = reader.getLocalName();
                    if (Tags.YEAR.equalsIgnoreCase(localName)) {
                        copyright.year = Integer.valueOf(reader.getElementText());
                    } else if (Tags.LICENSE.equalsIgnoreCase(localName)) {
                        try {
                            copyright.license = new URI(reader.getElementText());
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (Tags.COPYRIGHT.equalsIgnoreCase(reader.getLocalName())) {
                        return copyright;
                    }
                    break;
                }
            }
        }
        throw new XMLStreamException("Error in xml file, copyright tag without end.");
    }

    /**
     * Parse current URI element.
     * The stax reader must be placed to the start element.
     */
    private Link parseLink() throws XMLStreamException {
        final XMLStreamReader reader = getReader();
        String text = reader.getAttributeValue(null, Attributes.HREF);
        String mime = null;

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String localName = reader.getLocalName();
                    if (Tags.TEXT.equalsIgnoreCase(localName) && text==null) {
                        text = reader.getElementText();
                    } else if (Tags.TYPE.equalsIgnoreCase(localName)) {
                        mime = reader.getElementText();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (Tags.LINK.equalsIgnoreCase(reader.getLocalName())) {
                        try {
                            // End of the link element
                            return new Link(new URI(text));
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    }
                    break;
                }
            }
        }
        throw new XMLStreamException("Error in xml file, link tag without end.");
    }

    /**
     * Parse current Person element.
     * The stax reader must be placed to the start element.
     */
    private Person parsePerson() throws XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Person person = new Person();

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String localName = reader.getLocalName();
                    if (Tags.NAME.equalsIgnoreCase(localName)) {
                        person.name = reader.getElementText();
                    } else if (Tags.EMAIL.equalsIgnoreCase(localName)) {
                        person.email = reader.getElementText();
                    } else if (Tags.LINK.equalsIgnoreCase(localName)) {
                        person.link = parseLink();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (Tags.AUTHOR.equalsIgnoreCase(reader.getLocalName())) {
                        // End of the author element
                        return person;
                    }
                    break;
                }
            }
        }
        throw new XMLStreamException("Error in xml file, person tag without end.");
    }

    /**
     * Parse current Envelope element.
     * The stax reader must be placed to the start element.
     */
    private Bounds parseBound() throws XMLStreamException, EOFException {
        final XMLStreamReader reader = getReader();
        final String xmin = reader.getAttributeValue(null, Attributes.MIN_X);
        final String xmax = reader.getAttributeValue(null, Attributes.MAX_X);
        final String ymin = reader.getAttributeValue(null, Attributes.MIN_Y);
        final String ymax = reader.getAttributeValue(null, Attributes.MAX_Y);

        if (xmin == null || xmax == null || ymin == null || ymax == null) {
            throw new XMLStreamException("Error in xml file, metadata bounds not defined correctly");
        }

        skipUntilEnd(Tags.BOUNDS);
        final Bounds bounds = new Bounds();
        bounds.westBoundLongitude = Double.parseDouble(xmin);
        bounds.eastBoundLongitude = Double.parseDouble(xmax);
        bounds.southBoundLatitude = Double.parseDouble(ymin);
        bounds.northBoundLatitude = Double.parseDouble(ymax);
        return bounds;
    }

    /**
     * Parse way point type feature element.
     * The stax reader must be placed to the start element.
     */
    private Feature parseWayPoint(final int index) throws XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Feature feature = types.wayPoint.newInstance();
        feature.setPropertyValue("@identifier", index);

        //way points might be located in different tag names : wpt, rtept and trkpt
        //we kind the current tag name to know when we reach the end.
        final String tagName = reader.getLocalName();

        List<Link> links = null;

        final String lat = reader.getAttributeValue(null, Attributes.LATITUDE);
        final String lon = reader.getAttributeValue(null, Attributes.LONGITUDE);

        if (lat == null || lon == null) {
            throw new XMLStreamException("Error in xml file, way point lat/lon not defined correctly");
        } else{
            feature.setPropertyValue("@geometry", new Point(Double.parseDouble(lon), Double.parseDouble(lat)));
        }

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String localName = reader.getLocalName();
                    if (Tags.ELEVATION.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.ELEVATION, Double.valueOf(reader.getElementText()));
                    } else if (Tags.TIME.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.TIME, parseTime(reader.getElementText()));
                    } else if (Tags.MAGNETIC_VAR.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.MAGNETIC_VAR, Double.valueOf(reader.getElementText()));
                    } else if (Tags.GEOID_HEIGHT.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.GEOID_HEIGHT, Double.valueOf(reader.getElementText()));
                    } else if (Tags.NAME.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.NAME, reader.getElementText());
                    } else if (Tags.COMMENT.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.COMMENT,reader.getElementText());
                    } else if (Tags.DESCRIPTION.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.DESCRIPTION, reader.getElementText());
                    } else if (Tags.SOURCE.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.SOURCE, reader.getElementText());
                    } else if (Tags.LINK.equalsIgnoreCase(localName)) {
                        if (links == null) links = new ArrayList<>();
                        links.add(parseLink());
                    } else if (Tags.SYMBOL.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.SYMBOL, reader.getElementText());
                    } else if (Tags.TYPE.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.TYPE, reader.getElementText());
                    } else if (Tags.FIX.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.FIX, reader.getElementText());
                    } else if (Tags.SATELITTES.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.SATELITTES, Integer.valueOf(reader.getElementText()));
                    } else if (Tags.HDOP.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.HDOP, Double.valueOf(reader.getElementText()));
                    } else if (Tags.PDOP.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.PDOP, Double.valueOf(reader.getElementText()));
                    } else if (Tags.VDOP.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.VDOP, Double.valueOf(reader.getElementText()));
                    } else if (Tags.AGE_OF_GPS_DATA.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.AGE_OF_GPS_DATA, Double.valueOf(reader.getElementText()));
                    } else if (Tags.DGPS_ID.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.DGPS_ID, Integer.valueOf(reader.getElementText()));
                    } else if (isLegacy && Tags.URL.equalsIgnoreCase(localName)) {
                        // GPX 1.0 only
                        if (links == null) links = new ArrayList<>();
                        try {
                            links.add(new Link(new URI(reader.getElementText())));
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (tagName.equalsIgnoreCase(reader.getLocalName())) {
                        // End of the way point element
                        if (links!=null) feature.setPropertyValue(Tags.LINK, links);
                        return feature;
                    }
                    break;
                }
            }
        }
        throw new XMLStreamException("Error in xml file, "+tagName+" tag without end.");
    }

    /**
     * Parse route type feature element.
     * The stax reader must be placed to the start element.
     */
    private Feature parseRoute(final int index) throws XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Feature feature = types.route.newInstance();
        feature.setPropertyValue("@identifier", index);

        int ptInc = 0;
        List<Link> links = null;
        List<Feature> wayPoints = null;

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String localName = reader.getLocalName();
                    if (Tags.ROUTE_POINTS.equalsIgnoreCase(localName)) {
                        if (wayPoints == null) wayPoints = new ArrayList<>();
                        wayPoints.add(parseWayPoint(++ptInc));
                    } else if (Tags.NAME.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.NAME, reader.getElementText());
                    } else if (Tags.COMMENT.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.COMMENT, reader.getElementText());
                    } else if (Tags.DESCRIPTION.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.DESCRIPTION, reader.getElementText());
                    } else if (Tags.SOURCE.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.SOURCE, reader.getElementText());
                    } else if (Tags.LINK.equalsIgnoreCase(localName)) {
                        if (links == null) links = new ArrayList<>();
                        links.add(parseLink());
                    } else if (Tags.NUMBER.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.NUMBER, Integer.valueOf(reader.getElementText()));
                    } else if (Tags.TYPE.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.TYPE, reader.getElementText());
                    } else if (isLegacy && Tags.URL.equalsIgnoreCase(localName)) {
                        //GPX 1.0 only
                        if (links == null) links = new ArrayList<>();
                        try {
                            links.add(new Link(new URI(reader.getElementText())));
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (Tags.ROUTES.equalsIgnoreCase(reader.getLocalName())) {
                        // End of the route element
                        if (links!=null) feature.setPropertyValue(Tags.LINK, links);
                        if (wayPoints!=null) feature.setPropertyValue(Tags.ROUTE_POINTS, wayPoints);
                        return feature;
                    }
                    break;
                }
            }
        }
        throw new XMLStreamException("Error in xml file, "+Tags.ROUTES+" tag without end.");
    }

    /**
     * Parse track segment type feature element.
     * The stax reader must be placed to the start element.
     */
    private Feature parseTrackSegment(final int index) throws XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Feature feature = types.trackSegment.newInstance();
        feature.setPropertyValue("@identifier", index);
        int ptInc = 0;
        List<Feature> wayPoints = null;

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String localName = reader.getLocalName();
                    if (Tags.TRACK_POINTS.equalsIgnoreCase(localName)) {
                        if (wayPoints == null) wayPoints = new ArrayList<>();
                        wayPoints.add(parseWayPoint(++ptInc));
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (Tags.TRACK_SEGMENTS.equalsIgnoreCase(reader.getLocalName())) {
                        // End of the track segment element
                        if (wayPoints!=null) feature.setPropertyValue(Tags.TRACK_POINTS, wayPoints);
                        return feature;
                    }
                    break;
                }
            }
        }
        throw new XMLStreamException("Error in xml file, "+Tags.TRACK_SEGMENTS+" tag without end.");
    }

    /**
     * Parse track type feature element.
     * The stax reader must be placed to the start element.
     */
    private Feature parseTrack(final int index) throws XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Feature feature = types.track.newInstance();
        feature.setPropertyValue("@identifier", index);
        int segInc = 0;
        List<Link> links = null;
        List<Feature> segments = null;

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String localName = reader.getLocalName();
                    if (Tags.TRACK_SEGMENTS.equalsIgnoreCase(localName)) {
                        if (segments == null) segments = new ArrayList<>();
                        segments.add(parseTrackSegment(segInc++));
                    } else if (Tags.NAME.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.NAME, reader.getElementText());
                    } else if (Tags.COMMENT.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.COMMENT, reader.getElementText());
                    } else if (Tags.DESCRIPTION.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.DESCRIPTION, reader.getElementText());
                    } else if (Tags.SOURCE.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.SOURCE, reader.getElementText());
                    } else if (Tags.LINK.equalsIgnoreCase(localName)) {
                        if (links == null) links = new ArrayList<>();
                        links.add(parseLink());
                    } else if (Tags.NUMBER.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.NUMBER, Integer.valueOf(reader.getElementText()));
                    } else if (Tags.TYPE.equalsIgnoreCase(localName)) {
                        feature.setPropertyValue(Tags.TYPE, reader.getElementText());
                    } else if (isLegacy && Tags.URL.equalsIgnoreCase(localName)) {
                        // GPX 1.0 only
                        if (links == null) links = new ArrayList<>();
                        try {
                            links.add(new Link(new URI(reader.getElementText())));
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (Tags.TRACKS.equalsIgnoreCase(reader.getLocalName())) {
                        // End of the track element
                        if (links!=null) feature.setPropertyValue(Tags.LINK, links);
                        if (segments!=null) feature.setPropertyValue(Tags.TRACK_SEGMENTS, segments);
                        return feature;
                    }
                    break;
                }
            }
        }
        throw new XMLStreamException("Error in xml file, "+Tags.TRACKS+" tag without end.");
    }

    /**
     * Parse date or date time from string.
     * Dates and times in GPX files are Coordinated Universal Time (UTC) using ISO 8601 format.
     *
     * @param dateStr date in ISO date or data time format
     */
    private static Temporal parseTime(String dateStr) {
        try {
            final DateTimeFormatter format = DateTimeFormatter.ISO_INSTANT;
            final TemporalAccessor accessor = format.parse(dateStr);
            return Instant.from(accessor);
        } catch (UnsupportedTemporalTypeException | DateTimeParseException ex) {
            try {
                final DateTimeFormatter format = DateTimeFormatter.ISO_DATE;
                final TemporalAccessor accessor = format.parse(dateStr);
                return LocalDate.from(accessor);
            } catch (UnsupportedTemporalTypeException | DateTimeParseException e) {
                final DateTimeFormatter format = DateTimeFormatter.ISO_DATE_TIME;
                final TemporalAccessor accessor = format.parse(dateStr);
                return LocalDateTime.from(accessor);
            }
        }
    }
}
