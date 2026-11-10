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
package org.apache.sis.style.xml;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory;


/**
 * Reader of Styled Layer Descriptor and Symbology Encoding documents, using the StAX cursor API.
 * This reader accepts all versions listed in {@link StyleVersion}: the version does not need to
 * be known in advance. The version is detected from the root element, and the document is read
 * by the reader for that version. Reading is lenient: a document mixing the encodings of different
 * versions is accepted. The root element of the document can be any of the following elements:
 *
 * <table class="sis">
 *   <caption>Supported root elements</caption>
 *   <tr><th>XML element</th>                   <th>Java object</th></tr>
 *   <tr><td>{@code StyledLayerDescriptor}</td> <td>{@link org.apache.sis.style.sld1.StyledLayerDescriptor}</td></tr>
 *   <tr><td>{@code NamedLayer}</td>            <td>{@link org.apache.sis.style.sld1.NamedLayer}</td></tr>
 *   <tr><td>{@code UserLayer}</td>             <td>{@link org.apache.sis.style.sld1.UserLayer}</td></tr>
 *   <tr><td>{@code NamedStyle}</td>            <td>{@link org.apache.sis.style.sld1.NamedStyle}</td></tr>
 *   <tr><td>{@code UserStyle}</td>             <td>{@link org.apache.sis.style.se1.Symbology}</td></tr>
 *   <tr><td>{@code FeatureTypeStyle}</td>      <td>{@link org.apache.sis.style.se1.FeatureTypeStyle}</td></tr>
 *   <tr><td>{@code CoverageStyle}</td>         <td>{@link org.apache.sis.style.se1.CoverageStyle}</td></tr>
 *   <tr><td>{@code Rule}</td>                  <td>{@link org.apache.sis.style.se1.Rule}</td></tr>
 *   <tr><td>Any symbolizer</td>                <td>{@link org.apache.sis.style.se1.Symbolizer}</td></tr>
 *   <tr><td>{@code ogc:Filter}</td>            <td>{@link org.opengis.filter.Filter}</td></tr>
 *   <tr><td>Any expression</td>                <td>{@link org.opengis.filter.Expression}</td></tr>
 * </table>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>The content of {@code <InlineFeature>} is ignored.</li>
 *   <li>{@code <CoverageStyle>} and {@code <OnlineResource>} elements inside a {@code <UserStyle>} are ignored.</li>
 *   <li>Online resources are not resolved. For example, a rule given by a link is read as an empty rule
 *       with an {@linkplain org.apache.sis.style.se1.Rule#getOnlineSource() online source}.</li>
 *   <li>The geometries of spatial filters are read as {@link Geometry} instances of the Apache SIS geometry
 *       library. The filter factory shall support that geometry library.</li>
 * </ul>
 *
 * Unsupported or unknown elements are skipped and reported in the {@linkplain #getWarnings() warnings}.
 *
 * <p>Usage:</p>
 * {@snippet lang="java" :
 *     try (StyleReader reader = new StyleReader(inputStream)) {
 *         StyledLayerDescriptor sld = reader.read(StyledLayerDescriptor.class);
 *     }
 *     }
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class StyleReader implements AutoCloseable {
    /**
     * The underlying StAX cursor.
     */
    private final XMLStreamReader reader;

    /**
     * The factory to use for creating filters and expressions.
     */
    private final FilterFactory<Feature,Object,Object> filterFactory;

    /**
     * The reader for the version of the document, or {@code null} if the document has not yet been read.
     *
     * @see #read()
     */
    private SLDReader delegate;

    /**
     * The version detected from the root element, or {@code null} if unknown or not yet detected.
     *
     * @see #getVersion()
     */
    private StyleVersion version;

    /**
     * Creates a new reader using the given StAX cursor and a default filter factory.
     * The cursor can be positioned anywhere before the root element.
     *
     * @param  reader  the StAX cursor to read from.
     */
    public StyleReader(final XMLStreamReader reader) {
        this(reader, null);
    }

    /**
     * Creates a new reader using the given StAX cursor and the given filter factory.
     * The cursor can be positioned anywhere before the root element.
     *
     * @param  reader         the StAX cursor to read from.
     * @param  filterFactory  the factory to use for creating filters and expressions, or {@code null} for a default factory.
     */
    @SuppressWarnings("unchecked")
    public StyleReader(final XMLStreamReader reader, final FilterFactory<Feature,?,?> filterFactory) {
        this.reader = reader;
        this.filterFactory = (filterFactory != null)
                ? (FilterFactory<Feature,Object,Object>) filterFactory
                : defaultFilterFactory();
    }

    /**
     * Creates a new reader for the given input stream and a default filter factory.
     *
     * @param  in  the stream to read from.
     * @throws XMLStreamException if the StAX cursor cannot be created.
     */
    public StyleReader(final InputStream in) throws XMLStreamException {
        this(createReader(in));
    }

    /**
     * Creates a StAX cursor for the given input stream, with external entities disabled.
     */
    private static XMLStreamReader createReader(final InputStream in) throws XMLStreamException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        return factory.createXMLStreamReader(in);
    }

    /**
     * Returns the default filter factory. This factory uses the Apache SIS geometry library,
     * because the geometries in spatial filters are read as instances of that library.
     * If that library is not available, then the default factory for features is used.
     */
    @SuppressWarnings("unchecked")
    private static FilterFactory<Feature,Object,Object> defaultFilterFactory() {
        try {
            return (FilterFactory<Feature,Object,Object>) (FilterFactory<?,?,?>)
                    new DefaultFilterFactory.Features<>(Geometry.class, Object.class, WraparoundMethod.NONE);
        } catch (IllegalArgumentException e) {
            return DefaultFilterFactory.forFeatures();
        }
    }

    /**
     * Reads the root element of the document.
     * See the class Javadoc for the list of supported elements and the types of returned objects.
     *
     * @return the object read from the document.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreException if the content is not a supported style document.
     */
    public Object read() throws XMLStreamException, DataStoreException {
        AbstractStaxReader.moveToRoot(reader);
        version = AbstractStaxReader.detectVersion(reader);
        if (version == StyleVersion.V1_0_0) {
            delegate = new SLDReader100(reader, filterFactory);
        } else {
            delegate = new SLDReader110(reader, filterFactory);     // Also used when the version is unknown.
        }
        return delegate.read();
    }

    /**
     * Reads the root element of the document, which is expected to be of the given type.
     *
     * @param  <T>   compile-time value of the {@code type} argument.
     * @param  type  the expected type of the object to read.
     * @return the object read from the document.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreException if the content is not a supported style document,
     *         or if the root element is not of the expected type.
     */
    public <T> T read(final Class<T> type) throws XMLStreamException, DataStoreException {
        final Object value = read();
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new DataStoreContentException("Expected a " + type.getSimpleName() + " but found a "
                + value.getClass().getSimpleName() + '.');
    }

    /**
     * Returns the version of the document, as declared in the {@code version} attribute of the root element
     * or as inferred from the namespaces of the root element. This information is available after the document
     * has been read. May be empty if the version cannot be determined, for example for a document containing
     * only an {@code ogc:Filter}. In the latter case, the document has been read with the reader of the most
     * recent version.
     *
     * @return version of the document read.
     */
    public Optional<StyleVersion> getVersion() {
        return Optional.ofNullable(version);
    }

    /**
     * Returns the warnings emitted while reading the document.
     * Warnings are emitted for unsupported or unknown elements, which are skipped.
     *
     * @return the warnings, or an empty list if none.
     */
    public List<String> getWarnings() {
        return (delegate != null) ? Collections.unmodifiableList(delegate.warnings()) : List.of();
    }

    /**
     * Closes the underlying StAX cursor.
     *
     * @throws XMLStreamException if an error occurred while closing the cursor.
     */
    @Override
    public void close() throws XMLStreamException {
        reader.close();
    }
}
