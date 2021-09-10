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
package org.apache.sis.gui.metadata;

import java.text.FieldPosition;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.metadata.spatial.GridSpatialRepresentation;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.iso.Types;

import static org.apache.sis.internal.util.CollectionsExt.nonNull;


/**
 * The pane where to show the values of {@link SpatialRepresentation} objects.
 * The same pane can be used for an arbitrary amount of spatial representations.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class RepresentationInfo extends Section<SpatialRepresentation> {
    /**
     * The reference system, or {@code null} if none.
     */
    private ReferenceSystem referenceSystem;

    /**
     * Creates an initially empty view for spatial representation information.
     */
    RepresentationInfo(final MetadataSummary owner) {
        super(owner);
        finished();
    }

    /**
     * Sets the spatial representation information from the given metadata.
     */
    @Override
    void setInformation(final Metadata metadata) {
        referenceSystem = null;
        if (metadata != null) {
            for (final ReferenceSystem rs : nonNull(metadata.getReferenceSystemInfo())) {
                if (rs != null) {
                    referenceSystem = rs;
                    break;
                }
            }
        }
        setInformation(nonNull(metadata == null ? null : metadata.getSpatialRepresentationInfo()), SpatialRepresentation[]::new);
    }

    /**
     * Invoked when new spatial representation information should be shown.
     * This method updates all fields in this section with the content of given information.
     * The information to show depends on the {@code info} subtype.
     *
     * @todo if there is more than one CRS, verify which one would fit better the current spatial representation.
     */
    @Override
    void buildContent(final SpatialRepresentation info) {
        if (info instanceof GridSpatialRepresentation) {
            build((GridSpatialRepresentation) info);
        }
        addLine(Vocabulary.Keys.ReferenceSystem, IdentifiedObjects.getDisplayName(referenceSystem, owner.vocabulary.getLocale()));
    }

    /**
     * Adds information specific to the {@link GridSpatialRepresentation} subtype.
     *
     * @todo Change the representation for using a table instead. In addition of dimension name and size,
     *       the metadata may also provide a more meaningful title (e.g. "longitude"), a description and
     *       the resolution. Note that VectorSpatialRepresentation would also needs a table.
     */
    private void build(final GridSpatialRepresentation info) {
        final Integer dimension = info.getNumberOfDimensions();
        if (dimension != null) {
            addLine(Vocabulary.Keys.NumberOfDimensions, owner.getNumberFormat().format(dimension));
        }
        final StringBuffer gridSize   = new StringBuffer(20);
        final StringBuffer resolution = new StringBuffer(20);
        for (final Dimension dim : nonNull(info.getAxisDimensionProperties())) {
            final String  name = owner.string(Types.getCodeTitle(dim.getDimensionName()));
            final Integer size = dim.getDimensionSize();
            if (name != null || size != null) {
                if (gridSize.length() != 0) {
                    gridSize.append(" × ");
                }
                if (size != null) {
                    owner.getNumberFormat().format(size, gridSize, new FieldPosition(0));
                }
                if (name != null) {
                    if (size != null) gridSize.append(' ');
                    gridSize.append(name);
                }
            }
            final Double r = dim.getResolution();
            if (r != null) {
                if (resolution.length() != 0) {
                    resolution.append(" × ");
                }
                owner.getNumberFormat().format(r, resolution, new FieldPosition(0));
            }
        }
        if (gridSize.length() != 0) {
            addLine(Vocabulary.Keys.Dimensions, gridSize.toString());
        }
        if (resolution.length() != 0) {
            addLine(Vocabulary.Keys.Resolution, resolution.toString());
        }
        addLine(Vocabulary.Keys.CellGeometry, owner.string(Types.getCodeTitle(info.getCellGeometry())));
    }
}
