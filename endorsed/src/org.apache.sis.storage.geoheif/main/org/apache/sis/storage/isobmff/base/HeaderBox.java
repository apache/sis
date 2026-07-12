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

import java.time.Instant;
import java.time.Duration;
import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;
import org.apache.sis.util.internal.shared.Constants;


/**
 * Common parent of boxes that are header.
 * Those boxes have in common to begin with a creation date and a modification date.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
abstract class HeaderBox extends FullBox {
    /**
     * Creation time of the presentation.
     */
    public final Instant creationTime;

    /**
     * The most recent time the presentation was modified.
     */
    public final Instant modificationTime;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    protected HeaderBox(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        final ChannelDataInput input = reader.input;
        long t0, t1;
        switch (version()) {
            case 0: {
                t0 = input.readUnsignedInt();
                t1 = input.readUnsignedInt();
                break;
            }
            case 1: {
                t0 = input.readLong();
                t1 = input.readLong();
                break;
            }
            default: {
                throw new UnsupportedVersionException(type(), version());
            }
        }
        creationTime = EPOCH.plusSeconds(t0);
        modificationTime = (t0 == t1) ? creationTime : EPOCH.plusSeconds(t1);
    }

    /**
     * Reads a duration.
     * The timescale may be defined in another box than the box being read.
     *
     * @param  input      the channel from which to read a duration.
     * @param  timescale  number of time units that pass in one second (handled as unsigned), or 0 if unknown.
     * @return the duration, or {@code null} if indefinite.
     * @throws IOException if an error occurred while reading the payload.
     */
    final Duration duration(final ChannelDataInput input, final int timescale) throws IOException {
        long count;
        if (version() == 0) {
            count = input.readUnsignedInt();
            if (count == 0xFFFFFFFF) {
                return null;
            }
        } else {
            count = input.readLong();
            if (count == -1) {
                return null;
            }
        }
        if (timescale == 0) return null;
        final long t = Integer.toUnsignedLong(timescale);
        return Duration.ofSeconds(count / t, (count % t) * Constants.NANOS_PER_SECOND / t);
    }
}
