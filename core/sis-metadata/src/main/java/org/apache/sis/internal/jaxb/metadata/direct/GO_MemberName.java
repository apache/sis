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
package org.apache.sis.internal.jaxb.metadata.direct;

import org.apache.sis.util.iso.DefaultMemberName;
import org.opengis.util.MemberName;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class GO_MemberName extends MetadataAdapter<MemberName, DefaultMemberName> {
    /**
     * Converts a GeoAPI interface to the SIS implementation for XML marshalling.
     *
     * @param  value The bound type value, here the GeoAPI interface.
     * @return The adapter for the given value, here the SIS implementation.
     */
    @Override
    public DefaultMemberName marshal(MemberName value) {
        return DefaultMemberName.castOrCopy(value);
    }
}
