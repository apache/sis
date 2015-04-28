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
package org.apache.sis.internal.simple;

import java.util.Collection;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.util.InternationalString;

import static org.apache.sis.internal.util.CollectionsExt.singletonOrEmpty;


/**
 * A trivial implementation of {@link Extent} containing only geographic, vertical and temporal extent.
 * This class may be used only as adapter for API expecting the full {@code Extent} object when only a
 * component is available.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class SimpleExtent implements Extent {
    /**
     * Provides geographic component of the extent of the referring object.
     */
    private final GeographicExtent geographicElements;

    /**
     * Provides vertical component of the extent of the referring object.
     */
    private final VerticalExtent verticalElements;

    /**
     * Provides temporal component of the extent of the referring object.
     */
    private final TemporalExtent temporalElements;

    /**
     * Creates a new extent with the given elements.
     *
     * @param geographicElements Geographic components of the extent, or {@code null}.
     * @param verticalElements   Vertical   components of the extent, or {@code null}.
     * @param temporalElements   Temporal   components of the extent, or {@code null}.
     */
    public SimpleExtent(final GeographicExtent geographicElements,
                        final VerticalExtent   verticalElements,
                        final TemporalExtent   temporalElements)
    {
        this.geographicElements = geographicElements;
        this.verticalElements   = verticalElements;
        this.temporalElements   = temporalElements;
    }

    @Override public InternationalString          getDescription()        {return null;}
    @Override public Collection<GeographicExtent> getGeographicElements() {return singletonOrEmpty(geographicElements);}
    @Override public Collection<VerticalExtent>   getVerticalElements()   {return singletonOrEmpty(verticalElements);}
    @Override public Collection<TemporalExtent>   getTemporalElements()   {return singletonOrEmpty(temporalElements);}
}
