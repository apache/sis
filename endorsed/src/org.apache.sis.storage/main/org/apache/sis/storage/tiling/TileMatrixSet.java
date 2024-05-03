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
package org.apache.sis.storage.tiling;

import java.util.Optional;
import java.util.SortedMap;
import org.opengis.util.GenericName;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.storage.base.MetadataBuilder;


/**
 * A collection of {@code TileMatrix} in the same CRS but at different scale levels.
 * Each {@code TileMatrix} is optimized for a particular scale and is identified by a tile matrix identifier.
 * Tile matrices usually have 2 dimensions (width and height), but this API allows any number of dimensions.
 * However, the number of dimensions must be the same for all tile matrices.
 *
 * <p>The {@code TileMatrixSet} concept is derived from OGC standards. The same concept is called
 * <i>image pyramid</i> or <i>resolution levels</i> in some other standards.
 * Some standards require that all scales must be related by a power of 2,
 * but {@code TileMatrixSet} does not have this restriction.</p>
 *
 * <h2>Tile matrix identification</h2>
 * Each {@link TileMatrix} in a {@code TileMatrixSet} is identified by a {@link GenericName}.
 * Identifiers can be any character strings.
 * A common practice is to use zoom levels as identifiers, but this is not mandatory.
 * However, tile matrices must be sorted from coarser resolution (highest scale denominator)
 * to most detailed resolution (lowest scale denominator).
 *
 * <p>All methods in this interface return non-null values.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.2
 */
public interface TileMatrixSet {
    /**
     * Returns an alphanumeric identifier which is unique in the {@link TiledResource} that contains
     * this {@code TileMatrixSet}. A tiled resource may contains more than one tile matrix set if the
     * resource prepared different set of tiles for different CRS.
     *
     * @return a unique (within {@link TiledResource}) identifier.
     */
    GenericName getIdentifier();

    /**
     * Returns information about this tile matrix set.
     * The metadata should contain at least the following information:
     *
     * <ul class="verbose">
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     *       {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getCitation() citation} /
     *       {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getTitle() title}:<br>
     *       a human-readable designation for this tile matrix set.</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     *       {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getCitation() citation} /
     *       {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getIdentifier() identifier}:<br>
     *       this {@code TileMatrixSet} {@linkplain #getIdentifier() identifier}.</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     *       {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getExtents() extent}:<br>
     *       this {@code TileMatrixSet} {@linkplain #getEnvelope() envelope}.</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getReferenceSystemInfo() referenceSystemInfo}:<br>
     *       this {@code TileMatrixSet} {@linkplain #getCoordinateReferenceSystem() coordinate reference system}.</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     *       {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getResourceFormats() resourceFormat}:<br>
     *       a description of the tile format.</li>
     * </ul>
     *
     * <h4>Note for implementers</h4>
     * The default implementation creates a modifiable {@link org.apache.sis.metadata.iso.DefaultMetadata} instance with
     * values derived from {@link #getIdentifier()}, {@link #getEnvelope()} and {@link #getCoordinateReferenceSystem()}.
     * Subclasses (not users) can cast and complete those metadata.
     * In particular, implementations are encouraged to add the {@code title} and {@code resourceFormat} information.
     *
     * @return information about this tile matrix set. Should not be {@code null}.
     *
     * @since 1.5
     */
    default Metadata getMetadata() {
        final var mb = new MetadataBuilder();
        mb.addIdentifier(getIdentifier(), MetadataBuilder.Scope.RESOURCE);
        getEnvelope().ifPresent((envelope) -> mb.addExtent(envelope, null));
        mb.addReferenceSystem(getCoordinateReferenceSystem());
        return mb.build();
    }

    /**
     * Returns the coordinate reference system of all {@code TileMatrix} instances in this set.
     * This is the value returned by {@code TileMatrix.getTilingScheme().getCoordinateReferenceSystem()}.
     *
     * @return the CRS used by all {@code TileMatrix} instances in this set.
     *
     * @see TileMatrix#getTilingScheme()
     */
    CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Returns an envelope that encompasses all {@code TileMatrix} instances in this set.
     * This is the {@linkplain org.apache.sis.geometry.GeneralEnvelope#add(Envelope) union}
     * of all values returned by {@code TileMatrix.getTilingScheme().getEnvelope()}.
     * May be empty if too costly to compute.
     *
     * @return the bounding box for all tile matrices in CRS coordinates, if available.
     */
    Optional<Envelope> getEnvelope();

    /**
     * Returns all {@link TileMatrix} instances in this set, together with their identifiers.
     * For each value in the map, the associated key is {@link TileMatrix#getIdentifier()}.
     * Entries are sorted from coarser resolution (highest scale denominator) to most detailed
     * resolution (lowest scale denominator).
     * This is not necessarily the natural ordering of the {@link GenericName} instances used as keys.
     *
     * @return unmodifiable collection of all {@code TileMatrix} instances with their identifiers.
     */
    SortedMap<GenericName, ? extends TileMatrix> getTileMatrices();
}
