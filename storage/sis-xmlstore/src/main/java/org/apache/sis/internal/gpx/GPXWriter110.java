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

import java.net.URI;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import static org.apache.sis.internal.gpx.GPXConstants.*;


/**
 * Stax writer class for GPX 1.1 files.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.8
 * @module
 */
public class GPXWriter110 extends GPXWriter100{

    /**
     *
     * @param creator gpx file creator
     */
    public GPXWriter110(final String creator, final Object output) throws IOException, XMLStreamException {
        super(GPX_NAMESPACE_V11, creator, output);
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
    public void write(final MetaData metadata) throws XMLStreamException {
        writer.writeStartElement(namespace, TAG_METADATA);
        writeSimpleTag(namespace, TAG_NAME, metadata.name);
        writeSimpleTag(namespace, TAG_DESC, metadata.description);
        writePerson(metadata.person);
        writeCopyright(metadata.copyRight);
        for (URI uri : metadata.links) {
            writeLink(uri);
        }

        final Temporal d = metadata.time;
        if (d != null) {
            writeSimpleTag(namespace, TAG_METADATA_TIME, toString(d));
        }

        writeSimpleTag(namespace, TAG_METADATA_KEYWORDS, metadata.keywords);
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

        writer.writeStartElement(namespace, TAG_AUTHOR);
        writeSimpleTag(namespace, TAG_NAME, person.getName());
        writeSimpleTag(namespace, TAG_AUTHOR_EMAIL, person.email);
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
    protected void writeLinkURIs(final Collection<URI> links) throws XMLStreamException {
        if (links != null && !links.isEmpty()) {
            for (URI uri : links) {
                writeLink(uri);
            }
        }
    }

    /**
     * Write a link tag.
     *
     * @param uri if null nothing will be written
     * @throws XMLStreamException if underlying xml stax writer encounter an error
     */
    @Override
    protected void writeLink(final URI uri) throws XMLStreamException {
        if (uri == null) return;

        writer.writeStartElement(namespace, TAG_LINK);
        writer.writeAttribute(ATT_LINK_HREF, uri.toASCIIString());
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

        writer.writeStartElement(namespace, TAG_COPYRIGHT);
        final String author = copyRight.author;
        if (author != null) {
            writer.writeAttribute(ATT_COPYRIGHT_AUTHOR, author);
        }
        writeSimpleTag(namespace, TAG_COPYRIGHT_YEAR, copyRight.year);
        writeSimpleTag(namespace, TAG_COPYRIGHT_LICENSE, copyRight.license);
        writer.writeEndElement();
    }

}
