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
package org.apache.sis.metadata.iso.maintenance;

import java.util.Collection;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.maintenance.ScopeDescription;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.Namespaces;

// Specific to the main branch:
import org.opengis.metadata.quality.Scope;
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import org.apache.sis.util.collection.Containers;


/**
 * The target resource and physical extent for which information is reported.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_Scope}
 * {@code   ├─level…………………………………………………} Hierarchical level of the data specified by the scope.
 * {@code   └─levelDescription……………………} Detailed description about the level of the data specified by the scope.
 * {@code       ├─attributeInstances……} Attribute instances to which the information applies.
 * {@code       ├─attributes…………………………} Attributes to which the information applies.
 * {@code       ├─dataset…………………………………} Dataset to which the information applies.
 * {@code       ├─featureInstances…………} Feature instances to which the information applies.
 * {@code       ├─features………………………………} Features to which the information applies.
 * {@code       └─other………………………………………} Class of information that does not fall into the other categories.</div>
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
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "MD_Scope_Type", namespace = Namespaces.MCC, propOrder = {
   "level",
   "extents",
   "levelDescription"
})
@XmlRootElement(name = "MD_Scope", namespace = Namespaces.MCC)
public class DefaultScope extends ISOMetadata implements Scope {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -979575548481874359L;

    /**
     * Hierarchical level of the data specified by the scope.
     */
    private ScopeCode level;

    /**
     * Information about the spatial, vertical and temporal extent of the resource specified by the scope.
     */
    @SuppressWarnings("serial")
    private Collection<Extent> extents;

    /**
     * Detailed description about the level of the data specified by the scope.
     */
    @SuppressWarnings("serial")
    private Collection<ScopeDescription> levelDescription;

    /**
     * Constructs an initially empty scope.
     */
    public DefaultScope() {
    }

    /**
     * Creates a scope initialized to the given level.
     *
     * @param level  the hierarchical level of the data specified by the scope.
     */
    public DefaultScope(final ScopeCode level) {
        this.level = level;
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
        if (object != null) {
            level            = object.getLevel();
            levelDescription = copyCollection(object.getLevelDescription(), ScopeDescription.class);
            if (object instanceof DefaultScope) {
                extents = copyCollection(((DefaultScope) object).getExtents(), Extent.class);
            } else {
                extents = singleton(object.getExtent(), Extent.class);
            }
        }
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
     * Returns the hierarchical level of the data specified by the scope.
     *
     * @return hierarchical level of the data, or {@code null}.
     */
    @Override
    @XmlElement(name = "level", required = true)
    public ScopeCode getLevel() {
        return level;
    }

    /**
     * Sets the hierarchical level of the data specified by the scope.
     *
     * @param  newValue  the new level.
     */
    public void setLevel(final ScopeCode newValue) {
        checkWritePermission(level);
        level = newValue;
    }

    /**
     * Returns information about the spatial, vertical and temporal extents of the resource specified by the scope.
     *
     * @return information about the extent of the resource.
     *
     * @since 0.5
     */
    @XmlElement(name = "extent")
    @UML(identifier="extent", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Extent> getExtents() {
        return extents = nonNullCollection(extents, Extent.class);
    }

    /**
     * Sets information about the spatial, vertical and temporal extents of the resource specified by the scope.
     *
     * @param newValues New information about the extent of the resource.
     *
     * @since 0.5
     */
    public void setExtents(final Collection<? extends Extent> newValues) {
        extents = writeCollection(newValues, extents, Extent.class);
    }

    /**
     * Information about the spatial, vertical and temporal extent of the data specified by the scope.
     * This method fetches the value from the {@linkplain #getExtents() extents} collection.
     *
     * @return Information about the extent of the data, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getExtents()}.
     */
    @Override
    @Deprecated
    @Dependencies("getExtents")
    public Extent getExtent() {
        return LegacyPropertyAdapter.getSingleton(getExtents(), Extent.class, null, DefaultScope.class, "getExtent");
    }

    /**
     * Sets information about the spatial, vertical and temporal extent of the data specified by the scope.
     * This method stores the value in the {@linkplain #setExtents(Collection) extents} collection.
     *
     * @param newValue The new extent.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setExtents(Collection)}.
     */
    @Deprecated
    public void setExtent(final Extent newValue) {
        setExtents(Containers.singletonOrEmpty(newValue));
    }

    /**
     * Returns detailed descriptions about the level of the data specified by the scope.
     *
     * @return detailed description about the level of the data.
     */
    @Override
    @XmlElement(name = "levelDescription")
    public Collection<ScopeDescription> getLevelDescription() {
        return levelDescription = nonNullCollection(levelDescription, ScopeDescription.class);
    }

    /**
     * Sets detailed descriptions about the level of the data specified by the scope.
     *
     * @param  newValues  the new level description.
     */
    public void setLevelDescription(final Collection<? extends ScopeDescription> newValues) {
        levelDescription = writeCollection(newValues, levelDescription, ScopeDescription.class);
    }
}
