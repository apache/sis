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
package org.apache.sis.referencing.crs;

import java.time.temporal.Temporal;
import org.opengis.referencing.datum.Datum;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.referencing.internal.Epoch;
import org.apache.sis.referencing.privy.WKTKeywords;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DynamicReferenceFrame;


/**
 * An element inserted in the <abbr>WKT</abbr> formatting of dynamic <abbr>CRS</abbr>.
 *
 * @todo {@code MODEL} sub-element is not yet supported.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DynamicCRS extends FormattableObject {
    /**
     * The reference frame epoch.
     */
    private final Temporal epoch;

    /**
     * Creates a new element.
     *
     * @param  epoch  the reference frame epoch.
     */
    private DynamicCRS(final Temporal epoch) {
        this.epoch = epoch;
    }

    /**
     * Returns a {@code DYNAMIC} element for the given datum, or {@code null} if the datum is not dynamic.
     */
    static DynamicCRS createIfDynamic(final Datum datum) {
        if (datum instanceof DynamicReferenceFrame) {
            return new DynamicCRS(((DynamicReferenceFrame) datum).getFrameReferenceEpoch());
        }
        return null;
    }

    /**
     * Formats this epoch as a <i>Well Known Text</i> {@code CoordinateMetadata[…]} element.
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "Dynamic"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.append(new Epoch(epoch, true));
        return WKTKeywords.Dynamic;
    }
}
