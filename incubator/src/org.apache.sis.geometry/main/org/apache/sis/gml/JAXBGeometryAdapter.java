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

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import org.apache.sis.geometries.Geometry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Adapts an Apache SIS {@link Geometry} for use in a JAXB-annotated class.
 * Marshalling and unmarshalling it as a GML XML element through {@link GMLReader} and {@link GMLWriter}.
 * A property using this adapter must be annotated with {@code @XmlAnyElement}, not {@code @XmlElement}.
 *
 * Once {@code @XmlJavaTypeAdapter(JAXBGeometryAdapter.class)} is applied,
 * JAXB evaluates other annotations against the adapter's value type ({@link Element}, an already fully-named
 * DOM element such as {@code <gml:Point>}), and {@code @XmlAnyElement} is what inserts/reads such an element as-is,
 * without an extra synthetic wrapper element. {@code @XmlElement} cannot be used instead: {@code Element} is an
 * interface, and outside of {@code @XmlAnyElement} handling JAXB has no way to model it as a bound type, so
 * building the {@code JAXBContext} fails with an {@code IllegalAnnotationsException}.
 *
 * <p>
 * Usage:</p>  {@snippet lang="java" :
 *     @XmlAnyElement
 *     @XmlJavaTypeAdapter(JAXBGeometryAdapter.class)
 *     private Geometry geometry;
 * }
 *
 * <p>
 * Unmarshalling accepts any GML version tolerated by {@link GMLReader} (GML 2.0 or GML 3).
 * Marshalling always produces canonical GML 3.2.</p>
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since 2.0
 */
public final class JAXBGeometryAdapter extends XmlAdapter<Element, Geometry> {

    /**
     * Creates a new adapter. JAXB requires a public no-argument constructor.
     */
    public JAXBGeometryAdapter() {
    }

    /**
     * Reads the geometry described by the given DOM element.
     *
     * @param value the DOM element to convert, or {@code null}.
     * @return the geometry, or {@code null} if the given element was {@code null}.
     * @throws Exception if an error occurred while serializing the element or parsing the geometry.
     */
    @Override
    public Geometry unmarshal(final Element value) throws Exception {
        if (value == null) {
            return null;
        }

        final DOMXMLStreamReader xmlreader = new DOMXMLStreamReader(value);
        try (GMLReader reader = new GMLReader(xmlreader)) {
            return reader.readGeometry();
        }
    }

    /**
     * Writes the given geometry as a DOM element.
     *
     * @param value the geometry to convert, or {@code null}.
     * @return the DOM element, or {@code null} if the given geometry was {@code null}.
     * @throws Exception if an error occurred while writing the geometry or parsing the result.
     */
    @Override
    public Element marshal(final Geometry value) throws Exception {
        if (value == null) {
            return null;
        }

        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final XMLStreamWriter xmlwriter = XMLOutputFactory.newInstance().createXMLStreamWriter(new DOMResult(doc));

        try (GMLWriter writer = new GMLWriter(xmlwriter, GMLVersion.V3)) {
            writer.writeGeometry(value);
        }
        return doc.getDocumentElement();
    }
}
