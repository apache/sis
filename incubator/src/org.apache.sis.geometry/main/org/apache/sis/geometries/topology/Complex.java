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
package org.apache.sis.geometries.topology;

import java.util.Set;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.apache.sis.geometries.Geometry;


/**
 * A set of topological primitives, organized so that set operations on its elements perform the equivalent
 * operations on the positions of its geometric realization.
 *
 * <p>A complex of dimension <var>n</var> contains primitives of every dimension up to <var>n</var>:
 * a 2-dimensional complex contains faces, edges and nodes, while a 1-dimensional complex, or graph,
 * contains only edges and nodes.</p>
 *
 * <p>The default construction of a topological complex is from a geometric collection, which then becomes
 * its geometric realization. Only a collection of geometric primitives with mutually disjoint interiors
 * gives a complex without error. Java interfaces have no constructor, so this is left to implementations.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.7.2
 */
@UML(identifier="Complex", specification=ISO_19107)
public interface Complex extends Topology {

    /**
     * The primitives of this complex. This association makes a complex a set of primitives,
     * and the set operations of the {@linkplain #getSubComplexes() Contains} association agree with it.
     *
     * @return primitives of this complex, at least one.
     *
     * @see ISO 19107:2019 - 10.7.2.7
     */
    @UML(identifier="element", specification=ISO_19107)
    Set<Primitive> getElements();

    /**
     * Returns {@code true} if this complex is not contained in any larger complex.
     *
     * <p>The default implementation tests whether this complex is its own
     * {@linkplain #getMaximalComplex() maximal complex}.</p>
     *
     * @return whether this complex is maximal.
     *
     * @see ISO 19107:2019 - 10.7.2.4
     */
    @UML(identifier="isMaximal", specification=ISO_19107)
    default boolean isMaximal() {
        return equals(getMaximalComplex());
    }

    /**
     * Returns {@code true} if this complex is topologically connected, which implies that its geometric
     * realization is connected too. It is the case when every primitive of this complex is linked to every
     * other one by a sequence of boundary, co-boundary and isolated-in associations whose intermediate
     * primitives all belong to this complex.
     *
     * @return whether this complex is connected.
     *
     * @see ISO 19107:2019 - 10.7.2.5
     */
    @UML(identifier="isConnected", specification=ISO_19107)
    boolean isConnected();

    /**
     * The complexes contained in this complex, as sets of primitives.
     *
     * @return complexes contained in this complex.
     *
     * @see ISO 19107:2019 - 10.7.2.6
     */
    @UML(identifier="subComplex", specification=ISO_19107)
    Set<Complex> getSubComplexes();

    /**
     * The complexes containing this complex, as sets of primitives.
     *
     * @return complexes containing this complex.
     *
     * @see ISO 19107:2019 - 10.7.2.6
     */
    @UML(identifier="superComplex", specification=ISO_19107)
    Set<Complex> getSuperComplexes();

    /**
     * The geometric complex this complex represents, or {@code null} if none.
     *
     * <p>Difference with ISO 19107: the standard returns a geometric complex,
     * a type which does not exist yet in this module.</p>
     *
     * @return geometric realization of this complex, or {@code null} if none.
     *
     * @see ISO 19107:2019 - 10.7.2.8
     */
    @UML(identifier="geometry", specification=ISO_19107)
    Geometry getGeometry();

}
