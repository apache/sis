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
package org.apache.sis.style.sld1;

import java.util.Optional;
import org.opengis.metadata.citation.OnlineResource;


/**
 * Reference to an external SLD document to import into a {@link StyledLayerDescriptor}.
 * The library is used for resolving named layers and named styles which are not known
 * to the map server. This element was introduced in SLD 1.1; it has no equivalent in SLD 1.0.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class UseSLDLibrary extends SLDElement {
    /**
     * Location of the external SLD document, or {@code null} if none.
     *
     * @see #getOnlineResource()
     * @see #setOnlineResource(OnlineResource)
     */
    protected OnlineResource onlineResource;

    /**
     * Creates an initially empty library reference.
     */
    public UseSLDLibrary() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public UseSLDLibrary(final UseSLDLibrary source) {
        onlineResource = source.onlineResource;
    }

    /**
     * Returns the location of the external SLD document.
     *
     * @return location of the external SLD document.
     */
    public Optional<OnlineResource> getOnlineResource() {
        return Optional.ofNullable(onlineResource);
    }

    /**
     * Sets the location of the external SLD document.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new location of the external SLD document, or {@code null} if none.
     */
    public void setOnlineResource(final OnlineResource value) {
        onlineResource = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {onlineResource};
    }

    /**
     * Returns a clone of this object.
     * The online resource is not cloned.
     *
     * @return a clone of this element.
     */
    @Override
    public UseSLDLibrary clone() {
        return (UseSLDLibrary) super.clone();
    }
}
