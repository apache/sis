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
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Iterator;
import javax.xml.stream.XMLStreamException;
import static org.apache.sis.internal.gpx.GPXConstants.*;
import org.apache.sis.internal.xml.StaxStreamWriter;

import org.opengis.geometry.Envelope;

import static org.apache.sis.util.ArgumentChecks.*;
import org.apache.sis.util.collection.BackingStoreException;
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
    public GPXWriter100(final String creator, final Object output) throws IOException, XMLStreamException {
        this(GPX_NAMESPACE_V10, creator, output);
    }

    /**
     *
     * @param namespace gpx namespace
     * @param creator file creator
     */
    protected  GPXWriter100(final String namespace, final String creator, final Object output) throws IOException, XMLStreamException {
        super(output);
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
        writer.writeStartDocument("UTF-8", "1.0");
        writer.flush();
    }

    /**
     *
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void writeEndDocument() throws XMLStreamException {
        writer.writeEndDocument();
        writer.flush();
    }

    /**
     *
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void writeGPXTag() throws XMLStreamException {
        writer.setDefaultNamespace(namespace);
        writer.writeStartElement(namespace, TAG_GPX);
        writer.writeAttribute(ATT_GPX_VERSION, getVersion());
        writer.writeAttribute(ATT_GPX_CREATOR, creator);
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
    public void write(final MetaData metadata, final Collection<? extends Feature> wayPoints,
            final Collection<? extends Feature> routes, final Collection<? extends Feature> tracks) throws XMLStreamException {

        writeGPXTag();

        if (metadata != null) {
            write(metadata);
        }

        if (wayPoints != null) {
            final Iterator<? extends Feature> ite = wayPoints.iterator();
            try {
                while (ite.hasNext()) {
                    writeWayPoint(ite.next(), TAG_WPT);
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

        writer.writeEndElement();
    }

    /**
     * Write GPX Metadata.
     *
     * @param metadata no null
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void write(final MetaData metadata) throws XMLStreamException {
        writeSimpleTag(namespace, TAG_NAME, metadata.name);
        writeSimpleTag(namespace, TAG_DESC, metadata.description);

        if (metadata.person != null) {
            writeSimpleTag(namespace, TAG_AUTHOR, metadata.person.name);
            writeSimpleTag(namespace, TAG_AUTHOR_EMAIL, metadata.person.email);
        }

        //model is based on 1.1 so not all attributs can be written
        writeLinkURIs(metadata.links);

        if (metadata.time != null) {
            writeSimpleTag(namespace, TAG_METADATA_TIME, toString(metadata.time));
        }

        writeSimpleTag(namespace, TAG_METADATA_KEYWORDS, metadata.keywords);
        writeBounds(metadata.bounds);
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

        writer.writeStartElement(namespace, tagName);

        final Point pt = (Point) feature.getProperty("@geometry").getValue();
        writer.writeAttribute(ATT_WPT_LAT, Double.toString(pt.getY()));
        writer.writeAttribute(ATT_WPT_LON, Double.toString(pt.getX()));

        writeProperty(TAG_WPT_ELE,          feature.getProperty(TAG_WPT_ELE));
        writeProperty(TAG_WPT_TIME,         feature.getProperty(TAG_WPT_TIME));
        writeProperty(TAG_WPT_MAGVAR,       feature.getProperty(TAG_WPT_MAGVAR));
        writeProperty(TAG_WPT_GEOIHEIGHT,   feature.getProperty(TAG_WPT_GEOIHEIGHT));
        writeProperty(TAG_NAME,             feature.getProperty(TAG_NAME));
        writeProperty(TAG_CMT,              feature.getProperty(TAG_CMT));
        writeProperty(TAG_DESC,             feature.getProperty(TAG_DESC));
        writeProperty(TAG_SRC,              feature.getProperty(TAG_SRC));
        writeLinkURIs((Collection<URI>)     feature.getPropertyValue(TAG_LINK));
        writeProperty(TAG_WPT_SYM,          feature.getProperty(TAG_WPT_SYM));
        writeProperty(TAG_TYPE,             feature.getProperty(TAG_TYPE));
        writeProperty(TAG_WPT_FIX,          feature.getProperty(TAG_WPT_FIX));
        writeProperty(TAG_WPT_SAT,          feature.getProperty(TAG_WPT_SAT));
        writeProperty(TAG_WPT_HDOP,         feature.getProperty(TAG_WPT_HDOP));
        writeProperty(TAG_WPT_VDOP,         feature.getProperty(TAG_WPT_VDOP));
        writeProperty(TAG_WPT_PDOP,         feature.getProperty(TAG_WPT_PDOP));
        writeProperty(TAG_WPT_AGEOFGPSDATA, feature.getProperty(TAG_WPT_AGEOFGPSDATA));
        writeProperty(TAG_WPT_DGPSID,       feature.getProperty(TAG_WPT_DGPSID));

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

        writer.writeStartElement(namespace, TAG_RTE);

        writeProperty(TAG_NAME,             feature.getProperty(TAG_NAME));
        writeProperty(TAG_CMT,              feature.getProperty(TAG_CMT));
        writeProperty(TAG_DESC,             feature.getProperty(TAG_DESC));
        writeProperty(TAG_SRC,              feature.getProperty(TAG_SRC));
        writeLinkURIs((Collection<URI>)     feature.getPropertyValue(TAG_LINK));
        writeProperty(TAG_NUMBER,           feature.getProperty(TAG_NUMBER));
        writeProperty(TAG_TYPE,             feature.getProperty(TAG_TYPE));

        for (Feature prop : (Collection<Feature>)feature.getPropertyValue(TAG_RTE_RTEPT)) {
            writeWayPoint((Feature) prop,TAG_RTE_RTEPT);
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

        writer.writeStartElement(namespace, TAG_TRK);

        writeProperty(TAG_NAME,             feature.getProperty(TAG_NAME));
        writeProperty(TAG_CMT,              feature.getProperty(TAG_CMT));
        writeProperty(TAG_DESC,             feature.getProperty(TAG_DESC));
        writeProperty(TAG_SRC,              feature.getProperty(TAG_SRC));
        writeLinkURIs((Collection<URI>)     feature.getPropertyValue(TAG_LINK));
        writeProperty(TAG_NUMBER,           feature.getProperty(TAG_NUMBER));
        writeProperty(TAG_TYPE,             feature.getProperty(TAG_TYPE));

        for (Feature prop : (Collection<Feature>)feature.getPropertyValue(TAG_TRK_SEG)) {
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
        writer.writeStartElement(namespace, TAG_TRK_SEG);

        for (Feature prop : (Collection<Feature>)feature.getPropertyValue(TAG_TRK_SEG_PT)) {
            writeWayPoint((Feature) prop,TAG_TRK_SEG_PT);
        }

        writer.writeEndElement();
    }

    /**
     * Write multiple links.
     *
     * @param links links to write
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    protected void writeLinkURIs(final Collection<URI> links) throws XMLStreamException {
        if (links != null && !links.isEmpty()) {
            //in gpx 1.0 we only have one link available
            writeLink(links.iterator().next());
        }
    }

    /**
     * Write a link tag.
     *
     * @param uri if null nothing will be written
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    protected void writeLink(final URI uri) throws XMLStreamException {
        if (uri == null) return;

        writer.writeStartElement(namespace, TAG_LINK);
        writer.writeAttribute(ATT_LINK_HREF, uri.toASCIIString());
        writer.writeEndElement();
    }

    /**
     * Write bounds gpx tag.
     *
     * @param env if null nothing will be written
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    protected void writeBounds(final Envelope env) throws XMLStreamException {
        if (env == null) return;

        writer.writeStartElement(namespace, TAG_BOUNDS);

        writer.writeAttribute(ATT_BOUNDS_MINLAT, Double.toString(env.getMinimum(1)));
        writer.writeAttribute(ATT_BOUNDS_MINLON, Double.toString(env.getMinimum(0)));
        writer.writeAttribute(ATT_BOUNDS_MAXLAT, Double.toString(env.getMaximum(1)));
        writer.writeAttribute(ATT_BOUNDS_MAXLON, Double.toString(env.getMaximum(0)));

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
