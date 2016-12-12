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

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import com.esri.core.geometry.Point;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.xml.StaxStreamReader;
import org.apache.sis.util.Version;

import static javax.xml.stream.XMLStreamReader.*;

// Branch-dependent imports
import org.opengis.feature.Feature;


/**
 * Stax reader class for GPX 1.0 and 1.1 files.
 *
 * Usage :<br>
 * <pre>
 * {@code
 * final GPXReader reader = new GPXReader();
 * reader.setInput(gpxInput);
 *
 * final GPXVersion version = reader.getVersion();
 * final Metadata metadata = reader.getMetadata();
 *
 * while(reader.hasNext()) {
 *     Feature feature = reader.next();
 * }
 *
 * }
 * </pre>
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class GPXReader extends StaxStreamReader {

    private final Types types;
    private Metadata metadata;
    private Feature current;
    private int wayPointInc = 0;
    private int routeInc = 0;
    private int trackInc = 0;
    private Version version;
    private boolean isRevision;
    private String baseNamespace = Tags.NAMESPACE_V11;

    /**
     * {@inheritDoc }
     *
     * @param input input object
     * @throws IOException if input failed to be opened for any IO reason
     * @throws XMLStreamException if input is not a valid XML stream
     */
    public GPXReader(final Object input, final StorageConnector storage) throws DataStoreException, IOException, XMLStreamException {
        super(input, storage);
        types = Types.DEFAULT;
        final XMLStreamReader reader = getReader();

        // Search for the bound tag to generate the envelope
searchLoop:
        while(reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String typeName = reader.getLocalName();
                    if (Tags.GPX.equalsIgnoreCase(typeName)) {

                        String str = "1.1";     // Consider 1.1 by default
                        for (int i=0,n=reader.getAttributeCount(); i<n;i++) {
                            if (Constants.ATT_GPX_VERSION.equalsIgnoreCase(reader.getAttributeLocalName(i))) {
                                str = reader.getAttributeValue(i);
                            }
                        }

                        try {
                            this.version = new Version(str);
                        } catch (NumberFormatException ex) {
                            throw new XMLStreamException(ex);
                        }
                        isRevision = GPXStore.V1_1.equals(version);
                        if (isRevision) {
                            baseNamespace = Tags.NAMESPACE_V11;
                        } else if (GPXStore.V1_0.equals(version)) {
                            baseNamespace = Tags.NAMESPACE_V10;
                            //we wont found a metadata tag, must read the tags here.
                            metadata = parseMetadata100();
                            break searchLoop;
                        } else {
                            throw new DataStoreException("Unsupported version: " + version);
                        }

                    } else if (Tags.METADATA.equalsIgnoreCase(typeName)) {
                        metadata = parseMetadata110();
                        break searchLoop;
                    } else if (  Tags.WAY_POINT.equalsIgnoreCase(typeName)
                            || Tags.TRACKS.equalsIgnoreCase(typeName)
                            || Tags.ROUTES.equalsIgnoreCase(typeName)) {
                        //there is no metadata tag
                        break searchLoop;
                    }
                }
            }
        }
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
        wayPointInc = 0;
        routeInc = 0;
        trackInc = 0;
    }

    /**
     * Returns true if there is a next feature in the stream.
     *
     * @return true if there is next feature
     * @throws XMLStreamException if xml parser encounter an invalid element
     *         or underlying stream caused an exception
     */
    public boolean hasNext() throws IOException, XMLStreamException {
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
    public Feature next() throws IOException, XMLStreamException {
        findNext();
        final Feature ele = current;
        current = null;
        return ele;
    }

    /**
     * Search for the next feature in the stax stream.
     * This method will set the current local property if there is one.
     */
    private void findNext() throws IOException, XMLStreamException {
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
                    current = parseWayPoint(wayPointInc++);
                    break;
                } else if (Tags.ROUTES.equalsIgnoreCase(localName)) {
                    current = parseRoute(routeInc++);
                    break;
                } else if (Tags.TRACKS.equalsIgnoreCase(localName)) {
                    current = parseTrack(trackInc++);
                    break;
                }
            }
        }
    }

    /**
     * Parse current metadata element.
     * The stax reader must be placed to the start element of the metadata.
     */
    private Metadata parseMetadata100() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Metadata metadata = new Metadata();

searchLoop:
        while (reader.hasNext()) {
            final int type = reader.next();

            switch (type) {
                case START_ELEMENT: {
                    final String localName = reader.getLocalName();
                    if (Tags.NAME.equalsIgnoreCase(localName)) {
                        metadata.name = reader.getElementText();
                    } else if (Tags.DESCRIPTION.equalsIgnoreCase(localName)) {
                        metadata.description = reader.getElementText();
                    } else if (Tags.AUTHOR.equalsIgnoreCase(localName)) {
                        if (metadata.author == null) metadata.author = new Person();
                        metadata.author.name = reader.getElementText();
                    } else if (Tags.EMAIL.equalsIgnoreCase(localName)) {
                        if (metadata.author == null) metadata.author = new Person();
                        metadata.author.email = reader.getElementText();
                    } else if (Tags.URL.equalsIgnoreCase(localName)) {
                        try {
                            metadata.links.add(new Link(reader.getElementText()));
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    } else if (Tags.URL_NAME.equalsIgnoreCase(localName)) {
                        //reader.getElementText();
                    } else if (Tags.TIME.equalsIgnoreCase(localName)) {
                        metadata.time = parseTime(reader.getElementText());
                    } else if (Tags.KEYWORDS.equalsIgnoreCase(localName)) {
                        metadata.keywords = reader.getElementText();
                    } else if (Tags.BOUNDS.equalsIgnoreCase(localName)) {
                        metadata.bounds = parseBound();
                    } else if (  Tags.WAY_POINT.equalsIgnoreCase(localName)
                            || Tags.TRACKS.equalsIgnoreCase(localName)
                            || Tags.ROUTES.equalsIgnoreCase(localName)) {
                        //there is no more metadata tags
                        break searchLoop;
                    }
                    break;
                }
            }
        }
        return metadata;
    }

    /**
     * Parse current metadata element.
     * The stax reader must be placed to the start element of the metadata.
     */
    private Metadata parseMetadata110() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Metadata metadata = new Metadata();

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String localName = reader.getLocalName();
                    if (Tags.NAME.equalsIgnoreCase(localName)) {
                        metadata.name = reader.getElementText();
                    } else if (Tags.DESCRIPTION.equalsIgnoreCase(localName)) {
                        metadata.description = reader.getElementText();
                    } else if (Tags.AUTHOR.equalsIgnoreCase(localName)) {
                        metadata.author = parsePerson();
                    } else if (Tags.COPYRIGHT.equalsIgnoreCase(localName)) {
                        metadata.copyright = parseCopyright();
                    } else if (Tags.LINK.equalsIgnoreCase(localName)) {
                        metadata.links.add(parseLink());
                    } else if (Tags.TIME.equalsIgnoreCase(localName)) {
                        metadata.time = parseTime(reader.getElementText());
                    } else if (Tags.KEYWORDS.equalsIgnoreCase(localName)) {
                        metadata.keywords = reader.getElementText();
                    } else if (Tags.BOUNDS.equalsIgnoreCase(localName)) {
                        metadata.bounds = parseBound();
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (Tags.METADATA.equalsIgnoreCase(reader.getLocalName())) {
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
    private Copyright parseCopyright() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Copyright copyright = new Copyright();
        copyright.author = reader.getAttributeValue(null, Constants.ATT_COPYRIGHT_AUTHOR);

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String localName = reader.getLocalName();
                    if (Tags.YEAR.equalsIgnoreCase(localName)) {
                        copyright.year = Integer.valueOf(reader.getElementText());
                    } else if (Tags.LICENSE.equalsIgnoreCase(localName)) {
                        try {
                            copyright.license = new Link(reader.getElementText());
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
    private Link parseLink() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        String text = reader.getAttributeValue(null, Constants.ATT_LINK_HREF);
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
                            return new Link(text);
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
    private Person parsePerson() throws IOException, XMLStreamException {
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
    private GeographicBoundingBox parseBound() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final String xmin = reader.getAttributeValue(null, Constants.ATT_BOUNDS_MINLON);
        final String xmax = reader.getAttributeValue(null, Constants.ATT_BOUNDS_MAXLON);
        final String ymin = reader.getAttributeValue(null, Constants.ATT_BOUNDS_MINLAT);
        final String ymax = reader.getAttributeValue(null, Constants.ATT_BOUNDS_MAXLAT);

        if (xmin == null || xmax == null || ymin == null || ymax == null) {
            throw new XMLStreamException("Error in xml file, metadata bounds not defined correctly");
        }

        skipUntilEnd(Tags.BOUNDS);

        return new DefaultGeographicBoundingBox(
                Double.parseDouble(xmin),
                Double.parseDouble(xmax),
                Double.parseDouble(ymin),
                Double.parseDouble(ymax));
    }

    /**
     * Parse way point type feature element.
     * The stax reader must be placed to the start element.
     */
    private Feature parseWayPoint(final int index) throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Feature feature = types.wayPoint.newInstance();
        feature.setPropertyValue("@identifier", index);

        //way points might be located in different tag names : wpt, rtept and trkpt
        //we kind the current tag name to know when we reach the end.
        final String tagName = reader.getLocalName();

        List<Link> links = null;

        final String lat = reader.getAttributeValue(null, Constants.ATT_WPT_LAT);
        final String lon = reader.getAttributeValue(null, Constants.ATT_WPT_LON);

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
                    } else if (!isRevision && Tags.URL.equalsIgnoreCase(localName)) {
                        // GPX 1.0 only
                        if (links == null) links = new ArrayList<>();
                        try {
                            links.add(new Link(reader.getElementText()));
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
    private Feature parseRoute(final int index) throws IOException, XMLStreamException {
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
                        wayPoints.add(parseWayPoint(ptInc++));
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
                    } else if (!isRevision && Tags.URL.equalsIgnoreCase(localName)) {
                        //GPX 1.0 only
                        if (links == null) links = new ArrayList<>();
                        try {
                            links.add(new Link(reader.getElementText()));
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
    private Feature parseTrackSegment(final int index) throws IOException, XMLStreamException {
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
                        wayPoints.add(parseWayPoint(ptInc++));
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
    private Feature parseTrack(final int index) throws IOException, XMLStreamException {
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
                    } else if (!isRevision && Tags.URL.equalsIgnoreCase(localName)) {
                        // GPX 1.0 only
                        if (links == null) links = new ArrayList<>();
                        try {
                            links.add(new Link(reader.getElementText()));
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
     * The method support only ISO 8601 Date and DateTime formats.
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
