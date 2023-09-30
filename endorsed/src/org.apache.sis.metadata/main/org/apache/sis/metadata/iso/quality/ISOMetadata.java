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
package org.apache.sis.metadata.iso.quality;

import org.apache.sis.metadata.MetadataStandard;


/**
 * The base class of ISO 19157 implementation classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class ISOMetadata extends org.apache.sis.metadata.iso.ISOMetadata {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8310882201886331960L;

    /**
     * Constructs an initially empty metadata.
     */
    protected ISOMetadata() {
    }

    /**
     * Constructs a new metadata initialized with the values from the specified object.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     */
    protected ISOMetadata(final Object object) {
        super(object);
    }

    /**
     * Returns the metadata standard, which is {@linkplain MetadataStandard#ISO_19157 ISO 19157}.
     *
     * @return the metadata standard, which is {@linkplain MetadataStandard#ISO_19115 ISO 19157}.
     */
    @Override
    public MetadataStandard getStandard() {
        return MetadataStandard.ISO_19157;
    }
}
