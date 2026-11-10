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
import javax.xml.stream.XMLStreamWriter;
import org.opengis.util.GenericName;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.style.Style;
import org.apache.sis.style.se1.CoverageStyle;
import org.apache.sis.style.se1.Description;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Rule;
import org.apache.sis.style.se1.Symbolizer;
import org.apache.sis.style.se1.Symbology;
import org.apache.sis.style.sld1.CoverageConstraint;
import org.apache.sis.style.sld1.CoverageExtent;
import org.apache.sis.style.sld1.Extent;
import org.apache.sis.style.sld1.FeatureTypeConstraint;
import org.apache.sis.style.sld1.InlineFeature;
import org.apache.sis.style.sld1.Layer;
import org.apache.sis.style.sld1.LayerConstraints;
import org.apache.sis.style.sld1.LayerCoverageConstraints;
import org.apache.sis.style.sld1.LayerFeatureConstraints;
import org.apache.sis.style.sld1.LayerSource;
import org.apache.sis.style.sld1.NamedLayer;
import org.apache.sis.style.sld1.NamedStyle;
import org.apache.sis.style.sld1.RangeAxis;
import org.apache.sis.style.sld1.RemoteOWS;
import org.apache.sis.style.sld1.StyledLayerDescriptor;
import org.apache.sis.style.sld1.UseSLDLibrary;
import org.apache.sis.style.sld1.UserLayer;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;


/**
 * Base class of the writers of Styled Layer Descriptor documents.
 * The symbology elements are written by the {@linkplain #seWriter() symbology writer} of the same version.
 * This class contains the writing code shared by {@link SLDWriter100} and {@link SLDWriter110},
 * which are the concrete writers used by the {@link StyleWriter} facade.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class SLDWriter extends AbstractStaxWriter {
    /**
     * Creates a new writer using the given StAX cursor.
     * The XML document header is written immediately by this constructor.
     *
     * @param  writer  the StAX cursor to write to.
     * @throws XMLStreamException if the XML document header cannot be written.
     */
    protected SLDWriter(final XMLStreamWriter writer) throws XMLStreamException {
        super(writer);
    }

    /**
     * Returns the writer of symbology elements of the same version as this writer.
     *
     * @return the symbology writer used by this style writer.
     */
    abstract SEWriter seWriter();

    /**
     * Returns the namespace of the symbology elements, which is also used for the {@code <Name>}
     * and {@code <Description>} elements of SLD.
     */
    private String ns() {
        return seWriter().ns();
    }

    /**
     * Writes the given object as the root element of the document.
     *
     * @param  object  the object to write.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     * @throws DataStoreException if the object cannot be written.
     */
    final void write(final Object object) throws XMLStreamException, DataStoreException {
        final SEWriter se = seWriter();
        if      (object instanceof StyledLayerDescriptor sld) writeStyledLayerDescriptor(sld);
        else if (object instanceof NamedLayer layer)          writeNamedLayer(layer);
        else if (object instanceof UserLayer layer)           writeUserLayer(layer);
        else if (object instanceof NamedStyle style)          writeNamedStyle(style);
        else if (object instanceof Symbology style)           writeUserStyle(style);
        else if (object instanceof FeatureTypeStyle style)    se.writeFeatureTypeStyle(style);
        else if (object instanceof CoverageStyle style) {
            if (!isVersion11()) {
                throw new DataStoreContentException("Coverage styles are not supported in version " + getVersion().code + '.');
            }
            se.writeCoverageStyle(style);
        }
        else if (object instanceof Rule<?> rule)              se.writeRule(rule);
        else if (object instanceof Symbolizer<?> symbolizer)  se.writeSymbolizer(symbolizer);
        else if (object instanceof Filter<?> filter)          se.filterWriter().writeFilter(filter);
        else if (object instanceof Expression<?,?> e)         se.filterWriter().writeExpression(e);
        else {
            throw new DataStoreContentException("Unsupported object: "
                    + (object != null ? object.getClass().getName() : "null") + '.');
        }
    }

    /**
     * Writes the name and description of a layer or styled layer descriptor.
     *
     * @param  name         the name, or {@code null} if none.
     * @param  description  the description, or {@code null} if none.
     * @param  allowedIn10  whether a description is allowed in version 1.0.
     */
    private void writeNameAndDescription(final String name, final Description<?> description,
            final boolean allowedIn10) throws XMLStreamException
    {
        text(ns(), SETags.NAME, name);
        seWriter().writeDescription(description, allowedIn10);
    }

    /**
     * Writes a {@code <StyledLayerDescriptor>} element.
     */
    private void writeStyledLayerDescriptor(final StyledLayerDescriptor sld) throws XMLStreamException, DataStoreException {
        start(SLDTags.NAMESPACE, SLDTags.STYLED_LAYER_DESCRIPTOR);
        attribute(SETags.VERSION, getVersion().code);
        writeNameAndDescription(sld.getName().orElse(null), sld.getDescription().orElse(null), true);
        if (!sld.libraries().isEmpty()) {
            if (isVersion11()) {
                for (final UseSLDLibrary library : sld.libraries()) {
                    if (library.getOnlineResource().isPresent()) {
                        start(SLDTags.NAMESPACE, SLDTags.USE_SLD_LIBRARY);
                        seWriter().writeOnlineResource(ns(), library.getOnlineResource().get());
                        end();
                    }
                }
            } else {
                unsupported(SLDTags.USE_SLD_LIBRARY);
            }
        }
        for (final Layer layer : sld.layers()) {
            if (layer instanceof NamedLayer named) {
                writeNamedLayer(named);
            } else if (layer instanceof UserLayer user) {
                writeUserLayer(user);
            }
        }
        end();
    }

    /**
     * Writes a {@code <NamedLayer>} element.
     */
    private void writeNamedLayer(final NamedLayer layer) throws XMLStreamException, DataStoreException {
        start(SLDTags.NAMESPACE, SLDTags.NAMED_LAYER);
        writeNameAndDescription(layer.getName().orElse(null), layer.getDescription().orElse(null), false);
        if (layer.getConstraints().isPresent()) {
            writeLayerFeatureConstraints(layer.getConstraints().get());
        }
        for (final Style style : layer.styles()) {
            if (style instanceof NamedStyle named) {
                writeNamedStyle(named);
            } else if (style instanceof Symbology user) {
                writeUserStyle(user);
            } else {
                warning("Unsupported style: " + style.getClass().getSimpleName() + ", ignored.");
            }
        }
        end();
    }

    /**
     * Writes a {@code <UserLayer>} element.
     */
    private void writeUserLayer(final UserLayer layer) throws XMLStreamException, DataStoreException {
        start(SLDTags.NAMESPACE, SLDTags.USER_LAYER);
        writeNameAndDescription(layer.getName().orElse(null), layer.getDescription().orElse(null), false);
        final LayerSource source = layer.getSource().orElse(null);
        if (source instanceof RemoteOWS remote) {
            start(SLDTags.NAMESPACE, SLDTags.REMOTE_OWS);
            text(SLDTags.NAMESPACE, SLDTags.SERVICE, remote.getService().orElse(null));
            if (remote.getOnlineResource().isPresent()) {
                seWriter().writeOnlineResource(ns(), remote.getOnlineResource().get());
            }
            end();
        } else if (source instanceof InlineFeature inline) {
            if (isVersion11()) {
                if (!inline.features().isEmpty()) {
                    warning("The content of <" + SLDTags.INLINE_FEATURE + "> is not supported, written as an empty element.");
                }
                empty(SLDTags.NAMESPACE, SLDTags.INLINE_FEATURE);
            } else {
                unsupported(SLDTags.INLINE_FEATURE);
            }
        }
        final LayerConstraints<?> constraints = layer.getConstraints().orElse(null);
        if (constraints instanceof LayerFeatureConstraints c) {
            writeLayerFeatureConstraints(c);
        } else if (constraints instanceof LayerCoverageConstraints c) {
            if (isVersion11()) {
                writeLayerCoverageConstraints(c);
            } else {
                unsupported(SLDTags.LAYER_COVERAGE_CONSTRAINTS);
            }
        }
        for (final Symbology style : layer.styles()) {
            writeUserStyle(style);
        }
        end();
    }

    /**
     * Writes a {@code <NamedStyle>} element.
     */
    private void writeNamedStyle(final NamedStyle style) throws XMLStreamException {
        start(SLDTags.NAMESPACE, SLDTags.NAMED_STYLE);
        writeNameAndDescription(style.getName().orElse(null), style.getDescription().orElse(null), false);
        end();
    }

    /**
     * Writes a {@code <UserStyle>} element.
     */
    private void writeUserStyle(final Symbology style) throws XMLStreamException, DataStoreException {
        start(SLDTags.NAMESPACE, SLDTags.USER_STYLE);
        writeNameAndDescription(style.getName().orElse(null), style.getDescription().orElse(null), true);
        if (style.isDefault()) {
            text(SLDTags.NAMESPACE, SLDTags.IS_DEFAULT, Boolean.TRUE);
        }
        for (final FeatureTypeStyle fts : style.featureTypeStyles()) {
            seWriter().writeFeatureTypeStyle(fts);
        }
        end();
    }

    /**
     * Writes a {@code <LayerFeatureConstraints>} element.
     */
    private void writeLayerFeatureConstraints(final LayerFeatureConstraints constraints)
            throws XMLStreamException, DataStoreException
    {
        start(SLDTags.NAMESPACE, SLDTags.LAYER_FEATURE_CONSTRAINTS);
        for (final FeatureTypeConstraint constraint : constraints.constraints()) {
            start(SLDTags.NAMESPACE, SLDTags.FEATURE_TYPE_CONSTRAINT);
            final GenericName name = constraint.getFeatureTypeName().orElse(null);
            if (name != null) {
                text(ns(), SETags.FEATURE_TYPE_NAME, name.toString());
            }
            if (constraint.getFilter().isPresent()) {
                seWriter().filterWriter().writeFilter(constraint.getFilter().get());
            }
            for (final Extent extent : constraint.extents()) {
                start(SLDTags.NAMESPACE, SLDTags.EXTENT);
                text(ns(), SETags.NAME, extent.getName().orElse(null));
                text(SLDTags.NAMESPACE, SLDTags.VALUE, extent.getValue().orElse(null));
                end();
            }
            end();
        }
        end();
    }

    /**
     * Writes a {@code <LayerCoverageConstraints>} element. This element exists only in version 1.1.
     */
    private void writeLayerCoverageConstraints(final LayerCoverageConstraints constraints) throws XMLStreamException {
        start(SLDTags.NAMESPACE, SLDTags.LAYER_COVERAGE_CONSTRAINTS);
        for (final CoverageConstraint constraint : constraints.constraints()) {
            start(SLDTags.NAMESPACE, SLDTags.COVERAGE_CONSTRAINT);
            text(SETags.NAMESPACE, SETags.COVERAGE_NAME, constraint.getCoverageName().orElse(null));
            final CoverageExtent extent = constraint.getCoverageExtent().orElse(null);
            if (extent != null) {
                start(SLDTags.NAMESPACE, SLDTags.COVERAGE_EXTENT);
                for (final RangeAxis axis : extent.rangeAxes()) {
                    start(SLDTags.NAMESPACE, SLDTags.RANGE_AXIS);
                    text(SETags.NAMESPACE, SETags.NAME, axis.getName().orElse(null));
                    text(SLDTags.NAMESPACE, SLDTags.VALUE, axis.getValue().orElse(null));
                    end();
                }
                text(SLDTags.NAMESPACE, SLDTags.TIME_PERIOD, extent.getTimePeriod().orElse(null));
                end();
            }
            end();
        }
        end();
    }
}
