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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SortedMap;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.StoreUtilities;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.AbstractMap;
import org.apache.sis.util.internal.shared.Strings;


/**
 * Default implementation of {@code TileMatrixSet} as a wrapper for {@code GridCoverage} instances.
 * This is a pyramid of two-dimensional slices represented as {@link java.awt.image.RenderedImage}s.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ImagePyramid extends AbstractMap<GenericName, ImageTileMatrix>
        implements TileMatrixSet, SortedMap<GenericName, ImageTileMatrix>
{
    /**
     * An alphanumeric identifier unique in the {@code TiledResource} that contains this {@code TileMatrixSet}.
     * A tiled resource may contain more than one tile matrix set if the resource provides different set of tiles
     * for different <abbr>CRS</abbr>.
     *
     * @see #getIdentifier()
     */
    private final GenericName identifier;

    /**
     * The provider of pyramid levels.
     */
    private final TiledGridCoverageResource.Pyramid provider;

    /**
     * The tile matrices at each level, ordered from coarser resolution to most detailed resolution.
     * This list is initially empty. Pyramid levels are created when first requested.
     */
    private final List<ImageTileMatrix> matrices;

    /**
     * First valid index value (inclusive) in {@link #matrices}.
     */
    private final int lowerMatrixIndex;

    /**
     * First index value which is known to be invalid.
     * This is the lowest value for which {@link #getOrCreateLevel(int)} returned {@code null}.
     * This value may be higher than the true number of levels if we didn't tested all values.
     */
    private int upperMatrixIndex;

    /**
     * The union of the envelopes of all tile matrix sets, or {@code null} if not yet computed.
     *
     * @see #getEnvelope()
     */
    private Envelope envelope;

    /**
     * The grid coverage processor to use when tiles use a subset of the bands.
     *
     * @see #createResourceView(long[], RenderedImage)
     */
    private final GridCoverageProcessor processor;

    /**
     * Creates a new tile matrix set.
     *
     * @param  parent     identifier of the {@code TiledResource} that contains this {@code TileMatrixSet}.
     * @param  provider   information about the tile matrices to create, and provider of pyramid levels.
     * @param  processor  the grid coverage processor to use when tiles use a subset of the bands.
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    ImagePyramid(final GenericName parent,
                 final TiledGridCoverageResource.Pyramid provider,
                 final GridCoverageProcessor processor)
    {
        this.provider  = provider;
        this.processor = processor;
        identifier = Names.createScopedName(parent, null, provider.identifier());
        matrices = new ArrayList<>();
        lowerMatrixIndex = 0;
        upperMatrixIndex = Integer.MAX_VALUE;
    }

    /**
     * Creates a new tile matrix set as a subset of the given one.
     */
    private ImagePyramid(final ImagePyramid parent, final int lowerMatrixIndex, final int upperMatrixIndex) {
        this.identifier = parent.identifier;
        this.provider   = parent.provider;
        this.processor  = parent.processor;
        this.matrices   = parent.matrices;
        this.lowerMatrixIndex = lowerMatrixIndex;
        this.upperMatrixIndex = upperMatrixIndex;
    }

    /**
     * Returns the tile matrix at the given index.
     * If that tile matrix was not already created, it is created now.
     * This method shall be invoked with a synchronization lock on {@link #matrices}.
     *
     * @param  level  the level for which to get the tile matrix.
     * @return tile matrix at the given level, or {@code null} if the given level is too high.
     * @throws BackingStoreException if an error occurred while fetching information
     */
    private ImageTileMatrix getOrCreateLevel(final int level) {
        if (level < 0 || level >= upperMatrixIndex) {
            return null;
        }
        if (level < matrices.size()) {
            final ImageTileMatrix tm = matrices.get(level);
            if (tm != null) {
                return tm;
            }
        }
        final ImageTileMatrix tm;
        try {
            final TiledGridCoverageResource resource = provider.forPyramidLevel(level);
            if (resource == null) {
                upperMatrixIndex = level;
                return null;
            }
            GenericName id = Names.createScopedName(identifier, null, provider.identifierOfLevel(level));
            tm = new ImageTileMatrix(id, resource, processor);
        } catch (DataStoreException | TransformException e) {
            throw new BackingStoreException(e);
        }
        while (matrices.size() <= level) {
            matrices.add(null);
        }
        matrices.set(level, tm);
        return tm;
    }

    /**
     * Returns the index of the pyramid name for the given identifier, or -1 if none.
     * This method shall be invoked with a synchronization lock on {@link #matrices}.
     * The returned value is an index in the {@link #matrices} list.
     *
     * @param  name      identifier of the desired level.
     * @param  required  whether to thrown an exception if the identifier is not recognized.
     * @return index of the desired level, or -1 if none and {@code required} is {@code false}.
     * @throws IllegalArgumentException if the given name is not recognized and {@code required} is {@code true}.
     */
    private int indexOf(final GenericName name, final boolean required) {
        final LocalName tip = name.tip();
        if (tip.scope().name().equals(identifier)) {
            final int level;
            try {
                level = provider.levelOfIdentifier(tip.toString());
            } catch (IllegalArgumentException e) {
                if (required) throw e;
                Logging.ignorableException(StoreUtilities.LOGGER, ImagePyramid.class, "indexOf", e);
                return -1;
            }
            if (level >= lowerMatrixIndex && level < upperMatrixIndex) {
                if (!required || getOrCreateLevel(level) != null) {
                    return level;
                }
            }
        }
        if (required) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NoSuchValue_1, name));
        }
        return -1;
    }

    /**
     * Compares two generic names for order based on the resolution of the associated tile matrix.
     * If a call to {@code compare(o1, o2)}, the comparator returns a positive number of {@code o1}
     * is the identifier of a tile matrix having a finer resolution than {@code o2}.
     */
    @Override
    public Comparator<GenericName> comparator() {
        return (GenericName o1, GenericName o2) -> {
            synchronized (matrices) {
                return indexOf(o1, false) - indexOf(o2, false);
            }
        };
    }

    /**
     * Returns whether this Tile Matrix Set has no elements.
     * Empty tile matrix sets should not be returned to users.
     */
    @Override
    public boolean isEmpty() {
        synchronized (matrices) {
            if (upperMatrixIndex <= lowerMatrixIndex) {
                return true;
            }
            if (!matrices.isEmpty()) {
                return false;
            }
            return getOrCreateLevel(lowerMatrixIndex) == null;
        }
    }

    /**
     * Returns the number of elements in this tile matrix set.
     *
     * <b>Note:</b> if this implementation is modified, revisit {@link #lastKey()}.
     */
    @Override
    public int size() {
        synchronized (matrices) {
            int level;
            while ((level = matrices.size()) < upperMatrixIndex) {
                getOrCreateLevel(level);    // Force fetching.
            }
            return upperMatrixIndex - lowerMatrixIndex;
        }
    }

    /**
     * Returns a unique (within {@link TiledResource}) identifier.
     */
    @Override
    public GenericName getIdentifier() {
        return identifier;
    }

    /**
     * Returns the coordinate reference system of all {@code TileMatrix} instances in this set.
     *
     * @throws IncompleteGridGeometryException if the tile matrices have no <abbr>CRS</abbr>.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        final ImageTileMatrix tm;
        synchronized (matrices) {
            tm = getOrCreateLevel(0);   // Really 0, not `lowerMatrixIndex`.
        }
        return tm.getTilingScheme().getCoordinateReferenceSystem();
    }

    /**
     * Returns an envelope that encompasses all {@code TileMatrix} instances in this set.
     *
     * @throws IncompleteGridGeometryException if a tiling scheme has no envelope. While not strictly mandatory,
     *         for now we consider that missing extent or missing "grid to CRS" transform is probably an error.
     */
    @Override
    public Optional<Envelope> getEnvelope() {
        synchronized (matrices) {
            if (envelope == null) {
                int i = lowerMatrixIndex;
                Envelope mayReuse = getOrCreateLevel(i).getTilingScheme().getEnvelope();
                final var union = new GeneralEnvelope(mayReuse);
                ImageTileMatrix tm;
                while ((tm = getOrCreateLevel(++i)) != null) {
                    final Envelope e = tm.getTilingScheme().getEnvelope();
                    union.add(e);
                    if (union.equals(e, 0, false)) {
                        mayReuse = e;
                    }
                }
                envelope = (union == null || union.equals(mayReuse, 0, false)) ? mayReuse : new ImmutableEnvelope(union);
            }
            return Optional.of(envelope);
        }
    }

    /**
     * Returns all {@code TileMatrix} instances in this set, together with their identifiers.
     * Entries are sorted from coarser resolution to most detailed resolution.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public SortedMap<GenericName, ? extends TileMatrix> getTileMatrices() {
        return this;
    }

    /**
     * Returns an iterator over the entries in this map.
     */
    @Override
    protected EntryIterator<GenericName, ImageTileMatrix> entryIterator() {
        return new EntryIterator<GenericName, ImageTileMatrix>() {
            /** Index of the next element to return. */
            private int index;

            /** Next element to return. */
            private ImageTileMatrix next;

            @Override protected GenericName     getKey()   {return next.getIdentifier();}
            @Override protected ImageTileMatrix getValue() {return next;}
            @Override protected boolean next() {
                synchronized (matrices) {
                    return (next = getOrCreateLevel(index++)) != null;
                }
            }
        };
    }

    /**
     * Returns the tile matrix for the given identifier.
     *
     * @param  key  tile matrix identifier.
     * @return tile matrix for the given identifier, or {@code null} if none.
     */
    @Override
    public ImageTileMatrix get(final Object key) {
        if (key instanceof GenericName) {
            synchronized (matrices) {
                return getOrCreateLevel(indexOf((GenericName) key, false));
            }
        }
        return null;
    }

    /**
     * Returns the first key of the map.
     */
    @Override
    public GenericName firstKey() {
        final ImageTileMatrix tm;
        synchronized (matrices) {
            tm = getOrCreateLevel(lowerMatrixIndex);
        }
        if (tm != null) {
            return tm.getIdentifier();
        }
        throw new NoSuchElementException();
    }

    /**
     * Returns the last key of the map.
     */
    @Override
    public GenericName lastKey() {
        final ImageTileMatrix tm;
        synchronized (matrices) {
            final int size = size();     // Implementation of `size()` fills the list.
            tm = (size != 0) ? matrices.get(size - 1) : null;
        }
        if (tm != null) {
            return tm.getIdentifier();
        }
        throw new NoSuchElementException();
    }

    /**
     * Returns a view of this map whose keys are strictly less than {@code toKey}.
     *
     * @param  toKey  high endpoint (exclusive) of the keys in the returned map.
     */
    @Override
    public SortedMap<GenericName, ImageTileMatrix> headMap(GenericName toKey) {
        synchronized (matrices) {
            return subMap(lowerMatrixIndex, indexOf(toKey, true));
        }
    }

    /**
     * Returns a view of this map whose keys are greater than or equal to {@code fromKey}.
     *
     * @param  fromKey  low endpoint (inclusive) of the keys in the returned map.
     */
    @Override
    public SortedMap<GenericName, ImageTileMatrix> tailMap(GenericName fromKey) {
        synchronized (matrices) {
            return subMap(indexOf(fromKey, true), upperMatrixIndex);
        }
    }

    /**
     * Returns a view this map whose keys range from {@code fromKey}, inclusive, to {@code toKey}, exclusive.
     *
     * @param  fromKey  low endpoint (inclusive) of the keys in the returned map.
     * @param  toKey    high endpoint (exclusive) of the keys in the returned map.
     */
    @Override
    public SortedMap<GenericName, ImageTileMatrix> subMap(GenericName fromKey, GenericName toKey) {
        synchronized (matrices) {
            return subMap(indexOf(fromKey, true), indexOf(toKey, true));
        }
    }

    /**
     * Returns a view this map whose keys range from {@code fromKey}, inclusive, to {@code toKey}, exclusive.
     *
     * @param  fromKey  low endpoint (inclusive) of the keys in the returned map.
     * @param  toKey    high endpoint (exclusive) of the keys in the returned map.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private SortedMap<GenericName, ImageTileMatrix> subMap(final int fromKey, final int toKey) {
        if (fromKey >= toKey) {
            return Collections.emptySortedMap();
        }
        if (fromKey == lowerMatrixIndex && toKey == upperMatrixIndex) {
            return this;
        }
        return new ImagePyramid(this, fromKey, toKey);
    }

    /**
     * Returns a string representation for debugging purposes.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        final Object upper;
        synchronized (matrices) {
            final Integer max = (upperMatrixIndex == Integer.MAX_VALUE) ? null : upperMatrixIndex;
            upper = (matrices.size() >= upperMatrixIndex) ? max : new NumberRange<>(Integer.class, lowerMatrixIndex, true, max, true);
        }
        return Strings.toString(getClass(), null, identifier.toString(), "lowerMatrixIndex", lowerMatrixIndex, "upperMatrixIndex", upper);
    }
}
