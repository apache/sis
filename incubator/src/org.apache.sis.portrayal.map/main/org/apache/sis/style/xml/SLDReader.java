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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.style.se1.Description;
import org.apache.sis.style.se1.Symbolizer;
import org.apache.sis.style.se1.Symbology;
import org.apache.sis.style.sld1.CoverageConstraint;
import org.apache.sis.style.sld1.CoverageExtent;
import org.apache.sis.style.sld1.Extent;
import org.apache.sis.style.sld1.FeatureTypeConstraint;
import org.apache.sis.style.sld1.InlineFeature;
import org.apache.sis.style.sld1.Layer;
import org.apache.sis.style.sld1.LayerCoverageConstraints;
import org.apache.sis.style.sld1.LayerFeatureConstraints;
import org.apache.sis.style.sld1.NamedLayer;
import org.apache.sis.style.sld1.NamedStyle;
import org.apache.sis.style.sld1.RangeAxis;
import org.apache.sis.style.sld1.RemoteOWS;
import org.apache.sis.style.sld1.StyledLayerDescriptor;
import org.apache.sis.style.sld1.UseSLDLibrary;
import org.apache.sis.style.sld1.UserLayer;
import org.apache.sis.util.iso.Names;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;


/**
 * Base class of the readers of Styled Layer Descriptor documents.
 * The symbology elements are read by the {@linkplain #seReader() symbology reader} of the same version.
 * This class contains the reading code shared by {@link SLDReader100} and {@link SLDReader110},
 * which are the concrete readers used by the {@link StyleReader} facade.
 * That code is lenient: it accepts the encodings of all supported versions in the same document.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class SLDReader extends AbstractStaxReader {
    /**
     * Creates a new reader using the given StAX cursor.
     *
     * @param  reader         the StAX cursor to read from.
     * @param  filterFactory  the factory to use for creating filters and expressions.
     */
    protected SLDReader(final XMLStreamReader reader, final FilterFactory<Feature,Object,Object> filterFactory) {
        super(reader, filterFactory);
    }

    /**
     * Returns the reader of symbology elements of the same version as this reader.
     *
     * @return the symbology reader used by this style reader.
     */
    abstract SEReader seReader();

    /**
     * Reads the root element of the document.
     *
     * @return the style object read from the document.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreException if the content is not a supported style element.
     */
    final Object read() throws XMLStreamException, DataStoreException {
        moveToRoot(reader);
        final SEReader se = seReader();
        final String name = reader.getLocalName();
        switch (name) {
            case SLDTags.STYLED_LAYER_DESCRIPTOR: return readStyledLayerDescriptor();
            case SLDTags.NAMED_LAYER:             return readNamedLayer();
            case SLDTags.USER_LAYER:              return readUserLayer();
            case SLDTags.NAMED_STYLE:             return readNamedStyle();
            case SLDTags.USER_STYLE:              return readUserStyle();
            case SETags.FEATURE_TYPE_STYLE:       return se.readFeatureTypeStyle();
            case SETags.COVERAGE_STYLE:           return se.readCoverageStyle();
            case SETags.RULE:                     return se.readRule(features);
            case FilterTags.FILTER:               return se.filterReader().readFilter(features);
        }
        final Symbolizer<Feature> symbolizer = se.readSymbolizer(features);
        if (symbolizer != null) {
            return symbolizer;
        }
        final String ns = reader.getNamespaceURI();
        if (FilterTags.NAMESPACE.equals(ns) || SETags.NAMESPACE.equals(ns)) {
            final Expression<Feature,?> expression = se.filterReader().readExpression(features);
            if (expression != null) {
                return expression;
            }
        }
        throw new DataStoreContentException("Unsupported root element <" + name + ">.");
    }

    /**
     * Returns the description of the given layer, creating it if needed.
     * This is used for the {@code <Title>} and {@code <Abstract>} elements of SLD 1.0.
     */
    private Description<?> description(final Layer layer) {
        Description<?> description = layer.getDescription().orElse(null);
        if (description == null) {
            layer.setDescription(description = features.style().createDescription());
        }
        return description;
    }

    /**
     * Reads the {@code <StyledLayerDescriptor>} element at the current cursor position.
     */
    private StyledLayerDescriptor readStyledLayerDescriptor() throws XMLStreamException, DataStoreException {
        final var sld = new StyledLayerDescriptor();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.NAME:               sld.setName(readTextOrNull()); break;
                case SETags.DESCRIPTION:        sld.setDescription(seReader().readDescription(features)); break;
                case SLDTags.USE_SLD_LIBRARY:   sld.libraries().add(readUseSLDLibrary()); break;
                case SLDTags.NAMED_LAYER:       sld.layers().add(readNamedLayer()); break;
                case SLDTags.USER_LAYER:        sld.layers().add(readUserLayer()); break;
                case SETags.TITLE:
                case SETags.ABSTRACT: {
                    Description<?> description = sld.getDescription().orElse(null);
                    if (description == null) {
                        sld.setDescription(description = features.style().createDescription());
                    }
                    seReader().readTitleOrAbstract(description);
                    break;
                }
                default: unexpected(SLDTags.STYLED_LAYER_DESCRIPTOR); break;
            }
        }
        return sld;
    }

    /**
     * Reads the {@code <UseSLDLibrary>} element at the current cursor position.
     */
    private UseSLDLibrary readUseSLDLibrary() throws XMLStreamException, DataStoreException {
        final var library = new UseSLDLibrary();
        while (nextChild()) {
            if (SETags.ONLINE_RESOURCE.equals(reader.getLocalName())) {
                library.setOnlineResource(seReader().readOnlineResource());
            } else {
                unexpected(SLDTags.USE_SLD_LIBRARY);
            }
        }
        return library;
    }

    /**
     * Reads the {@code <NamedLayer>} element at the current cursor position.
     */
    private NamedLayer readNamedLayer() throws XMLStreamException, DataStoreException {
        final var layer = new NamedLayer();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.NAME:                       layer.setName(readTextOrNull()); break;
                case SETags.DESCRIPTION:                layer.setDescription(seReader().readDescription(features)); break;
                case SETags.TITLE:
                case SETags.ABSTRACT:                   seReader().readTitleOrAbstract(description(layer)); break;
                case SLDTags.LAYER_FEATURE_CONSTRAINTS: layer.setConstraints(readLayerFeatureConstraints()); break;
                case SLDTags.NAMED_STYLE:               layer.styles().add(readNamedStyle()); break;
                case SLDTags.USER_STYLE:                layer.styles().add(readUserStyle()); break;
                default: unexpected(SLDTags.NAMED_LAYER); break;
            }
        }
        return layer;
    }

    /**
     * Reads the {@code <UserLayer>} element at the current cursor position.
     */
    private UserLayer readUserLayer() throws XMLStreamException, DataStoreException {
        final var layer = new UserLayer();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.NAME:                        layer.setName(readTextOrNull()); break;
                case SETags.DESCRIPTION:                 layer.setDescription(seReader().readDescription(features)); break;
                case SETags.TITLE:
                case SETags.ABSTRACT:                    seReader().readTitleOrAbstract(description(layer)); break;
                case SLDTags.REMOTE_OWS:                 layer.setSource(readRemoteOWS()); break;
                case SLDTags.LAYER_FEATURE_CONSTRAINTS:  layer.setConstraints(readLayerFeatureConstraints()); break;
                case SLDTags.LAYER_COVERAGE_CONSTRAINTS: layer.setConstraints(readLayerCoverageConstraints()); break;
                case SLDTags.USER_STYLE:                 layer.styles().add(readUserStyle()); break;
                case SLDTags.INLINE_FEATURE: {
                    warning("The content of <" + SLDTags.INLINE_FEATURE + "> is not supported, ignored.");
                    skipElement();
                    layer.setSource(new InlineFeature());
                    break;
                }
                default: unexpected(SLDTags.USER_LAYER); break;
            }
        }
        return layer;
    }

    /**
     * Reads the {@code <NamedStyle>} element at the current cursor position.
     */
    private NamedStyle readNamedStyle() throws XMLStreamException, DataStoreException {
        final var style = new NamedStyle();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.NAME:        style.setName(readTextOrNull()); break;
                case SETags.DESCRIPTION: style.setDescription(seReader().readDescription(features)); break;
                case SETags.TITLE:
                case SETags.ABSTRACT: {
                    Description<?> description = style.getDescription().orElse(null);
                    if (description == null) {
                        style.setDescription(description = features.style().createDescription());
                    }
                    seReader().readTitleOrAbstract(description);
                    break;
                }
                default: unexpected(SLDTags.NAMED_STYLE); break;
            }
        }
        return style;
    }

    /**
     * Reads the {@code <UserStyle>} element at the current cursor position.
     * Coverage styles and links to external styles are not supported by {@link Symbology} and are ignored.
     */
    private Symbology readUserStyle() throws XMLStreamException, DataStoreException {
        final var style = new Symbology();
        while (nextChild()) {
            final String name = reader.getLocalName();
            switch (name) {
                case SETags.NAME:               style.setName(readTextOrNull()); break;
                case SETags.DESCRIPTION:        style.setDescription(seReader().readDescription(features)); break;
                case SLDTags.IS_DEFAULT:        style.setDefault(parseBoolean(readText(), name)); break;
                case SETags.FEATURE_TYPE_STYLE: style.featureTypeStyles().add(seReader().readFeatureTypeStyle()); break;
                case SETags.TITLE:
                case SETags.ABSTRACT: {
                    Description<?> description = style.getDescription().orElse(null);
                    if (description == null) {
                        style.setDescription(description = features.style().createDescription());
                    }
                    seReader().readTitleOrAbstract(description);
                    break;
                }
                case SETags.COVERAGE_STYLE:
                case SETags.ONLINE_RESOURCE: {
                    warning("The <" + name + "> element is not supported in <" + SLDTags.USER_STYLE + ">, ignored.");
                    skipElement();
                    break;
                }
                default: unexpected(SLDTags.USER_STYLE); break;
            }
        }
        return style;
    }

    /**
     * Reads the {@code <RemoteOWS>} element at the current cursor position.
     * The service is usually {@code "WFS"} or {@code "WCS"}.
     */
    private RemoteOWS readRemoteOWS() throws XMLStreamException, DataStoreException {
        final var remote = new RemoteOWS();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SLDTags.SERVICE:        remote.setService(readTextOrNull()); break;
                case SETags.ONLINE_RESOURCE: remote.setOnlineResource(seReader().readOnlineResource()); break;
                default: unexpected(SLDTags.REMOTE_OWS); break;
            }
        }
        return remote;
    }

    /**
     * Reads the {@code <LayerFeatureConstraints>} element at the current cursor position.
     */
    private LayerFeatureConstraints readLayerFeatureConstraints() throws XMLStreamException, DataStoreException {
        final var constraints = new LayerFeatureConstraints();
        while (nextChild()) {
            if (SLDTags.FEATURE_TYPE_CONSTRAINT.equals(reader.getLocalName())) {
                constraints.constraints().add(readFeatureTypeConstraint());
            } else {
                unexpected(SLDTags.LAYER_FEATURE_CONSTRAINTS);
            }
        }
        return constraints;
    }

    /**
     * Reads the {@code <FeatureTypeConstraint>} element at the current cursor position.
     */
    private FeatureTypeConstraint readFeatureTypeConstraint() throws XMLStreamException, DataStoreException {
        final var constraint = new FeatureTypeConstraint();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.FEATURE_TYPE_NAME: {
                    final String name = readTextOrNull();
                    constraint.setFeatureTypeName((name != null) ? Names.createLocalName(null, null, name) : null);
                    break;
                }
                case FilterTags.FILTER: constraint.setFilter(seReader().filterReader().readFilter(features)); break;
                case SLDTags.EXTENT: {
                    final var extent = new Extent();
                    while (nextChild()) {
                        switch (reader.getLocalName()) {
                            case SETags.NAME:   extent.setName(readTextOrNull()); break;
                            case SLDTags.VALUE: extent.setValue(readTextOrNull()); break;
                            default: unexpected(SLDTags.EXTENT); break;
                        }
                    }
                    constraint.extents().add(extent);
                    break;
                }
                default: unexpected(SLDTags.FEATURE_TYPE_CONSTRAINT); break;
            }
        }
        return constraint;
    }

    /**
     * Reads the {@code <LayerCoverageConstraints>} element at the current cursor position.
     */
    private LayerCoverageConstraints readLayerCoverageConstraints() throws XMLStreamException, DataStoreException {
        final var constraints = new LayerCoverageConstraints();
        while (nextChild()) {
            if (SLDTags.COVERAGE_CONSTRAINT.equals(reader.getLocalName())) {
                constraints.constraints().add(readCoverageConstraint());
            } else {
                unexpected(SLDTags.LAYER_COVERAGE_CONSTRAINTS);
            }
        }
        return constraints;
    }

    /**
     * Reads the {@code <CoverageConstraint>} element at the current cursor position.
     */
    private CoverageConstraint readCoverageConstraint() throws XMLStreamException, DataStoreException {
        final var constraint = new CoverageConstraint();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.COVERAGE_NAME: constraint.setCoverageName(readTextOrNull()); break;
                case SLDTags.COVERAGE_EXTENT: {
                    final var extent = new CoverageExtent();
                    while (nextChild()) {
                        switch (reader.getLocalName()) {
                            case SLDTags.TIME_PERIOD: extent.setTimePeriod(readTextOrNull()); break;
                            case SLDTags.RANGE_AXIS: {
                                final var axis = new RangeAxis();
                                while (nextChild()) {
                                    switch (reader.getLocalName()) {
                                        case SETags.NAME:   axis.setName(readTextOrNull()); break;
                                        case SLDTags.VALUE: axis.setValue(readTextOrNull()); break;
                                        default: unexpected(SLDTags.RANGE_AXIS); break;
                                    }
                                }
                                extent.rangeAxes().add(axis);
                                break;
                            }
                            default: unexpected(SLDTags.COVERAGE_EXTENT); break;
                        }
                    }
                    constraint.setCoverageExtent(extent);
                    break;
                }
                default: unexpected(SLDTags.COVERAGE_CONSTRAINT); break;
            }
        }
        return constraint;
    }
}
