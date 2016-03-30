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
package org.apache.sis.internal.storage;

import java.util.Collections;
import java.nio.charset.Charset;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.Static;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;


/**
 * Helper methods for the metadata created by {@code DataStore} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class MetadataHelper extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private MetadataHelper() {
    }

    /**
     * Creates a metadata object for a text file parsed from the given connector.
     *
     * @param  connector The connector.
     * @return A new, initially almost empty, metadata object.
     */
    public static DefaultMetadata createForTextFile(final StorageConnector connector) {
        final DefaultMetadata metadata = new DefaultMetadata();
        final Charset encoding = connector.getOption(OptionKey.ENCODING);
        if (encoding != null) {
            metadata.setCharacterSets(Collections.singleton(encoding));
        }
        return metadata;
    }

    /**
     * Adds the given extent to the given metadata.
     *
     * @param  addTo    The metadata where to add the extent.
     * @param  envelope The extent to add in the given metadata, or {@code null} if none.
     * @throws TransformException if an error occurred while converting the given envelope to extents.
     */
    public static void add(final DefaultMetadata addTo, final AbstractEnvelope envelope) throws TransformException {
        if (envelope != null) {
            addTo.setReferenceSystemInfo(Collections.singleton(envelope.getCoordinateReferenceSystem()));
            if (!envelope.isAllNaN()) {
                final DefaultExtent extent = new DefaultExtent();
                extent.addElements(envelope);
                final DefaultDataIdentification id = new DefaultDataIdentification();
                id.setExtents(Collections.singleton(extent));
                addTo.setIdentificationInfo(Collections.singleton(id));
            }
        }
    }
}
