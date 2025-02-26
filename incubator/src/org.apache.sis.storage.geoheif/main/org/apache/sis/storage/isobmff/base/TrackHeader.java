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
package org.apache.sis.storage.isobmff.base;

import java.time.Duration;
import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;


/**
 * Characteristics of a single track.
 * Exactly one {@code TrackHeader} is contained in a {@code Track}.
 *
 * @todo Not yet fully implemented.
 *
 * <h4>Container</h4>
 * The container can be a {@link Track} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class TrackHeader extends HeaderBox {
    /**
     * Numerical representation of the {@code "tkhd"} box type.
     */
    public static final int BOXTYPE = ((((('t' << 8) | 'k') << 8) | 'h') << 8) | 'd';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * An identifier that uniquely identifies this track over the entire life-time of the presentation.
     * Cannot be zero.
     */
    @Interpretation(Type.UNSIGNED)
    public final int identifier;

    /**
     * Length of the track, or {@code null} if unknown.
     */
    public final Duration duration;

    /**
     * Front to back ordering. Tracks with lower numbers are closer to the viewer.
     * Value can be negative.
     */
    public final short layer;

    /*
     * Other information not yet parsed: group or collections of tracks,
     * volume, transformation matrix, width and height.
     */

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @param  movie   the movie header declared before the track, or {@code null} if unknown.
     * @throws IOException if an error occurred while reading the payload.
     * @throws DataStoreContentException if the box version is unsupported.
     */
    public TrackHeader(final Reader reader, final MovieHeader movie) throws IOException, DataStoreContentException {
        super(reader);
        final ChannelDataInput input = reader.input;
        identifier = (int) (input.readLong() >>> Integer.SIZE);   // Low bits are reserved and not yet used.
        duration = duration(input, (movie != null) ? movie.timescale : 0);
        input.readLong();           // Reserved and not yet used.
        layer = input.readShort();
    }

    /**
     * Whether the track is enabled.
     * Track marked as not enabled should be ignored and treated as if not present.
     *
     * @return whether the track is enabled.
     */
    public boolean isEnabled() {
        return (flags & 1) != 0;
    }

    /**
     * Appends properties other than the ones defined by public fields.
     * Those properties will be shown first in the tree.
     *
     * @param  context  the tree being formatted. Can be used for fetching contextual information.
     * @param  target   the node where to add properties.
     * @param  after    {@code false} for the first nodes, or {@code true} for the last nodes.
     */
    @Override
    protected void appendTreeNodes(final Tree context, final TreeTable.Node target, final boolean after) {
        super.appendTreeNodes(context, target, after);
        if (!after) {
            final boolean enabled = isEnabled();
            TreeTable.Node child = target.newChild();
            child.setValue(TableColumn.NAME, "enabled");
            child.setValue(TableColumn.VALUE, enabled);
            child.setValue(TableColumn.VALUE_AS_TEXT, String.valueOf(enabled));
        }
    }
}
