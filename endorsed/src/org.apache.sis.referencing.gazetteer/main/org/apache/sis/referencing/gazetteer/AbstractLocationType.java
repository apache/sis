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
package org.apache.sis.referencing.gazetteer;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.IdentityHashMap;
import java.util.Map;
import org.apache.sis.referencing.gazetteer.internal.Resources;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;

// Specific to the main branch:
import java.util.Collection;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.GeographicExtent;
import org.apache.sis.metadata.iso.citation.AbstractParty;


/**
 * Default implementation of {@code toString()}, {@code equals(Object)} and {@code hashCode()} methods
 * for {@code LocationType} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class AbstractLocationType implements LenientComparable {
    /**
     * For sub-class constructors.
     */
    protected AbstractLocationType() {
    }

    /**
     * Creates unmodifiable snapshots of the given location types. This method returns a new collection within which
     * all elements are snapshots of the given location types (in iteration order), except the reference system which
     * is set to the given value.
     *
     * <p>The location types returned by this method are {@linkplain java.io.Serializable serializable}
     * if all properties ({@linkplain ModifiableLocationType#getName() name},
     * {@linkplain ModifiableLocationType#getTerritoryOfUse() territory of use}, <i>etc.</i>
     * are also serializable).</p>
     *
     * @param  rs     the reference system to assign to the new location types, or {@code null} if none.
     * @param  types  the location types for which to take a snapshot.
     * @return unmodifiable copies of the given location types.
     */
    public static List<AbstractLocationType> snapshot(final ReferencingByIdentifiers rs, final AbstractLocationType... types) {
        ArgumentChecks.ensureNonNull("types", types);
        final List<AbstractLocationType> snapshot = FinalLocationType.snapshot(Arrays.asList(types), rs, new IdentityHashMap<>());
        final Map<AbstractLocationType,Boolean> parents = new IdentityHashMap<>();
        for (final AbstractLocationType type : snapshot) {
            checkForCycles(type, parents);
        }
        return snapshot;
    }

    /**
     * Implementation of {@link #checkForCycles()} to be invoked recursively for each children.
     *
     * @throws IllegalArgumentException if an infinite recursion is detected.
     */
    private static void checkForCycles(final AbstractLocationType type, final Map<AbstractLocationType,Boolean> parents) {
        if (parents.put(type, Boolean.TRUE) != null) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.LocationTypeCycle_1, type.getName()));
        }
        for (final AbstractLocationType child : type.getChildren()) {
            checkForCycles(child, parents);
        }
        parents.remove(type);
    }

    /**
     * Verifies that there is not cycles in the children.
     * This method should be invoked for validating a user argument.
     *
     * @throws IllegalArgumentException if an infinite recursion is detected.
     */
    final void checkForCycles() {
        checkForCycles(this, new IdentityHashMap<>());
    }

    /**
     * Name of the location type.
     */
    public abstract InternationalString getName();

    /**
     * Property used as the defining characteristic of the location type.
     */
    public abstract InternationalString getTheme();

    /**
     * Method(s) of uniquely identifying location instances.
     */
    public abstract Collection<? extends InternationalString> getIdentifications();

    /**
     * The way in which location instances are defined.
     */
    public abstract InternationalString getDefinition();

    /**
     * Geographic area within which the location type occurs.
     */
    public abstract GeographicExtent getTerritoryOfUse();

    /**
     * The reference system that comprises this location type.
     */
    public abstract ReferencingByIdentifiers getReferenceSystem();

    /**
     * Name of organization or class of organization able to create and destroy location instances.
     */
    public abstract AbstractParty getOwner();

    /**
     * Parent location types (location types of which this location type is a sub-division).
     * A location type can have more than one possible parent. For example the parent of a
     * location type named <cite>“street”</cite> could be <cite>“locality”</cite>, <cite>“town”</cite>
     * or <cite>“administrative area”</cite>.
     */
    public abstract Collection<? extends AbstractLocationType> getParents();

    /**
     * Child location types (location types which sub-divides this location type).
     */
    public abstract Collection<? extends AbstractLocationType> getChildren();

    /**
     * Compares this location type with the specified object for equality.
     * This method compares the value of {@link #getName()} and {@link #getChildren()} in all modes.
     * At the opposite, values of {@link #getParents()} and {@link #getReferenceSystem()} are never
     * compared, no matter the mode, for avoiding never-ending loops. Other properties may or may
     * not be compared depending on the {@code mode} argument.
     *
     * <p>If the {@code mode} argument value is {@link ComparisonMode#STRICT STRICT} or
     * {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then almost all properties are compared
     * including the {@linkplain #getTheme() theme} and the {@linkplain #getOwner() owner}.</p>
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (object != null) switch (mode) {
            case STRICT: {
                if (getClass() != object.getClass()) break;
                // Fall through
            }
            case BY_CONTRACT: {
                if (!(object instanceof AbstractLocationType)) break;
                final AbstractLocationType that = (AbstractLocationType) object;
                // Do not compare the ReferenceSystem as it may cause an infinite recursion.
                if (!Utilities.deepEquals(getTheme(),           that.getTheme(),           mode) ||
                    !Utilities.deepEquals(getIdentifications(), that.getIdentifications(), mode) ||
                    !Utilities.deepEquals(getDefinition(),      that.getDefinition(),      mode) ||
                    !Utilities.deepEquals(getTerritoryOfUse(),  that.getTerritoryOfUse(),  mode) ||
                    !Utilities.deepEquals(getOwner(),           that.getOwner(),           mode))
                {
                    break;
                }
                // Fall through
            }
            default: {
                if (!(object instanceof AbstractLocationType)) break;
                final AbstractLocationType that = (AbstractLocationType) object;
                if (Objects.equals(getName(), that.getName())) {
                    /*
                     * To be safe, we should apply some check against infinite recursion here.
                     * We do not on the assumption that subclasses verified that we do not have
                     * any cycle.
                     */
                    return Utilities.deepEquals(getChildren(), that.getChildren(), mode);
                }
                break;
            }
        }
        return false;
    }

    /**
     * Compares this location type with the specified object for strict equality.
     * This method compares all properties except the value returned by {@link #getParents()}
     * and {@link #getReferenceSystem()}, for avoiding never-ending loops.
     *
     * <p>This method is implemented as below:</p>
     * {@snippet lang="java" :
     *     return equals(object, ComparisonMode.STRICT);
     *     }
     *
     * @param  object  the object to compare to {@code this}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public final boolean equals(final Object object) {
        return equals(object, ComparisonMode.STRICT);
    }

    /**
     * Returns a hash code value for this location type.
     *
     * @return a hash code value for this location type.
     */
    @Override
    public int hashCode() {
        int code = Objects.hashCode(getName());
        for (final AbstractLocationType child : getChildren()) {
            // Take only children name without recursion over their own children.
            code = code*31 + Objects.hashCode(child.getName());
        }
        return code;
    }

    /**
     * Returns a string representation of this location type and all its children.
     * Current implementation formats a tree with the {@linkplain ModifiableLocationType#getName() name}
     * and {@linkplain ModifiableLocationType#getDefinition() definition} of each type, like below:
     *
     * <pre class="text">
     *   administrative area………………… area of responsibility of highest level local authority
     *     ├─town……………………………………………… city or town
     *     │   └─street……………………………… thoroughfare providing access to properties
     *     └─street………………………………………… thoroughfare providing access to properties</pre>
     *
     * The string representation is mostly for debugging purpose and may change in any future SIS version.
     *
     * @return a string representation of this location type.
     */
    @Override
    public String toString() {
        final DefaultTreeTable table = new DefaultTreeTable(TableColumn.NAME, TableColumn.VALUE_AS_TEXT);
        format(this, table.getRoot());
        return table.toString();
    }

    /**
     * Invoked recursively for formatting the given type in the given tree.
     * This method does not perform any check against infinite recursion
     * on the assumption that subclasses verified this constraint by calls
     * to {@link #checkForCycles()}.
     */
    private static void format(final AbstractLocationType type, final TreeTable.Node node) {
        node.setValue(TableColumn.NAME, type.getName());
        node.setValue(TableColumn.VALUE_AS_TEXT, type.getDefinition());
        for (final AbstractLocationType child : type.getChildren()) {
            format(child, node.newChild());
        }
    }
}
