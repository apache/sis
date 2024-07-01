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
package org.apache.sis.storage.geopackage;

import java.util.Optional;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.Names;
import org.opengis.util.GenericName;
import org.apache.sis.storage.geopackage.privy.Record;

/**
 * An unidentified content data in the geopackage.
 * This class represent resources for which no GpkgContentHandler has been found.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GpkgUndefinedResource extends AbstractResource {

    protected final GpkgStore store;
    protected final Record.Content row;
    protected final NamedIdentifier identifier;

    public GpkgUndefinedResource(GpkgStore store, Record.Content row) {
        super(null, false);
        this.store = store;
        this.row = row;
        this.identifier = NamedIdentifier.castOrCopy(Names.createLocalName(null, null, row.identifier));
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(identifier);
    }
}
