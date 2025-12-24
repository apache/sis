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

import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.metadata.quality.Scope;
import org.opengis.metadata.maintenance.ScopeCode;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Collection;
import org.opengis.metadata.extent.Extent;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import org.apache.sis.util.collection.Containers;


/**
 * Description of the data specified by the scope.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @version 1.4
 * @since   0.3
 *
 * @deprecated As of ISO 19115:2014, {@code DQ_Scope} has been replaced by {@code MD_Scope}.
 *             The latter is defined in the {@link org.apache.sis.metadata.iso.maintenance} package.
 */
@Deprecated(since="1.0")
@XmlTransient
public class DefaultScope extends org.apache.sis.metadata.iso.maintenance.DefaultScope implements Scope {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7517784393752337009L;

    /**
     * Constructs an initially empty scope.
     */
    public DefaultScope() {
    }

    /**
     * Creates a scope initialized to the given level.
     *
     * @param level The hierarchical level of the data specified by the scope.
     */
    public DefaultScope(final ScopeCode level) {
        super(level);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Scope)
     */
    public DefaultScope(final Scope object) {
        super(object);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultScope}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultScope} instance is created using the
     *       {@linkplain #DefaultScope(Scope) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultScope castOrCopy(final Scope object) {
        if (object == null || object instanceof DefaultScope) {
            return (DefaultScope) object;
        }
        return new DefaultScope(object);
    }

    /**
     * Information about the spatial, vertical and temporal extent of the data specified by the scope.
     * This method fetches the value from the {@linkplain #getExtents() extents} collection.
     *
     * @return information about the extent of the data, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getExtents()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getExtents")
    public Extent getExtent() {
        return LegacyPropertyAdapter.getSingleton(getExtents(), Extent.class, null, DefaultScope.class, "getExtent");
    }

    /**
     * Sets information about the spatial, vertical and temporal extent of the data specified by the scope.
     * This method stores the value in the {@linkplain #setExtents(Collection) extents} collection.
     *
     * @param  newValue  the new extent.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setExtents(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setExtent(final Extent newValue) {
        setExtents(Containers.singletonOrEmpty(newValue));
    }
}
