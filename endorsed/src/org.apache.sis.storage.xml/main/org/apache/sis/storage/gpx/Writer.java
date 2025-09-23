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
package org.apache.sis.storage.gpx;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import javax.xml.stream.XMLStreamException;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalFeatureTypeException;
import org.apache.sis.storage.gps.Fix;
import org.apache.sis.storage.xml.stream.StaxStreamWriter;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.util.Version;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;


/**
 * Writer for GPX 1.0 and 1.1 files.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Writer extends StaxStreamWriter {
    /**
     * The GPX file version: 0 for GPX 1.0 or 1 for GPX 1.1.
     */
    private final int version;

    /**
     * The metadata to write, or {@code null} if none.
     */
    private final Metadata metadata;

    /**
     * Creates a new GPX writer for the given data store.
     *
     * @param  owner      the data store for which this writer is created.
     * @param  metadata   the metadata to write, or {@code null} if none.
     * @param  temporary  the temporary stream where to write, or {@code null} for the main storage.
     * @throws DataStoreException if the output type is not recognized or the data store is closed.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     * @throws IOException if an error occurred while preparing the output stream.
     */
    Writer(final WritableStore owner, final Metadata metadata, final OutputStream temporary)
            throws DataStoreException, XMLStreamException, IOException
    {
        super(owner, temporary);
        this.metadata = metadata;
        final Version ver = owner.version;
        if (ver != null && ver.compareTo(StoreProvider.V1_0, 2) <= 0) {
            version = 0;
        } else {
            version = 1;
        }
    }

    /**
     * Writes the XML declaration followed by GPX metadata.
     * This method shall be invoked exactly once before {@code write(Feature)}.
     *
     * @throws Exception if an error occurred while writing to the XML file.
     */
    @Override
    public void writeStartDocument() throws Exception {
        final String namespace;
        final Version ver;
        switch (version) {
            default:
            case 1: ver = StoreProvider.V1_1; namespace = Tags.NAMESPACE_V11; break;
            case 0: ver = StoreProvider.V1_0; namespace = Tags.NAMESPACE_V10; break;
        }
        super.writeStartDocument();
        writer.setDefaultNamespace(namespace);
        writer.writeStartElement(Tags.GPX);
        writer.writeDefaultNamespace(namespace);
        writer.writeAttribute(Attributes.VERSION, ver.toString());
        if (metadata != null) {
            final String creator = metadata.creator;
            if (creator != null) {
                writer.writeAttribute(Attributes.CREATOR, creator);
            }
            switch (version) {
                default:
                case 1: {
                    /*
                     * In GPX 1.1 format, the metadata are stored under a <metadata> node.
                     * This can conveniently be written by JAXB.
                     */
                    marshal(Tags.NAMESPACE_V11, Tags.METADATA, Metadata.class, metadata);
                    break;
                }
                case 0: {
                    /*
                     * In GPX 1.0 format, the metadata were written inline in the root <gpx> element.
                     * We need to write them ourself. Not all metadata can be written in that legacy format.
                     */
                    writeSingleValue(Tags.NAME,        metadata.name);
                    writeSingleValue(Tags.DESCRIPTION, metadata.description);
                    final Person author = metadata.author;
                    if (author != null) {
                        writeSingleValue(Tags.AUTHOR,  author.name);
                        writeSingleValue(Tags.EMAIL,   author.email);
                    }
                    writeLinks(metadata.links);
                    writeSingle(Tags.TIME, metadata.time);
                    writeList(Tags.KEYWORDS, metadata.keywords);
                    // Really 1.1 namespace below, not 1.0. See 'marshal(…)' javadoc for explanation.
                    marshal(Tags.NAMESPACE_V11, Tags.BOUNDS, Bounds.class, metadata.bounds);
                }
            }
        }
    }

    /**
     * Writes the given feature.
     *
     * @param  feature  the feature to write, or {@code null} if none.
     * @throws DataStoreException if the given feature is not a recognized type.
     * @throws XMLStreamException if underlying STAX writer encounter an error.
     * @throws JAXBException if underlying JAXB marshaller encounter an error.
     */
    @Override
    public void write(final AbstractFeature feature) throws DataStoreException, XMLStreamException, JAXBException {
        if (feature != null) {
            final Types types = ((WritableStore) owner).types;
            final DefaultFeatureType type = feature.getType();
            if (types.wayPoint.isAssignableFrom(type)) {
                writeWayPoint(feature, Tags.WAY_POINT);
            } else {
                final boolean isRoute = types.route.isAssignableFrom(type);
                if (!isRoute && !types.track.isAssignableFrom(type)) {
                    throw new IllegalFeatureTypeException(owner.getLocale(), owner.getFormatName(), type.getName());
                }
                writer.writeStartElement(isRoute ? Tags.ROUTES : Tags.TRACKS);
                writeSingleValue(Tags.NAME,        feature.getPropertyValue(Tags.NAME));
                writeSingleValue(Tags.COMMENT,     feature.getPropertyValue(Tags.COMMENT));
                writeSingleValue(Tags.DESCRIPTION, feature.getPropertyValue(Tags.DESCRIPTION));
                writeSingleValue(Tags.SOURCE,      feature.getPropertyValue(Tags.SOURCE));
                writeLinks((Collection<?>)         feature.getPropertyValue(Tags.LINK));
                writeSingleValue(Tags.NUMBER,      feature.getPropertyValue(Tags.NUMBER));
                if (version != 0) {
                    writeSingleValue(Tags.TYPE,    feature.getPropertyValue(Tags.TYPE));
                }
                if (isRoute) {
                    for (Object prop : (Collection<?>) feature.getPropertyValue(Tags.ROUTE_POINTS)) {
                        writeWayPoint((AbstractFeature) prop, Tags.ROUTE_POINTS);
                    }
                } else {
                    for (Object segment : (Collection<?>) feature.getPropertyValue(Tags.TRACK_SEGMENTS)) {
                        if (segment != null) {
                            writer.writeStartElement(Tags.TRACK_SEGMENTS);
                            for (Object prop : (Collection<?>) ((AbstractFeature) segment).getPropertyValue(Tags.TRACK_POINTS)) {
                                writeWayPoint((AbstractFeature) prop, Tags.TRACK_POINTS);
                            }
                            writer.writeEndElement();
                        }
                    }
                }
                writer.writeEndElement();
            }
        }
    }

    /**
     * Writes a way point, which may be standalone or part of a route or a track segment.
     *
     * @param  feature  feature to write, or {@code null} if none.
     * @param  tagName  way point tag name (cannot be {@code null}).
     * @throws XMLStreamException if underlying STAX writer encounter an error.
     * @throws JAXBException if underlying JAXB marshaller encounter an error.
     */
    private void writeWayPoint(final AbstractFeature feature, final String tagName) throws XMLStreamException, JAXBException {
        if (feature != null) {
            final double[] pt = Geometries.wrap(feature.getPropertyValue(AttributeConvention.GEOMETRY))
                                           .map(GeometryWrapper::getPointCoordinates).orElse(null);
            if (pt != null && pt.length >= 2) {
                writer.writeStartElement(tagName);
                writer.writeAttribute(Attributes.LATITUDE,  Double.toString(pt[1]));
                writer.writeAttribute(Attributes.LONGITUDE, Double.toString(pt[0]));

                writeSingleValue(Tags.ELEVATION,       feature.getPropertyValue(Tags.ELEVATION));
                writeSingleValue(Tags.TIME,            feature.getPropertyValue(Tags.TIME));
                writeSingleValue(Tags.MAGNETIC_VAR,    feature.getPropertyValue(Tags.MAGNETIC_VAR));
                writeSingleValue(Tags.GEOID_HEIGHT,    feature.getPropertyValue(Tags.GEOID_HEIGHT));
                writeSingleValue(Tags.NAME,            feature.getPropertyValue(Tags.NAME));
                writeSingleValue(Tags.COMMENT,         feature.getPropertyValue(Tags.COMMENT));
                writeSingleValue(Tags.DESCRIPTION,     feature.getPropertyValue(Tags.DESCRIPTION));
                writeSingleValue(Tags.SOURCE,          feature.getPropertyValue(Tags.SOURCE));
                writeLinks((Collection<?>)             feature.getPropertyValue(Tags.LINK));
                writeSingleValue(Tags.SYMBOL,          feature.getPropertyValue(Tags.SYMBOL));
                writeSingleValue(Tags.TYPE,            feature.getPropertyValue(Tags.TYPE));
                writeSingle((Fix)                      feature.getPropertyValue(Tags.FIX));
                writeSingleValue(Tags.SATELITTES,      feature.getPropertyValue(Tags.SATELITTES));
                writeSingleValue(Tags.HDOP,            feature.getPropertyValue(Tags.HDOP));
                writeSingleValue(Tags.VDOP,            feature.getPropertyValue(Tags.VDOP));
                writeSingleValue(Tags.PDOP,            feature.getPropertyValue(Tags.PDOP));
                writeSingleValue(Tags.AGE_OF_GPS_DATA, feature.getPropertyValue(Tags.AGE_OF_GPS_DATA));
                writeSingleValue(Tags.DGPS_ID,         feature.getPropertyValue(Tags.DGPS_ID));

                writer.writeEndElement();
            }
        }
    }

    /**
     * Writes the value of the given enumeration. This method does nothing if the given value is null.
     */
    private void writeSingle(final Fix fix) throws XMLStreamException {
        if (fix != null) {
            writeSingleValue(Tags.FIX, fix.toGPX());
        }
    }

    /**
     * Writes multiple links. This method does nothing if the given list is null.
     *
     * @param  links  the links to write.
     * @throws XMLStreamException if underlying STAX writer encounter an error.
     * @throws JAXBException if underlying JAXB marshaller encounter an error.
     */
    private void writeLinks(final Collection<?> links) throws XMLStreamException, JAXBException {
        if (links != null) {
            for (final Object link : links) {
                if (link != null) {
                    switch (version) {
                        default:
                        case 1: {
                            marshal(Tags.NAMESPACE_V11, Tags.LINK, Link.class, (Link) link);
                            break;
                        }
                        case 0: {
                            writeSingleValue(Tags.URL,      ((Link) link).uri.toASCIIString());
                            writeSingleValue(Tags.URL_NAME, ((Link) link).text);
                            return;                 // GPX 1.0 allows only 1 URL.
                        }
                    }
                }
            }
        }
    }
}
