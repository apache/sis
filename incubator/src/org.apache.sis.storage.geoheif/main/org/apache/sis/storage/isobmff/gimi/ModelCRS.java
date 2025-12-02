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
package org.apache.sis.storage.isobmff.gimi;

import java.util.UUID;
import java.io.IOException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;


/**
 * The Coordinate Reference System.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ModelCRS extends FullBox {
    /**
     * Numerical representation of the {@code "mcrs"} box type.
     */
    public static final int BOXTYPE = ((((('m' << 8) | 'c') << 8) | 'r') << 8) | 's';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * The most significant bits of the <abbr>UUID</abbr> as a long integer.
     * It was used in an older version of the <abbr>GIMI</abbr> specification.
     * Should not be used anymore, but nevertheless kept for compatibility.
     */
    public static final long UUID_HIGH_BITS = 0x137a1742_75ac_4747L;

    /**
     * The <abbr>UUID</abbr> that identify this extension.
     * It was used in an older version of the <abbr>GIMI</abbr> specification.
     * Should not be used anymore, but nevertheless kept for compatibility.
     */
    public static final UUID EXTENDED_TYPE = new UUID(UUID_HIGH_BITS, 0x82bc_659576e8675bL);

    /**
     * Returns the identifier of this extension.
     * This value is fixed to {@link #EXTENDED_TYPE}.
     */
    @Override
    public final UUID extendedType() {
        return EXTENDED_TYPE;
    }

    /**
     * Possible values for {@link #crsEncoding}.
     */
    private static final int CRSU = ((((('c' << 8) | 'r') << 8) | 's') << 8) | 'u',
                             CURI = ((((('c' << 8) | 'u') << 8) | 'r') << 8) | 'i',
                             WKT2 = ((((('w' << 8) | 'k') << 8) | 't') << 8) | '2';

    /**
     * The encoding used for the {@link #crs} string. Values can be:
     *
     * <ul>
     *   <li>{@code "crsu"} for an <abbr>URL</abbr> such as {@code "http://www.opengis.net/def/crs/EPSG/0/32755"}.</li>
     *   <li>{@code "curi"} for a simple identifier such as {@code "[EPSG:32755]"}.</li>
     *   <li>{@code "wkt2"} for a Well-Known Text definition.</li>
     * </ul>
     */
    @Interpretation(Type.FOURCC)
    public final int crsEncoding;

    /**
     * The <abbr>CRS</abbr> definition in Well Known Text format.
     * Should be version 2 (<abbr>ISO</abbr> 19162), but this class is robust to older versions.
     */
    public final String crs;

    /**
     * The epoch as a fractional year, of {@link Float#NaN} if not applicable.
     */
    public final float epoch;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    public ModelCRS(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        crsEncoding = input.readInt();
        crs = reader.readNullTerminatedString(false);
        epoch = ((flags & 1) != 0) ? input.readFloat() : Float.NaN;
    }

    /**
     * Parses the Well Known Text.
     *
     * @param  listeners  where to report warnings.
     * @return the coordinate reference system parsed from the Well Known Text, or {@code null} if none or unparsable.
     */
    public CoordinateReferenceSystem toCRS(final StoreListeners listeners) {
        if (crs != null) try {
            switch (crsEncoding) {
                case CURI: // Fall through.
                case CRSU: return CRS.forCode(crs);
                case WKT2: return CRS.fromWKT(crs);
                default: listeners.warning("Unknown CRS encoding:" + formatFourCC(crsEncoding));
            }
        } catch (FactoryException e) {
            listeners.warning(e);
        }
        return null;
    }
}
