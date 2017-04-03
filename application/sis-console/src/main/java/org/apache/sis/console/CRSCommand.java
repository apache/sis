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

import org.opengis.metadata.Metadata;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * The "crs" sub-command.
 * CRS are considered as a kind of metadata here.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
final class CRSCommand extends FormattedOutputCommand {
    /**
     * Creates the {@code "crs"} sub-command.
     */
    CRSCommand(final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, MetadataCommand.options(), OutputFormat.WKT, OutputFormat.XML);
    }

    /**
     * Prints metadata or CRS information.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than an uncaught Java exception.
     */
    @Override
    public int run() throws Exception {
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
