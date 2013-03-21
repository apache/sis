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

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.quality.Scope;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.maintenance.ScopeDescription;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Description of the data specified by the scope.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "DQ_Scope_Type", propOrder = {
   "level",
   "extent",
   "levelDescription"
})
@XmlRootElement(name = "DQ_Scope")
public class DefaultScope extends ISOMetadata implements Scope {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8021256328527422972L;

    /**
     * Hierarchical level of the data specified by the scope.
     */
    private ScopeCode level;

    /**
     * Information about the spatial, vertical and temporal extent of the data specified by the
     * scope.
     */
    private Extent extent;

    /**
     * Detailed description about the level of the data specified by the scope.
     */
    private Collection<ScopeDescription> levelDescription;

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
        this.level = level;
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultScope castOrCopy(final Scope object) {
        if (object == null || object instanceof DefaultScope) {
            return (DefaultScope) object;
        }
        final DefaultScope copy = new DefaultScope();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the hierarchical level of the data specified by the scope.
     */
    @Override
    @XmlElement(name = "level", required = true)
    public synchronized ScopeCode getLevel() {
        return level;
    }

    /**
     * Sets the hierarchical level of the data specified by the scope.
     *
     * @param newValue The new level.
     */
    public synchronized void setLevel(final ScopeCode newValue) {
        checkWritePermission();
        level = newValue;
    }

    /**
     * Returns detailed descriptions about the level of the data specified by the scope.
     * Should be defined only if the {@linkplain #getLevel level} is not equal
     * to {@link ScopeCode#DATASET DATASET} or {@link ScopeCode#SERIES SERIES}.
     */
    @Override
    @XmlElement(name = "levelDescription")
    public synchronized Collection<ScopeDescription> getLevelDescription() {
        return levelDescription = nonNullCollection(levelDescription, ScopeDescription.class);
    }

    /**
     * Sets detailed descriptions about the level of the data specified by the scope.
     *
     * @param newValues The new level description.
     */
    public synchronized void setLevelDescription(final Collection<? extends ScopeDescription> newValues) {
        levelDescription = copyCollection(newValues, levelDescription, ScopeDescription.class);
    }

    /**
     * Information about the spatial, vertical and temporal extent of the data specified by the
     * scope.
     */
    @Override
    @XmlElement(name = "extent")
    public synchronized Extent getExtent() {
        return extent;
    }

    /**
     * Sets information about the spatial, vertical and temporal extent of the data specified
     * by the scope.
     *
     * @param newValue The new extent.
     */
    public synchronized void setExtent(final Extent newValue) {
        checkWritePermission();
        extent = newValue;
    }
}
