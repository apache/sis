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
package org.apache.sis.temporal;

import java.util.List;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.Serializable;
import org.opengis.metadata.Identifier;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.bind.ModifiableIdentifierMap;


/**
 * Base class of temporal objects. This class allows to associate identifiers to this temporal object.
 * The list of identifiers is modifiable because identifiers often need to be added after creation time,
 * for example in order to associate {@code gml:id} during XML unmarshalling.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class TemporalObject implements IdentifiedObject, LenientComparable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5408766446198380089L;

    /**
     * The identifier for this temporal object, or {@code null} if not yet created.
     */
    private CopyOnWriteArrayList<Identifier> identifiers;

    /**
     * Creates a new temporal object with no identifier.
     */
    TemporalObject() {
    }

    /**
     * Returns all identifiers associated to this temporal object.
     *
     * @return all identifiers associated to this object, or an empty collection if none.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final synchronized Collection<Identifier> getIdentifiers() {
        if (identifiers == null) {
            identifiers = new CopyOnWriteArrayList<>();
        }
        return identifiers;
    }

    /**
     * Returns map view of the identifiers collection as (<var>authority</var>, <var>code</var>) entries.
     *
     * @return the identifiers as a map of (<var>authority</var>, <var>code</var>) entries, or an empty map if none.
     */
    @Override
    public final IdentifierMap getIdentifierMap() {
        return new ModifiableIdentifierMap(getIdentifiers());
    }

    /**
     * Compares that identifiers of this temporal object with the identifiers of the given object.
     */
    final boolean equalIdentifiers(final TemporalObject that) {
        List<Identifier> id1, id2;
        synchronized (this) {id1 = this.identifiers;}
        synchronized (that) {id2 = that.identifiers;}
        if (id1 == null) id1 = List.of();
        if (id2 == null) id2 = List.of();
        return id1.equals(id2);
    }
}
