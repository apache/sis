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


import com.esri.core.geometry.Point;
import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Iterator;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.internal.xml.StaxStreamWriter;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.BackingStoreException;

import static org.apache.sis.util.ArgumentChecks.*;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.Property;


/**
 * Stax writer class for GPX 1.0 files.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.8
 * @module
 */
public class GPXWriter100 extends StaxStreamWriter {

    private final String creator;

    /**
     * GPX file namespace
     */
    protected final String namespace;

    /**
     *
     * @param creator file creator
     */
    public GPXWriter100(final GPXStore owner, final String creator, final Object output, final String encoding)
            throws IOException, XMLStreamException, DataStoreException
    {
        this(owner, Tags.NAMESPACE_V10, creator, output, encoding);
    }

    /**
     *
     * @param namespace gpx namespace
     * @param creator file creator
     */
    GPXWriter100(final GPXStore owner, final String namespace, final String creator, final Object output, final String encoding)
            throws DataStoreException, XMLStreamException, IOException
    {
        super(owner, output, encoding);
        ensureNonNull("creator", creator);
        this.creator = creator;
        this.namespace = namespace;
    }

    /**
     *
     * @return GPX version 1.0
     */
    protected String getVersion() {
        return "1.0";
    }

    /**
     * Start gpx document.
     *
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void writeStartDocument() throws XMLStreamException {
        final XMLStreamWriter writer = getWriter();
        writer.writeStartDocument("UTF-8", "1.0");
        writer.flush();
    }

    /**
     *
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void writeEndDocument() throws XMLStreamException {
        final XMLStreamWriter writer = getWriter();
        writer.writeEndDocument();
        writer.flush();
    }

    /**
     *
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void writeGPXTag() throws XMLStreamException {
        final XMLStreamWriter writer = getWriter();
        writer.setDefaultNamespace(namespace);
        writer.writeStartElement(namespace, Tags.GPX);
        writer.writeAttribute(Constants.ATT_GPX_VERSION, getVersion());
        writer.writeAttribute(Constants.ATT_GPX_CREATOR, creator);
        writer.writeDefaultNamespace(namespace);
        writer.flush();
    }

    /**
     * Shortcut methods to write all gpx elements.
     *
     * @param metadata can be null
     * @param wayPoints can be null
     * @param routes can be null
     * @param tracks can be null
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void write(final Metadata metadata, final Collection<? extends Feature> wayPoints,
            final Collection<? extends Feature> routes, final Collection<? extends Feature> tracks) throws XMLStreamException {

        writeGPXTag();

        if (metadata != null) {
            write(metadata);
        }

        if (wayPoints != null) {
            final Iterator<? extends Feature> ite = wayPoints.iterator();
            try {
                while (ite.hasNext()) {
                    writeWayPoint(ite.next(), Tags.WAY_POINT);
                }
            } catch (BackingStoreException ex) {
                final Throwable cause = ex.getCause();
                throw (cause instanceof XMLStreamException) ? (XMLStreamException) cause : new XMLStreamException(cause);
            } finally {
                if (ite instanceof Closeable) {
                    try {
                        ((Closeable) ite).close();
                    } catch (IOException ex) {
                        throw new XMLStreamException(ex);
                    }
                }
            }
        }

        if (routes != null) {
            final Iterator<? extends Feature> ite = routes.iterator();
            try {
                while (ite.hasNext()) {
                    writeRoute(ite.next());
                }
            } catch (BackingStoreException ex) {
                final Throwable cause = ex.getCause();
                throw (cause instanceof XMLStreamException) ? (XMLStreamException) cause : new XMLStreamException(cause);
            } finally {
                if (ite instanceof Closeable) {
                    try {
                        ((Closeable) ite).close();
                    } catch (IOException ex) {
                        throw new XMLStreamException(ex);
                    }
                }
            }
        }

        if (tracks != null) {
            final Iterator<? extends Feature> ite = tracks.iterator();
            try {
                while (ite.hasNext()) {
                    writeTrack(ite.next());
                }
            } catch (BackingStoreException ex) {
                final Throwable cause = ex.getCause();
                throw (cause instanceof XMLStreamException) ? (XMLStreamException) cause : new XMLStreamException(cause);
            } finally {
                if (ite instanceof Closeable) {
                    try {
                        ((Closeable) ite).close();
                    } catch (IOException ex) {
                        throw new XMLStreamException(ex);
                    }
                }
            }
        }

        final XMLStreamWriter writer = getWriter();
        writer.writeEndElement();
    }

    /**
     * Write GPX Metadata.
     *
     * @param metadata no null
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void write(final Metadata metadata) throws XMLStreamException {
        writeSimpleTag(namespace, Tags.NAME, metadata.name);
        writeSimpleTag(namespace, Tags.DESCRIPTION, metadata.description);

        if (metadata.author != null) {
            writeSimpleTag(namespace, Tags.AUTHOR, metadata.author.name);
            writeSimpleTag(namespace, Tags.EMAIL, metadata.author.email);
        }

        //model is based on 1.1 so not all attributs can be written
        writeLinkURIs(metadata.links);

        if (metadata.time != null) {
            writeSimpleTag(namespace, Tags.TIME, toString(metadata.time));
        }

        writeSimpleTag(namespace, Tags.KEYWORDS, metadata.keywords);
        writeBounds(metadata.bounds);
        final XMLStreamWriter writer = getWriter();
        writer.flush();
    }

    /**
     * Write a way point.
     *
     * @param feature, can be null
     * @param tagName waypoint tag name, not null
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void writeWayPoint(final Feature feature, final String tagName) throws XMLStreamException {
        if (feature == null) return;
        final XMLStreamWriter writer = getWriter();
        writer.writeStartElement(namespace, tagName);

        final Point pt = (Point) feature.getProperty("@geometry").getValue();
        writer.writeAttribute(Constants.ATT_WPT_LAT, Double.toString(pt.getY()));
        writer.writeAttribute(Constants.ATT_WPT_LON, Double.toString(pt.getX()));

        writeProperty(Tags.ELEVATION,       feature.getProperty(Tags.ELEVATION));
        writeProperty(Tags.TIME,            feature.getProperty(Tags.TIME));
        writeProperty(Tags.MAGNETIC_VAR,    feature.getProperty(Tags.MAGNETIC_VAR));
        writeProperty(Tags.GEOID_HEIGHT,    feature.getProperty(Tags.GEOID_HEIGHT));
        writeProperty(Tags.NAME,            feature.getProperty(Tags.NAME));
        writeProperty(Tags.COMMENT,         feature.getProperty(Tags.COMMENT));
        writeProperty(Tags.DESCRIPTION,     feature.getProperty(Tags.DESCRIPTION));
        writeProperty(Tags.SOURCE,          feature.getProperty(Tags.SOURCE));
        writeLinkURIs((Collection<Link>)    feature.getPropertyValue(Tags.LINK));
        writeProperty(Tags.SYMBOL,          feature.getProperty(Tags.SYMBOL));
        writeProperty(Tags.TYPE,            feature.getProperty(Tags.TYPE));
        writeProperty(Tags.FIX,             feature.getProperty(Tags.FIX));
        writeProperty(Tags.SATELITTES,      feature.getProperty(Tags.SATELITTES));
        writeProperty(Tags.HDOP,            feature.getProperty(Tags.HDOP));
        writeProperty(Tags.VDOP,            feature.getProperty(Tags.VDOP));
        writeProperty(Tags.PDOP,            feature.getProperty(Tags.PDOP));
        writeProperty(Tags.AGE_OF_GPS_DATA, feature.getProperty(Tags.AGE_OF_GPS_DATA));
        writeProperty(Tags.DGPS_ID,         feature.getProperty(Tags.DGPS_ID));

        writer.writeEndElement();
    }

    /**
     * Write a route feature.
     *
     * @param feature, can be null
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void writeRoute(final Feature feature) throws XMLStreamException {
        if (feature == null) return;

        final XMLStreamWriter writer = getWriter();
        writer.writeStartElement(namespace, Tags.ROUTES);

        writeProperty(Tags.NAME,         feature.getProperty(Tags.NAME));
        writeProperty(Tags.COMMENT,      feature.getProperty(Tags.COMMENT));
        writeProperty(Tags.DESCRIPTION,  feature.getProperty(Tags.DESCRIPTION));
        writeProperty(Tags.SOURCE,       feature.getProperty(Tags.SOURCE));
        writeLinkURIs((Collection<Link>) feature.getPropertyValue(Tags.LINK));
        writeProperty(Tags.NUMBER,       feature.getProperty(Tags.NUMBER));
        writeProperty(Tags.TYPE,         feature.getProperty(Tags.TYPE));

        for (Feature prop : (Collection<Feature>)feature.getPropertyValue(Tags.ROUTE_POINTS)) {
            writeWayPoint((Feature) prop,Tags.ROUTE_POINTS);
        }

        writer.writeEndElement();
    }

    /**
     * Write a track feature.
     *
     * @param feature track, can be null
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void writeTrack(final Feature feature) throws XMLStreamException {
        if (feature == null) return;

        final XMLStreamWriter writer = getWriter();
        writer.writeStartElement(namespace, Tags.TRACKS);

        writeProperty(Tags.NAME,         feature.getProperty(Tags.NAME));
        writeProperty(Tags.COMMENT,      feature.getProperty(Tags.COMMENT));
        writeProperty(Tags.DESCRIPTION,  feature.getProperty(Tags.DESCRIPTION));
        writeProperty(Tags.SOURCE,       feature.getProperty(Tags.SOURCE));
        writeLinkURIs((Collection<Link>) feature.getPropertyValue(Tags.LINK));
        writeProperty(Tags.NUMBER,       feature.getProperty(Tags.NUMBER));
        writeProperty(Tags.TYPE,         feature.getProperty(Tags.TYPE));

        for (Feature prop : (Collection<Feature>)feature.getPropertyValue(Tags.TRACK_SEGMENTS)) {
            writeTrackSegment(prop);
        }

        writer.writeEndElement();
    }

    /**
     * Write a track segment feature.
     *
     * @param feature track segment, can be null
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    protected void writeTrackSegment(final Feature feature) throws XMLStreamException {
        if (feature == null) return;
        final XMLStreamWriter writer = getWriter();
        writer.writeStartElement(namespace, Tags.TRACK_SEGMENTS);

        for (Feature prop : (Collection<Feature>)feature.getPropertyValue(Tags.TRACK_POINTS)) {
            writeWayPoint((Feature) prop,Tags.TRACK_POINTS);
        }

        writer.writeEndElement();
    }

    /**
     * Write multiple links.
     *
     * @param links links to write
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    protected void writeLinkURIs(final Collection<Link> links) throws XMLStreamException {
        if (links != null && !links.isEmpty()) {
            //in gpx 1.0 we only have one link available
            writeLink(links.iterator().next());
        }
    }

    /**
     * Write a link tag.
     *
     * @param link if null nothing will be written
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    protected void writeLink(final Link link) throws XMLStreamException {
        if (link == null) return;

        final XMLStreamWriter writer = getWriter();
        writer.writeStartElement(namespace, Tags.LINK);
        writer.writeAttribute(Constants.ATT_LINK_HREF, link.uri.toASCIIString());
        writer.writeEndElement();
    }

    /**
     * Write bounds gpx tag.
     *
     * @param env if null nothing will be written
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    protected void writeBounds(final GeographicBoundingBox env) throws XMLStreamException {
        if (env == null) return;

        final XMLStreamWriter writer = getWriter();
        writer.writeStartElement(namespace, Tags.BOUNDS);

        writer.writeAttribute(Constants.ATT_BOUNDS_MINLAT, Double.toString(env.getSouthBoundLatitude()));
        writer.writeAttribute(Constants.ATT_BOUNDS_MINLON, Double.toString(env.getWestBoundLongitude()));
        writer.writeAttribute(Constants.ATT_BOUNDS_MAXLAT, Double.toString(env.getNorthBoundLatitude()));
        writer.writeAttribute(Constants.ATT_BOUNDS_MAXLON, Double.toString(env.getEastBoundLongitude()));

        writer.writeEndElement();
    }

    /**
     *
     * @param tagName property tag name
     * @param prop can be null
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    protected void writeProperty(final String tagName,final Property prop) throws XMLStreamException {
        if (prop == null) return;

        Object val = prop.getValue();
        if (val instanceof Temporal) {
            val = toString((Temporal)val);
        }

        writeSimpleTag(namespace, tagName, val);
    }

    /**
     * Convert temporal object to it's most appropriate ISO-8601 string representation.
     *
     * @param temp not null
     * @return String representation
     */
    protected static String toString(final Temporal temp){
        if(temp instanceof LocalDate){
            return DateTimeFormatter.ISO_DATE.format(temp);
        }else if(temp instanceof LocalDateTime){
            return DateTimeFormatter.ISO_DATE_TIME.format(temp);
        }else if(temp instanceof Instant){
            return DateTimeFormatter.ISO_INSTANT.format(temp);
        }else{
            throw new IllegalArgumentException("Unsupported temporal element "+temp.getClass());
        }
    }
}
