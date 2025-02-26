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


/**
 * Overall information relevant to the entire presentation considered as a whole.
 *
 * @todo Not yet implemented. This is currently an almost empty box.
 *
 * <h4>Container</h4>
 * The container can be a {@link Movie} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class MovieHeader extends HeaderBox {
    /**
     * Numerical representation of the {@code "mvhd"} box type.
     */
    public static final int BOXTYPE = ((((('m' << 8) | 'v') << 8) | 'h') << 8) | 'd';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * The number of time units that pass in one second.
     */
    @Interpretation(Type.UNSIGNED)
    public final int timescale;

    /**
     * Length of the presentation.
     */
    public final Duration duration;

    /*
     * Other information not yet parsed: preferred rate to play the presentation, preferred playback volume,
     * transformation matrix for the video, next track identifier to be added to the presentation.
     */

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws DataStoreContentException if the box version is unsupported.
     */
    public MovieHeader(final Reader reader) throws IOException, DataStoreContentException {
        super(reader);
        final ChannelDataInput input = reader.input;
        timescale = input.readInt();
        duration = duration(input, timescale);
    }
}
