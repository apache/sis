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
import java.util.ArrayList;
import java.util.Objects;
import java.io.IOException;
import java.io.EOFException;
import java.net.URISyntaxException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.bind.JAXBException;
import com.esri.core.geometry.Point;
import org.apache.sis.storage.gps.Fix;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.internal.xml.StaxStreamReader;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Version;

// Branch-dependent imports
import java.util.function.Consumer;
import java.time.format.DateTimeParseException;
import org.opengis.feature.Feature;


/**
 * Reader for GPX 1.0 and 1.1 files.
 * This reader is itself a spliterator over all features found in the XML file.
 * Usage:
 *
 * {@preformat java
 *     Consumer<Feature> consumer = ...;
 *     try (Reader reader = new Reader(dataStore, connector)) {
 *         final Version  version  = reader.initialize(true);
 *         final Metadata metadata = reader.getMetadata();
 *         reader.forEachRemaining(consumer);
 *     }
 * }
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
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
     * The namespace, which should be either {@link Tags#NAMESPACE_V10} or {@link Tags#NAMESPACE_V11}.
     * We store this information for identifying the closing {@code <gpx>} tag.
     */
    private String namespace;

    /**
     * The metadata (ISO 19115 compatible), or {@code null} if none.
     */
    private Metadata metadata;

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
     * The {@link #initialize(boolean)} method must be invoked after this constructor.
     *
     * @param  owner      the data store for which this reader is created.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if the input type is not recognized.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     */
    public GPXReader(final GPXStore owner, final StorageConnector connector)
            throws DataStoreException, XMLStreamException
    {
        super(owner, connector);
        types = Types.DEFAULT;
    }

    /**
     * Returns {@code true} if the given namespace is a GPX namespace or is null.
     */
    private static boolean isGPX(final String ns) {
        return (ns == null) || ns.startsWith(Tags.NAMESPACE + "/GPX/");
    }

    /**
     * Returns {@code true} if the current position of the given reader is the closing {@code </gpx>} tag.
     * The reader event should be {@link #END_DOCUMENT} before to invoke this method.
     */
    private boolean isEndGPX(final XMLStreamReader reader) {
        return Tags.GPX.equals(reader.getLocalName()) && Objects.equals(namespace, reader.getNamespaceURI());
    }

    /**
     * Reads the metadata. This method should be invoked exactly once after construction.
     * This work is performed outside the constructor for allowing {@link #close()} method
     * invocation no matter if this {@code initialize(boolean)} method fails.
     *
     * @param  readMetadata  if {@code false}, skip the reading of metadata elements.
     * @return the GPX file version, or {@code null} if no version information was found.
     * @throws DataStoreException if the root element is not the expected one.
     * @throws XMLStreamException if an error occurred while reading the XML file.
     * @throws JAXBException if an error occurred while parsing GPX 1.1 metadata.
     * @throws ClassCastException if an object unmarshalled by JAXB was not of the expected type.
     * @throws URISyntaxException if an error occurred while parsing URI in GPX 1.0 metadata.
     * @throws DateTimeParseException if a text can not be parsed as a date.
     * @throws EOFException if the file seems to be truncated.
     */
    public Version initialize(final boolean readMetadata) throws DataStoreException,
            XMLStreamException, JAXBException, URISyntaxException, EOFException
    {
        /*
         * Skip comments, characters, entity declarations, etc. until we find the root element.
         * If that root is anything other than <gpx>, we consider that this is not a GPX file.
         */
        moveToRootElement(GPXReader::isGPX, Tags.GPX);
        /*
         * If a version attribute is found on the <gpx> element, use that value for detecting the GPX version.
         * If a version is specified, we require major.minor version 1.0 or 1.1 but accept any bug-fix versions
         * (e.g. 1.1.x). If no version attribute was found, try to infer the version from the namespace URL.
         */
        final XMLStreamReader reader = getReader();
        namespace = reader.getNamespaceURI();
        String ver = reader.getAttributeValue(null, Attributes.VERSION);
        Version version = null;
        if (ver != null) {
            version = new Version(ver);
            final int c = version.compareTo(GPXStore.V1_0, 2);
            if (c < 0 || version.compareTo(GPXStore.V1_1, 2) > 0) {
                throw new DataStoreContentException(errors().getString(
                        Errors.Keys.UnsupportedFormatVersion_2, owner.getFormatName(), version));
            }
        } else if (namespace != null) {
            switch (namespace) {
                case Tags.NAMESPACE_V10: version = GPXStore.V1_0; break;
                case Tags.NAMESPACE_V11: version = GPXStore.V1_1; break;
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
         *     more elaborated than what it was in the previous version. We will use JAXB for parsing them.
         */
parse:  while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    /*
                     * GPX 1.0 and 1.1 metadata should not be mixed. However the following code will work even
                     * if GPX 1.0 metadata like <name> or <author> appear after the GPX 1.1 <metadata> element.
                     */
                    if (isGPX(reader.getNamespaceURI())) {
                        final String name = reader.getLocalName();
                        if (readMetadata) {
                            switch (name) {
                                // GPX 1.1 metadata
                                case Tags.METADATA:     metadata = unmarshal(Metadata.class); break;

                                // GPX 1.0 metadata
                                case Tags.NAME:         metadata().name        = getElementText();        break;
                                case Tags.DESCRIPTION:  metadata().description = getElementText();        break;
                                case Tags.AUTHOR:       author()  .name        = getElementText();        break;
                                case Tags.EMAIL:        author()  .email       = getElementText();        break;
                                case Tags.URL:          link()    .uri         = getElementAsURI();       break;
                                case Tags.URL_NAME:     link()    .text        = getElementText();        break;
                                case Tags.TIME:         metadata().time        = getElementAsDate();      break;
                                case Tags.KEYWORDS:     metadata().keywords    = getElementAsList();      break;
                                case Tags.BOUNDS:       metadata().bounds      = unmarshal(Bounds.class); break;
                                case Tags.WAY_POINT:    // stop metadata parsing.
                                case Tags.TRACKS:
                                case Tags.ROUTES:       break parse;
                                case Tags.GPX:          throw new DataStoreContentException(nestedElement(Tags.GPX));
                            }
                        } else {
                            /*
                             * If the caller asked to skip metadata, just look for the end of metadata elements.
                             */
                            switch (name) {
                                case Tags.METADATA:  skipUntilEnd(reader.getName()); break;
                                case Tags.WAY_POINT:
                                case Tags.TRACKS:
                                case Tags.ROUTES:    break parse;
                                case Tags.GPX:       throw new DataStoreContentException(nestedElement(Tags.GPX));
                            }
                        }
                    }
                    break;
                }
                case END_ELEMENT: {
                    /*
                     * Reminder: END_ELEMENT events are already handled by XMLStreamReader.getElementText(),
                     * Unmarshaller.unmarshal(XMLStreamReader, …) and our parseBound(…) methods. There is only
                     * the enclosing <gpx> tag to check.
                     */
                    if (isEndGPX(reader)) {
                        break parse;
                    }
                    break;
                }
            }
        }
        return version;
    }

    /**
     * Returns the {@link #metadata} field, creating it if needed.
     * This is a convenience method for GPX 1.0 metadata parsing.
     */
    private Metadata metadata() {
        if (metadata == null) {
            metadata = new Metadata();
        }
        return metadata;
    }

    /**
     * Returns the {@link Metadata#author} field, creating all necessary objects if needed.
     * This is a convenience method for GPX 1.0 metadata parsing.
     */
    private Person author() {
        final Metadata metadata = metadata();
        if (metadata.author == null) {
            metadata.author = new Person();
        }
        return metadata.author;
    }

    /**
     * Returns the first element of the {@link Metadata#links} field, creating all necessary objects if needed.
     * This is a convenience method for GPX 1.0 metadata parsing.
     */
    private Link link() {
        final List<Link> links = metadata().links;
        final Link first;
        if (links.isEmpty()) {
            first = new Link();
            links.add(first);
        } else {
            first = links.get(0);
        }
        return first;
    }

    /**
     * Adds the given element to the given list if non null, or do nothing otherwise.
     *
     * @param  links    the list where to add the element, or {@code null} if not yet created.
     * @param  element  the element to add, or {@code null} if none.
     * @return the list where the element has been added.
     */
    private static List<Link> addIfNonNull(List<Link> links, final Link element) {
        if (element != null) {
            if (links == null) {
                links = new ArrayList<>(3);
            }
            links.add(element);
        }
        return links;
    }

    /**
     * Returns the metadata (ISO 19115 compatible), or {@code null} if none.
     * This method can return a non-null value only if {@code initialize(true)}
     * has been invoked before this method.
     *
     * @return the metadata, or {@code null} if none.
     *
     * @see #initialize(boolean)
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Performs the given action on the next feature instance, or returns {@code null} if there is no more
     * feature to parse.
     *
     * @param  action  the action to perform on the next feature instances.
     * @return {@code true} if a feature has been found, or {@code false} if we reached the end of GPX file.
     * @throws BackingStoreException if an error occurred while parsing the next feature instance.
     *         The cause may be {@link DataStoreException}, {@link IOException}, {@link URISyntaxException}
     *         or various {@link RuntimeException}.
     */
    @Override
    public boolean tryAdvance(final Consumer<? super Feature> action) throws BackingStoreException {
        try {
            return parse(action, false);
        } catch (Exception e) {                 // Many possible exceptions including unchecked ones.
            throw new BackingStoreException(canNotReadFile(), e);
        }
    }

    /**
     * Performs the given action for each remaining element until all elements have been processed.
     *
     * @param  action  the action to perform on the remaining feature instances.
     * @throws BackingStoreException if an error occurred while parsing the next feature instance.
     *         The cause may be {@link DataStoreException}, {@link IOException}, {@link URISyntaxException}
     *         or various {@link RuntimeException}.
     */
    @Override
    public void forEachRemaining(final Consumer<? super Feature> action) throws BackingStoreException {
        try {
            parse(action, true);
        } catch (Exception e) {                 // Many possible exceptions including unchecked ones.
            throw new BackingStoreException(canNotReadFile(), e);
        }
    }

    /**
     * Implementation of {@link #tryAdvance(Consumer)} and {@link #forEachRemaining(Consumer)}.
     *
     * @param  action  the action to perform on the remaining feature instances.
     * @param  all     whether to perform the action on all remaining instances or only the next one.
     * @return {@code false} if this method as detected the end of {@code <gpx>} element or the end of document.
     * @throws DataStoreException if the file contains invalid elements.
     * @throws XMLStreamException if an error occurred while reading the XML file.
     * @throws URISyntaxException if an error occurred while parsing GPX 1.0 URI.
     * @throws JAXBException if an error occurred while parsing GPX 1.1 link.
     * @throws ClassCastException if an object unmarshalled by JAXB was not of the expected type.
     * @throws NumberFormatException if a text can not be parsed as an integer or a floating point number.
     * @throws DateTimeParseException if a text can not be parsed as a date.
     * @throws EOFException if the file seems to be truncated.
     */
    @SuppressWarnings("fallthrough")
    private boolean parse(final Consumer<? super Feature> action, final boolean all)
            throws DataStoreException, XMLStreamException, JAXBException, URISyntaxException, EOFException
    {
        final XMLStreamReader reader = getReader();
        for (int type = reader.getEventType(); ; type = reader.next()) {
            /*
             * We do not need to check 'reader.hasNext()' in above loop
             * since this check is done by the END_DOCUMENT case below.
             */
            switch (type) {
                case START_ELEMENT: {
                    final Feature f;
                    switch (isGPX(reader.getNamespaceURI()) ? reader.getLocalName() : "") {
                        case Tags.WAY_POINT: f = parseWayPoint(reader, ++wayPointId); break;
                        case Tags.ROUTES:    f = parseRoute   (reader, ++routeId);    break;
                        case Tags.TRACKS:    f = parseTrack   (reader, ++trackId);    break;
                        case Tags.GPX:       throw new DataStoreContentException(nestedElement(Tags.GPX));
                        default:             skipUntilEnd(reader.getName()); continue;
                    }
                    action.accept(f);
                    if (all) continue;
                    reader.next();                                      // Skip the END_ELEMENT
                    return true;
                }
                case END_ELEMENT:  if (!isEndGPX(reader)) continue;     // else fallthrough
                case END_DOCUMENT: return false;
            }
        }
    }

    /**
     * Parses a {@code <wpt>}, {@code <rtept>} or {@code <trkpt>} element.
     * The STAX reader {@linkplain XMLStreamReader#getEventType() current event} must be a {@link #START_ELEMENT}.
     */
    private Feature parseWayPoint(final XMLStreamReader reader, final int index)
            throws DataStoreException, XMLStreamException, JAXBException, URISyntaxException, EOFException
    {
        /*
         * Way points might be located in different tag elements: <wpt>, <rtept> and <trkpt>.
         * We have to keep the current tag name in order to know when we reached the end.
         * We are lenient about namespace since we do not allow nested way points.
         */
        final String tagName = reader.getLocalName();
        final String lat = reader.getAttributeValue(null, Attributes.LATITUDE);
        final String lon = reader.getAttributeValue(null, Attributes.LONGITUDE);
        if (lat == null || lon == null) {
            throw new XMLStreamException(errors().getString(Errors.Keys.MandatoryAttribute_2,
                    (lat == null) ? Attributes.LATITUDE : Attributes.LONGITUDE, tagName));
        }
        final Feature feature = types.wayPoint.newInstance();
        feature.setPropertyValue("@identifier", index);
        feature.setPropertyValue("@geometry", new Point(Double.parseDouble(lon), Double.parseDouble(lat)));
        List<Link> links = null;
        while (true) {
            /*
             * We do not need to check 'reader.hasNext()' in above loop
             * since this check is done by the END_DOCUMENT case below.
             */
            switch (reader.next()) {
                case START_ELEMENT: {
                    final Object value;
                    final String name = reader.getLocalName();
                    switch (isGPX(reader.getNamespaceURI()) ? name : "") {
                        case Tags.NAME:             // Fallthrough to getElementText()
                        case Tags.COMMENT:          // ︙
                        case Tags.DESCRIPTION:      // ︙
                        case Tags.SOURCE:           // ︙
                        case Tags.SYMBOL:           // ︙
                        case Tags.TYPE:             value = getElementText(); break;
                        case Tags.TIME:             value = getElementAsTemporal(); break;
                        case Tags.MAGNETIC_VAR:     // Fallthrough to getElementAsDouble()
                        case Tags.GEOID_HEIGHT:     // ︙
                        case Tags.AGE_OF_GPS_DATA:  // ︙
                        case Tags.HDOP:             // ︙
                        case Tags.PDOP:             // ︙
                        case Tags.VDOP:             // ︙
                        case Tags.ELEVATION:        value = getElementAsDouble(); break;
                        case Tags.SATELITTES:       // Fallthrough to getElementAsInteger()
                        case Tags.DGPS_ID:          value = getElementAsInteger(); break;
                        case Tags.FIX:              value = Fix.fromGPX(getElementText()); break;
                        case Tags.LINK:             links = addIfNonNull(links, unmarshal(Link.class)); continue;
                        case Tags.URL:              links = addIfNonNull(links, Link.valueOf(getElementAsURI())); continue;
                        default: {
                            if (name.equals(tagName)) {
                                throw new DataStoreContentException(nestedElement(name));
                            }
                            continue;
                        }
                    }
                    feature.setPropertyValue(name, value);
                    break;
                }
                case END_ELEMENT: {
                    if (tagName.equals(reader.getLocalName()) && isGPX(reader.getNamespaceURI())) {
                        if (links != null) feature.setPropertyValue(Tags.LINK, links);
                        return feature;
                    }
                    break;
                }
                case END_DOCUMENT: {
                    throw new EOFException(endOfFile());
                }
            }
        }
    }

    /**
     * Parses a {@code <rte>} element. The STAX reader {@linkplain XMLStreamReader#getEventType() current event}
     * must be a {@link #START_ELEMENT} and the name of that start element must be {@link Tags#ROUTES}.
     */
    private Feature parseRoute(final XMLStreamReader reader, final int index)
            throws DataStoreException, XMLStreamException, JAXBException, URISyntaxException, EOFException
    {
        final Feature feature = types.route.newInstance();
        feature.setPropertyValue("@identifier", index);
        List<Feature> wayPoints = null;
        List<Link> links = null;
        while (true) {
            /*
             * We do not need to check 'reader.hasNext()' in above loop
             * since this check is done by the END_DOCUMENT case below.
             */
            switch (reader.next()) {
                case START_ELEMENT: {
                    final Object value;
                    final String name = reader.getLocalName();
                    switch (isGPX(reader.getNamespaceURI()) ? name : "") {
                        default: continue;
                        case Tags.NAME:        // Fallthrough to getElementText()
                        case Tags.COMMENT:     // ︙
                        case Tags.DESCRIPTION: // ︙
                        case Tags.SOURCE:      // ︙
                        case Tags.TYPE:        value = getElementText(); break;
                        case Tags.NUMBER:      value = getElementAsInteger(); break;
                        case Tags.LINK:        links = addIfNonNull(links, unmarshal(Link.class)); continue;
                        case Tags.URL:         links = addIfNonNull(links, Link.valueOf(getElementAsURI())); continue;
                        case Tags.ROUTES:      throw new DataStoreContentException(nestedElement(name));
                        case Tags.ROUTE_POINTS: {
                            if (wayPoints == null) wayPoints = new ArrayList<>(8);
                            wayPoints.add(parseWayPoint(reader, wayPoints.size() + 1));
                            continue;
                        }
                    }
                    feature.setPropertyValue(name, value);
                    break;
                }
                case END_ELEMENT: {
                    if (Tags.ROUTES.equals(reader.getLocalName()) && isGPX(reader.getNamespaceURI())) {
                        if (wayPoints != null) feature.setPropertyValue(Tags.ROUTE_POINTS, wayPoints);
                        if (links     != null) feature.setPropertyValue(Tags.LINK, links);
                        return feature;
                    }
                    break;
                }
                case END_DOCUMENT: {
                    throw new EOFException(endOfFile());
                }
            }
        }
    }

    /**
     * Parses a {@code <trkseg>} element. The STAX reader {@linkplain XMLStreamReader#getEventType() current event}
     * must be a {@link #START_ELEMENT} and the name of that start element must be {@link Tags#TRACK_SEGMENTS}.
     */
    private Feature parseTrackSegment(final XMLStreamReader reader, final int index)
            throws DataStoreException, XMLStreamException, JAXBException, URISyntaxException, EOFException
    {
        final Feature feature = types.trackSegment.newInstance();
        feature.setPropertyValue("@identifier", index);
        List<Feature> wayPoints = null;
        while (true) {
            /*
             * We do not need to check 'reader.hasNext()' in above loop
             * since this check is done by the END_DOCUMENT case below.
             */
            switch (reader.next()) {
                case START_ELEMENT: {
                    final String name = reader.getLocalName();
                    switch (isGPX(reader.getNamespaceURI()) ? name : "") {
                        default: continue;
                        case Tags.TRACK_POINTS: {
                            if (wayPoints == null) wayPoints = new ArrayList<>(8);
                            wayPoints.add(parseWayPoint(reader, wayPoints.size() + 1));
                            continue;
                        }
                        case Tags.TRACK_SEGMENTS: throw new DataStoreContentException(nestedElement(name));
                    }
                }
                case END_ELEMENT: {
                    if (Tags.TRACK_SEGMENTS.equals(reader.getLocalName()) && isGPX(reader.getNamespaceURI())) {
                        if (wayPoints != null) feature.setPropertyValue(Tags.TRACK_POINTS, wayPoints);
                        return feature;
                    }
                    break;
                }
                case END_DOCUMENT: {
                    throw new EOFException(endOfFile());
                }
            }
        }
    }

    /**
     * Parses a {@code <trk>} element. The STAX reader {@linkplain XMLStreamReader#getEventType() current event}
     * must be a {@link #START_ELEMENT} and the name of that start element must be {@link Tags#TRACKS}.
     */
    private Feature parseTrack(final XMLStreamReader reader, final int index)
            throws DataStoreException, XMLStreamException, JAXBException, URISyntaxException, EOFException
    {
        final Feature feature = types.track.newInstance();
        feature.setPropertyValue("@identifier", index);
        List<Feature> segments = null;
        List<Link> links = null;
        while (true) {
            /*
             * We do not need to check 'reader.hasNext()' in above loop
             * since this check is done by the END_DOCUMENT case below.
             */
            switch (reader.next()) {
                case START_ELEMENT: {
                    final Object value;
                    final String name = reader.getLocalName();
                    switch (isGPX(reader.getNamespaceURI()) ? name : "") {
                        default: continue;
                        case Tags.NAME:         // Fallthrough to getElementText()
                        case Tags.COMMENT:      // ︙
                        case Tags.DESCRIPTION:  // ︙
                        case Tags.SOURCE:       // ︙
                        case Tags.TYPE:         value = getElementText(); break;
                        case Tags.NUMBER:       value = getElementAsInteger(); break;
                        case Tags.LINK:         links = addIfNonNull(links, unmarshal(Link.class)); continue;
                        case Tags.URL:          links = addIfNonNull(links, Link.valueOf(getElementAsURI())); continue;
                        case Tags.TRACKS:       throw new DataStoreContentException(nestedElement(name));
                        case Tags.TRACK_SEGMENTS: {
                            if (segments == null) segments = new ArrayList<>(8);
                            segments.add(parseTrackSegment(reader, segments.size() + 1));
                            continue;
                        }
                    }
                    feature.setPropertyValue(name, value);
                    break;
                }
                case END_ELEMENT: {
                    if (Tags.TRACKS.equalsIgnoreCase(reader.getLocalName()) && isGPX(reader.getNamespaceURI())) {
                        if (segments != null) feature.setPropertyValue(Tags.TRACK_SEGMENTS, segments);
                        if (links    != null) feature.setPropertyValue(Tags.LINK, links);
                        return feature;
                    }
                    break;
                }
                case END_DOCUMENT: {
                    throw new EOFException(endOfFile());
                }
            }
        }
    }
}
