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
package org.apache.sis.referencing.cs;

import java.util.Map;
import java.util.List;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Workaround;

import static java.util.Collections.singletonMap;
import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.util.Utilities.deepEquals;


/**
 * A coordinate system made of two or more independent coordinate systems.
 *
 * <table class="sis">
 * <caption>Permitted associations</caption>
 * <tr>
 *   <th>Used with CRS</th>
 *   <th>Permitted axis names</th>
 * </tr><tr>
 *   <td>{@linkplain org.apache.sis.referencing.crs.DefaultCompoundCRS Compound}</td>
 *   <td>(not applicable)</td>
 * </tr></table>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * and the {@link CoordinateSystemAxis} instances given to the constructor are also immutable. Unless otherwise
 * noted in the javadoc, this condition holds if all components were created using only SIS factories and static
 * constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@XmlTransient
public class DefaultCompoundCS extends AbstractCS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5726410275278843373L;

    /**
     * The coordinate systems.
     */
    private final List<CoordinateSystem> components;

    /**
     * Constructs a coordinate system from a set of properties and a sequence of coordinate systems.
     * The properties map is given unchanged to the
     * {@linkplain AbstractCS#AbstractCS(Map,CoordinateSystemAxis[]) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties The properties to be given to the identified object.
     * @param components The set of coordinate system.
     */
    public DefaultCompoundCS(final Map<String,?> properties, CoordinateSystem... components) {
        super(properties, getAxes(components = clone(components)));
        this.components = UnmodifiableArrayList.wrap(components);
    }

    /**
     * Constructs a compound coordinate system from a sequence of coordinate systems.
     * A default name for this CS will be inferred from the names of all specified CS.
     *
     * @param components The set of coordinate system.
     */
    public DefaultCompoundCS(CoordinateSystem... components) {
        this(components = clone(components), getAxes(components));
    }

    /**
     * This is a work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     *
     * @param components The coordinate systems.
     */
    @Workaround(library="JDK", version="1.7")
    private DefaultCompoundCS(final CoordinateSystem[] components, final CoordinateSystemAxis[] axes) {
        super(singletonMap(NAME_KEY, AxisDirections.appendTo(new StringBuilder(60).append("Compound CS"), axes)), axes);
        this.components = UnmodifiableArrayList.wrap(components);
    }

    /**
     * Returns a clone of the given array, making sure that it contains only non-null elements.
     */
    private static CoordinateSystem[] clone(CoordinateSystem[] components) {
        ensureNonNull("components", components);
        components = components.clone();
        for (int i=0; i<components.length; i++) {
            ensureNonNullElement("components", i, components[i]);
        }
        return components;
    }

    /**
     * Returns all axes in the given sequence of components.
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static CoordinateSystemAxis[] getAxes(final CoordinateSystem[] components) {
        int count = 0;
        for (int i=0; i<components.length; i++) {
            count += components[i].getDimension();
        }
        final CoordinateSystemAxis[] axis = new CoordinateSystemAxis[count];
        count = 0;
        for (final CoordinateSystem c : components) {
            final int dim = c.getDimension();
            for (int j=0; j<dim; j++) {
                axis[count++] = c.getAxis(j);
            }
        }
        assert count == axis.length;
        return axis;
    }

    /**
     * Returns all coordinate systems in this compound CS.
     *
     * @return All coordinate systems in this compound CS.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<CoordinateSystem> getComponents() {
        return components;  // Unmodifiable.
    }

    /*
     * Do not override createForAxes(…) and forConvention(…) because we can not create a new DefaultCompoundCS
     * without knownledge of the CoordinateSystem components to give to it. It would be possible to recursively
     * invoke components[i].forConvention(…), but it would be useless to perform such decomposition here because
     * DefaultCompoundCRS will need to perform its own decomposition anyway.
     *
     * If the user invokes DefaultCompoundCS.forConvention(…) anyway, he will get an AbstractCS instance, which
     * is not that bad.
     */

    /**
     * Compares this coordinate system with the specified object for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (!(object instanceof DefaultCompoundCS && super.equals(object, mode))) {
            return false;
        }
        return deepEquals(components, ((DefaultCompoundCS) object).components, mode);
    }
}
