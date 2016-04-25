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
package org.apache.sis.console;

import java.io.IOException;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.Metadata;
import org.opengis.util.FactoryException;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.io.wkt.Convention;


/**
 * The "crs" sub-command.
 * CRS are considered as a kind of metadata here.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
final class CRSCommand extends MetadataCommand {
    /**
     * Creates the {@code "metadata"}, {@code "crs"} or {@code "identifier"} sub-command.
     */
    CRSCommand(final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args);

        // Default output format.
        outputFormat = Format.WKT;
        convention = Convention.WKT2_SIMPLIFIED;
    }

    /**
     * Prints metadata or CRS information.
     *
     * @throws DataStoreException if an error occurred while reading the file.
     * @throws JAXBException if an error occurred while producing the XML output.
     * @throws FactoryException if an error occurred while looking for a CRS identifier.
     * @throws IOException should never happen, since we are appending to a print writer.
     */
    @Override
    public int run() throws InvalidOptionException, DataStoreException, JAXBException, FactoryException, IOException {
        parseArguments();
        final Object metadata = readMetadataOrCRS();
        if (hasUnexpectedFileCount) {
            return Command.INVALID_ARGUMENT_EXIT_CODE;
        }
        if (metadata != null) {
            if (metadata instanceof CoordinateReferenceSystem) {
                format(metadata);
            } else {
                for (final ReferenceSystem rs : ((Metadata) metadata).getReferenceSystemInfo()) {
                    if (rs instanceof CoordinateReferenceSystem) {
                        format(rs);
                        break;
                    }
                }
            }
        }
        return 0;
    }
}
