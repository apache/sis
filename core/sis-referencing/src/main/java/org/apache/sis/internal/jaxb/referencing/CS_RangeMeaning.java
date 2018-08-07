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
package org.apache.sis.internal.jaxb.referencing;

import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.internal.jaxb.gml.CodeListAdapter;

import static org.apache.sis.internal.util.Constants.EPSG;


/**
 * JAXB adapter for (un)marshalling of GeoAPI code list.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 * @since   0.4
 * @module
 */
public final class CS_RangeMeaning extends CodeListAdapter<RangeMeaning> {
    /**
     * Empty constructor for JAXB only.
     */
    public CS_RangeMeaning() {
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code RangeMeaning.class}
     */
    @Override
    protected Class<RangeMeaning> getCodeListClass() {
        return RangeMeaning.class;
    }

    /**
     * Sets the default code space to {@code "EPSG"}.
     *
     * @return {@code "EPSG"}.
     */
    @Override
    protected String getCodeSpace() {
        return EPSG;
    }
}
