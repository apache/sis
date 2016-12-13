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

import java.time.temporal.Temporal;
import java.util.Collection;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.sis.storage.DataStoreException;


/**
 * Stax writer class for GPX 1.1 files.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.8
 * @module
 */
public class GPXWriter110 extends GPXWriter100 {
    /**
     *
     * @param creator gpx file creator
     */
    public GPXWriter110(final GPXStore owner, final String creator, final Object output, final String encoding)
            throws IOException, XMLStreamException, DataStoreException
    {
        super(owner, Tags.NAMESPACE_V11, creator, output, encoding);
    }

    /**
     *
     * @return GPX version 1.1
     */
    @Override
    protected String getVersion() {
        return "1.1";
    }

    /**
     * Write metadata gpx tag.
     *
     * @param metadata not null
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    @Override
    public void write(final Metadata metadata) throws XMLStreamException {
        final XMLStreamWriter writer = getWriter();
        writer.writeStartElement(namespace, Tags.METADATA);
        writeSimpleTag(namespace, Tags.NAME, metadata.name);
        writeSimpleTag(namespace, Tags.DESCRIPTION, metadata.description);
        writePerson(metadata.author);
        writeCopyright(metadata.copyright);
        for (Link uri : metadata.links) {
            writeLink(uri);
        }

        final Temporal d = metadata.time;
        if (d != null) {
            writeSimpleTag(namespace, Tags.TIME, toString(d));
        }

        writeSimpleTag(namespace, Tags.KEYWORDS, metadata.keywords);
        writeBounds(metadata.bounds);

        writer.writeEndElement();
        writer.flush();
    }

    /**
     * Write person gpx ag.
     *
     * @param person if null nothing will be written
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    protected void writePerson(final Person person) throws XMLStreamException {
        if (person == null) return;

        final XMLStreamWriter writer = getWriter();
        writer.writeStartElement(namespace, Tags.AUTHOR);
        writeSimpleTag(namespace, Tags.NAME, person.getName());
        writeSimpleTag(namespace, Tags.EMAIL, person.email);
        writeLink(person.link);
        writer.writeEndElement();
    }

    /**
     * Write multiple links.
     *
     * @param links links to write
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    @Override
    protected void writeLinkURIs(final Collection<Link> links) throws XMLStreamException {
        if (links != null && !links.isEmpty()) {
            for (Link link : links) {
                writeLink(link);
            }
        }
    }

    /**
     * Write a link tag.
     *
     * @param link if null nothing will be written
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    @Override
    protected void writeLink(final Link link) throws XMLStreamException {
        if (link == null) return;

        final XMLStreamWriter writer = getWriter();
        writer.writeStartElement(namespace, Tags.LINK);
        writer.writeAttribute(Constants.ATT_LINK_HREF, link.uri.toASCIIString());
        writer.writeEndElement();
    }

    /**
     * Write copyright xml tag.
     *
     * @param copyRight if null nothing will be written
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    public void writeCopyright(final Copyright copyRight) throws XMLStreamException {
        if (copyRight == null) return;

        final XMLStreamWriter writer = getWriter();
        writer.writeStartElement(namespace, Tags.COPYRIGHT);
        final String author = copyRight.author;
        if (author != null) {
            writer.writeAttribute(Constants.ATT_COPYRIGHT_AUTHOR, author);
        }
        writeSimpleTag(namespace, Tags.YEAR, copyRight.year);
        writeSimpleTag(namespace, Tags.LICENSE, copyRight.license);
        writer.writeEndElement();
    }

}
